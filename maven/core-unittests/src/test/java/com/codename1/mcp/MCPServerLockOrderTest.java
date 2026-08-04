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
package com.codename1.mcp;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stopping a server whose transport is still inside a blocking {@code open()}.
 *
 * <p>{@link MCPTransport} is a public interface, so an implementation is entitled to write
 * {@code synchronized void close()} -- and a transport that waits for a client inside
 * {@code open()} is the normal shape, since that is what {@code close()} exists to
 * interrupt. Those two facts together used to hang the process: the reader thread held the
 * transport's own monitor across {@code open()} and wanted the server monitor inside
 * {@code isCurrent()}, while {@code stop()} held the server monitor and wanted the
 * transport's monitor inside {@code close()}. Neither ever moves again, and the one call
 * that could have ended the blocking {@code open()} is the one that is stuck.</p>
 *
 * <p>Written with a timeout rather than a plain join, so a regression fails the suite
 * instead of hanging it.</p>
 */
class MCPServerLockOrderTest {

    /** Long enough that a slow machine cannot fail it; short enough to notice. */
    private static final long TIMEOUT_MS = 10000L;

    @Test
    void stopEndsABlockingOpenOnATransportThatSynchronizesItself() throws Exception {
        BlockingTransport transport = new BlockingTransport();
        MCPServer server = new MCPServer();
        server.start(transport);

        assertTrue(transport.opening.await(TIMEOUT_MS, TimeUnit.MILLISECONDS),
                "the reader thread should have reached open()");

        final CountDownLatch stopped = new CountDownLatch(1);
        Thread stopper = new Thread(new Runnable() {
            public void run() {
                server.stop();
                stopped.countDown();
            }
        }, "mcp-lock-order-stop");
        stopper.setDaemon(true);
        stopper.start();

        assertTrue(stopped.await(TIMEOUT_MS, TimeUnit.MILLISECONDS),
                "stop() must not wait on a lock the blocked open() is holding");
        assertTrue(transport.closed.await(TIMEOUT_MS, TimeUnit.MILLISECONDS),
                "and it has to actually reach close(), which is what ends the open()");
        assertFalse(server.isRunning());
    }

    /**
     * The shape the interface allows and the fix has to tolerate: {@code open()} parks
     * until {@code close()} says otherwise, and {@code close()} is synchronized on the
     * transport.
     */
    private static final class BlockingTransport implements MCPTransport {

        private final CountDownLatch opening = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        public void open() throws IOException {
            opening.countDown();
            try {
                // Waits for a client, as a real listening transport does. Bounded only so
                // a failing run ends rather than parking a thread forever.
                release.await(TIMEOUT_MS * 3, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted");
            }
        }

        @Override
        public synchronized void close() {
            release.countDown();
            closed.countDown();
        }

        @Override
        public String readMessage() throws IOException {
            return null;
        }

        @Override
        public void writeMessage(String message) throws IOException {
        }
    }
}
