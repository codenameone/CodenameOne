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
 * Listening sockets and a readiness poller for server-side binaries.
 *
 * Why a reactor rather than a thread per connection: a parked ParparVM thread was
 * measured at 243KB on musl/arm64 (see vm/benchmarks ThreadCost), so ten thousand
 * connections would be gigabytes of threads. A connection here is an fd; only the
 * ones with a request in flight occupy a worker.
 *
 * epoll on Linux, kqueue on the BSDs and macOS, behind one interface. Level-
 * triggered on purpose: edge-triggered requires draining every fd to EAGAIN on
 * every wake-up, and the whole point of this design is that the poller hands a
 * ready fd to a worker and stops thinking about it.
 */
#include "cn1_globals.h"
#include "cn1_virtual_thread.h"
#include <stdatomic.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <signal.h>
#ifndef _WIN32
#include <poll.h>
#endif
#include <stdlib.h>

#ifndef _WIN32
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <netdb.h>
#endif

#if defined(__linux__)
#include <sys/epoll.h>
#define CN1_HAVE_EPOLL 1
#elif defined(__APPLE__) || defined(__FreeBSD__) || defined(__NetBSD__) || defined(__OpenBSD__)
#include <sys/event.h>
#include <sys/time.h>
#define CN1_HAVE_KQUEUE 1
#endif

/* Mirrors the Java side; keep in sync with Reactor. */
#define CN1_EVENT_READ  1
#define CN1_EVENT_WRITE 2
#define CN1_EVENT_ONESHOT 4

#ifdef MSG_NOSIGNAL
#define CN1_SEND_FLAGS MSG_NOSIGNAL
#else
#define CN1_SEND_FLAGS 0
#endif

JAVA_INT com_codename1_backend_ServerSocket_bindImpl___java_lang_String_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT host, JAVA_INT port, JAVA_INT backlog) {
#ifdef _WIN32
    (void)host; (void)port; (void)backlog;
    return -1;
#else
    struct sockaddr_in addr;
    int fd;
    int on = 1;
    const char* h = host == JAVA_NULL ? NULL : stringToUTF8(threadStateData, host);

    /* SIGPIPE's default action is to kill the process, and a client that goes away
       mid-response makes send() raise it. Ignoring it here rather than only inside
       Signals.installShutdownHandler(): that call is optional, so a server that
       never made it died the first time a browser closed a tab. Setting it once at
       bind costs nothing and cannot be skipped by a server that listens. */
    signal(SIGPIPE, SIG_IGN);

    if(h != NULL && h[0] != 0 && strcmp(h, "0.0.0.0") != 0) {
        /* Resolved, not parsed as numeric IPv4.
           inet_pton alone accepted only a dotted quad, so "localhost" -- the most
           ordinary bind host there is -- and every IPv6 address failed startup, but
           ONLY once packaged natively: the JavaSE side goes through
           InetSocketAddress and takes all of them, so the configuration was proven
           under cn1:backend and then would not start. */
        struct addrinfo hints;
        struct addrinfo* res = NULL;
        struct addrinfo* it;
        char portStr[16];
        memset(&hints, 0, sizeof(hints));
        hints.ai_family = AF_UNSPEC;
        hints.ai_socktype = SOCK_STREAM;
        hints.ai_flags = AI_PASSIVE;
        snprintf(portStr, sizeof(portStr), "%d", (int)port);
        /* The same reason the outbound connect yields around this: getaddrinfo
           blocks, and a thread the VM believes is running holds up a collection
           for as long as the resolver takes. `h` is in the thread state's own
           malloc'd buffer, so it survives the yield. */
        CN1_YIELD_THREAD;
        if(getaddrinfo(h, portStr, &hints, &res) != 0) {
            CN1_RESUME_THREAD;
            return -1;
        }
        CN1_RESUME_THREAD;
        for(it = res ; it != NULL ; it = it->ai_next) {
            fd = socket(it->ai_family, it->ai_socktype, it->ai_protocol);
            if(fd < 0) {
                continue;
            }
            setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, (const char*)&on, sizeof(on));
            if(bind(fd, it->ai_addr, it->ai_addrlen) == 0 && listen(fd, backlog) == 0) {
                freeaddrinfo(res);
                return fd;
            }
            close(fd);
        }
        freeaddrinfo(res);
        return -1;
    }

    /* A null host means "every interface", and that has to include the v6 ones:
       an AF_INET socket cannot accept an IPv6 client, so the default binding was
       unreachable in an IPv6-only deployment while an explicitly named host --
       which takes the getaddrinfo path above -- worked. A v6 socket with
       V6ONLY cleared serves both families through one descriptor. Falling back
       to AF_INET keeps hosts with no IPv6 at all working exactly as before. */
    {
        struct sockaddr_in6 addr6;
        int off = 0;
        fd = socket(AF_INET6, SOCK_STREAM, 0);
        if(fd >= 0) {
            setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, (const char*)&on, sizeof(on));
            if(setsockopt(fd, IPPROTO_IPV6, IPV6_V6ONLY, (const char*)&off,
                    sizeof(off)) == 0) {
                memset(&addr6, 0, sizeof(addr6));
                addr6.sin6_family = AF_INET6;
                addr6.sin6_port = htons((unsigned short)port);
                addr6.sin6_addr = in6addr_any;
                if(bind(fd, (struct sockaddr*)&addr6, sizeof(addr6)) == 0
                        && listen(fd, backlog) == 0) {
                    return fd;
                }
            }
            close(fd);
        }
    }

    fd = socket(AF_INET, SOCK_STREAM, 0);
    if(fd < 0) {
        return -1;
    }
    /* Without SO_REUSEADDR a restart inside the TIME_WAIT window fails to bind,
       which in a container is every restart. */
    setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, (const char*)&on, sizeof(on));

    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons((unsigned short)port);
    addr.sin_addr.s_addr = htonl(INADDR_ANY);
    if(bind(fd, (struct sockaddr*)&addr, sizeof(addr)) != 0) {
        close(fd);
        return -1;
    }
    if(listen(fd, backlog) != 0) {
        close(fd);
        return -1;
    }
    return fd;
