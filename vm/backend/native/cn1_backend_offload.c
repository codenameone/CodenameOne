/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

/*
 * Blocking local I/O, run off the virtual-thread hosts.
 *
 * A virtual thread is a coroutine on a host pthread, and a socket wait parks it
 * (cn1BackendVtWait) so the host runs the others. A DISK call cannot park that
 * way: a regular file is always "ready" to epoll and kqueue, and read(), pread(),
 * open(), sqlite3_step() simply block the calling pthread until the disk answers.
 * On a host that is every virtual thread scheduled there, so one cold page or one
 * slow query stalled requests that never touched a file.
 *
 * cn1BackendOffload hands such a call to a small pool of platform threads and
 * parks the virtual thread on a completion descriptor, which the existing poller
 * watches like any socket. The handler is none the wiser: the native it called
 * returns the same answer, later, without having held the host.
 *
 * It does that ONLY for a virtual thread that may park (cn1BackendCanPark). A
 * platform thread, a worker in pool mode, a virtual thread pinned inside
 * `synchronized`, and a build without CN1_VIRTUAL_THREADS all run the call
 * inline, as before: they hold their thread for the call anyway, and a round
 * trip through the pool would only add latency.
 *
 * Rules for a job function, which runs on a pool thread:
 *   - Touch no Java object and no thread state. Copy what it needs out of the
 *     heap BEFORE the offload (the caller's frame keeps a byte[] alive, and the
 *     collector does not move objects, so a pointer into an array's data stays
 *     valid while the caller is parked) and build Java results AFTER it.
 *   - Read errno inside the job, not in the caller: errno is per pthread.
 *
 * The caller must be inside CN1_YIELD_THREAD, as every blocking native already
 * is: a parked virtual thread cannot answer the collector.
 *
 * On Linux the plain opens, reads and writes go through io_uring instead
 * (cn1BackendUringRead and friends, at the end of this file): the kernel runs
 * them and no pool thread is involved. Everything io_uring cannot express --
 * SQLite, which does its own I/O, realpath, mkstemp, sendfile -- and every
 * kernel or container that refuses io_uring stays on this pool.
 */
#include "cn1_globals.h"
#include <stdlib.h>
#include <string.h>
#include <errno.h>

#ifndef _WIN32
#include <unistd.h>
#include <fcntl.h>
#include <poll.h>
#include <pthread.h>
#include <signal.h>
#if defined(__linux__)
#include <sys/eventfd.h>
#define CN1_OFFLOAD_EVENTFD 1
#endif
#endif

typedef void (*cn1OffloadFn)(void* arg);

int cn1BackendCanPark(void);
void cn1BackendVtOffloading(int delta);
int cn1BackendVtWait(int count, const int* fds, const int* events, long long timeoutMillis);

#if defined(_WIN32) || !defined(CN1_VIRTUAL_THREADS) || defined(CN1_BACKEND_NO_IO_OFFLOAD)

/*
 * No virtual threads, nothing to keep free: every call is the inline call.
 *
 * -DCN1_BACKEND_NO_IO_OFFLOAD compiles the pool out for an A/B, and it is what
 * a reviewer re-injects to watch BackendHttpIntegrationTest's two
 * "...does not hold the virtual-thread host" tests fail. A build-time ablation
 * rather than a runtime switch: a deployment that can turn this off is one whose
 * stalls the switch hides.
 */
void cn1BackendOffload(cn1OffloadFn fn, void* arg) {
    fn(arg);
}

/* No ring either: every caller takes its pool path, which is the inline call. */
int cn1BackendUringRead(int fd, void* buf, size_t len, long long offset, long long* result) {
    (void)fd; (void)buf; (void)len; (void)offset; (void)result;
    return 0;
}
int cn1BackendUringWrite(int fd, const void* buf, size_t len, long long* result) {
    (void)fd; (void)buf; (void)len; (void)result;
    return 0;
}
int cn1BackendUringOpenat(int dirFd, const char* path, int flags, long long* result) {
    (void)dirFd; (void)path; (void)flags; (void)result;
    return 0;
}
int cn1BackendUringOpenat2(int dirFd, const char* path, void* how, size_t howSize, long long* result) {
    (void)dirFd; (void)path; (void)how; (void)howSize; (void)result;
    return 0;
}

