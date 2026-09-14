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

    private Deadlines() {
    }

    static void set(int fd, int millis) {
        TIMEOUTS.put(Integer.valueOf(fd), Integer.valueOf(millis));
    }

    static void clear(int fd) {
        TIMEOUTS.remove(Integer.valueOf(fd));
    }

    static int readWithDeadline(int fd, SocketChannel channel, ByteBuffer target)
            throws IOException {
        Integer timeout = TIMEOUTS.get(Integer.valueOf(fd));
        if(timeout == null || timeout.intValue() <= 0) {
            return channel.read(target);
        }
        boolean wasBlocking = channel.isBlocking();
        Selector selector = null;
        try {
            channel.configureBlocking(false);
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
            if(wasBlocking && channel.isOpen()) {
                channel.configureBlocking(true);
            }
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
            channel.configureBlocking(false);
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
            if(wasBlocking && channel.isOpen()) {
                channel.configureBlocking(true);
            }
        }
    }
}