#endif
}

/* The port actually bound, so a caller may ask for 0 and be told what it got. */
/*
 * A byte[] whose storage is a C buffer this file owns, handed to Java with no
 * copy and never allocated by the collector.
 *
 * This works because of three properties of ParparVM that a moving or precise VM
 * would not give us, all of them already true -- nothing about GC semantics is
 * changed here:
 *
 *   1. `struct JavaArrayPrototype` holds `void* data` as a POINTER, separate from
 *      the header. allocArray happens to point it just past itself, but nothing
 *      requires that, so it can address a buffer the GC never allocated.
 *   2. gcMarkObject validates a pointer against the page/extent tables BEFORE it
 *      dereferences anything, and returns for one that does not resolve. A header
 *      outside every heap page is therefore ignored rather than corrupted.
 *   3. The sweep walks heap pages, so an object that is in none is never freed.
 *
 * The header is registered as an immortal root anyway. That is not needed to keep
 * THIS array alive -- nothing sweeps it -- it is needed so the mark guard accepts
 * the pointer, which is what lets a foreign array hold references that still get
 * traced. A byte[] has no reference children, so for this one it is belt and
 * braces; for the same trick applied to an object with fields it is load bearing.
 * cn1AddImmortalRoot documents the off-heap registrant case explicitly.
 *
 * One per thread, allocated once and reused, so the steady-state allocation rate
 * contributed by the read path is zero rather than small.
 */
static __thread struct JavaArrayPrototype* cn1BackendReadArray = 0;
static __thread char* cn1BackendReadStorage = 0;
static __thread JAVA_INT cn1BackendReadCap = 0;

/*
 * Whether awaitReadable probes with poll() before parking. Read once; see the
 * discussion at the call site. 1 (probe) is the shipped default until the A/B
 * on an idle host says otherwise.
 */
static int cn1BackendSpeculativePoll(void) {
    static int cached = -1;
    if(cached < 0) {
        const char* v = getenv("CN1_HTTP_SPECULATIVE_POLL");
        cached = (v != 0 && v[0] == '0') ? 0 : 1;
    }
    return cached;
}

/*
 * The poller's event array, one per thread, grown on demand and reused.
 *
 * This ran as a malloc/free pair on EVERY poller wait. The read path above is
 * pooled precisely "so the steady-state allocation rate contributed by the read
 * path is zero rather than small", and the poller sits on the same loop -- once
 * per scheduling turn, which under virtual threads is about once per request --
 * so it was contributing the allocation the read path had been taught not to.
 *
 * epoll and kqueue never both compile in (#if / #elif below), so one buffer with
 * a byte capacity serves whichever is built, and the entry size is passed in
 * rather than baked in.
 */
static __thread void* cn1BackendEventBuf = 0;
static __thread int cn1BackendEventCap = 0;

static void* cn1BackendEnsureEventBuf(int capacity, size_t entrySize) {
    void* grown;
    if(capacity <= 0) {
        return 0;
    }
    if(cn1BackendEventBuf != 0 && cn1BackendEventCap >= capacity) {
        return cn1BackendEventBuf;
    }
    grown = realloc(cn1BackendEventBuf, entrySize * (size_t)capacity);
    if(grown == 0) {
        return 0;
    }
    cn1BackendEventBuf = grown;
    cn1BackendEventCap = capacity;
    return grown;
}

static struct JavaArrayPrototype* cn1BackendEnsureReadArray(JAVA_INT capacity) {
    if(capacity <= 0) {
        return 0;
    }
    if(cn1BackendReadArray != 0 && cn1BackendReadCap >= capacity) {
        return cn1BackendReadArray;
    }
    if(cn1BackendReadArray == 0) {
        cn1BackendReadArray = (struct JavaArrayPrototype*)
                calloc(1, sizeof(struct JavaArrayPrototype));
        if(cn1BackendReadArray == 0) {
            return 0;
        }
        cn1BackendReadArray->__codenameOneParentClsReference = &class_array1__JAVA_BYTE;
        cn1BackendReadArray->__codenameOneGcMark = -1;
        cn1BackendReadArray->__heapPosition = -1;
        cn1BackendReadArray->dimensions = 1;
        cn1BackendReadArray->primitiveSize = sizeof(JAVA_ARRAY_BYTE);
        cn1AddImmortalRoot((JAVA_OBJECT)cn1BackendReadArray);
    }
    {
        char* grown = (char*)realloc(cn1BackendReadStorage, (size_t)capacity);
        if(grown == 0) {
            return 0;
        }
        cn1BackendReadStorage = grown;
        cn1BackendReadCap = capacity;
        // The header outlives every grow, so the Java side keeps one identity and
        // only the storage moves -- which is safe precisely because no Java
        // reference points INTO the storage, only at the header.
        cn1BackendReadArray->data = cn1BackendReadStorage;
    }
    return cn1BackendReadArray;
}

JAVA_OBJECT com_codename1_backend_ServerSocket_threadReadBufferImpl___int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_INT capacity) {
    struct JavaArrayPrototype* a = cn1BackendEnsureReadArray(capacity);
    if(a == 0) {
        return JAVA_NULL;
    }
    a->length = capacity;
    return (JAVA_OBJECT)a;
}

