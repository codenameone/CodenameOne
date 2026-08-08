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
 * Synthetic reproduction for issue 5537: an iOS app killed by the kernel with
 * EXC_RESOURCE (RESOURCE_TYPE_MEMORY, high watermark) at about 1.4GB resident
 * near the end of a deep, garbage-heavy game-tree search, while the identical
 * build is fine in the Xcode simulator and on Android.
 *
 * WHAT THIS MEASURES. A collector-paced heap is supposed to bound resident
 * memory as a function of the LIVE set. This app holds the live set fixed and
 * tiny (LIVE_BYTES) and sweeps the mutator's allocation RATE, so the question
 * becomes falsifiable: does resident memory track the live set, or does it
 * track how fast the program allocates? If RSS grows with rate at a fixed live
 * set, the VM has a GC trigger but no effective throttle, and any device slow
 * enough for the mutator to outrun the collector walks into the jetsam limit.
 *
 * WHY TWO PATHS. codenameOneGcMalloc dispatches purely on total block size: at
 * or below CN1_BIBOP_MAX_OBJECT (512) an allocation -- INCLUDING a small array
 * -- is served from the BiBOP page heap, which paces the mutator against the
 * collector in bytes (cn1BibopMaybeGc / cn1BibopPacingCap). Above it the
 * allocation falls through to the legacy calloc + allObjectsInHeap path, whose
 * byte-denominated trigger (CN1_LEGACY_GC_TRIGGER_BYTES) only schedules an
 * ASYNCHRONOUS System.gc() and never parks the allocating thread; its only
 * backpressure is a COUNT of outstanding slots (CN1_MAX_HEAP_SIZE).
 *
 * The arms are therefore the SAME program -- same type, same loop, same
 * retained-ring shape, same live bytes, same wall duration. The ONLY difference
 * is the element size, which is what selects the path:
 *
 *   path bibop   byte[256]    block stays under 512  -> BiBOP page heap
 *   path legacy  byte[16384]  block exceeds 512      -> legacy calloc path
 *
 * 16384 is not arbitrary: the faulting frame in the reporter's debugger capture
 * is inside _platform_memmove's non-temporal copy loop, which is only reached
 * for copies of 0x4000 bytes or more.
 *
 * HOW IT IS MEASURED. Resident memory is sampled by the HARNESS, not here: the
 * clean target emits .c rather than .m, so java_lang_Runtime_freeMemoryImpl is
 * compiled without __OBJC__ and returns a stub constant. Each phase boundary is
 * stamped with System.currentTimeMillis() and the harness correlates those
 * stamps against its own sampler, which is immune to stdout buffering delaying
 * a marker line. Every allocated page is written to once, because a calloc'd
 * block that is never touched costs no resident memory and would understate the
 * effect that kills the app on device.
 *
 * RESULT= is fed only by the RATE-LIMITED arms, whose iteration counts are
 * fixed constants, so it stays comparable against the same program on the host
 * JVM: an arm cannot be made to look well-behaved by quietly allocating less.
 * The unbounded arms and the pacing spin are runner-dependent and are kept out
 * of it deliberately.
 */
public class LegacyArrayPacingApp {

    /**
     * Total block size (array header + data + trailing slot pointer) must stay
     * at or below CN1_BIBOP_MAX_OBJECT for this path to reach the page heap.
     * 256 bytes of payload leaves generous room for the header at every
     * pointer width.
     */
    private static final int SMALL_BYTES = 256;

    /** The issue-5537 size class: at or above memmove's 0x4000 bulk-copy threshold. */
    private static final int LARGE_BYTES = 16384;

    /**
     * Retained working set, held in a ring and identical for every arm. A
     * game-tree search retains its principal variation and a transposition
     * table -- small and roughly constant -- while the rest of the search is
     * garbage the moment it is popped. Resident memory should track THIS.
     */
    private static final int LIVE_BYTES = 4 * 1024 * 1024;