#else

#define CN1_OFFLOAD_EVENT_READ 1   /* CN1_EVENT_READ in cn1_backend_server.c */
#define CN1_OFFLOAD_DEFAULT_THREADS 4
#define CN1_OFFLOAD_MAX_THREADS 64

/*
 * A completion descriptor: an eventfd on Linux, a pipe elsewhere. Kept on a
 * per-host free list, so an offload creates none once the host is warm.
 */
struct cn1OffloadSignal {
    int readFd;
    int writeFd;
    /* 1 from hand-off until the wakeup write has RETURNED (atomics). The write
       lands after `done`, so a signal back on the free list may still be about
       to receive it; teardown closes only signals that are not busy. */
    int busy;
    struct cn1OffloadSignal* next;
};

/* Lives on the parked virtual thread's own stack, which stays put until it resumes. */
struct cn1OffloadJob {
    cn1OffloadFn fn;
    void* arg;
    struct cn1OffloadSignal* signal;
    int done;                       /* read and written with __atomic builtins */
    struct cn1OffloadJob* next;
};

static pthread_mutex_t cn1OffloadLock = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cn1OffloadReady = PTHREAD_COND_INITIALIZER;
static struct cn1OffloadJob* cn1OffloadHead = 0;
static struct cn1OffloadJob* cn1OffloadTail = 0;
static pthread_once_t cn1OffloadOnce = PTHREAD_ONCE_INIT;
static int cn1OffloadThreads = 0;  /* written once, inside the once */

/*
 * Per HOST, not per virtual thread: only host-thread code touches it, and the
 * virtual threads on a host run one at a time, so it needs no lock. A virtual
 * thread that resumes on another host returns its signal to that host's list,
 * which is just as good -- this is a cache, not ownership.
 */
static __thread struct cn1OffloadSignal* cn1OffloadFree = 0;

static void* cn1OffloadWorker(void* unused) {
    (void)unused;
    for(;;) {
        struct cn1OffloadJob* job;
        struct cn1OffloadSignal* sig;
        int fd;
        pthread_mutex_lock(&cn1OffloadLock);
        while(cn1OffloadHead == 0) {
            pthread_cond_wait(&cn1OffloadReady, &cn1OffloadLock);
        }
        job = cn1OffloadHead;
        cn1OffloadHead = job->next;
        if(cn1OffloadHead == 0) {
            cn1OffloadTail = 0;
        }
        pthread_mutex_unlock(&cn1OffloadLock);

        job->fn(job->arg);

        /*
         * The descriptor is read out BEFORE `done` is published, and the job is
         * never touched after. Once `done` is visible the caller may return and
         * its stack -- this job -- is gone, so the write below must not reach
         * through `job`. A late byte landing on a signal some other caller has
         * since taken is harmless: every waiter re-tests its own `done` flag.
         */
        sig = job->signal;
        fd = sig->writeFd;
        __atomic_store_n(&job->done, 1, __ATOMIC_RELEASE);
#ifdef CN1_OFFLOAD_EVENTFD
        {
            uint64_t one = 1;
            ssize_t ignored;
            do {
                ignored = write(fd, &one, sizeof(one));
            } while(ignored < 0 && errno == EINTR);
        }
#else
        {
            char one = 1;
            ssize_t ignored;
            do {
                ignored = write(fd, &one, 1);
            } while(ignored < 0 && errno == EINTR);
            /* EAGAIN means the pipe already holds a wakeup, which is all it needs. */
        }
#endif
        /* The signal is heap memory that outlives every caller, so this is safe
           after `done`; only teardown reads it. */
        __atomic_store_n(&sig->busy, 0, __ATOMIC_RELEASE);
    }
    return 0;
}

