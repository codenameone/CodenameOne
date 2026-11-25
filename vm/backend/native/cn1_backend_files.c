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
 * Static file serving, on the kernel's zero-copy path where there is one.
 *
 * sendfile() moves bytes from a file descriptor to a socket inside the kernel:
 * no read into a user buffer, no write back out, and on Linux no copy at all for
 * the page-cache pages. For a file server that is the difference between two
 * copies per byte and none, and it is why this is worth a native rather than a
 * read/write loop in Java.
 *
 * The signature differs between Linux and the BSDs -- Linux returns the count and
 * advances an offset pointer, macOS takes the length by reference and reports how
 * much it moved -- so both are wrapped behind one call that always returns bytes
 * sent.
 *
 * There is deliberately no sendfile path for TLS: the whole point is that the
 * kernel copies bytes it does not have to look at, and encrypted bytes have to be
 * produced in user space. The Java side falls back to read + SSL_write there, and
 * says so.
 */
#include "cn1_globals.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>   /* PATH_MAX, for the component walk below */
#include <errno.h>
#include <poll.h>
#include <sys/socket.h>

#ifndef _WIN32
#include <unistd.h>
#include <fcntl.h>
#if defined(__linux__)
#include <sys/syscall.h>
#include <errno.h>
/* From linux/openat2.h. Defined here so the build does not require a kernel header
   that older distributions ship without. */
#define CN1_RESOLVE_BENEATH 0x08
#endif
#include <sys/types.h>
#include <sys/stat.h>
#endif

#if defined(__linux__)
#include <sys/sendfile.h>
#define CN1_HAVE_SENDFILE 1
#elif defined(__APPLE__) || defined(__FreeBSD__)
#include <sys/socket.h>
#include <sys/uio.h>
#define CN1_HAVE_SENDFILE 1
#endif

/*
 * Every call here that can wait on the DISK -- open, read, write, pread, mkstemp,
 * realpath, and sendfile's read of the file -- goes through cn1BackendOffload
 * (cn1_backend_offload.c). A regular file is always "ready" to the poller, so the
 * call cannot park the way a socket wait does; on a virtual thread the pool runs
 * it while the caller is parked, and the host keeps serving everything else.
 *
 * A job never touches the heap: strings are converted before the offload, a
 * byte[]'s data pointer is taken before it (the caller's frame keeps the array
 * alive and the collector does not move it), and Java results are built after.
 * errno is per pthread, so a job that needs it classifies it itself.
 */
typedef void (*cn1OffloadFn)(void* arg);
void cn1BackendOffload(cn1OffloadFn fn, void* arg);
/* io_uring on Linux (cn1_backend_offload.c): 1 with the kernel's answer (a count
   or descriptor, or -errno), or 0 -- not run, use the pool job. */
int cn1BackendUringRead(int fd, void* buf, size_t len, long long offset, long long* result);
int cn1BackendUringWrite(int fd, const void* buf, size_t len, long long* result);
int cn1BackendUringOpenat(int dirFd, const char* path, int flags, long long* result);
int cn1BackendUringOpenat2(int dirFd, const char* path, void* how, size_t howSize, long long* result);

#ifndef _WIN32
struct cn1FileJob {
    int fd;
    int fd2;
    const char* path;
    const char* path2;
    char* out;
    JAVA_ARRAY_BYTE* data;
    size_t length;
    long long position;
    long long result;
};
#endif

/* Opens for reading. Returns the descriptor, or -1. */
/*
 * Opens a path under `root` and refuses anything that resolves outside it, in one
 * syscall that the filesystem cannot race.
 *
 * open-then-realPath cannot do this. The check runs against a SECOND lookup, so a
 * symlink under a writable document root can point outside for the open and inside
 * for the check, and the descriptor that gets served is the outside file. Comparing
 * st_dev/st_ino afterwards narrows that window without closing it, because the
 * second lookup is racy in the same way.
 *
 * RESOLVE_BENEATH makes the kernel refuse the escape during resolution instead, so
 * there is no window to lose. Returns -2 where the kernel or platform has no
 * openat2 -- the caller falls back to the older check rather than serving nothing.
 */