/*
 * Read straight into this thread's buffer and hand back an array whose length is
 * exactly the byte count, so the parser can scan to array.length as it always has.
 *
 * Setting `length` per read is the whole trick, and it is sound here for a reason
 * worth stating: `length` is an ordinary int in a struct this file allocated and
 * owns, the array is reachable only through the return value for the duration of
 * one callback, and no Java reference points INTO the storage -- only at the
 * header. A VM that packed the length into an object header the collector reads,
 * or that moved objects, could not do this.
 *
 * Returns null at end of stream or on error, which the caller treats as the peer
 * having gone away -- the same contract the copying path has.
 */
JAVA_OBJECT com_codename1_backend_ServerSocket_readIntoThreadBufferImpl___int_int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_INT capacity) {
    struct JavaArrayPrototype* a = cn1BackendEnsureReadArray(capacity);
    ssize_t n;
    if(a == 0 || fd < 0) {
        return JAVA_NULL;
    }
    // YIELD around the blocking read, exactly as readImpl does. Without it the
    // thread stays marked active while it sits in the kernel, so the collector has
    // to wait for every worker that is parked on a socket before it can stop the
    // world. Omitting it cost HALF the throughput -- 147k against 288k req/s -- and
    // it is a liveness bug before it is a performance one: a quiet connection
    // would hold the collector for as long as the client stayed silent.
    CN1_YIELD_THREAD;
    do {
        n = read(fd, cn1BackendReadStorage, (size_t)capacity);
    } while(n < 0 && errno == EINTR);
    CN1_RESUME_THREAD;
    if(n <= 0) {
        return JAVA_NULL;
    }
    a->length = (int)n;
    return (JAVA_OBJECT)a;
}

JAVA_INT com_codename1_backend_ServerSocket_boundPortImpl___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd) {
#ifdef _WIN32
    (void)fd;
    return -1;
#else
    /* sockaddr_storage, because the bind above may have chosen IPv6 and the port
       does not sit at the same offset in the two families -- reading a v6 socket
       through sockaddr_in reports a number that was never the port. */
    struct sockaddr_storage addr;
    socklen_t len = sizeof(addr);
    if(fd < 0 || getsockname(fd, (struct sockaddr*)&addr, &len) != 0) {
        return -1;
    }
    if(addr.ss_family == AF_INET6) {
        return (JAVA_INT)ntohs(((struct sockaddr_in6*)&addr)->sin6_port);
    }
    return (JAVA_INT)ntohs(((struct sockaddr_in*)&addr)->sin_port);
#endif
}

/* -1 means "nothing waiting" (EAGAIN) as well as a real error; the caller is a
   poller that will be told again if there is more. */
JAVA_INT com_codename1_backend_ServerSocket_acceptImpl___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT serverFd) {
#ifdef _WIN32
    (void)serverFd;
    return -1;
#else
    int fd;
    if(serverFd < 0) {
        return -1;
    }
    CN1_YIELD_THREAD;
    fd = accept(serverFd, NULL, NULL);
    CN1_RESUME_THREAD;
    if(fd < 0) {
        return -1;
    }
    /* Nagle batches small writes, which on a request/response protocol means the
       response header waits for the body. Off. */
    {
        int on = 1;
        setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, (const char*)&on, sizeof(on));
    }
    return fd;
#endif
}

JAVA_INT com_codename1_backend_ServerSocket_setBlockingImpl___int_boolean_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_BOOLEAN blocking) {
#ifdef _WIN32
    (void)fd; (void)blocking;
    return -1;
#else
    int flags;
    if(fd < 0) {
        return -1;
    }
    flags = fcntl(fd, F_GETFL, 0);
    if(flags < 0) {
        return -1;
    }
    flags = blocking ? (flags & ~O_NONBLOCK) : (flags | O_NONBLOCK);
    return fcntl(fd, F_SETFL, flags) == 0 ? 0 : -1;
#endif
}

/*
 * A receive and send deadline for one descriptor, in milliseconds.
 *
 * This is what stops a connection that opens and then says nothing from holding a
 * worker forever. The worker pool is bounded on purpose, so without a deadline a
 * handful of silent connections is a complete denial of service -- open as many as
 * there are workers and the server stops answering anyone.
 */
/*
 * Wait for the socket to become readable, for at most timeoutMillis.
 *
 * ONE syscall, and it changes no socket state -- which is the whole point. The
 * caller uses this between requests on a keep-alive connection, and the obvious
 * alternatives both cost more: SO_RCVTIMEO has to be set and restored around
 * every wait (two setsockopt each way, measured at 4 per request), and switching
 * the descriptor to non-blocking costs an fcntl pair. poll leaves the descriptor
 * exactly as it was, so the deadline that governs a real request read is never
 * disturbed.
 *
 * Returns 1 readable, 0 timed out, -1 error. EINTR retries rather than reporting
 * a timeout: the collector signals threads, and a signal is not a quiet client.
 */
