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

    /// Set on the returned workout when the platform could not store the
    /// session record itself.
    ///
    /// Neither HealthKit nor the Health Connect bridge accepts a workout
    /// through the sample write path in this release. The child
    /// measurements are persisted; the workout is returned for the caller
    /// to keep or upload. Check for this rather than assuming getId() is
    /// populated.
    public static final String WORKOUT_NOT_PERSISTED =
            "cn1.workoutNotPersisted";

    @Override
    protected void doEnd(final AsyncResource<WorkoutSample> out) {
        final WorkoutSample workout = WorkoutSample.create(
                getConfiguration().getActivityType(), getStartedAtMillis(),
                getEndedAtMillis());
        workout.setActiveDurationMillis(getElapsedMillis());
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
        synchronized (collected) {
            for (HealthSample s : collected) {
                if (store.isWritable(s.getType())) {
                    toWrite.add(s);
                }
            }
        }

        if (!store.isSupported() || toWrite.isEmpty()) {
            // Nowhere to persist it, but the record itself is still real
            // and the caller may want to upload it themselves. Report the
            // workout rather than failing the whole session.
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
