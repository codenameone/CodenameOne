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
package com.codenameone.examples.wayline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/// Time on line cut at midnight, which is how a driver's hours are credited.
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