static void cn1OffloadStart(void) {
    const char* configured = getenv("CN1_BACKEND_IO_THREADS");
    int want = CN1_OFFLOAD_DEFAULT_THREADS;
    int iter;
    pthread_attr_t attr;
    sigset_t all;
    sigset_t saved;
    if(configured != 0 && configured[0] != 0) {
        /* A size, never an off switch: at least one thread. */
        int parsed = atoi(configured);
        if(parsed >= 1) {
            want = parsed > CN1_OFFLOAD_MAX_THREADS ? CN1_OFFLOAD_MAX_THREADS : parsed;
        }
    }
    if(pthread_attr_init(&attr) != 0) {
        return;
    }
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    /*
     * The workers start with every signal blocked, so SIGTERM, SIGINT and SIGPIPE
     * keep going where cn1_backend_signals.c routes them rather than interrupting
     * a pool thread in the middle of a read.
     */
    sigfillset(&all);
    pthread_sigmask(SIG_SETMASK, &all, &saved);
    for(iter = 0 ; iter < want ; iter++) {
        pthread_t worker;
        if(pthread_create(&worker, &attr, cn1OffloadWorker, 0) != 0) {
            break;
        }
        cn1OffloadThreads++;
    }
    pthread_sigmask(SIG_SETMASK, &saved, 0);
    pthread_attr_destroy(&attr);
}

/*
 * HOST TEARDOWN. Hosts are not permanent -- every server start makes new
 * poller threads -- so a host's pooled signals and its io_uring are released
 * when the thread exits, through a pthread key destructor. Anything that may
 * still be written to is LEAKED, not closed: a busy signal (its wakeup write
 * has not returned, and a closed descriptor number can already belong to
 * someone's file) and a ring with an operation still in the kernel (it writes
 * into a virtual thread's stack, which freeImpl leaks for the same reason).
 */
static pthread_once_t cn1OffloadKeyOnce = PTHREAD_ONCE_INIT;
static pthread_key_t cn1OffloadKey;
static int cn1OffloadKeyOk = 0;
static void cn1UringHostExit(void);

static void cn1OffloadHostExit(void* unused) {
    struct cn1OffloadSignal* s = cn1OffloadFree;
    struct cn1OffloadSignal* kept = 0;
    (void)unused;
    cn1UringHostExit();
    while(s != 0) {
        struct cn1OffloadSignal* next = s->next;
        if(__atomic_load_n(&s->busy, __ATOMIC_ACQUIRE)) {
            s->next = kept;
            kept = s;
        } else {
            close(s->readFd);
            if(s->writeFd != s->readFd) {
                close(s->writeFd);
            }
            free(s);
        }
        s = next;
    }
    cn1OffloadFree = kept;
}

static void cn1OffloadKeyCreate(void) {
    cn1OffloadKeyOk = pthread_key_create(&cn1OffloadKey, cn1OffloadHostExit) == 0;
}

static void cn1OffloadHostRegister(void) {
    pthread_once(&cn1OffloadKeyOnce, cn1OffloadKeyCreate);
    if(cn1OffloadKeyOk && pthread_getspecific(cn1OffloadKey) == 0) {
        pthread_setspecific(cn1OffloadKey, (void*)1);
    }
}

static struct cn1OffloadSignal* cn1OffloadTakeSignal(void) {
    struct cn1OffloadSignal* s = cn1OffloadFree;
    if(s != 0) {
        cn1OffloadFree = s->next;
        s->next = 0;
        return s;
    }
    s = (struct cn1OffloadSignal*)malloc(sizeof(struct cn1OffloadSignal));
    if(s == 0) {
        return 0;
    }
#ifdef CN1_OFFLOAD_EVENTFD
    s->readFd = eventfd(0, EFD_NONBLOCK | EFD_CLOEXEC);
    s->writeFd = s->readFd;
    if(s->readFd < 0) {
        free(s);
        return 0;
    }
#else
    {
        int fds[2];
        if(pipe(fds) != 0) {
            free(s);
            return 0;
        }
        fcntl(fds[0], F_SETFL, fcntl(fds[0], F_GETFL) | O_NONBLOCK);
        fcntl(fds[1], F_SETFL, fcntl(fds[1], F_GETFL) | O_NONBLOCK);
        fcntl(fds[0], F_SETFD, FD_CLOEXEC);
        fcntl(fds[1], F_SETFD, FD_CLOEXEC);
        s->readFd = fds[0];
        s->writeFd = fds[1];
    }
#endif
    s->busy = 0;
    s->next = 0;
    cn1OffloadHostRegister();
    return s;
}

