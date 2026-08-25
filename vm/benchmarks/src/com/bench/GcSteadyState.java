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
 * Issue #5537, the steady-state question: does the VM ever GIVE THE MEMORY BACK?
 *
 * <p>Every GC test in the repo measures a PEAK under load. GcOverflowSpiralIntegrationTest
 * asserts peak &lt; 2GB over 50 bounded rounds; a heap that grows forever at a modest rate
 * passes it. The reporter's build climbs 500MB to 5GB over five minutes on the iOS
 * Simulator against a live set of a few hundred objects, so the statistic that matters is
 * the SLOPE of the footprint at a fixed live set, and nothing here could express it.</p>
 *
 * <p>The shape is the reporter's: a deep, CPU-bound game-tree search producing millions of
 * tiny short-lived objects, on worker threads, with a live set that returns to the same
 * baseline every round. It is derived from {@code GcOverflowSpiralApp} and differs from it
 * in the five things that make a drift measurable:</p>
 *
 * <ul>
 * <li><b>Several workers</b>, because the collector is single-threaded
 *     ({@code gcMarkResolveThreadCount} returns 1) and at lowered priority, so whether it
 *     keeps up is a function of how many cores the mutator holds.</li>
 * <li><b>Wall-clock duration</b> instead of a fixed round count, because a drift that takes
 *     minutes cannot be sampled by a run that ends in seconds.</li>
 * <li><b>Depth as a knob, defaulting deep.</b> Depth sets the extent of the native stack,
 *     which is the input to the conservative root scan: every stale word in a live frame
 *     marks whatever it points at.</li>
 * <li><b>A wall-clock sampler thread.</b> Sampling every N nodes stops exactly when the
 *     collector stalls, which is the state being measured.</li>
 * <li><b>A sleep knob.</b> The reporter's own observation -- 1ms and 100ms do not help,
 *     1000ms does -- is a crude rate measurement, and sweeping it is the cheapest
 *     discriminator there is between a rate problem and a retention problem.</li>
 * </ul>
 *
 * <p>Knobs come from the environment through a native, because the clean target's generated
 * main() passes JAVA_NULL for args. Requires -DCN1_GC_CONFORM.</p>
 */
public class GcSteadyState {

    private static native int cfg(int which);
    private static native long probeMs();

    private static final int CFG_SECONDS = 0, CFG_THREADS = 1, CFG_DEPTH = 2, CFG_BRANCH = 3;
    private static final int CFG_SLEEP_MS = 4, CFG_MOVES = 5, CFG_LEGACY = 6, CFG_SCRUB = 7;

    private static final int BOARD_CELLS = 64;
    private static final int LEGACY_BLOCK_REFS = 128;

    static int seconds, threads, depth, branch, sleepMs, movesPerNode, legacyBlocks, scrubDepth;
    static volatile boolean stop = false;
    static Object[][] legacyLiveSet;
    static final Object SUM_LOCK = new Object();
    static long checksum = 0;
    /**
     * Per-worker node counts, one slot each, so counting costs no synchronisation and
     * loses no increments. A single shared counter cannot be used for this: an
     * unsynchronised read-modify-write from four workers drops updates at a rate that
     * depends on CONTENTION, and contention is precisely what differs between the builds
     * this benchmark compares -- a build whose threads park more would lose fewer
     * increments and so report a throughput advantage it does not have.
     *
     * Each worker republishes its slot once per round, so the SAMPLE series tracks
     * progress live. NODES= at the end is the authoritative figure: it is summed after
     * join(), which gives it a happens-before edge to every worker's last write. The
     * SAMPLE series reads the same slots while they are still being written, so it is a
     * progress indicator and not a measurement.
     */
    static long[] nodeCounts;

    /** One node of the search: small, short-lived, and REFERENCE-CARRYING. Only a non-leaf
     * object has a mark function, and only such an object is eligible for maturation into
     * the legacy heap -- a board of ints is a leaf and never graduates. */
    static final class Move {
        int from;
        int to;
        int score;
        int[] board;
        Move next;
    }

