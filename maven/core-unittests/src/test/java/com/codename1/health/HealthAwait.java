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
package com.codename1.health;

import com.codename1.ui.CN;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Waits for a health operation to settle.
 *
 * <p>Needed because results are delivered on the EDT on every backend. A test
 * runs off the EDT, so a store that used to answer inline on the calling thread
 * now queues the delivery, and {@code isDone()} immediately after the call is
 * false. That is the contract working, not a hang: the assertions these tests
 * make are still the right ones, they just have to be made after the delivery
 * rather than before it.</p>
 *
 * <p>Two waits, because the caller can be on either side. Off the EDT a plain
 * bounded poll is right and the event loop runs freely. On the EDT the poll
 * would block the very loop that has to deliver, so
 * {@code CN.invokeAndBlock} moves the waiting elsewhere and keeps it
 * pumping.</p>
 */
final class HealthAwait {

    private static final long LIMIT_MILLIS = 10_000L;

    private HealthAwait() {
    }

    /** Blocks until `res` has settled, and returns it for chaining. */
    static <T> AsyncResource<T> settled(final AsyncResource<T> res) {
        if (res.isDone()) {
            return res;
        }
        if (CN.isEdt()) {
            CN.invokeAndBlock(new Runnable() {
                public void run() {
                    poll(res);
                }
            });
        } else {
            poll(res);
        }
        assertTrue(res.isDone(),
                "the operation must settle rather than hang");
        return res;
    }

    /**
     * Settles `res` and returns the failure it carries, or null if it
     * succeeded.
     *
     * <p>Five test classes each had their own copy of this, all of them
     * registering an {@code except} callback and reading the result straight
     * back on the assumption that a callback attached to an already-failed
     * resource fires inline on the registering thread. That assumption no
     * longer holds: health results are delivered on the EDT whether the
     * listener arrives before the outcome or after it, so an off-EDT caller
     * gets the failure queued. Waiting for it is the same thing {@link
     * #settled} does one step earlier, and having one copy means the next
     * change to the delivery rule has one place to land.</p>
     */
    static <T> Throwable errorOf(AsyncResource<T> res) {
        settled(res);
        // Atomics rather than a one-element array: the callback runs on the
        // EDT and the value is read from the test thread, so an unguarded
        // field would be a data race that only misbehaves under CI timing.
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        final AtomicBoolean delivered = new AtomicBoolean();
        res.except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                err.set(t);
                delivered.set(true);
            }
        });
        // Both sides, because plenty of callers ask this of a resource that
        // succeeded and expect null back. Only one of the two ever fires, so
        // waiting on `except` alone would wait out the whole limit on every
        // successful call.
        res.ready(new SuccessCallback<T>() {
            public void onSucess(T value) {
                delivered.set(true);
            }
        });
        if (!delivered.get()) {
            if (CN.isEdt()) {
                CN.invokeAndBlock(new Runnable() {
                    public void run() {
                        pollDelivered(delivered);
                    }
                });
            } else {
                pollDelivered(delivered);
            }
        }
        assertTrue(delivered.get(),
                "the failure must be delivered rather than hang");
        return err.get();
    }

    private static void pollDelivered(AtomicBoolean delivered) {
        long deadline = System.currentTimeMillis() + LIMIT_MILLIS;
        while (!delivered.get() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(5L);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }

    private static void poll(AsyncResource<?> res) {
        long deadline = System.currentTimeMillis() + LIMIT_MILLIS;
        while (!res.isDone() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(5L);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }
}
