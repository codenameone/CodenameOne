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
package com.codename1.impl.javase.health;

import com.codename1.health.HealthDataType;
import com.codename1.health.HealthQuantity;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthUnit;
import com.codename1.health.QuantitySample;
import com.codename1.health.RecordingMethod;
import com.codename1.health.SleepSample;
import com.codename1.health.SleepStage;
import com.codename1.health.SleepStageInterval;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/// Generates physiologically plausible health data for the simulator.
///
/// #### Why generated rather than recorded
///
/// The Bluetooth simulator replays recorded traces, scrubbed by
/// `FixtureScrambler`. That approach does not transfer to health data, and
/// the reason is not squeamishness: for BLE the sensitive parts are
/// *identifiers*, so scrubbing MAC addresses leaves a trace that is still
/// useful because tests assert on structure. For health, the sensitive part
/// **is** the value being asserted on. Scrubbing it either destroys the
/// signal, making the fixture worthless, or preserves it -- and a resting
/// heart rate, sleep timing and step cadence together are quasi-identifying
/// biometric data, committed to a public repository, in git history,
/// forever.
///
/// There is also nothing to record from: there is no desktop HealthKit.
///
/// So the simulator fabricates instead. Everything here is seeded, so a
/// test asserting an exact total is stable forever, and what would be
/// committed is a persona's parameters rather than anyone's data.
public final class SyntheticHealthData {

    private final Random random;

    /// Creates a generator with a fixed seed, so the same seed always
    /// produces the same dataset.
    public SyntheticHealthData(long seed) {
        this.random = new Random(seed);
    }

    /// A week of plausible data for a moderately active adult, ending at
    /// `endMillis`.
    public List<HealthSample> generateWeek(long endMillis) {
        return generate(endMillis, 7);
    }

    /// `days` of data ending at `endMillis`: hourly step totals, resting
    /// and active heart rate, a nightly sleep session with stages, and a
    /// morning weight.
    public List<HealthSample> generate(long endMillis, int days) {
        List<HealthSample> out = new ArrayList<HealthSample>();
        long dayMillis = 24L * 3600 * 1000;
        long startOfWindow = endMillis - days * dayMillis;
        double weightKg = 72 + random.nextDouble() * 4;

        for (int d = 0; d < days; d++) {
            long dayStart = startOfWindow + d * dayMillis;
            generateSteps(out, dayStart);
            generateHeartRate(out, dayStart);
            generateSleep(out, dayStart);

            // Weight drifts slowly with daily noise, the way a real scale
            // reading does -- a flat line would make trend UI look broken.
            weightKg += (random.nextDouble() - 0.5) * 0.3;
            QuantitySample w = QuantitySample.create(HealthDataType.BODY_MASS,
                    new HealthQuantity(weightKg + random.nextDouble() * 0.4,
                            HealthUnit.KILOGRAM),
                    dayStart + 7 * 3600000L);
            w.setRecordingMethod(RecordingMethod.MANUAL_ENTRY);
            out.add(w);
        }
        return out;
    }

    /// Hourly step totals shaped like a real day: nothing overnight, a
    /// commute bump, a lunch walk and an evening peak.
    private void generateSteps(List<HealthSample> out, long dayStart) {
        int[] hourlyMean = {
            0, 0, 0, 0, 0, 0, 120, 900, 1400, 400, 300, 350,
            1100, 700, 300, 350, 400, 1200, 1500, 800, 400, 200, 80, 0
        };
        for (int h = 0; h < 24; h++) {
            if (hourlyMean[h] == 0) {
                continue;
            }
            int steps = (int) (hourlyMean[h]
                    * (0.6 + random.nextDouble() * 0.8));
            long start = dayStart + h * 3600000L;
            out.add(QuantitySample.create(HealthDataType.STEPS,
                    new HealthQuantity(steps, HealthUnit.COUNT),
                    start, start + 3600000L));
        }
    }

    /// A circadian heart-rate curve: a resting floor overnight, a daytime
    /// baseline, and excursions during the active hours.
    private void generateHeartRate(List<HealthSample> out, long dayStart) {
        double resting = 52 + random.nextDouble() * 8;
        for (int h = 0; h < 24; h++) {
            boolean asleep = h < 6 || h >= 23;
            boolean active = h == 7 || h == 12 || h == 17 || h == 18;
            double base = asleep ? resting
                    : (active ? resting + 45 : resting + 18);
            for (int q = 0; q < 4; q++) {
                double bpm = base + (random.nextDouble() - 0.5) * 10;
                bpm = Math.max(35, Math.min(205, bpm));
                long at = dayStart + h * 3600000L + q * 900000L;
                out.add(QuantitySample.create(HealthDataType.HEART_RATE,
                        new HealthQuantity(Math.round(bpm),
                                HealthUnit.COUNT_PER_MINUTE), at));
            }
        }
        out.add(QuantitySample.create(HealthDataType.RESTING_HEART_RATE,
                new HealthQuantity(Math.round(resting),
                        HealthUnit.COUNT_PER_MINUTE),
                dayStart + 8 * 3600000L));
    }

    /// A night's sleep with realistic stage proportions and one or two
    /// brief wakings.
    private void generateSleep(List<HealthSample> out, long dayStart) {
        long onset = dayStart - 1 * 3600000L
                + (long) (random.nextDouble() * 3600000L);
        long duration = (long) ((6.5 + random.nextDouble() * 1.5)
                * 3600000L);
        long end = onset + duration;

        List<SleepStageInterval> stages = new ArrayList<SleepStageInterval>();
        long cursor = onset;
        // Roughly 5% awake, 50% light, 20% deep, 25% REM -- the usual adult
        // proportions, cycling rather than in one block.
        while (cursor < end) {
            long cycle = Math.min(90 * 60000L, end - cursor);
            long light = (long) (cycle * 0.5);
            long deep = (long) (cycle * 0.2);
            long rem = cycle - light - deep;
            stages.add(new SleepStageInterval(SleepStage.LIGHT, cursor,
                    cursor + light));
            cursor += light;
            stages.add(new SleepStageInterval(SleepStage.DEEP, cursor,
                    cursor + deep));
            cursor += deep;
            stages.add(new SleepStageInterval(SleepStage.REM, cursor,
                    Math.min(end, cursor + rem)));
            cursor += rem;
            if (cursor < end && random.nextDouble() < 0.4) {
                long awake = Math.min(4 * 60000L, end - cursor);
                stages.add(new SleepStageInterval(SleepStage.AWAKE, cursor,
                        cursor + awake));
                cursor += awake;
            }
        }
        out.add(SleepSample.create(onset, end, stages));
    }
}
