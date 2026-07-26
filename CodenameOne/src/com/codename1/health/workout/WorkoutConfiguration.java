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

import com.codename1.health.HealthDataType;
import com.codename1.health.WorkoutActivityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Describes a workout about to be started.
public final class WorkoutConfiguration {

    private WorkoutActivityType activityType = WorkoutActivityType.OTHER;
    private WorkoutLocationType locationType = WorkoutLocationType.UNKNOWN;
    private boolean keepAliveInBackground;
    private final List<HealthDataType> collectedTypes =
            new ArrayList<HealthDataType>();
    private String title;

    /// The kind of exercise. Defaults to [WorkoutActivityType#OTHER].
    public WorkoutConfiguration setActivityType(
            WorkoutActivityType activityType) {
        this.activityType = activityType == null
                ? WorkoutActivityType.OTHER : activityType;
        return this;
    }

    /// The configured activity.
    public WorkoutActivityType getActivityType() {
        return activityType;
    }

    /// Indoors or outdoors -- see [WorkoutLocationType], which affects
    /// whether GPS is used.
    public WorkoutConfiguration setLocationType(
            WorkoutLocationType locationType) {
        this.locationType = locationType == null
                ? WorkoutLocationType.UNKNOWN : locationType;
        return this;
    }

    /// The configured location type.
    public WorkoutLocationType getLocationType() {
        return locationType;
    }

    /// Asks the operating system to keep the app running while the workout
    /// records.
    ///
    /// On watchOS and iOS 26 the workout session itself holds the process
    /// alive and this needs nothing further. **On Android it requires a
    /// foreground service**, so the app must set the
    /// `android.health.foregroundService` build hint -- without it,
    /// starting such a session fails configuration validation rather than
    /// being silently killed mid-run.
    public WorkoutConfiguration setKeepAliveInBackground(
            boolean keepAliveInBackground) {
        this.keepAliveInBackground = keepAliveInBackground;
        return this;
    }

    /// `true` when background recording was requested.
    public boolean isKeepAliveInBackground() {
        return keepAliveInBackground;
    }

    /// Asks the platform to collect this type automatically during the
    /// workout. Honoured only where
    /// [WorkoutManager#isSensorCollectionSupported()] is `true`; elsewhere
    /// you must feed samples in yourself with
    /// [WorkoutSession#addSamples(java.util.List)].
    public WorkoutConfiguration addCollectedType(HealthDataType type) {
        if (type != null && !collectedTypes.contains(type)) {
            collectedTypes.add(type);
        }
        return this;
    }

    /// The types requested for automatic collection.
    public List<HealthDataType> getCollectedTypes() {
        return Collections.unmodifiableList(collectedTypes);
    }

    /// A user-visible title for the workout.
    public WorkoutConfiguration setTitle(String title) {
        this.title = title;
        return this;
    }

    /// The configured title, or null.
    public String getTitle() {
        return title;
    }
}
