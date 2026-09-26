/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.bench;

/**
 * Drives the monitor side table from every direction at once: four threads take
 * shared locks (lookups on live entries) while attaching monitors to short-lived
 * objects that die and are swept (inserts, then removals by the collector), so
 * lock-free readers keep racing writers on the same buckets.
 *
 * <p>Mutual exclusion is what is checked: every shared counter is only ever
 * incremented under its own lock, so a reader that returned the wrong monitor --
 * or no monitor, and created a second one -- loses updates and the totals come out
 * short. The expected totals are exact, so the output matches the JVM byte for
 * byte or the table is broken.</p>
 */
public class MonitorChurnT {
    private static final int THREADS = 4;
    private static final int ITERATIONS = 60000;
    private static final int LOCKS = 8;
    private static final Object[] SHARED = new Object[LOCKS];
    private static final long[] COUNTS = new long[LOCKS];
    static volatile Object sink;

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < LOCKS; i++) {
            SHARED[i] = new Object();
        }
        Thread[] threads = new Thread[THREADS];
        for (int t = 0; t < THREADS; t++) {
            final int seed = t;
            threads[t] = new Thread() {
                public void run() {
                    int local = 0;
                    for (int i = 0; i < ITERATIONS; i++) {
                        Object temp = new int[4];
                        synchronized (temp) {
                            local++;
                        }
                        sink = temp;
                        int k = (i + seed) % LOCKS;
                        synchronized (SHARED[k]) {
                            COUNTS[k]++;
                        }
                    }
                    if (local != ITERATIONS) {
                        System.out.println("local count wrong: " + local);
                    }
                }
            };
            threads[t].start();
        }
        for (int t = 0; t < THREADS; t++) {
            threads[t].join();
        }
        long total = 0;
        for (int i = 0; i < LOCKS; i++) {
            total += COUNTS[i];
        }
        System.out.println("total=" + total + " expected=" + ((long) THREADS * ITERATIONS));
        for (int i = 0; i < LOCKS; i++) {
            System.out.println("lock " + i + " = " + COUNTS[i]);
        }
    }
}
