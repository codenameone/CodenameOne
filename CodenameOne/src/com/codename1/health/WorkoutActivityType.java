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

/// What kind of exercise a workout records.
///
/// #### A deliberate subset
///
/// HealthKit defines around eighty activity types and Health Connect
/// around ninety, and the two lists do not line up. An exhaustive mapping
/// table would be large, unverifiable, and wrong in ways nobody would
/// notice until a user's yoga session showed up as pilates.
///
/// So this enum covers the common activities honestly and offers [#OTHER]
/// plus [WorkoutSample#getPlatformCode()] as the escape hatch for apps
/// that need exact platform fidelity. Reading `getPlatformCode()` gives
/// you the raw `HKWorkoutActivityType` or `ExerciseSessionRecord` constant
/// the platform actually recorded.
public enum WorkoutActivityType {

    /// An activity this enum does not name. Check
    /// [WorkoutSample#getPlatformCode()] for the platform's own value.
    OTHER,

    WALKING,
    RUNNING,
    HIKING,
    CYCLING,
    MOUNTAIN_BIKING,
    SWIMMING,
    ROWING,
    ELLIPTICAL,
    STAIR_CLIMBING,
    HIGH_INTENSITY_INTERVAL_TRAINING,
    STRENGTH_TRAINING,
    CORE_TRAINING,
    FUNCTIONAL_TRAINING,
    YOGA,
    PILATES,
    DANCE,
    MARTIAL_ARTS,
    BOXING,
    CLIMBING,
    SKIING,
    SNOWBOARDING,
    SKATING,
    SURFING,
    PADDLING,
    GOLF,
    TENNIS,
    BADMINTON,
    SQUASH,
    TABLE_TENNIS,
    BASKETBALL,
    FOOTBALL,
    AMERICAN_FOOTBALL,
    BASEBALL,
    VOLLEYBALL,
    CRICKET,
    RUGBY,
    WHEELCHAIR,
    COOLDOWN,
    STRETCHING
}
