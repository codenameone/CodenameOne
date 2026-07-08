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
 * Raw TCP client sockets for the Windows port, over WinSock2. Backs the
 * com.codename1.io.Socket SPI (and, through it, com.codename1.io.WebSocket).
 * A socket peer is a CN1Socket* carrying the WinSock SOCKET and the last error;
 * reads block (recv) like every other Codename One port, the EDT/worker thread
 * that owns the connection drives them. winsock2.h must precede windows.h, so
 * it is included before cn1_windows.h.
 */

#ifdef _WIN32

#include <winsock2.h>
#include <ws2tcpip.h>
#include "cn1_windows.h"
#include <stdlib.h>
#include <string.h>

/* Sink that forces a socket read/write buffer object to stay live across a parked
 * blocking call so the conservative GC can't sweep it mid-I/O (see socketRead). */
static volatile void* cn1SocketReadKeepAlive;

typedef struct CN1Socket {
    SOCKET s;
    int errorCode;
} CN1Socket;

static volatile LONG cn1SockStarted = 0;

static void cn1SockEnsureStartup(void) {
    if (InterlockedCompareExchange(&cn1SockStarted, 1, 0) == 0) {
        WSADATA wsa;
        WSAStartup(MAKEWORD(2, 2), &wsa);
    }
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_socketConnect___java_lang_String_int_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3) {
    const char* host;
    char portStr[16];
    struct addrinfo hints;
    struct addrinfo* result = NULL;
    struct addrinfo* ai;
    SOCKET sock = INVALID_SOCKET;
    CN1Socket* peer;

    if (__cn1Arg1 == JAVA_NULL) {
        return 0;
    }
    cn1SockEnsureStartup();
    host = stringToUTF8(threadStateData, __cn1Arg1);
    wsprintfA(portStr, "%d", (int) __cn1Arg2);

    ZeroMemory(&hints, sizeof(hints));
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;
    {
        int gai;
        CN1_YIELD_THREAD;
        gai = getaddrinfo(host, portStr, &hints, &result);
        CN1_RESUME_THREAD;
        if (gai != 0) {
            cn1WindowsLog("socketConnect: getaddrinfo failed");
            return 0;
        }
    }

    for (ai = result; ai != NULL; ai = ai->ai_next) {
        sock = socket(ai->ai_family, ai->ai_socktype, ai->ai_protocol);
        if (sock == INVALID_SOCKET) {
            continue;
        }
        if (__cn1Arg3 > 0) {
            /* Non-blocking connect with a select() timeout, then restore
             * blocking mode for the synchronous read/write the port uses. */
            u_long nonblock = 1;
            ioctlsocket(sock, FIONBIO, &nonblock);
            connect(sock, ai->ai_addr, (int) ai->ai_addrlen);
            fd_set writable;
            struct timeval tv;
            FD_ZERO(&writable);
            FD_SET(sock, &writable);
            tv.tv_sec = __cn1Arg3 / 1000;
            tv.tv_usec = (__cn1Arg3 % 1000) * 1000;
            /* select() blocks up to the connect timeout; yield the thread state
             * so a concurrent GC stop-the-world is not held up waiting for this
             * (lightweight) connect thread -- otherwise the whole VM can stall,
             * and the connect never completes within the suite. */
            int sel;
            CN1_YIELD_THREAD;
            sel = select(0, NULL, &writable, NULL, &tv);
            CN1_RESUME_THREAD;
            if (sel == 1) {
                u_long blocking = 0;
                ioctlsocket(sock, FIONBIO, &blocking);
                break;
            }
            closesocket(sock);
            sock = INVALID_SOCKET;
        } else {
            if (connect(sock, ai->ai_addr, (int) ai->ai_addrlen) == 0) {
                break;
            }
            closesocket(sock);
            sock = INVALID_SOCKET;
        }
    }
    freeaddrinfo(result);

    if (sock == INVALID_SOCKET) {
        cn1WindowsLog("socketConnect: connect failed");
        return 0;
    }
    peer = (CN1Socket*) malloc(sizeof(CN1Socket));
    if (peer == NULL) {
        closesocket(sock);
        return 0;
    }
    peer->s = sock;
    peer->errorCode = 0;
    return (JAVA_LONG) (intptr_t) peer;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_socketRead___long_byte_1ARRAY_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4) {
    CN1Socket* peer = (CN1Socket*) (intptr_t) __cn1Arg1;
    JAVA_ARRAY_BYTE* data;
    int n;
    if (peer == NULL || __cn1Arg2 == JAVA_NULL || __cn1Arg4 <= 0) {
        return -1;
    }
    data = (JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY) __cn1Arg2).data;
    /* recv blocks; yield the thread state so the GC is not held up by it. */
    CN1_YIELD_THREAD;
    n = recv(peer->s, (char*) (data + __cn1Arg3), (int) __cn1Arg4, 0);
    CN1_RESUME_THREAD;
    /* Keep the buffer ARRAY OBJECT reachable across the parked recv. Only `data` (an
     * interior pointer to the array body) is used above, so the optimizer is free to drop
     * __cn1Arg2; the concurrent GC scans this parked thread and, finding no root to the
     * buffer, sweeps it mid-read -> the observed WindowsSocket_SocketInput use-after-free
     * (0xC0000005 on the cn1ss WebSocket reader). The volatile sink forces __cn1Arg2 to stay
     * live through the recv so the conservative stack/register scan resolves it. */
    cn1SocketReadKeepAlive = (volatile void*) __cn1Arg2;
    if (n == 0) {
        return -1; /* peer closed */
    }
    if (n == SOCKET_ERROR) {
        peer->errorCode = WSAGetLastError();
        return -1;
    }
    return (JAVA_INT) n;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_socketWrite___long_byte_1ARRAY_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4) {
    CN1Socket* peer = (CN1Socket*) (intptr_t) __cn1Arg1;
    JAVA_ARRAY_BYTE* data;
    int sent = 0;
    if (peer == NULL || __cn1Arg2 == JAVA_NULL || __cn1Arg4 <= 0) {
        return 0;
    }
    data = (JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY) __cn1Arg2).data;
    while (sent < __cn1Arg4) {
        int n = send(peer->s, (char*) (data + __cn1Arg3 + sent), (int) (__cn1Arg4 - sent), 0);
        if (n == SOCKET_ERROR) {
            peer->errorCode = WSAGetLastError();
            return -1;
        }
        sent += n;
    }
    return (JAVA_INT) sent;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_socketAvailable___long_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Socket* peer = (CN1Socket*) (intptr_t) __cn1Arg1;
    u_long avail = 0;
    if (peer == NULL) {
        return 0;
    }
    if (ioctlsocket(peer->s, FIONREAD, &avail) == SOCKET_ERROR) {
        return 0;
    }
    return (JAVA_INT) avail;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_socketClose___long(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Socket* peer = (CN1Socket*) (intptr_t) __cn1Arg1;
    if (peer == NULL) {
        return;
    }
    if (peer->s != INVALID_SOCKET) {
        shutdown(peer->s, SD_BOTH);
        closesocket(peer->s);
        peer->s = INVALID_SOCKET;
    }
    free(peer);
}

JAVA_INT com_codename1_impl_windows_WindowsNative_socketErrorCode___long_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Socket* peer = (CN1Socket*) (intptr_t) __cn1Arg1;
    return peer == NULL ? -1 : (JAVA_INT) peer->errorCode;
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_socketConnected___long_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Socket* peer = (CN1Socket*) (intptr_t) __cn1Arg1;
    return (peer != NULL && peer->s != INVALID_SOCKET) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_getHostOrIP___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    char name[256];
    cn1SockEnsureStartup();
    if (gethostname(name, sizeof(name)) != 0) {
        return JAVA_NULL;
    }
    return newStringFromCString(threadStateData, name);
}

#endif /* _WIN32 */
