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

import java.util.Hashtable;

/**
 * Heap-integrity workload for the -DCN1_GC_VERIFY gate (issue 5425).
 *
 * <p>Builds the reference-shapes the collector's keep-rules are most likely to
 * mishandle, so the post-sweep verifier has something to find if one of them
 * regresses: fresh objects taking the ONLY reference to an older object and
 * being dropped while a concurrent mark runs (the grace-pass hazard #5442
 * fixed), the same shape on the legacy path with objects above
 * CN1_BIBOP_MAX_OBJECT, retained map entries whose values are separately
 * allocated arrays (the reporter's dictionary), and survivor sets that outlive
 * several collections so objects mature into the legacy heap.</p>
 *
 * <p>The verifier is what asserts; this program only has to keep the
 * collector working and finish. It also prints a checksum so the run is
 * compared against a host JVM like every other driver.</p>
 */
public class GcVerifyApp {
    static class Node {
        Object a;
        Object b;
        long tag;
    }

    static class Filler {
        long a, b, c, d, e, f, g, h, i2, j, k, l;
    }

    static final int KEEP = 512;
    static final int LEGACY_ELEMENTS = 65;   // > CN1_BIBOP_MAX_OBJECT once boxed in an array

    static Object[] keep = new Object[KEEP];
    static Object[] sink = new Object[16];
    static Hashtable dict = new Hashtable();
    static long checksum;
    static long scrubSink;

    public static void main(String[] args) throws Exception {
        for (int round = 0; round < 12; round++) {
            refill(round);

            // Hazard 1 -- page heap, DURING a mark. System.gc() is asynchronous,
            // so these fresh nodes land while the collector is marking: some
            // after the grace pass has already visited their page.
            System.gc();
            for (int slice = 0; slice < 24; slice++) {
                for (int i = 0; i < 8; i++) {
                    Node n = new Node();
                    int j = (slice * 8 + i) & (KEEP - 1);
                    n.a = keep[j];
                    keep[j] = null;
                    sink[0] = n;
                    sink[0] = null;
                }
                Thread.sleep(2);
            }

            // Hazard 2 -- legacy path, in a QUIET window. Outside a mark the
            // SATB barriers are disarmed, so nothing logs the reference as it
            // moves into an object the collector has never seen.
            refill(round);
            settle();
            transferToLegacy();
            settle();
            settle();

            // Hazard 3 -- retained map whose values are separately allocated
            // arrays, dropped wholesale. This is the reporter's dictionary
            // shape: entries mature into the legacy heap while their payload
            // arrays stay page-resident, so the two halves of the collector
            // must agree about when the pair dies.
            dict = new Hashtable();
            for (int i = 0; i < 4000; i++) {
                dict.put("k" + i, new byte[64]);
            }
            for (int i = 0; i < 40000; i++) {
                Filler f = new Filler();
                f.b = i;
                sink[i & 15] = f;
            }
            settle();
        }

        for (int i = 0; i < KEEP; i++) {
            if (keep[i] != null) {
                checksum += 3;
            }
        }
        for (int i = 0; i < 16; i++) {
            if (sink[i] != null) {
                checksum++;
            }
        }
        checksum += dict.size();
        System.out.println("RESULT=" + checksum);
        System.out.println("GC_VERIFY_APP_DONE");
    }

    private static void refill(int round) {
        for (int j = 0; j < KEEP; j++) {
            if (keep[j] == null) {
                Node n = new Node();
                n.b = n;
                n.tag = round * 31L + j;
                keep[j] = n;
            }
        }
    }

    private static void transferToLegacy() {
        for (int i = 0; i < KEEP; i++) {
            Object[] parent = new Object[LEGACY_ELEMENTS];
            parent[0] = keep[i];
            parent[LEGACY_ELEMENTS - 1] = keep[i];
            keep[i] = null;
            sink[0] = parent;
            sink[0] = null;
        }
    }

    /**
     * One collection with the dropped references genuinely unreachable.
     *
     * <p>The stack scrub is load-bearing: the collector scans native stacks
     * conservatively, so a word left behind by a returned frame keeps marking
     * whatever it points at. Without overwriting that region the program pins
     * the hazard it just built and the verifier has nothing to check.</p>
     */
    private static void settle() throws Exception {
        scrub(300);
        System.gc();
        Thread.sleep(120);
    }

    private static long scrub(int depth) {
        long a = depth * 0x5DEECE66DL;
        long b = a ^ 0x1234567890ABCDEFL;
        long c = b + 0x0F0F0F0F0F0F0F0FL;
        if (depth > 0) {
            a += scrub(depth - 1);
        }
        scrubSink = a ^ b ^ c;
        return scrubSink;
    }
}
