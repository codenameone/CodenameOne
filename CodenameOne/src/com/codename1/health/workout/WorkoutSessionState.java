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

/// The lifecycle of a [WorkoutSession].
///
/// Transitions are enforced: calling a method that does not apply to the
/// current state fails with
/// [com.codename1.health.HealthError#SESSION_STATE] rather than silently
/// doing nothing or throwing.
public enum WorkoutSessionState {

    /// Created but not yet prepared or started.
    NOT_STARTED,

    /// Sensors are warming up. Optional -- see
    /// [WorkoutSession#prepare()] -- and worth using if you show a
    /// countdown, because heart rate is not immediately available when a
    /// session begins.
    PREPARING,

    /// Recording.
    RUNNING,

    /// Paused. The elapsed clock stops; wall-clock time keeps passing and
    /// is excluded from [WorkoutSession#getElapsedMillis()].
    PAUSED,

    /// Stopping: collection has ceased but the workout has not been
    /// written yet.
    STOPPED,

    /// Ended and persisted.
    ENDED,

    /// Ended in error, or discarded.
    FAILED
}
