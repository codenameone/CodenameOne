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
 * Blocking TCP client sockets for the clean (server-side) target.
 *
 * This is deliberately NOT the Linux port's cn1_linux_socket.c: that one is
 * reached through CodenameOneImplementation, which a server-side binary does not
 * have. Same system calls, no platform layer.
 *
 * The handle is fd+1 rather than a heap struct, so 0 is "not connected" and
 * nothing has to be freed on a failed connect -- a leak here would be a leak per
 * request.
 *
 * Every blocking call is bracketed with CN1_YIELD_THREAD / CN1_RESUME_THREAD.
 * Without that, a thread parked in recv() is a thread the concurrent collector
 * cannot mark past, so one idle connection would stall GC for the whole process.
 */
#include "cn1_globals.h"
#include <stdio.h>
#include <string.h>
#include <errno.h>

#ifdef _WIN32
#include <winsock2.h>
#include <ws2tcpip.h>
#define CN1_CLOSE_SOCKET closesocket
typedef int cn1_socklen;
#else
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <unistd.h>
#include <fcntl.h>
#include <poll.h>
#include <signal.h>
#define CN1_CLOSE_SOCKET close
typedef socklen_t cn1_socklen;
#endif

static int cn1BackendFd(JAVA_LONG handle) {
    return handle <= 0 ? -1 : (int)(handle - 1);
}

static int cn1SetNonBlocking(int fd, int on) {
#ifdef _WIN32
    u_long mode = on ? 1 : 0;
    return ioctlsocket(fd, FIONBIO, &mode) == 0 ? 0 : -1;
#else
    int flags = fcntl(fd, F_GETFL, 0);
    if(flags < 0) {
        return -1;
    }
    flags = on ? (flags | O_NONBLOCK) : (flags & ~O_NONBLOCK);
    return fcntl(fd, F_SETFL, flags) == 0 ? 0 : -1;
#endif
}

static int cn1ConnectPending(void) {
#ifdef _WIN32
    return WSAGetLastError() == WSAEWOULDBLOCK;
#else
    return errno == EINPROGRESS;
#endif
}

/*
 * connect() that gives up when the caller said to.
 *
 * A blocking connect ignores the timeout entirely and waits out the OS TCP
 * timeout, which is minutes when an address silently drops packets rather than
 * refusing. A database URL's ten-second default then means nothing on the device
 * while meaning exactly ten seconds in the JavaSE runtime, so the same
 * misconfiguration looks like a slow start in development and a hung process in
 * production.
 *
 * The socket goes back to blocking before returning: every read and write after
 * this expects that. The deadline is per address, so a host resolving to several
 * can take the timeout once for each -- which is the point, since the reachable
 * one is usually not the first.
 */
/* MSG_NOSIGNAL where the platform has it, as the listener's write path uses. */
#ifndef _WIN32
#ifdef MSG_NOSIGNAL
#define CN1_OUT_SEND_FLAGS MSG_NOSIGNAL
#else
#define CN1_OUT_SEND_FLAGS 0
#endif

/*
 * Ignores SIGPIPE, whose default action is to KILL the process.
 *
 * The listener does this when it binds, and Signals.installShutdownHandler does
 * it too, but a packaged runtime need do neither: LambdaRuntime.run() only makes
 * outbound connections. In that process a database or Runtime API peer that went
 * away between one write and the next took the whole runtime down instead of
 * raising an IOException. Done on connect because it has to precede any write,
 * and it covers the TLS client as well -- SSL_write goes through write(2), where
 * MSG_NOSIGNAL cannot reach. Idempotent, so calling it per connection is free.
 */
static void cn1IgnoreSigPipe(void) {
    signal(SIGPIPE, SIG_IGN);
}
#else
#define CN1_OUT_SEND_FLAGS 0
static void cn1IgnoreSigPipe(void) {
}
#endif

static int cn1ConnectWithTimeout(int fd, const struct sockaddr* addr, cn1_socklen len,
                                 int timeoutMillis) {
    int err = 0;
    cn1_socklen errLen = (cn1_socklen)sizeof(err);
    int rc;
    if(timeoutMillis <= 0 || cn1SetNonBlocking(fd, 1) != 0) {
        /* No deadline asked for, or the socket refused to go non-blocking: the
           blocking connect is still the right answer, just without a deadline. */
        return connect(fd, addr, len) == 0 ? 0 : -1;
    }
    rc = connect(fd, addr, len);
    if(rc != 0) {
        if(!cn1ConnectPending()) {
            cn1SetNonBlocking(fd, 0);
            return -1;
        }
#ifdef _WIN32
        {
            fd_set writable;
            struct timeval tv;
            FD_ZERO(&writable);
            FD_SET((SOCKET)fd, &writable);
            tv.tv_sec = timeoutMillis / 1000;
            tv.tv_usec = (timeoutMillis % 1000) * 1000;
            rc = select(0, 0, &writable, 0, &tv);
        }
#else
        {
            /* poll rather than select: a server with many open connections hands
               out descriptors above FD_SETSIZE, and select is undefined there. */
            struct pollfd waiting;
            waiting.fd = fd;
            waiting.events = POLLOUT;
            waiting.revents = 0;
            do {
                rc = poll(&waiting, 1, timeoutMillis);
            } while(rc < 0 && errno == EINTR);
        }
#endif
        if(rc <= 0) {
            cn1SetNonBlocking(fd, 0);
            return -1;                  /* timed out, or the wait itself failed */
        }
        if(getsockopt(fd, SOL_SOCKET, SO_ERROR, (char*)&err, &errLen) != 0 || err != 0) {
            cn1SetNonBlocking(fd, 0);
            return -1;
        }
    }
    cn1SetNonBlocking(fd, 0);
    return 0;
}

