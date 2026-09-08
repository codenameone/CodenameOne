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

import java.util.ArrayList;

/**
 * Diagnostic, not a torture. Three jobs:
 *
 * 1. Report which tag code each boxed type actually got, so a change to the encoding is
 *    provable rather than assumed. System.identityHashCode returns the low 32 bits of the
 *    reference on this VM, which for a tagged value IS the encoded word.
 * 2. ASSERT that every real object is 8-byte aligned. The tag occupies the low three bits,
 *    so an object at a 4-byte address would be misread as an immediate. The invariant is
 *    already load-bearing -- cn1ConservativeResolve rejects the same three bits -- but it
 *    had never been checked against the allocator, and it has to hold on every path: the
 *    BiBOP bump, the legacy calloc above CN1_BIBOP_MAX_OBJECT, arrays, fused
 *    String/StringBuilder, and the class objects themselves.
 * 3. Report the COVERAGE of the two partial encodings. Long and Double cannot represent
 *    every value in 61 bits, and a partial scheme that looks excellent on a benchmark and
 *    never fires on real data is the failure mode worth guarding against.
 *
 * Deliberately NOT in the gauntlet: the output is target specific and is not bit-identical
 * to a host JVM. It self-checks and exits non-zero instead.
 */
public final class TagProbe {
    static int failures;

    // Over CN1_BIBOP_MAX_OBJECT (512 bytes), so this takes the legacy calloc path.
    static final class Big {
        long a0,a1,a2,a3,a4,a5,a6,a7,a8,a9,b0,b1,b2,b3,b4,b5,b6,b7,b8,b9;
        long c0,c1,c2,c3,c4,c5,c6,c7,c8,c9,d0,d1,d2,d3,d4,d5,d6,d7,d8,d9;
        long e0,e1,e2,e3,e4,e5,e6,e7,e8,e9,f0,f1,f2,f3,f4,f5,f6,f7,f8,f9;
        long g0,g1,g2,g3,g4,g5,g6,g7,g8,g9,h0,h1,h2,h3,h4,h5,h6,h7,h8,h9;
    }

    static final class Small { int a; Object b; }

    /**
     * The low bits of a HEAP reference's identity hash, which is a truncated address here.
     * Only valid for real objects: identityHashCode folds a tagged word's two halves, so a
     * tagged value's low bits are no longer its tag code.
     */
    static int tagOf(Object o) { return System.identityHashCode(o) & 7; }

    /**
     * Whether valueOf returned an IMMEDIATE, tested by its observable consequence rather
     * than by reading pointer bits: two separately-obtained boxes of one value are the same
     * reference only if that value is an immediate. The callers below all pass a value
     * outside every -128..127 wrapper cache, so a shared cache entry cannot fake it.
     */
    static boolean immediate(Object a, Object b) { return a == b; }

    static void mustBeHeap(String what, Object o) {
        if (tagOf(o) != 0) {
            System.out.println("MISALIGNED " + what + " lowBits=" + tagOf(o));
            failures++;
        }
    }

    static void check(String what, boolean ok) {
        if (!ok) { System.out.println("FAILED " + what); failures++; }
    }