    public static void main(String[] args) {
        seconds = cfg(CFG_SECONDS);
        threads = cfg(CFG_THREADS);
        depth = cfg(CFG_DEPTH);
        branch = cfg(CFG_BRANCH);
        sleepMs = cfg(CFG_SLEEP_MS);
        movesPerNode = cfg(CFG_MOVES);
        legacyBlocks = cfg(CFG_LEGACY);
        scrubDepth = cfg(CFG_SCRUB);
        System.out.println("WLCONFIG seconds=" + seconds + " threads=" + threads
                + " depth=" + depth + " branch=" + branch + " sleepMs=" + sleepMs
                + " moves=" + movesPerNode + " legacy=" + legacyBlocks
                + " scrub=" + scrubDepth);

        // A retained legacy population, held for the whole run. Without it the collector's
        // table walk costs nothing and this workload cannot tell a cheap drain from an
        // O(heap) one. Reference-carrying, because the rescan skips objects with no mark
        // function.
        nodeCounts = new long[threads];
        legacyLiveSet = new Object[legacyBlocks][];
        for (int i = 0; i < legacyBlocks; i++) {
            Object[] block = new Object[LEGACY_BLOCK_REFS];
            for (int j = 0; j < LEGACY_BLOCK_REFS; j++) {
                Move held = new Move();
                held.from = i;
                held.to = j;
                block[j] = held;
            }
            legacyLiveSet[i] = block;
        }

        Thread sampler = new Thread(new Runnable() {
            public void run() {
                while (!stop) {
                    System.out.println("SAMPLE tMs=" + probeMs() + " fpKb=" + footprintKb()
                            + " nodes~=" + sumNodes());
                    sleep(250);
                }
            }
        });
        sampler.start();

        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            final int seed = t * 7919;
            final int slot = t;
            workers[t] = new Thread(new Runnable() {
                public void run() {
                    long sum = 0;
                    long[] counter = new long[1];
                    int[] root = new int[BOARD_CELLS];
                    int round = 0;
                    while (!stop) {
                        sum += search(root, depth, seed + round, counter);
                        // Publish once per round so the SAMPLE series is a live progress
                        // indicator rather than a row of zeroes. A worker that stalls
                        // stops publishing, and its slot going flat IS the signal.
                        nodeCounts[slot] = counter[0];
                        round++;
                        if (sleepMs > 0) {
                            sleep(sleepMs);
                        }
                    }
                    nodeCounts[slot] = counter[0];
                    // Order-independent, so the checksum does not depend on scheduling.
                    synchronized (SUM_LOCK) {
                        checksum += sum;
                    }
                }
            });
        }

        long startMs = System.currentTimeMillis();
        for (int t = 0; t < threads; t++) {
            workers[t].start();
        }
        while (System.currentTimeMillis() - startMs < seconds * 1000L) {
            sleep(200);
        }
        stop = true;
        for (int t = 0; t < threads; t++) {
            try {
                workers[t].join();
            } catch (InterruptedException e) {
            }
        }
        try {
            sampler.join();
        } catch (InterruptedException e) {
        }

        // Optional: overwrite the deep frames the search left behind. Conservative root
        // scanning reads every aligned word in [sp, stackBase), so a returned frame's
        // leftover words still pin whatever they point at. Scrubbing is therefore an
        // ablation of that retention that costs no rebuild -- which is why it is a knob
        // and NOT on during the measurement window.
        if (scrubDepth > 0) {
            scrub(scrubDepth);
        }

        System.out.println("NODES=" + sumNodes());
        System.out.println("ELAPSED_MS=" + (System.currentTimeMillis() - startMs));
        System.out.println("FINAL_FOOTPRINT_KB=" + footprintKb());
        Move lastHeld = (Move) legacyLiveSet[legacyBlocks - 1][LEGACY_BLOCK_REFS - 1];
        System.out.println("RESULT=" + (checksum + lastHeld.from + lastHeld.to));
        System.out.println("GC_STEADY_STATE_DONE");
    }

    private static int search(int[] board, int d, int seed, long[] c) {
        if (stop) {
            return 0;
        }
        // Thread-private: this array belongs to one worker for the whole run.
        c[0]++;
        if (d == 0) {
            int s = 0;
            for (int i = 0; i < BOARD_CELLS; i++) {
                s += board[i] * (i + 1);
            }
            return s & 0xff;
        }
        int best = -1;
        for (int b = 0; b < branch; b++) {
            int[] child = new int[BOARD_CELLS];
            for (int i = 0; i < BOARD_CELLS; i++) {
                child[i] = board[i] + ((seed + b + i) & 7);
            }
            Move chain = null;
            for (int m = 0; m < movesPerNode; m++) {
                Move mv = new Move();
                mv.from = b;
                mv.to = m;
                mv.score = seed + m;
                mv.board = child;
                mv.next = chain;
                chain = mv;
            }
            int v = search(child, d - 1, seed + b + chain.to, c);
            if (v > best) {
                best = v;
            }
        }
        return best;
    }

    /** Writes zeroes over the stack region the search used, so its leftover words stop
     * resolving to dead objects. Recursion, not an array: the words to overwrite are the
     * frames themselves. */
    private static int scrub(int d) {
        int[] pad = new int[16];
        for (int i = 0; i < pad.length; i++) {
            pad[i] = 0;
        }
        if (d <= 0) {
            return pad[0];
        }
        return pad[0] + scrub(d - 1);
    }

    private static long sumNodes() {
        long total = 0;
        for (int i = 0; i < nodeCounts.length; i++) {
            total += nodeCounts[i];
        }
        return total;
    }

    private static long footprintKb() {
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / 1024;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
}
