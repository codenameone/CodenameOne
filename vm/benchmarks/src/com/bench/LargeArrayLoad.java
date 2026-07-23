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

import java.util.Hashtable;

/**
 * Models the FINAL issue-5425 Dtest shape (the one from the last round of
 * comments, after the reporter removed all transient garbage): pass 1 builds a
 * persistent survivor set of SMALL (BiBOP-resident) objects, pass 2 then
 * allocates ONLY LARGE (>CN1_BIBOP_MAX_OBJECT, legacy-path) retained byte[]
 * blocks -- the reporter's volume shape, producing NO garbage.
 *
 * Each pass is stretched to a fixed WALL duration with allocation-free
 * compute, because the regression being guarded is a TRIGGER-CADENCE bug: the
 * pre-BiBOP 1MB isHighFrequencyGC re-arm keeps a full collection cycle
 * starting every re-arm window for as long as >1MB is allocated per interval
 * -- "allocating the large arrays is triggering a global multiple times" --
 * while the fixed VM (24MB budget, event-driven legacy trigger) collects only
 * when allocation VOLUME demands it. Run with CN1_GC_LOG_CYCLES=1 and count
 * [GC-CYCLE] stderr lines: the cycle count is the load-independent observable
 * (measured on this workload: unfixed 15 cycles, fixed 6, stable across
 * runs; wall-clock phase times only inflate when cycles contend with the
 * mutator, which depends on core count and machine load).
 */
public class LargeArrayLoad {
    static Hashtable dict;      // survivor set for the current round
    static byte[][] blocks;     // retained large arrays for the current round

    // Spin without allocating until the phase has lasted stretchMs, so the
    // number of 200ms GC re-arm windows per phase is machine-independent.
    private static long stretch(long phaseStart, long stretchMs, long seed) {
        long v = seed;
        while (System.currentTimeMillis() - phaseStart < stretchMs) {
            for (int i = 0; i < 5000; i++) {
                v = v * 6364136223846793005L + 1442695040888963407L;
            }
        }
        return v;
    }

    public static void main(String[] args) {
        int rounds = 12;
        int smallCount = 15000;
        int blockCount = 400;      // ~4MB of 10K blocks per round: above the old
        int blockSize = 10240;     // 1MB re-arm, well below the 24MB budget
        long stretchMs = 400;
        long checksum = 0;
        StringBuilder sb = new StringBuilder();
        String[] probeKeys = new String[smallCount >> 9];
        for (int round = 0; round < rounds; round++) {
            long t0 = System.currentTimeMillis();
            // Pass 1: persistent small objects (Hashtable nodes, keys, tiny
            // byte[] defs) -- BiBOP-sized, ~100% survival for the round.
            Hashtable h = new Hashtable();
            for (int i = 0; i < smallCount; i++) {
                sb.setLength(0);
                sb.append("word");
                sb.append(i);
                String key = sb.toString();
                byte[] def = new byte[16 + (i & 31)];
                def[0] = (byte) i;
                def[def.length - 1] = (byte) (i >> 8);
                h.put(key, def);
                if ((i & 511) == 0 && (i >> 9) < probeKeys.length) {
                    probeKeys[i >> 9] = key;
                }
            }
            dict = h;
            checksum += stretch(t0, stretchMs, round);
            long t1 = System.currentTimeMillis();
            // Pass 2: ONLY large retained arrays -- the legacy allocation
            // path. No garbage is produced here at all.
            byte[][] b = new byte[blockCount][];
            for (int i = 0; i < blockCount; i++) {
                byte[] blk = new byte[blockSize];
                for (int j = 0; j < blockSize; j += 512) {
                    blk[j] = (byte) (i + j);
                }
                b[i] = blk;
            }
            blocks = b;
            checksum += stretch(t1, stretchMs, round + 31);
            long t2 = System.currentTimeMillis();
            // Pass 3: allocation-free lookups -- models the app using the
            // loaded dictionary.
            long probe = 0;
            for (int rep = 0; rep < 40; rep++) {
                for (int i = 0; i < probeKeys.length; i++) {
                    byte[] def = (byte[]) h.get(probeKeys[i]);
                    probe += def[0];
                }
            }
            long t3 = System.currentTimeMillis();
            checksum += probe + b[blockCount - 1][512];
            System.out.println("round " + round
                    + " small_ms=" + (t1 - t0)
                    + " large_ms=" + (t2 - t1)
                    + " settle_ms=" + (t3 - t2));
        }
        // The stretch() return values feed checksum, so RESULT would differ
        // between VMs/machines; keep the comparable part separate.
        long comparable = 0;
        for (int i = 0; i < probeKeys.length; i++) {
            byte[] def = (byte[]) dict.get(probeKeys[i]);
            comparable += def[0] + def[def.length - 1];
        }
        comparable += blocks[blockCount - 1][512] + blocks.length;
        if (checksum == Long.MIN_VALUE) {
            System.out.println("(unreachable sink " + checksum + ")");
        }
        System.out.println("RESULT=" + comparable);
        System.out.println("LARGE_ARRAY_LOAD_DONE");
    }
}