static void cn1OffloadReturnSignal(struct cn1OffloadSignal* s) {
    s->next = cn1OffloadFree;
    cn1OffloadFree = s;
}

/* Empties a signal of every wakeup it holds; it is non-blocking. */
static void cn1OffloadDrain(int fd) {
#ifdef CN1_OFFLOAD_EVENTFD
    uint64_t value;
    while(read(fd, &value, sizeof(value)) > 0) {
    }
#else
    char buffer[16];
    while(read(fd, buffer, sizeof(buffer)) > 0) {
    }
#endif
}

void cn1BackendOffload(cn1OffloadFn fn, void* arg) {
    struct cn1OffloadJob job;
    struct cn1OffloadSignal* signal;
    int events = CN1_OFFLOAD_EVENT_READ;
    if(!cn1BackendCanPark()) {
        fn(arg);
        return;
    }
    pthread_once(&cn1OffloadOnce, cn1OffloadStart);
    if(cn1OffloadThreads == 0) {
        /* Not one thread could be started; the call still has to happen. */
        fn(arg);
        return;
    }
    signal = cn1OffloadTakeSignal();
    if(signal == 0) {
        fn(arg);
        return;
    }
    job.fn = fn;
    job.arg = arg;
    job.signal = signal;
    __atomic_store_n(&signal->busy, 1, __ATOMIC_RELAXED);
    job.done = 0;
    job.next = 0;
    /* Counted before the hand-off: from here a pool thread may be writing into
       this stack, so a server stop must not free it (VirtualThread freeImpl). */
    cn1BackendVtOffloading(1);

    pthread_mutex_lock(&cn1OffloadLock);
    if(cn1OffloadTail == 0) {
        cn1OffloadHead = &job;
    } else {
        cn1OffloadTail->next = &job;
    }
    cn1OffloadTail = &job;
    pthread_cond_signal(&cn1OffloadReady);
    pthread_mutex_unlock(&cn1OffloadLock);

    /*
     * Wait for OUR flag, never for a byte. The descriptor can hold a late wakeup
     * from a job that finished after its own caller had already seen `done` and
     * moved on, so a readable signal proves nothing; the flag does. A resume for
     * any other reason (a server stop) just loops back into the park, because the
     * job owns `arg` until it reports.
     */
    while(!__atomic_load_n(&job.done, __ATOMIC_ACQUIRE)) {
        if(!cn1BackendVtWait(1, &signal->readFd, &events, -1)) {
            /* Could not park after all: wait holding the host, as the inline call would have. */
            struct pollfd p;
            p.fd = signal->readFd;
            p.events = POLLIN;
            p.revents = 0;
            poll(&p, 1, 10);
        }
        cn1OffloadDrain(signal->readFd);
    }
    cn1BackendVtOffloading(-1);
    cn1OffloadReturnSignal(signal);
}

