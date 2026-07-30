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
/// #### Neither phone reads sleep in this release
///
/// Sleep is available on the simulator, the desktop and the JavaScript
/// port, and on none of the mobile ones: a sleep query is refused on iOS
/// and Android alike, so no session of this shape ever comes back from a
/// device. Build against it by all means -- but do not build a mobile
/// screen around it yet.
///
/// The reason it is unfinished on iOS is worth knowing, because it shapes
/// what finishing it will look like. HealthKit has no sleep-session
/// object at all, only a run of overlapping category samples, so the port
/// has to group them into sessions itself -- Apple's own convention is to
/// split on a gap, which is what [SampleQuery#setSleepSessionGapMillis(long)]
/// is there to tune. That grouping is a heuristic, which is why it
/// belongs in the port rather than baked into the shared code.
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
            for (SleepStageInterval interval : stages) {
                // Skipping the null during validation and then copying the
                // whole list anyway meant it was stored unvalidated, and
                // every accessor that walks the stages -- hasStageDetail,
                // the two duration roll-ups -- dereferenced it. Dropping it
                // here instead matches addStage(null), which has always
                // ignored one.
                if (interval != null) {
                    requireInsideSession(interval, startMillis, endMillis);
                    copy.add(interval);
                }
            }
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
            requireInsideSession(interval, getStartMillis(), getEndMillis());
            stages.add(interval);
        }
    }

    /// A stage outside the session it belongs to is not a stage of it.
    ///
    /// Unchecked, a session could report more time asleep than it lasted,
    /// and carry stage data outside the very span the record is queried
    /// by -- so a query that found the session would return detail from
    /// outside its own range.
    private static void requireInsideSession(SleepStageInterval interval,
            long startMillis, long endMillis) {
        if (interval.getStartMillis() < startMillis
                || interval.getEndMillis() > endMillis) {
            throw new IllegalArgumentException("sleep stage "
                    + interval.getStartMillis() + ".."
                    + interval.getEndMillis()
                    + " falls outside the session's span " + startMillis
                    + ".." + endMillis);
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

    /// The total time in `stage` -- the time actually covered by its
    /// spans, so two overlapping spans of the same stage count the overlap
    /// once.
    public long getDurationMillis(SleepStage stage) {
        List<SleepStageInterval> matching =
                new ArrayList<SleepStageInterval>();
        for (SleepStageInterval iv : stages) {
            if (iv.getStage() == stage) {
                matching.add(iv);
            }
        }
        return coveredMillis(matching);
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
        List<SleepStageInterval> asleep =
                new ArrayList<SleepStageInterval>();
        for (SleepStageInterval iv : stages) {
            SleepStage s = iv.getStage();
            // Named, not "anything that is not awake". The negative form
            // swept in UNKNOWN, which the enum defines as a span the source
            // did not classify -- so a session carrying one ten-minute
            // unclassified interval reported ten minutes of sleep, turning
            // missing information into a measurement. A reading this API
            // will not make up: an unclassified span counts towards neither
            // side, and hasStageDetail() is what tells a caller the
            // breakdown is unavailable.
            if (s == SleepStage.ASLEEP_UNSPECIFIED || s == SleepStage.LIGHT
                    || s == SleepStage.DEEP || s == SleepStage.REM) {
                asleep.add(iv);
            }
        }
        return coveredMillis(asleep);
    }

    /// Time actually covered by a set of spans, counting an overlap once.
    ///
    /// Summing the spans instead was wrong for the data both platforms
    /// really produce: this class documents platform sleep categories as
    /// overlapping, and a LIGHT span of `[0,10]` beside a DEEP span of
    /// `[5,15]` summed to 20 in a session that lasted 15. A total time
    /// asleep longer than the night it happened in is the kind of number
    /// that gets drawn straight into a chart.
    ///
    /// Note this cannot be replaced by clamping to the session length --
    /// that would hide the overlap while still reporting the wrong figure
    /// for every night short enough not to hit the clamp.
    private static long coveredMillis(List<SleepStageInterval> spans) {
        if (spans.isEmpty()) {
            return 0;
        }
        Collections.sort(spans, ByStart.INSTANCE);
        long covered = 0;
        long openFrom = spans.get(0).getStartMillis();
        long openTo = spans.get(0).getEndMillis();
        for (int iter = 1; iter < spans.size(); iter++) {
            SleepStageInterval iv = spans.get(iter);
            if (iv.getStartMillis() > openTo) {
                covered += openTo - openFrom;
                openFrom = iv.getStartMillis();
                openTo = iv.getEndMillis();
            } else if (iv.getEndMillis() > openTo) {
                openTo = iv.getEndMillis();
            }
        }
        return covered + openTo - openFrom;
    }

    /// Sorts spans by start so the merge above only has to look at the one
    /// span it currently has open.
    private static final class ByStart
            implements java.util.Comparator<SleepStageInterval> {

        static final ByStart INSTANCE = new ByStart();

        @Override
        public int compare(SleepStageInterval a, SleepStageInterval b) {
            long left = a.getStartMillis();
            long right = b.getStartMillis();
            if (left == right) {
                return 0;
            }
            return left < right ? -1 : 1;
        }
    }

    @Override
    public String toString() {
        return "SleepSample[" + getStartMillis() + ".." + getEndMillis()
                + " stages=" + stages.size() + "]";
    }
}