JAVA_INT com_codename1_backend_ServerSocket_awaitReadableImpl___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_INT timeoutMillis) {
#ifdef _WIN32
    (void)fd; (void)timeoutMillis;
    return -1;
#else
    struct pollfd p;
    int rc;
    if(fd < 0) {
        return -1;
    }
    p.fd = fd;
    p.events = POLLIN;
    // On a virtual thread, ask once without blocking; if nothing is there, park
    // rather than hold the host thread for the timeout. The scheduler only
    // resumes a parked virtual thread once the poller reports its descriptor
    // ready, so coming back IS the readiness answer.
    if(cn1VirtualThreadCurrent() != 0) {
        // ASK FIRST, and this poll is an optimisation rather than the waste it
        // looks like in a syscall census.
        //
        // It was removed once on the grounds that Go does not do it -- its
        // FD.Read calls read() straight away and parks on EAGAIN -- and that the
        // census showed 1.9 ppoll per request against Go's zero. Throughput fell
        // from 1047890 requests to about 110000 and /json died. The census was
        // counting a cheap syscall that PREVENTS an expensive one: when the next
        // request has already arrived, which under keep-alive it usually has,
        // this answers immediately and the virtual thread never parks. Without
        // it every keep-alive wait costs a park, an epoll round trip and a
        // resume.
        //
        // The lesson generalises: syscall COUNT is not cost. Go can afford to
        // skip this because its park is a goroutine switch inside a scheduler
        // that is already awake; ours goes out to the poller and back.
        // Left switchable rather than deleted, because the case against it is
        // real and the case for it was measured on an older scheduler.
        //
        // Against: the caller's next move is a recv, and on a virtual thread that
        // recv already parks on EAGAIN and retries when the poller says the
        // descriptor is ready. Polling here first costs one extra syscall on
        // EVERY request to learn what the read is about to learn anyway -- a
        // corrected census puts it at exactly 1.0 ppoll per request, the only
        // syscall in our profile that Go does not make at all.
        //
        // For: removing it costs almost all of the throughput, and that is still
        // true after the host loop learned to drain a local run queue before
        // polling -- which was the reason to expect otherwise. RE-MEASURED, four
        // arms interleaved on a quiet host, /plaintext at 64 connections, medians
        // of four steady-state reps:
        //
        //     go                       243,161 req/s
        //     virtual threads, probe   207,808        0.854 of go
        //     virtual threads, NO probe 20,134        0.082 of go   <-- 12x worse
        //
        // The syscall census makes the trap explicit: without the probe a request
        // costs 2.05 syscalls against Go's 2.64 -- FEWER than Go -- and it is ten
        // times slower, because the ppoll it saves is replaced by a park, and a
        // park is a poller round trip plus a resume. Syscall COUNT is not cost.
        // Do not re-run this experiment expecting a different answer; run it only
        // after the PARK itself gets cheaper.
        //
        // A zero timeout is a genuine probe (a caller asking "is anything there"
        // without wanting to wait), so that one still has to ask regardless.
        if(!cn1BackendSpeculativePoll() && timeoutMillis != 0) {
            return 1;
        }
        p.revents = 0;
        rc = poll(&p, 1, 0);
        if(rc > 0) {
            return 1;
        }
        if(rc < 0 && errno != EINTR) {
            return -1;
        }
        if(timeoutMillis == 0) {
            return 0;               // a pure probe: no data, do not park
        }
        // YIELD around the park, and this is not optional bookkeeping.
        //
        // A parked virtual thread is not running and never will be until somebody
        // resumes it, so leaving it marked ACTIVE tells the collector to wait for
        // it to reach a safepoint that it cannot reach. This is the keep-alive
        // wait, so in this mode every idle connection is parked here: the first
        // burst of traffic works, and the moment the connections go quiet the
        // collector stops being able to finish a cycle and every mutator ends up
        // in the pacing park behind it. Observed exactly that -- 204712 requests
        // served, then nothing, with no thread in epoll_pwait, five in futex,
        // five in nanosleep, and the process at 7% CPU.
        CN1_YIELD_THREAD;
        cn1VirtualThreadYield();
        CN1_RESUME_THREAD;
        return 1;
    }
    for(;;) {
        int pollErrno;
        p.revents = 0;
        CN1_YIELD_THREAD;
        rc = poll(&p, 1, timeoutMillis);
        /* CAPTURED HERE, before CN1_RESUME_THREAD. The resume is a GC safepoint: it
           can park this thread on a timed condition wait, and that leaves errno set
           to ETIMEDOUT. Reading errno after it therefore reported the WAIT's outcome
           rather than the poll's, so an ordinary EINTR from the collector's stop
           signal was misread as a fatal poll error and a healthy keep-alive
           connection was closed -- which the client sees as a reset, because there
           is already a pipelined request sitting unread in the receive buffer.
           Rare, load dependent, and it took several connections down at once because
           one collector pause signals every worker. */
        pollErrno = errno;
        CN1_RESUME_THREAD;
        if(rc >= 0) {
            break;
        }
        if(pollErrno != EINTR) {
            return -1;
        }
    }
    return rc > 0 ? 1 : 0;
#endif
}

JAVA_INT com_codename1_backend_ServerSocket_setTimeoutImpl___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_INT millis) {
#ifdef _WIN32
    (void)fd; (void)millis;
    return -1;
#else
    struct timeval tv;
    if(fd < 0) {
        return -1;
    }
    tv.tv_sec = millis / 1000;
    tv.tv_usec = (millis % 1000) * 1000;
    if(setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, (const char*)&tv, sizeof(tv)) != 0) {
        return -1;
    }
    /* A send deadline too: a peer that stops reading would otherwise block a
       worker in send() just as effectively as one that stops writing. */
    return setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, (const char*)&tv, sizeof(tv)) == 0 ? 0 : -1;
#endif
}

/* Blocking read while a worker owns the connection. -1 is end of stream, -2 an
   error; the fd is set blocking before a worker gets it, so there is no EAGAIN. */
JAVA_INT com_codename1_backend_ServerSocket_readImpl___int_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
#ifdef _WIN32
    (void)fd; (void)buffer; (void)offset; (void)length;
    return -2;