#if defined(__linux__) && defined(SYS_openat2)
static void cn1ClearNonBlocking(int fd); /* see cn1OpenReadPath */

static void cn1OpenBeneathJob(void* p) {
    struct cn1FileJob* j = (struct cn1FileJob*)p;
    const char* rel = j->path2;
    int dirFd;
    int fd;
    struct cn1_open_how {
        uint64_t flags;
        uint64_t mode;
        uint64_t resolve;
    } how;
    j->result = -1;
    dirFd = open(j->path, O_RDONLY | O_DIRECTORY | O_CLOEXEC);
    if(dirFd < 0) {
        return;
    }
    /* RESOLVE_BENEATH rejects an absolute path outright, and the caller's target
       always starts at the root. */
    while(*rel == '/') {
        rel++;
    }
    if(*rel == 0) {
        close(dirFd);
        return;
    }
    memset(&how, 0, sizeof(how));
    how.flags = (uint64_t)(O_RDONLY | O_CLOEXEC | O_NONBLOCK);
    how.resolve = (uint64_t)CN1_RESOLVE_BENEATH;
    fd = (int)syscall(SYS_openat2, dirFd, rel, &how, sizeof(how));
    close(dirFd);
    if(fd >= 0) {
        cn1ClearNonBlocking(fd);
    }
    if(fd < 0) {
        /* An old kernel knows the number but not the struct, or does not know the
           call at all. Either way this cannot answer, so say so rather than let the
           caller read a refusal as "file missing". */
        if(errno == ENOSYS || errno == EINVAL || errno == E2BIG) {
            j->result = -2;
        }
        return;
    }
    j->result = fd;
}

/*
 * The same open through io_uring: the directory, then openat2 beneath it, each
 * an operation on the ring. 1 when it answered; 0 when the ring did not run a
 * step, and the pool job (which starts over) has to.
 */
static int cn1OpenBeneathUring(struct cn1FileJob* j) {
    const char* rel = j->path2;
    long long dirFd;
    long long fd;
    struct cn1_open_how {
        uint64_t flags;
        uint64_t mode;
        uint64_t resolve;
    } how;
    if(!cn1BackendUringOpenat(AT_FDCWD, j->path, O_RDONLY | O_DIRECTORY | O_CLOEXEC, &dirFd)) {
        return 0;
    }
    j->result = -1;
    if(dirFd < 0) {
        return 1;
    }
    while(*rel == '/') {
        rel++;
    }
    if(*rel == 0) {
        close((int)dirFd);
        return 1;
    }
    memset(&how, 0, sizeof(how));
    how.flags = (uint64_t)(O_RDONLY | O_CLOEXEC | O_NONBLOCK);
    how.resolve = (uint64_t)CN1_RESOLVE_BENEATH;
    if(!cn1BackendUringOpenat2((int)dirFd, rel, &how, sizeof(how), &fd)) {
        close((int)dirFd);
        return 0;
    }
    close((int)dirFd);
    if(fd >= 0) {
        cn1ClearNonBlocking((int)fd);
    }
    if(fd < 0) {
        if(fd == -ENOSYS || fd == -EINVAL || fd == -E2BIG) {
            j->result = -2;
        }
        return 1;
    }
    j->result = fd;
    return 1;
}
#endif

