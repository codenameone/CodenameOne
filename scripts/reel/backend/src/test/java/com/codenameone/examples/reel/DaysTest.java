/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.reel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/// Time cut at midnight, which is how the daily statistics are bucketed.
class DaysTest {
    /// 2026-03-01 00:00:00 UTC.
    private static final long MIDNIGHT = 20513L * Days.MILLIS;

    @Test
    void aStretchAcrossMidnightIsCreditedHalfToEachDay() {
        // 23:59:40 to 00:00:20: twenty seconds on each side. Credited whole to
        // the day it ended in, the new day had all forty.
        long day = Days.number(MIDNIGHT);
        assertArrayEquals(new long[] {day - 1, 20000L, day, 20000L},
                Days.split(MIDNIGHT - 20000L, MIDNIGHT + 20000L));
    }

    @Test
    void aStretchWithinOneDayIsThatDays() {
        long day = Days.number(MIDNIGHT);
        assertArrayEquals(new long[] {day, 40000L},
                Days.split(MIDNIGHT + 1000L, MIDNIGHT + 41000L));
        // Ending exactly at midnight is all the old day's.
        assertArrayEquals(new long[] {day - 1, 40000L},
                Days.split(MIDNIGHT - 40000L, MIDNIGHT));
        // Starting exactly at it is all the new one's.
        assertArrayEquals(new long[] {day, 40000L}, Days.split(MIDNIGHT, MIDNIGHT + 40000L));
        assertArrayEquals(new long[0], Days.split(MIDNIGHT, MIDNIGHT));
    }
}
