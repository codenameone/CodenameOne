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
 * Bulk object-array copies while a collection is in progress.
 *
 * <p>The SATB barrier on {@code System.arraycopy} and {@code Object[].clone()} has to log
 * the references a bulk copy moves, and the per-store barrier's enqueue takes the SATB
 * mutex once per reference. This driver exists to price that: a large array of OLD objects
 * copied over and over while a second thread keeps the collector busy, so the barrier is
 * armed for most of the run.</p>
 */
public class BulkCopyCost {
    private static final int ARRAY = 200000;
    private static final int COPIES = 400;
    private static final int CHURN = 60000;

    static final class Node { int v; Node peer; }

    static Object[] source;
    static Object[] dest;
    static Object sink;
    static volatile boolean stop;
    static long checksum;

    public static void main(String[] args) {
        source = new Object[ARRAY];
        for (int i = 0; i < ARRAY; i++) {
            Node n = new Node();
            n.v = i;
            source[i] = n;
        }
        dest = new Object[ARRAY];

        Thread churn = new Thread() {
            public void run() {
                Object last = null;
                while (!stop) {
                    for (int i = 0; i < CHURN; i++) {
                        Node n = new Node();
                        n.peer = (Node) last;
                        if ((i & 127) == 0) { last = n; }
                    }
                    sink = last;
                }
            }
        };
        churn.start();

        long t0 = System.currentTimeMillis();
        for (int r = 0; r < COPIES; r++) {
            System.arraycopy(source, 0, dest, 0, ARRAY);
            Object[] c = (Object[]) source.clone();
            checksum += ((Node) c[r % ARRAY]).v;
        }
        long elapsed = System.currentTimeMillis() - t0;
        stop = true;
        try { churn.join(); } catch (InterruptedException e) { }

        System.out.println("COPIES=" + COPIES + " ARRAY=" + ARRAY);
        System.out.println("COPY_MS=" + elapsed);
        System.out.println("RESULT=" + checksum);
        System.out.println("BULK_COPY_COST_DONE");
    }
}