/*
 * IO_URING, LINUX ONLY: the plain file calls without a thread hop.
 *
 * One ring per HOST thread, created on the host's first file call. Every
 * operation is submitted as a pair: the operation itself, HARD-linked to an
 * 8-byte write into the caller's pooled eventfd. The kernel posts the
 * operation's completion and then runs the write, so the virtual thread parks
 * on its own descriptor through the existing poller -- exactly as it does for a
 * pool job -- and needs no thread of ours to wake it. HARD link, not a plain
 * link: a short read counts as a failure to IOSQE_IO_LINK and would cancel the
 * wakeup.
 *
 * The completion queue is shared by everything on the host, so whichever
 * virtual thread wakes reaps EVERY finished entry and writes each result into
 * the job it names (user_data, a pointer to the job on its owner's stack). That
 * is safe because a virtual thread never moves to another host (HttpServer.stop:
 * "a yielded task can only ever run on the host it started on"), so every job in
 * this ring belongs to a virtual thread parked on this same OS thread, and the
 * virtual threads of a host run one at a time. The wakeup write carries
 * user_data 0 and is discarded.
 *
 * Nothing here is an off switch. A kernel without io_uring, a seccomp profile
 * that refuses it (Docker's and containerd's defaults do), a ring that cannot be
 * mapped, or an opcode the probe does not list each make the call answer 0, and
 * the caller runs its pool job instead. One line on stderr says which path the
 * process got, because "io_uring was refused" is otherwise invisible.
 */
#if defined(__linux__) && defined(__has_include)
#if __has_include(<linux/io_uring.h>)
#define CN1_HAVE_URING 1
#endif
#endif

#ifdef CN1_HAVE_URING
#include <linux/io_uring.h>
#include <sys/mman.h>
#include <sys/syscall.h>
#include <stdio.h>
#if !defined(__NR_io_uring_setup) || !defined(__NR_io_uring_enter) || !defined(__NR_io_uring_register)
#undef CN1_HAVE_URING
#endif
#endif

#ifdef CN1_HAVE_URING

#define CN1_URING_ENTRIES 64
/* The safety net if a wakeup write is ever lost: the parked thread looks at the
   completion queue itself this often. Normally the write wakes it at once. */
#define CN1_URING_RECHECK_MILLIS 1000

struct cn1Uring {
    int fd;
    unsigned sqEntries;
    unsigned* sqHead;
    unsigned* sqTail;
    unsigned sqMask;
    unsigned* sqArray;
    struct io_uring_sqe* sqes;
    unsigned* cqHead;
    unsigned* cqTail;
    unsigned cqMask;
    struct io_uring_cqe* cqes;
    void* ringBase;
    size_t ringSize;
    size_t sqesSize;
    int inflight;                   /* operations submitted and not yet seen done */
    unsigned char supported[256];
};

/* On the submitting virtual thread's stack until `done`. */
struct cn1UringJob {
    long long res;
    int done;
};

static __thread struct cn1Uring* cn1UringHost = 0;
static __thread int cn1UringHostTried = 0;
static int cn1UringAnnounced = 0;
static const uint64_t cn1UringOne = 1;

static void cn1UringAnnounce(int err) {
    if(__atomic_exchange_n(&cn1UringAnnounced, 1, __ATOMIC_ACQ_REL)) {
        return;
    }
    if(err == 0) {
        fprintf(stderr, "local file I/O: io_uring\n");
    } else {
        fprintf(stderr, "local file I/O: thread pool (io_uring unavailable: %s)\n", strerror(err));
    }
}

