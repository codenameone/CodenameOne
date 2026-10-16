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

/* POSIX sockets. Tokens, rather than exposed heap addresses or descriptors, make
 * close/read/accept races safe: operations retain a peer while the registry owns
 * another reference. Shutdown wakes blocking I/O; the last operation closes the
 * descriptor, so it cannot be recycled underneath a blocked syscall. */
#define _GNU_SOURCE
#include "cn1_linux.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <limits.h>
#include <pthread.h>
#include <netdb.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <netinet/in.h>
#include <arpa/inet.h>

extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);
extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
#define CN1_SOCKET_KEEP_ALIVE(obj) __asm__ __volatile__("" : : "r"(obj))

typedef struct CN1Socket {
    JAVA_LONG token;
    int fd, lastError, connected, refs, port;
    struct CN1Socket *next, *nextListener;
} CN1Socket;

#define CN1_SOCKET_BUCKETS 256
static pthread_mutex_t cn1SocketMutex = PTHREAD_MUTEX_INITIALIZER;
static CN1Socket* cn1Sockets[CN1_SOCKET_BUCKETS];
static CN1Socket* cn1Listeners;
static JAVA_LONG cn1NextSocketToken = 1;

/* All registry helpers require cn1SocketMutex. */
static CN1Socket* cn1SocketNew(int fd, int listenerPort) {
    CN1Socket* s;
    unsigned bucket;
    if (cn1NextSocketToken == LLONG_MAX) {
        close(fd);
        return NULL;
    }
    s = (CN1Socket*) calloc(1, sizeof(CN1Socket));
    if (!s) {
        close(fd);
        return NULL;
    }
    s->token = cn1NextSocketToken++;
    s->fd = fd;
    s->connected = 1;
    s->refs = 1;
    s->port = listenerPort;
    bucket = (unsigned) (s->token % CN1_SOCKET_BUCKETS);
    s->next = cn1Sockets[bucket];
    cn1Sockets[bucket] = s;
    if (listenerPort != 0) {
        s->nextListener = cn1Listeners;
        cn1Listeners = s;
    }
    return s;
}

static CN1Socket* cn1SocketFind(JAVA_LONG token) {
    CN1Socket* s;
    if (token <= 0) {
        return NULL;
    }
    for (s = cn1Sockets[token % CN1_SOCKET_BUCKETS]; s; s = s->next) {
        if (s->token == token) {
            return s;
        }
    }
    return NULL;
}

static void cn1SocketReleaseLocked(CN1Socket* s) {
    if (--s->refs == 0) {
        close(s->fd);
        free(s);
    }
}

static CN1Socket* cn1SocketAcquire(JAVA_LONG token) {
    CN1Socket* s;
    pthread_mutex_lock(&cn1SocketMutex);
    s = cn1SocketFind(token);
    if (s) {
        s->refs++;
    }
    pthread_mutex_unlock(&cn1SocketMutex);
    return s;
}

static void cn1SocketRelease(CN1Socket* s) {
    pthread_mutex_lock(&cn1SocketMutex);
    cn1SocketReleaseLocked(s);
    pthread_mutex_unlock(&cn1SocketMutex);
}

