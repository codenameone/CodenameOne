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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/// `quitSafely()` delivers every message already due and drops the future
/// ones. It used to be quit(), which dropped the due ones too.
public class LooperQuitSafelyTest {

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
    public void dueMessagesRunFutureOnesDoNot() throws Exception {
        HandlerThread t = new HandlerThread("quit-safely");
        t.start();
        final Object gate = new Object();
        final boolean[] open = new boolean[1];
        final boolean[] blocking = new boolean[1];
        final List<String> log = new ArrayList<String>();
        Handler h = new Handler(t.getLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                synchronized (gate) {
                    blocking[0] = true;
                    gate.notifyAll();
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
        synchronized (gate) {
            while (!blocking[0]) {
                gate.wait();
            }
        }
        h.post(record(log, "a"));
        h.post(record(log, "b"));
        h.postDelayed(record(log, "later"), 60000);
        t.quitSafely();
        synchronized (gate) {
            open[0] = true;
            gate.notifyAll();
        }
        t.join(10000);
        assertFalse("looper did not exit", t.isAlive());
        synchronized (log) {
            assertEquals("[a, b]", log.toString());
        }
    }
}