static struct cn1Uring* cn1UringSetup(int* err) {
    struct io_uring_params params;
    struct io_uring_probe* probe;
    struct cn1Uring* u;
    size_t sqSize;
    size_t cqSize;
    size_t ringSize;
    char* ring;
    void* sqes;
    int fd;
    unsigned iter;
    memset(&params, 0, sizeof(params));
    fd = (int)syscall(__NR_io_uring_setup, CN1_URING_ENTRIES, &params);
    if(fd < 0) {
        *err = errno;
        return 0;
    }
    /* 5.6 and later: one mapping for both rings, no dropped completions, and an
       offset of -1 meaning "the file position" -- which read() and write() need. */
    if((params.features & IORING_FEAT_SINGLE_MMAP) == 0
            || (params.features & IORING_FEAT_NODROP) == 0
            || (params.features & IORING_FEAT_RW_CUR_POS) == 0) {
        close(fd);
        *err = ENOSYS;
        return 0;
    }
    sqSize = params.sq_off.array + params.sq_entries * sizeof(unsigned);
    cqSize = params.cq_off.cqes + params.cq_entries * sizeof(struct io_uring_cqe);
    ringSize = sqSize > cqSize ? sqSize : cqSize;
    ring = (char*)mmap(0, ringSize, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_POPULATE,
            fd, IORING_OFF_SQ_RING);
    if(ring == (char*)MAP_FAILED) {
        *err = errno;
        close(fd);
        return 0;
    }
    sqes = mmap(0, params.sq_entries * sizeof(struct io_uring_sqe), PROT_READ | PROT_WRITE,
            MAP_SHARED | MAP_POPULATE, fd, IORING_OFF_SQES);
    if(sqes == MAP_FAILED) {
        *err = errno;
        munmap(ring, ringSize);
        close(fd);
        return 0;
    }
    u = (struct cn1Uring*)calloc(1, sizeof(struct cn1Uring));
    probe = (struct io_uring_probe*)calloc(1, sizeof(struct io_uring_probe)
            + 256 * sizeof(struct io_uring_probe_op));
    if(u == 0 || probe == 0
            || syscall(__NR_io_uring_register, fd, IORING_REGISTER_PROBE, probe, 256) < 0) {
        *err = (u == 0 || probe == 0) ? ENOMEM : errno;
        free(u);
        free(probe);
        munmap(sqes, params.sq_entries * sizeof(struct io_uring_sqe));
        munmap(ring, ringSize);
        close(fd);
        return 0;
    }
    for(iter = 0 ; iter < probe->ops_len && iter < 256 ; iter++) {
        if(probe->ops[iter].flags & IO_URING_OP_SUPPORTED) {
            u->supported[probe->ops[iter].op] = 1;
        }
    }
    free(probe);
    if(!u->supported[IORING_OP_READ] || !u->supported[IORING_OP_WRITE]) {
        /* WRITE is also the wakeup; without it nothing here can work. */
        free(u);
        munmap(sqes, params.sq_entries * sizeof(struct io_uring_sqe));
        munmap(ring, ringSize);
        close(fd);
        *err = ENOSYS;
        return 0;
    }
    u->fd = fd;
    u->ringBase = ring;
    u->ringSize = ringSize;
    u->sqesSize = params.sq_entries * sizeof(struct io_uring_sqe);
    u->sqEntries = params.sq_entries;
    u->sqHead = (unsigned*)(ring + params.sq_off.head);
    u->sqTail = (unsigned*)(ring + params.sq_off.tail);
    u->sqMask = *(unsigned*)(ring + params.sq_off.ring_mask);
    u->sqArray = (unsigned*)(ring + params.sq_off.array);
    u->sqes = (struct io_uring_sqe*)sqes;
    u->cqHead = (unsigned*)(ring + params.cq_off.head);
    u->cqTail = (unsigned*)(ring + params.cq_off.tail);
    u->cqMask = *(unsigned*)(ring + params.cq_off.ring_mask);
    u->cqes = (struct io_uring_cqe*)(ring + params.cq_off.cqes);
    *err = 0;
    return u;
}

static struct cn1Uring* cn1UringForHost(void) {
    if(!cn1UringHostTried) {
        int err = 0;
        cn1UringHostTried = 1;
        cn1UringHost = cn1UringSetup(&err);
        if(cn1UringHost != 0) {
            cn1OffloadHostRegister();
        }
        cn1UringAnnounce(cn1UringHost == 0 ? (err == 0 ? ENOSYS : err) : 0);
    }
    return cn1UringHost;
}

/* Hands every finished operation its result. Host thread only. */
static void cn1UringReap(struct cn1Uring* u) {
    unsigned head = *u->cqHead;
    unsigned tail = __atomic_load_n(u->cqTail, __ATOMIC_ACQUIRE);
    while(head != tail) {
        struct io_uring_cqe* cqe = &u->cqes[head & u->cqMask];
        if((cqe->user_data & 1) != 0) {
            /* The wakeup write finished: its signal may be closed at teardown. */
            struct cn1OffloadSignal* sig =
                    (struct cn1OffloadSignal*)(uintptr_t)(cqe->user_data & ~(uint64_t)1);
            __atomic_store_n(&sig->busy, 0, __ATOMIC_RELEASE);
        } else if(cqe->user_data != 0) {
            struct cn1UringJob* job = (struct cn1UringJob*)(uintptr_t)cqe->user_data;
            job->res = cqe->res;
            job->done = 1;
        }
        head++;
    }
    __atomic_store_n(u->cqHead, head, __ATOMIC_RELEASE);
}

