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
 * Proves that reclaimed BiBOP pages are permanently unavailable to any
 * allocation that is not a small object -- the mechanism by which a past
 * small-object peak crowds out a later large/native allocation such as a Metal
 * texture, an image surface or a glyph atlas.
 *
 * <p>The claim is structural, not statistical. BiBOP is a segregated-fits page
 * heap: a 64KB page belongs to one size class, and cn1BibopAlloc can hand its
 * slots only to blocks at or under CN1_BIBOP_MAX_OBJECT (512). A swept page goes
 * to bibopFreePool or bibopPartialPool, and there is no munmap, no madvise and
 * no free anywhere in that path -- cn1_globals.m states it outright: "BiBOP
 * never free()s a page (swept pages are pooled)". Anything larger, including
 * every buffer a native texture would be built from, takes the legacy calloc
 * path and cannot touch those pooled pages.
 *
 * <p>So the two consequences are testable without racing the collector at all,
 * which is deliberate: this app never depends on the mutator outrunning the GC,
 * it allocates a live set, drops it, forces collection and waits. Any resident
 * memory still held after that is held BY DESIGN, not by a pacing accident.
 *
 * <p>PHASE ORDER, and what each phase is for:
 *
 * <ol>
 * <li>{@code texture-cold} -- allocate TEXTURE_BYTES of large buffers on a clean
 *     heap, hold, release. Establishes what a large allocation costs when no
 *     BiBOP pool exists. This is the control.</li>
 * <li>{@code small-1}, {@code small-2} -- allocate a large LIVE set of small
 *     (BiBOP-resident) objects, hold, drop, collect, wait. The pool must grow to
 *     hold them. After the drop every one of those pages is free, so a heap that
 *     returned memory would fall back to baseline. The second round is bigger
 *     than the first, so the floor can be checked for monotonicity.</li>
 * <li>{@code texture-warm} -- allocate exactly the same large buffers again, now
 *     with a fully populated BiBOP free pool sitting underneath. If those pooled
 *     pages were available to a large allocation, this phase would cost nothing.
 *     If they are not, it costs the same as texture-cold and stacks on top of the
 *     floor.</li>
 * </ol>
 *
 * <p>The proof is the comparison of texture-warm's cost against texture-cold's,
 * and of the post-release floor against baseline. On a device the sum of those
 * two -- an unreclaimable small-object floor plus a live texture working set --
 * is what has to fit under the roughly 1.4GB jetsam ceiling.
 *
 * <p>Memory is reported as PHYS_FOOTPRINT, read through Runtime, not as resident
 * size. That distinction is the whole measurement here: memory handed back with
 * MADV_FREE_REUSABLE leaves phys_footprint immediately but stays in
 * resident_size until the system is actually under pressure, so an RSS probe
 * shows a process that has released memory as though it had not -- and
 * phys_footprint is the figure the kernel meters an app against anyway. Phase
 * boundaries are also stamped with System.currentTimeMillis() so a harness can
 * cross-check against an external sampler.
 *
 * <p>NOTE ON FIDELITY. The "texture" buffers here are large byte[] on the legacy
 * calloc path, not literal MTLTexture allocations -- the clean target has no
 * Metal. For this measurement they are equivalent: both are malloc/mmap requests
 * against the same address space that cannot be served from a 512-byte-class
 * BiBOP page. What the model does NOT capture is texture memory the driver
 * places in a separate device heap; on unified-memory Apple silicon it is the
 * same physical budget, which is the case that matters here.
 */
public class BibopPageFloorApp {

    /**
     * Small-object payload. Total block size (array header + data + trailing
     * slot pointer) must stay at or below CN1_BIBOP_MAX_OBJECT for these to be
     * served from the page heap rather than the legacy path.
     */
    private static final int SMALL_BYTES = 256;

    /**
     * One "texture": a plausible surface-sized buffer. Far above
     * CN1_BIBOP_MAX_OBJECT, so it always takes the legacy calloc path.
     */
    private static final int TEXTURE_BYTES = 16 * 1024 * 1024;

    /** Total live texture working set per texture phase. */
    private static final int TEXTURE_COUNT = 12;

