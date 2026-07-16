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

package com.codenameone.examples.hellocodenameone.tests;

import com.bench.CommonWorkloads;

/** Runs the canonical ParparVM common workloads inside every port app. */
public class CommonWorkloadBenchmarkTest extends BaseTest {
    private static final int WARMUP = 3;
    private static final int MEASUREMENTS = 5;
    private static long sink;
    private static long minimumUsedMemory;
    private static long peakUsedMemory;

    private interface Workload {
        long run();
    }

    @Override
    public boolean runTest() {
        try {
            sink = 0;
            minimumUsedMemory = 0;
            peakUsedMemory = 0;
            System.gc();
            recordMemory();
            run("intArithmetic", new Workload() {
                public long run() { return CommonWorkloads.intArithmetic(); }
            });
            run("longArithmetic", new Workload() {
                public long run() { return CommonWorkloads.longArithmetic(); }
            });
            run("mathTranscendental", new Workload() {
                public long run() { return CommonWorkloads.mathTranscendental(); }
            });
            run("arraySequential", new Workload() {
                public long run() { return CommonWorkloads.arraySequential(); }
            });
            run("arrayRandom", new Workload() {
                public long run() { return CommonWorkloads.arrayRandom(); }
            });
            run("objectAllocation", new Workload() {
                public long run() { return CommonWorkloads.objectAllocation(); }
            });
            run("hashMapChurn", new Workload() {
                public long run() { return CommonWorkloads.hashMapChurn(); }
            });
            run("stringBuilding", new Workload() {
                public long run() { return CommonWorkloads.stringBuilding(); }
            });
            run("recursion", new Workload() {
                public long run() { return CommonWorkloads.recursion(); }
            });
            run("quicksort", new Workload() {
                public long run() { return CommonWorkloads.quicksortBench(); }
            });
            emit("memory kind=managed-heap minimum_bytes=" + minimumUsedMemory
                    + " peak_bytes=" + peakUsedMemory);
            emit("complete benchmark_version=1 checksum=" + sink);
            done();
            return true;
        } catch (Throwable t) {
            fail("Common workload benchmark failed: " + t);
            return false;
        }
    }

    private static void run(String id, Workload workload) {
        for (int i = 0; i < WARMUP; i++) {
            sink ^= workload.run();
            recordMemory();
        }
        long minimumNanos = Long.MAX_VALUE;
        long checksum = 0;
        for (int i = 0; i < MEASUREMENTS; i++) {
            long start = System.nanoTime();
            checksum = workload.run();
            long elapsed = System.nanoTime() - start;
            if (elapsed < minimumNanos) {
                minimumNanos = elapsed;
            }
            recordMemory();
        }
        sink ^= checksum;
        emit("benchmark id=" + id + " duration_ns=" + minimumNanos + " checksum=" + checksum);
    }

    private static void recordMemory() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        if (used < 0) {
            return;
        }
        if (minimumUsedMemory == 0 || used < minimumUsedMemory) {
            minimumUsedMemory = used;
        }
        if (used > peakUsedMemory) {
            peakUsedMemory = used;
        }
    }

    private static void emit(String value) {
        System.out.println("CN1SS:PERF:" + value);
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}
