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
    private static final int MEASURE = 5;

    private interface BenchFn {
        long run();
    }

    private static void runBench(String name, BenchFn fn) {
        for (int warmup = 0; warmup < WARMUP; warmup++) {
            fn.run();
        }
        for (int repetition = 0; repetition < MEASURE; repetition++) {
            long started = System.nanoTime();
            long checksum = fn.run();
            long elapsed = System.nanoTime() - started;
            System.out.println("BENCH " + name + " rep " + repetition
                    + " ns=" + elapsed + " checksum=" + checksum);
        }
    }

    public static void main(String[] args) {
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
