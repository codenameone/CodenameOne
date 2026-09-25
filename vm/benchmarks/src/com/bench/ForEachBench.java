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
import java.util.List;

/**
 * Iteration-dominated driver for the for-each work. Deliberately narrow: a
 * whole-program ratio cannot resolve a change to one loop shape, so this makes the
 * loop the overwhelming majority of the program.
 *
 * MEASURE THIS WITH ThinLTO OR THE NUMBER IS ABOUT THE HARNESS, NOT THE VM:
 *
 *     CN1_BENCH_CFLAGS=-flto=thin ./translate-and-build.sh ForEachBench /tmp/fe
 *
 * translate-and-build.sh links without LTO by default, and the indexed loop's three
 * ArrayList field reads (size, array, firstIndex) plus Integer.intValue() are defined
 * in OTHER translation units -- so without it they stay four real calls per element and
 * the loop measures call overhead. Measured on this shape: 19ms without LTO, 4-6ms with
 * it, against 15ms for JDK 25. iOS ships ThinLTO (LLVM_LTO = YES_THIN in both Xcode
 * templates, -flto=thin on the CMake Release targets), so the LTO number is the shipping
 * one and the default-build number is not.
 *
 * The 12 inner reps are min-of-N over the SAME list, which invites a compiler to compute
 * the sum once and reuse it. It does not happen here and the check is cheap to redo:
 * double the outer pass count and the time must double (measured 4-6ms -> 10ms). If it
 * does not, the loop was hoisted and the figure is worthless.
 */
public class ForEachBench {
    public static void main(String[] args) {
        List<Integer> l = new ArrayList<Integer>();
        for (int i = 0; i < 20000; i++) {
            l.add(Integer.valueOf(i));
        }
        long best = Long.MAX_VALUE;
        for (int rep = 0; rep < 12; rep++) {
            long t0 = System.nanoTime();
            long sum = 0;
            for (int outer = 0; outer < 400; outer++) {
                for (Integer v : l) {
                    sum += v.intValue();
                }
            }
            long dt = System.nanoTime() - t0;
            if (dt < best) {
                best = dt;
            }
            if (sum != 400L * 19999L * 20000L / 2L) {
                System.out.println("BAD " + sum);
                return;
            }
        }
        System.out.println("bestMs=" + (best / 1000000L));
    }
}
