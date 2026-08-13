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
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When a port's deferred work actually runs.
 *
 * <p>Two different questions get two different entry points, and conflating them cost a stack
 * overflow one way and a stranded record the other. {@code requestReplayAfterDrain} means "as soon
 * as a delivery can reach a listener", and must therefore refuse to run while a drain is in flight.
 * {@code runWhenListenerRegisters} means "when someone starts listening", and must never run
 * inline.</p>
 */
public class WearableReplaySchedulingTest extends UITestBase {

    /**
     * A replay asked for during a drain is held, not run.
     *
     * <p>The eviction path calls this from inside the drain it would re-enter. Running it there put
     * the re-offered payload straight back into the queue it was evicted from, which evicted
     * another one-shot, which asked for a replay, which ran immediately because a listener was
     * still registered -- one stack frame deeper each time, until the process overflowed.</p>
     */
    @Test
    public void aReplayRequestedDuringADrainIsDeferred() {
        final AtomicInteger ran = new AtomicInteger();
        WearableDataListener listener = new WearableDataListener() {
            public void dataChanged(WearableMessage data) {
            }

            public void dataRemoved(String path) {
            }
        };
        try {
            WearableConnection.addDataListener(listener);

            // Idle and listening: this is the case that may run inline, and the one the port
            // relies on when it is simply catching up outside a drain.
            WearableConnection.requestReplayAfterDrain("idle", new Runnable() {
                public void run() {
                    ran.incrementAndGet();
                }
            });
            assertEquals(1, ran.get(), "with a listener and nothing parked the replay runs now");

            // The same request made from inside a drain has to wait for it.
            setDraining(true);
            try {
                WearableConnection.requestReplayAfterDrain("mid-drain", new Runnable() {
                    public void run() {
                        ran.incrementAndGet();
                    }
                });
                assertEquals(1, ran.get(),
                        "a replay must not run while a drain is in flight -- that is the recursion");
            } finally {
                setDraining(false);
            }

            // And it is not lost: registering drains what was held.
            WearableConnection.removeDataListener(listener);
            WearableConnection.addDataListener(listener);
            assertEquals(2, ran.get(), "the held replay runs on the next drain");
        } finally {
            WearableConnection.removeDataListener(listener);
        }
    }

    /**
     * A message listener registering releases what was waiting on it.
     *
     * <p>The Android bridge spools one-shot messages to disk and drains at construction, before the
     * app has registered anything. Nothing re-ran that drain but inbound traffic, so a cold launch
     * with no new traffic left the previous run's messages on disk indefinitely. The data queue's
     * replay hand-off cannot cover it: it fires only for data listeners.</p>
     */
    @Test
    public void aMessageListenerReleasesWhatWasWaitingForOne() {
        final AtomicInteger ran = new AtomicInteger();
        WearableConnection.runWhenListenerRegisters("spool", new Runnable() {
            public void run() {
                ran.incrementAndGet();
            }
        });
        assertEquals(0, ran.get(), "this one never runs inline, whatever the queue looks like");

        WearableMessageListener listener = new WearableMessageListener() {
            public WearableMessage messageReceived(WearableMessage message,
                    boolean expectsReply) {
                return null;
            }
        };
        try {
            WearableConnection.addMessageListener(listener);
            assertEquals(1, ran.get(), "registering a MESSAGE listener has to release it");

            // Re-armed from inside the action -- which the spool drain does whenever a record
            // still needs the other kind of listener -- is held for the NEXT registration rather
            // than being wiped by the pass that is running.
            WearableConnection.runWhenListenerRegisters("spool", new Runnable() {
                public void run() {
                    ran.incrementAndGet();
                }
            });
            WearableConnection.removeMessageListener(listener);
            WearableConnection.addMessageListener(listener);
            assertEquals(2, ran.get(), "and the re-armed request runs on the following one");
        } finally {
            WearableConnection.removeMessageListener(listener);
        }
    }

    /** Repeated requests under one key collapse, so a rescan cannot accumulate one per eviction. */
    @Test
    public void oneKeyIsOneAction() {
        final AtomicInteger first = new AtomicInteger();
        final AtomicInteger second = new AtomicInteger();
        WearableConnection.runWhenListenerRegisters("rescan", new Runnable() {
            public void run() {
                first.incrementAndGet();
            }
        });
        WearableConnection.runWhenListenerRegisters("rescan", new Runnable() {
            public void run() {
                second.incrementAndGet();
            }
        });

        WearableMessageListener listener = new WearableMessageListener() {
            public WearableMessage messageReceived(WearableMessage message,
                    boolean expectsReply) {
                return null;
            }
        };
        try {
            WearableConnection.addMessageListener(listener);
            assertEquals(0, first.get(), "the superseded action must not also run");
            assertEquals(1, second.get());
        } finally {
            WearableConnection.removeMessageListener(listener);
        }
    }

    /** A throwing action costs itself, not the ones queued behind it. */
    @Test
    public void oneFailingActionDoesNotStrandTheRest() {
        final AtomicInteger ran = new AtomicInteger();
        WearableConnection.runWhenListenerRegisters("throws", new Runnable() {
            public void run() {
                throw new IllegalStateException("port failed");
            }
        });
        WearableConnection.runWhenListenerRegisters("survives", new Runnable() {
            public void run() {
                ran.incrementAndGet();
            }
        });

        WearableMessageListener listener = new WearableMessageListener() {
            public WearableMessage messageReceived(WearableMessage message,
                    boolean expectsReply) {
                return null;
            }
        };
        try {
            WearableConnection.addMessageListener(listener);
            assertTrue(ran.get() == 1, "the second action still ran");
        } finally {
            WearableConnection.removeMessageListener(listener);
        }
    }

    /**
     * Drives the drain guard directly.
     *
     * <p>Reflection because the guard is private state with no public setter, and inventing one
     * purely for a test would widen the class's surface for every caller. The alternative -- a real
     * concurrent drain -- would be timing-dependent, and this asserts a decision, not a race.</p>
     */
    private static void setDraining(boolean draining) {
        try {
            java.lang.reflect.Method m = WearableConnection.class
                    .getDeclaredMethod("setDrainingData", boolean.class);
            m.setAccessible(true);
            m.invoke(null, Boolean.valueOf(draining));
        } catch (Exception e) {
            throw new IllegalStateException("the drain guard moved; this test must follow it", e);
        }
    }
}
