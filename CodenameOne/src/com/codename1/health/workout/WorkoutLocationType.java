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

/// Whether a workout happens indoors or outdoors.
///
/// A label in this release: nothing here drives a live workout session,
/// so it is recorded with the workout and read back rather than acted on.
/// It is more than a label on a live session -- on watchOS it selects
/// whether GPS is used, which materially changes both the distance
/// calculation and battery drain -- so set it correctly now and the
/// behaviour follows when live sessions arrive.
public enum WorkoutLocationType {

    /// Not specified; the platform decides.
    UNKNOWN,

    /// Indoors -- distance comes from motion sensors rather than GPS.
    INDOOR,

    /// Outdoors -- GPS is used where available.
    OUTDOOR
}
