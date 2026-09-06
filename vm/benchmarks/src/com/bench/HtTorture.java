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

import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Semantic torture for {@link Hashtable}, comparable byte-for-byte against
 * HotSpot.
 *
 * <p>Written as a SAFETY NET before the representation changed, and it passed
 * against the chained-Entry implementation first -- a torture that only ever ran
 * against the new code proves nothing about the change.
 *
 * <p>Iteration order is unspecified and differs between implementations, so
 * every aggregation here is ORDER-INDEPENDENT (separate {@code +} and {@code ^}
 * accumulators, each of which commutes). {@code toString} is deliberately NOT
 * compared for the same reason -- only that it round-trips through
 * {@code length}.
 *
 * <p>Covers: the Dictionary half (keys/elements Enumerations, size, isEmpty),
 * the Map half (get/put/remove/containsKey/containsValue/contains/clear/putAll/
 * equals/hashCode), null rejection on both key and value, growth across several
 * rehashes, remove churn, all four views including removal through each and
 * entrySet setValue write-through, and a PRNG op mix replayed against a plain
 * array model.
 */
public class HtTorture {
    static int seed = 0x1234567;

    static int rnd(int bound) {
        seed = seed * 1103515245 + 12345;
        return (seed >>> 16) % bound;
    }

    /** order-independent digest: separate + and ^ accumulators. */
    static long digest(Map<Integer, Integer> m) {
        long sum = 0;
        long xor = 0;
        Iterator<Map.Entry<Integer, Integer>> it = m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> e = it.next();
            long k = e.getKey().intValue();
            long v = e.getValue().intValue();
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
        // ---- null rejection --------------------------------------------------
        Hashtable<Integer, Integer> t = new Hashtable<Integer, Integer>();
        int npes = 0;
        try {
            t.put(null, Integer.valueOf(1));
        } catch (NullPointerException e) {
            npes++;
        }
        try {
            t.put(Integer.valueOf(1), null);
        } catch (NullPointerException e) {
            npes++;
        }
        // NOTE: get(null)/containsKey(null) are deliberately NOT exercised.
        // They reach key.hashCode() on a null receiver, and a null receiver is
        // only a NullPointerException on this VM by way of the iOS port's
        // SIGSEGV handler (installSignalHandlers in
        // CodenameOne_GLAppDelegate.m). The clean target this torture runs on
        // installs no handler, so the same call is a hard SIGSEGV here -- see
        // the comment on cn1ThrowNullPointerOrDie in cn1_globals.h. Only the
        // EXPLICIT throws in put() are portable enough to compare.
        emit("nullRejection.npes", npes);
        emit("nullRejection.size", t.size());

        // ---- basic put/get/replace ------------------------------------------
        emit("basic.putFresh", t.put(Integer.valueOf(7), Integer.valueOf(70)) == null ? 1 : 0);
        emit("basic.putReplace", t.put(Integer.valueOf(7), Integer.valueOf(71)).intValue());
        emit("basic.get", t.get(Integer.valueOf(7)).intValue());
        emit("basic.size", t.size());
        emit("basic.containsKey", t.containsKey(Integer.valueOf(7)) ? 1 : 0);
        emit("basic.containsMissing", t.containsKey(Integer.valueOf(8)) ? 1 : 0);
        emit("basic.contains", t.contains(Integer.valueOf(71)) ? 1 : 0);
        emit("basic.containsValue", t.containsValue(Integer.valueOf(71)) ? 1 : 0);
        emit("basic.getMissing", t.get(Integer.valueOf(8)) == null ? 1 : 0);
        emit("basic.removeMissing", t.remove(Integer.valueOf(8)) == null ? 1 : 0);
        emit("basic.remove", t.remove(Integer.valueOf(7)).intValue());
        emit("basic.afterRemove.size", t.size());
        emit("basic.isEmpty", t.isEmpty() ? 1 : 0);

        // ---- growth across several rehashes ---------------------------------
        int n = 5000;
        Hashtable<Integer, Integer> grow = new Hashtable<Integer, Integer>();
        for (int i = 0; i < n; i++) {
            grow.put(Integer.valueOf(i), Integer.valueOf(i * 3));
        }
        emit("grow.size", grow.size());
        emit("grow.digest", digest(grow));
        emit("grow.hashCode", grow.hashCode());
        long found = 0;
        for (int i = 0; i < n; i++) {
            Integer v = grow.get(Integer.valueOf(i));
            if (v != null && v.intValue() == i * 3) {
                found++;
            }
        }
        emit("grow.allFound", found);

        // small capacity + a large load factor, so the table rehashes often
        Hashtable<Integer, Integer> tight = new Hashtable<Integer, Integer>(3, 0.9f);
        for (int i = 0; i < 2000; i++) {
            tight.put(Integer.valueOf(i * 17), Integer.valueOf(i));
        }
        emit("tight.size", tight.size());
        emit("tight.digest", digest(tight));

        // ---- Enumerations (the Dictionary half) -----------------------------
        long keySum = 0;
        long keyXor = 0;
        Enumeration<Integer> ke = grow.keys();
        while (ke.hasMoreElements()) {
            long k = ke.nextElement().intValue();
            keySum += k;
            keyXor ^= k * 31 + 7;
        }
        emit("keys.sum", keySum);
        emit("keys.xor", keyXor);
        long valSum = 0;
        long valXor = 0;
        Enumeration<Integer> ve = grow.elements();
        while (ve.hasMoreElements()) {
            long v = ve.nextElement().intValue();
            valSum += v;
            valXor ^= v * 31 + 7;
        }
        emit("elements.sum", valSum);
        emit("elements.xor", valXor);

        // ---- remove churn ----------------------------------------------------
        long survivors = 0;
        for (int pass = 0; pass < 3; pass++) {
            for (int i = pass; i < n; i += 3) {
                grow.remove(Integer.valueOf(i));
            }
            for (int i = 0; i < n; i++) {
                if (grow.containsKey(Integer.valueOf(i))) {
                    survivors++;
                }
            }
            emit("removeChurn.pass" + pass + ".size", grow.size());
            emit("removeChurn.pass" + pass + ".digest", digest(grow));
        }
        emit("removeChurn.survivors", survivors);

        // ---- interleaved remove/put -----------------------------------------
        Hashtable<Integer, Integer> churn = new Hashtable<Integer, Integer>();
        int live = 800;
        for (int i = 0; i < live; i++) {
            churn.put(Integer.valueOf(i), Integer.valueOf(i));
        }
        for (int i = 0; i < live * 3; i++) {
            churn.put(Integer.valueOf(i + live), Integer.valueOf(i));
            churn.remove(Integer.valueOf(i));
        }
        emit("interleaved.size", churn.size());
        emit("interleaved.digest", digest(churn));

        // ---- views -----------------------------------------------------------
        Hashtable<Integer, Integer> views = new Hashtable<Integer, Integer>();
        for (int i = 0; i < 300; i++) {
            views.put(Integer.valueOf(i), Integer.valueOf(i * 2));
        }
        Set<Integer> ks = views.keySet();
        Collection<Integer> vs = views.values();
        Set<Map.Entry<Integer, Integer>> es = views.entrySet();
        emit("views.keySet.size", ks.size());
        emit("views.values.size", vs.size());
        emit("views.entrySet.size", es.size());
        emit("views.keySet.contains", ks.contains(Integer.valueOf(5)) ? 1 : 0);
        emit("views.values.contains", vs.contains(Integer.valueOf(10)) ? 1 : 0);

        long ksum = 0;
        Iterator<Integer> ki = ks.iterator();
        while (ki.hasNext()) {
            ksum += ki.next().intValue();
        }
        emit("views.keySet.sum", ksum);

        // setValue write-through
        Iterator<Map.Entry<Integer, Integer>> ei = es.iterator();
        while (ei.hasNext()) {
            Map.Entry<Integer, Integer> e = ei.next();
            if (e.getKey().intValue() % 5 == 0) {
                e.setValue(Integer.valueOf(-e.getKey().intValue()));
            }
        }
        emit("views.afterSetValue.digest", digest(views));
        emit("views.setValueVisible", views.get(Integer.valueOf(10)).intValue());

        // removal through the entrySet iterator
        ei = es.iterator();
        while (ei.hasNext()) {
            if ((ei.next().getKey().intValue() & 1) == 0) {
                ei.remove();
            }
        }
        emit("views.afterIteratorRemove.size", views.size());
        emit("views.afterIteratorRemove.digest", digest(views));

        // removal through keySet
        views.keySet().remove(Integer.valueOf(3));
        emit("views.afterKeySetRemove.size", views.size());
        emit("views.containsRemoved", views.containsKey(Integer.valueOf(3)) ? 1 : 0);

        // ---- putAll, copy ctor, equals --------------------------------------
        Hashtable<Integer, Integer> src = new Hashtable<Integer, Integer>();
        for (int i = 0; i < 400; i++) {
            src.put(Integer.valueOf(i * 7), Integer.valueOf(i));
        }
        Hashtable<Integer, Integer> dst = new Hashtable<Integer, Integer>();
        dst.putAll(src);
        emit("putAll.size", dst.size());
        emit("putAll.digest", digest(dst));
        emit("putAll.equalsSrc", dst.equals(src) ? 1 : 0);
        emit("putAll.hashCodeMatches", dst.hashCode() == src.hashCode() ? 1 : 0);
        Hashtable<Integer, Integer> copy = new Hashtable<Integer, Integer>(src);
        emit("copyCtor.digest", digest(copy));
        emit("copyCtor.equals", copy.equals(src) ? 1 : 0);
        copy.remove(Integer.valueOf(0));
        emit("copyCtor.notEqualAfterRemove", copy.equals(src) ? 1 : 0);

        // toString is order-dependent, so only its shape is comparable
        emit("toString.startsWithBrace", src.toString().charAt(0) == '{' ? 1 : 0);
        emit("toString.endsWithBrace",
                src.toString().charAt(src.toString().length() - 1) == '}' ? 1 : 0);

        // ---- PRNG op mix vs a plain array model -----------------------------
        int universe = 2000;
        Integer[] model = new Integer[universe];
        boolean[] present = new boolean[universe];
        Hashtable<Integer, Integer> mix = new Hashtable<Integer, Integer>();
        long mismatches = 0;
        long modelSize = 0;
        for (int op = 0; op < 80000; op++) {
            int slot = rnd(universe);
            Integer key = Integer.valueOf(slot);
            int kind = rnd(10);
            if (kind < 5) {
                Integer v = Integer.valueOf(op + 1);
                Integer prev = mix.put(key, v);
                Integer modelPrev = present[slot] ? model[slot] : null;
                if ((prev == null) != (modelPrev == null)
                        || (prev != null && !prev.equals(modelPrev))) {
                    mismatches++;
                }
                if (!present[slot]) {
                    modelSize++;
                }
                present[slot] = true;
                model[slot] = v;
            } else if (kind < 7) {
                Integer got = mix.get(key);
                Integer want = present[slot] ? model[slot] : null;
                if ((got == null) != (want == null)
                        || (got != null && !got.equals(want))) {
                    mismatches++;
                }
            } else if (kind < 8) {
                if (mix.containsKey(key) != present[slot]) {
                    mismatches++;
                }
            } else {
                Integer removed = mix.remove(key);
                Integer want = present[slot] ? model[slot] : null;
                if ((removed == null) != (want == null)
                        || (removed != null && !removed.equals(want))) {
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
        long reachable = 0;
        for (int i = 0; i < universe; i++) {
            if (present[i] && model[i].equals(mix.get(Integer.valueOf(i)))) {
                reachable++;
            }
        }
        emit("opMix.reachable", reachable);

        // ---- clear -----------------------------------------------------------
        mix.clear();
        emit("clear.size", mix.size());
        emit("clear.isEmpty", mix.isEmpty() ? 1 : 0);
        emit("clear.keysEmpty", mix.keys().hasMoreElements() ? 1 : 0);
        emit("clear.getAfter", mix.get(Integer.valueOf(0)) == null ? 1 : 0);
        mix.put(Integer.valueOf(0), Integer.valueOf(77));
        emit("clear.reusable", mix.get(Integer.valueOf(0)).intValue());

        // ---- String keys, since that is how CN1 actually uses this -----------
        Hashtable<String, String> strs = new Hashtable<String, String>();
        for (int i = 0; i < 1000; i++) {
            strs.put("Component.style.uiid." + i, "value" + i);
        }
        long strSum = 0;
        long strXor = 0;
        Iterator<Map.Entry<String, String>> si = strs.entrySet().iterator();
        while (si.hasNext()) {
            Map.Entry<String, String> e = si.next();
            long mixv = e.getKey().hashCode() * 31L + e.getValue().hashCode();
            strSum += mixv;
            strXor ^= mixv * 17 + 3;
        }
        emit("strings.size", strs.size());
        emit("strings.sum", strSum);
        emit("strings.xor", strXor);
        emit("strings.get", strs.get("Component.style.uiid.500").equals("value500") ? 1 : 0);
        emit("strings.removeAll", strs.remove("Component.style.uiid.0") != null ? 1 : 0);
    }
}