JAVA_INT com_codename1_backend_FileIo_openBeneathImpl___java_lang_String_java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT root, JAVA_OBJECT relative) {
#if defined(__linux__) && defined(SYS_openat2)
    const char* rootPath;
    const char* rel;
    if(root == JAVA_NULL || relative == JAVA_NULL) {
        return -1;
    }
    rootPath = stringToUTF8(threadStateData, root);
    if(rootPath == NULL) {
        return -1;
    }
    /* Copied: stringToUTF8 reuses one buffer per thread, so the second call
       would overwrite the first. */
    {
        char rootCopy[PATH_MAX];
        struct cn1FileJob job;
        size_t rootLen = strlen(rootPath);
        if(rootLen >= sizeof(rootCopy)) {
            return -1;
        }
        memcpy(rootCopy, rootPath, rootLen + 1);
        rel = stringToUTF8(threadStateData, relative);
        if(rel == NULL) {
            return -1;
        }
        memset(&job, 0, sizeof(job));
        job.path = rootCopy;
        job.path2 = rel;
        CN1_YIELD_THREAD;
        if(!cn1OpenBeneathUring(&job)) {
            cn1BackendOffload(cn1OpenBeneathJob, &job);
        }
        CN1_RESUME_THREAD;
        return (JAVA_INT)job.result;
    }
#else
    (void)root; (void)relative;
    return -2;
#endif
}

/*
 * Whether any component of `path` is a symlink whose target is missing.
 *
 * EVERY COMPONENT, not just the last one. lstat on the full path asks about the
 * entry the path names and nothing above it, so cn1.config.location=/config/current
 * with current -> missing-release opened /config/current/application.properties,
 * got ENOENT for a component in the MIDDLE, and answered "absent" -- the same
 * silent discard of every file-based setting, TLS paths included, that the
 * final-component check was added to stop. A release directory swung by symlink is
 * how most deployments roll forward, so the broken middle is the likelier half.
 *
 * Walked from the root down: a component that lstats but does not stat is a link
 * pointing at nothing. A path that simply names nothing has no such component.
 */
static int cn1BackendDanglingLinkIn(const char* path) {
    char buffer[PATH_MAX];
    size_t len = strlen(path);
    size_t iter;
    if(len == 0 || len >= sizeof(buffer)) {
        return 0;
    }
    memcpy(buffer, path, len + 1);
    for(iter = 1 ; iter <= len ; iter++) {
        if(buffer[iter] != '/' && buffer[iter] != 0) {
            continue;
        }
        char saved = buffer[iter];
        struct stat linkInfo;
        struct stat targetInfo;
        buffer[iter] = 0;
        if(lstat(buffer, &linkInfo) == 0 && stat(buffer, &targetInfo) != 0) {
            return 1;
        }
        buffer[iter] = saved;
    }
    return 0;
}

#ifndef _WIN32
/*
 * NO OPEN HERE WAITS FOR A FIFO'S WRITER. A plain O_RDONLY open of a FIFO
 * blocks until something opens the other end, so a FIFO in a static root held a
 * request -- and, on the I/O pool, a pool thread -- for as long as nobody wrote
 * to it: four such requests stopped every disk call in the process. io_uring
 * does not block there at all (it tries the open non-blocking first), so the two
 * paths also disagreed. Every open is O_NONBLOCK, which a regular file ignores,
 * and the flag is cleared again at once so reads behave exactly as before. A
 * pipe handed over as a file -- a process substitution, which already has its
 * writer -- still reads normally; StaticFiles refuses anything that is not a
 * regular file (FileIo.stat).
 */
static void cn1ClearNonBlocking(int fd) {
    int flags = fcntl(fd, F_GETFL);
    if(flags >= 0 && (flags & O_NONBLOCK) != 0) {
        fcntl(fd, F_SETFL, flags & ~O_NONBLOCK);
    }
}

