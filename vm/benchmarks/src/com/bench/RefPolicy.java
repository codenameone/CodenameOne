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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 * The driver behind ParparVM's java.lang.ref support: one half asserts that
 * references behave, the other half is the workload the retention policy is
 * chosen from.
 *
 * <h2>Why both halves are here</h2>
 *
 * <p>They fail in opposite directions, and a driver that only had one of them
 * would pass while the VM was badly wrong. A reference that is never cleared is
 * the bug this feature replaced -- ParparVM traced the referent like any other
 * field, so every cache built on {@code Display.createSoftWeakRef} pinned its
 * contents for the life of the process. A reference cleared too eagerly is the
 * bug BEFORE that one -- the constructor dropped its argument, so {@code get()}
 * always answered null and the same caches could never hit. Phase A pins the
 * first end, phase B the second: a cache with a zero hit rate is a cache that is
 * being emptied faster than it is filled.</p>
 *
 * <h2>Reading the output</h2>
 *
 * <pre>
 * WEAK_LIVE_KEPT=n/n        phase A: referents still strongly held; must be all
 * WEAK_DEAD_CLEARED=n/m     phase A: unreachable referents the collector cleared
 * HITS / MISSES / HIT_RATE  phase B: what the cache actually bought
 * FINAL_FOOTPRINT_KB        phase B: what it cost
 * CHECKSUM                  policy invariant -- see below
 * </pre>
 *
 * <p>HIT_RATE and FINAL_FOOTPRINT_KB are the pair the policy is chosen on, and
 * NEITHER MEANS ANYTHING ALONE. A policy that never clears wins the hit rate by
 * keeping everything, and a policy that clears everything wins the footprint by
 * keeping nothing; the question ranking has to answer is whether it holds a
 * higher hit rate than the clear-everything arm at the same footprint.</p>
 *
 * <p>CHECKSUM is deliberately independent of the policy: a rebuilt payload is
 * byte-for-byte what it replaced, and the checksum accumulates what was READ
 * rather than what was rebuilt. So it is a parity check across arms (and against
 * a host JVM) that a policy change cannot legitimately move, while the hit rate
 * is free to move as much as it likes.</p>
 *
 * <h2>The trap this driver exists inside</h2>
 *
 * <p>ParparVM scans native C stacks conservatively, so a machine word left behind
 * by a returned frame keeps whatever it points at marked. A driver that drops
 * references and then sleeps pins them with its own dead frames and reports every
 * referent as retained -- the measurement comes back green having measured
 * nothing. {@link #scrub(int)} overwrites that region on purpose before any
 * assertion about collection; every phase-A step goes through
 * {@link #quiesce()}.</p>
 */
public class RefPolicy {

    // Phase B's shape, from argv: keys, payload bytes, accesses, churn per access.
    //
    // SIZE THE CACHE AGAINST THE BUDGET, not against convenience. The defaults are 2048
    // keys of 32KB, so a fully retained cache is 64MB -- most of the ceiling the A/B runs
    // under (CN1_SIMULATE_PROC_MEMORY_LIMIT). That is the only regime in which the
    // retention policy is the variable: an earlier 512x4KB cache was 2MB against a 96MB
    // ceiling, every policy retained all of it, and the run reported a 99.9% hit rate for
    // all three arms while measuring nothing but the collector's reaction to the ceiling.
    static int keys = 2048;
    /** Payload bytes. Well over CN1_BIBOP_MAX_OBJECT (512), so these take the legacy path. */
    static int payload = 32768;
    static int accesses = 400000;
    /** Objects allocated per access, to drive the collector rather than wait for it. */
    static int churnPerAccess = 24;
    /** Referents phase A drops. */
    static final int WEAK_SAMPLES = 256;

    static final int SCRUB_DEPTH = 400;
    static long scrubSink;

    /** Cache of SoftReference tokens; index is the key. */
    static Object[] cache;
    /** Somewhere for churn to land so the optimizer cannot delete it. */
    static Object[] churnSink = new Object[8];

    static long hits;
    static long misses;
    static long checksum;

    /** Aliases per referent in the alias phase. */
    static final int ALIASES = 4;

    public static void main(String[] args) throws Exception {
        if (args != null) {
            if (args.length > 0) { keys = Integer.parseInt(args[0]); }
            if (args.length > 1) { payload = Integer.parseInt(args[1]); }
            if (args.length > 2) { accesses = Integer.parseInt(args[2]); }
            if (args.length > 3) { churnPerAccess = Integer.parseInt(args[3]); }
        }
        cache = new Object[keys];
        System.out.println("CONFIG keys=" + keys + " payloadBytes=" + payload
                + " accesses=" + accesses + " churnPerAccess=" + churnPerAccess
                + " cacheBytes=" + ((long) keys * payload));
        weakPhase();
        aliasPhase();
        cachePhase();
        System.out.println("RESULT=" + checksum);
    }

    // ---------------------------------------------------------------- phase A

    /**
     * Both ends of the weak-reference contract, in one pass over one array.
     *
     * <p>Half the referents stay strongly reachable through {@code live} and must
     * still be answered; the other half are dropped and must eventually stop
     * being answered. Asserting only the second half would pass on a VM that
     * cleared every reference unconditionally.</p>
     *
     * <p>"Eventually" is two collections, not one, and that is not slop: the
     * sweep's grace rule keeps anything allocated since the last sweep, so a
     * referent is never cleared in the cycle it dies.</p>
     */
    private static void weakPhase() throws Exception {
        Object[] live = new Object[WEAK_SAMPLES];
        Object[] refsLive = new Object[WEAK_SAMPLES];
        Object[] refsDead = new Object[WEAK_SAMPLES];

        for (int i = 0; i < WEAK_SAMPLES; i++) {
            byte[] kept = build(i);
            live[i] = kept;
            refsLive[i] = new WeakReference(kept);

            byte[] doomed = build(i + WEAK_SAMPLES);
            refsDead[i] = new WeakReference(doomed);
            // The only strong path to `doomed` ends here. Nothing else in this frame
            // may keep it: no array slot, no local that outlives the iteration.
        }

        quiesce();
        quiesce();

        int liveKept = 0;
        for (int i = 0; i < WEAK_SAMPLES; i++) {
            if (((Reference) refsLive[i]).get() != null) {
                liveKept++;
            }
        }
        int deadCleared = 0;
        for (int i = 0; i < WEAK_SAMPLES; i++) {
            if (((Reference) refsDead[i]).get() == null) {
                deadCleared++;
            }
        }
        // `live` is read after the counting loop so it cannot be optimized away
        // before it, which would turn the retention half into a tautology.
        for (int i = 0; i < WEAK_SAMPLES; i++) {
            checksum += ((byte[]) live[i])[0];
        }

        System.out.println("WEAK_LIVE_KEPT=" + liveKept + "/" + WEAK_SAMPLES);
        System.out.println("WEAK_DEAD_CLEARED=" + deadCleared + "/" + WEAK_SAMPLES);
    }

    /** Set by the racing reader so the collector's clear pass sees fresh touch stamps. */
    static volatile boolean aliasRacing;
    static volatile Object aliasSink;
    static Object[] aliasRefs;

    /**
     * Several references over ONE referent must agree, INCLUDING while a mutator is
     * reading them.
     *
     * <p>The contract is that all references to a weakly reachable object are cleared
     * atomically -- not one at a time -- and a collector that clears them entry by entry
     * cannot honour it: clear the first alias, let a mutator's {@code get()} on the second
     * stamp it as recently read before the loop arrives there, and the second is kept. One
     * alias then answers null while another answers the object.</p>
     *
     * <p>For a cache that is a spurious miss. For the callers that use a reference as a
     * LIFETIME ORACLE -- reading a null {@code get()} as proof the referent died, and
     * releasing something on that basis -- it is a false death report on one alias while
     * the object is demonstrably alive through another.</p>
     *
     * <p><b>This is NOT a self-test for the split, and the distinction matters.</b> The
     * split needs a {@code get()} to land between the clear pass reaching one alias of a
     * group and reaching another, and that window is microseconds wide. Measured here:
     * with {@code -DCN1_REF_NO_ALIAS_ATOMICITY} restoring the single-loop form that has
     * the bug, three runs reported {@code ALIAS_SPLIT=0/256} -- the same as the fixed
     * build -- while both collected 255 of 256 groups. So the phase exercises the path
     * with real concurrent readers and asserts a real invariant, but it cannot be cited
     * as evidence that the invariant holds: it has never been seen to fail. Do not read
     * a green {@code ALIAS_SPLIT} as proof.</p>
     *
     * <p>Two earlier versions were worse and are worth not rebuilding. Reading the
     * aliases only after quiescing asserted something that could not fail at all, since
     * with no {@code get()} in flight every alias carries the same stamp. Hammering the
     * FIRST alias without pausing was hollow in the other direction: it kept every
     * referent marked, so nothing was ever condemned and {@code ALIAS_CLEARED_GROUPS} was
     * 0/256 -- a test in which the thing being tested never happens. Check that number
     * before trusting the one above it.</p>
     *
     * <p>The assertion is about AGREEMENT, not about collection: all cleared and all kept
     * both pass, split does not. Whether a group is collected at all depends on the
     * conservative root scan and on whether the reader happened to be holding it.</p>
     */
    private static void aliasPhase() throws Exception {
        final int groups = 256;
        aliasRefs = new Object[groups * ALIASES];
        for (int g = 0; g < groups; g++) {
            byte[] doomed = build(g + 4096);
            for (int a = 0; a < ALIASES; a++) {
                aliasRefs[g * ALIASES + a] = new WeakReference(doomed);
            }
            // `doomed` dies with this iteration; only the aliases above refer to it.
        }

        aliasRacing = true;
        Thread reader = new Thread(new Runnable() {
            public void run() {
                // The LAST alias of each group, and only in bursts.
                //
                // Last, because the split needs the read to land after the pass has
                // already cleared the group's earlier aliases -- a read that lands on the
                // first entry finds the group not yet condemned and changes nothing.
                //
                // In bursts, because a reader that never pauses keeps every referent
                // marked, so nothing is ever condemned and no split is possible. The pause
                // lets a group become collectable between bursts.
                while (aliasRacing) {
                    for (int g = 0; g < groups; g++) {
                        aliasSink = ((Reference) aliasRefs[g * ALIASES + ALIASES - 1]).get();
                    }
                    aliasSink = null;
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        });
        reader.start();

        for (int i = 0; i < 12; i++) {
            quiesce();
        }

        aliasRacing = false;
        reader.join();
        scrub(SCRUB_DEPTH);

        int split = 0;
        int clearedGroups = 0;
        for (int g = 0; g < groups; g++) {
            int cleared = 0;
            for (int a = 0; a < ALIASES; a++) {
                if (((Reference) aliasRefs[g * ALIASES + a]).get() == null) {
                    cleared++;
                }
            }
            if (cleared == ALIASES) {
                clearedGroups++;
            } else if (cleared != 0) {
                split++;
            }
        }
        System.out.println("ALIAS_SPLIT=" + split + "/" + groups);
        System.out.println("ALIAS_CLEARED_GROUPS=" + clearedGroups + "/" + groups);
    }

    // ---------------------------------------------------------------- phase B

    /**
     * A cache of rebuildable payloads under a skewed access distribution, which
     * is the shape every real caller of this feature has: a decoded bitmap behind
     * an EncodedImage, the int[] behind Image.getRGB, a rasterized gradient.
     *
     * <p>Skewed rather than uniform on purpose. Under a uniform distribution
     * there is no such thing as a cold entry, so every retention policy that
     * keeps the same NUMBER of entries scores the same and ranking cannot show a
     * difference even if it has one. The skew is what makes "which entries" a
     * question with an answer.</p>
     */
    private static void cachePhase() {
        long seed = 0x2545F4914F6CDD1DL;
        for (int i = 0; i < accesses; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int r = (int) ((seed >>> 33) % keys);
            // Squaring a uniform draw concentrates it near zero: a few hot keys, a
            // long cold tail.
            int key = (int) (((long) r * r) / keys);

            byte[] buf = null;
            Object token = cache[key];
            if (token != null) {
                buf = (byte[]) ((Reference) token).get();
            }
            if (buf == null) {
                misses++;
                buf = build(key);
                cache[key] = new SoftReference(buf);
            } else {
                hits++;
            }
            // Accumulate what was READ. A rebuild reproduces the same bytes, so this
            // is identical across policies while the hit rate is not.
            checksum += buf[0] + buf[payload - 1];

            for (int c = 0; c < churnPerAccess; c++) {
                churnSink[c & 7] = new byte[64];
            }
            churnSink[0] = null;
        }

        long total = hits + misses;
        System.out.println("HITS=" + hits);
        System.out.println("MISSES=" + misses);
        System.out.println("HIT_RATE_PPM=" + (total == 0 ? 0 : (hits * 1000000L) / total));
        System.out.println("FINAL_FOOTPRINT_KB=" + footprintKb());
    }

    // ---------------------------------------------------------------- helpers

    /** Deterministic content, so a rebuild is byte-for-byte what it replaced. */
    private static byte[] build(int key) {
        byte[] b = new byte[payload];
        b[0] = (byte) key;
        b[payload - 1] = (byte) (key * 31);
        return b;
    }

    /** One full collection with the driver's own stack overwritten first. */
    private static void quiesce() throws Exception {
        scrub(SCRUB_DEPTH);
        System.gc();
        Thread.sleep(250);
    }

    /**
     * Overwrites the native C stack the loop above ran on. See the class comment:
     * without this the conservative root scan keeps the dropped referents marked
     * and phase A reports zero collections while claiming success.
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

    /**
     * Physical footprint, read in process. On Apple platforms totalMemory() is
     * physical RAM and freeMemory() is RAM minus phys_footprint, which is the
     * number Apple's own limits are enforced against -- and the reason not to use
     * RSS, which counts shared clean pages and moves with whatever else the
     * machine is doing.
     */
    private static long footprintKb() {
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / 1024;
    }
}