#else
    JAVA_ARRAY_BYTE* data;
    long n;
    int readErrno;
    if(fd < 0 || buffer == JAVA_NULL) {
        return -2;
    }
    data = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)buffer)->data;
    CN1_YIELD_THREAD;
    for(;;) {
        n = (long)recv(fd, (char*)&data[offset], (size_t)length, 0);
        if(n >= 0 || (errno != EINTR && errno != EAGAIN && errno != EWOULDBLOCK)) {
            break;
        }
        if(errno == EINTR) {
            continue;
        }
        // EAGAIN on a VIRTUAL thread is not an error and not a deadline: it means
        // the bytes have not arrived. Park, and the scheduler resumes this virtual
        // thread when the poller says the descriptor is readable -- the host thread
        // goes and runs somebody else in the meantime, which is the entire point.
        //
        // On a platform thread there is no one to hand the host to, so the old
        // answer stands: report the deadline and let the caller decide.
        if(cn1VirtualThreadCurrent() == 0) {
            break;
        }
        // The array may MOVE while we are parked -- a collection can run, and the
        // buffer is an ordinary Java object -- so re-read the data pointer after
        // every resume rather than trusting the one taken before the park.
        cn1VirtualThreadYield();
        data = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)buffer)->data;
    }
    /* Captured before CN1_RESUME_THREAD for the same reason as the poll loop above:
       the resume is a GC safepoint and can park this thread on a timed wait, which
       overwrites errno. Reading it afterwards classified an ordinary deadline as a
       fault (-2 instead of -3), and could equally hide a real error behind -3. */
    readErrno = errno;
    CN1_RESUME_THREAD;
    if(n == 0) {
        return -1;
    }
    if(n < 0) {
        /* -3 is the deadline expiring, which is an ordinary event a server sheds
           rather than a fault worth logging as one. */
        return (readErrno == EAGAIN || readErrno == EWOULDBLOCK) ? -3 : -2;
    }
    return (JAVA_INT)n;
#endif
}

/*
 * Waits for a descriptor to become writable, bounded by its own send deadline.
 *
 * Needed because serveOne leaves plaintext descriptors NON-BLOCKING in
 * virtual-thread mode, so send() answers EAGAIN as soon as the client's receive
 * window fills. Both write paths treated that as a permanent failure and dropped
 * the connection, which truncates any response a client reads slowly -- while a
 * blocking descriptor, which is what they were written against, simply waited.
 *
 * Waiting here rather than parking the virtual thread: a park is resumed by the
 * poller, and a connection descriptor is registered for READ only, so a thread
 * parked on writability would never be woken. Yielding as RUNNABLE instead would
 * spin, and a running thread has no idle deadline to expire it. SO_SNDTIMEO is
 * already set per connection, so this bounds the wait the same way a blocking
 * send would have.
 *
 * Returns 1 when writable, 0 on timeout, -1 on error.
 */
static int cn1AwaitWritable(int fd) {
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

JAVA_INT com_codename1_backend_ServerSocket_writeImpl___int_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
#ifdef _WIN32
    (void)fd; (void)buffer; (void)offset; (void)length;
    return -1;
#else
    JAVA_ARRAY_BYTE* data;
    JAVA_INT written = 0;
    if(fd < 0 || buffer == JAVA_NULL) {
        return -1;
    }
    data = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)buffer)->data;
    CN1_YIELD_THREAD;
    while(written < length) {
        /* MSG_NOSIGNAL where it exists, so this write cannot raise SIGPIPE even if
           the disposition were somehow restored. It is 0 on platforms without it --
           macOS among them -- where the SIG_IGN set at bind is what covers this. */
        long n = (long)send(fd, (const char*)&data[offset + written],
                            (size_t)(length - written), CN1_SEND_FLAGS);
        if(n < 0 && errno == EINTR) {
            continue;
        }
        if(n < 0 && (errno == EAGAIN || errno == EWOULDBLOCK)) {
            /* Backpressure, not failure: the client has not drained its window
               yet. Returning -1 here dropped the connection and truncated the
               response the moment a client read slower than the server wrote. */
            int ready = cn1AwaitWritable(fd);
            if(ready > 0) {
                continue;
            }
            CN1_RESUME_THREAD;
            return ready == 0 ? -3 : -1;    /* -3 is the deadline, as on the read side */
        }
        if(n <= 0) {
            CN1_RESUME_THREAD;
            return -1;
        }
        written += (JAVA_INT)n;
    }
    CN1_RESUME_THREAD;
    return written;
#endif
}

JAVA_VOID com_codename1_backend_ServerSocket_closeFdImpl___int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd) {
#ifndef _WIN32
    if(fd >= 0) {
        close(fd);
    }
#else
    (void)fd;
#endif
}

/* ------------------------------------------------------------------ */
/* Reactor                                                            */
/* ------------------------------------------------------------------ */

JAVA_INT com_codename1_backend_Reactor_createImpl___R_int(CODENAME_ONE_THREAD_STATE) {
#if defined(CN1_HAVE_EPOLL)
    return epoll_create1(0);
#elif defined(CN1_HAVE_KQUEUE)
    return kqueue();
#else
    return -1;
#endif
}

