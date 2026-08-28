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
 * Duty-cycle accounting across mutator lifetimes.
 *
 * <p>Worker threads are created, allocate hard enough to be parked by the collector, and
 * then exit -- repeatedly. Every generation takes real stalls and then takes its own
 * counters with it when {@code markDeadThread()} drops its thread-local data. The
 * {@code [GCSTALL-T]} stall clock must keep rising across those exits; a source that only
 * sums the LIVE threads falls back to zero at each generation boundary and reports the
 * process as though it had never been stopped at all.</p>
 */
public class MutatorChurnDuty {
    private static final int GENERATIONS = 40;
    private static final int WORKERS = 4;
    private static final int ROUNDS = 20000;
    private static final int WIDTH = 700;

    static Object sink;
    static long checksum;

    static final class Node { int v; Node next; }

    public static void main(String[] args) {
        for (int g = 0; g < GENERATIONS; g++) {
            Thread[] t = new Thread[WORKERS];
            for (int w = 0; w < WORKERS; w++) {
                t[w] = new Thread() {
                    public void run() {
                        Object keep = null;
                        for (int r = 0; r < ROUNDS; r++) {
                            Node head = null;
                            for (int i = 0; i < WIDTH; i++) {
                                Node n = new Node();
                                n.v = i;
                                n.next = head;
                                head = n;
                            }
                            if ((r & 63) == 0) { keep = head; }
                        }
                        sink = keep;
                    }
                };
                t[w].start();
            }
            for (int w = 0; w < WORKERS; w++) {
                try { t[w].join(); } catch (InterruptedException e) { }
            }
            checksum += g;
        }
        System.out.println("GENERATIONS=" + GENERATIONS + " WORKERS=" + WORKERS);
        System.out.println("RESULT=" + checksum);
        System.out.println("MUTATOR_CHURN_DUTY_DONE");
    }
}
