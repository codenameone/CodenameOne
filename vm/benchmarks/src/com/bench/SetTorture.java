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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Differential torture for HashSet and LinkedHashSet.
 *
 * <p>Written BEFORE changing how a set stores its elements, so that the change has a
 * before/after control rather than a hope. Neither of these classes had any coverage
 * in the gauntlet: the four existing map tortures (MapTorture, MapTorture2, HtTorture,
 * IdmTorture) mention HashSet exactly zero times, which is how a set could have been
 * re-implemented on top of new storage with nothing to catch a wrong probe sequence,
 * a lost tombstone or an off-by-one in growth.
 *
 * <p>ITERATION ORDER IS THE TRAP, and it is why this file is shaped the way it is.
 * HashSet's order is explicitly unspecified, so ours and the reference JDK's are free
 * to differ for correct implementations and a raw dump would report a false
 * divergence. Everything order-dependent here is therefore SORTED before printing, and
 * only facts the specification actually fixes -- size, membership, equals, hashCode --
 * are compared directly. LinkedHashSet is the exception: its order IS specified as
 * insertion order, so it is printed as-is and a divergence there is a real bug.
 *
 * <p>The cases that break open-addressed sets, and why each is here:
 * <ul>
 * <li>FORCED COLLISIONS. Clash gives every instance the same hashCode, so every add
 *     walks the probe sequence to its end. Without this the table is never stressed.
 * <li>TOMBSTONES. Interleaved add/remove leaves deleted slots that a lookup must probe
 *     THROUGH but an insert may reuse -- getting that wrong loses elements that are
 *     still present, and only under a specific add/remove order.
 * <li>GROWTH ACROSS A RESIZE with tombstones present, which is where a rebuild that
 *     copies dead slots or drops live ones shows up.
 * <li>null, which is a legal element and a separate path in every probe.
 * <li>Iterator.remove, and the ConcurrentModificationException contract around it.
 * </ul>
 */
public class SetTorture {
    private static final StringBuilder OUT = new StringBuilder();

    /** Same hashCode for every instance: every add and lookup probes the whole chain. */
    static final class Clash {
        final int id;
        Clash(int id) { this.id = id; }
        @Override public int hashCode() { return 7; }
        @Override public boolean equals(Object o) {
            return o instanceof Clash && ((Clash) o).id == id;
        }
        @Override public String toString() { return "C" + id; }
    }

    private static void p(String label, Object v) {
        OUT.append(label).append('=').append(v).append('\n');
    }

    /** Order-independent fingerprint: sorted contents plus the specified facts. */
    private static void dump(String label, Set<?> set) {
        List<String> items = new ArrayList<String>();
        for (Object o : set) {
            items.add(String.valueOf(o));
        }
        Collections.sort(items);
        OUT.append(label)
           .append(" size=").append(set.size())
           .append(" empty=").append(set.isEmpty())
           .append(" hash=").append(set.hashCode())
           .append(" items=").append(items)
           .append('\n');
    }

    /** LinkedHashSet only: insertion order is specified, so print it raw. */
    private static void dumpOrdered(String label, Set<?> set) {
        OUT.append(label).append(" size=").append(set.size()).append(" order=[");
        boolean first = true;
        for (Object o : set) {
            if (!first) OUT.append(", ");
            OUT.append(String.valueOf(o));
            first = false;
        }
        OUT.append("]\n");
    }

    private static void exercise(String tag, Set<String> set, boolean ordered) {
        for (int i = 0; i < 40; i++) {
            set.add("e" + i);
        }
        if (ordered) dumpOrdered(tag + ".filled", set); else dump(tag + ".filled", set);

        // Tombstones: remove every third, then probe for the survivors.
        for (int i = 0; i < 40; i += 3) {
            set.remove("e" + i);
        }
        if (ordered) dumpOrdered(tag + ".holes", set); else dump(tag + ".holes", set);
        for (int i = 0; i < 40; i++) {
            OUT.append(tag).append(".contains").append(i).append('=')
               .append(set.contains("e" + i)).append('\n');
        }

        // Re-add through the tombstones, which must reuse them rather than grow.
        for (int i = 0; i < 40; i += 3) {
            p(tag + ".readd" + i, set.add("e" + i));
        }
        if (ordered) dumpOrdered(tag + ".readded", set); else dump(tag + ".readded", set);

        // Growth well past the initial table, with holes already present.
        for (int i = 40; i < 400; i++) {
            set.add("e" + i);
        }
        if (ordered) dumpOrdered(tag + ".grown", set); else dump(tag + ".grown", set);

        p(tag + ".dupAdd", set.add("e5"));
        p(tag + ".missRemove", set.remove("nope"));
        p(tag + ".nullAdd", set.add(null));
        p(tag + ".nullContains", set.contains(null));
        p(tag + ".nullAddAgain", set.add(null));
        p(tag + ".nullRemove", set.remove(null));
        p(tag + ".nullContainsAfter", set.contains(null));

        // Iterator.remove across the whole table.
        int removed = 0;
        for (Iterator<String> it = set.iterator(); it.hasNext(); ) {
            String s = it.next();
            if (s != null && s.length() % 2 == 0) { it.remove(); removed++; }
        }
        p(tag + ".iterRemoved", removed);
        if (ordered) dumpOrdered(tag + ".afterIterRemove", set); else dump(tag + ".afterIterRemove", set);

        // Bulk operations, all specified order-independently.
        List<String> some = new ArrayList<String>();
        for (int i = 0; i < 60; i += 2) some.add("e" + i);
        p(tag + ".containsAll", set.containsAll(some));
        p(tag + ".addAll", set.addAll(some));
        if (ordered) dumpOrdered(tag + ".afterAddAll", set); else dump(tag + ".afterAddAll", set);
        p(tag + ".removeAll", set.removeAll(some));
        if (ordered) dumpOrdered(tag + ".afterRemoveAll", set); else dump(tag + ".afterRemoveAll", set);
        p(tag + ".retainAll", set.retainAll(some));
        if (ordered) dumpOrdered(tag + ".afterRetainAll", set); else dump(tag + ".afterRetainAll", set);

        set.clear();
        if (ordered) dumpOrdered(tag + ".cleared", set); else dump(tag + ".cleared", set);
        p(tag + ".addAfterClear", set.add("fresh"));
        if (ordered) dumpOrdered(tag + ".afterClear", set); else dump(tag + ".afterClear", set);
    }

