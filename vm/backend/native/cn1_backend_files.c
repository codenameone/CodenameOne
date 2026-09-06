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
#include <string.h>
#include <errno.h>

#ifndef _WIN32
#include <unistd.h>
#include <fcntl.h>
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

/* Opens for reading. Returns the descriptor, or -1. */
JAVA_INT com_codename1_backend_FileIo_openReadImpl___java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
#ifdef _WIN32
    (void)path;
    return -1;
#else
    const char* p = path == JAVA_NULL ? NULL : stringToUTF8(threadStateData, path);
    if(p == NULL) {
        return -1;
    }
    return open(p, O_RDONLY | O_CLOEXEC);
#endif
}

/*
 * Fills out[0]=size, out[1]=modified-time-millis, out[2]=1 when it is a directory.
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
    data = (JAVA_ARRAY_LONG*)((JAVA_ARRAY)out)->data;
    data[0] = (JAVA_LONG)st.st_size;
    data[1] = (JAVA_LONG)st.st_mtime * 1000LL;
    data[2] = S_ISDIR(st.st_mode) ? 1 : 0;
    return 0;
#endif
}

/*
 * Sends count bytes of inFd starting at offset straight to the socket. Returns how
 * many moved, which may be fewer than asked -- the caller loops. -1 on error.
 */
JAVA_LONG com_codename1_backend_FileIo_sendFileImpl___int_int_long_long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT outFd, JAVA_INT inFd, JAVA_LONG offset, JAVA_LONG count) {
#if defined(CN1_HAVE_SENDFILE) && defined(__linux__)
    off_t off = (off_t)offset;
    ssize_t n;
    if(outFd < 0 || inFd < 0) {
        return -1;
    }
    CN1_YIELD_THREAD;
    do {
        n = sendfile(outFd, inFd, &off, (size_t)count);
    } while(n < 0 && errno == EINTR);
    CN1_RESUME_THREAD;
    return n < 0 ? -1 : (JAVA_LONG)n;
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
    do {
        rc = sendfile(inFd, outFd, (off_t)offset, &len, NULL, 0);
    } while(rc < 0 && errno == EINTR);
    /* Captured before CN1_RESUME_THREAD: the resume is a GC safepoint and can park
       this thread on a timed wait, which overwrites errno. Read afterwards, this
       classified a real sendfile failure by the WAIT's errno instead of its own. */
    sendErrno = errno;
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

/* Plain read, for the TLS path and for platforms with no sendfile. */
JAVA_INT com_codename1_backend_FileIo_readImpl___int_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
#ifdef _WIN32
    (void)fd; (void)buffer; (void)offset; (void)length;
    return -1;
#else
    JAVA_ARRAY_BYTE* data;
    ssize_t n;
    if(fd < 0 || buffer == JAVA_NULL) {
        return -1;
    }
    data = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)buffer)->data;
    CN1_YIELD_THREAD;
    do {
        n = read(fd, &data[offset], (size_t)length);
    } while(n < 0 && errno == EINTR);
    CN1_RESUME_THREAD;
    return n < 0 ? -1 : (JAVA_INT)n;
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
    char resolved[4096];
    const char* p = path == JAVA_NULL ? NULL : stringToUTF8(threadStateData, path);
    if(p == NULL) {
        return JAVA_NULL;
    }
    if(realpath(p, resolved) == NULL) {
        return JAVA_NULL;
    }
    return newStringFromCString(threadStateData, resolved);
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
