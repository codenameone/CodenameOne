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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A restart over the same transport instance must not leave two readers on one stream.
 *
 * <p>Both production transports clear their closed flag in {@code open()}, so a reader
 * still parked in {@code readMessage()} from the previous generation is looking at a live
 * stream again the moment the replacement opens. It can then take a frame the new client
 * sent -- handled by a loop belonging to a stopped server, or dropped entirely, which
 * presents as a client whose first request never gets an answer.</p>
 *
 * <p>The transport here is deliberately the awkward-but-legal shape: {@code readMessage()}
 * parks until {@code close()} releases it, and reopening makes it readable again.</p>
 */
class MCPRestartReaderFenceTest {

    /** Long enough that a slow machine cannot fail it; short enough to notice. */
    private static final long TIMEOUT_MS = 10000L;

    @Test
    void theSecondGenerationWaitsForTheFirstReaderToLeave() throws Exception {
        SlowReaderTransport transport = new SlowReaderTransport();
        MCPServer server = new MCPServer();
        server.start(transport);

        assertTrue(transport.reading.await(TIMEOUT_MS, TimeUnit.MILLISECONDS),
                "the first generation should be inside readMessage()");

        // A read that does not unwind the instant close() is called. Real ones usually
        // do -- a closed socket ends the read -- but "usually" is the whole problem: the
        // fence exists for the interval between close() and the read noticing, and this
        // fixture is that interval held open so it can be observed.
        server.stop();
        server.start(transport);
        Thread.sleep(300L);

        assertEquals(1, transport.opensSeen.get(),
                "the replacement must not open the transport while a reader from the "
                + "previous generation is still inside it -- open() clears the closed "
                + "flag, so that reader is looking at a live stream again and can take "
                + "the new client's first frame");
        assertFalse(transport.openedWithReaderInside,
                "and if it ever does, the two generations are sharing one stream");

        transport.letTheStaleReadFinish();
        assertTrue(transport.reopened.await(TIMEOUT_MS, TimeUnit.MILLISECONDS),
                "once the stale reader leaves, the replacement gets its own open()");

        transport.letTheStaleReadFinish();
        server.stop();
        assertFalse(server.isRunning());
    }

    /**
     * Parks in {@code readMessage()} until the test says otherwise, and becomes readable
     * again on reopen -- which is exactly what makes a stale reader dangerous.
     */
    private static final class SlowReaderTransport implements MCPTransport {

        private final CountDownLatch reading = new CountDownLatch(1);
        private final CountDownLatch reopened = new CountDownLatch(1);
        private final AtomicInteger opensSeen = new AtomicInteger();
        private final AtomicInteger readersInside = new AtomicInteger();
        private volatile boolean openedWithReaderInside;
        private volatile CountDownLatch release = new CountDownLatch(1);

        void letTheStaleReadFinish() {
            release.countDown();
        }

        @Override
        public void open() throws IOException {
            if (readersInside.get() > 0) {
                openedWithReaderInside = true;
            }
            // A fresh gate per open, like a transport clearing its closed flag.
            release = new CountDownLatch(1);
            if (opensSeen.incrementAndGet() >= 2) {
                reopened.countDown();
            }
        }

        @Override
        public synchronized void close() {
            // Deliberately does NOT end the parked read. See the test.
        }

        @Override
        public String readMessage() throws IOException {
            CountDownLatch gate = release;
            readersInside.incrementAndGet();
            reading.countDown();
            try {
                gate.await(TIMEOUT_MS * 3, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted");
            } finally {
                readersInside.decrementAndGet();
            }
            return null;
        }

        @Override
        public void writeMessage(String message) throws IOException {
        }
    }
}
