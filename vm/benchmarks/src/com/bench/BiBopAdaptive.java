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
 * Sustained small-array allocation with a large retained set. This deliberately
 * models the issue-5425 shape: a temporary key-sized byte[] and a retained
 * compressed-value-sized byte[] per definition. Unlike GcStress, it runs long
 * enough for the allocator and collector to observe more than one survival
 * epoch, then verifies the retained data after every collection.
 */
public final class BiBopAdaptive {
    private static final int RETAINED = 560000;
    private static final int CHURN_PER_PHASE = 360000;
    private static byte[] escape;
    private static byte[][] retainedRoot;

    private static void pause(long millis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + millis;
        for (;;) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return;
            }
            // Native signals may interrupt usleep on some targets; retry until
            // wall-clock time proves the concurrent collector got its window.
            Thread.sleep(remaining);
        }
    }

    private static long fill(byte[] value, int seed) {
        long checksum = 0;
        for (int i = 0; i < value.length; i++) {
            value[i] = (byte)(seed * 31 + i * 17);
            checksum += value[i];
        }
        return checksum;
    }

    private static long verify(byte[][] retained) {
        long checksum = 0;
        for (int i = 0; i < retained.length; i += 97) {
            byte[] value = retained[i];
            if (value == null || value.length != 16) {
                throw new RuntimeException("retained array lost at " + i);
            }
            for (int j = 0; j < value.length; j++) {
                byte expected = (byte)(i * 31 + j * 17);
                if (value[j] != expected) {
                    throw new RuntimeException("retained array corrupted at " + i + "/" + j);
                }
                checksum += value[j];
            }
        }
        return checksum;
    }

    private static long churn(int phase) {
        long checksum = 0;
        for (int i = 0; i < CHURN_PER_PHASE; i++) {
            byte[] temporaryKey = new byte[8];
            checksum += fill(temporaryKey, i ^ phase);
            if ((i & 8191) == 0) {
                escape = temporaryKey;
            }
        }
        return checksum;
    }

    public static void main(String[] args) throws Exception {
        // ParparVM deliberately delays the first GC cycle for two seconds to avoid
        // startup interference. Start it before measuring the sustained phase and
        // wait past that one-time delay so every explicit collection below is real.
        System.gc();
        pause(2200);
        byte[][] retained = new byte[RETAINED][];
        retainedRoot = retained;
        long checksum = 0;
        for (int i = 0; i < retained.length; i++) {
            byte[] temporaryKey = new byte[8];
            checksum += fill(temporaryKey, i ^ 0x55aa);
            byte[] compressed = new byte[16];
            checksum += fill(compressed, i);
            retained[i] = compressed;
        }

        // Multiple epochs are essential: one-cycle grace must not be mistaken for
        // a survivor-heavy workload, and both trigger growth and legacy bypass use
        // a consecutive-sample decision.
        for (int phase = 0; phase < 5; phase++) {
            checksum += churn(phase);
            System.gc();
            pause(200);
            checksum += verify(retained);
        }

        // Keep the retained set observable through the final checksum, then drop it
        // and make sure the collector can return to a churn-heavy/reclaiming phase.
        checksum += verify(retained);
        retained = null;
        retainedRoot = null;
        for (int phase = 5; phase < 8; phase++) {
            checksum += churn(phase);
            System.gc();
            pause(200);
        }
        // The collector is concurrent and System.gc() is intentionally non-blocking.
        // Keep the process alive long enough for the final requested cycles and their
        // diagnostics to finish before the native benchmark main exits.
        pause(3000);
        if (escape == null || escape.length != 8) {
            throw new RuntimeException("escape sink lost");
        }
        // Host-JVM oracle for this deterministic workload. Every collector variant
        // must reach the identical value; a mismatch exits nonzero before timing is
        // considered.
        if (checksum != -18515648L) {
            throw new RuntimeException("checksum mismatch " + checksum);
        }
        System.out.println("BIBOP_ADAPTIVE_OK checksum=" + checksum);
    }
}
