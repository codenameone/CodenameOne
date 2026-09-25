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
import java.util.HashMap;

/**
 * Exact per-shape memory representation probe.
 *
 * <p>This is deliberately NOT a benchmark. It measures no time at all. It retains
 * exactly {@code n} instances of one object shape and then stops, so the process
 * peak physical footprint is a straight line in {@code n} whose SLOPE is the deep
 * retained cost of one instance. Two runs at {@code n} and {@code 2n} give that
 * slope without a baseline subtraction, which cancels VM startup, arena slack and
 * every other fixed term -- on ParparVM and on HotSpot alike, from the same source
 * and the same driver. See memshape.sh, which does the arithmetic.
 *
 * <p>Why a slope and not a heap report: the two VMs have no common live-bytes API.
 * ParparVM's Runtime.freeMemory reports the process footprint against physical RAM,
 * not the heap, and HotSpot's reports a heap sized with GC slack. Process footprint
 * is the one quantity both report identically, and it is also the quantity the
 * 1.59x-of-HotSpot memory gap is stated in, so it is the number worth closing.
 *
 * <p>Shapes that take elements draw them from a SHARED pool, so what the slope
 * measures is the container's own storage and never the payload's. The pool is
 * built once and retained, so it contributes a fixed term the slope removes.
 *
 * <p>Allocation is kept garbage-free on purpose: a shape that churned temporaries
 * would raise the high-water mark by an amount that depends on the collector rather
 * than on the representation, and the slope would stop meaning what it claims.
 */
public class MemShape {
    private static Object[] hold;
    private static Object[] pool;
    private static String[] keyPool;

    private static final int POOL = 64;

    private static void buildPool() {
        pool = new Object[POOL];
        keyPool = new String[POOL];
        for (int i = 0; i < POOL; i++) {
            pool[i] = new Object();
            keyPool[i] = "k" + i;
        }
    }

    // ONE scratch buffer, refilled in place and never re-allocated. new String(char[])
    // copies, so reuse is safe -- and it matters: a fresh char[] per iteration would be
    // transient garbage, and transient garbage raises the footprint high-water mark by
    // an amount that depends on when each collector happens to run rather than on how
    // either VM represents a String. That is the difference between measuring a
    // representation and measuring a collector, and this file only claims the former.
    private static char[] scratch;

    private static char[] asciiChars(int len, int seed) {
        if (scratch == null || scratch.length != len) {
            scratch = new char[len];
        }
        for (int i = 0; i < len; i++) {
            scratch[i] = (char) ('a' + ((seed + i) & 15));
        }
        return scratch;
    }

