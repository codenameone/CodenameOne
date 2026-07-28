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
import com.codename1.health.HealthException;

/// Receives workout lifecycle and statistics updates. All callbacks arrive
/// on the EDT.
public interface WorkoutSessionListener {

    /// The session moved to a new state.
    ///
    /// Every transition is one the app asked for in this release, since
    /// recorded sessions are the only kind here. The callback exists for
    /// the unrequested ones a live session brings -- watchOS ends one when
    /// the wearer starts another workout -- so that code written against
    /// it keeps working when that arrives.
    void workoutStateChanged(WorkoutSession session,
            WorkoutSessionState state);

    /// A live statistic changed. Read the new value with
    /// [WorkoutSession#getStatistic(HealthDataType,
    /// com.codename1.health.AggregateMetric)].
    void workoutStatisticsUpdated(WorkoutSession session,
            HealthDataType type);

    /// An event was recorded, whether by the app or detected by the
    /// platform.
    void workoutEvent(WorkoutSession session, WorkoutEvent event);

    /// The session failed and will not continue.
    void workoutFailed(WorkoutSession session, HealthException error);
}
