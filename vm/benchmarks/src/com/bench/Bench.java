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

/** Standalone line-oriented runner for the shared common workloads. */
public final class Bench {
    private static final int WARMUP = 3;

    /* Reps per benchmark. Optional argv[0] raises it; the default is unchanged, so
     * every existing caller measures exactly what it always did.
     *
     * WHY IT IS ADJUSTABLE. objectAllocation's live set is tiny and a rep lasts
     * ~34ms, so only a handful of GC cycles fit in the measured window and whether
     * one lands inside it decides the number: 21.95ms to 50.56ms was measured for
     * provably identical work (same binary, same process, identical checksum).
     * Five reps is then too few for ANY statistic to settle -- min-of-5 is itself a
     * noisy order statistic, and four processes put the floor at 24.07 / 28.03 /
     * 21.95 / 22.14, a 27.7% spread in the floor alone. That is the whole reason
     * one A/B scored that row 0.861 and the next scored it 1.167.
     *
     * Twenty-five CONSECUTIVE reps reach a sustained allocator steady state, and
     * six processes then agreed to 0.80% (28.60-28.83ms). The depth has to be
     * consecutive: taking the min over more SEPARATE processes does not converge,
     * it just drifts downward, because a minimum over independent samples is
     * monotonically non-increasing. The steady-state figure is also the honest one
     * -- sustained allocation throughput is what the row reports, and the cold
     * window flatters it by ~25%. */
    private static final int DEFAULT_MEASURE = 5;
    private static int measure = DEFAULT_MEASURE;

    private interface BenchFn {
        long run();
    }

    /* Optional argv[1]: run only this benchmark. Lets a caller re-measure ONE
     * unsettled row at depth without paying the deeper rep count on all eleven. */
    private static String only = null;

    private static void runBench(String name, BenchFn fn) {
        if (only != null && !only.equals(name)) {
            return;
        }
        for (int warmup = 0; warmup < WARMUP; warmup++) {
            fn.run();
        }
        for (int repetition = 0; repetition < measure; repetition++) {
            long started = System.nanoTime();
            long checksum = fn.run();
            long elapsed = System.nanoTime() - started;
            System.out.println("BENCH " + name + " rep " + repetition
                    + " ns=" + elapsed + " checksum=" + checksum);
        }
    }

    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            measure = Integer.parseInt(args[0]);
        }
        if (args != null && args.length > 1) {
            only = args[1];
        }
        runBench("intArithmetic", new BenchFn() {
            public long run() { return CommonWorkloads.intArithmetic(); }
        });
        runBench("longArithmetic", new BenchFn() {
            public long run() { return CommonWorkloads.longArithmetic(); }
        });
        runBench("mathTranscendental", new BenchFn() {
            public long run() { return CommonWorkloads.mathTranscendental(); }
        });
        runBench("arraySequential", new BenchFn() {
            public long run() { return CommonWorkloads.arraySequential(); }
        });
        runBench("arrayRandom", new BenchFn() {
            public long run() { return CommonWorkloads.arrayRandom(); }
        });
        runBench("objectAllocation", new BenchFn() {
            public long run() { return CommonWorkloads.objectAllocation(); }
        });
        runBench("valueEscape", new BenchFn() {
            public long run() { return CommonWorkloads.valueEscape(); }
        });
        runBench("hashMapChurn", new BenchFn() {
            public long run() { return CommonWorkloads.hashMapChurn(); }
        });
        runBench("stringBuilding", new BenchFn() {
            public long run() { return CommonWorkloads.stringBuilding(); }
        });
        runBench("recursion", new BenchFn() {
            public long run() { return CommonWorkloads.recursion(); }
        });
        runBench("quicksort", new BenchFn() {
            public long run() { return CommonWorkloads.quicksortBench(); }
        });
        System.out.println("DONE");
    }

    private Bench() {
    }
}