JAVA_INT com_codename1_backend_Reactor_registerImpl___int_int_int_boolean_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT poller, JAVA_INT fd, JAVA_INT events, JAVA_BOOLEAN modify) {
#if defined(CN1_HAVE_EPOLL)
    struct epoll_event ev;
    memset(&ev, 0, sizeof(ev));
    ev.data.fd = fd;
    if(events & CN1_EVENT_READ) {
        ev.events |= EPOLLIN;
    }
    if(events & CN1_EVENT_WRITE) {
        ev.events |= EPOLLOUT;
    }
    if(events & CN1_EVENT_ONESHOT) {
        // EPOLLONESHOT is what makes it safe for the WORKERS to poll the same
        // epoll set directly instead of a reactor thread dispatching to them.
        // After an event is delivered the kernel disarms the fd, so exactly one
        // waiter can ever receive it and the "two workers on one connection"
        // hazard that forces the reactor path to EPOLL_CTL_DEL before handing
        // over cannot arise. Re-arming afterwards is one EPOLL_CTL_MOD, against
        // the DEL + ADD that path pays, and it costs no cross-thread wake.
        ev.events |= EPOLLONESHOT;
    }
    return epoll_ctl(poller, modify ? EPOLL_CTL_MOD : EPOLL_CTL_ADD, fd, &ev) == 0 ? 0 : -1;
#elif defined(CN1_HAVE_KQUEUE)
    struct kevent ev[2];
    int n = 0;
    (void)modify; /* kevent's ADD is idempotent, so a modify is the same call */
    if(events & CN1_EVENT_READ) {
        // EV_DISPATCH is kqueue's EPOLLONESHOT: deliver once, then disable the
        // filter until it is re-enabled. EV_ENABLE on the re-arm turns it back on.
        EV_SET(&ev[n++], fd, EVFILT_READ,
               EV_ADD | EV_ENABLE | ((events & CN1_EVENT_ONESHOT) ? EV_DISPATCH : 0),
               0, 0, NULL);
    }
    if(events & CN1_EVENT_WRITE) {
        EV_SET(&ev[n++], fd, EVFILT_WRITE, EV_ADD | EV_ENABLE, 0, 0, NULL);
    }
    if(n == 0) {
        return 0;
    }
    return kevent(poller, ev, n, NULL, 0, NULL) < 0 ? -1 : 0;
#else
    (void)poller; (void)fd; (void)events; (void)modify;
    return -1;
#endif
}

JAVA_INT com_codename1_backend_Reactor_unregisterImpl___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT poller, JAVA_INT fd) {
#if defined(CN1_HAVE_EPOLL)
    return epoll_ctl(poller, EPOLL_CTL_DEL, fd, NULL) == 0 ? 0 : -1;
#elif defined(CN1_HAVE_KQUEUE)
    struct kevent ev[2];
    EV_SET(&ev[0], fd, EVFILT_READ, EV_DELETE, 0, 0, NULL);
    EV_SET(&ev[1], fd, EVFILT_WRITE, EV_DELETE, 0, 0, NULL);
    /* ENOENT here just means it was not registered for that filter. */
    kevent(poller, ev, 2, NULL, 0, NULL);
    return 0;
#else
    (void)poller; (void)fd;
    return -1;
#endif
}

/*
 * Fills readyFds with the descriptors that became ready and returns how many.
 * Bracketed with CN1_YIELD_THREAD because this blocks for as long as the server
 * is idle, which is most of its life -- without it the collector could not mark
 * past the reactor thread.
 */
JAVA_INT com_codename1_backend_Reactor_waitImpl___int_int_1ARRAY_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT poller, JAVA_OBJECT readyFds, JAVA_INT timeoutMillis) {
    JAVA_ARRAY arr;
    JAVA_ARRAY_INT* out;
    int capacity;
    int count = 0;
    if(readyFds == JAVA_NULL) {
        return -1;
    }
    arr = (JAVA_ARRAY)readyFds;
    out = (JAVA_ARRAY_INT*)arr->data;
    capacity = arr->length;
#if defined(CN1_HAVE_EPOLL)
    {
        struct epoll_event* events = (struct epoll_event*)cn1BackendEnsureEventBuf(
                capacity, sizeof(struct epoll_event));
        int n, i;
        if(events == NULL) {
            return -1;
        }
        CN1_YIELD_THREAD;
        do {
            n = epoll_wait(poller, events, capacity, timeoutMillis);
        } while(n < 0 && errno == EINTR);
        CN1_RESUME_THREAD;
        for(i = 0 ; i < n && count < capacity ; i++) {
            out[count++] = events[i].data.fd;
        }
        return n < 0 ? -1 : count;
    }
#elif defined(CN1_HAVE_KQUEUE)
    {
        struct kevent* events = (struct kevent*)cn1BackendEnsureEventBuf(
                capacity, sizeof(struct kevent));
        struct timespec ts;
        struct timespec* tsp = NULL;
        int n, i;
        if(events == NULL) {
            return -1;
        }
        if(timeoutMillis >= 0) {
            ts.tv_sec = timeoutMillis / 1000;
            ts.tv_nsec = (long)(timeoutMillis % 1000) * 1000000L;
            tsp = &ts;
        }
        CN1_YIELD_THREAD;
        do {
            n = kevent(poller, NULL, 0, events, capacity, tsp);
        } while(n < 0 && errno == EINTR);
        CN1_RESUME_THREAD;
        for(i = 0 ; i < n && count < capacity ; i++) {
            out[count++] = (JAVA_INT)events[i].ident;
        }
        return n < 0 ? -1 : count;
    }
#else
    (void)poller; (void)timeoutMillis;
    return -1;
#endif
}

/* ===================== VIRTUAL THREADS FOR CONNECTIONS =====================
 *
 * One virtual thread per connection, which is the shape Go gets from a goroutine
 * per connection and the shape a bounded pool of OS threads cannot reach. The
 * measured reason: handing a request between OS threads costs 21181ns here and
 * switching a virtual thread costs 2.6ns.
 *
 * The scheduler is deliberately tiny, because the interesting part is done by
 * the parking above. A host thread polls, resumes the virtual thread belonging
 * to whichever descriptor is ready, and gets control back when that virtual
 * thread either finishes the connection or parks waiting for more bytes. It
 * never needs to know WHICH of those happened for any reason other than deciding
 * whether to re-arm the descriptor.
 */

/* The Java entry point a connection's virtual thread runs. Referencing the
 * symbol here is also what keeps it alive: the dead-code pass treats a method
 * named in native sources as used, and nothing in Java calls this one. */
extern JAVA_VOID com_codename1_backend_HttpServer_serveVirtual___int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd);

struct cn1BackendVtArg {
    JAVA_INT fd;
};

extern void markDeadThread(struct ThreadLocalData* d);