    /**
     * size() MUST agree with what the iterator yields, and toArray() is where a
     * disagreement becomes someone else's crash rather than a wrong count:
     * AbstractCollection.toArray allocates new Object[size()] and fills it from the
     * iterator, so an iterator that yields FEWER elements leaves trailing NULLs in an
     * array the caller then dereferences. That is a null receiver far from the set,
     * which is exactly the shape of the failure this file exists to catch.
     */
    private static void agreement(String tag, Set<?> set) {
        int declared = set.size();
        int counted = 0;
        for (Iterator<?> it = set.iterator(); it.hasNext(); ) { it.next(); counted++; }
        Object[] array = set.toArray();
        int nulls = 0;
        for (int i = 0; i < array.length; i++) if (array[i] == null) nulls++;
        OUT.append(tag).append(" size=").append(declared)
           .append(" iterated=").append(counted)
           .append(" agree=").append(declared == counted)
           .append(" arrayLen=").append(array.length)
           .append(" arrayNulls=").append(nulls)
           .append('\n');
    }

    private static void collisions(String tag, Set<Clash> set) {
        for (int i = 0; i < 64; i++) set.add(new Clash(i));
        dump(tag + ".clashFilled", set);
        for (int i = 0; i < 64; i += 2) set.remove(new Clash(i));
        dump(tag + ".clashHoles", set);
        for (int i = 0; i < 64; i++) {
            OUT.append(tag).append(".clashContains").append(i).append('=')
               .append(set.contains(new Clash(i))).append('\n');
        }
        for (int i = 0; i < 64; i += 2) set.add(new Clash(i));
        dump(tag + ".clashReadded", set);
    }

    /** Build sets in many shapes and check size/iterator/toArray agree on each. */
    private static void agreementSweep() {
        for (int n = 0; n < 200; n += 7) {
            Set<String> s = new HashSet<String>();
            for (int i = 0; i < n; i++) s.add("a" + i);
            agreement("agree.add" + n, s);
            for (int i = 0; i < n; i += 3) s.remove("a" + i);
            agreement("agree.rm" + n, s);
            for (int i = 0; i < n; i += 3) s.add("a" + i);
            agreement("agree.re" + n, s);
            for (Iterator<String> it = s.iterator(); it.hasNext(); ) {
                if (it.next().length() % 2 == 0) it.remove();
            }
            agreement("agree.iter" + n, s);
        }
        for (int n = 0; n < 80; n += 5) {
            Set<Clash> s = new HashSet<Clash>();
            for (int i = 0; i < n; i++) s.add(new Clash(i));
            for (int i = 0; i < n; i += 2) s.remove(new Clash(i));
            for (int i = 0; i < n; i += 2) s.add(new Clash(i));
            agreement("agree.clash" + n, s);
        }
    }

    public static void main(String[] args) {
        agreementSweep();
        exercise("hs", new HashSet<String>(), false);
        exercise("lhs", new LinkedHashSet<String>(), true);
        exercise("hsCap", new HashSet<String>(1), false);
        exercise("lhsCap", new LinkedHashSet<String>(1), true);

        collisions("hs", new HashSet<Clash>());
        collisions("lhs", new LinkedHashSet<Clash>());

        // Construction from a collection, and set-to-set equality.
        List<String> seed = new ArrayList<String>();
        for (int i = 0; i < 50; i++) seed.add("s" + (i % 30));
        Set<String> fromCollection = new HashSet<String>(seed);
        dump("fromCollection", fromCollection);
        Set<String> other = new HashSet<String>(seed);
        p("setEquals", fromCollection.equals(other));
        p("setHashEq", fromCollection.hashCode() == other.hashCode());
        Set<String> linked = new LinkedHashSet<String>(seed);
        p("crossTypeEquals", fromCollection.equals(linked));
        p("crossTypeHashEq", fromCollection.hashCode() == linked.hashCode());
        dumpOrdered("linkedFromCollection", linked);

        // Churn, so the storage runs under real GC pressure and reuse.
        long ck = 0;
        for (int round = 0; round < 3000; round++) {
            Set<String> s = (round & 1) == 0
                ? new HashSet<String>() : new LinkedHashSet<String>();
            for (int i = 0; i < (round % 37) + 1; i++) s.add("k" + i + "_" + (round & 7));
            for (int i = 0; i < (round % 13); i++) s.remove("k" + i + "_" + (round & 7));
            ck += s.size() + s.hashCode();
            if (s.contains("k0_" + (round & 7))) ck++;
        }
        p("churn", ck);

        System.out.println(OUT.toString());
    }
}