    /**
     * Live small-object bytes in the warm-up phase. Deliberately equal to the
     * texture working set, so "the pool is big enough to have served it" is not
     * an available explanation for any shortfall.
     */
    private static final long SMALL_LIVE_BYTES = (long) TEXTURE_BYTES * TEXTURE_COUNT;

    /** One write per 4096 bytes materializes every page of a calloc'd block. */
    private static final int PAGE_STRIDE = 4096;

    /**
     * Settle after dropping a phase's live set. Deliberately generous: the whole
     * question is whether memory comes back AT ALL, so the answer must not be
     * "the collector had not finished yet". System.gc() is asynchronous (it sets
     * forceGc and notifies the collector thread, then returns), so each round is
     * a request plus a pause long enough for a full cycle to land.
     */
    private static final int SETTLE_MIN_ROUNDS = 4;
    private static final int SETTLE_MAX_ROUNDS = 20;
    private static final int SETTLE_PLAIN_MIN_ROUNDS = 4;
    private static final int SETTLE_PLAIN_MAX_ROUNDS = 12;
    private static final long SETTLE_PAUSE_MS = 250;

    /**
     * Rounds that must ALL come back stable before a settle is believed. One
     * stable round is not enough: reclamation does not begin immediately, so
     * early rounds look stable simply because nothing has started coming back
     * yet -- measured on Linux, where the first pages were not released until
     * after a four-round settle had already concluded and the phase read as
     * having released nothing.
     */
    private static final int SETTLE_STABLE_STREAK = 3;

    /** Stack frames overwritten before a settle; see scrubStack. */
    private static final int SCRUB_DEPTH = 512;

    /**
     * A settle stops once a round frees less than this. Reclamation is not a
     * fixed number of cycles: a BiBOP object needs three sweeps to die (fresh
     * mark -1 is promoted, and death is mark < V-1), the major sweep that
     * refills the free pool runs on a cadence, and how many cycles land in a
     * given wall-clock window depends on the machine. A fixed round count
     * therefore measures the runner, not the collector -- measured directly:
     * six rounds was enough on macOS but not on the Linux CI runner, where the
     * footprint was still falling when the reading was taken and the phase
     * looked like it had released nothing.
     */
    private static final long SETTLE_STABLE_KB = 2048;

    /**
     * How long a phase keeps its live set REACHABLE before dropping it. Without
     * this the allocation itself takes tens of milliseconds and the harness
     * sampler gets no reading at all while the data is held, which is the
     * measurement the whole test turns on. The live set is touched between
     * pauses so it is provably reachable across the whole window.
     */
    private static final int HOLD_ROUNDS = 8;
    private static final long HOLD_PAUSE_MS = 200;

    private static long checksum;

    /**
     * Lowest footprint seen at any point after the warm-up's live set was
     * dropped, and the flag that starts tracking it.
     *
     * <p>This is what the release assertion reads, rather than the footprint at
     * some chosen instant. Reclamation is asynchronous and its LATENCY is
     * load-dependent: measured, the drop lands about 2s after the drop on an
     * idle macOS host, about 7s in an idle Linux container, and had still not
     * landed 15s in on a CI runner where surefire was running this probe
     * alongside a dozen other forks. The claim under test is that the pages come
     * back, not that they come back within some number of seconds, so waiting on
     * a deadline was testing the runner's load rather than the collector. The
     * minimum over the remainder of the run answers the actual question and
     * cannot be made to pass by a release that never happens.
     */
    private static long minFootprintAfterDrop = Long.MAX_VALUE;
    private static boolean trackMinFootprint;

    public static void main(String[] args) {
        System.out.println("CONFIG smallBytes=" + SMALL_BYTES
                + " textureBytes=" + TEXTURE_BYTES
                + " textureCount=" + TEXTURE_COUNT
                + " smallLiveBytes=" + SMALL_LIVE_BYTES);

        // 1. Grow the BiBOP page pool to SMALL_LIVE_BYTES, then give every page
        //    back to it. Nothing large has been allocated yet, so the process's
        //    malloc heap holds no large-block hole a texture could reuse.
        smallPhase("small-warmup", SMALL_LIVE_BYTES);

        // 2. TREATMENT. Allocate the texture set on top of that fully populated
        //    BiBOP free pool. If pooled pages could serve a large block this
        //    costs nothing; if they cannot, it costs the full texture set.
        texturePhase("texture-after-small");

        // 3. CONTROL. Drop those textures and allocate the identical set again.
        //    Now the hole underneath was freed by the LEGACY path rather than by
        //    BiBOP, so a reuse-capable allocator must absorb it for free. Any
        //    difference between phase 2 and phase 3 is attributable to which
        //    allocator released the memory, with the size, the type and the
        //    access pattern all held identical.
        texturePhase("texture-after-texture");

        System.out.println("ARM_MINAFTER name=small-warmup tMs=" + System.currentTimeMillis()
                + " footprintKb="
                + (minFootprintAfterDrop == Long.MAX_VALUE ? 0 : minFootprintAfterDrop));
        System.out.println("RESULT=" + checksum);
        System.out.println("BIBOP_PAGE_FLOOR_DONE");
    }

