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

    static final int ROUNDS = 8;
    static final int KEEP = 512;
    static final int LEGACY_ELEMENTS = 65;   // > CN1_BIBOP_MAX_OBJECT once boxed in an array

    static Object[] keep = new Object[KEEP];
    static Object[] sink = new Object[16];
    static Hashtable dict = new Hashtable();
    static long checksum;
    static long scrubSink;

    public static void main(String[] args) throws Exception {
        // The JVM reference run says so on the command line. It has no ParparVM collector to ask
        // -- the native below resolves only in a translated build -- and nothing to synchronize
        // with either, since it runs to establish what the program computes rather than to
        // verify a heap. Catching a linkage error instead would have been neater, except that
        // java.lang.UnsatisfiedLinkError is not part of the ParparVM class library, so naming it
        // breaks the very build that does have the collector.
        handshake = args == null || args.length == 0 || !"reference".equals(args[0]);
        for (int round = 0; round < ROUNDS; round++) {
            // HAZARD 1 -- page heap, allocated DURING a concurrent mark. This is
            // the shape that breaks a missing grace pass, and the timing is the
            // whole point: System.gc() is asynchronous, so the nodes below are
            // created while the mark is running, each taking the only reference
            // to an older object and then being dropped. The slices with a sleep
            // in between are what spread the allocation across the mark rather
            // than racing past it in a burst.
            refill(round);
            long beforeMark = marksDone();
            System.gc();
            // Allocate INSIDE the mark, established by asking the collector rather than by
            // sleeping. The old loop spread 40 slices across 3ms sleeps and hoped they landed
            // during the mark; when the machine was loaded they did not, and the fault-injected
            // half of the gate then reported that the verifier could not detect a defect the
            // workload had never created.
            // Exactly the 40 slices of 8 this workload has always done -- the count is load
            // bearing. It empties 320 of the 512 keep[] slots, and the 192 that survive are what
            // a dropped fresh node can still be holding the only reference to. Allocating more
            // than this empties the array outright, leaves no survivor, and the hazard stops
            // existing: an earlier attempt at this fix looped for as long as the mark lasted and
            // produced a run with three times the collections and nothing to find.
            //
            // What changed is only WHEN they run. Each slice is placed inside a live mark by
            // asking the collector, instead of by sleeping 3ms and hoping; if a mark ends
            // early, the next slice starts another one and waits for it.
            for (int slice = 0; slice < 40; slice++) {
                if (handshake) {
                    if (!marking()) {
                        System.gc();
                        if (!awaitMarkStart(20000)) {
                            System.out.println("GC_VERIFY_NO_MARK round=" + round
                                    + " slice=" + slice);
                        }
                    }
                } else if (slice > 0) {
                    Thread.sleep(3);
                }
                for (int i = 0; i < 8; i++) {
                    Node n = new Node();
                    int j = (slice * 8 + i) & (KEEP - 1);
                    n.a = keep[j];
                    keep[j] = null;
                    sink[0] = n;
                    sink[0] = null;
                }
            }
            if (handshake) {
                awaitMarkEnd(beforeMark, 20000);
            }
            // Quiet phase: no Node allocation at all across the next cycle, so
            // nothing re-traces that size class and the dropped nodes' children
            // age past the sweep's free threshold. Filler drives the collection
            // trigger without touching the hazard.
            for (int i = 0; i < 120000; i++) {
                Filler f = new Filler();
                f.b = i;
                sink[i & 15] = f;
            }
            // The quiet cycle this phase describes, waited out rather than slept through: the
            // dropped nodes' children have to age past the sweep's threshold, and a sleep that
            // ended early left them still young.
            long beforeQuiet = marksDone();
            System.gc();
            if (handshake) {
                awaitMarkEnd(beforeQuiet, 20000);
            } else {
                Thread.sleep(150);
            }

            // HAZARD 2 -- legacy path, in a QUIET window. Outside a mark the SATB
            // barriers are disarmed, so nothing logs the reference as it moves
            // into an object above CN1_BIBOP_MAX_OBJECT that the collector has
            // never seen.
            refill(round);
            settle();
            transferToLegacy();
            settle();
            settle();

            // HAZARD 3 -- a retained map whose values are separately allocated
            // arrays, dropped wholesale: the reporter's dictionary shape. Entries
            // mature into the legacy heap while their payload arrays stay
            // page-resident, so the two halves of the collector have to agree
            // about when the pair dies.
            dict = new Hashtable();
            for (int i = 0; i < 4000; i++) {
                dict.put("k" + i, new byte[64]);
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

    /**
     * The collector's mark state: how many marks have finished, and whether one is running now.
     *
     * <p>Implemented in cn1_globals.m under CN1_GC_VERIFY, which is the only build this app is
     * ever compiled into. The count is in the high bits and the in-progress flag in bit 0, packed
     * into one value so a mark cannot begin and end between two separate reads.
     */
    static native long gcMarkState();

    /**
     * Whether the collector can be asked about its mark at all.
     *
     * <p>This app runs twice: once on a plain JVM, to establish what the program computes, and
     * once as a translated native binary, which is where the heap is actually verified. Only the
     * second has the collector this handshake talks to -- on the JVM the native does not resolve,
     * and the reference run has nothing to synchronize with anyway, since it is HotSpot's
     * collector deciding when to run. So the handshake is probed once and the timing-based
     * behaviour is kept for the run that cannot use it.
     */
    private static boolean handshake = true;


    /** Whether a mark is running right now. */
    private static boolean marking() {
        return (gcMarkState() & 1L) != 0L;
    }

    /** How many marks have finished. */
    private static long marksDone() {
        return handshake ? gcMarkState() >>> 1 : 0L;
    }

    /**
     * Waits for a mark to start, and answers whether one did.
     *
     * <p>Bounded, and the bound is generous rather than tuned: a run that never collects should
     * fail on the assertion that the hazard was not produced, not hang here. Returning false is
     * how the caller learns to say so.
     */
    private static boolean awaitMarkStart(long deadlineMillis) throws Exception {
        long limit = System.currentTimeMillis() + deadlineMillis;
        while (System.currentTimeMillis() < limit) {
            if (marking()) {
                return true;
            }
            Thread.sleep(1);
        }
        return false;
    }

    /** Waits until the mark that is running -- or the next one -- has finished. */
    private static void awaitMarkEnd(long from, long deadlineMillis) throws Exception {
        long limit = System.currentTimeMillis() + deadlineMillis;
        while (marksDone() <= from && System.currentTimeMillis() < limit) {
            Thread.sleep(1);
        }
    }

    private static void settle() throws Exception {
        scrub(300);
        // A whole cycle, waited for rather than slept through. The old 120ms was a guess at how
        // long a collection takes, and on a loaded machine it expired mid-mark -- so the "quiet
        // window" the hazards below rely on was not quiet, and the run proved nothing.
        long before = marksDone();
        System.gc();
        if (handshake) {
            awaitMarkEnd(before, 20000);
        } else {
            Thread.sleep(120);
        }
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
