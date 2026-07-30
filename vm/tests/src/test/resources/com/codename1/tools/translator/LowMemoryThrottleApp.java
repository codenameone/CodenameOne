/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

/**
 * The issue-5482 shape: a background loader that allocates a large number of
 * LEGACY-path buffers (byte[] above CN1_BIBOP_MAX_OBJECT, so BiBOP does not
 * serve them) while the OS reports memory pressure.
 *
 * Under CN1_SIMULATE_MEMORY_WARNING_MS the VM raises lowMemoryMode at a fixed
 * cadence, standing in for the sustained didReceiveMemoryWarning delivery a
 * memory-constrained iOS device produces. Every throttled allocation used to
 * park for a millisecond, capping the loader near 1000 allocations/second: the
 * reporter's dictionary load went from seconds to never finishing, with no
 * crash and no log line to explain it.
 *
 * The loader is deliberately allocation-dense and short: ALLOCATIONS buffers
 * with no artificial pacing, so the ratio between throttle parks and throttled
 * allocations is the observable, not wall-clock time (which depends on runner
 * speed and load). RESULT= is a content checksum, compared against the same
 * program on the host JVM so the throttle cannot be "fixed" by dropping work.
 */
public class LowMemoryThrottleApp {

    /** Legacy-path allocations: comfortably above CN1_BIBOP_MAX_OBJECT (512). */
    private static final int BUFFER_SIZE = 2048;

    /** Enough allocations that per-allocation parking is unmistakable (20s+). */
    private static final int ALLOCATIONS = 20000;

    /** Retained working set, so the load is not pure garbage. */
    private static final int RETAINED = 256;

    public static void main(String[] args) {
        byte[][] retained = new byte[RETAINED][];
        long checksum = 0;

        long start = System.currentTimeMillis();
        for (int i = 0; i < ALLOCATIONS; i++) {
            byte[] buffer = new byte[BUFFER_SIZE];
            // Touch a few bytes so the buffer cannot be optimized away and the
            // checksum depends on every iteration.
            buffer[0] = (byte) i;
            buffer[BUFFER_SIZE / 2] = (byte) (i >> 8);
            buffer[BUFFER_SIZE - 1] = (byte) (i >> 16);
            checksum = checksum * 31 + buffer[0] + buffer[BUFFER_SIZE / 2] + buffer[BUFFER_SIZE - 1];

            // Keep a bounded window alive, and read one back so the retained
            // slot is genuinely reachable rather than dead on arrival.
            byte[] evicted = retained[i % RETAINED];
            if (evicted != null) {
                checksum += evicted[0];
            }
            retained[i % RETAINED] = buffer;
        }
        long elapsed = System.currentTimeMillis() - start;

        for (int i = 0; i < RETAINED; i++) {
            if (retained[i] != null) {
                checksum += retained[i][BUFFER_SIZE - 1];
            }
        }

        System.out.println("RESULT=" + checksum);
        System.out.println("ALLOCATIONS=" + ALLOCATIONS);
        System.out.println("ELAPSED_MS=" + elapsed);
        System.out.println("LOW_MEMORY_THROTTLE_DONE");
    }
}
