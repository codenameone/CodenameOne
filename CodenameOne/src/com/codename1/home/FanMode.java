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

/// How a fan is running, for [Trait#FAN_MODE].
///
/// #### This one is heavily lossy on HomeKit
///
/// Matter's Fan Control cluster has a real mode attribute with seven values.
/// HomeKit has no equivalent: its fan service carries a speed percentage and a
/// two-state "manual or auto" characteristic, and off is expressed through the
/// power characteristic rather than as a mode.
///
/// So on iOS: writing [#LOW], [#MEDIUM] or [#HIGH] is translated into a
/// [Trait#FAN_SPEED] write of 33, 66 or 100 percent, and a **read never
/// returns them** -- it answers [#AUTO] or [#ON]. If your UI needs to show the
/// speed the user picked, read [Trait#FAN_SPEED] and render that; it works the
/// same everywhere.
public enum FanMode {

    /// Not running.
    OFF,

    /// Low speed. **Never read back on HomeKit**; see the class note.
    LOW,

    /// Medium speed. **Never read back on HomeKit**; see the class note.
    MEDIUM,

    /// High speed. **Never read back on HomeKit**; see the class note.
    HIGH,

    /// Running at whatever speed [Trait#FAN_SPEED] says.
    ON,

    /// The fan is choosing its own speed.
    AUTO,

    /// The fan is choosing its own speed using its own sensors and schedule.
    ///
    /// **Matter and Google Home only.** HomeKit reports [#AUTO] instead.
    SMART;

    /// Reads a fan mode out of a trait value, total: a value that is not an
    /// enum, or whose ordinal is outside this set, answers [#OFF].
    ///
    /// #### Parameters
    ///
    /// - `value`: a value read from [Trait#FAN_MODE], or `null`
    ///
    /// #### Returns
    ///
    /// the mode, never `null`
    public static FanMode of(TraitValue value) {
        if (value == null || value.getKind() != TraitValueKind.ENUM) {
            return OFF;
        }
        int ordinal = value.getEnumOrdinal();
        FanMode[] all = values();
        if (ordinal < 0 || ordinal >= all.length) {
            return OFF;
        }
        return all[ordinal];
    }
}