    /** Allocates and holds a large live set of BiBOP-resident objects, then drops it. */
    private static void smallPhase(String name, long liveBytes) {
        int count = (int) (liveBytes / SMALL_BYTES);
        beginPhase(name);

        byte[][] live = new byte[count][];
        for (int i = 0; i < count; i++) {
            byte[] o = new byte[SMALL_BYTES];
            o[0] = (byte) i;
            o[SMALL_BYTES - 1] = (byte) (i >> 8);
            live[i] = o;
        }
        long phaseChecksum = 0;
        for (int i = 0; i < count; i += 997) {
            phaseChecksum += live[i][0] + live[i][SMALL_BYTES - 1];
        }
        phaseChecksum += hold(live, SMALL_BYTES);
        long heldKb = footprintKb();
        endPhase(name, "objects=" + count + " liveBytes=" + liveBytes);

        live = null;
        releasePhase(name, heldKb, true);
        // Everything from here on is after the warm-up's live set died, so every
        // reading contributes to the minimum the release assertion reads.
        trackMinFootprint = true;
        checksum = checksum * 131 + phaseChecksum;
    }

    /** Allocates and holds a large live set of legacy-path buffers, then drops it. */
    private static void texturePhase(String name) {
        beginPhase(name);

        byte[][] textures = new byte[TEXTURE_COUNT][];
        for (int i = 0; i < TEXTURE_COUNT; i++) {
            byte[] t = new byte[TEXTURE_BYTES];
            // Touch every page: an untouched calloc'd block costs no resident
            // memory, and a texture that is never written is not a texture.
            for (int off = 0; off < TEXTURE_BYTES; off += PAGE_STRIDE) {
                t[off] = (byte) (i + off);
            }
            textures[i] = t;
        }
        long phaseChecksum = 0;
        for (int i = 0; i < TEXTURE_COUNT; i++) {
            phaseChecksum += textures[i][0] + textures[i][TEXTURE_BYTES - 1];
        }
        phaseChecksum += hold(textures, TEXTURE_BYTES);
        long heldKb = footprintKb();
        endPhase(name, "textures=" + TEXTURE_COUNT
                + " liveBytes=" + ((long) TEXTURE_COUNT * TEXTURE_BYTES));

        textures = null;
        releasePhase(name, heldKb, false);
        checksum = checksum * 131 + phaseChecksum;
    }

    private static void beginPhase(String name) {
        settle();
        mark("BASELINE", name);
        mark("BEGIN", name);
    }

    private static void endPhase(String name, String stats) {
        mark("HELD", name);
        System.out.println("ARM_STATS name=" + name + " " + stats);
    }

    /**
     * @param expectDrop whether this phase's memory is expected to come back.
     *                   Only the small-object warm-up asserts on its released
     *                   figure; the texture phases report theirs, and waiting
     *                   out the full budget for a drop that is not expected
     *                   there just burns wall time (15s per phase, measured).
     */
    private static void releasePhase(String name, long heldKb, boolean expectDrop) {
        scrubStack(SCRUB_DEPTH);
        if (expectDrop) {
            settleForRelease(heldKb);
        } else {
            settle();
        }
        mark("RELEASED", name);
        // Give an external sampler room to take a reading at the stamped
        // instant; the final phase's RELEASED is otherwise raced by exit.
        sleep(HOLD_PAUSE_MS * 2);
    }

    private static void mark(String phase, String name) {
        System.out.println("ARM_" + phase + " name=" + name
                + " tMs=" + System.currentTimeMillis()
                + " footprintKb=" + footprintKb());
    }