static void cn1BackendVtBody(void* arg) {
    struct cn1BackendVtArg* a = (struct cn1BackendVtArg*)arg;
    /* getThreadLocalData returns THIS virtual thread's state, because it is the
     * one running -- see the hook in nativeMethods.m. Taking the host's state
     * here would give two threads of control one Java stack. */
    struct ThreadLocalData* mine = getThreadLocalData();
    com_codename1_backend_HttpServer_serveVirtual___int(mine, a->fd);

    /*
     * RETIRE THE THREAD STATE. This is not tidiness, it is the difference between
     * a server that works and one that stops after its first burst of traffic.
     *
     * A virtual thread's ThreadLocalData is registered in allThreads and marked
     * lightweightThread, and the collector's stop-the-world does this for every
     * such entry:
     *
     *     t->threadBlockedByGC = JAVA_TRUE;
     *     while(t->threadActive) { usleep(500); }      // no timeout
     *
     * A finished virtual thread will never run again, so nothing will ever clear
     * threadActive for it, and the collector waits on it for ever. Every GC cycle
     * after the first connection closes simply never completes; the allocation
     * pacing then never releases, and the whole server settles to a few hundred
     * requests a second while looking completely idle -- no crash, no spin, 8% of
     * a CPU. Found by asking gdb where the collector was, and reading what it was
     * waiting for.
     *
     * A platform thread has exactly this call at the end of threadRunner, for
     * exactly this reason. A virtual thread needs it just as much: it is a Java
     * thread of control as far as the collector is concerned, and it has to
     * announce its own death.
     */
    /*
     * Say "not running" here, but do NOT retire the state here.
     *
     * The collector waits on threadActive for any lightweightThread, so clearing
     * it closes the window between this virtual thread finishing and its host
     * getting round to freeing it. Retiring the state is a different matter:
     * markDeadThread calls collectThreadResources, which frees threadObjectStack
     * -- the Java stack this function is still standing on. Doing it here killed
     * the process inside the first burst. It belongs on the host, after the
     * switch back, which is where freeImpl runs.
     */
    mine->threadActive = JAVA_FALSE;
}

/* Instrumentation: a virtual thread that is never resumed again is still
 * registered with the collector, and if it is marked active the collector waits
 * for a safepoint it can never reach. Counting created against freed says
 * whether that is happening without having to infer it from thread states. */
static _Atomic long cn1VtCreated = 0;
static _Atomic long cn1VtFreed = 0;
static _Atomic long cn1VtFinished = 0;

static void cn1VtReport(const char* why) {
    fprintf(stderr, "[CN1-VT] %s created=%ld finished=%ld freed=%ld live=%ld\n", why,
            atomic_load(&cn1VtCreated), atomic_load(&cn1VtFinished),
            atomic_load(&cn1VtFreed),
            atomic_load(&cn1VtCreated) - atomic_load(&cn1VtFreed));
}

JAVA_VOID com_codename1_backend_VirtualThread_reportImpl__(CODENAME_ONE_THREAD_STATE) {
    cn1VtReport("report");
}

JAVA_LONG com_codename1_backend_VirtualThread_createImpl___int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_INT stackBytes) {
    struct cn1BackendVtArg* a;
    struct cn1VirtualThread* vt;
    a = (struct cn1BackendVtArg*)malloc(sizeof(struct cn1BackendVtArg));
    if(a == 0) {
        return 0;
    }
    a->fd = fd;
    vt = cn1SpawnVirtualThread(cn1BackendVtBody, a, (size_t)stackBytes);
    if(vt == 0) {
        static int reported = 0;
        if(!reported) {
            reported = 1;
            fprintf(stderr, "[CN1-VT] spawn failed (stackBytes=%d, errno=%d %s)\n",
                    (int)stackBytes, errno, strerror(errno));
        }
        free(a);
        return 0;
    }
    atomic_fetch_add(&cn1VtCreated, 1);
    return (JAVA_LONG)(intptr_t)vt;
}

/* True once the connection is done with. False means it parked and is waiting
 * for its descriptor to become readable again. */
/*
 * 0 finished, 1 parked waiting for its descriptor, 2 yielded but RUNNABLE.
 *
 * The third answer is the one that matters. A virtual thread that gave up its
 * host inside the collector's allocation backpressure is not waiting for bytes:
 * handing its descriptor to the poller waits for a client that is itself waiting
 * for the response this virtual thread still owes it, and neither side ever
 * moves. It has to go back on a run queue instead.
 */
JAVA_INT com_codename1_backend_VirtualThread_resumeImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    struct cn1VirtualThread* vt = (struct cn1VirtualThread*)(intptr_t)handle;
    if(vt == 0) {
        return 0;
    }
    cn1VirtualThreadSetYieldReason(CN1_VT_YIELD_IO);   /* the default for a plain park */
    cn1VirtualThreadResume(vt);
    if(cn1VirtualThreadFinished(vt)) {
        atomic_fetch_add(&cn1VtFinished, 1);
        return 0;
    }
    return cn1VirtualThreadYieldReason(vt) == CN1_VT_YIELD_RUNNABLE ? 2 : 1;
}

/* The descriptor this virtual thread serves. The run queue holds handles, and a
 * handle that comes back from the queue has to be matched to its slot again. */
JAVA_INT com_codename1_backend_VirtualThread_descriptorImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    struct cn1VirtualThread* vt = (struct cn1VirtualThread*)(intptr_t)handle;
    struct cn1BackendVtArg* a;
    if(vt == 0) {
        return -1;
    }
    a = (struct cn1BackendVtArg*)cn1VirtualThreadArg(vt);
    return a == 0 ? -1 : a->fd;
}

