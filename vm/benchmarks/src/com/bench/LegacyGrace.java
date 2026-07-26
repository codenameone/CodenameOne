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
 * The LEGACY-path twin of {@link GraceAudit}. Objects above
 * CN1_BIBOP_MAX_OBJECT never reach the page heap, so the BiBOP grace pass
 * cannot see them, yet the legacy sweep grants them exactly the same
 * one-cycle grace. This driver builds the hazard shape directly: a fresh
 * legacy-sized parent takes the ONLY reference to an older object and is then
 * dropped, in the quiet window before a collection, so the parent enters the
 * heap table fresh (gcMark == -1) at mark start and its child is reachable
 * through nothing else.
 *
 * <p>Run under -DCN1_GRACE_AUDIT: every [GRACE-AUDIT-LEGACY] line must report
 * doomedChildren=0.</p>
 */
public class LegacyGrace {
    /** Small older object: the child that must not be freed while referenced. */
    static class Payload {
        Object self;
        long tag;
    }

    /** Filler drives the allocation-volume GC trigger without adding hazards. */
    static class Filler {
        long a, b, c, d, e, f, g, h, i2, j, k, l;
    }

    static final int KEEP = 10000;
    /** > CN1_BIBOP_MAX_OBJECT (512) once the array header is added. */
    static final int LEGACY_ELEMENTS = 65;

    static Object[] keep = new Object[KEEP];
    static Object[] sink = new Object[16];
    static long checksum;

    public static void main(String[] args) throws Exception {
        for (int round = 0; round < 12; round++) {
            // Older generation: allocate the children and let them survive a
            // collection so they are NOT themselves in grace when the hazard runs.
            for (int j = 0; j < KEEP; j++) {
                if (keep[j] == null) {
                    Payload p = new Payload();
                    p.self = p;
                    p.tag = round * 31L + j;
                    keep[j] = p;
                }
            }
            quiesce();

            // THE HAZARD, in a strictly quiet window. Two properties make it a
            // hazard at all, and both are easy to lose:
            //
            // NOTHING ELSE IS ALLOCATED HERE. transfer() creates KEEP (10,000)
            // arrays of LEGACY_ELEMENTS (65) references -- about 560 bytes each,
            // 5.6 MB in total, against the collector's 24 MB allocation-volume
            // trigger. Raising KEEP or LEGACY_ELEMENTS far enough to cross that
            // budget would start a collection in the middle of the window and
            // invalidate everything below.
            //
            // NO MARK IS IN FLIGHT. The preceding quiesce() drained one, so the
            // SATB barriers are DISARMED and nothing logs the reference as it
            // moves out of keep[] and into an object the collector has never
            // seen. During a mark those barriers cover this exact move, so a
            // driver that keeps the collector busy silently tests nothing.
            transfer();

            // Age the hazard out over three collections with no allocation at
            // all, so every cycle is a clean epoch advance: the parents are
            // graced into the heap table, then age, while the children -- traced
            // by nothing -- fall past the sweep's free threshold.
            quiesce();
            quiesce();
            quiesce();

            // Churn phase: unrelated volume, well after the hazard, to keep the
            // allocator and the page heap in a realistic state.
            for (int i = 0; i < 60000; i++) {
                Filler f = new Filler();
                f.b = i;
                sink[i & 15] = f;
            }
            quiesce();
        }
        for (int i = 0; i < 16; i++) {
            if (sink[i] != null) {
                checksum++;
            }
        }
        for (int i = 0; i < KEEP; i++) {
            if (keep[i] != null) {
                checksum += 3;
            }
        }
        System.out.println("LEGACY_GRACE_DRIVER_DONE checksum=" + checksum);
    }

    /**
     * Hands each child's ONLY reference to a fresh legacy-sized parent and drops
     * the parent. Its own frame is the only thing that ever held those parents,
     * so returning from it is what makes them unreachable.
     */
    private static void transfer() {
        for (int i = 0; i < KEEP; i++) {
            Object[] parent = new Object[LEGACY_ELEMENTS];
            parent[0] = keep[i];
            parent[LEGACY_ELEMENTS - 1] = keep[i];
            keep[i] = null;
            sink[0] = parent;
            sink[0] = null;
        }
    }

    /** One full collection with nothing allocated during it. */
    private static void quiesce() throws Exception {
        scrub(SCRUB_DEPTH);
        System.gc();
        Thread.sleep(250);
    }

    static final int SCRUB_DEPTH = 400;
    static long scrubSink;

    /**
     * Overwrites the native C stack the transfer loop ran on.
     *
     * <p>Without this the test measures nothing. ParparVM's collector scans each
     * thread's native stack and registers CONSERVATIVELY, so a machine word left
     * behind by a returned frame still marks whatever it points at. A dropped
     * object therefore stays reachable-by-accident for as long as nothing
     * overwrites that word -- and a driver that drops objects and then sleeps
     * leaves them pinned by its own dead frames, so the hazard it just built is
     * quietly retained and the run comes back green. Real applications scrub
     * their stacks continuously simply by running; a test has to do it on
     * purpose. Each frame here writes its own locals, so the recursion walks
     * over the region the hazard loop used.</p>
     */
    private static long scrub(int depth) {
        long a = depth * 0x5DEECE66DL;
        long b = a ^ 0x1234567890ABCDEFL;
        long c = b + 0x0F0F0F0F0F0F0F0FL;
        long d = c ^ 0x7FFFFFFFFFFFFFFFL;
        if (depth > 0) {
            a += scrub(depth - 1);
        }
        scrubSink = a ^ b ^ c ^ d;
        return scrubSink;
    }
}
