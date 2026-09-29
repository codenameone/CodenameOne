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

/**
 * Bounds-check elimination where the array's length is parked in a local.
 *
 * Nine tenths of this corpus' counted loops compile to
 * {@code ALOAD a ; ARRAYLENGTH ; ISTORE n} followed by a loop testing
 * {@code i < n}, and the pass used to recognize only the form that re-reads the
 * length at the test. Recognizing the hoisted form means proving that n IS a.length
 * at the loop, which is a claim about two slots rather than one, and the shapes that
 * break it are all here:
 *
 *   basic                  the proven case: capture, then loop
 *   reassignAfterCapture   a is replaced AFTER n was taken -- must still throw
 *   reassignShorterLater   the same, with the loop reading past the new end
 *   rewriteBeforeCapture   a is replaced BEFORE the capture, every round. LEGAL:
 *                          re-running the write re-runs the capture with it
 *   twoArraysOneLength     n bounds a, and b -- shorter -- is indexed by the same i
 *   nReassigned            n is written twice, so it is not a.length any more
 *   storeShapes            a[i] = <expr>, where the value sits between index and store
 *   wideStores             long[] and double[] stores, whose value is two slots
 *   objectStores           AASTORE, which also carries the covariance rule
 *   nestedHoist            an inner hoisted loop inside an outer one
 *
 * The AIOOBE cases are the point. A wrong proof here does not throw and does not
 * crash -- it reads or writes past the end of a heap object, which is the worst
 * failure this VM has, so every one of them is checked against the JDK's answer.
 */
