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

/// A marked moment inside a workout -- a lap, a segment boundary, a
/// user-placed marker.
public final class WorkoutEvent {

    /// What kind of moment an event marks.
    public enum Kind {
        /// A lap boundary.
        LAP,
        /// A user-placed marker.
        MARKER,
        /// The start or end of a named segment.
        SEGMENT,
        /// The session paused.
        PAUSE,
        /// The session resumed.
        RESUME,
        /// The platform detected that the user stopped moving.
        MOTION_PAUSED,
        /// The platform detected that the user resumed moving.
        MOTION_RESUMED
    }

    private final Kind kind;
    private final long timestampMillis;
    private final String label;

    /// Creates an event at a moment.
    public WorkoutEvent(Kind kind, long timestampMillis, String label) {
        if (kind == null) {
            throw new IllegalArgumentException("an event requires a kind");
        }
        this.kind = kind;
        this.timestampMillis = timestampMillis;
        this.label = label;
    }

    /// A lap boundary at `timestampMillis`.
    public static WorkoutEvent lap(long timestampMillis) {
        return new WorkoutEvent(Kind.LAP, timestampMillis, null);
    }

    /// A user-placed marker at `timestampMillis`.
    public static WorkoutEvent marker(long timestampMillis, String label) {
        return new WorkoutEvent(Kind.MARKER, timestampMillis, label);
    }

    /// What this event marks.
    public Kind getKind() {
        return kind;
    }

    /// When it happened, epoch millis UTC.
    public long getTimestampMillis() {
        return timestampMillis;
    }

    /// A user-visible label, or null.
    public String getLabel() {
        return label;
    }

    public String toString() {
        return "WorkoutEvent[" + kind + " @" + timestampMillis + "]";
    }
}