static void cn1SocketCloseLocked(CN1Socket* s) {
    CN1Socket** link = &cn1Sockets[s->token % CN1_SOCKET_BUCKETS];
    while (*link != s) {
        link = &(*link)->next;
    }
    *link = s->next;
    if (s->port != 0) {
        link = &cn1Listeners;
        while (*link != s) {
            link = &(*link)->nextListener;
        }
        *link = s->nextListener;
    }
    s->connected = 0;
    shutdown(s->fd, SHUT_RDWR);
    cn1SocketReleaseLocked(s);
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_socketConnect___java_lang_String_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT host, JAVA_INT port, JAVA_INT timeoutMillis) {
    const char* h = host == JAVA_NULL ? NULL : stringToUTF8(threadStateData, host);
    struct addrinfo hints, *res = NULL, *it;
    char portStr[16];
    int fd = -1;
    JAVA_LONG token = 0;
    CN1Socket* s;
    (void) timeoutMillis;
    if (!h || port < 1 || port > 65535) {
        return 0;
    }
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    snprintf(portStr, sizeof(portStr), "%d", (int) port);
    CN1_YIELD_THREAD;
    if (getaddrinfo(h, portStr, &hints, &res) == 0) {
        for (it = res; it; it = it->ai_next) {
            fd = socket(it->ai_family, it->ai_socktype | SOCK_CLOEXEC, it->ai_protocol);
            if (fd < 0) {
                continue;
            }
            if (connect(fd, it->ai_addr, it->ai_addrlen) == 0) {
                break;
            }
            close(fd);
            fd = -1;
        }
        freeaddrinfo(res);
    }
    CN1_RESUME_THREAD;
    CN1_SOCKET_KEEP_ALIVE(host);
    if (fd < 0) {
        return 0;
    }
    pthread_mutex_lock(&cn1SocketMutex);
    s = cn1SocketNew(fd, 0);
    if (s) {
        token = s->token;
    }
    pthread_mutex_unlock(&cn1SocketMutex);
    return token;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_socketListenLoopback___int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT port) {
    CN1Socket *listener, *client;
    int fd, accepted;
    JAVA_LONG token = 0;
    if (port < 1 || port > 65535) {
        return 0;
    }
    pthread_mutex_lock(&cn1SocketMutex);
    for (listener = cn1Listeners; listener && listener->port != port; listener = listener->nextListener) {}
    if (!listener) {
        struct sockaddr_in address;
        int reuse = 1;
        fd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        if (fd >= 0) {
            setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &reuse, sizeof(reuse));
            memset(&address, 0, sizeof(address));
            address.sin_family = AF_INET;
            address.sin_port = htons((unsigned short) port);
            address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
            if (bind(fd, (struct sockaddr*) &address, sizeof(address)) != 0 || listen(fd, 16) != 0) {
                close(fd);
            } else {
                listener = cn1SocketNew(fd, port);
            }
        }
    }
    if (listener) {
        listener->refs++;
    }
    pthread_mutex_unlock(&cn1SocketMutex);
    if (!listener) {
        return 0;
    }
    CN1_YIELD_THREAD;
    do {
        accepted = accept4(listener->fd, NULL, NULL, SOCK_CLOEXEC);
    } while (accepted < 0 && errno == EINTR);
    CN1_RESUME_THREAD;
    pthread_mutex_lock(&cn1SocketMutex);
    if (accepted >= 0) {
        if (listener->connected) {
            client = cn1SocketNew(accepted, 0);
            if (client) {
                token = client->token;
            }
        } else {
            close(accepted);
        }
    }
    cn1SocketReleaseLocked(listener);
    pthread_mutex_unlock(&cn1SocketMutex);
    return token;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_socketStopListening___int(CODENAME_ONE_THREAD_STATE, JAVA_INT port) {
    CN1Socket* s;
    pthread_mutex_lock(&cn1SocketMutex);
    for (s = cn1Listeners; s && s->port != port; s = s->nextListener) {}
    if (s) {
        cn1SocketCloseLocked(s);
    }
    pthread_mutex_unlock(&cn1SocketMutex);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_socketRead___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG socket, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    CN1Socket* s;
    ssize_t n;
    char* data;
    int savedError;
    if (buffer == JAVA_NULL || offset < 0 || length < 0 || offset > CN1_ARRAY_LENGTH(buffer) - length) {
        return -1;
    }
    s = cn1SocketAcquire(socket);
    if (!s) {
        return -1;
    }
    if (length == 0) {
        cn1SocketRelease(s);
        return 0;
    }
    data = (char*) CN1_ARRAY_DATA(buffer);
    CN1_YIELD_THREAD;
    do {
        n = recv(s->fd, data + offset, (size_t) length, 0);
    } while (n < 0 && errno == EINTR);
    savedError = n < 0 ? errno : 0;
    CN1_RESUME_THREAD;
    CN1_SOCKET_KEEP_ALIVE(buffer);
    pthread_mutex_lock(&cn1SocketMutex);
    if (n <= 0) {
        s->lastError = savedError;
        s->connected = 0;
    }
    cn1SocketReleaseLocked(s);
    pthread_mutex_unlock(&cn1SocketMutex);
    return n <= 0 ? -1 : (JAVA_INT) n;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_socketWrite___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG socket, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    CN1Socket* s;
    int written = 0, savedError = 0;
    char* data;
    if (buffer == JAVA_NULL || offset < 0 || length < 0 || offset > CN1_ARRAY_LENGTH(buffer) - length) {
        return -1;
    }
    s = cn1SocketAcquire(socket);
    if (!s) {
        return -1;
    }
    data = (char*) CN1_ARRAY_DATA(buffer);
    CN1_YIELD_THREAD;
    while (written < length) {
        ssize_t n = send(s->fd, data + offset + written,
                (size_t) (length - written), MSG_NOSIGNAL);
        if (n < 0 && errno == EINTR) {
            continue;
        }
        if (n <= 0) {
            savedError = n < 0 ? errno : EPIPE;
            break;
        }
        written += (int) n;
    }
    CN1_RESUME_THREAD;
    CN1_SOCKET_KEEP_ALIVE(buffer);
    pthread_mutex_lock(&cn1SocketMutex);
    if (savedError) {
        s->lastError = savedError;
        s->connected = 0;
    }
    cn1SocketReleaseLocked(s);
    pthread_mutex_unlock(&cn1SocketMutex);
    return written == 0 && savedError ? -1 : written;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_socketAvailable___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG socket) {
    CN1Socket* s = cn1SocketAcquire(socket);
    int available = 0;
    if (s) {
        if (ioctl(s->fd, FIONREAD, &available) != 0) {
            available = 0;
        }
        cn1SocketRelease(s);
    }
    return available;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_socketClose___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG socket) {
    CN1Socket* s;
    pthread_mutex_lock(&cn1SocketMutex);
    s = cn1SocketFind(socket);
    if (s) {
        cn1SocketCloseLocked(s);
    }
    pthread_mutex_unlock(&cn1SocketMutex);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_socketErrorCode___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG socket) {
    CN1Socket* s;
    int result;
    pthread_mutex_lock(&cn1SocketMutex);
    s = cn1SocketFind(socket);
    result = s ? s->lastError : -1;
    pthread_mutex_unlock(&cn1SocketMutex);
    return result;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_socketConnected___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG socket) {
    CN1Socket* s;
    int connected;
    pthread_mutex_lock(&cn1SocketMutex);
    s = cn1SocketFind(socket);
    connected = s && s->connected;
    pthread_mutex_unlock(&cn1SocketMutex);
    return connected ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_getHostOrIP___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    char name[256];
    if (gethostname(name, sizeof(name)) == 0) {
        name[sizeof(name) - 1] = 0;
        return newStringFromCString(threadStateData, name);
    }
    return newStringFromCString(threadStateData, "localhost");
}
