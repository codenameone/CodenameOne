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
 * Heap integrity ACROSS the fused string-concat natives.
 *
 * String.cn1ConcatN (cn1FusedConcat2..5) reads raw interior pointers into its
 * source Strings' byte[]s and then ALLOCATES, which can collect. If the sources
 * were not rooted across that allocation, the collector could reclaim the very
 * bytes the copy reads and hand the block to another allocation -- and the
 * damage would surface somewhere else entirely, as an unrelated array whose
 * header has been overwritten.
 *
 * So this does not check the strings. It checks EVERYTHING ELSE: a population of
 * int[] with known lengths and known contents is held live across heavy concat
 * traffic, and every element is re-verified each round. A wrong length or a
 * wrong element is the corruption, reported at the round that produced it.
 *
 * THE HAZARD DOES NOT EXIST IN ANY CONFIGURATION THIS VM BUILDS, and that is
 * MEASURED, not assumed. The sources are rooted twice over: as C parameters of
 * the native they are covered by the conservative scan of that frame, and with
 * conservative roots compiled out they are still in the caller's operand-stack
 * slots, which are scanned precisely. Both arms were run --
 *
 *     default (conservative roots)                      corrupt=0
 *     -DCN1_DISABLE_CONSERVATIVE_GC_ROOTS, with the
 *     translator at -Dcn1.frameless.objects=false
 *                  -Dcn1.frameless.instance=false       corrupt=0
 *
 * -- and both match HotSpot byte-for-byte. A GC bracket was once added to these
 * natives on the strength of the hazard and then withdrawn because the premise
 * was wrong; this is the measurement that says so, rather than the argument.
 *
 * SO BE HONEST ABOUT WHAT A GREEN RUN MEANS. Its NON-VACUITY IS UNPROVEN: there
 * is no arm in which it has been seen to fail, so it cannot be cited as proof
 * that the natives root their own arguments. What it is worth is two things a
 * checksum cannot give. It is a regression guard on the rooting property, which
 * would break if cn1FusedConcatN were rewritten to stop keeping its sources live
 * across the allocation. And it is a genuine heap-integrity torture in its own
 * right -- 256 int[] held live and fully re-verified across 400,000
 * concatenations -- which catches any corruption under that allocation shape,
 * whatever its cause. Read a pass as the second of those, not the first.
 *
 * Output must match HotSpot byte-for-byte.
 */
public class ConcatCorrupt {
    static final int ARRAYS = 256;
    static final int LEN = 1000;

    static int[][] live = new int[ARRAYS][];
    static String[] keep = new String[64];

    static void fill() {
        for (int i = 0; i < ARRAYS; i++) {
            live[i] = new int[LEN];
            for (int j = 0; j < LEN; j++) {
                live[i][j] = (i * 31) + j;
            }
        }
    }

    /** returns the number of corrupted observations */
    static int verify(int round) {
        int bad = 0;
        for (int i = 0; i < ARRAYS; i++) {
            int[] a = live[i];
            if (a.length != LEN) {
                System.out.println("CORRUPT round=" + round + " array=" + i
                        + " length=" + a.length + " expected=" + LEN);
                bad++;
                continue;
            }
            for (int j = 0; j < LEN; j++) {
                if (a[j] != (i * 31) + j) {
                    System.out.println("CORRUPT round=" + round + " array=" + i
                            + " index=" + j + " value=" + a[j]
                            + " expected=" + ((i * 31) + j));
                    bad++;
                    break;
                }
            }
        }
        return bad;
    }

    public static void main(String[] args) {
        long ck = 0;
        int bad = 0;
        fill();
        // Operands are built at runtime so javac cannot constant-fold the
        // concatenations away; each is a Latin-1 String, which is what routes the
        // expression through the fused path rather than the two-object fallback.
        for (int round = 0; round < 200; round++) {
            String p = "p" + round;
            String q = "q" + (round * 7);
            String r = "r" + (round * 13);
            String s = "s" + (round * 17);
            for (int i = 0; i < 2000; i++) {
                String two = p + q;
                String three = p + q + r;
                String four = p + q + r + s;
                String five = p + q + r + s + two;
                ck += two.length() + three.length() + four.length() + five.length();
                keep[i & 63] = five;
                if ((i & 255) == 0) {
                    ck += keep[i & 63].charAt(0);
                }
            }
            bad += verify(round);
            if (bad > 0) {
                break;
            }
        }
        System.out.println("checksum=" + ck);
        System.out.println("corrupt=" + bad);
    }
}