JAVA_LONG com_codename1_backend_Tcp_connectImpl___java_lang_String_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT host, JAVA_INT port, JAVA_INT timeoutMillis) {
    struct addrinfo hints;
    struct addrinfo* res = 0;
    struct addrinfo* it;
    char portStr[16];
    int fd = -1;
    const char* h = host == JAVA_NULL ? 0 : stringToUTF8(threadStateData, host);
    if(!h) {
        return 0;
    }
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    snprintf(portStr, sizeof(portStr), "%d", (int)port);
    cn1IgnoreSigPipe();
    /* Yielded BEFORE the resolver, not after it. getaddrinfo blocks -- for the
       full resolver timeout when DNS is slow or unreachable -- and the VM counted
       this thread as running throughout, so a collection waited for it and every
       unrelated request waited with it. The timeoutMillis argument does not cover
       this either: it starts once there is an address to connect to.

       Safe across a yield because `h` is a copy in the thread state's own utf8
       buffer, which stringToUTF8 mallocs; nothing here holds a heap pointer. */
    CN1_YIELD_THREAD;
    if(getaddrinfo(h, portStr, &hints, &res) != 0) {
        CN1_RESUME_THREAD;
        return 0;
    }
    for(it = res ; it != 0 ; it = it->ai_next) {
        fd = (int)socket(it->ai_family, it->ai_socktype, it->ai_protocol);
        if(fd < 0) {
            continue;
        }
        if(cn1ConnectWithTimeout(fd, it->ai_addr, (cn1_socklen)it->ai_addrlen,
                                 (int)timeoutMillis) == 0) {
            break;
        }
        CN1_CLOSE_SOCKET(fd);
        fd = -1;
    }
    CN1_RESUME_THREAD;
    freeaddrinfo(res);
    if(fd < 0) {
        return 0;
    }
    return (JAVA_LONG)fd + 1;
}

JAVA_INT com_codename1_backend_Tcp_readImpl___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    int fd = cn1BackendFd(handle);
    JAVA_ARRAY_BYTE* data;
    long n;
    if(fd < 0 || buffer == JAVA_NULL) {
        return -2;
    }
    data = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)buffer)->data;
    /*
     * This blocks, and on a virtual thread it blocks the HOST.
     *
     * CN1_YIELD_THREAD releases the thread to the COLLECTOR; it is not a park. The
     * server's own readImpl parks on EAGAIN because its descriptor is registered in
     * a host's poller, which is what resumes it. An outbound socket is in no poller,
     * so there is nothing to wake it and yielding here would spin.
     *
     * The consequence is real: with one host per core, as many concurrent slow
     * database reads as there are cores occupy every host, and unrelated HTTP
     * connections stop being served. Making this park means giving outbound
     * descriptors the same poller registration inbound ones have -- a scheduler
     * feature, not a local change -- and until that exists the guide says so under
     * "Limits worth knowing" rather than the mode quietly not holding.
     */
    CN1_YIELD_THREAD;
    n = (long)recv(fd, (char*)&data[offset], (size_t)length, 0);
    CN1_RESUME_THREAD;
    if(n == 0) {
        return -1; /* orderly shutdown by the peer */
    }
    if(n < 0) {
        return -2;
    }
    return (JAVA_INT)n;
}

JAVA_INT com_codename1_backend_Tcp_writeImpl___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    int fd = cn1BackendFd(handle);
    JAVA_ARRAY_BYTE* data;
    JAVA_INT written = 0;
    if(fd < 0 || buffer == JAVA_NULL) {
        return -1;
    }
    data = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)buffer)->data;
    CN1_YIELD_THREAD;
    /* send() may accept less than asked; loop so the Java side can treat a short
       write as a hard failure rather than having to retry it itself. */
    while(written < length) {
        long n = (long)send(fd, (const char*)&data[offset + written],
                (size_t)(length - written), CN1_OUT_SEND_FLAGS);
        if(n <= 0) {
            CN1_RESUME_THREAD;
            return -1;
        }
        written += (JAVA_INT)n;
    }
    CN1_RESUME_THREAD;
    return written;
}

JAVA_INT com_codename1_backend_Tcp_closeImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    int fd = cn1BackendFd(handle);
    if(fd < 0) {
        return 0;
    }
    return CN1_CLOSE_SOCKET(fd) == 0 ? 0 : -1;
}
