/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
 * Raw TCP sockets for the native Codename One Linux port, backed by the POSIX
 * BSD socket API. The socket peer is a small heap struct holding the fd and the
 * last errno; the bridge mirrors the Codename One Socket SPI (connect / blocking
 * read+write / available / close).
 */

#define _GNU_SOURCE
#include "cn1_linux.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <netdb.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>

extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);
extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);

/* Sink that forces a socket buffer object to stay live across a parked blocking read
 * so the conservative GC cannot sweep it mid-I/O (see socketRead). */
static volatile void* cn1SocketReadKeepAlive;

typedef struct {
    int fd;
    int lastError;
    int connected;
} CN1Socket;

JAVA_LONG com_codename1_impl_linux_LinuxNative_socketConnect___java_lang_String_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT host, JAVA_INT port, JAVA_INT timeoutMillis) {
    const char* h = host == JAVA_NULL ? 0 : stringToUTF8(threadStateData, host);
    struct addrinfo hints;
    struct addrinfo* res = 0;
    struct addrinfo* it;
    char portStr[16];
    int fd = -1;
    CN1Socket* s;
    (void) timeoutMillis;
    if (!h) {
        return 0;
    }
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    snprintf(portStr, sizeof(portStr), "%d", (int) port);
    if (getaddrinfo(h, portStr, &hints, &res) != 0) {
        return 0;
    }
    /* connect() blocks during the TCP handshake; yield so a GC mark isn't stalled. */
    CN1_YIELD_THREAD;
    for (it = res; it != 0; it = it->ai_next) {
        fd = socket(it->ai_family, it->ai_socktype, it->ai_protocol);
        if (fd < 0) {
            continue;
        }
        if (connect(fd, it->ai_addr, it->ai_addrlen) == 0) {
            break;
        }
        close(fd);
        fd = -1;
    }
    CN1_RESUME_THREAD;
    freeaddrinfo(res);
    if (fd < 0) {
        return 0;
    }
    s = (CN1Socket*) calloc(1, sizeof(CN1Socket));
    s->fd = fd;
    s->connected = 1;
    return (JAVA_LONG) (intptr_t) s;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_socketRead___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG socket, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    CN1Socket* s = (CN1Socket*) (intptr_t) socket;
    char* data;
    ssize_t n;
    if (!s || s->fd < 0 || buffer == JAVA_NULL || length <= 0) {
        return -1;
    }
    data = (char*) (*(JAVA_ARRAY) buffer).data;
    /* read() can block indefinitely (e.g. the cn1ss WebSocket reader waiting on
     * the server). Mark the thread inactive across the blocking call so the
     * concurrent GC -- which spins waiting for every lightweight thread to pause
     * before it can traverse stacks -- is not deadlocked by a thread parked in a
     * syscall. Every other CN1 port's blocking I/O does the same. */
    CN1_YIELD_THREAD;
    n = read(s->fd, data + offset, (size_t) length);
    CN1_RESUME_THREAD;
    /* Keep the buffer array object reachable across the parked read: only `data` (an
     * interior pointer) is used, so the optimizer may drop `buffer` and the concurrent GC
     * -- scanning this parked thread -- can sweep it mid-read (a use-after-free; see the
     * Windows port where this manifested on the cn1ss WebSocket reader). Force liveness. */
    cn1SocketReadKeepAlive = (volatile void*) buffer;
    if (n <= 0) {
        s->lastError = n < 0 ? errno : 0;
        if (n == 0) {
            s->connected = 0;
        }
        return -1;
    }
    return (JAVA_INT) n;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_socketWrite___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG socket, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    CN1Socket* s = (CN1Socket*) (intptr_t) socket;
    char* data;
    int written = 0;
    if (!s || s->fd < 0 || buffer == JAVA_NULL || length <= 0) {
        return -1;
    }
    data = (char*) (*(JAVA_ARRAY) buffer).data;
    /* Yield to the concurrent GC across the (potentially blocking) write loop -- a
     * thread parked in write() must not stall a GC mark (see socketRead). */
    CN1_YIELD_THREAD;
    while (written < length) {
        ssize_t n = write(s->fd, data + offset + written, (size_t) (length - written));
        if (n <= 0) {
            s->lastError = errno;
            CN1_RESUME_THREAD;
            return written > 0 ? written : -1;
        }
        written += (int) n;
    }
    CN1_RESUME_THREAD;
    return written;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_socketAvailable___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG socket) {
    CN1Socket* s = (CN1Socket*) (intptr_t) socket;
    int avail = 0;
    if (!s || s->fd < 0) {
        return 0;
    }
    if (ioctl(s->fd, FIONREAD, &avail) != 0) {
        return 0;
    }
    return avail;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_socketClose___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG socket) {
    CN1Socket* s = (CN1Socket*) (intptr_t) socket;
    if (!s) {
        return;
    }
    if (s->fd >= 0) {
        close(s->fd);
        s->fd = -1;
    }
    s->connected = 0;
    free(s);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_socketErrorCode___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG socket) {
    CN1Socket* s = (CN1Socket*) (intptr_t) socket;
    return s ? s->lastError : -1;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_socketConnected___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG socket) {
    CN1Socket* s = (CN1Socket*) (intptr_t) socket;
    return (s && s->connected && s->fd >= 0) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_getHostOrIP___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    char name[256];
    if (gethostname(name, sizeof(name)) == 0) {
        name[sizeof(name) - 1] = 0;
        return newStringFromCString(threadStateData, name);
    }
    return newStringFromCString(threadStateData, "localhost");
}
