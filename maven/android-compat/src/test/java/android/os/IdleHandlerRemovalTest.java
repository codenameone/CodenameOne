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
package android.os;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.ui.Display;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// `removeIdleHandler` stops a handler: one removed before its idle run is
/// never invoked, and one that asked to stay is not invoked again. Removal
/// used to do nothing, so a handler returning true ran forever.
public class IdleHandlerRemovalTest {

    private static final class Counting implements MessageQueue.IdleHandler {
        final AtomicInteger runs = new AtomicInteger();

        @Override
        public boolean queueIdle() {
            runs.incrementAndGet();
            return true;
        }
    }

    /// Waits off the EDT until `h` has run `n` times.
    private static void awaitRuns(Counting h, int n) throws InterruptedException {
        long end = System.currentTimeMillis() + 10000;
        while (h.runs.get() < n && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        assertTrue("the idle handler ran " + n + " times", h.runs.get() >= n);
    }

    private static void onEdt(Runnable r) {
        Display.getInstance().callSeriallyAndWait(r);
    }

    private static void remove(final MessageQueue q, final Counting h) {
        onEdt(new Runnable() {
            @Override
            public void run() {
                q.removeIdleHandler(h);
            }
        });
    }

    @Test
    public void removedBeforeItsRunNeverRuns() throws Exception {
        AndroidTestSupport.context();
        final MessageQueue q = Looper.getMainLooper().getQueue();
        final Counting removed = new Counting();
        final Counting sentinel = new Counting();
        onEdt(new Runnable() {
            @Override
            public void run() {
                q.addIdleHandler(removed);
                q.removeIdleHandler(removed);
                q.addIdleHandler(sentinel);
            }
        });
        awaitRuns(sentinel, 3);
        remove(q, sentinel);
        remove(q, removed);
        assertEquals(0, removed.runs.get());
    }

    @Test
    public void removedAfterRunningStops() throws Exception {
        AndroidTestSupport.context();
        final MessageQueue q = Looper.getMainLooper().getQueue();
        final Counting kept = new Counting();
        onEdt(new Runnable() {
            @Override
            public void run() {
                q.addIdleHandler(kept);
            }
        });
        awaitRuns(kept, 1);
        final int[] before = new int[1];
        final Counting sentinel = new Counting();
        onEdt(new Runnable() {
            @Override
            public void run() {
                q.removeIdleHandler(kept);
                before[0] = kept.runs.get();
                q.addIdleHandler(sentinel);
            }
        });
        awaitRuns(sentinel, 3);
        remove(q, sentinel);
        assertEquals(before[0], kept.runs.get());
    }
}