/* The open and its classification, on whichever thread cn1BackendOffload picks. */
static int cn1OpenReadPath(const char* p) {
    int fd = open(p, O_RDONLY | O_CLOEXEC | O_NONBLOCK);
    if(fd >= 0) {
        cn1ClearNonBlocking(fd);
    }
    if(fd < 0) {
        /*
         * -1 means THERE IS NO SUCH FILE and -2 means there is one and it could
         * not be opened. Collapsing both into -1 made a configuration file that
         * exists but cannot be read indistinguishable from an absent optional
         * one, so a server whose TLS certificate and key are named in
         * application.properties started in plaintext when the file's
         * permissions were wrong.
         *
         * ONLY ENOENT is absent. ENOTDIR -- a path component that is not a
         * directory -- reads like "names nothing", and classifying it that way
         * made the two arms disagree: the JVM's FileChannel.open throws a plain
         * FileSystemException there rather than NoSuchFileException, so it
         * already answered -2. Measured, not assumed, and -2 is the better of
         * the two answers anyway: a configuration path of the shape
         * /etc/hosts/application.properties is a broken deployment, not an
         * optional file somebody chose not to write.
         */
        if(errno == ENOENT) {
            /*
             * A DANGLING SYMLINK IS NOT AN ABSENT FILE. open() follows the link
             * and reports ENOENT for the missing TARGET, which is the same errno
             * a path that names nothing gives -- so a deployment whose
             * application.properties is a symlink into a volume that failed to
             * mount read as "no configuration", every file-based setting fell
             * back to an environment default, and a server naming its TLS
             * certificate and key in that file came up in PLAINTEXT. That is the
             * failure this whole split exists to prevent, arriving through the
             * one errno the split treats as benign.
             *
             * lstat does not follow the link, so an entry that is there answers
             * 0 while a path that names nothing answers -1. The link itself is
             * present, so this is the "there is one and it could not be opened"
             * case, which nobody may quietly ignore.
             */
            if(cn1BackendDanglingLinkIn(p)) {
                return -2;
            }
            return -1;
        }
        return -2;
    }
    return fd;
}

static void cn1OpenReadJob(void* arg) {
    struct cn1FileJob* j = (struct cn1FileJob*)arg;
    j->result = cn1OpenReadPath(j->path);
}
#endif

JAVA_INT com_codename1_backend_FileIo_openReadImpl___java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
#ifdef _WIN32
    (void)path;
    return -1;
#else
    struct cn1FileJob job;
    const char* p = path == JAVA_NULL ? NULL : stringToUTF8(threadStateData, path);
    if(p == NULL) {
        return -1;
    }
    memset(&job, 0, sizeof(job));
    job.path = p;
    CN1_YIELD_THREAD;
    cn1BackendOffload(cn1OpenReadJob, &job);
    CN1_RESUME_THREAD;
    return (JAVA_INT)job.result;
#endif
}

/*
 * Fills out[0]=size, out[1]=modified-time-millis, out[2]=1 when it is a directory
 * and 2 when it is neither a directory nor a regular file.
 * One call rather than three so a request costs one stat, and taken from the OPEN
 * DESCRIPTOR rather than the path: stat-then-open lets the file change underneath
 * between the two, which is how a length header ends up disagreeing with the body.
 */
