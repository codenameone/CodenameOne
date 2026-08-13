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
package com.codename1.wearable;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What the queue cap owes a port that wrote the payload down.
 *
 * <p>A delivery evicted to make room was never seen by application code, so its durable record has
 * to survive -- but the port's in-process claim on that record must not. Those are two different
 * facts and only the port can act on either, so the cap has to say something. Saying nothing left
 * the Android spool holding a record it had marked in flight, for a delivery that no longer
 * existed: unclaimable until the process restarted, and charged an attempt from a budget meant to
 * bound poison payloads.</p>
 */
public class WearableDurableEvictionTest extends UITestBase {

    /** Mirrors WearableConnection.MAX_PENDING. */
    private static final int MAX_PENDING = 256;

    @BeforeEach
    public void emptyTheQueues() {
        clear("pendingMessages");
        clear("pendingData");
    }

    /**
     * A durable message is a one-shot, and its eviction is reported.
     *
     * <p>Parked as an ordinary runnable it was the FIRST thing the cap discarded and it went
     * silently -- no delivered callback, because nothing was delivered, and no dropped callback,
     * because there was none to call.</p>
     */
    @Test
    public void evictingADurableMessageTellsThePort() {
        AtomicInteger delivered = new AtomicInteger();
        final AtomicInteger dropped = new AtomicInteger();
        final AtomicInteger droppedFirst = new AtomicInteger();

        for (int i = 0; i <= MAX_PENDING; i++) {
            final int index = i;
            WearableConnection.deliverMessage("/m/" + i, new byte[] {1}, 0,
                    counter(delivered), new Runnable() {
                        public void run() {
                            dropped.incrementAndGet();
                            if (index == 0) {
                                droppedFirst.incrementAndGet();
                            }
                        }
                    });
        }

        assertEquals(1, dropped.get(), "one over the cap evicts exactly one");
        assertEquals(1, droppedFirst.get(), "and it is the oldest, not the newcomer");
        assertEquals(0, delivered.get(),
                "nothing reached a listener, so nothing may be reported delivered");
        assertEquals(MAX_PENDING, queueSize("pendingMessages"));
    }

    /**
     * A durable message outranks a replaceable delivery in the eviction order.
     *
     * <p>That is what marking it one-shot buys: the cap takes what can be recovered before what
     * cannot. Here the queue is full of ordinary messages, and the durable one must survive them.</p>
     */
    @Test
    public void aDurableMessageIsEvictedLast() {
        final AtomicInteger dropped = new AtomicInteger();
        WearableConnection.deliverMessage("/durable", new byte[] {1}, 0, null, new Runnable() {
            public void run() {
                dropped.incrementAndGet();
            }
        });
        for (int i = 0; i < MAX_PENDING; i++) {
            WearableConnection.deliverMessage("/plain/" + i, new byte[] {1}, 0);
        }

        assertEquals(0, dropped.get(),
                "an ordinary message is replaceable and goes first; the written-down one stays");
        assertEquals(MAX_PENDING, queueSize("pendingMessages"));
    }

    /**
     * A removal's port claim is released when the cap discards it.
     *
     * <p>The listener side of this was already covered -- the drain re-announces an evicted removal
     * by path -- but that re-announcement carries no callback, so a port holding the removal in a
     * durable spool never heard either outcome and its record stayed claimed for good.</p>
     */
    @Test
    public void evictingARemovalReleasesItsClaim() {
        AtomicInteger delivered = new AtomicInteger();
        final AtomicInteger dropped = new AtomicInteger();

        WearableConnection.deliverDataRemoved("/gone", counter(delivered), new Runnable() {
            public void run() {
                dropped.incrementAndGet();
            }
        });
        // The cap is what evicts, so it has to be reached: superseding a parked path only matters
        // once there is no room, and until then the newer statement simply queues behind it.
        for (int i = 0; i < MAX_PENDING - 1; i++) {
            WearableConnection.deliverDataRemoved("/other/" + i, null, null);
        }
        assertEquals(0, dropped.get(), "still room, so nothing has been discarded yet");

        // Now full. A newer statement about the SAME path takes the parked one's place.
        WearableConnection.deliverDataRemoved("/gone", null, null);

        assertEquals(1, dropped.get(), "the superseded removal was never seen by a listener");
        assertEquals(0, delivered.get());
    }

    private static Runnable counter(final AtomicInteger n) {
        return new Runnable() {
            public void run() {
                n.incrementAndGet();
            }
        };
    }

    private static int queueSize(String field) {
        return queue(field).size();
    }

    private static void clear(String field) {
        List<Runnable> q = queue(field);
        synchronized (q) {
            q.clear();
        }
    }

    /**
     * Reaches the parked-delivery queue directly.
     *
     * <p>Reflection because these are private and there is no public way to observe a park -- which
     * is the point of the design, and not a reason to leave the cap's contract untested. A test
     * that could only see delivered payloads would pass on exactly the silent discard this exists
     * to catch.</p>
     */
    @SuppressWarnings("unchecked")
    private static List<Runnable> queue(String field) {
        try {
            java.lang.reflect.Field f = WearableConnection.class.getDeclaredField(field);
            f.setAccessible(true);
            return (List<Runnable>) f.get(null);
        } catch (Exception e) {
            throw new IllegalStateException("the pending queues moved; this test must follow", e);
        }
    }
}