    /**
     * Allocation rates swept per path, in MB/s. 0 means unbounded: allocate as
     * fast as the machine permits for the same wall duration. The rate-limited
     * points bracket what a real search sustains; the unbounded point is what a
     * fast machine does when nothing throttles it.
     */
    private static final int[] RATES_MB_S = {0, 2048, 512, 128};

    /** Wall duration of every arm, so each rate gets identical collector opportunity. */
    private static final long ARM_DURATION_MS = 6000;

    /**
     * Hard byte ceiling for an UNBOUNDED arm, whichever comes first with the
     * duration above. Without it a fast desktop churns tens of GB in six seconds
     * and, since nothing here is ever returned to the OS, drives the process to
     * a resident size that can take the whole machine down -- measured 17GB on
     * an M-series host. Four gigabytes is still three orders of magnitude above
     * LIVE_BYTES, which is all the arm needs to demonstrate.
     */
    private static final long FULL_RATE_BYTE_CAP = 4L * 1024 * 1024 * 1024;

    /** Allocation is spread over this many chunks to hold the rate steady. */
    private static final int CHUNKS = 120;

    /** One write per 4096 bytes materializes every page of a calloc'd block. */
    private static final int PAGE_STRIDE = 4096;

    /** Footprint sample cadence inside an arm, in allocation batches. */
    private static final int SAMPLE_EVERY_BATCHES = 4;

    /** Settle attempts at a phase boundary: an asynchronous System.gc() plus a pause. */
    private static final int SETTLE_ROUNDS = 2;
    private static final long SETTLE_PAUSE_MS = 300;

    /** Checksum over the rate-limited arms only -- deterministic, host-JVM comparable. */
    private static long checksum;

    /** Sink for runner-dependent work, kept out of RESULT=. */
    private static long sink;

    public static void main(String[] args) {
        System.out.println("CONFIG smallBytes=" + SMALL_BYTES
                + " largeBytes=" + LARGE_BYTES
                + " liveBytes=" + LIVE_BYTES
                + " armDurationMs=" + ARM_DURATION_MS);

        for (int r = 0; r < RATES_MB_S.length; r++) {
            runArm("bibop", SMALL_BYTES, RATES_MB_S[r]);
            runArm("legacy", LARGE_BYTES, RATES_MB_S[r]);
        }

        if (sink == Long.MIN_VALUE) {
            System.out.println("(unreachable sink " + sink + ")");
        }
        System.out.println("RESULT=" + checksum);
        System.out.println("LEGACY_ARRAY_PACING_DONE");
    }