JAVA_INT com_codename1_backend_FileIo_statImpl___int_long_1ARRAY_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_OBJECT out) {
#ifdef _WIN32
    (void)fd; (void)out;
    return -1;
#else
    struct stat st;
    JAVA_ARRAY_LONG* data;
    if(fd < 0 || out == JAVA_NULL || ((JAVA_ARRAY)out)->length < 3) {
        return -1;
    }
    if(fstat(fd, &st) != 0) {
        return -1;
    }
    data = (JAVA_ARRAY_LONG*)CN1_ARRAY_DATA(out);
    data[0] = (JAVA_LONG)st.st_size;
    /* Milliseconds, not whole seconds. StaticFiles builds its ETag from size and
       this, so at one-second resolution a file replaced by different content of
       the SAME size within the same second kept both halves of its validator and
       every client holding the old ETag got a 304 for as long as it asked. The
       Last-Modified header formats from the same value and is unaffected: HTTP
       dates are whole seconds, so the extra precision is simply dropped there. */
#if defined(__APPLE__)
    data[1] = (JAVA_LONG)st.st_mtimespec.tv_sec * 1000LL
            + (JAVA_LONG)(st.st_mtimespec.tv_nsec / 1000000L);
#elif defined(st_mtime)
    /* POSIX.1-2008 defines st_mtime as a macro exactly when st_mtim exists. */
    data[1] = (JAVA_LONG)st.st_mtim.tv_sec * 1000LL
            + (JAVA_LONG)(st.st_mtim.tv_nsec / 1000000L);
#else
    data[1] = (JAVA_LONG)st.st_mtime * 1000LL;
#endif
    /* 2: neither -- a FIFO, socket or device, which nothing serves as a file. */
    data[2] = S_ISDIR(st.st_mode) ? 1 : (S_ISREG(st.st_mode) ? 0 : 2);
    if(((JAVA_ARRAY)out)->length > 3) {
        /* THE INODE, as a third component of the ETag. Size and mtime alone name
           the representation only as well as the filesystem's timestamps allow:
           a deployment that copies files with their timestamps preserved -- cp -p,
           tar -p, rsync -t -- reproduces the same millisecond, and if the new
           content happens to be the same length the validator does not change and
           every client holding the old one is told 304 indefinitely. Such a copy
           creates a NEW file, so the inode is exactly what differs in that case.
           An in-place rewrite that restores the timestamp and keeps the length is
           still invisible; closing that needs the content itself, which means
           reading every byte of every response on a path built to avoid reading
           any of them. */
        data[3] = (JAVA_LONG)st.st_ino;
    }
    return 0;
#endif
}

/*
 * Sends count bytes of inFd starting at offset straight to the socket. Returns how
 * many moved, which may be fewer than asked -- the caller loops. -1 on error.
 */
/*
 * Waits for the output socket to drain, bounded by its own send deadline.
 *
 * A copy of the one in cn1_backend_server.c rather than a shared symbol: it is
 * fifteen lines and the two files are compiled independently. See that copy for
 * why this waits instead of parking the virtual thread.
 *
 * Returns 1 when writable, 0 on timeout, -1 on error.
 */
static int cn1AwaitSocketWritable(int fd) {
    struct pollfd waiting;
    struct timeval tv;
    socklen_t len = (socklen_t)sizeof(tv);
    int timeout = -1;
    int rc;
    if(getsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, (char*)&tv, &len) == 0) {
        long millis = (long)tv.tv_sec * 1000L + (long)(tv.tv_usec / 1000);
        if(millis > 0) {
            timeout = (int)millis;
        }
    }
    waiting.fd = fd;
    waiting.events = POLLOUT;
    waiting.revents = 0;
    do {
        rc = poll(&waiting, 1, timeout);
    } while(rc < 0 && errno == EINTR);
    if(rc < 0) {
        return -1;
    }
    return rc == 0 ? 0 : 1;
}

#ifdef CN1_HAVE_SENDFILE
/* One sendfile() call, on whichever thread cn1BackendOffload picks. */
struct cn1SendFileJob {
    int outFd;
    int inFd;
    off_t off;              /* Linux: in-out offset */
    off_t len;              /* BSD: bytes moved */
    long long count;
    long long result;
    int err;
};

static void cn1SendFileJobRun(void* arg) {
    struct cn1SendFileJob* j = (struct cn1SendFileJob*)arg;
#if defined(__linux__)
    ssize_t n = sendfile(j->outFd, j->inFd, &j->off, (size_t)j->count);
    j->err = n < 0 ? errno : 0;
    j->result = (long long)n;
#else
    int rc;
    j->len = (off_t)j->count;
    rc = sendfile(j->inFd, j->outFd, j->off, &j->len, NULL, 0);
    j->err = rc < 0 ? errno : 0;
    j->result = rc;
#endif
}
#endif

