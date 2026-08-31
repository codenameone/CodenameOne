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

/**
 * The uncooperative-mutator half of issue #5537: what happens to everybody else while one
 * thread runs code that reaches no GC safepoint.
 *
 * <p>ParparVM's mark phase stops each lightweight thread cooperatively -- it raises
 * {@code threadBlockedByGC} and then spins on {@code threadActive} until the thread parks
 * itself -- and the translator emits no safepoint polls in generated code, neither on
 * method entry nor on loop back-edges. Every safepoint lives inside a runtime function:
 * the allocator handshakes, contended {@code monitorEnter}, {@code Thread.sleep}, and the
 * native-call bracket. A Java loop that allocates nothing and enters no contended monitor
 * therefore reaches no safepoint at all, and the collector's spin never ends -- taking
 * every other thread with it, because they park at their next allocation waiting for a
 * cycle that can never start. The reporter's debugger caught the collector ten minutes
 * into that spin while a game-tree search ran a compute-only evaluation loop.</p>
 *
 * <p>The shape here is that reduced to its two essential threads:</p>
 *
 * <ul>
 * <li><b>A spinner</b> that runs one uninterrupted long-arithmetic loop. No allocation, no
 *     monitor, no native call -- so on a VM without the escalation it is unstoppable for
 *     its whole duration.</li>
 * <li><b>An allocator</b> that churns small objects and times every chunk, reporting the
 *     LONGEST it was ever stalled. That number is the whole measurement: a mutator blocked
 *     behind a collector blocked behind the spinner.</li>
 * </ul>
 *
 * <p>Both figures are printed, and the gate compares them against each other rather than
 * against a wall-clock constant: {@code MAXSTALL} is a small fraction of {@code SPINMS}
 * when the collector can force the spinner to stop, and essentially all of it when it
 * cannot. Self-calibrating, so one pair of thresholds holds on a fast developer machine
 * and a slow CI runner alike.</p>
 *
 * <p>The spin length is calibrated at runtime for the same reason -- a fixed iteration
 * count is seconds on one machine and milliseconds on another, and a spin shorter than a
 * collection cycle cannot express the failure. Calibration runs BEFORE the allocator
 * starts, so it measures the machine rather than the contention.</p>
 *
 * <p>Note that the allocator cannot escape the wedge by finishing early: once the
 * collector is stuck on the spinner, the allocator blocks at its next page acquisition and
 * stays there until the spinner ends. Its chunk count therefore only has to be enough to
 * still be running when the spin begins.</p>
 *
 * <p>Declares no natives and uses no Codename One API, so a stock JVM runs it unchanged
 * and {@code RESULT} can be compared across the two. {@code RESULT} deliberately excludes
 * everything derived from the calibration, which is wall-clock dependent and so differs
 * between any two runs. On a stock JVM the spinner is stoppable by construction (HotSpot
 * polls at loop back-edges), which is the behaviour this gate asks ParparVM to
 * approximate.</p>
 */
public class GcUncooperativeThreadApp {

    /** How long the uninterruptible spin should last. Comfortably longer than a collection
     *  cycle on any runner, so a wedge is unmistakable next to a legitimate GC pause. */
    private static final long TARGET_SPIN_MS = 6000;

    /** Iterations used to measure the machine before sizing the real spin. Large enough to
     *  take milliseconds rather than to be swallowed by clock granularity. */
    private static final long CALIBRATE_ITERS = 20000000L;

    /** Floor on the calibrated spin, in case the calibration lands inside one clock tick. */
    private static final long MIN_SPIN_ITERS = 200000000L;

    /** Objects per timed chunk on the allocator. A chunk has to cross several BiBOP page
     *  acquisitions -- the page acquire is where the allocator's own safepoint lives, so a
     *  chunk that fitted inside one page would never park and would time nothing. */
    private static final int CHUNK_OBJECTS = 20000;

    /** Timed chunks. The measurement is a MAXIMUM, so this only has to keep the allocator
     *  running into the spin; see the class comment on why it cannot finish during one. */
    private static final int CHUNKS = 1500;

    /** Retained set, so the collector has something real to trace and cycles cost time. */
    private static final int LIVE_SET = 4000;

    /** A small reference-carrying object: reaches the mark worklist, unlike a leaf. */
    private static final class Node {
        Node next;
        long a;
        long b;

        Node(Node next, long a) {
            this.next = next;
            this.a = a;
            this.b = a * 31;
        }
    }

    /** Iterations for the spin, published before the spinner starts. */
    private static long spinIterations;

    /** Written by the spinner, read only after join. */
    private static volatile long spinResult;
    private static volatile long spinMillis;

    /** Held so the retained set cannot be optimized into nothing. */
    private static Node[] liveSet;

    /**
     * The unstoppable loop. Pure long arithmetic on locals: no allocation (so no allocator
     * handshake), no monitor, no native call, and on ParparVM no back-edge poll either.
     * Deliberately NOT split into chunks with a check between them -- a chunk boundary that
     * touches the runtime is a safepoint, and the point of this method is to have none.
     */
    private static long burn(long iterations) {
        long a = 1;
        for (long i = 0; i < iterations; i++) {
            a = a * 31 + i;
            a ^= (a >>> 7);
        }
        return a;
    }

    private static final class Spinner implements Runnable {
        public void run() {
            long t0 = System.currentTimeMillis();
            long r = burn(spinIterations);
            spinMillis = System.currentTimeMillis() - t0;
            spinResult = r;
        }
    }

    public static void main(String[] args) throws Exception {
        // Retained set first: a collection that finds nothing to do finishes instantly and
        // would not hold the allocator long enough for the comparison to mean anything.
        liveSet = new Node[LIVE_SET];
        for (int i = 0; i < LIVE_SET; i++) {
            liveSet[i] = new Node(i > 0 ? liveSet[i - 1] : null, i);
        }

        // Size the spin against THIS machine, before any contention exists.
        long calStart = System.currentTimeMillis();
        long calAcc = burn(CALIBRATE_ITERS);
        long calMs = System.currentTimeMillis() - calStart;
        long iters = calMs <= 0 ? MIN_SPIN_ITERS : (CALIBRATE_ITERS * TARGET_SPIN_MS) / calMs;
        if (iters < MIN_SPIN_ITERS) {
            iters = MIN_SPIN_ITERS;
        }
        spinIterations = iters;

        Thread spinner = new Thread(new Spinner());
        spinner.start();

        // The allocator. Every chunk is timed; the maximum is the report.
        long maxStallMs = 0;
        long checksum = 0;
        for (int chunk = 0; chunk < CHUNKS; chunk++) {
            long t0 = System.currentTimeMillis();
            Node head = null;
            for (int i = 0; i < CHUNK_OBJECTS; i++) {
                head = new Node(head, chunk * 31L + i);
            }
            checksum += head.b;
            // Keep the retained set churning so marking has real work every cycle.
            liveSet[chunk % LIVE_SET] = new Node(null, chunk);
            long dt = System.currentTimeMillis() - t0;
            if (dt > maxStallMs) {
                maxStallMs = dt;
            }
        }

        spinner.join();

        System.out.println("SPINMS=" + spinMillis);
        System.out.println("MAXSTALL=" + maxStallMs);
        System.out.println("SPINACC=" + spinResult);
        System.out.println("RESULT=" + (checksum ^ calAcc ^ liveSet[0].b));
        System.out.println("GC_UNCOOPERATIVE_DONE");
    }
}
