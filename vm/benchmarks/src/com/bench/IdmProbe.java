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

/**
 * Diagnostic, not a benchmark: reports what IdentityHashMap's indexing actually
 * does to REAL identity hash codes on whichever VM runs it.
 *
 * <p>A host-side simulation cannot answer this. ParparVM's
 * {@code System.identityHashCode} is a truncated object address, so the input
 * distribution is a property of the allocator -- BiBOP slot size, page layout,
 * allocation order -- and exists only on the target. This allocates the keys the
 * same way the workload does, harvests their real hashes, and replays several
 * indexing schemes over them.
 *
 * <p>Prints, per scheme: the fraction of distinct home slots (how much of the
 * table the hash can even reach), and the average probe length for a build and
 * for a random successful lookup.
 */
public final class IdmProbe {

    /** the shipped-before-2026 scheme: modulo a non-power-of-two, no scramble. */
    static int oldIndex(int h, int len) {
        return ((h & 0x7FFFFFFF) % (len / 2)) * 2;
    }

    /** power-of-two mask, JDK scramble (h * -254). */
    static int newIndex(int h, int len) {
        return ((h << 1) - (h << 8)) & (len - 1);
    }

    /** power-of-two mask, NO scramble -- isolates what the multiply is worth. */
    static int rawIndex(int h, int len) {
        return (h & 0x7FFFFFFF) & (len - 1) & ~1;
    }

    /** fold the high half down, then mask. */
    static int foldIndex(int h, int len) {
        h ^= (h >>> 16);
        return h & (len - 1) & ~1;
    }

    /** fold, then Fibonacci multiply, then fold again. */
    static int mixIndex(int h, int len) {
        h ^= (h >>> 16);
        h *= 0x9E3779B1;
        h ^= (h >>> 16);
        return h & (len - 1) & ~1;
    }

    /** Fibonacci multiply then fold (one fold). */
    static int fibIndex(int h, int len) {
        h *= 0x9E3779B1;
        h ^= (h >>> 16);
        return h & (len - 1) & ~1;
    }

    static int index(int h, int len, int scheme) {
        switch (scheme) {
            case 0: return oldIndex(h, len);
            case 1: return newIndex(h, len);
            case 2: return rawIndex(h, len);
            case 3: return foldIndex(h, len);
            case 4: return mixIndex(h, len);
            default: return fibIndex(h, len);
        }
    }

    /** Fills a table with the hashes; returns {buildProbes, distinctHomeSlots}. */
    static long[] build(int[] hashes, int len, int scheme, int[] table) {
        for (int i = 0; i < len; i++) {
            table[i] = 0;
        }
        boolean[] home = new boolean[len];
        long probes = 0;
        int mask = len - 1;
        for (int k = 0; k < hashes.length; k++) {
            int h = hashes[k];
            int start = index(h, len, scheme);
            home[start] = true;
            int i = start;
            int n = 1;
            while (table[i] != 0) {
                i = scheme == 0 ? (i + 2) % len : (i + 2) & mask;
                n++;
            }
            table[i] = h == 0 ? 1 : h;
            probes += n;
        }
        long distinct = 0;
        for (int i = 0; i < len; i++) {
            if (home[i]) {
                distinct++;
            }
        }
        return new long[] { probes, distinct };
    }

    static long lookupProbes(int[] hashes, int len, int scheme, int[] table) {
        long probes = 0;
        int mask = len - 1;
        int r = 7;
        for (int t = 0; t < 200000; t++) {
            r ^= r << 13;
            r ^= r >>> 17;
            r ^= r << 5;
            int h = hashes[(r >>> 1) % hashes.length];
            int want = h == 0 ? 1 : h;
            int i = index(h, len, scheme);
            int n = 1;
            while (table[i] != want && table[i] != 0) {
                i = scheme == 0 ? (i + 2) % len : (i + 2) & mask;
                n++;
            }
            probes += n;
        }
        return probes;
    }

    static void report(String name, int[] hashes, int len, int scheme) {
        int[] table = new int[len];
        long[] b = build(hashes, len, scheme, table);
        long look = lookupProbes(hashes, len, scheme, table);
        System.out.println(name
                + " len=" + len
                + " homeSlots=" + b[1] + "/" + (len / 2)
                + " buildProbes/key=" + (b[0] * 100 / hashes.length) / 100.0
                + " lookupProbes/key=" + (look * 100 / 200000) / 100.0);
    }

    public static void main(String[] args) {
        int n = 50000;
        Object[] keys = new Object[n];
        int[] hashes = new int[n];
        for (int i = 0; i < n; i++) {
            keys[i] = new Object();
        }
        for (int i = 0; i < n; i++) {
            hashes[i] = System.identityHashCode(keys[i]);
        }

        // What do the raw hashes even look like?
        int orAll = 0;
        int andAll = -1;
        for (int i = 0; i < n; i++) {
            orAll |= hashes[i];
            andAll &= hashes[i];
        }
        int alignZeros = 0;
        while (alignZeros < 31 && (orAll & (1 << alignZeros)) == 0) {
            alignZeros++;
        }
        System.out.println("identityHashCode: alwaysZeroLowBits=" + alignZeros
                + " (i.e. addresses are " + (1 << alignZeros) + "-byte aligned)");

        // Old sizing was (threshold*10000/7500)*2 with no rounding; new sizing
        // rounds that up to a power of two.
        int oldLen = (int) (((long) n * 10000) / 7500) * 2;
        int newLen = 8;
        while (newLen < oldLen) {
            newLen <<= 1;
        }
        int tightLen = newLen >> 1;

        System.out.println("-- at the OLD array size (" + oldLen + " cells) --");
        report("old   modulo, no scramble   ", hashes, oldLen, 0);
        System.out.println("-- at a power-of-two size matched to the old one (" + tightLen + " cells) --");
        report("jdk   h*-254                ", hashes, tightLen, 1);
        report("raw   no scramble           ", hashes, tightLen, 2);
        report("fold  h^=h>>>16             ", hashes, tightLen, 3);
        report("mix   fold,*phi,fold        ", hashes, tightLen, 4);
        report("fib   *phi,fold             ", hashes, tightLen, 5);
        System.out.println("-- at the next power of two up (" + newLen + " cells) --");
        report("fold  h^=h>>>16             ", hashes, newLen, 3);
        report("mix   fold,*phi,fold        ", hashes, newLen, 4);
    }

    private IdmProbe() {
    }
}