JAVA_LONG com_codename1_backend_FileIo_sendFileImpl___int_int_long_long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT outFd, JAVA_INT inFd, JAVA_LONG offset, JAVA_LONG count) {
#if defined(CN1_HAVE_SENDFILE) && defined(__linux__)
    off_t off = (off_t)offset;
    ssize_t n;
    if(outFd < 0 || inFd < 0) {
        return -1;
    }
    CN1_YIELD_THREAD;
    for(;;) {
        /* Offloaded: the socket is non-blocking in virtual-thread mode, but the
           FILE side reads from disk for every uncached page. errno comes back in
           the job, since the pool thread's errno is not this one's. */
        struct cn1SendFileJob job;
        int sendErrno;
        job.outFd = outFd;
        job.inFd = inFd;
        job.off = off;
        job.count = (long long)count;
        cn1BackendOffload(cn1SendFileJobRun, &job);
        n = (ssize_t)job.result;
        off = job.off;
        sendErrno = job.err;
        if(n >= 0) {
            break;
        }
        if(sendErrno == EINTR) {
            continue;
        }
        if(sendErrno == EAGAIN || sendErrno == EWOULDBLOCK) {
            /* The client's window is full, not an error. The descriptor is
               non-blocking in virtual-thread mode, so this is the ordinary way a
               large download to a slow client proceeds -- and reporting it as -1
               made StaticFiles close the connection and truncate the file. */
            int ready = cn1AwaitSocketWritable(outFd);
            if(ready > 0) {
                continue;
            }
            n = ready == 0 ? -3 : -1;
            break;
        }
        n = -1;
        break;
    }
    CN1_RESUME_THREAD;
    return (JAVA_LONG)n;
#elif defined(CN1_HAVE_SENDFILE)
    /* macOS/FreeBSD: len is in-out -- asked for on the way in, moved on the way
       out -- and a partial send reports success with a smaller len, so a short
       write is not an error here. */
    off_t len = (off_t)count;
    int rc;
    int sendErrno;
    if(outFd < 0 || inFd < 0) {
        return -1;
    }
    CN1_YIELD_THREAD;
    for(;;) {
        len = (off_t)count;
        do {
            /* RESET EVERY ATTEMPT, and only retried when nothing moved. `len` is
               in-out: the interrupted call leaves the bytes it DID send in it, so
               retrying from the same offset sent that prefix twice -- a static
               body corrupted in the middle and a keep-alive connection misframed
               against the Content-Length that went out ahead of it. It also asked
               for the wrong length the second time, since the count it was given
               had been overwritten by the count it had sent. An interrupted call
               that moved bytes is progress, and the caller advances on it: the
               test below breaks out and the tail returns it. */
            /* Offloaded: the FILE side reads from disk for every uncached page.
               errno comes back in the job, since the pool thread's is not ours. */
            struct cn1SendFileJob job;
            job.outFd = outFd;
            job.inFd = inFd;
            job.off = (off_t)offset;
            job.count = (long long)count;
            cn1BackendOffload(cn1SendFileJobRun, &job);
            rc = (int)job.result;
            len = job.len;
            sendErrno = job.err;
        } while(rc < 0 && sendErrno == EINTR && len == 0);
        /* Anything except "the buffer was full and nothing moved" is an answer:
           success, a real error, or a partial send the caller can advance on. */
        if(rc >= 0 || sendErrno != EAGAIN || len > 0) {
            break;
        }
        /* EAGAIN having moved nothing is backpressure, and returning the 0 in
           len made sendBody() read "no progress" as "the peer is gone" and drop
           a large file mid-transfer for any client reading slower than the
           server writes. That is the same truncation the Linux branch above
           fixes; this twin kept it. Wait for the socket the same way. */
        if(cn1AwaitSocketWritable(outFd) <= 0) {
            CN1_RESUME_THREAD;
            return -1;
        }
    }
    CN1_RESUME_THREAD;
    if(rc < 0 && sendErrno != EAGAIN) {
        return len > 0 ? (JAVA_LONG)len : -1;
    }
    return (JAVA_LONG)len;
#else
    (void)outFd; (void)inFd; (void)offset; (void)count;
    return -1;
#endif
}

