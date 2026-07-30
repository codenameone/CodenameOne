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