    public static void main(String[] args) {
        String shape = args.length > 0 ? args[0] : "obj";
        int n = args.length > 1 ? Integer.parseInt(args[1]) : 1000000;
        buildPool();
        hold = new Object[n];
        long ck = 0;

        if (shape.equals("obj")) {
            for (int i = 0; i < n; i++) {
                hold[i] = new Object();
            }
            ck = n;
        } else if (shape.equals("strA8") || shape.equals("strA32") || shape.equals("strA128")) {
            int len = shape.equals("strA8") ? 8 : (shape.equals("strA32") ? 32 : 128);
            for (int i = 0; i < n; i++) {
                String s = new String(asciiChars(len, i));
                hold[i] = s;
                ck += s.length();
            }
        } else if (shape.equals("strCat8") || shape.equals("strCat32")) {
            // A FUSED string, which is what the real corpus mostly holds and what
            // new String(char[]) is NOT. @Fused packs the value array inside the
            // String's own block only when the constructor sees an inline NEWARRAY
            // it can pack; String(char[]) gets its byte[] back from a call to
            // toLatin1, so nothing is packed and the string costs two separate
            // allocations. Concatenation goes through cn1FusedConcatN, which builds
            // the whole result in ONE block -- the shape the census counts at
            // 923,004 Strings against only 89,708 standalone byte[].
            String tail = shape.equals("strCat8") ? "" : "0123456789abcdefghijklmnopq";
            for (int i = 0; i < n; i++) {
                String s2 = "s" + (1000000 + i) + tail;
                hold[i] = s2;
                ck += s2.length();
            }
        } else if (shape.equals("strU8")) {
            for (int i = 0; i < n; i++) {
                char[] c = asciiChars(8, i);
                c[0] = (char) (0x400 + (i & 255));
                String s = new String(c);
                hold[i] = s;
                ck += s.length();
            }
            scratch = null;
        } else if (shape.equals("boxInt")) {
            for (int i = 0; i < n; i++) {
                Integer v = Integer.valueOf(100000 + i);
                hold[i] = v;
                ck += v.intValue();
            }
        } else if (shape.equals("boxLong")) {
            for (int i = 0; i < n; i++) {
                Long v = Long.valueOf(100000L + i);
                hold[i] = v;
                ck += v.longValue();
            }
        } else if (shape.equals("objArr0") || shape.equals("objArr8") || shape.equals("objArr32")) {
            int len = shape.equals("objArr0") ? 0 : (shape.equals("objArr8") ? 8 : 32);
            for (int i = 0; i < n; i++) {
                Object[] a = new Object[len];
                for (int j = 0; j < len; j++) {
                    a[j] = pool[j & (POOL - 1)];
                }
                hold[i] = a;
                ck += a.length;
            }
        } else if (shape.equals("intArr8") || shape.equals("intArr32")) {
            int len = shape.equals("intArr8") ? 8 : 32;
            for (int i = 0; i < n; i++) {
                int[] a = new int[len];
                a[0] = i;
                hold[i] = a;
                ck += a.length;
            }
        } else if (shape.equals("byteArr8") || shape.equals("byteArr32")) {
            int len = shape.equals("byteArr8") ? 8 : 32;
            for (int i = 0; i < n; i++) {
                byte[] a = new byte[len];
                a[0] = (byte) i;
                hold[i] = a;
                ck += a.length;
            }
        } else if (shape.equals("charArr8")) {
            for (int i = 0; i < n; i++) {
                char[] a = new char[8];
                a[0] = (char) i;
                hold[i] = a;
                ck += a.length;
            }
        } else if (shape.equals("alist0") || shape.equals("alist8") || shape.equals("alist32")) {
            int len = shape.equals("alist0") ? 0 : (shape.equals("alist8") ? 8 : 32);
            for (int i = 0; i < n; i++) {
                ArrayList<Object> a = new ArrayList<Object>();
                for (int j = 0; j < len; j++) {
                    a.add(pool[j & (POOL - 1)]);
                }
                hold[i] = a;
                ck += a.size();
            }
        } else if (shape.equals("hmap0") || shape.equals("hmap8")) {
            int len = shape.equals("hmap0") ? 0 : 8;
            for (int i = 0; i < n; i++) {
                HashMap<String, Object> m = new HashMap<String, Object>();
                for (int j = 0; j < len; j++) {
                    m.put(keyPool[j & (POOL - 1)], pool[j & (POOL - 1)]);
                }
                hold[i] = m;
                ck += m.size();
            }
        } else if (shape.equals("sbNarrow32") || shape.equals("sbWide32")) {
            // Does the compact StringBuilder representation actually pay? A narrow
            // builder stores one byte per code unit and a wide one stores two, so
            // these two shapes differ ONLY in whether a single character above 0xFF
            // was ever appended. If they measure the same, the `wide` flag is not
            // reaching the storage and the compact path is decorative.
            boolean goWide = shape.equals("sbWide32");
            for (int i = 0; i < n; i++) {
                StringBuilder b = new StringBuilder();
                for (int j = 0; j < 32; j++) {
                    b.append((char) ('a' + ((i + j) & 15)));
                }
                if (goWide) {
                    b.setCharAt(0, '\u4e2d');
                }
                hold[i] = b;
                ck += b.length();
            }
        } else if (shape.equals("sb0")) {
            for (int i = 0; i < n; i++) {
                StringBuilder b = new StringBuilder();
                hold[i] = b;
                ck += b.length();
            }
        } else {
            System.out.println("UNKNOWN SHAPE " + shape);
            return;
        }

        // Touch the retained set so nothing above can be proven dead, and so every
        // page is resident when the footprint is sampled.
        for (int i = 0; i < n; i += 4096) {
            if (hold[i] == null) {
                ck++;
            }
        }
        System.out.println("SHAPE=" + shape + " N=" + n + " CK=" + ck);
    }
}