JAVA_VOID com_codename1_backend_VirtualThread_freeImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    struct cn1VirtualThread* vt = (struct cn1VirtualThread*)(intptr_t)handle;
    struct ThreadLocalData* victim;
    if(vt == 0) {
        return;
    }
    /*
     * RETIRE THE VIRTUAL THREAD'S VM STATE, and this is the whole bug fixed.
     *
     * That state is registered in allThreads and flagged lightweightThread, and
     * the collector's stop-the-world does this for every such entry:
     *
     *     t->threadBlockedByGC = JAVA_TRUE;
     *     while(t->threadActive) { usleep(500); }      // no timeout
     *
     * A finished virtual thread never runs again, so if its state stays in that
     * list the collector waits on it for ever: every cycle after the first
     * connection closes fails to complete, the allocation pacing never releases,
     * and the server settles at a few hundred requests a second while looking
     * completely idle -- 8% of a CPU, no crash, no spin. A platform thread makes
     * exactly this call at the end of threadRunner; a virtual thread is a Java
     * thread of control to the collector and owes it the same announcement.
     *
     * Here rather than at the end of the body because markDeadThread frees the
     * thread's object stack, and the body is still standing on it.
     */
    victim = (struct ThreadLocalData*)cn1VirtualThreadState(vt);
    if(victim != 0) {
        cn1VirtualThreadSetState(vt, 0);
        markDeadThread(victim);
        /*
         * ASK FOR THE RELEASE. markDeadThread only QUEUES the state: it sets
         * gcQueuedForDrain and hands the TLD to cn1DrainDeadThreadPending, which
         * migrates the pending allocations and then frees the TLD only if
         * gcReleaseRequested is set. Nothing else sets it for a virtual thread --
         * an OS thread gets it from the Thread object's finalizer, and
         * cn1RetireVirtualThread (the VM's own retirement path, which this native
         * duplicates) sets it right here for exactly this reason.
         *
         * Without it the drain runs, clears gcQueuedForDrain, and walks away
         * leaving the TLD allocated for ever. That is ~68KB per connection --
         * callStack arrays ~50KB, pendingHeapAllocations ~27KB, the try-block
         * array ~15KB, all malloc'd -- and it never comes back: 900 closed
         * connections took resident memory from 3MB to 65MB, and 249 collections
         * returned none of it, because every one of those drains found the flag
         * clear.
         *
         * Deferred rather than freed here, and that is deliberate: codenameOneGCMark
         * copies each ThreadLocalData* out of allThreads under the critical section
         * and dereferences it outside, so a mark already past that copy still holds
         * this pointer. The drain runs at the start of the next mark, which is the
         * one point where no collector iteration can.
         */
        lockCriticalSection();
        victim->gcReleaseRequested = JAVA_TRUE;
        unlockCriticalSection();
    }
    /* The argument block outlives the body, so it is freed here rather than at
     * the end of the body: the body's stack frame is gone by then. */
    free(cn1VirtualThreadArg(vt));
    cn1VirtualThreadFree(vt);
    atomic_fetch_add(&cn1VtFreed, 1);
}

/*
 * Whether this build actually has the context switch, so the server can DEFAULT
 * to virtual threads without breaking a target that lacks them.
 *
 * cn1_virtual_thread.h compiles the real implementation only on non-Windows
 * aarch64/x86_64; everywhere else every entry point is a stub and
 * cn1SpawnVirtualThread returns 0. A default of "virtual threads" that did not
 * ask this would drop every connection on those targets rather than fall back.
 */
JAVA_BOOLEAN com_codename1_backend_VirtualThread_supportedImpl___R_boolean(CODENAME_ONE_THREAD_STATE) {
#ifdef CN1_VIRTUAL_THREADS
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_BOOLEAN com_codename1_backend_VirtualThread_isVirtualImpl___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return cn1VirtualThreadCurrent() != 0 ? JAVA_TRUE : JAVA_FALSE;
}

/*
 * Give up the host thread without waiting for anything.
 *
 * A virtual thread parks by itself when the bytes it wants have not arrived, and
 * under a load generator that always has the next request queued that never
 * happens -- so a virtual thread would hold its host for as long as the client
 * kept talking, and with fewer hosts than connections the rest starve. Removing
 * the burst cap entirely produced exactly that: two hosts serving two of sixty
 * four connections. The cap has to stay; what was wrong was closing the
 * connection to honour it rather than stepping aside.
 */
JAVA_VOID com_codename1_backend_VirtualThread_yieldImpl__(CODENAME_ONE_THREAD_STATE) {
    if(cn1VirtualThreadCurrent() != 0) {
        // Marked inactive across the switch for the same reason the keep-alive
        // park is: a virtual thread that is not on a host cannot answer the
        // collector, and a collector waiting for it stops the whole server.
        CN1_YIELD_THREAD;
        cn1VirtualThreadYield();
        CN1_RESUME_THREAD;
    }
}

/*
 * Cores available to this process.
 *
 * Virtual-thread mode needs it because the host count must track the cores and
 * not the expected concurrency: concurrency comes from the virtual threads, so
 * a host per core is enough, and more than that is actively harmful. Measured on
 * two pinned cores, 16 hosts served 117 requests where 2 served 257297 -- the
 * host threads simply contend for the cores the server needs. Unpinned, where
 * the machine has cores to spare, every host count from 2 to 32 behaves and the
 * difference disappears, which is why this has to be read at runtime rather than
 * guessed at build time.
 */
JAVA_INT com_codename1_backend_ServerSocket_availableProcessorsImpl___R_int(CODENAME_ONE_THREAD_STATE) {
#ifdef _WIN32
    return 1;
#else
    long n = sysconf(_SC_NPROCESSORS_ONLN);
    return n > 0 ? (JAVA_INT)n : 1;
#endif
}
