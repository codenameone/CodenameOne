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
JAVA_INT com_codename1_backend_FileIo_openBeneathImpl___java_lang_String_java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT root, JAVA_OBJECT relative) {
#if defined(__linux__) && defined(SYS_openat2)
    const char* rootPath;
    const char* rel;
    int dirFd;
    int fd;
    struct cn1_open_how {
        uint64_t flags;
        uint64_t mode;
        uint64_t resolve;
    } how;
    if(root == JAVA_NULL || relative == JAVA_NULL) {
        return -1;
    }
    rootPath = stringToUTF8(threadStateData, root);
    if(rootPath == NULL) {
        return -1;
    }
    dirFd = open(rootPath, O_RDONLY | O_DIRECTORY | O_CLOEXEC);
    if(dirFd < 0) {
        return -1;
    }
    rel = stringToUTF8(threadStateData, relative);
    if(rel == NULL) {
        close(dirFd);
        return -1;
    }
    /* RESOLVE_BENEATH rejects an absolute path outright, and the caller's target
       always starts at the root. */
    while(*rel == '/') {
        rel++;
    }
    if(*rel == 0) {
        close(dirFd);
        return -1;
    }
    memset(&how, 0, sizeof(how));
    how.flags = (uint64_t)(O_RDONLY | O_CLOEXEC);
    how.resolve = (uint64_t)CN1_RESOLVE_BENEATH;
    fd = (int)syscall(SYS_openat2, dirFd, rel, &how, sizeof(how));
    close(dirFd);
    if(fd < 0) {
        /* An old kernel knows the number but not the struct, or does not know the
           call at all. Either way this cannot answer, so say so rather than let the
           caller read a refusal as "file missing". */
        if(errno == ENOSYS || errno == EINVAL || errno == E2BIG) {
            return -2;
        }
        return -1;
    }
    return fd;
#else
    (void)root; (void)relative;
    return -2;
#endif
}

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
    data[2] = S_ISDIR(st.st_mode) ? 1 : 0;
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

JAVA_LONG com_codename1_backend_FileIo_sendFileImpl___int_int_long_long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT outFd, JAVA_INT inFd, JAVA_LONG offset, JAVA_LONG count) {
#if defined(CN1_HAVE_SENDFILE) && defined(__linux__)
    off_t off = (off_t)offset;
    ssize_t n;
    if(outFd < 0 || inFd < 0) {
        return -1;
    }
    CN1_YIELD_THREAD;
    for(;;) {
        n = sendfile(outFd, inFd, &off, (size_t)count);
        if(n >= 0) {
            break;
        }
        if(errno == EINTR) {
            continue;
        }
        if(errno == EAGAIN || errno == EWOULDBLOCK) {
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
            rc = sendfile(inFd, outFd, (off_t)offset, &len, NULL, 0);
        } while(rc < 0 && errno == EINTR);
        /* Captured before CN1_RESUME_THREAD: the resume is a GC safepoint and can
           park this thread on a timed wait, which overwrites errno. Read after
           it, this classified a real sendfile failure by the WAIT's errno. */
        sendErrno = errno;
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
