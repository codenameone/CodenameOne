/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.bench;

/**
 * What one parked thread costs. This is the number that decides whether a
 * server-side ParparVM can serve a connection per thread or has to grow
 * continuations: if a thread costs 300KB, ten thousand connections is 3GB and the
 * answer is no; if it costs 20KB it is 200MB and the answer is yes.
 *
 * Spawns CN1_TC_THREADS (default 512) threads that park on a monitor and holds
 * them, so peak RSS measured from outside is the steady state with them all
 * alive. Compare against Noop, which is the same runtime with no threads.
 *
 * Run:
 *   translate-and-build.sh ThreadCost /tmp/threadcost
 *   CN1_TC_THREADS=512 /usr/bin/time -l /tmp/threadcost
 */
public class ThreadCost {
    private static final Object LOCK = new Object();
    private static int started;

    public static void main(String[] args) throws Exception {
        int n = envInt("CN1_TC_THREADS", 512);
        int holdMs = envInt("CN1_TC_HOLD_MS", 3000);
        for (int i = 0; i < n; i++) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    synchronized (LOCK) {
                        started++;
                        try {
                            // Parked, not spinning: a spinning thread would measure
                            // the scheduler instead of the footprint.
                            LOCK.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            });
            t.start();
        }
        // Let every thread reach its park before the measurement is taken.
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            synchronized (LOCK) {
                if (started >= n) {
                    break;
                }
            }
            Thread.sleep(5);
        }
        Thread.sleep(holdMs);
        System.out.println("threads=" + n + " started=" + started);
    }

    private static int envInt(String name, int fallback) {
        String v = System.getenv(name);
        if (v == null || v.length() == 0) {
            return fallback;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
