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
    public void setTotalEnergy(HealthQuantity totalEnergy) {
        this.totalEnergy = totalEnergy;
    }

    /// Distance covered, or null when not measured.
    public HealthQuantity getTotalDistance() {
        return totalDistance;
    }

    /// Sets the distance total.
    public void setTotalDistance(HealthQuantity totalDistance) {
        this.totalDistance = totalDistance;
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
