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

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Semantic torture for {@link IdentityHashMap}, comparable byte-for-byte
 * against HotSpot.
 *
 * <p>Two things make this map awkward to compare across VMs, and both are
 * designed around here:
 *
 * <ul>
 * <li><b>Identity hash codes differ by construction</b> (ParparVM's is a
 * truncated object address), so iteration order differs and every aggregation
 * below is ORDER-INDEPENDENT -- separate {@code +} and {@code ^} accumulators,
 * each of which commutes.
 * <li><b>The keys cannot be bare Objects</b>, because a digest then has nothing
 * stable to hash. {@link K} carries an {@code id} the digest can use, and is
 * deliberately given {@code equals}/{@code hashCode} by id so that two distinct
 * {@code K(5)} instances are EQUAL yet must remain SEPARATE keys -- the defining
 * property of this map, and the one a String- or Integer-keyed test cannot
 * check.
 * </ul>
 *
 * <p>Covers: put/get/remove/containsKey/containsValue/clear/size, null key and
 * null value, equal-but-distinct keys, growth across several rehashes, the
 * backward-shift relocation on remove (which is what a wrong probe index
 * corrupts most quietly), keySet/values/entrySet iteration and removal through
 * views, putAll, and a PRNG op mix replayed against a plain array model.
 */
public class IdmTorture {
    static int seed = 0x1234567;

    static int rnd(int bound) {
        seed = seed * 1103515245 + 12345;
        return (seed >>> 16) % bound;
    }

    /** A key with a stable payload, equal by id but distinct by identity. */
    static final class K {
        final int id;

        K(int id) {
            this.id = id;
        }

        public boolean equals(Object o) {
            return (o instanceof K) && ((K) o).id == id;
        }

        public int hashCode() {
            return id;
        }
    }

