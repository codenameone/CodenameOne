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
 * The second shape of issue #5537: a deep game-tree search on a worker thread whose
 * allocation is almost entirely short-lived SMALL objects, with a live set of one path
 * through the tree.
 *
 * <p>Small is the whole point, and it is what makes this a different workload from
 * {@code ProcessBudgetPacingApp}, whose blocks are all above CN1_BIBOP_MAX_OBJECT and so
 * take the legacy calloc path. Everything here is a page-resident BiBOP object, which
 * puts it in front of the grace pass: the collector walks the page registry every cycle
 * and marks every object allocated since the last one, because a fresh object may
 * already be linked into the live graph. The number of those is set by the mutator's
 * ALLOCATION RATE, not by the live set -- and the pass used to push all of them onto a
 * fixed 65536-entry mark worklist before draining any of it.</p>
 *
 * <p>Once that overflows, the collector arms its belt: a full O(heap) rescan to recover
 * marked-but-unscanned objects. That makes the cycle several times longer, the mutator
 * produces proportionally more fresh objects before the next cycle, and the next cycle
 * overflows for certain. The collector never returns to the fast path. Measured on this
 * workload before the fix: footprint 90MB -> 6.2GB in 20 seconds, cycle time 6ms -> 750ms,
 * against a live set of a few hundred bytes. On device that is a kill by the iOS
 * per-process ceiling; with the process-budget pacing of #5563 holding the process under
 * that ceiling, it is instead a thread that parks on nearly every allocation, which is
 * the "GC pauses become effectively continuous" the reporter saw next.</p>
 *
 * <p>The shape is tuned so the overflow is not marginal. Each node allocates one board
 * copy plus a short chain of Move objects, so a 24MB collection interval covers roughly
 * 190K fresh objects that carry references -- three times the worklist -- and the
 * unfixed collector overflows on essentially every cycle rather than on the unlucky
 * ones. Move carries references because only a non-leaf object is pushed; a board of
 * ints is a leaf and never reaches the worklist.</p>
 *
 * <p>It also holds a retained legacy population for the whole run (see
 * {@code LEGACY_BLOCKS}), so the collector's table walks cost something. Without that,
 * this app cannot distinguish a periodic drain that is cheap from one that rescans the
 * whole heap every time -- and the second is what hung a real app.</p>
 *
 * <p>Deterministic by construction: a fixed number of rounds rather than a wall-clock
 * budget, so RESULT can be compared against the same program on a stock JVM.</p>
 */
public class GcOverflowSpiralApp {

    /** Board payload. 64 ints + header is ~272 bytes: a BiBOP size class, and a leaf. */
    private static final int BOARD_CELLS = 64;

    /** Branching factor and depth of the synthetic search. */
    private static final int BRANCH = 6;
    private static final int DEPTH = 5;

    /** Move objects allocated per node. See the class comment: this is what oversubscribes
     * the worklist, since only reference-carrying objects are pushed by the grace pass. */
    private static final int MOVES_PER_NODE = 4;

    /** Top-level searches. Sized for a few seconds and a few hundred collection cycles. */
    private static final int ROUNDS = 50;

    /** Distinct root moves explored per round. */
    private static final int ROOT_MOVES = 64;

    /** Footprint sample cadence, in nodes visited. Cheap next to the allocation it follows. */
    private static final int SAMPLE_EVERY_NODES = 100000;

    /**
     * A RETAINED legacy population, held for the whole run. Not decoration: every one of
     * these is above CN1_BIBOP_MAX_OBJECT, so it lives in allObjectsInHeap, and the
     * collector's full drain walks that table from index 0 on EVERY call, re-running the
     * mark function of everything already marked. A workload whose table holds only what
     * a translated hello-world allocates cannot tell a cheap periodic drain from an
     * O(heap) one -- which is precisely how a first cut at this fix passed here and then
     * hung the Mac Catalyst screenshot suite, whose table is a live UI.
     *
     * <p>REFERENCE-CARRYING on purpose. The rescan only re-pushes objects that have a
     * mark function, so a population of primitive arrays is skipped and costs nothing:
     * an earlier version of this app used byte[] and measured the two drains as equally
     * fast. Object[] has a mark function that walks every slot, which is what a real
     * app's retained graph looks like to the collector.</p>
     */
    private static final int LEGACY_BLOCKS = 256;

