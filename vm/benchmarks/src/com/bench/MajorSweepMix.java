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

package com.bench;

/**
 * The workload that exposes a major-sweep misclassification: a large BiBOP
 * survivor set driven by heavy LEGACY churn.
 *
 * <p>Both halves matter. The survivors are small (BiBOP-resident) objects that
 * stay live for the whole run, so their pages sit in bibopPartialPool where the
 * ordinary sweep never revisits them -- that is the population a major sweep has
 * to walk. The churn is large arrays, which take the legacy calloc path, so it
 * is what actually drives the collection cycles while contributing nothing to
 * bibopBytesSinceGc.
 *
 * <p>A quiet-cycle test that looks only at BiBOP volume therefore calls every
 * one of these cycles quiet and splices every partial page into every sweep,
 * which is the O(all pages) cost issue 5425 removed. Measured here with
 * CN1_LOG_PAGE_RELEASE=1 and CN1_GC_LOG_CYCLES=1, counting [MAJOR-SWEEP] lines
 * against [GC-CYCLE] lines: 36 major sweeps in 38 cycles when the quiet test
 * ignores legacy bytes, 2 in 39 when it includes them.
 *
 * <p>The churn phase runs for a fixed wall duration rather than a fixed count,
 * so the number of 200ms-paced collection cycles is machine-independent.
 */
public class MajorSweepMix {
    /** Retained for the whole run, so these pages stay in the partial pool. */
    static Object[] survivors;

    /** Small enough for the BiBOP page heap (at or under CN1_BIBOP_MAX_OBJECT). */
    private static final int SMALL_BYTES = 256;

    /** Comfortably above CN1_BIBOP_MAX_OBJECT, so the churn is legacy-path. */
    private static final int LARGE_BYTES = 65536;

    private static final int SURVIVOR_COUNT = 400000;
    private static final long CHURN_MS = 12000;

    public static void main(String[] args) {
        Object[] live = new Object[SURVIVOR_COUNT];
        for (int i = 0; i < SURVIVOR_COUNT; i++) {
            byte[] o = new byte[SMALL_BYTES];
            o[0] = (byte) i;
            live[i] = o;
        }
        survivors = live;

        long checksum = 0;
        long deadline = System.currentTimeMillis() + CHURN_MS;
        int i = 0;
        while (System.currentTimeMillis() < deadline) {
            // Check the clock once per batch; the call would otherwise dominate.
            for (int b = 0; b < 64; b++, i++) {
                byte[] big = new byte[LARGE_BYTES];
                // Touch every page: an untouched calloc'd block costs no memory.
                for (int off = 0; off < LARGE_BYTES; off += 4096) {
                    big[off] = (byte) (i + off);
                }
                checksum += big[0] + big[LARGE_BYTES - 1];
            }
        }
        checksum += ((byte[]) survivors[0])[0];
        System.out.println("ALLOCATIONS=" + i);
        System.out.println("RESULT=" + checksum);
    }
}
