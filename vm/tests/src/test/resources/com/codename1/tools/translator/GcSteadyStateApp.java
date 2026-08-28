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
 * The steady-state half of issue #5537: does the collector reach a working set and STAY
 * there, or does it compound?
 *
 * <p>Every other GC workload in this suite measures a PEAK under load, and a peak cannot
 * express the reported failure. {@code GcOverflowSpiralApp} runs 50 bounded rounds and its
 * test asserts peak &lt; 2GB; a heap that grows forever at a modest rate passes that. The
 * reporter's build climbed 500MB to 5GB over five minutes against a live set of a few
 * hundred objects, so the statistic that matters is whether the growth STOPS.</p>
 *
 * <p>The shape is the reporter's -- a deep, CPU-bound game-tree search on worker threads,
 * allocating millions of tiny short-lived reference-carrying objects -- with two
 * properties this gate depends on:</p>
 *
 * <ul>
 * <li><b>The live set is constant by construction.</b> {@code legacyLiveSet} is built once
 *     and held for the whole run; the search retains only one path through the tree. If the
 *     footprint compounds, it is the VM's doing and not the program's.</li>
 * <li><b>Several worker threads.</b> Marking is single-threaded and runs at lowered
 *     priority, so whether the collector keeps up is a function of how many cores the
 *     mutator holds. A single-threaded version of this workload does not reproduce.</li>
 * </ul>
 *
 * <p>Deterministic by construction -- a fixed round count and fixed seeds rather than a
 * wall-clock budget -- so {@code RESULT} can be compared against the same program on a
 * stock JVM, and so the gate's own measurement window is reproducible. It declares no
 * natives for the same reason: the reference run has to be able to execute it unchanged.
 * The VM-side numbers come from the {@code [GCPROBE]} series, which the test reads from
 * stderr.</p>
 */
public class GcSteadyStateApp {

    /** Board payload: 64 ints + header, a BiBOP size class, and a LEAF (no mark function). */
    private static final int BOARD_CELLS = 64;

    /** Search geometry. Depth is what sets native-stack extent, which is the input to the
     * conservative root scan; branch keeps one round to a few hundred thousand nodes. */
    private static final int DEPTH = 12;
    private static final int BRANCH = 3;

    /** Reference-carrying allocations per node. Only a non-leaf object reaches the mark
     * worklist and only a non-leaf object can be matured into the legacy heap, so this is
     * what makes the workload visible to the parts of the collector under test. */
    private static final int MOVES_PER_NODE = 4;

    /**
     * Worker threads. FIXED, not derived from the runner: the gate compares two halves of
     * one run against each other, so the shape has to be the same on every machine -- and
     * Runtime.availableProcessors() is not part of ParparVM's JavaAPI anyway, so a
     * translated build cannot ask. Four is enough to keep the single-threaded collector
     * behind on any runner with two cores or more; one worker does not reproduce.
     */
    private static final int THREADS = 4;

    /** Rounds per worker. Sized for a few hundred collection cycles: the gate compares the
     * second half of the run against the first, so it needs enough cycles in each. */
    private static final int ROUNDS = 24;

    /** A retained legacy population, held for the whole run, so the collector's table walks
     * cost something. Reference-carrying on purpose -- the rescan skips objects with no
     * mark function, so a population of primitive arrays would be free and prove nothing. */
    private static final int LEGACY_BLOCKS = 256;
    private static final int LEGACY_BLOCK_REFS = 128;

    static Object[][] legacyLiveSet;
    static final Object SUM_LOCK = new Object();
    static long checksum = 0;

    /** One node of the search: small, short-lived, and carrying references. */
    static final class Move {
        int from;
        int to;
        int score;
        int[] board;
        Move next;
    }

    public static void main(String[] args) {
        int threads = THREADS;
        System.out.println("CONFIG depth=" + DEPTH + " branch=" + BRANCH
                + " moves=" + MOVES_PER_NODE + " rounds=" + ROUNDS + " threads=" + threads);

        legacyLiveSet = new Object[LEGACY_BLOCKS][];
        for (int i = 0; i < LEGACY_BLOCKS; i++) {
            Object[] block = new Object[LEGACY_BLOCK_REFS];
            for (int j = 0; j < LEGACY_BLOCK_REFS; j++) {
                Move held = new Move();
                held.from = i;
                held.to = j;
                block[j] = held;
            }
            legacyLiveSet[i] = block;
        }
        System.out.println("BASELINE_FOOTPRINT_KB=" + footprintKb());

        long startMs = System.currentTimeMillis();
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            final int seed = t * 7919;
            workers[t] = new Thread(new Runnable() {
                public void run() {
                    long sum = 0;
                    int[] root = new int[BOARD_CELLS];
                    for (int r = 0; r < ROUNDS; r++) {
                        sum += search(root, DEPTH, seed + r);
                    }
                    // Order-independent, so RESULT does not depend on scheduling.
                    synchronized (SUM_LOCK) {
                        checksum += sum;
                    }
                }
            });
            workers[t].start();
        }
        for (int t = 0; t < threads; t++) {
            try {
                workers[t].join();
            } catch (InterruptedException e) {
            }
        }

        System.out.println("ELAPSED_MS=" + (System.currentTimeMillis() - startMs));
        System.out.println("FINAL_FOOTPRINT_KB=" + footprintKb());
        // Keeps the population reachable to the end and folds it into RESULT, so the
        // reference comparison covers it too.
        Move lastHeld = (Move) legacyLiveSet[LEGACY_BLOCKS - 1][LEGACY_BLOCK_REFS - 1];
        System.out.println("RESULT=" + (checksum + lastHeld.from + lastHeld.to));
        System.out.println("GC_STEADY_STATE_DONE");
    }

    /**
     * Recursive search. Every level copies the board and builds a short chain of moves, all
     * of it dead the moment the level returns -- the allocation shape of a game-tree search
     * with no transposition table.
     */
    private static int search(int[] board, int depth, int seed) {
        if (depth == 0) {
            int s = 0;
            for (int i = 0; i < BOARD_CELLS; i++) {
                s += board[i] * (i + 1);
            }
            return s & 0xff;
        }
        int best = -1;
        for (int b = 0; b < BRANCH; b++) {
            int[] child = new int[BOARD_CELLS];
            for (int i = 0; i < BOARD_CELLS; i++) {
                child[i] = board[i] + ((seed + b + i) & 7);
            }
            Move chain = null;
            for (int m = 0; m < MOVES_PER_NODE; m++) {
                Move mv = new Move();
                mv.from = b;
                mv.to = m;
                mv.score = seed + m;
                mv.board = child;
                mv.next = chain;
                chain = mv;
            }
            int v = search(child, depth - 1, seed + b + chain.to);
            if (v > best) {
                best = v;
            }
        }
        return best;
    }

    private static long footprintKb() {
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / 1024;
    }
}