public class BceHoisted {
    private static int[] fill(int n, int seed) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = (seed + i) * 7 + (i & 3);
        }
        return a;
    }

    private static long basic(int size, int seed) {
        int[] a = fill(size, seed);
        int n = a.length;
        long s = 0;
        for (int i = 0; i < n; i++) {
            s += a[i];
        }
        return s;
    }

    /** a is replaced after n was captured: n is no longer a.length. */
    private static long reassignAfterCapture(int size, int seed) {
        int[] a = fill(size, seed);
        int n = a.length;
        a = fill(size / 2, seed + 1);
        long s = 0;
        int caught = 0;
        try {
            for (int i = 0; i < n; i++) {
                s += a[i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            caught = 1;
        }
        return s * 4 + caught;
    }

    /** The same hazard reached through a branch the compiler cannot fold. */
    private static long reassignShorterLater(int size, int seed, boolean shrink) {
        int[] a = fill(size, seed);
        int n = a.length;
        if (shrink) {
            a = fill(1, seed + 2);
        }
        long s = 0;
        int caught = 0;
        try {
            for (int i = 0; i < n; i++) {
                s += a[i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            caught = 1;
        }
        return s * 4 + caught;
    }

    /** Legal: the write is BEFORE the capture, so re-running it re-runs the capture. */
    private static long rewriteBeforeCapture(int size, int seed) {
        int[] a = null;
        long s = 0;
        for (int round = 0; round < 3; round++) {
            a = fill(size + round, seed + round);
            int n = a.length;
            for (int i = 0; i < n; i++) {
                a[i] = a[i] ^ (i * 5);
                s += a[i];
            }
        }
        return s;
    }

    /** n bounds a; b is half as long and indexed by the same i. */
    private static long twoArraysOneLength(int size, int seed) {
        int[] a = fill(size, seed);
        int[] b = fill(size / 2, seed + 3);
        int n = a.length;
        long s = 0;
        int caught = 0;
        try {
            for (int i = 0; i < n; i++) {
                s += a[i] + b[i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            caught = 1;
        }
        return s * 4 + caught;
    }

    /** n is written twice, so the capture does not describe it at the loop. */
    private static long nReassigned(int size, int seed, boolean widen) {
        int[] a = fill(size, seed);
        int n = a.length;
        if (widen) {
            n = size * 2;
        }
        long s = 0;
        int caught = 0;
        try {
            for (int i = 0; i < n; i++) {
                s += a[i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            caught = 1;
        }
        return s * 4 + caught;
    }

    private static int helper(int v) {
        return v * 3 + 1;
    }

    /** Stores whose value expression sits between the index and the store. */
    private static long storeShapes(int size, int seed) {
        int[] a = fill(size, seed);
        int n = a.length;
        for (int i = 0; i < n; i++) {
            a[i] = 0;
        }
        for (int i = 0; i < n; i++) {
            a[i] = i * 2 + (seed & 7);
        }
        for (int i = 0; i < n; i++) {
            a[i] = helper(i + seed);
        }
        long s = 0;
        for (int i = 0; i < n; i++) {
            s += a[i];
        }
        return s;
    }

    /** Two-slot values: the walk back from the store has to count them as two. */
    private static long wideStores(int size, int seed) {
        long[] la = new long[size];
        double[] da = new double[size];
        int n = la.length;
        for (int i = 0; i < n; i++) {
            la[i] = (long) i * 1000000007L + seed;
        }
        int m = da.length;
        for (int i = 0; i < m; i++) {
            da[i] = i * 0.5 + seed;
        }
        long s = 0;
        for (int i = 0; i < n; i++) {
            s += la[i];
        }
        for (int i = 0; i < m; i++) {
            s += (long) da[i];
        }
        return s;
    }

    private static long objectStores(int size, int seed) {
        String[] a = new String[size];
        int n = a.length;
        for (int i = 0; i < n; i++) {
            a[i] = "v" + (i + seed);
        }
        long s = 0;
        for (int i = 0; i < n; i++) {
            s += a[i].length();
        }
        return s;
    }

    private static long nestedHoist(int size, int seed) {
        int[] a = fill(size, seed);
        int[] b = fill(size, seed + 4);
        int n = a.length;
        long s = 0;
        for (int i = 0; i < n; i++) {
            int m = b.length;
            for (int k = 0; k < m; k++) {
                s += a[i] ^ b[k];
            }
        }
        return s;
    }

    public static void main(String[] args) {
        StringBuilder out = new StringBuilder();
        for (int size = 2; size <= 64; size *= 2) {
            out.append("size=").append(size).append('\n');
            out.append("  basic                 ").append(basic(size, size)).append('\n');
            out.append("  reassignAfterCapture  ").append(reassignAfterCapture(size, size + 1)).append('\n');
            out.append("  reassignShorterLaterF ").append(reassignShorterLater(size, size + 2, false)).append('\n');
            out.append("  reassignShorterLaterT ").append(reassignShorterLater(size, size + 2, true)).append('\n');
            out.append("  rewriteBeforeCapture  ").append(rewriteBeforeCapture(size, size + 3)).append('\n');
            out.append("  twoArraysOneLength    ").append(twoArraysOneLength(size, size + 4)).append('\n');
            out.append("  nReassignedF          ").append(nReassigned(size, size + 5, false)).append('\n');
            out.append("  nReassignedT          ").append(nReassigned(size, size + 5, true)).append('\n');
            out.append("  storeShapes           ").append(storeShapes(size, size + 6)).append('\n');
            out.append("  wideStores            ").append(wideStores(size, size + 7)).append('\n');
            out.append("  objectStores          ").append(objectStores(size, size + 8)).append('\n');
            out.append("  nestedHoist           ").append(nestedHoist(size, size + 9)).append('\n');
        }
        // Hot, so a wrong proof has room to run off a page rather than into a
        // neighbouring live object, and so the loops are worth compiling well.
        long acc = 0;
        for (int rep = 0; rep < 300; rep++) {
            acc += basic(257, rep);
            acc += storeShapes(257, rep);
            acc += rewriteBeforeCapture(129, rep);
            acc += twoArraysOneLength(257, rep);
            acc += wideStores(129, rep);
        }
        out.append("hot=").append(acc).append('\n');
        System.out.print(out);
    }
}
