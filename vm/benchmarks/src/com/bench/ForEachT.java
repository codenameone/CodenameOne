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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Semantics gate for for-each loop specialization.
 *
 * Where the receiver is a PROVABLE java.util.ArrayList the translator deletes the
 * iterator entirely and walks the backing array, with no runtime class test, because
 * the class is proven rather than checked. The body is not duplicated -- only the loop
 * header and the element fetch are rewritten in place. Everything here must stay
 * byte-identical to the host JVM through that change. The shapes are the ones such a
 * rewrite gets wrong: control flow that leaves the body, control flow that re-enters
 * it, nesting, and receivers that must NOT take the fast path.
 *
 * IT MUST DRIVE BOTH PATHS OR IT PROVES NOTHING. The receiver matrix below passes its
 * list as a PARAMETER, which the analysis deliberately refuses -- so the matrix alone
 * exercised the fast path exactly once out of twenty sites and would have passed
 * whether or not the rewrite worked at all. The two blocks after it exist for that:
 * `*Local` builds the ArrayList as a local (resolved by single assignment) and Holder
 * reads it from an instance field (resolved by the whole-program store map), so every
 * shape above runs down the fast path by BOTH routes and has to agree with the host.
 *
 * Mutation and repeated iterator removal also exercise the native cursor state.
 */
