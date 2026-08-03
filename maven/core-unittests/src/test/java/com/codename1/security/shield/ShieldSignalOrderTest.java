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
package com.codename1.security.shield;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Display;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a listener is holding when the dust settles has to be what the bus holds.
 *
 * <p>Storing an observation and enqueueing its notification are separate steps, and they
 * cannot be merged: {@code Display.callSerially} runs the task inline before the EDT is
 * up, so notifying under the signal monitor would run application listeners while holding
 * it. Two workers reporting different observations of one signal could therefore
 * interleave as A stores, B stores over it, B enqueues, A enqueues -- and listeners
 * finished on A while {@code snapshot()} already answered B, with nothing later
 * guaranteed to correct them. A device that reported a hooking framework and then a clean
 * state, in that order, would leave an app looking at the compromise indefinitely; the
 * reverse leaves it looking at the clean one.</p>
 */
class ShieldSignalOrderTest extends UITestBase {

    private static final long GENEROUS_TIMEOUT_MS = 10000L;

    private final List<ShieldSignal> seen = new ArrayList<ShieldSignal>();
    private ShieldListener listener;

    @BeforeEach
    void attach() {
        ShieldSignals.clear();
        listener = new ShieldListener() {
            public void signalRaised(ShieldSignal signal) {
                synchronized (seen) {
                    seen.add(signal);
                }
            }

            public void tokenRefreshed(ShieldToken token) {
            }

            public void statusChanged(ShieldStatus status) {
            }
        };
        ShieldSignals.addListener(listener);
    }

    @AfterEach
    void detach() {
        ShieldSignals.removeListener(listener);
        ShieldSignals.clear();
    }

    /**
     * A superseded observation is never announced, whichever order the two notifications
     * were queued in.
     *
     * <p>Staged through the listener monitor, which the notification path takes after the
     * signal has already been stored. That parks the first report exactly where its
     * notification is enqueued, so the second can overtake it deterministically -- racing
     * two threads at a window this narrow is not something a test can rely on.</p>
     */
    @Test
    void aListenerNeverEndsOnAnObservationTheBusHasReplaced() throws Exception {
        java.lang.reflect.Field listenersField =
                ShieldSignals.class.getDeclaredField("listeners");
        listenersField.setAccessible(true);
        Object listenerLock = listenersField.get(null);

        final CountDownLatch firstIn = new CountDownLatch(1);
        final CountDownLatch secondIn = new CountDownLatch(1);
        Thread first;
        Thread second;
        synchronized (listenerLock) {
            first = new Thread(new Runnable() {
                public void run() {
                    firstIn.countDown();
                    ShieldSignals.add(ShieldSignal.HOOK, 90, "frida");
                }
            }, "shield-signal-first");
            first.setDaemon(true);
            first.start();
            assertTrue(firstIn.await(GENEROUS_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            Thread.sleep(200L);

            second = new Thread(new Runnable() {
                public void run() {
                    secondIn.countDown();
                    ShieldSignals.add(ShieldSignal.HOOK, 10, "gone");
                }
            }, "shield-signal-second");
            second.setDaemon(true);
            second.start();
            assertTrue(secondIn.await(GENEROUS_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            Thread.sleep(200L);
        }
        first.join(GENEROUS_TIMEOUT_MS);
        second.join(GENEROUS_TIMEOUT_MS);
        drainTheEventQueue();

        ShieldSignal current = null;
        for (ShieldSignal s : ShieldSignals.snapshot()) {
            if (ShieldSignal.HOOK.equals(s.getId())) {
                current = s;
            }
        }
        assertEquals(10, current == null ? -1 : current.getSeverity(),
                "the fixture needs the second report to be the one the bus holds");

        synchronized (seen) {
            assertEquals(1, countFor(ShieldSignal.HOOK),
                    "the superseded report was already wrong when it was queued, so it "
                    + "is not announced: " + seen);
            assertEquals(10, lastFor(ShieldSignal.HOOK),
                    "and what the listener is left holding is what snapshot() answers");
        }
    }

    /**
     * And an identical repeat arriving mid-flight does not swallow the first notification.
     *
     * <p>The two rules meet here. An identical repeat replaces the stored entry -- so the
     * timestamp is the latest sighting -- and deliberately queues nothing, because a
     * detector polling on a timer would otherwise put a runnable on the EDT per poll. The
     * pending notification for the first report then failed a currency test asking about
     * object identity, and dropped itself: nobody was told, while the signal sat in
     * {@code snapshot()}. A detector on a timer is the normal case, and the sighting that
     * went unannounced is the FIRST one, which is the whole reason a listener is
     * attached.</p>
     */
    @Test
    void anIdenticalRepeatDoesNotSwallowTheNotificationAlreadyInFlight() throws Exception {
        java.lang.reflect.Field listenersField =
                ShieldSignals.class.getDeclaredField("listeners");
        listenersField.setAccessible(true);
        Object listenerLock = listenersField.get(null);

        final CountDownLatch firstIn = new CountDownLatch(1);
        Thread first;
        synchronized (listenerLock) {
            first = new Thread(new Runnable() {
                public void run() {
                    firstIn.countDown();
                    ShieldSignals.add(ShieldSignal.ROOT, 70, "su");
                }
            }, "shield-signal-first-sighting");
            first.setDaemon(true);
            first.start();
            assertTrue(firstIn.await(GENEROUS_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            // Parked at the notification, with the signal already stored.
            Thread.sleep(200L);

            // The next poll of the same detector, on this thread: identical, so it replaces
            // the entry and returns without ever reaching the listener lock.
            ShieldSignals.add(ShieldSignal.ROOT, 70, "su");
        }
        first.join(GENEROUS_TIMEOUT_MS);
        drainTheEventQueue();

        synchronized (seen) {
            assertEquals(1, countFor(ShieldSignal.ROOT),
                    "the device is rooted and the bus knows it -- a listener that is "
                    + "never told is the bug: " + seen);
            assertEquals(70, lastFor(ShieldSignal.ROOT));
        }
    }

    private int countFor(String id) {
        int n = 0;
        for (ShieldSignal s : seen) {
            if (id.equals(s.getId())) {
                n++;
            }
        }
        return n;
    }

    private int lastFor(String id) {
        int severity = -1;
        for (ShieldSignal s : seen) {
            if (id.equals(s.getId())) {
                severity = s.getSeverity();
            }
        }
        return severity;
    }

    /** Waits for everything queued so far to have run, without blocking the EDT. */
    private void drainTheEventQueue() throws Exception {
        final CountDownLatch drained = new CountDownLatch(1);
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                drained.countDown();
            }
        });
        assertTrue(drained.await(GENEROUS_TIMEOUT_MS, TimeUnit.MILLISECONDS),
                "the dispatch queue has to drain for this to mean anything");
    }
}
