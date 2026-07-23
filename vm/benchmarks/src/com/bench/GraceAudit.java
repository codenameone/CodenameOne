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
 * Repro driver for issue 5425: a bursty small-object size class that goes
 * quiet across GC cycle boundaries. Fresh objects allocated into a page AFTER
 * that page's fresh-stack entry was consumed by the grace pass (same epoch)
 * are never grace-traced if the size class receives no allocation in the next
 * epoch before its grace pass. Run with -DCN1_GRACE_AUDIT to count them.
 */
public class GraceAudit {
    static class Node {
        Object a, b, c;
    }

    static class Filler {
        long a, b, c, d, e, f, g, h, i2, j, k, l;
    }

    static Object sink;
    static Object[] keep = new Object[256];
    static Object[] tmp = new Object[16];
    static long checksum;

    // deterministic LCG so runs are comparable without java.util.Random
    static long seed = 42;

    static int next(int bound) {
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        int v = (int) (seed >>> 33) % bound;
        return v < 0 ? v + bound : v;
    }

    public static void main(String[] args) throws Exception {
        for (int round = 0; round < 120; round++) {
            // Refill payload children (each will end up referenced ONLY by an
            // unpublished fresh node).
            for (int j = 0; j < 256; j++) {
                if (keep[j] == null) {
                    Node k = new Node();
                    k.b = k;
                    keep[j] = k;
                }
            }
            // Kick a concurrent mark, then keep allocating fresh dropped nodes
            // WHILE it runs: allocations landing after the grace pass consumed
            // this page's fresh-stack entry stay unqueued for this epoch.
            System.gc();
            for (int slice = 0; slice < 40; slice++) {
                for (int i = 0; i < 8; i++) {
                    Node n = new Node();
                    int j = (slice * 8 + i) & 255;
                    n.a = keep[j];
                    keep[j] = null;
                    tmp[0] = n;
                    tmp[0] = null;
                }
                Thread.sleep(3);
            }
            // Quiet phase: no Node allocation at all across the next cycle, so
            // the Node page is never re-queued; filler drives the byte trigger.
            for (int i = 0; i < 120000; i++) {
                Filler f = new Filler();
                f.b = i;
                tmp[i & 15] = f;
            }
            System.gc();
            Thread.sleep(150);
        }
        for (int i = 0; i < 16; i++) {
            if (tmp[i] != null) {
                checksum++;
            }
        }
        for (int i = 0; i < 256; i++) {
            if (keep[i] != null) {
                checksum += 3;
            }
        }
        System.out.println("GRACE_AUDIT_DRIVER_DONE checksum=" + checksum + " sink=" + (sink == null));
    }
}
