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
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// `postAtFrontOfQueue` runs ahead of work already queued on the handler.
/// It used to be a plain `post`, so the "front" runnable ran last.
public class HandlerFrontOfQueueTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static Runnable record(final List<String> log, final String name) {
        return new Runnable() {
            @Override
            public void run() {
                synchronized (log) {
                    log.add(name);
                }
            }
        };
    }

    @Test
    public void mainLooperRunsFrontWorkFirst() {
        AndroidTestSupport.context();
        List<String> log = new ArrayList<String>();
        Handler h = new Handler(Looper.getMainLooper());
        h.post(record(log, "a"));
        h.post(record(log, "b"));
        h.postAtFrontOfQueue(record(log, "front"));
        h.sendMessageAtFrontOfQueue(Message.obtain(h, record(log, "frontMsg")));
        MainThreadRule.drain();
        assertEquals("[frontMsg, front, a, b]", log.toString());
    }

    @Test
    public void mainLooperSkipsRemovedWorkAndKeepsOrder() {
        AndroidTestSupport.context();
        List<String> log = new ArrayList<String>();
        Handler h = new Handler(Looper.getMainLooper());
        Runnable gone = record(log, "gone");
        h.post(gone);
        h.post(record(log, "a"));
        h.removeCallbacks(gone);
        h.postAtFrontOfQueue(record(log, "front"));
        h.post(record(log, "b"));
        MainThreadRule.drain();
        assertEquals("[front, a, b]", log.toString());
    }

    @Test
    public void backgroundLooperRunsFrontWorkFirst() throws Exception {
        HandlerThread t = new HandlerThread("front-of-queue");
        t.start();
        try {
            final Object gate = new Object();
            final boolean[] open = new boolean[1];
            final List<String> log = new ArrayList<String>();
            Handler h = new Handler(t.getLooper());
            // Holds the looper while the rest is queued behind it.
            h.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (gate) {
                        while (!open[0]) {
                            try {
                                gate.wait();
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                    }
                }
            });
            h.post(record(log, "a"));
            h.post(record(log, "b"));
            h.postAtFrontOfQueue(record(log, "front"));
            final Object done = new Object();
            final boolean[] finished = new boolean[1];
            h.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (done) {
                        finished[0] = true;
                        done.notifyAll();
                    }
                }
            });
            synchronized (gate) {
                open[0] = true;
                gate.notifyAll();
            }
            synchronized (done) {
                long end = System.currentTimeMillis() + 5000;
                while (!finished[0] && System.currentTimeMillis() < end) {
                    done.wait(100);
                }
            }
            assertTrue("the looper ran the queue", finished[0]);
            synchronized (log) {
                assertEquals("[front, a, b]", log.toString());
            }
        } finally {
            t.quit();
        }
    }
}
