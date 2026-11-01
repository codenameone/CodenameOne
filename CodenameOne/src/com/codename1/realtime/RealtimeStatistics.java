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
package com.codename1.realtime;

/// A snapshot of how a [RealtimeThread]'s periods went: how late each release woke the
/// thread (latency), how long its cycles ran, and how many releases it missed.
///
/// Latency is the number to watch. Its maximum is the loop's real worst case on this
/// device, under whatever load ran beside it; [#getLatencyHistogram()] shows how the
/// rest are spread. Taken with [RealtimeThread#getStatistics()].
@com.codename1.impl.SharedWithBackend
public final class RealtimeStatistics {
    static final int CYCLES = 0;
    static final int MISSED = 1;
    static final int MAX_LATENCY = 2;
    static final int SUM_LATENCY = 3;
    static final int MAX_EXEC = 4;
    static final int SUM_EXEC = 5;
    static final int VIOLATIONS = 6;
    static final int HIST = 7;
    /// The number of buckets in [#getLatencyHistogram()].
    public static final int HISTOGRAM_BUCKETS = 16;
    static final int COUNT = HIST + HISTOGRAM_BUCKETS;

    private final long[] values;

    RealtimeStatistics(long[] values) {
        this.values = values;
    }

    /// The number of cycles run.
    public long getCycles() {
        return values[CYCLES];
    }

    /// The number of releases skipped because a cycle ran past them.
    public long getMissedPeriods() {
        return values[MISSED];
    }

    /// The latest any release woke the thread, in nanoseconds.
    public long getMaxLatencyNanos() {
        return values[MAX_LATENCY];
    }

    /// The mean wake-up latency, in nanoseconds.
    public long getMeanLatencyNanos() {
        return values[CYCLES] == 0 ? 0 : values[SUM_LATENCY] / values[CYCLES];
    }

    /// The longest a cycle ran, in nanoseconds: from its wake-up to its next wait.
    public long getMaxExecutionNanos() {
        return values[MAX_EXEC];
    }

    /// The mean time a cycle ran, in nanoseconds.
    public long getMeanExecutionNanos() {
        return values[CYCLES] == 0 ? 0 : values[SUM_EXEC] / values[CYCLES];
    }

    /// The number of allocations that ended a steady state on this thread.
    public long getAllocationViolations() {
        return values[VIOLATIONS];
    }

    /// Wake-up latencies counted by powers of two of a microsecond: index 0 is under
    /// one microsecond, index 1 under two, index `i` under `2^i` microseconds, and the
    /// last bucket everything from about 16 milliseconds up.
    public long[] getLatencyHistogram() {
        long[] h = new long[HISTOGRAM_BUCKETS];
        System.arraycopy(values, HIST, h, 0, HISTOGRAM_BUCKETS);
        return h;
    }

    /// The latency, in nanoseconds, under which at least `fraction` of the wake-ups
    /// fell, read from the histogram -- so it is the upper edge of a power-of-two
    /// bucket, an over-estimate by at most a factor of two.
    ///
    /// #### Parameters
    ///
    /// - `fraction`: between 0 and 1, for example 0.9999
    public long getLatencyPercentileNanos(double fraction) {
        long total = 0;
        for (int i = 0; i < HISTOGRAM_BUCKETS; i++) {
            total += values[HIST + i];
        }
        if (total == 0) {
            return 0;
        }
        long need = (long) Math.ceil(total * fraction);
        long seen = 0;
        for (int i = 0; i < HISTOGRAM_BUCKETS; i++) {
            seen += values[HIST + i];
            if (seen >= need) {
                return i == HISTOGRAM_BUCKETS - 1 ? values[MAX_LATENCY] : (1000L << i);
            }
        }
        return values[MAX_LATENCY];
    }

    /// One line, for a log.
    @Override
    public String toString() {
        return "cycles=" + getCycles() + " missed=" + getMissedPeriods()
                + " latency max=" + getMaxLatencyNanos() + "ns mean=" + getMeanLatencyNanos()
                + "ns p99.99<=" + getLatencyPercentileNanos(0.9999)
                + "ns exec max=" + getMaxExecutionNanos() + "ns mean=" + getMeanExecutionNanos()
                + "ns violations=" + getAllocationViolations();
    }
}