    /** References per retained block: 128 * 8 bytes + header is over the 512 threshold. */
    private static final int LEGACY_BLOCK_REFS = 128;

    /** Held in a static so nothing here is collectable. */
    static Object[][] legacyLiveSet;

    /** One node of the search: small, short-lived, and carrying references. */
    static final class Move {
        int from;
        int to;
        int score;
        int[] board;
        Move next;
    }

    static long nodes = 0;
    static long peakKb = 0;
    static long checksum = 0;
    static long sampleCountdown = SAMPLE_EVERY_NODES;

    public static void main(String[] args) {
        System.out.println("CONFIG branch=" + BRANCH + " depth=" + DEPTH
                + " movesPerNode=" + MOVES_PER_NODE + " rounds=" + ROUNDS);
        legacyLiveSet = new Object[LEGACY_BLOCKS][];
        for (int i = 0; i < LEGACY_BLOCKS; i++) {
            Object[] block = new Object[LEGACY_BLOCK_REFS];
            // Every slot filled, so the mark function has real references to follow
            // rather than nulls it can skip.
            for (int j = 0; j < LEGACY_BLOCK_REFS; j++) {
                Move held = new Move();
                held.from = i;
                held.to = j;
                block[j] = held;
            }
            legacyLiveSet[i] = block;
        }

        long baselineKb = footprintKb();
        peakKb = baselineKb;
        System.out.println("BASELINE_FOOTPRINT_KB=" + baselineKb);

        // On a WORKER thread, as the reporter's search is. The GC's grace pass and its
        // root scanning both treat a non-EDT thread differently enough that running this
        // on the main thread would not be the same test.
        Thread worker = new Thread(new Runnable() {
            public void run() {
                long sum = 0;
                int[] root = new int[BOARD_CELLS];
                for (int r = 0; r < ROUNDS; r++) {
                    for (int i = 0; i < ROOT_MOVES; i++) {
                        sum += search(root, DEPTH, i + r);
                    }
                }
                checksum = sum;
            }
        });
        long startMs = System.currentTimeMillis();
        worker.start();
        try {
            worker.join();
        } catch (InterruptedException e) {
        }
        long elapsedMs = System.currentTimeMillis() - startMs;

        System.out.println("PEAK_FOOTPRINT_KB=" + peakKb);
        System.out.println("BASELINE_KB=" + baselineKb);
        System.out.println("NODES=" + nodes);
        System.out.println("ELAPSED_MS=" + elapsedMs);
        // Keeps the population reachable to the end, and folds it into RESULT so the
        // JavaSE comparison covers it too.
        Move lastHeld = (Move) legacyLiveSet[LEGACY_BLOCKS - 1][LEGACY_BLOCK_REFS - 1];
        System.out.println("RESULT=" + (checksum + lastHeld.from + lastHeld.to));
        System.out.println("GC_OVERFLOW_SPIRAL_DONE");
    }

    /**
     * Recursive search. Every level copies the board and builds a small chain of moves,
     * all of it dead the moment the level returns -- the allocation shape of a game-tree
     * search with no transposition table.
     */
    private static int search(int[] board, int depth, int seed) {
        nodes++;
        if (--sampleCountdown <= 0) {
            sampleCountdown = SAMPLE_EVERY_NODES;
            long kb = footprintKb();
            if (kb > peakKb) {
                peakKb = kb;
            }
        }
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
                child[i] = board[i];
            }
            child[(seed + b * depth) & (BOARD_CELLS - 1)] = depth + b;
            Move head = null;
            for (int m = 0; m < MOVES_PER_NODE; m++) {
                Move mv = new Move();
                mv.from = seed;
                mv.to = b + m;
                mv.board = child;
                mv.next = head;
                head = mv;
            }
            int v = search(child, depth - 1, seed + b);
            head.score = v;
            if (v > best) {
                best = v;
            }
        }
        return best;
    }

    /**
     * This process's phys_footprint in KB. Runtime.totalMemory() reports physical RAM and
     * Runtime.freeMemory() reports physical RAM minus phys_footprint, so the difference is
     * the footprint -- the same figure the kernel charges an app against. On a stock JVM
     * the expression means something else entirely, which is why the JavaSE leg of the
     * harness compares only RESULT.
     */
    private static long footprintKb() {
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / 1024;
    }
}
