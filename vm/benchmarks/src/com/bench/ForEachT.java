package com.bench;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Semantics gate for for-each loop specialization.
 *
 * The planned optimization emits a for-each body TWICE -- a clamp-free indexed loop
 * over ArrayList's backing array when the receiver's class matches, and the existing
 * iterator path otherwise. Everything here must stay byte-identical to the host JVM
 * through that change. The shapes are chosen to be the ones a loop duplicator gets
 * wrong: control flow that leaves the body, control flow that re-enters it, nesting,
 * and receivers that must NOT take the fast path.
 *
 * DELIBERATELY NOT COVERED HERE: mutation during iteration. The specialization drops
 * the modCount check by design, so a mutating loop yields stale elements instead of
 * ConcurrentModificationException and CANNOT be byte-identical. That deviation has its
 * own reproducer; a torture in run-gauntlet.sh has to match the host exactly.
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
        System.out.println("checksum=" + ck);
    }
}