JAVA_BOOLEAN com_codename1_backend_FileIo_hasSendFileImpl___R_boolean(CODENAME_ONE_THREAD_STATE) {
#ifdef CN1_HAVE_SENDFILE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

#ifndef _WIN32
static void cn1ReadJob(void* arg) {
    struct cn1FileJob* j = (struct cn1FileJob*)arg;
    ssize_t n;
    do {
        n = read(j->fd, j->data, j->length);
    } while(n < 0 && errno == EINTR);
    j->result = n < 0 ? -1 : (long long)n;
}

static void cn1PreadJob(void* arg) {
    struct cn1FileJob* j = (struct cn1FileJob*)arg;
    ssize_t n;
    do {
        n = pread(j->fd, j->data, j->length, (off_t)j->position);
    } while(n < 0 && errno == EINTR);
    j->result = n < 0 ? -1 : (long long)n;
}

/* All of the slice or -1. */
static void cn1WriteJob(void* arg) {
    struct cn1FileJob* j = (struct cn1FileJob*)arg;
    size_t done = 0;
    while(done < j->length) {
        ssize_t n = write(j->fd, j->data + done, j->length - done);
        if(n < 0 && errno == EINTR) {
            continue;
        }
        if(n <= 0) {
            j->result = -1;
            return;
        }
        done += (size_t)n;
    }
    j->result = (long long)done;
}

static void cn1RealPathJob(void* arg) {
    struct cn1FileJob* j = (struct cn1FileJob*)arg;
    j->result = realpath(j->path, j->out) == NULL ? -1 : 0;
}

/* `out` holds the mkstemp template and receives the name, which is unlinked at once. */
static void cn1CreateTempJob(void* arg) {
    struct cn1FileJob* j = (struct cn1FileJob*)arg;
    int fd = mkstemp(j->out);
    if(fd < 0) {
        j->result = -1;
        return;
    }
    unlink(j->out);
    fcntl(fd, F_SETFD, FD_CLOEXEC);
    j->result = fd;
}
#endif

/* Plain read, for the TLS path and for platforms with no sendfile. */
JAVA_INT com_codename1_backend_FileIo_readImpl___int_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
#ifdef _WIN32
    (void)fd; (void)buffer; (void)offset; (void)length;
    return -1;
#else
    JAVA_ARRAY_BYTE* data;
    if(fd < 0 || buffer == JAVA_NULL) {
        return -1;
    }
    struct cn1FileJob job;
    data = (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(buffer);
    memset(&job, 0, sizeof(job));
    job.fd = fd;
    job.data = &data[offset];
    job.length = (size_t)length;
    CN1_YIELD_THREAD;
    if(cn1BackendUringRead(fd, job.data, job.length, -1, &job.result)) {
        if(job.result < 0) {
            job.result = -1;
        }
    } else {
        cn1BackendOffload(cn1ReadJob, &job);
    }
    CN1_RESUME_THREAD;
    return (JAVA_INT)job.result;
#endif
}

/*
 * Resolves a path to its canonical form, following symlinks. The static handler
 * uses this to prove a resolved file really is inside the document root -- a
 * check on the request string alone is defeated by an encoded traversal or by a
 * symlink pointing out of the tree.
 */
JAVA_OBJECT com_codename1_backend_FileIo_realPathImpl___java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
#ifdef _WIN32
    (void)path;
    return JAVA_NULL;
#else
    char resolved[PATH_MAX > 4096 ? PATH_MAX : 4096];
    struct cn1FileJob job;
    const char* p = path == JAVA_NULL ? NULL : stringToUTF8(threadStateData, path);
    if(p == NULL) {
        return JAVA_NULL;
    }
    memset(&job, 0, sizeof(job));
    job.path = p;
    job.out = resolved;
    CN1_YIELD_THREAD;
    cn1BackendOffload(cn1RealPathJob, &job);
    CN1_RESUME_THREAD;
    if(job.result != 0) {
        return JAVA_NULL;
    }
    return newStringFromUtf8Len(threadStateData, resolved, (int)strlen(resolved));
#endif
}