/*
 * Runs `op` on this host's ring and parks until it completes. 1 with the
 * kernel's answer in *result (a count or descriptor, or -errno); 0 when the
 * ring did not run it, and the caller must use its pool job.
 */
static int cn1UringRun(struct io_uring_sqe* op, long long* result) {
    struct cn1Uring* u;
    struct cn1OffloadSignal* signal;
    struct cn1UringJob job;
    struct io_uring_sqe* sqe;
    unsigned head;
    unsigned tail;
    int events = CN1_OFFLOAD_EVENT_READ;
    int entered;
    if(!cn1BackendCanPark()) {
        return 0;
    }
    u = cn1UringForHost();
    if(u == 0 || !u->supported[op->opcode]) {
        return 0;
    }
    signal = cn1OffloadTakeSignal();
    if(signal == 0) {
        return 0;
    }
    head = __atomic_load_n(u->sqHead, __ATOMIC_ACQUIRE);
    tail = *u->sqTail;
    if(tail - head + 2 > u->sqEntries) {
        cn1OffloadReturnSignal(signal);
        return 0;
    }
    job.res = 0;
    job.done = 0;

    sqe = &u->sqes[tail & u->sqMask];
    *sqe = *op;
    sqe->flags |= IOSQE_IO_HARDLINK;
    sqe->user_data = (uint64_t)(uintptr_t)&job;
    u->sqArray[tail & u->sqMask] = tail & u->sqMask;

    sqe = &u->sqes[(tail + 1) & u->sqMask];
    memset(sqe, 0, sizeof(*sqe));
    sqe->opcode = IORING_OP_WRITE;
    sqe->fd = signal->writeFd;
    sqe->addr = (uint64_t)(uintptr_t)&cn1UringOne;
    sqe->len = sizeof(cn1UringOne);
    /* Tagged with the low bit (a malloc'd signal is aligned) so the reap can
       tell it from a job and clear the signal's busy flag. */
    sqe->user_data = (uint64_t)(uintptr_t)signal | 1;
    u->sqArray[(tail + 1) & u->sqMask] = (tail + 1) & u->sqMask;
    __atomic_store_n(&signal->busy, 1, __ATOMIC_RELAXED);

    __atomic_store_n(u->sqTail, tail + 2, __ATOMIC_RELEASE);
    /* Counted before the kernel can touch this stack; see VirtualThread freeImpl. */
    cn1BackendVtOffloading(1);
    entered = (int)syscall(__NR_io_uring_enter, u->fd, 2, 0, 0, NULL, 0);
    if(entered < 0 && errno == EBUSY) {
        /* Completions backed up (NODROP keeps them): make room and go again. */
        cn1UringReap(u);
        entered = (int)syscall(__NR_io_uring_enter, u->fd, 2, 0, 0, NULL, 0);
    }
    if(entered < 0 && __atomic_load_n(u->sqHead, __ATOMIC_ACQUIRE) == head) {
        /* The kernel took nothing, so nothing references the job: withdraw it. */
        __atomic_store_n(u->sqTail, tail, __ATOMIC_RELEASE);
        __atomic_store_n(&signal->busy, 0, __ATOMIC_RELAXED);
        cn1BackendVtOffloading(-1);
        cn1OffloadReturnSignal(signal);
        return 0;
    }
    u->inflight++;
    cn1UringReap(u);
    while(!job.done) {
        if(!cn1BackendVtWait(1, &signal->readFd, &events, CN1_URING_RECHECK_MILLIS)) {
            struct pollfd p;
            p.fd = signal->readFd;
            p.events = POLLIN;
            p.revents = 0;
            poll(&p, 1, 10);
        }
        cn1OffloadDrain(signal->readFd);
        cn1UringReap(u);
    }
    u->inflight--;
    cn1BackendVtOffloading(-1);
    /* The wakeup write may still be on its way: a late byte in a pooled
       descriptor is a spurious wakeup the next user already tolerates, and the
       signal stays busy until its completion is reaped. */
    cn1OffloadReturnSignal(signal);
    if(job.res == -EINTR || job.res == -EAGAIN) {
        /* Nothing was transferred; let the pool's retrying call answer. */
        return 0;
    }
    *result = job.res;
    return 1;
}

