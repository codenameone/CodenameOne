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

/// A completed workout: what kind, how long, and the totals the platform
/// computed for it.
///
/// Totals are nullable on purpose. A workout recorded without a heart-rate
/// sensor has no energy total, and reporting that as zero would put a
/// false 0 kcal into every summary. `null` means "not measured"; zero
/// means "measured, and it was zero".
public final class WorkoutSample extends SessionSample {

    private final WorkoutActivityType activityType;
    private int platformCode = -1;
    private HealthQuantity totalEnergy;
    private HealthQuantity totalDistance;
    private long activeDurationMillis = -1;

    private WorkoutSample(WorkoutActivityType activityType, long startMillis,
            long endMillis) {
        super(HealthDataType.WORKOUT, startMillis, endMillis);
        this.activityType = activityType == null
                ? WorkoutActivityType.OTHER : activityType;
    }

    /// Creates a workout spanning `[startMillis, endMillis]`.
    /// Metadata key set on a workout the platform could not store as a
    /// session record of its own.
    ///
    /// Neither HealthKit nor the Health Connect bridge accepts a workout
    /// through the sample write path in this release. The child
    /// measurements are persisted; the workout comes back for you to keep
    /// or upload. Check for this rather than assuming [#getId()] is
    /// populated.
    ///
    /// ```java
    /// if (workout.getMetadata().containsKey(
    ///         WorkoutSample.WORKOUT_NOT_PERSISTED)) {
    ///     uploadToMyServer(workout);
    /// }
    /// ```
    public static final String WORKOUT_NOT_PERSISTED =
            "cn1.workoutNotPersisted";

    /// Metadata key naming the data types the platform refused to store,
    /// comma separated, or absent when everything fed in was persisted.
    ///
    /// Health Connect has no single-value write form for the
    /// series-shaped types -- power, speed and both cadences -- which is
    /// exactly what a bike or foot pod feeds into a workout. Those samples
    /// cannot be stored there, so the workout names them rather than
    /// dropping them silently and resolving as though nothing had
    /// happened.
    public static final String SAMPLES_NOT_PERSISTED =
            "cn1.workout.samplesNotPersisted";

    public static WorkoutSample create(WorkoutActivityType activityType,
            long startMillis, long endMillis) {
        return new WorkoutSample(activityType, startMillis, endMillis);
    }

    /// The activity, mapped onto this API's shared vocabulary. See
    /// [#getPlatformCode()] when you need exactly what the platform said.
    public WorkoutActivityType getActivityType() {
        return activityType;
    }

    /// The raw platform activity constant -- an `HKWorkoutActivityType` on
    /// iOS or an `ExerciseSessionRecord` exercise type on Android -- or
    /// `-1` when unknown.
    ///
    /// This is the fidelity escape hatch for the deliberately partial
    /// [WorkoutActivityType] vocabulary. It is platform-specific by
    /// definition: the same integer means different things on the two
    /// platforms, so branch on [#getActivityType()] first and only reach
    /// for this when you must.
    public int getPlatformCode() {
        return platformCode;
    }

    /// Records the raw platform activity constant. Called by ports.
    public void setPlatformCode(int platformCode) {
        this.platformCode = platformCode;
    }

    /// Energy burned across the workout, or null when not measured.
    public HealthQuantity getTotalEnergy() {
        return totalEnergy;
    }

    /// Sets the energy total.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the quantity does not measure
    ///   energy.
    public void setTotalEnergy(HealthQuantity totalEnergy) {
        requireDimension(totalEnergy, HealthUnit.KILOCALORIE, "energy");
        this.totalEnergy = totalEnergy;
    }

    /// Distance covered, or null when not measured.
    public HealthQuantity getTotalDistance() {
        return totalDistance;
    }

    /// Sets the distance total.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the quantity does not measure
    ///   length.
    public void setTotalDistance(HealthQuantity totalDistance) {
        requireDimension(totalDistance, HealthUnit.METER, "distance");
        this.totalDistance = totalDistance;
    }

    /// A workout total in the wrong dimension is not caught anywhere else.
    ///
    /// The store writes whatever it is handed, so a total energy given in
    /// kilograms round-tripped through the local and simulator stores
    /// intact and only failed much later, in the caller that did the
    /// documented thing and asked for the value in kilocalories. Rejecting
    /// it at the setter puts the exception on the line that made the
    /// mistake -- the same reason [BloodPressureSample] checks its
    /// readings. Null stays legal: it is how a workout says the total was
    /// never measured.
    private static void requireDimension(HealthQuantity q, HealthUnit expected,
            String which) {
        if (q != null && !q.getUnit().isCompatibleWith(expected)) {
            throw new IllegalArgumentException("a workout " + which
                    + " total must measure " + expected.getDimension()
                    + ", but " + q.getUnit().getSymbol() + " measures "
                    + q.getUnit().getDimension());
        }
    }

    /// Time actually exercising, excluding pauses. Falls back to the wall
    /// duration when the platform did not report it separately.
    public long getActiveDurationMillis() {
        return activeDurationMillis < 0
                ? getDurationMillis() : activeDurationMillis;
    }

    /// Sets the paused-time-excluded duration.
    public void setActiveDurationMillis(long activeDurationMillis) {
        this.activeDurationMillis = activeDurationMillis;
    }

    @Override
    public String toString() {
        return "WorkoutSample[" + activityType + " " + getStartMillis()
                + ".." + getEndMillis() + "]";
    }
}
