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
import com.codename1.ui.*;

/**
 * Monitors under contention, on the device.
 *
 * Each counter is guarded by exactly one monitor -- a synchronized method takes
 * the class's or the receiver's, a block takes whatever it names -- so the
 * totals are exact, and an interpreter that treats monitorenter as a no-op
 * loses increments rather than merely reordering them.
 */
public class SyncProbe {
    static int byLock;
    static int byMethod;
    int byInstance;
    static final Object LOCK = new Object();

    static synchronized void bumpStatic() { byMethod++; }
    synchronized void bumpInstance() { byInstance++; }
    static void guarded() { synchronized (LOCK) { byLock++; } }

    public static void main(String[] a) throws Exception {
        final SyncProbe shared = new SyncProbe();
        Thread[] t = new Thread[4];
        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread(new Runnable() {
                public void run() {
                    for (int j = 0; j < 1000; j++) {
                        bumpStatic(); shared.bumpInstance(); guarded();
                    }
                }
            });
        }
        for (int i = 0; i < t.length; i++) { t[i].start(); }
        for (int i = 0; i < t.length; i++) { t[i].join(); }
        System.out.println("PROBE SyncProbe: method=" + byMethod + " instance=" + shared.byInstance
            + " lock=" + byLock + " expected=4000 each");
        new Form("Sync").show();
    }
}
