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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Read deadlines for the Java SE runtime.
 *
 * The translated target sets SO_RCVTIMEO on the descriptor and the kernel enforces
 * it. An NIO channel has no such option, and a blocking channel read cannot be
 * interrupted by a timer -- so a deadline is applied by putting the channel into
 * non-blocking mode around a select() with a timeout. Without this a client that
 * connects and says nothing holds a worker forever, and the pool is bounded.
 */
final class Deadlines {
    private static final Map<Integer, Integer> TIMEOUTS = new ConcurrentHashMap<Integer, Integer>();
    /**
     * The receive deadline where it differs from TIMEOUTS.
     *
     * A websocket wants its read allowance and its send allowance apart: idle by
     * design in one direction, and still obliged to give up on a peer that has
     * stopped reading in the other.
     */
    private static final Map<Integer, Integer> RECEIVE_TIMEOUTS =
            new ConcurrentHashMap<Integer, Integer>();

    /**
     * How many callers currently need a channel in non-blocking mode.
     *
     * BOTH paths below flip the channel and register a selector, and on a
     * websocket a broadcast thread can be writing while the connection's own
     * worker is blocked in a read. Whichever finished first used to put the
     * channel back to blocking under the other, so the survivor's next
     * `configureBlocking` or `register` threw IllegalBlockingModeException and
     * took the session down -- a failure that only appears when a send really
     * does overlap a read, which is exactly what the arbitrary-thread send this
     * server advertises makes ordinary.
     *
     * Counting rather than locking, because a lock held across the select would
     * make a broadcast wait out the reader's whole idle timeout.
     */
    private static final Map<Integer, int[]> NONBLOCKING = new ConcurrentHashMap<Integer, int[]>();

    private Deadlines() {
    }

    static void set(int fd, int millis) {
        TIMEOUTS.put(Integer.valueOf(fd), Integer.valueOf(millis));
    }

    /** The receive deadline alone; the send deadline stays as `set` left it. */
    static void setReceive(int fd, int millis) {
        RECEIVE_TIMEOUTS.put(Integer.valueOf(fd), Integer.valueOf(millis));
    }

    static void clear(int fd) {
        TIMEOUTS.remove(Integer.valueOf(fd));
        RECEIVE_TIMEOUTS.remove(Integer.valueOf(fd));
        NONBLOCKING.remove(Integer.valueOf(fd));
    }

    private static Integer receiveTimeout(int fd) {
        Integer own = RECEIVE_TIMEOUTS.get(Integer.valueOf(fd));
        return own != null ? own : TIMEOUTS.get(Integer.valueOf(fd));
    }

    /** Puts the channel in non-blocking mode, counting this caller in. */
    private static void enterNonBlocking(int fd, SocketChannel channel) throws IOException {
        int[] count = NONBLOCKING.get(Integer.valueOf(fd));
        if(count == null) {
            synchronized(NONBLOCKING) {
                count = NONBLOCKING.get(Integer.valueOf(fd));
                if(count == null) {
                    count = new int[1];
                    NONBLOCKING.put(Integer.valueOf(fd), count);
                }
            }
        }
        synchronized(count) {
            if(count[0]++ == 0) {
                channel.configureBlocking(false);
            }
        }
    }

    /** Restores blocking mode once the LAST caller has left. */
    private static void leaveNonBlocking(int fd, SocketChannel channel, boolean wasBlocking) {
        int[] count = NONBLOCKING.get(Integer.valueOf(fd));
        if(count == null) {
            return;
        }
        synchronized(count) {
            if(--count[0] <= 0) {
                count[0] = 0;
                if(wasBlocking && channel.isOpen()) {
                    try {
                        channel.configureBlocking(true);
                    } catch (IOException ignored) {
                        // The channel is going away; the caller will see it.
                    }
                }
            }
        }
    }

    static int readWithDeadline(int fd, SocketChannel channel, ByteBuffer target)
            throws IOException {
        Integer timeout = receiveTimeout(fd);
        if(timeout == null || timeout.intValue() <= 0) {
            return channel.read(target);
        }
        boolean wasBlocking = channel.isBlocking();
        Selector selector = null;
        try {
            enterNonBlocking(fd, channel);
            int n = channel.read(target);
            if(n != 0) {
                return n;
            }
            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);
            if(selector.select(timeout.intValue()) == 0) {
                throw new ServerSocket.TimeoutException("Read timed out on " + fd);
            }
            return channel.read(target);
        } finally {
            if(selector != null) {
                selector.close();
            }
            leaveNonBlocking(fd, channel, wasBlocking);
        }
    }

    /**
     * Writes the whole buffer, or gives up when the descriptor's deadline passes.
     *
     * A blocking write has no timeout of its own, so a client that requests a large
     * response and then stops reading fills its receive window and parks the worker
     * in write() for as long as it likes. Enough of them and every worker is held by
     * a client that is doing nothing -- the native server has SO_SNDTIMEO for exactly
     * this, and this runtime had nothing.
     */
    static void writeWithDeadline(int fd, SocketChannel channel, ByteBuffer source)
            throws IOException {
        Integer timeout = TIMEOUTS.get(Integer.valueOf(fd));
        if(timeout == null || timeout.intValue() <= 0) {
            while(source.hasRemaining()) {
                if(channel.write(source) < 0) {
                    throw new IOException("Write failed on " + fd);
                }
            }
            return;
        }
        boolean wasBlocking = channel.isBlocking();
        Selector selector = null;
        try {
            enterNonBlocking(fd, channel);
            while(source.hasRemaining()) {
                int n = channel.write(source);
                if(n < 0) {
                    throw new IOException("Write failed on " + fd);
                }
                if(n > 0) {
                    // Progress restarts the clock, so a slow but moving client is not
                    // cut off; only one that has stopped entirely is.
                    continue;
                }
                if(selector == null) {
                    selector = Selector.open();
                    channel.register(selector, SelectionKey.OP_WRITE);
                }
                if(selector.select(timeout.intValue()) == 0) {
                    throw new ServerSocket.TimeoutException("Write timed out on " + fd);
                }
                selector.selectedKeys().clear();
            }
        } finally {
            if(selector != null) {
                selector.close();
            }
            leaveNonBlocking(fd, channel, wasBlocking);
        }
    }
}
