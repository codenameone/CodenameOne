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

/// One stage within a [SleepSample]. The values mirror Health Connect's
/// eight stage constants; the iOS mapping is noted per value.
///
/// Not every source reports stages. Check
/// [SleepSample#hasStageDetail()] before drawing a hypnogram -- a session
/// recorded by a phone rather than a watch will only say "asleep".
public enum SleepStage {

    /// The source did not classify this span.
    UNKNOWN,

    /// Awake during the sleep session. HealthKit `awake`.
    AWAKE,

    /// Awake but in bed, before or after sleeping. HealthKit `inBed`.
    AWAKE_IN_BED,

    /// Out of bed during the session. **Android only** -- HealthKit has no
    /// equivalent and never reports it.
    OUT_OF_BED,

    /// Asleep, with no stage breakdown available. HealthKit
    /// `asleepUnspecified`, and the only asleep value iOS reported before
    /// iOS 16.
    ASLEEP_UNSPECIFIED,

    /// Light sleep. Mapped from HealthKit's `asleepCore`.
    ///
    /// This mapping is an **approximation**: Apple's "core" is that
    /// vendor's own classification and is not clinically identical to the
    /// N1+N2 light-sleep stages the Health Connect value denotes. Treat it
    /// as indicative rather than diagnostic.
    LIGHT,

    /// Deep sleep. HealthKit `asleepDeep`, iOS 16+.
    DEEP,

    /// REM sleep. HealthKit `asleepREM`, iOS 16+.
    REM
}
