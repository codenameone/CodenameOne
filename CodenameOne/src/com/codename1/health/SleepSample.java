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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A sleep session, optionally broken into [SleepStageInterval] spans.
///
/// #### Stage detail is not guaranteed
///
/// A session recorded by a watch usually carries stages; one inferred by a
/// phone usually does not, and neither did iOS before version 16. Check
/// [#hasStageDetail()] and fall back to showing the total duration --
/// drawing a hypnogram from a single "asleep" span produces an empty chart
/// that looks like a bug.
///
/// ```java
/// if (night.hasStageDetail()) {
///     for (SleepStageInterval iv : night.getStages()) {
///         drawBand(iv.getStage(), iv.getStartMillis(), iv.getEndMillis());
///     }
/// } else {
///     showTotal(night.getDurationMillis());
/// }
/// ```
///
/// #### Sessions on iOS are reassembled
///
/// HealthKit has no sleep-session object at all -- only a run of
/// overlapping category samples. The iOS port groups them into sessions
/// using Apple's own convention of splitting on a gap, configurable via
/// [SampleQuery#setSleepSessionGapMillis(long)]. That grouping is a
/// heuristic, which is why it lives in the port and is tunable rather than
/// being silently baked into the shared code.
public final class SleepSample extends SessionSample {

    private final List<SleepStageInterval> stages;

    private SleepSample(long startMillis, long endMillis,
            List<SleepStageInterval> stages) {
        super(HealthDataType.SLEEP, startMillis, endMillis);
        this.stages = stages;
    }

    /// A session with no stage breakdown.
    public static SleepSample create(long startMillis, long endMillis) {
        return new SleepSample(startMillis, endMillis,
                new ArrayList<SleepStageInterval>());
    }

    /// A session with stage detail. The list is copied defensively.
    public static SleepSample create(long startMillis, long endMillis,
            List<SleepStageInterval> stages) {
        List<SleepStageInterval> copy = new ArrayList<SleepStageInterval>();
        if (stages != null) {
            copy.addAll(stages);
        }
        return new SleepSample(startMillis, endMillis, copy);
    }

    /// The stage breakdown, in the order the platform reported it. Empty
    /// when no breakdown is available.
    public List<SleepStageInterval> getStages() {
        return Collections.unmodifiableList(stages);
    }

    /// Appends a stage span. Used when building a session to write.
    public void addStage(SleepStageInterval interval) {
        if (interval != null) {
            stages.add(interval);
        }
    }

    /// `true` when this session carries a real stage breakdown -- that is,
    /// at least one span classified as something more specific than
    /// "asleep", "in bed" or "unknown".
    ///
    /// Returns `false` for an empty stage list and for a list containing
    /// only [SleepStage#ASLEEP_UNSPECIFIED], [SleepStage#AWAKE_IN_BED] and
    /// [SleepStage#UNKNOWN], because none of those tell you anything a
    /// hypnogram could show.
    public boolean hasStageDetail() {
        for (SleepStageInterval stage : stages) {
            SleepStage s = stage.getStage();
            if (s == SleepStage.LIGHT || s == SleepStage.DEEP
                    || s == SleepStage.REM || s == SleepStage.AWAKE
                    || s == SleepStage.OUT_OF_BED) {
                return true;
            }
        }
        return false;
    }

    /// The total time in `stage`, summed across every span.
    public long getDurationMillis(SleepStage stage) {
        long total = 0;
        for (SleepStageInterval iv : stages) {
            if (iv.getStage() == stage) {
                total += iv.getDurationMillis();
            }
        }
        return total;
    }

    /// The total time spent asleep -- every span except [SleepStage#AWAKE],
    /// [SleepStage#AWAKE_IN_BED] and [SleepStage#OUT_OF_BED].
    ///
    /// Falls back to the whole session duration when no stages are
    /// present, which is the best available answer for a source that only
    /// reported "asleep from X to Y".
    public long getAsleepDurationMillis() {
        if (stages.isEmpty()) {
            return getDurationMillis();
        }
        long total = 0;
        for (SleepStageInterval iv : stages) {
            SleepStage s = iv.getStage();
            if (s != SleepStage.AWAKE && s != SleepStage.AWAKE_IN_BED
                    && s != SleepStage.OUT_OF_BED) {
                total += iv.getDurationMillis();
            }
        }
        return total;
    }

    @Override
    public String toString() {
        return "SleepSample[" + getStartMillis() + ".." + getEndMillis()
                + " stages=" + stages.size() + "]";
    }
}
