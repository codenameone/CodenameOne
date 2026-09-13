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
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Java SE twin of ServerSocket, over NIO channels behind synthetic descriptors.
 *
 * Blocking mode is real here, as it is on the translated side: the reactor runs a
 * Selector over non-blocking channels, and a worker that takes a connection flips
 * it to blocking so request parsing is a read loop rather than a state machine.
 */
public final class ServerSocket {
    private final ServerSocketChannel channel;
    private final int fd;

    private ServerSocket(ServerSocketChannel channel, int fd) {
        this.channel = channel;
        this.fd = fd;
    }

    /** Thrown when a read or write deadline expires. */
    public static final class TimeoutException extends IOException {
        TimeoutException(String message) {
            super(message);
        }
    }

    public static ServerSocket bind(String host, int port, int backlog) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        try {
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);
            channel.bind(host == null || "0.0.0.0".equals(host)
                    ? new InetSocketAddress(port)
                    : new InetSocketAddress(host, port), backlog);
            return new ServerSocket(channel, Descriptors.add(channel));
        } catch (IOException err) {
            channel.close();
            throw new IOException("Could not bind " + (host == null ? "*" : host) + ":" + port);
        }
    }

    public int getFd() {
        return fd;
    }

    public int getPort() {
        try {
            return ((InetSocketAddress)channel.getLocalAddress()).getPort();
        } catch (IOException err) {
            return -1;
        }
    }

    public int accept() {
        try {
            SocketChannel client = channel.accept();
            if(client == null) {
                return -1;
            }
            client.setOption(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE);
            return Descriptors.add(client);
        } catch (IOException err) {
            return -1;
        }
    }

    public void close() {
        Descriptors.remove(fd);
        try {
            channel.close();
        } catch (IOException ignored) {
            // already gone
        }
    }

    public static void setBlocking(int fd, boolean blocking) throws IOException {
        Object entry = Descriptors.get(fd);
        if(entry instanceof SocketChannel) {
            ((SocketChannel)entry).configureBlocking(blocking);
            return;
        }
        if(entry instanceof ServerSocketChannel) {
            ((ServerSocketChannel)entry).configureBlocking(blocking);
            return;
        }
        throw new IOException("Not a socket: " + fd);
    }

    /**
     * A read deadline. NIO channels have no SO_RCVTIMEO, so the deadline is
     * enforced by the reader below; without one a silent client would hold a
     * worker for as long as it liked, and the pool is bounded.
     */
    public static void setTimeout(int fd, int millis) throws IOException {
        Deadlines.set(fd, millis);
    }

    /**
     * Java SE twin of the readiness wait. See the translated version for why the
     * shared code asks for this rather than juggling deadlines.
     *
     * A Selector is heavier than the single poll the translated side makes, which
     * is acceptable here: this arm is the development loop, and its job is to
     * behave the same, not to match the deployed binary's syscall count.
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
     */
    /**
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
    public static byte[] readIntoThreadBuffer(int fd, int capacity) throws IOException {
        byte[] scratch = threadReadBuffer(capacity);
        int n = read(fd, scratch, 0, capacity);
        if(n <= 0) {
            return null;
        }
        byte[] exact = new byte[n];
        System.arraycopy(scratch, 0, exact, 0, n);
        return exact;
    }

    public static byte[] threadReadBuffer(int capacity) {
        byte[] cached = (byte[])THREAD_READ_BUFFER.get();
        if(cached == null || cached.length < capacity) {
            cached = new byte[capacity];
            THREAD_READ_BUFFER.set(cached);
        }
        return cached;
    }

    private static final ThreadLocal THREAD_READ_BUFFER = new ThreadLocal();

    public static boolean awaitReadable(int fd, int timeoutMillis) throws IOException {
        Object entry = Descriptors.get(fd);
        if(!(entry instanceof SocketChannel)) {
            throw new IOException("Not a socket: " + fd);
        }
        SocketChannel channel = (SocketChannel)entry;
        boolean wasBlocking = channel.isBlocking();
        Selector selector = null;
        try {
            channel.configureBlocking(false);
            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);
            return selector.select(timeoutMillis) > 0;
        } finally {
            if(selector != null) {
                selector.close();
            }
            if(wasBlocking && channel.isOpen()) {
                channel.configureBlocking(true);
            }
        }
    }

    public static int read(int fd, byte[] buffer, int offset, int length) throws IOException {
        Object entry = Descriptors.get(fd);
        if(!(entry instanceof SocketChannel)) {
            throw new IOException("Not a socket: " + fd);
        }
        SocketChannel channel = (SocketChannel)entry;
        ByteBuffer target = ByteBuffer.wrap(buffer, offset, length);
        if(channel.isBlocking()) {
            // A blocking channel read cannot be interrupted by a timer, so the
            // deadline is applied with a selector around it.
            return Deadlines.readWithDeadline(fd, channel, target);
        }
        int n = channel.read(target);
        return n;
    }

    public static void write(int fd, byte[] buffer, int offset, int length) throws IOException {
        Object entry = Descriptors.get(fd);
        if(!(entry instanceof SocketChannel)) {
            throw new IOException("Not a socket: " + fd);
        }
        SocketChannel channel = (SocketChannel)entry;
        // Through the deadline, as reads are. A client that stops reading otherwise
        // parks this worker in write() indefinitely.
        Deadlines.writeWithDeadline(fd, channel, ByteBuffer.wrap(buffer, offset, length));
    }

    public static void closeFd(int fd) {
        Object entry = Descriptors.remove(fd);
        Deadlines.clear(fd);
        Descriptors.closeQuietly(entry);
    }
    /** Cores available to this process. */
    public static int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }
}
