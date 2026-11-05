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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/// A websocket reads on its worker while another thread writes to the same
/// descriptor. On the Java SE arm both go through Deadlines, which flips the
/// channel to non-blocking for the duration; the mode has to come back as
/// blocking once both are out, or the next ServerSocket.read takes the
/// non-blocking branch, answers 0 for "nothing yet", and the session reads that
/// as end of stream -- a 1006 with no error, which neither peer asked for.
class ConcurrentReadWriteBlockingModeTest {

    @Test
    void readerAndWriterTogetherLeaveTheChannelBlocking() throws Exception {
        ServerSocketChannel listen = ServerSocketChannel.open();
        listen.bind(new InetSocketAddress("127.0.0.1", 0));
        final SocketChannel peer = SocketChannel.open(listen.getLocalAddress());
        final SocketChannel served = listen.accept();
        listen.close();
        final int fd = Descriptors.add(served);
        ServerSocket.setTimeout(fd, 5000);
        ServerSocket.setReceiveTimeout(fd, 5000);
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        Thread drain = new Thread(new Runnable() {
            @Override
            public void run() {
                ByteBuffer sink = ByteBuffer.allocate(65536);
                try {
                    while (true) {
                        sink.clear();
                        if (peer.read(sink) < 0) {
                            return;
                        }
                    }
                } catch (Exception ignored) {
                    // closed at the end of the test
                }
            }
        });
        drain.start();
        try {
            for (int round = 0; round < 2000 && failure.get() == null; round++) {
                Thread writer = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            byte[] frame = new byte[1024];
                            ServerSocket.write(fd, frame, 0, frame.length);
                        } catch (Throwable err) {
                            failure.compareAndSet(null, err);
                        }
                    }
                });
                writer.start();
                peer.write(ByteBuffer.wrap(new byte[] {1}));
                byte[] in = new byte[16];
                int n = ServerSocket.read(fd, in, 0, in.length);
                writer.join();
                if (n <= 0) {
                    fail("read answered " + n + " on round " + round + " with the peer open");
                }
                if (!served.isBlocking()) {
                    fail("channel left non-blocking after round " + round);
                }
            }
            if (failure.get() != null) {
                throw new AssertionError(failure.get());
            }
            assertTrue(served.isBlocking());
        } finally {
            ServerSocket.closeFd(fd);
            peer.close();
            drain.join(2000);
        }
    }
}