JAVA_VOID com_codename1_backend_FileIo_closeImpl___int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd) {
#ifndef _WIN32
    if(fd >= 0) {
        close(fd);
    }
#else
    (void)fd;
#endif
}

/*
 * Spool files: the disk side of a request body or multipart part too large to
 * hold in memory (Spool.java). The file is unlinked the moment it is created,
 * so it has no name for anything else to open and the kernel reclaims it when
 * the descriptor closes -- on the normal path, on a handler that throws, on a
 * dropped connection, and on a process that dies, none of which has to find a
 * path to delete. Owner-only either way: mkstemp creates it 0600.
 */
JAVA_INT com_codename1_backend_FileIo_createTempImpl___java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT dir) {
#ifdef _WIN32
    (void)dir;
    return -1;
#else
    const char* d = dir == JAVA_NULL ? NULL : stringToUTF8(threadStateData, dir);
    char path[PATH_MAX];
    struct cn1FileJob job;
    if(d == NULL || d[0] == 0) {
        d = getenv("TMPDIR");
    }
    if(d == NULL || d[0] == 0) {
        d = "/tmp";
    }
    if(snprintf(path, sizeof(path), "%s/cn1-spool-XXXXXX", d) >= (int)sizeof(path)) {
        return -1;
    }
    memset(&job, 0, sizeof(job));
    job.out = path;
    CN1_YIELD_THREAD;
    cn1BackendOffload(cn1CreateTempJob, &job);
    CN1_RESUME_THREAD;
    return (JAVA_INT)job.result;
#endif
}

/* Writes all of the slice or fails: -1 on error, otherwise length. */
JAVA_INT com_codename1_backend_FileIo_writeImpl___int_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
#ifdef _WIN32
    (void)fd; (void)buffer; (void)offset; (void)length;
    return -1;
#else
    JAVA_ARRAY_BYTE* data;
    struct cn1FileJob job;
    if(fd < 0 || buffer == JAVA_NULL) {
        return -1;
    }
    data = (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(buffer);
    memset(&job, 0, sizeof(job));
    job.fd = fd;
    job.data = &data[offset];
    job.length = (size_t)length;
    CN1_YIELD_THREAD;
    {
        /* The ring answers a short write like write() does, so loop; whatever it
           does not take -- refused, or no ring at all -- the pool job finishes. */
        size_t written = 0;
        int failed = 0;
        while(written < job.length) {
            long long n;
            if(!cn1BackendUringWrite(fd, job.data + written, job.length - written, &n)) {
                break;
            }
            if(n <= 0) {
                failed = 1;
                break;
            }
            written += (size_t)n;
        }
        if(failed) {
            job.result = -1;
        } else if(written < job.length) {
            job.data += written;
            job.length -= written;
            cn1BackendOffload(cn1WriteJob, &job);
        } else {
            job.result = (long long)written;
        }
    }
    CN1_RESUME_THREAD;
    return job.result < 0 ? -1 : length;
#endif
}

/* A positioned read, so any number of streams can read one spool file. */
JAVA_INT com_codename1_backend_FileIo_preadImpl___int_byte_1ARRAY_int_int_long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length, JAVA_LONG position) {
#ifdef _WIN32
    (void)fd; (void)buffer; (void)offset; (void)length; (void)position;
    return -1;
#else
    JAVA_ARRAY_BYTE* data;
    if(fd < 0 || buffer == JAVA_NULL || position < 0) {
        return -1;
    }
    struct cn1FileJob job;
    data = (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(buffer);
    memset(&job, 0, sizeof(job));
    job.fd = fd;
    job.data = &data[offset];
    job.length = (size_t)length;
    job.position = (long long)position;
    CN1_YIELD_THREAD;
    if(cn1BackendUringRead(fd, job.data, job.length, job.position, &job.result)) {
        if(job.result < 0) {
            job.result = -1;
        }
    } else {
        cn1BackendOffload(cn1PreadJob, &job);
    }
    CN1_RESUME_THREAD;
    return (JAVA_INT)job.result;
#endif
}