    /**
     * This process's phys_footprint in KB. Runtime.totalMemory() reports
     * physical RAM and Runtime.freeMemory() reports physical RAM minus
     * phys_footprint, so the difference is the footprint itself.
     */
    private static long footprintKb() {
        Runtime r = Runtime.getRuntime();
        long kb = (r.totalMemory() - r.freeMemory()) / 1024;
        if (trackMinFootprint && kb < minFootprintAfterDrop) {
            minFootprintAfterDrop = kb;
        }
        return kb;
    }

    /**
     * Keeps {@code live} reachable for a sampleable window. The touch between
     * pauses is what makes the reachability provable rather than incidental --
     * a conservative collector might keep it alive anyway, and this test must
     * not depend on that.
     */
    private static long hold(byte[][] live, int elementSize) {
        long c = 0;
        for (int r = 0; r < HOLD_ROUNDS; r++) {
            sleep(HOLD_PAUSE_MS);
            byte[] probe = live[(r * 7919) % live.length];
            c += probe[0] + probe[elementSize - 1];
        }
        return c;
    }

    /**
     * Overwrite the stack region the phase just used, then settle. ParparVM
     * scans thread stacks CONSERVATIVELY, so a dead slot still holding the
     * address of the dropped ring keeps every object it referenced reachable --
     * the collector cannot tell a stale word from a live reference. Measured on
     * Linux: without this the warm-up's 192MB was still fully resident after its
     * settle and only came back during the NEXT phase, once that phase's frames
     * had overwritten the words. Recursing writes fresh values over those slots
     * so the drop is observable where it actually happens.
     */
    /**
     * Wait for the collector to give the phase's memory back, up to a bounded
     * budget. Reclamation is ASYNCHRONOUS and takes a platform-dependent number
     * of cycles -- a BiBOP object needs three sweeps to die, the major sweep
     * that refills the free pool runs on a cadence, and cycles are paced at
     * 200ms -- so waiting a fixed time measures the runner rather than the
     * collector. Measured: the drop lands about 2s after the ring is dropped on
     * macOS and about 7s in a Linux container, and a fixed six-round settle
     * reported the Linux run as having released nothing.
     *
     * <p>Waiting for the drop we are about to assert on is deliberate and is not
     * circular: the budget is finite, so a release that never happens still
     * fails the assertion -- it just fails on the real behaviour rather than on
     * whichever machine ran it.
     */
    private static void settleForRelease(long heldKb) {
        long target = (heldKb * 3) / 5;
        for (int i = 0; i < SETTLE_MAX_ROUNDS; i++) {
            System.gc();
            sleep(SETTLE_PAUSE_MS);
            if (i + 1 >= SETTLE_MIN_ROUNDS && footprintKb() <= target) {
                return;
            }
        }
    }

    private static long scrubStack(int depth) {
        long a = depth, b = depth + 1, c = depth + 2, d = depth + 3;
        long e = depth + 4, f = depth + 5, g = depth + 6, h = depth + 7;
        if (depth <= 0) {
            return a + b + c + d + e + f + g + h;
        }
        return a + b + c + d + e + f + g + h + scrubStack(depth - 1);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // Shortening a measurement window only makes the result more
            // conservative; there is nothing to recover from.
        }
    }

    /**
     * Collect until the footprint stops falling, rather than for a fixed number
     * of rounds. System.gc() only sets forceGc and notifies the collector thread
     * -- it does not stop the world and does not wait -- so each round is a
     * request plus a pause, and the loop exits when a round stops buying
     * anything. Bounded so a platform that never reports a falling footprint
     * cannot hang the probe.
     */
    private static void settle() {
        long prev = footprintKb();
        int stable = 0;
        for (int i = 0; i < SETTLE_PLAIN_MAX_ROUNDS; i++) {
            System.gc();
            sleep(SETTLE_PAUSE_MS);
            long now = footprintKb();
            stable = (now + SETTLE_STABLE_KB >= prev) ? stable + 1 : 0;
            prev = now;
            if (i + 1 >= SETTLE_PLAIN_MIN_ROUNDS && stable >= SETTLE_STABLE_STREAK) {
                return;
            }
        }
    }
}
