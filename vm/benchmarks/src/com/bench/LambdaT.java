/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
import java.util.List;

/**
 * Lambda semantics gate. A lambda that captures nothing has no instance fields, so the
 * translator hands back a shared instance instead of allocating one per evaluation. That
 * must be invisible: same results, same ordering, same behaviour under repetition.
 *
 * DELIBERATELY NOT TESTED: reference identity between two evaluations. The JLS does not
 * guarantee a lambda expression produces a new object -- the JDK's own LambdaMetafactory
 * caches non-capturing instances -- so `a == b` is unspecified on BOTH sides and a torture
 * that asserted it would be testing the host, not this VM.
 *
 * Capturing lambdas ARE still allocated per evaluation and must keep their captures
 * distinct; the counted loops below would collapse to one value if a capture leaked
 * between instances.
 */
public class LambdaT {
    interface IntOp { int apply(int v); }
    interface Sink { void take(int v); }

    static int[] total = new int[1];

    static int runOp(IntOp op, int v) { return op.apply(v); }

    static long nonCapturing(int rounds) {
        long acc = 0;
        for (int i = 0; i < rounds; i++) {
            // same lambda expression evaluated repeatedly -- one shared instance now
            acc += runOp(v -> v * 3 + 1, i);
            acc += runOp(v -> v ^ 0x5a5a, i);
        }
        return acc;
    }

    static long capturing(int rounds) {
        long acc = 0;
        for (int i = 0; i < rounds; i++) {
            final int base = i * 7;
            // captures `base`: every instance must see ITS OWN capture
            acc += runOp(v -> v + base, 1);
        }
        return acc;
    }

    static long mixedInLoop(List<Integer> data) {
        long acc = 0;
        for (int round = 0; round < 3; round++) {
            final int mul = round + 2;
            for (Integer d : data) {
                acc += runOp(v -> v * mul, d.intValue());     // capturing
                acc += runOp(v -> v - 1, d.intValue());       // non-capturing
            }
        }
        return acc;
    }

    static long sinks(int rounds) {
        total[0] = 0;
        for (int i = 0; i < rounds; i++) {
            Sink s = v -> total[0] += v;   // captures nothing (total is static)
            s.take(i);
        }
        return total[0];
    }

    public static void main(String[] args) {
        List<Integer> data = new ArrayList<Integer>();
        for (int i = 0; i < 64; i++) { data.add(Integer.valueOf(i)); }
        System.out.println("nonCapturing=" + nonCapturing(500));
        System.out.println("capturing=" + capturing(500));
        System.out.println("mixed=" + mixedInLoop(data));
        System.out.println("sinks=" + sinks(500));
        long ck = nonCapturing(97) * 3 + capturing(97) * 5 + mixedInLoop(data) * 7 + sinks(97) * 11;
        System.out.println("checksum=" + ck);
    }
}
