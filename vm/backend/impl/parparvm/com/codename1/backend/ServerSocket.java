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
package com.codename1.backend;

import java.io.IOException;

/**
 * A listening TCP socket and the blocking read/write a worker uses once it owns a
 * connection. Deliberately fd-based rather than object-per-socket: the reactor
 * deals in descriptors and an extra object per idle connection is exactly the cost
 * this design exists to avoid.
 */
public final class ServerSocket {
    private int fd;

    private ServerSocket(int fd) {
        this.fd = fd;
    }

    /**
     * - `host`: null or "0.0.0.0" to listen on every interface
     * - `port`: 0 to let the OS choose, then ask {@link #getPort}
     */
    public static ServerSocket bind(String host, int port, int backlog) throws IOException {
        int fd = bindImpl(host, port, backlog);
        if(fd < 0) {
            throw new IOException("Could not bind " + (host == null ? "*" : host) + ":" + port);
        }
        return new ServerSocket(fd);
    }

    public int getFd() {
        return fd;
    }

    public int getPort() {
        return boundPortImpl(fd);
    }

    /** The accepted descriptor, or -1 when nothing was waiting. */
    public int accept() {
        return acceptImpl(fd);
    }

    public void close() {
        if(fd >= 0) {
            int f = fd;
            fd = -1;
            closeFdImpl(f);
        }
    }

    /**
     * Blocking or non-blocking mode for one descriptor. The reactor needs
     * non-blocking; a worker that owns a connection wants blocking, so it can read
     * a request without a state machine.
     */
    public static void setBlocking(int fd, boolean blocking) throws IOException {
        if(setBlockingImpl(fd, blocking) != 0) {
            throw new IOException("Could not change blocking mode on fd " + fd);
        }
    }

    /** Thrown when a read or write deadline expires. */
    public static final class TimeoutException extends IOException {
        TimeoutException(String message) {
            super(message);
        }
    }

    /**
     * Applies a receive and send deadline. Without one a connection that opens and
     * says nothing holds a worker forever, and the pool is bounded.
     */
    public static void setTimeout(int fd, int millis) throws IOException {
        if(setTimeoutImpl(fd, millis) != 0) {
            throw new IOException("Could not set a deadline on fd " + fd);
        }
    }

    /** -1 at end of stream, as InputStream does. */
    /**
     * Waits for the socket to become readable, for at most timeoutMillis. True if
     * it is, false if the wait expired.
     *
     * One syscall, and it leaves the descriptor exactly as it was. The caller uses
     * it between requests on a keep-alive connection, where the alternatives --
     * setting and restoring a receive deadline, or flipping to non-blocking and
     * back -- cost two to four syscalls each way and disturb the deadline that
     * governs a real request read.
     */
    /**
     * A reusable per-thread read buffer of at least {@code capacity} bytes.
     *
     * The same array comes back on every call for a thread, so a server that reads
     * through it allocates nothing per request. Its contents belong to the current
     * callback only -- the next read on this thread overwrites them, so nothing may
     * retain it or hand it to code that might.
     *
     * On the translated target the storage is a C buffer that the collector never
     * allocated and never sweeps, so the read path contributes nothing at all to
     * the allocation rate that paces the GC. Java SE cannot do that and returns an
     * ordinary cached array; the observable contract is the same, which is the
     * point -- only the allocation accounting differs.
     *
     * Read from {@code fd} into this thread's reusable buffer and return an array
     * whose length is exactly the number of bytes read, or null at end of stream.
     *
     * On the translated target this allocates nothing and copies nothing: the array
     * header and its storage are C memory the collector never touches, and the
     * length is set per read so the caller can scan to {@code array.length}. Java SE
     * cannot resize an array and returns a right-sized copy instead -- same
     * contract, different allocation accounting.
     *
     * The bytes belong to the current callback on the current thread. Anything that
     * must outlive either has to be copied out first.
     */
    public static byte[] readIntoThreadBuffer(int fd, int capacity) {
        return readIntoThreadBufferImpl(fd, capacity);
    }

    public static byte[] threadReadBuffer(int capacity) {
        byte[] foreign = threadReadBufferImpl(capacity);
        if(foreign != null) {
            return foreign;
        }
        // The native refused (allocation failure). An ordinary array is correct,
        // just not free, so the server keeps working rather than failing a request
        // over an optimisation.
        return new byte[capacity];
    }

    public static boolean awaitReadable(int fd, int timeoutMillis) throws IOException {
        int rc = awaitReadableImpl(fd, timeoutMillis);
        if(rc < 0) {
            throw new IOException("Poll failed on fd " + fd);
        }
        return rc > 0;
    }

    public static int read(int fd, byte[] buffer, int offset, int length) throws IOException {
        int n = readImpl(fd, buffer, offset, length);
        if(n == -3) {
            throw new TimeoutException("Read timed out on fd " + fd);
        }
        if(n < -1) {
            throw new IOException("Read failed on fd " + fd);
        }
        return n;
    }

    public static void write(int fd, byte[] buffer, int offset, int length) throws IOException {
        if(writeImpl(fd, buffer, offset, length) != length) {
            throw new IOException("Write failed on fd " + fd);
        }
    }

    public static void closeFd(int fd) {
        if(fd >= 0) {
            closeFdImpl(fd);
        }
    }

    private static native int bindImpl(String host, int port, int backlog);
    private static native int boundPortImpl(int fd);
    private static native int acceptImpl(int serverFd);
    private static native int setBlockingImpl(int fd, boolean blocking);
    private static native int setTimeoutImpl(int fd, int millis);
    /**
     * A byte[] backed by this thread's C read buffer, handed over without a copy.
     *
     * The same object comes back on every call for a thread, its storage is never
     * allocated by the collector, and it is not swept -- so the read path
     * contributes nothing to the allocation rate that paces the GC. Returns null
     * if the buffer cannot be provided, and the caller must then fall back to an
     * ordinary array rather than assume it worked.
     *
     * The contents belong to the current callback ONLY. Nothing may retain this
     * array or hand it to code that might: the next read on this thread overwrites
     * it, and a grow moves the storage underneath it.
     */
    static native byte[] threadReadBufferImpl(int capacity);

    static native byte[] readIntoThreadBufferImpl(int fd, int capacity);


    private static native int awaitReadableImpl(int fd, int timeoutMillis);
    private static native int readImpl(int fd, byte[] buffer, int offset, int length);
    private static native int writeImpl(int fd, byte[] buffer, int offset, int length);
    private static native void closeFdImpl(int fd);
    /** Cores available to this process. */
    public static int availableProcessors() {
        int n = availableProcessorsImpl();
        return n > 0 ? n : 1;
    }

    private static native int availableProcessorsImpl();
}