    /** order-independent digest over (key id, value) pairs. */
    static long digest(Map<K, Integer> m) {
        long sum = 0;
        long xor = 0;
        Iterator<Map.Entry<K, Integer>> it = m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, Integer> e = it.next();
            long k = e.getKey() == null ? -7 : e.getKey().id;
            long v = e.getValue() == null ? -13 : e.getValue().intValue();
            long mix = (k * 0x9E3779B97F4A7C15L) ^ (v * 0xC2B2AE3D27D4EB4FL);
            sum += mix;
            xor ^= mix * 31 + 17;
        }
        return sum * 31 + xor + m.size();
    }

    static void emit(String label, long value) {
        System.out.println(label + "=" + value);
    }

    public static void main(String[] args) {
        // ---- equal-but-distinct keys stay separate --------------------------
        IdentityHashMap<K, Integer> id = new IdentityHashMap<K, Integer>();
        K a = new K(5);
        K b = new K(5);
        id.put(a, Integer.valueOf(1));
        id.put(b, Integer.valueOf(2));
        emit("distinctEqualKeys.size", id.size());
        emit("distinctEqualKeys.a", id.get(a).intValue());
        emit("distinctEqualKeys.b", id.get(b).intValue());
        emit("distinctEqualKeys.containsA", id.containsKey(a) ? 1 : 0);
        emit("distinctEqualKeys.containsFresh", id.containsKey(new K(5)) ? 1 : 0);
        // replacing through the SAME reference must overwrite, not add
        id.put(a, Integer.valueOf(9));
        emit("distinctEqualKeys.afterReplace.size", id.size());
        emit("distinctEqualKeys.afterReplace.a", id.get(a).intValue());

        // ---- null key and null value ---------------------------------------
        IdentityHashMap<K, Integer> nulls = new IdentityHashMap<K, Integer>();
        nulls.put(null, Integer.valueOf(42));
        nulls.put(new K(1), null);
        emit("nulls.size", nulls.size());
        emit("nulls.nullKey", nulls.get(null).intValue());
        emit("nulls.containsNullKey", nulls.containsKey(null) ? 1 : 0);
        emit("nulls.containsNullValue", nulls.containsValue(null) ? 1 : 0);
        emit("nulls.removeNullKey", nulls.remove(null).intValue());
        emit("nulls.afterRemove.size", nulls.size());

        // ---- growth across several rehashes --------------------------------
        int n = 4000;
        K[] keys = new K[n];
        IdentityHashMap<K, Integer> grow = new IdentityHashMap<K, Integer>();
        for (int i = 0; i < n; i++) {
            keys[i] = new K(i);
            grow.put(keys[i], Integer.valueOf(i * 3));
        }
        emit("grow.size", grow.size());
        emit("grow.digest", digest(grow));
        long found = 0;
        for (int i = 0; i < n; i++) {
            Integer v = grow.get(keys[i]);
            if (v != null && v.intValue() == i * 3) {
                found++;
            }
        }
        emit("grow.allFound", found);

        // ---- remove churn: the backward-shift relocation --------------------
        // Every removal shifts a following entry back over the hole when that
        // entry probed past it. Get the index arithmetic wrong and a key that
        // is still present becomes unreachable -- silently, and only for some
        // keys, which is why this re-checks EVERY survivor after every pass.
        long survivors = 0;
        for (int pass = 0; pass < 3; pass++) {
            for (int i = pass; i < n; i += 3) {
                grow.remove(keys[i]);
            }
            for (int i = 0; i < n; i++) {
                if (grow.containsKey(keys[i])) {
                    survivors++;
                }
            }
            emit("removeChurn.pass" + pass + ".size", grow.size());
            emit("removeChurn.pass" + pass + ".digest", digest(grow));
        }
        emit("removeChurn.survivors", survivors);

        // ---- interleaved remove/put (relocation under reuse) ----------------
        IdentityHashMap<K, Integer> churn = new IdentityHashMap<K, Integer>();
        int live = 600;
        K[] ck = new K[live * 3];
        for (int i = 0; i < ck.length; i++) {
            ck[i] = new K(i);
        }
        for (int i = 0; i < live; i++) {
            churn.put(ck[i], Integer.valueOf(i));
        }
        for (int i = 0; i < live * 2; i++) {
            churn.put(ck[(i + live) % ck.length], Integer.valueOf(i));
            churn.remove(ck[i % ck.length]);
        }
        emit("interleaved.size", churn.size());
        emit("interleaved.digest", digest(churn));

        // ---- views ----------------------------------------------------------
        IdentityHashMap<K, Integer> views = new IdentityHashMap<K, Integer>();
        K[] vk = new K[200];
        for (int i = 0; i < vk.length; i++) {
            vk[i] = new K(i);
            views.put(vk[i], Integer.valueOf(i));
        }
        long keySum = 0;
        Iterator<K> ki = views.keySet().iterator();
        while (ki.hasNext()) {
            keySum += ki.next().id;
        }
        emit("views.keySum", keySum);
        long valSum = 0;
        Iterator<Integer> vi = views.values().iterator();
        while (vi.hasNext()) {
            valSum += vi.next().intValue();
        }
        emit("views.valSum", valSum);
        // removal through the entrySet iterator
        Iterator<Map.Entry<K, Integer>> ei = views.entrySet().iterator();
        while (ei.hasNext()) {
            if ((ei.next().getKey().id & 1) == 0) {
                ei.remove();
            }
        }
        emit("views.afterIteratorRemove.size", views.size());
        emit("views.afterIteratorRemove.digest", digest(views));
        // removal through keySet
        views.keySet().remove(vk[3]);
        emit("views.afterKeySetRemove.size", views.size());
        emit("views.containsRemoved", views.containsKey(vk[3]) ? 1 : 0);

        // ---- putAll ---------------------------------------------------------
        IdentityHashMap<K, Integer> src = new IdentityHashMap<K, Integer>();
        K[] pk = new K[500];
        for (int i = 0; i < pk.length; i++) {
            pk[i] = new K(i * 7);
            src.put(pk[i], Integer.valueOf(i));
        }
        IdentityHashMap<K, Integer> dst = new IdentityHashMap<K, Integer>();
        dst.putAll(src);
        emit("putAll.size", dst.size());
        emit("putAll.digest", digest(dst));
        emit("putAll.copyCtor.digest", digest(new IdentityHashMap<K, Integer>(src)));

        // ---- PRNG op mix, replayed against a plain array model --------------
        // The model is deliberately dumb -- linear scan over parallel arrays,
        // compared with == -- so it shares no code or assumption with the map.
        int universe = 1500;
        K[] uk = new K[universe];
        for (int i = 0; i < universe; i++) {
            uk[i] = new K(i);
        }
        Integer[] model = new Integer[universe];
        boolean[] present = new boolean[universe];
        IdentityHashMap<K, Integer> mix = new IdentityHashMap<K, Integer>();
        long mismatches = 0;
        long modelSize = 0;
        for (int op = 0; op < 60000; op++) {
            int slot = rnd(universe);
            int kind = rnd(10);
            if (kind < 5) {
                Integer v = Integer.valueOf(op);
                Integer prev = mix.put(uk[slot], v);
                Integer modelPrev = present[slot] ? model[slot] : null;
                if (prev != modelPrev) {
                    mismatches++;
                }
                if (!present[slot]) {
                    modelSize++;
                }
                present[slot] = true;
                model[slot] = v;
            } else if (kind < 7) {
                Integer got = mix.get(uk[slot]);
                Integer want = present[slot] ? model[slot] : null;
                if (got != want) {
                    mismatches++;
                }
            } else if (kind < 8) {
                if (mix.containsKey(uk[slot]) != present[slot]) {
                    mismatches++;
                }
            } else {
                Integer removed = mix.remove(uk[slot]);
                Integer want = present[slot] ? model[slot] : null;
                if (removed != want) {
                    mismatches++;
                }
                if (present[slot]) {
                    modelSize--;
                }
                present[slot] = false;
                model[slot] = null;
            }
            if (mix.size() != modelSize) {
                mismatches++;
            }
        }
        emit("opMix.mismatches", mismatches);
        emit("opMix.size", mix.size());
        emit("opMix.digest", digest(mix));

        // every surviving key must still be reachable
        long reachable = 0;
        for (int i = 0; i < universe; i++) {
            if (present[i] && mix.get(uk[i]) == model[i]) {
                reachable++;
            }
        }
        emit("opMix.reachable", reachable);

        // ---- clear ----------------------------------------------------------
        mix.clear();
        emit("clear.size", mix.size());
        emit("clear.isEmpty", mix.isEmpty() ? 1 : 0);
        emit("clear.getAfter", mix.get(uk[0]) == null ? 1 : 0);
        mix.put(uk[0], Integer.valueOf(77));
        emit("clear.reusable", mix.get(uk[0]).intValue());
    }
}
