/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Map-shaped workloads that {@code CommonWorkloads.hashMapChurn} does not reach.
 *
 * <p>These live OUTSIDE {@code CommonWorkloads} on purpose: that class is the
 * ten-benchmark set every generated port application runs, and
 * {@code scripts/hellocodenameone/conformance/port_status.py} enforces exactly
 * ten unique ids. This class is driven only by {@code vm/benchmarks}.
 *
 * <p>Two rules govern every workload here:
 * <ul>
 * <li><b>Checksums must not depend on iteration order or on hash VALUES.</b>
 * ParparVM's open-addressed map and HotSpot's chained map enumerate in
 * different orders, and identity hash codes differ by construction, so a
 * checksum is only ever a sum or a count over lookups the workload chose.
 * <li><b>Keys are pre-boxed into arrays.</b> {@code Integer.valueOf} allocates
 * above 127 on HotSpot and never allocates on ParparVM (tagged immediates), so
 * boxing inside the timed loop would measure the allocator rather than the
 * probe. Every shape below hoists that out.
 * </ul>
 */
public final class MapBench {

    /** xorshift32; identical on both VMs, and cheap enough not to dominate. */
    private static int rnd(int x) {
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        return x;
    }

    // ---- 1. miss-heavy containsKey ----------------------------------------
    // The case linear probing is worst at: an unsuccessful probe walks to the
    // terminating empty slot, ~8.5 slots at a 0.75 load factor against ~2.5 for
    // a hit. If SIMD group probing is worth anything, it is worth it here.
    public static long missHeavy() {
        int present = 100000;
        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
        Integer[] keys = new Integer[present * 10];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = Integer.valueOf(i);
        }
        for (int i = 0; i < present; i++) {
            map.put(keys[i], keys[i]);
        }
        long hits = 0;
        int r = 12345;
        for (int i = 0; i < 3000000; i++) {
            r = rnd(r);
            if (map.containsKey(keys[(r >>> 1) % keys.length])) {
                hits++;
            }
        }
        return hits;
    }

    // ---- 2. String-keyed lookup (the UIManager theme shape) ---------------
    // A small, long-lived, string-keyed map read over and over. String caches
    // its hash in a field, so this isolates probe + equals, not hashing --
    // except that cn1HmMarker still reaches String.hashCode through an
    // indirect virtual_ call, which is what makes this the lever-D shape.
    public static long stringKeys() {
        int distinct = 2000;
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        String[] keys = new String[distinct];
        for (int i = 0; i < distinct; i++) {
            keys[i] = "Component.style.uiid." + i;
            map.put(keys[i], Integer.valueOf(i));
        }
        long checksum = 0;
        int r = 987654321;
        for (int i = 0; i < 3000000; i++) {
            r = rnd(r);
            Integer v = map.get(keys[(r >>> 1) % distinct]);
            if (v != null) {
                checksum += v.intValue();
            }
        }
        return checksum;
    }

    // ---- 3. large table, random hits -------------------------------------
    // 1M entries: keys 8MB + values 8MB + meta 4MB, well past any L2. Every
    // probe is a cache miss, which is where a 1-byte control word (4x less
    // metadata traffic) would pay for itself if it ever does.
    public static long largeTable() {
        int n = 1000000;
        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>(n * 2);
        Integer[] keys = new Integer[n];
        for (int i = 0; i < n; i++) {
            keys[i] = Integer.valueOf(i);
            map.put(keys[i], keys[i]);
        }
        long checksum = 0;
        int r = 24680;
        for (int i = 0; i < 2000000; i++) {
            r = rnd(r);
            Integer v = map.get(keys[(r >>> 1) % n]);
            if (v != null) {
                checksum += v.intValue();
            }
        }
        return checksum;
    }

    // ---- 4. tombstone-heavy remove/insert --------------------------------
    // Keeps live count flat while churning slots, so the table fills with
    // tombstones between rebuilds. Linear probing degrades on tombstones (they
    // do not terminate a probe); a swiss table can convert a tombstone back to
    // empty when its group has a free slot, which this shape would show.
    public static long tombstones() {
        int live = 50000;
        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
        Integer[] keys = new Integer[live * 4];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = Integer.valueOf(i);
        }
        for (int i = 0; i < live; i++) {
            map.put(keys[i], keys[i]);
        }
        long checksum = 0;
        for (int i = 0; i < 1500000; i++) {
            Integer add = keys[(i + live) % keys.length];
            Integer drop = keys[i % keys.length];
            map.put(add, add);
            Integer removed = map.remove(drop);
            if (removed != null) {
                checksum += removed.intValue();
            }
        }
        return checksum + map.size();
    }

    // ---- 5. grow-dominated build ------------------------------------------
    // Builds from an empty default-capacity map every round, so the run is
    // dominated by cn1Grow. This is the shape that PRICES a swiss layout: the
    // current int[] meta rehashes by reusing the stored marker and makes zero
    // hashCode() calls on resize, while a 7-bit control byte forces one virtual
    // hashCode() per live key per grow.
    public static long growBuild() {
        int perRound = 20000;
        Integer[] keys = new Integer[perRound];
        for (int i = 0; i < perRound; i++) {
            keys[i] = Integer.valueOf(i);
        }
        long checksum = 0;
        for (int round = 0; round < 120; round++) {
            HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
            for (int i = 0; i < perRound; i++) {
                map.put(keys[i], keys[i]);
            }
            checksum += map.size();
        }
        return checksum;
    }

    // ---- 6. identity-keyed lookup ----------------------------------------
    // Object.hashCode is a truncated raw pointer with no header cache, and
    // BiBOP slots are size-class aligned, so the low bits are structurally
    // non-random. Any h2 derivation that narrows the hash has to survive THIS
    // shape, not just Integer and String keys.
    //
    // The checksum is a HIT COUNT, never a sum of hashes -- identity hash codes
    // legitimately differ between the two VMs.
    public static long identityKeys() {
        int n = 50000;
        Object[] keys = new Object[n];
        HashMap<Object, Integer> map = new HashMap<Object, Integer>();
        for (int i = 0; i < n; i++) {
            keys[i] = new Object();
            map.put(keys[i], Integer.valueOf(i));
        }
        long hits = 0;
        int r = 1357911;
        for (int i = 0; i < 2000000; i++) {
            r = rnd(r);
            if (map.get(keys[(r >>> 1) % n]) != null) {
                hits++;
            }
        }
        return hits;
    }

    // ---- 7. LinkedHashMap lookup (lever E) --------------------------------
    // Same shape as stringKeys, but LinkedHashMap overrides get/put/remove in
    // JAVA -- the native HashMap functions are deliberately base-class-only.
    // The ratio between this and stringKeys is the cost of that gap.
    public static long linkedStringKeys() {
        int distinct = 2000;
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        String[] keys = new String[distinct];
        for (int i = 0; i < distinct; i++) {
            keys[i] = "Component.style.uiid." + i;
            map.put(keys[i], Integer.valueOf(i));
        }
        long checksum = 0;
        int r = 987654321;
        for (int i = 0; i < 3000000; i++) {
            r = rnd(r);
            Integer v = map.get(keys[(r >>> 1) % distinct]);
            if (v != null) {
                checksum += v.intValue();
            }
        }
        return checksum;
    }

    // ---- 8. Hashtable lookup (lever C) ------------------------------------
    // Same shape again. Hashtable never got the compact layout: one Entry
    // object per mapping, chain walking, synchronized accessors, and a bucket
    // index computed with % (integer division) rather than a mask.
    public static long hashtableStringKeys() {
        int distinct = 2000;
        Hashtable<String, Integer> map = new Hashtable<String, Integer>();
        String[] keys = new String[distinct];
        for (int i = 0; i < distinct; i++) {
            keys[i] = "Component.style.uiid." + i;
            map.put(keys[i], Integer.valueOf(i));
        }
        long checksum = 0;
        int r = 987654321;
        for (int i = 0; i < 3000000; i++) {
            r = rnd(r);
            Integer v = map.get(keys[(r >>> 1) % distinct]);
            if (v != null) {
                checksum += v.intValue();
            }
        }
        return checksum;
    }

    // ---- 9. Hashtable build (lever C, allocation half) --------------------
    // One Entry allocation per mapping is the other half of Hashtable's cost;
    // this shape is what JSONParser's map-per-object-node actually looks like.
    public static long hashtableBuild() {
        int perRound = 20000;
        Integer[] keys = new Integer[perRound];
        for (int i = 0; i < perRound; i++) {
            keys[i] = Integer.valueOf(i);
        }
        long checksum = 0;
        for (int round = 0; round < 120; round++) {
            Map<Integer, Integer> map = new Hashtable<Integer, Integer>();
            for (int i = 0; i < perRound; i++) {
                map.put(keys[i], keys[i]);
            }
            checksum += map.size();
        }
        return checksum;
    }

    // ---- 10. IdentityHashMap ---------------------------------------------
    // Its own open-addressed table, and its own hazard: getModuloHash feeds
    // System.identityHashCode straight into a modulo with no scramble, and an
    // identity hash here is a truncated pointer whose low bits carry the
    // allocator's slot alignment rather than entropy. Probing is linear
    // (index + 2), so any home-slot bias turns into a cluster walk.
    //
    // The checksum is a HIT COUNT: identity hashes, and therefore this map's
    // iteration order, legitimately differ between the two VMs.
    public static long identityMapLookup() {
        int n = 50000;
        Object[] keys = new Object[n];
        java.util.IdentityHashMap<Object, Integer> map =
                new java.util.IdentityHashMap<Object, Integer>(n);
        for (int i = 0; i < n; i++) {
            keys[i] = new Object();
            map.put(keys[i], Integer.valueOf(i));
        }
        long hits = 0;
        int r = 24681357;
        for (int i = 0; i < 500000; i++) {
            r = rnd(r);
            if (map.get(keys[(r >>> 1) % n]) != null) {
                hits++;
            }
        }
        return hits;
    }

    // Build-dominated: grows from the default capacity, so every rehash
    // re-probes every live key.
    public static long identityMapBuild() {
        int n = 20000;
        Object[] keys = new Object[n];
        for (int i = 0; i < n; i++) {
            keys[i] = new Object();
        }
        long checksum = 0;
        for (int round = 0; round < 20; round++) {
            java.util.IdentityHashMap<Object, Integer> map =
                    new java.util.IdentityHashMap<Object, Integer>();
            for (int i = 0; i < n; i++) {
                map.put(keys[i], Integer.valueOf(i));
            }
            checksum += map.size();
        }
        return checksum;
    }

    // ---- 11. low load factor build ---------------------------------------
    // The regression guard for the growth rule. A table asked for a load factor
    // below 0.5 used to reach its threshold while still less than half full,
    // rebuild at the SAME capacity, and so rebuild again on the very next put:
    // 20000 inserts cost 19999 full rebuilds and 200 million rehashed entries.
    // Nothing at a default load factor can see that, so it needs its own shape.
    public static long lowLoadFactorBuild() {
        int perRound = 20000;
        Integer[] keys = new Integer[perRound];
        for (int i = 0; i < perRound; i++) {
            keys[i] = Integer.valueOf(i);
        }
        long checksum = 0;
        for (int round = 0; round < 20; round++) {
            HashMap<Integer, Integer> map = new HashMap<Integer, Integer>(16, 0.25f);
            for (int i = 0; i < perRound; i++) {
                map.put(keys[i], keys[i]);
            }
            checksum += map.size();
        }
        return checksum;
    }

    private static final int WARMUP = 3;
    private static final int MEASURE = 5;

    private interface BenchFn {
        long run();
    }

    private static void runBench(String name, BenchFn fn) {
        for (int warmup = 0; warmup < WARMUP; warmup++) {
            fn.run();
        }
        for (int repetition = 0; repetition < MEASURE; repetition++) {
            long started = System.nanoTime();
            long checksum = fn.run();
            long elapsed = System.nanoTime() - started;
            System.out.println("BENCH " + name + " rep " + repetition
                    + " ns=" + elapsed + " checksum=" + checksum);
        }
    }

    public static void main(String[] args) {
        runBench("missHeavy", new BenchFn() {
            public long run() { return missHeavy(); }
        });
        runBench("stringKeys", new BenchFn() {
            public long run() { return stringKeys(); }
        });
        runBench("largeTable", new BenchFn() {
            public long run() { return largeTable(); }
        });
        runBench("tombstones", new BenchFn() {
            public long run() { return tombstones(); }
        });
        runBench("growBuild", new BenchFn() {
            public long run() { return growBuild(); }
        });
        runBench("identityKeys", new BenchFn() {
            public long run() { return identityKeys(); }
        });
        runBench("linkedStringKeys", new BenchFn() {
            public long run() { return linkedStringKeys(); }
        });
        runBench("hashtableStringKeys", new BenchFn() {
            public long run() { return hashtableStringKeys(); }
        });
        runBench("hashtableBuild", new BenchFn() {
            public long run() { return hashtableBuild(); }
        });
        runBench("identityMapLookup", new BenchFn() {
            public long run() { return identityMapLookup(); }
        });
        runBench("identityMapBuild", new BenchFn() {
            public long run() { return identityMapBuild(); }
        });
        runBench("lowLoadFactorBuild", new BenchFn() {
            public long run() { return lowLoadFactorBuild(); }
        });
        System.out.println("DONE");
    }

    private MapBench() {
    }
}
