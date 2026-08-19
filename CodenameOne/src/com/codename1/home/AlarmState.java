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
package com.codename1.home;

/// The state of a life-safety alarm, for [Trait#SMOKE_DETECTED] and
/// [Trait#CO_DETECTED]. Read-only.
///
/// #### Read the caveat before you build on this
///
/// This API is not a fire-alarm system and must not be the only thing standing
/// between a user and a fire. Notifications here arrive only while the app is
/// running -- see [TraitSubscription#isPushDelivery()] -- and a sleeping phone
/// is told nothing. Treat these values as information, never as a warning
/// channel.
public enum AlarmState {

    /// Nothing detected.
    NORMAL,

    /// Detected below the alarm threshold.
    ///
    /// **HomeKit never produces this**: its smoke and carbon-monoxide
    /// characteristics are two-state, so anything it detects arrives as
    /// [#CRITICAL].
    WARNING,

    /// Detected at or above the alarm threshold.
    CRITICAL,

    /// The accessory could not say.
    UNKNOWN;

    /// Reads an alarm state out of a trait value, total: a value that is not
    /// an enum, or whose ordinal is outside this set, answers [#UNKNOWN].
    ///
    /// #### Parameters
    ///
    /// - `value`: a value read from [Trait#SMOKE_DETECTED] or
    ///   [Trait#CO_DETECTED], or `null`
    ///
    /// #### Returns
    ///
    /// the state, never `null`
    public static AlarmState of(TraitValue value) {
        if (value == null || value.getKind() != TraitValueKind.ENUM) {
            return UNKNOWN;
        }
        int ordinal = value.getEnumOrdinal();
        AlarmState[] all = values();
        if (ordinal < 0 || ordinal >= all.length) {
            return UNKNOWN;
        }
        return all[ordinal];
    }
}
