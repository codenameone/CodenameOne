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
package com.codename1.health;

/// A contiguous span of one [SleepStage] inside a [SleepSample].
public final class SleepStageInterval {

    private final SleepStage stage;
    private final long startMillis;
    private final long endMillis;

    /// The platform's own value for this span, or [#NO_PLATFORM_CODE].
    ///
    /// The fidelity escape hatch. A portable [SleepStage] is a
    /// lowest-common-denominator of two vendors' vocabularies, and an app
    /// that needs exactly what HealthKit or Health Connect said -- to match
    /// another tool, or to handle a value this build predates -- would
    /// otherwise have to give up the portable API entirely to get it.
    private int platformCode = NO_PLATFORM_CODE;

    /// Returned by [#getPlatformCode()] when the port did not record one.
    public static final int NO_PLATFORM_CODE = -1;

    /// The raw platform value, or [#NO_PLATFORM_CODE].
    ///
    /// `HKCategoryValueSleepAnalysis` on iOS, the Health Connect stage
    /// constant on Android. Meaningless across platforms by design: read it
    /// only after checking which one you are on.
    public int getPlatformCode() {
        return platformCode;
    }

    /// Records the platform's own value. Called by ports while reading.
    public void setPlatformCode(int platformCode) {
        this.platformCode = platformCode;
    }

    /// Creates a stage interval.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `stage` is null or the end
    ///   precedes the start.
    public SleepStageInterval(SleepStage stage, long startMillis,
            long endMillis) {
        if (stage == null) {
            throw new IllegalArgumentException(
                    "a stage interval requires a stage");
        }
        if (endMillis < startMillis) {
            throw new IllegalArgumentException(
                    "stage interval ends before it starts");
        }
        this.stage = stage;
        this.startMillis = startMillis;
        this.endMillis = endMillis;
    }

    /// The stage this span was classified as.
    public SleepStage getStage() {
        return stage;
    }

    /// Inclusive start, epoch millis UTC.
    public long getStartMillis() {
        return startMillis;
    }

    /// Exclusive end, epoch millis UTC.
    public long getEndMillis() {
        return endMillis;
    }

    /// The length of this span in milliseconds.
    public long getDurationMillis() {
        return endMillis - startMillis;
    }

    @Override
    public String toString() {
        return stage + "[" + startMillis + ".." + endMillis + "]";
    }
}