public class ForEachT {
    static List<Integer> arrayList(int n) {
        List<Integer> l = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) { l.add(Integer.valueOf(i)); }
        return l;
    }

    static List<Integer> linkedList(int n) {
        List<Integer> l = new LinkedList<Integer>();
        for (int i = 0; i < n; i++) { l.add(Integer.valueOf(i)); }
        return l;
    }

    /** plain loop -- the shape the fast path exists for */
    static long plain(List<Integer> l) {
        long n = 0;
        for (Integer v : l) { n += v.intValue(); }
        return n;
    }

    /** break leaves the body for a label the clone must SHARE with the original */
    static long withBreak(List<Integer> l, int stop) {
        long n = 0;
        for (Integer v : l) {
            if (v.intValue() == stop) { break; }
            n += v.intValue();
        }
        return n;
    }

    /** continue re-enters the condition -- in the clone it must reach the CLONE's */
    static long withContinue(List<Integer> l) {
        long n = 0;
        for (Integer v : l) {
            if ((v.intValue() & 1) == 0) { continue; }
            n += v.intValue();
        }
        return n;
    }

    /** an early return out of the middle of a duplicated body */
    static long withReturn(List<Integer> l, int stop) {
        long n = 0;
        for (Integer v : l) {
            n += v.intValue();
            if (v.intValue() == stop) { return n * 1000; }
        }
        return n;
    }

    /** nested loops: every label in the inner loop has to be cloned too */
    static long nested(List<Integer> a, List<Integer> b) {
        long n = 0;
        for (Integer x : a) {
            for (Integer y : b) {
                n += x.intValue() * y.intValue();
            }
        }
        return n;
    }

    /** two sequential for-each loops over the same local */
    static long sequential(List<Integer> l) {
        long n = 0;
        for (Integer v : l) { n += v.intValue(); }
        for (Integer v : l) { n -= v.intValue() / 2; }
        return n;
    }

    /**
     * A try/catch INSIDE the body -- exception ranges are label-relative, so a loop
     * duplicator that clones the body without cloning the range silently drops the
     * handler.
     *
     * The throw is EXPLICIT rather than an integer division by zero. ParparVM answers
     * 0 for x/0 instead of throwing ArithmeticException, so a div-by-zero here would
     * diverge from the host for a reason that has nothing to do with for-each, and a
     * torture in run-gauntlet.sh must isolate what it is testing.
     */
    static long withTry(List<Integer> l) {
        long n = 0;
        for (Integer v : l) {
            try {
                if (v.intValue() == 5) {
                    throw new IllegalStateException("five");
                }
                n += v.intValue();
            } catch (IllegalStateException e) {
                n += 7;
            }
        }
        return n;
    }

    /** the loop variable is reassigned inside the body */
    static long reassigns(List<Integer> l) {
        long n = 0;
        for (Integer v : l) {
            v = Integer.valueOf(v.intValue() * 2);
            n += v.intValue();
        }
        return n;
    }

    // ---------------------------------------------------------------------------
    // FAST-PATH DRIVERS. Same shapes, but with a receiver the translator can prove.

    /**
     * Fills a list the caller allocated. The list MUST be allocated in the caller and
     * kept in a local there: a local assigned from a method call is not a proven
     * receiver (the analysis refuses it deliberately), and an earlier version of this
     * file did exactly that and drove the fast path zero times.
     */
    private static void fill(List<Integer> l, int n) {
        for (int i = 0; i < n; i++) { l.add(Integer.valueOf(i)); }
    }

    /**
     * Every shape over a LOCAL whose single assignment is `new ArrayList`, which is the
     * first of the two ways the receiver's class is proven. Written out rather than
     * delegating: passing the list to a helper would put it in a parameter and take the
     * slow path, which is exactly the vacuity this block exists to remove.
     */
    static long localShapes(int n, int stop) {
        long acc = 0;

        List<Integer> a = new ArrayList<Integer>();
        fill(a, n);
        for (Integer v : a) { acc += v.intValue(); }

        List<Integer> b = new ArrayList<Integer>();
        fill(b, n);
        for (Integer v : b) { if (v.intValue() == stop) { break; } acc += v.intValue() * 3; }

        List<Integer> c = new ArrayList<Integer>();
        fill(c, n);
        for (Integer v : c) { if ((v.intValue() & 1) == 0) { continue; } acc += v.intValue() * 5; }

        List<Integer> d = new ArrayList<Integer>();
        fill(d, n);
        for (Integer v : d) { if (v.intValue() == stop) { return acc * 7; } acc += v.intValue(); }

        return acc;
    }

    /** early return out of a fast-path loop, reached on its own so the value is used */
    static long localReturn(int n, int stop) {
        List<Integer> l = new ArrayList<Integer>();
        fill(l, n);
        long acc = 0;
        for (Integer v : l) {
            if (v.intValue() == stop) { return acc; }
            acc += v.intValue();
        }
        return acc + 1;
    }

    /** two fast-path loops over one local, then a nested pair */
    static long localSequentialAndNested(int n) {
        List<Integer> l = new ArrayList<Integer>();
        fill(l, n);
        long acc = 0;
        for (Integer v : l) { acc += v.intValue(); }
        for (Integer v : l) { acc += v.intValue() * 2; }
        List<Integer> inner = new ArrayList<Integer>();
        fill(inner, 3);
        for (Integer v : l) {
            for (Integer w : inner) { acc += v.intValue() * w.intValue(); }
        }
        return acc;
    }

    /**
     * try/catch INSIDE a fast-path body. Exception ranges are label-relative, so a
     * rewrite that disturbs the body's labels silently drops the handler. The throw is
     * explicit: ParparVM answers 0 for integer division by zero instead of throwing, so
     * a div-by-zero here would diverge for a reason unrelated to for-each.
     */
    static long localTry(int n) {
        List<Integer> l = new ArrayList<Integer>();
        fill(l, n);
        long acc = 0;
        for (Integer v : l) {
            try {
                if (v.intValue() == 5) { throw new IllegalStateException("five"); }
                acc += v.intValue();
            } catch (IllegalStateException e) {
                acc += 7;
            }
        }
        return acc;
    }

    /** the loop variable reassigned inside a fast-path body */
    static long localReassigns(int n) {
        List<Integer> l = new ArrayList<Integer>();
        fill(l, n);
        long acc = 0;
        for (Integer v : l) {
            v = Integer.valueOf(v.intValue() * 2);
            acc += v.intValue();
        }
        return acc;
    }

    /**
     * The second route to a proven receiver: an INSTANCE FIELD into which the whole
     * program only ever stores `new ArrayList`. This is the route that matters most --
     * 75 of the 141 field-receiver for-each sites on the self-hosting corpus resolve
     * this way, against 13 for locals -- and nothing else here would exercise it.
     */
    static final class Holder {
        private final List<Integer> items = new ArrayList<Integer>();

        Holder(int n) {
            for (int i = 0; i < n; i++) { items.add(Integer.valueOf(i)); }
        }

        long plain() {
            long acc = 0;
            for (Integer v : items) { acc += v.intValue(); }
            return acc;
        }

        long withBreak(int stop) {
            long acc = 0;
            for (Integer v : items) { if (v.intValue() == stop) { break; } acc += v.intValue(); }
            return acc;
        }

        long withContinue() {
            long acc = 0;
            for (Integer v : items) { if ((v.intValue() % 3) == 0) { continue; } acc += v.intValue(); }
            return acc;
        }

        long withReturn(int stop) {
            long acc = 0;
            for (Integer v : items) { if (v.intValue() == stop) { return acc; } acc += v.intValue(); }
            return acc + 1;
        }

        long nested() {
            long acc = 0;
            for (Integer v : items) {
                for (Integer w : items) { acc += v.intValue() ^ w.intValue(); }
            }
            return acc;
        }

        long withTry() {
            long acc = 0;
            for (Integer v : items) {
                try {
                    if (v.intValue() == 5) { throw new IllegalStateException("five"); }
                    acc += v.intValue();
                } catch (IllegalStateException e) {
                    acc += 7;
                }
            }
            return acc;
        }

        long sequential() {
            long acc = 0;
            for (Integer v : items) { acc += v.intValue(); }
            for (Integer v : items) { acc += v.intValue() * 2; }
            return acc;
        }

        long reassigns() {
            long acc = 0;
            for (Integer v : items) {
                v = Integer.valueOf(v.intValue() * 2);
                acc += v.intValue();
            }
            return acc;
        }
    }

    static int mutationChecks() {
        int score = 0;
        List<Integer> list = new ArrayList<Integer>();
        list.add(1); list.add(2); list.add(3);
        try {
            for (Integer value : list) list.add(value);
        } catch (java.util.ConcurrentModificationException expected) { score++; }
        list.clear(); list.add(1); list.add(2); list.add(3);
        for (java.util.Iterator<Integer> iterator = list.iterator(); iterator.hasNext();) {
            iterator.next();
            iterator.remove();
            try { iterator.remove(); }
            catch (IllegalStateException expected) { score += 10; }
        }
        return score + list.size() * 100;
    }

    static int collectionSum(java.util.Collection<Integer> values) {
        int sum = 0;
        for (Integer value : values) if (value != null) sum += value;
        return sum;
    }

    static String setChecks() {
        java.util.Set<Integer> set = new java.util.HashSet<Integer>();
        set.add(null);
        for (int i = 0; i < 40; i++) set.add(i);
        int sum = collectionSum(set);
        for (java.util.Iterator<Integer> it = set.iterator(); it.hasNext();) {
            Integer value = it.next();
            if (value == null || (value & 1) == 0) it.remove();
        }
        sum += collectionSum(set);
        java.util.Set<Integer> ordered = new java.util.LinkedHashSet<Integer>();
        for (int i = 10; i >= 0; i--) ordered.add(i);
        for (java.util.Iterator<Integer> it = ordered.iterator(); it.hasNext();) {
            if ((it.next() & 1) == 0) {
                it.remove();
                try { it.remove(); throw new AssertionError(); }
                catch (IllegalStateException expected) { }
            }
        }
        java.util.HashMap<Integer, Integer> map = new java.util.HashMap<Integer, Integer>();
        for (int i = 0; i < 20; i++) map.put(i, i * 2);
        for (Integer value : map.values()) sum += value;
        for (Integer key : map.keySet()) sum -= key;
        sum += collectionSum(map.values());
        java.util.LinkedHashMap<Integer, Integer> linked = new java.util.LinkedHashMap<Integer, Integer>();
        for (int i = 10; i >= 0; i--) linked.put(i, i);
        for (java.util.Iterator<Integer> it = linked.values().iterator(); it.hasNext();) {
            if ((it.next() & 1) == 0) it.remove();
        }
        String order = ordered.toString() + "/" + linked.keySet().toString();
        boolean failedFast = false;
        try { for (Integer value : ordered) ordered.add(99); }
        catch (java.util.ConcurrentModificationException expected) { failedFast = true; }
        java.util.Set<Integer> custom = new java.util.HashSet<Integer>() {
            public java.util.Iterator<Integer> iterator() { return java.util.Collections.singleton(123).iterator(); }
        };
        custom.add(456);
        return sum + ":" + order + ":" + failedFast + ":" + collectionSum(custom);
    }

    public static void main(String[] args) {
        long ck = 0;
        List<Integer> al = arrayList(64);
        List<Integer> ll = linkedList(64);
        List<Integer> empty = new ArrayList<Integer>();
        List<Integer> one = arrayList(1);
        List<Integer> unmod = Collections.unmodifiableList(al);
        List<Integer> fixed = Arrays.asList(new Integer[] {
            Integer.valueOf(3), Integer.valueOf(1), Integer.valueOf(4) });
        List<Integer> sub = al.subList(4, 20);

        // every receiver shape: the fast path must fire for some and NOT for others,
        // and the answer has to be the same either way
        List<List<Integer>> all = new ArrayList<List<Integer>>();
        all.add(al); all.add(ll); all.add(empty); all.add(one);
        all.add(unmod); all.add(fixed); all.add(sub);

        String[] names = { "ArrayList", "LinkedList", "empty", "one",
                           "unmodifiable", "Arrays.asList", "subList" };
        int idx = 0;
        for (List<Integer> l : all) {
            long p = plain(l), b = withBreak(l, 13), c = withContinue(l);
            long r = withReturn(l, 21), q = sequential(l), t = withTry(l);
            long a2 = reassigns(l), ne = nested(l, one);
            // printed per receiver, not just summed: a checksum says a duplicated loop
            // is wrong somewhere, this says WHICH shape and WHICH receiver.
            System.out.println(names[idx] + " plain=" + p + " break=" + b + " cont=" + c
                + " ret=" + r + " seq=" + q + " try=" + t + " reas=" + a2 + " nest=" + ne);
            ck += p * 2 + b * 3 + c * 5 + r * 7 + q * 11 + t * 13 + a2 * 17 + ne * 19;
            idx++;
        }
        ck += nested(al, ll);
        ck += nested(ll, al);

        // FAST PATH, route 1: a local whose single assignment is the allocation.
        System.out.println("local shapes=" + localShapes(64, 13)
            + " ret=" + localReturn(64, 21)
            + " seqNest=" + localSequentialAndNested(64)
            + " try=" + localTry(64)
            + " reas=" + localReassigns(64));
        ck += localShapes(64, 13) * 23 + localReturn(64, 21) * 29
            + localSequentialAndNested(64) * 31 + localTry(64) * 37
            + localReassigns(64) * 41;
        // and the empty and single-element cases down the same path
        System.out.println("local edge empty=" + localShapes(0, 13) + " one=" + localShapes(1, 13));
        ck += localShapes(0, 13) * 43 + localShapes(1, 13) * 47;

        // FAST PATH, route 2: an instance field the whole program only stores new ArrayList into.
        Holder h = new Holder(64);
        System.out.println("field plain=" + h.plain() + " break=" + h.withBreak(13)
            + " cont=" + h.withContinue() + " ret=" + h.withReturn(21)
            + " nest=" + h.nested() + " try=" + h.withTry()
            + " seq=" + h.sequential() + " reas=" + h.reassigns());
        ck += h.plain() * 53 + h.withBreak(13) * 59 + h.withContinue() * 61
            + h.withReturn(21) * 67 + h.nested() * 71 + h.withTry() * 73
            + h.sequential() * 79 + h.reassigns() * 83;
        Holder he = new Holder(0);
        System.out.println("field empty plain=" + he.plain() + " nest=" + he.nested());
        ck += he.plain() * 89 + he.nested() * 97;

        System.out.println("mutation=" + mutationChecks());
        System.out.println("sets=" + setChecks());
        System.out.println("checksum=" + ck);
    }
}