    public static void main(String[] a) {
        // --- which code did each type get, and does the value survive the round trip ---
        // Values outside every -128..127 cache, so "same reference twice" means immediate.
        System.out.println("Integer   immediate=" + immediate(Integer.valueOf(1000), Integer.valueOf(1000))
                + " roundTrip=" + (Integer.valueOf(-7).intValue() == -7));
        System.out.println("Long      immediate=" + immediate(Long.valueOf(1000L), Long.valueOf(1000L))
                + " roundTrip=" + (Long.valueOf(-7L).longValue() == -7L));
        System.out.println("Double    immediate=" + immediate(Double.valueOf(1.5), Double.valueOf(1.5))
                + " roundTrip=" + (Double.valueOf(-1.5).doubleValue() == -1.5));
        System.out.println("Float     immediate=" + immediate(Float.valueOf(1.5f), Float.valueOf(1.5f))
                + " roundTrip=" + (Float.valueOf(-1.5f).floatValue() == -1.5f));
        System.out.println("Character immediate=" + immediate(Character.valueOf((char) 1000), Character.valueOf((char) 1000))
                + " roundTrip=" + (Character.valueOf(Character.MAX_VALUE).charValue() == Character.MAX_VALUE));
        System.out.println("Short     immediate=" + immediate(Short.valueOf((short) 1000), Short.valueOf((short) 1000))
                + " roundTrip=" + (Short.valueOf((short) -32768).shortValue() == -32768));

        // Constructors must still produce real, distinct heap objects -- `new` guarantees
        // identity, and only the valueOf factories are allowed to return an immediate.
        Long n1 = new Long(42L), n2 = new Long(42L);
        mustBeHeap("new Long", n1);
        mustBeHeap("new Double", new Double(1.5));
        mustBeHeap("new Float", new Float(1.5f));
        mustBeHeap("new Character", new Character('q'));
        mustBeHeap("new Short", new Short((short) 42));
        mustBeHeap("new Integer", new Integer(42));
        check("new Long identity distinct", n1 != n2);
        check("new Long equals tagged", n1.equals(Long.valueOf(42L)));
        check("tagged equals new Long", Long.valueOf(42L).equals(n1));

        // --- alignment, over every allocation shape, many times ---
        for (int i = 0; i < 20000; i++) {
            mustBeHeap("Small", new Small());
            mustBeHeap("Big", new Big());
            mustBeHeap("Object", new Object());
            mustBeHeap("byte[7]", new byte[7]);
            mustBeHeap("byte[1000]", new byte[1000]);
            mustBeHeap("int[3]", new int[3]);
            mustBeHeap("Object[5]", new Object[5]);
            mustBeHeap("String", new String(new char[] { 'a', (char) i }));
            mustBeHeap("StringBuilder", new StringBuilder("x").append(i));
            mustBeHeap("ArrayList", new ArrayList());
            mustBeHeap("clazz(Small)", new Small().getClass());
            mustBeHeap("clazz(int[])", new int[1].getClass());
            mustBeHeap("interned", ("lit" + (i & 3)).intern());
        }

        // --- identity hashes of tagged values must actually spread ---
        // The Double encoding carries the raw IEEE pattern, whose distinguishing bits for
        // 1.0, 2.0, 3.0 ... all live in the HIGH word. Truncating the tagged word to an int
        // gave every integral double the same identity hash, which turns IdentityHashMap's
        // linear probe quadratic. identityHashCode folds both halves for tagged values now;
        // this is what proves it, and it fails loudly if the fold is ever removed.
        int n = 4096;
        java.util.HashSet<Integer> dh = new java.util.HashSet<Integer>();
        java.util.HashSet<Integer> lh = new java.util.HashSet<Integer>();
        for (int i = 0; i < n; i++) {
            dh.add(Integer.valueOf(System.identityHashCode(Double.valueOf(i))));
            lh.add(Integer.valueOf(System.identityHashCode(Long.valueOf(i))));
        }
        System.out.println("distinctIdentityHash double=" + dh.size() + "/" + n
                + " long=" + lh.size() + "/" + n);
        // Allow some collision, but nothing like the single bucket this used to produce.
        check("integral doubles spread across identity hashes", dh.size() > n * 3 / 4);
        check("longs spread across identity hashes", lh.size() > n * 3 / 4);

        // --- the taggable boundary, exactly ---
        // This is the one place an off-by-one CORRUPTS rather than merely misses: a value
        // wrongly judged representable is truncated into the payload and read back wrong.
        // The payload is bits 3..63 read as signed, so the range is [-2^60, 2^60).
        long[] edges = {
            (1L << 60) - 1, 1L << 60, (1L << 60) + 1,
            -(1L << 60), -(1L << 60) - 1, -(1L << 60) + 1,
            Long.MAX_VALUE, Long.MIN_VALUE, 0L, -1L
        };
        for (int i = 0; i < edges.length; i++) {
            Long boxed = Long.valueOf(edges[i]);
            boolean isImm = immediate(Long.valueOf(edges[i]), Long.valueOf(edges[i]));
            boolean inRange = edges[i] >= -(1L << 60) && edges[i] < (1L << 60);
            // Whatever the encoding decides, the value must survive. A tagged value outside
            // the range would fail here, which is the failure that matters.
            check("long roundTrip " + edges[i], boxed.longValue() == edges[i]);
            check("long equals " + edges[i], boxed.equals(new Long(edges[i])));
            if (isImm && !inRange) {
                System.out.println("OVERTAGGED " + edges[i] + " was tagged but is out of range");
                failures++;
            }
            if (!isImm && inRange) {
                System.out.println("UNDERTAGGED " + edges[i] + " is in range but was not tagged");
                failures++;
            }
        }

        // Doubles whose low mantissa bits are set must not be tagged, and must round-trip.
        double[] dedges = { 0.1, 3.14159265358979, Double.MIN_VALUE, Double.MAX_VALUE,
                            -0.0, 0.0, 1.0, 0.5, Double.NaN,
                            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };
        for (int i = 0; i < dedges.length; i++) {
            Double boxed = Double.valueOf(dedges[i]);
            // doubleToLongBits, not RawLongBits: vm/JavaAPI's Double declares no raw
            // variant (the C native exists but nothing in Java reaches it). The only
            // difference is NaN canonicalization, and every NaN constructible here is
            // already the canonical one.
            boolean bitsClear = (Double.doubleToLongBits(dedges[i]) & 7L) == 0;
            check("double roundTrip " + i,
                  Double.doubleToLongBits(boxed.doubleValue())
                      == Double.doubleToLongBits(dedges[i]));
            if (immediate(Double.valueOf(dedges[i]), Double.valueOf(dedges[i])) && !bitsClear) {
                System.out.println("OVERTAGGED double index " + i);
                failures++;
            }
        }

        // --- coverage of the two partial encodings ---
        int longTagged = 0, longTotal = 0;
        long seed = 12345;
        for (int i = 0; i < 100000; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            // A spread of realistic magnitudes rather than uniform 64-bit noise.
            long v;
            switch (i & 3) {
                case 0: v = i; break;                          // small counter
                case 1: v = 1757000000000L + i; break;         // epoch millis
                case 2: v = seed >> 20; break;                 // 44-bit-ish
                default: v = seed; break;                      // full 64-bit noise
            }
            longTotal++;
            if (immediate(Long.valueOf(v), Long.valueOf(v))) longTagged++;
        }
        System.out.println("longTaggedPct=" + (longTagged * 100 / longTotal));

        int dblTagged = 0, dblTotal = 0;
        for (int i = 0; i < 100000; i++) {
            double d;
            switch (i & 3) {
                case 0: d = i; break;                 // JSON integer
                case 1: d = i / 2.0; break;           // exact halves
                case 2: d = i / 100.0; break;         // money, mostly inexact
                default: d = Math.sqrt(i); break;     // arbitrary
            }
            dblTotal++;
            if (immediate(Double.valueOf(d), Double.valueOf(d))) dblTagged++;
        }
        System.out.println("doubleTaggedPct=" + (dblTagged * 100 / dblTotal));

        System.out.println("failures=" + failures);
        System.out.println(failures == 0 ? "TAGPROBE GREEN" : "TAGPROBE RED");
        if (failures != 0) {
            System.exit(1);
        }
    }
}