    /**
     * @param rateMbS target allocation rate in MB/s, or 0 for unbounded
     */
    private static void runArm(String path, int elementSize, int rateMbS) {
        String name = path + "@" + (rateMbS == 0 ? "full" : Integer.toString(rateMbS));
        int liveSlots = LIVE_BYTES / elementSize;
        if (liveSlots < 1) {
            liveSlots = 1;
        }

        byte[][] retained = new byte[liveSlots][];
        settle();
        long baseline = footprintKb();
        long peak = baseline;
        mark("BASELINE", name, baseline);

        long armChecksum = 0;
        long allocated = 0;
        long start = System.currentTimeMillis();
        mark("BEGIN", name, baseline);

        if (rateMbS == 0) {
            // Unbounded: allocate flat out until the arm's deadline.
            long deadline = start + ARM_DURATION_MS;
            long i = 0;
            while (allocated < FULL_RATE_BYTE_CAP && System.currentTimeMillis() < deadline) {
                // Check the clock once per batch; the call is not free and would
                // otherwise dominate the loop and cap the rate artificially.
                for (int b = 0; b < 256; b++, i++) {
                    armChecksum += fill(newBuffer(elementSize, i), i, retained,
                            (int) (i % liveSlots));
                }
                allocated += 256L * elementSize;
                long kb = footprintKb();
                if (kb > peak) {
                    peak = kb;
                }
            }
        } else {
            long totalBytes = (long) rateMbS * 1024 * 1024 * ARM_DURATION_MS / 1000L;
            long iterations = totalBytes / elementSize;
            long done = 0;
            for (int chunk = 0; chunk < CHUNKS; chunk++) {
                long target = (iterations * (chunk + 1)) / CHUNKS;
                for (long i = done; i < target; i++) {
                    armChecksum += fill(newBuffer(elementSize, i), i, retained,
                            (int) (i % liveSlots));
                }
                done = target;
                if ((chunk % SAMPLE_EVERY_BATCHES) == 0) {
                    long kb = footprintKb();
                    if (kb > peak) {
                        peak = kb;
                    }
                }
                sink += spinUntil(start + (ARM_DURATION_MS * (chunk + 1)) / CHUNKS, chunk);
            }
            allocated = done * elementSize;
        }

        long end = System.currentTimeMillis();
        {
            long kb = footprintKb();
            if (kb > peak) {
                peak = kb;
            }
        }
        mark("PEAK", name, peak);

        // Read the whole retained ring back so it is unambiguously reachable
        // across the loop, then measure again after giving the collector room.
        for (int i = 0; i < liveSlots; i++) {
            if (retained[i] != null) {
                armChecksum += retained[i][elementSize - 1];
            }
        }
        settle();
        mark("SETTLED", name, footprintKb());

        // Only now may the ring die.
        retained = null;
        settle();
        mark("RELEASED", name, footprintKb());

        if (rateMbS == 0) {
            // Runner-dependent iteration count: cannot feed RESULT=.
            sink += armChecksum;
        } else {
            checksum = checksum * 131 + armChecksum;
        }

        System.out.println("ARM_STATS name=" + name
                + " path=" + path
                + " elementBytes=" + elementSize
                + " targetMbS=" + rateMbS
                + " allocatedBytes=" + allocated
                + " liveSlots=" + liveSlots
                + " elapsedMs=" + (end - start));
    }

    private static void mark(String phase, String name, long footprintKb) {
        System.out.println("ARM_" + phase + " name=" + name
                + " tMs=" + System.currentTimeMillis()
                + " footprintKb=" + footprintKb);
    }

    /**
     * This process's phys_footprint in KB. Runtime.totalMemory() reports physical
     * RAM and Runtime.freeMemory() reports physical RAM minus phys_footprint, so
     * the difference is the footprint. Returns 0 on a platform whose Runtime
     * natives are still stubs, which the harness treats as "cannot measure here".
     */
    private static long footprintKb() {
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / 1024;
    }

    private static byte[] newBuffer(int elementSize, long i) {
        byte[] buffer = new byte[elementSize];
        // Touch every page so the block is genuinely resident, and make the
        // contents depend on the iteration so nothing can be elided.
        for (int off = 0; off < elementSize; off += PAGE_STRIDE) {
            buffer[off] = (byte) (i + off);
        }
        buffer[elementSize - 1] = (byte) (i >> 8);
        return buffer;
    }

    private static long fill(byte[] buffer, long i, byte[][] retained, int slot) {
        long c = buffer[0] + buffer[buffer.length - 1];
        byte[] evicted = retained[slot];
        if (evicted != null) {
            c += evicted[0];
        }
        retained[slot] = buffer;
        return c;
    }

    /**
     * Burn wall time without allocating, so a rate-limited arm's allocation rate
     * is set by the harness rather than by how fast the runner happens to be.
     */
    private static long spinUntil(long deadlineMs, int seed) {
        long v = seed;
        while (System.currentTimeMillis() < deadlineMs) {
            for (int i = 0; i < 4096; i++) {
                v = v * 6364136223846793005L + 1442695040888963407L;
            }
        }
        return v;
    }

    /**
     * System.gc() only sets forceGc and notifies the collector thread -- it does
     * not stop the world and does not wait -- so a settle point has to be an
     * explicit request followed by a pause long enough for a cycle to land.
     */
    private static void settle() {
        for (int i = 0; i < SETTLE_ROUNDS; i++) {
            System.gc();
            try {
                Thread.sleep(SETTLE_PAUSE_MS);
            } catch (InterruptedException e) {
                // A settle pause that is cut short only makes the measurement
                // more conservative; there is nothing to recover from.
            }
        }
    }
}
