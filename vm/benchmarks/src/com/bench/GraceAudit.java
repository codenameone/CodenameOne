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
 * Grace-completeness gate born from issue 5425: a bursty small-object size
 * class allocates fresh objects WHILE the concurrent mark runs (System.gc is
 * asynchronous), each holding the only reference to an older object, then
 * goes quiet across the next GC cycle. Any grace scheme that tracks "pages
 * with fresh slots" incrementally must still trace those objects; the
 * fresh-page-stack scheme this driver was written against dropped them and
 * the sweep freed their children while still referenced. Run with
 * -DCN1_GRACE_AUDIT: every reported doomedChildren value must be zero.
 */
public class GraceAudit {
    static class Node {
        Object a, b, c;
    }

    static class Filler {
        long a, b, c, d, e, f, g, h, i2, j, k, l;
    }

    static Object[] keep = new Object[256];
    static Object[] tmp = new Object[16];
    static long checksum;

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
            // WHILE it runs: some land after the grace pass already visited (or
            // dismissed) this page, the window where queue/dedup-based grace
            // schemes lose track of fresh objects.
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
        // READ EVERY FIELD THE HAZARD STORES THROUGH. DeadFieldElimination (#5903) deletes an
        // instance field nothing reads, and Node's fields were write-only: the translated Node
        // had no fields at all, so a fresh node no longer held the only reference to its
        // child, the grace pass had nothing to rescue, and run-gc-verify.sh's nograce
        // self-test -- which re-injects exactly the missing grace pass -- caught nothing on
        // any tree since. Filler's are read for the same reason: they are what makes it a
        // page-sized filler rather than an empty object.
        for (int i = 0; i < 256; i++) {
            Object o = keep[i];
            if (o instanceof Node) {
                Node k = (Node) o;
                checksum += (k.a != null ? 5 : 0) + (k.b == k ? 7 : 0) + (k.c != null ? 11 : 0);
            }
        }
        for (int i = 0; i < 16; i++) {
            Object o = tmp[i];
            if (o instanceof Filler) {
                Filler f = (Filler) o;
                checksum += f.a + f.b + f.c + f.d + f.e + f.f + f.g + f.h + f.i2 + f.j + f.k + f.l;
            }
        }
        System.out.println("GRACE_AUDIT_DRIVER_DONE checksum=" + checksum);
    }
}
