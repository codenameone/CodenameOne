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

/// What a thermostat is doing, for [Trait#CURRENT_HEATING_COOLING], or what it
/// has been asked to do, for [Trait#TARGET_HEATING_COOLING].
public enum HeatingCoolingMode {

    /// Idle.
    OFF,

    /// Heating.
    HEAT,

    /// Cooling.
    COOL,

    /// Maintaining a band between [Trait#TARGET_HEATING_TEMPERATURE] and
    /// [Trait#TARGET_COOLING_TEMPERATURE], choosing for itself which to run.
    ///
    /// A **target** only. [Trait#CURRENT_HEATING_COOLING] never reports it,
    /// because at any given moment the thermostat is heating, cooling or
    /// idle -- "auto" describes the policy, not the state.
    ///
    /// While this is the target, [Trait#TARGET_TEMPERATURE] reports no value:
    /// there is no single setpoint to report, and answering with either
    /// threshold would silently be the wrong one half the time.
    AUTO,

    /// A mode this API cannot name, carried so it is not misreported as
    /// something it is not.
    ///
    /// Matter's thermostat has `EmergencyHeat`, `Precooling`, `FanOnly`,
    /// `Dry` and `Sleep`, none of which HomeKit can express and none of which
    /// have a portable meaning. Folding them into [#OFF] would tell an app a
    /// running dehumidifier is idle. Read
    /// [TraitValue#getRawPlatformValue()] for Matter's own ordinal when you
    /// need to know which.
    ///
    /// **Writing this is rejected** with [HomeError#INVALID_ARGUMENT] -- an
    /// API that cannot name a mode cannot ask for it either.
    OTHER;

    /// Reads a mode out of a trait value, total: a value that is not an enum,
    /// or whose ordinal is outside this set, answers [#OFF].
    ///
    /// [#OFF] rather than [#OTHER] as the fallback, deliberately: [#OTHER]
    /// means "the accessory named a mode we cannot express", which is a claim
    /// about the accessory. A value this code could not read at all is not
    /// evidence of that.
    ///
    /// #### Parameters
    ///
    /// - `value`: a value read from one of the heating-cooling traits, or
    ///   `null`
    ///
    /// #### Returns
    ///
    /// the mode, never `null`
    public static HeatingCoolingMode of(TraitValue value) {
        if (value == null || value.getKind() != TraitValueKind.ENUM) {
            return OFF;
        }
        int ordinal = value.getEnumOrdinal();
        HeatingCoolingMode[] all = values();
        if (ordinal < 0 || ordinal >= all.length) {
            return OFF;
        }
        return all[ordinal];
    }

    /// Whether this mode may be written to [Trait#TARGET_HEATING_COOLING].
    ///
    /// #### Returns
    ///
    /// `true` for everything except [#OTHER]
    public boolean isWritable() {
        return this != OTHER;
    }
}
