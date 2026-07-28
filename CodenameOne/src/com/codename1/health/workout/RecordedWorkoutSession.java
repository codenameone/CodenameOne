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
package com.codename1.health.workout;

import com.codename1.health.AggregateMetric;
import com.codename1.health.Health;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthQuantity;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthStore;
import com.codename1.health.HealthUnit;
import com.codename1.health.HealthWriteResult;
import com.codename1.health.WorkoutSample;
import com.codename1.util.AsyncResource;
import com.codename1.util.AsyncResult;

import java.util.ArrayList;
import java.util.List;

/// A workout recorded by the framework rather than by the operating
/// system.
///
/// Used wherever the platform has no live workout session -- Android
/// phones, and iOS before version 26. The state machine, the elapsed clock
/// and the statistics rollup all come from [WorkoutSession]; this class
/// adds the part that is genuinely different: it accumulates the samples
/// the app feeds in and, on [WorkoutSession#end()], writes them to the
/// health store together with the workout record.
///
/// That is precisely the flow Google documents for Health Connect on
/// phones -- record the session yourself, batch the associated data, write
/// it when the session ends -- so on Android this is the correct design
/// rather than a fallback.
final class RecordedWorkoutSession extends WorkoutSession {

    private final List<HealthSample> collected = new ArrayList<HealthSample>();

    RecordedWorkoutSession(WorkoutConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected void doStart(AsyncResource<Boolean> out) {
        out.complete(Boolean.TRUE);
    }

    @Override
    protected void doPause(AsyncResource<Boolean> out) {
        out.complete(Boolean.TRUE);
    }

    @Override
    protected void doResume(AsyncResource<Boolean> out) {
        out.complete(Boolean.TRUE);
    }

    @Override
    protected void doAddSamples(List<HealthSample> samples,
            AsyncResource<Boolean> out) {
        synchronized (collected) {
            collected.addAll(samples);
        }
        out.complete(Boolean.TRUE);
    }

    @Override
    protected void doDiscard() {
        synchronized (collected) {
            collected.clear();
        }
    }

    /// The markers live on [WorkoutSample] because this class is not
    /// public: an app told to check for one has to be able to name it.
    private static final String SAMPLES_NOT_PERSISTED =
            WorkoutSample.SAMPLES_NOT_PERSISTED;

    private static final String WORKOUT_NOT_PERSISTED =
            WorkoutSample.WORKOUT_NOT_PERSISTED;

    @Override
    protected void doEnd(final AsyncResource<WorkoutSample> out) {
        final WorkoutSample workout = WorkoutSample.create(
                getConfiguration().getActivityType(), getStartedAtMillis(),
                getEndedAtMillis());
        // Clamped only against the two clock reads inside end(): the
        // accumulated total and the end timestamp are taken a few
        // instructions apart, and a wall clock that steps backwards
        // between them can make the first exceed the second by a
        // millisecond. The setter rejects anything larger, which is the
        // right answer for a caller supplying its own figure and the
        // wrong one for losing a real workout to an NTP correction.
        workout.setActiveDurationMillis(Math.min(getElapsedMillis(),
                workout.getDurationMillis()));
        workout.setTitle(getConfiguration().getTitle());
        applyTotals(workout);

        final HealthStore store = Health.getInstance().getStore();
        // Only what this platform can actually store. Neither HealthKit nor
        // the Health Connect bridge accepts a workout *session* through the
        // sample write path yet, and sending one made shared validation
        // reject the whole batch -- failing the session and discarding the
        // child samples that would have been written perfectly well.
        List<HealthSample> toWrite = new ArrayList<HealthSample>();
        if (store.isWritable(HealthDataType.WORKOUT)) {
            toWrite.add(workout);
        }
        StringBuilder rejected = new StringBuilder();
        synchronized (collected) {
            for (HealthSample s : collected) {
                if (store.isWritable(s.getType())) {
                    toWrite.add(s);
                } else {
                    noteRejected(rejected, s.getType().getId());
                }
            }
        }
        if (rejected.length() > 0) {
            workout.putMetadata(SAMPLES_NOT_PERSISTED, rejected.toString());
        }

        if (!store.isSupported() || toWrite.isEmpty()) {
            // Nowhere to persist it, but the record itself is still real
            // and the caller may want to upload it themselves. Report the
            // workout rather than failing the whole session.
            //
            // The marker goes on here too. A workout with no writable
            // child samples -- the ordinary no-sensor case on both mobile
            // platforms -- took this path and came back with neither an id
            // nor the signal that says why, which is the one thing a
            // caller needs in order to keep the record itself.
            if (!store.isWritable(HealthDataType.WORKOUT)) {
                workout.putMetadata(WORKOUT_NOT_PERSISTED, "true");
            }
            setState(WorkoutSessionState.ENDED);
            out.complete(workout);
            return;
        }
        store.write(toWrite).onResult(new AsyncResult<HealthWriteResult>() {
            @Override
            public void onReady(HealthWriteResult value, Throwable err) {
                if (err != null) {
                    setState(WorkoutSessionState.FAILED);
                    out.error(err);
                    return;
                }
                if (!value.getSampleIds().isEmpty()
                        && store.isWritable(HealthDataType.WORKOUT)) {
                    // The first id belongs to the workout only when the
                    // workout was part of the batch.
                    workout.setId(value.getSampleIds().get(0));
                } else if (!store.isWritable(HealthDataType.WORKOUT)) {
                    // Neither mobile platform accepts a workout through
                    // the sample write path in this release, so the child
                    // measurements are stored and the session record is
                    // not. Saying so through the returned sample beats
                    // resolving as though the workout had been persisted.
                    workout.putMetadata(WORKOUT_NOT_PERSISTED, "true");
                }
                setState(WorkoutSessionState.ENDED);
                out.complete(workout);
            }
        });
    }

    /// Adds `typeId` to the comma-separated rejected list, once.
    private static void noteRejected(StringBuilder rejected, String typeId) {
        String needle = "," + typeId + ",";
        if (("," + rejected.toString() + ",").indexOf(needle) >= 0) {
            return;
        }
        if (rejected.length() > 0) {
            rejected.append(',');
        }
        rejected.append(typeId);
    }

    /// Copies the rolled-up energy and distance onto the record, leaving
    /// them null when nothing was fed in -- see
    /// [WorkoutSession#getStatistic(HealthDataType,AggregateMetric)].
    private void applyTotals(WorkoutSample workout) {
        HealthQuantity energy = getStatistic(HealthDataType.ACTIVE_ENERGY,
                AggregateMetric.TOTAL);
        if (energy != null) {
            workout.setTotalEnergy(energy.in(HealthUnit.KILOCALORIE));
        }
        HealthQuantity distance = getStatistic(
                HealthDataType.DISTANCE_WALKING_RUNNING,
                AggregateMetric.TOTAL);
        if (distance == null) {
            distance = getStatistic(HealthDataType.DISTANCE_CYCLING,
                    AggregateMetric.TOTAL);
        }
        if (distance != null) {
            workout.setTotalDistance(distance.in(HealthUnit.METER));
        }
    }
}
