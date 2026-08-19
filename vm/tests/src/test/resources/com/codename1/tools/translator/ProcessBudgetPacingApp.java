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
 * Reproduces the allocation shape that killed a real app on device in issue #5537:
 * a CPU-bound worker churning short-lived buffers with a live set of almost nothing.
 *
 * <p>The workload is deliberately narrow. Every block is well above
 * CN1_BIBOP_MAX_OBJECT (512), so every one of them takes the LEGACY calloc path --
 * the path that had byte-based GC <em>scheduling</em> but no byte-based
 * <em>backpressure</em> at all. Its only blocking check was a COUNT of pending
 * allocations (CN1_MAX_HEAP_SIZE, derived from free RAM over a 128-byte average
 * object), so a thread allocating multi-kilobyte arrays could run hundreds of
 * megabytes ahead of the collector before anything stalled it. Every array a real
 * program allocates is above that threshold, which is why a game-tree search
 * copying board arrays could take a process past the iOS ceiling while holding
 * nothing.</p>
 *
 * <p>The live set is one block. Nothing here is retained, so a collector that keeps
 * up leaves the footprint flat: any peak above the trivial live set is the mutator
 * running ahead of it, which is exactly the quantity the pacing cap is supposed to
 * bound. That makes the reading a direct measurement of pacing rather than of
 * retention -- the opposite end of the problem from BibopPageFloorApp, which
 * measures what the collector fails to give back.</p>
 *
 * <p>Every page of every block is written. An untouched calloc'd block costs no
 * dirty memory, and dirty memory is the only kind the kernel meters an app against;
 * a workload that never writes what it allocates would report a flat footprint no
 * matter how far ahead it ran.</p>
 *
 * <p>Footprint is sampled by the allocating loop itself rather than by an external
 * sampler, because the peak is transient by construction: it exists only between
 * the moment the mutator has outrun the collector and the moment the next cycle
 * lands. An external sampler at any fixed cadence would miss it on a fast machine
 * and catch it on a slow one, which measures the runner. Reported as PHYS_FOOTPRINT
 * through Runtime (totalMemory is physical RAM, freeMemory is physical RAM minus
 * phys_footprint, so the difference is the footprint) -- the same figure the kernel
 * charges an app against, and the same one BibopPageFloorApp reads.</p>
 */
public class ProcessBudgetPacingApp {

    /**
     * One "board state". Far above CN1_BIBOP_MAX_OBJECT, so it always takes the
     * legacy calloc path -- the path under test. Large enough that the 24MB legacy
     * trigger is crossed every few dozen allocations, so the loop spends its life
     * on the far side of that threshold rather than tiptoeing up to it.
     */
    private static final int BLOCK_BYTES = 512 * 1024;

    /**
     * Total churn. Chosen so an unpaced run has room to run far ahead of the
     * collector while a bounded run cannot: enough volume for the gap to open, not
     * so much that the control run's peak becomes a hazard to the machine running
     * the test.
     */
    private static final int BLOCK_COUNT = 1536;

    /** One write per 4096 bytes materializes every page of a calloc'd block. */
    private static final int PAGE_STRIDE = 4096;

    /**
     * How often the loop reads its own footprint. A syscall per allocation would
     * pace the loop by accident and hide the very effect being measured; once per
     * this many blocks is frequent enough to catch a peak that takes dozens of
     * allocations to build.
     */
    private static final int SAMPLE_EVERY = 4;

    public static void main(String[] args) {
        System.out.println("CONFIG blockBytes=" + BLOCK_BYTES
                + " blockCount=" + BLOCK_COUNT
                + " churnBytes=" + ((long) BLOCK_BYTES * BLOCK_COUNT));

        long baselineKb = footprintKb();
        System.out.println("BASELINE_FOOTPRINT_KB=" + baselineKb);

        long checksum = 0;
        long peakKb = baselineKb;
        // Exactly one block stays reachable, so the live set never grows and every
        // byte of the peak is uncollected garbage rather than retained data.
        byte[] live = null;
        long startMs = System.currentTimeMillis();
        for (int i = 0; i < BLOCK_COUNT; i++) {
            byte[] block = new byte[BLOCK_BYTES];
            for (int off = 0; off < BLOCK_BYTES; off += PAGE_STRIDE) {
                block[off] = (byte) (i + off);
            }
            checksum += block[0] + block[BLOCK_BYTES - 1];
            live = block;
            if ((i % SAMPLE_EVERY) == 0) {
                long kb = footprintKb();
                if (kb > peakKb) {
                    peakKb = kb;
                }
            }
        }
        long elapsedMs = System.currentTimeMillis() - startMs;
        checksum += live[0];

        System.out.println("PEAK_FOOTPRINT_KB=" + peakKb);
        System.out.println("BASELINE_KB=" + baselineKb);
        System.out.println("ELAPSED_MS=" + elapsedMs);
        System.out.println("RESULT=" + checksum);
        System.out.println("PROCESS_BUDGET_PACING_DONE");
    }

    /**
     * This process's phys_footprint in KB. Runtime.totalMemory() reports physical
     * RAM and Runtime.freeMemory() reports physical RAM minus phys_footprint, so the
     * difference is the footprint itself. On a stock JVM the same expression is the
     * heap in use, which is why the JavaSE leg of the harness compares only RESULT.
     */
    private static long footprintKb() {
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / 1024;
    }
}