int cn1BackendUringRead(int fd, void* buf, size_t len, long long offset, long long* result) {
    struct io_uring_sqe op;
    if(len > 0x7fffffffU) {
        return 0;
    }
    memset(&op, 0, sizeof(op));
    op.opcode = IORING_OP_READ;
    op.fd = fd;
    op.addr = (uint64_t)(uintptr_t)buf;
    op.len = (unsigned)len;
    op.off = offset < 0 ? (uint64_t)-1 : (uint64_t)offset;
    return cn1UringRun(&op, result);
}

int cn1BackendUringWrite(int fd, const void* buf, size_t len, long long* result) {
    struct io_uring_sqe op;
    if(len > 0x7fffffffU) {
        return 0;
    }
    memset(&op, 0, sizeof(op));
    op.opcode = IORING_OP_WRITE;
    op.fd = fd;
    op.addr = (uint64_t)(uintptr_t)buf;
    op.len = (unsigned)len;
    op.off = (uint64_t)-1;
    return cn1UringRun(&op, result);
}

int cn1BackendUringOpenat(int dirFd, const char* path, int flags, long long* result) {
    struct io_uring_sqe op;
    memset(&op, 0, sizeof(op));
    op.opcode = IORING_OP_OPENAT;
    op.fd = dirFd;
    op.addr = (uint64_t)(uintptr_t)path;
    op.open_flags = (unsigned)flags;
    return cn1UringRun(&op, result);
}

int cn1BackendUringOpenat2(int dirFd, const char* path, void* how, size_t howSize, long long* result) {
    struct io_uring_sqe op;
    memset(&op, 0, sizeof(op));
    op.opcode = IORING_OP_OPENAT2;
    op.fd = dirFd;
    op.addr = (uint64_t)(uintptr_t)path;
    op.len = (unsigned)howSize;
    op.off = (uint64_t)(uintptr_t)how;
    return cn1UringRun(&op, result);
}

static void cn1UringHostExit(void) {
    struct cn1Uring* u = cn1UringHost;
    if(u == 0) {
        return;
    }
    cn1UringReap(u);
    if(u->inflight != 0) {
        /* An operation may still write into a parked thread's stack. */
        return;
    }
    cn1UringHost = 0;
    munmap(u->sqes, u->sqesSize);
    munmap(u->ringBase, u->ringSize);
    close(u->fd);
    free(u);
}

#else /* !CN1_HAVE_URING */

static void cn1UringHostExit(void) {
}

int cn1BackendUringRead(int fd, void* buf, size_t len, long long offset, long long* result) {
    (void)fd; (void)buf; (void)len; (void)offset; (void)result;
    return 0;
}
int cn1BackendUringWrite(int fd, const void* buf, size_t len, long long* result) {
    (void)fd; (void)buf; (void)len; (void)result;
    return 0;
}
int cn1BackendUringOpenat(int dirFd, const char* path, int flags, long long* result) {
    (void)dirFd; (void)path; (void)flags; (void)result;
    return 0;
}
int cn1BackendUringOpenat2(int dirFd, const char* path, void* how, size_t howSize, long long* result) {
    (void)dirFd; (void)path; (void)how; (void)howSize; (void)result;
    return 0;
}

#endif /* CN1_HAVE_URING */

#endif
