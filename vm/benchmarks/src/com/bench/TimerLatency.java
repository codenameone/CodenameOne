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
 * Sleep/timer fidelity probe under allocation churn. Guards the
 * Thread.sleep signal-truncation defect found via issue 5425's PR CI: the
 * conservative-roots collector signal-stops SLEEPING threads to scan their
 * stacks, and usleep()/nanosleep() return EINTR without ever being
 * restarted (SA_RESTART excludes them), so a single-usleep Thread.sleep
 * was measured sleeping ~20ms of a requested 3000ms on the Linux port --
 * every java.util.Timer task (ToastBar show/expiry among them) fired
 * almost immediately. Deviations are SIGNED: the broken behavior is EARLY
 * fire, which a max(late, 0) metric silently clamps away.
 */
public class TimerLatency {
    static final Object lock = new Object();
    static int fired;
    static long[] deviation = new long[30];
    static volatile boolean stop;

    public static void main(String[] args) throws Exception {
        Thread churn = new Thread(new Runnable() {
            public void run() {
                java.util.Vector<Object> keep = new java.util.Vector<Object>();
                int i = 0;
                while (!stop) {
                    byte[] b = new byte[4096];
                    b[0] = (byte) i;
                    if ((i++ & 127) == 0) {
                        keep.clear();
                    }
                    keep.addElement(b);
                    if ((i & 1023) == 0) {
                        try { Thread.sleep(1); } catch (Exception e) { }
                    }
                }
            }
        });
        churn.start();
        for (int i = 0; i < 30; i++) {
            final long scheduled = System.currentTimeMillis();
            final long delay = 100 + (i % 5) * 100;
            final int slot = i;
            new java.util.Timer().schedule(new java.util.TimerTask() {
                public void run() {
                    long d = System.currentTimeMillis() - scheduled - delay;
                    synchronized (lock) {
                        deviation[slot] = d;
                        fired++;
                        lock.notifyAll();
                    }
                }
            }, delay);
            Thread.sleep(40);
        }
        long deadline = System.currentTimeMillis() + 20000;
        synchronized (lock) {
            while (fired < 30 && System.currentTimeMillis() < deadline) {
                lock.wait(500);
            }
        }
        // direct sleep-truncation probe: the broken VM returned in ~20ms
        long minSleep = Long.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            long t0 = System.currentTimeMillis();
            Thread.sleep(1500);
            minSleep = Math.min(minSleep, System.currentTimeMillis() - t0);
        }
        stop = true;
        long maxLate = 0, maxEarly = 0;
        for (long d : deviation) {
            maxLate = Math.max(maxLate, d);
            maxEarly = Math.min(maxEarly, d);
        }
        System.out.println("fired=" + fired
                + " max_late_ms=" + maxLate
                + " max_early_ms=" + (-maxEarly)
                + " min_sleep1500_ms=" + minSleep);
        boolean healthy = fired == 30 && maxEarly > -50 && minSleep >= 1400;
        System.out.println(healthy ? "TIMER_LATENCY_OK" : "TIMER_LATENCY_DEGRADED");
        System.out.println("TIMER_LATENCY_DONE");
        System.exit(0);
    }
}
