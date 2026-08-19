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

/// A summarized air-quality rating, for [Trait#AIR_QUALITY]. Read-only.
///
/// #### The two backends do not agree on how many levels there are
///
/// HomeKit has six -- unknown, excellent, good, fair, inferior, poor -- and
/// Matter's Air Quality cluster has seven: unknown, good, fair, moderate,
/// poor, very poor, extremely poor. There is no mapping between them that
/// loses nothing.
///
/// This enum follows Matter, because collapsing seven levels into six discards
/// a distinction the accessory made, while spreading six across seven only
/// leaves a gap. So HomeKit's excellent becomes [#GOOD], good becomes [#FAIR],
/// fair becomes [#MODERATE], inferior becomes [#POOR], poor becomes
/// [#VERY_POOR], and **HomeKit never produces [#EXTREMELY_POOR]**.
///
/// That is a judgment call and it is visible as one: read
/// [TraitValue#getRawPlatformValue()] for the platform's own ordinal if your
/// app needs to apply its own scale. Do not hard-code thresholds against these
/// constants and expect them to mean the same air on both platforms.
public enum AirQualityLevel {

    /// The accessory has not measured yet, or could not.
    UNKNOWN,

    /// Good. HomeKit's "excellent" also arrives here.
    GOOD,

    /// Fair. HomeKit's "good" also arrives here.
    FAIR,

    /// Moderate. HomeKit's "fair" arrives here.
    MODERATE,

    /// Poor. HomeKit's "inferior" arrives here.
    POOR,

    /// Very poor. HomeKit's "poor" arrives here.
    VERY_POOR,

    /// Extremely poor. **Matter and Google Home only** -- HomeKit's scale
    /// stops one level short.
    EXTREMELY_POOR;

    /// Reads an air-quality level out of a trait value, total: a value that
    /// is not an enum, or whose ordinal is outside this set, answers
    /// [#UNKNOWN].
    ///
    /// #### Parameters
    ///
    /// - `value`: a value read from [Trait#AIR_QUALITY], or `null`
    ///
    /// #### Returns
    ///
    /// the level, never `null`
    public static AirQualityLevel of(TraitValue value) {
        if (value == null || value.getKind() != TraitValueKind.ENUM) {
            return UNKNOWN;
        }
        int ordinal = value.getEnumOrdinal();
        AirQualityLevel[] all = values();
        if (ordinal < 0 || ordinal >= all.length) {
            return UNKNOWN;
        }
        return all[ordinal];
    }
}
