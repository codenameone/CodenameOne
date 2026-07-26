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

/// What shape of data a [HealthDataType] produces, and therefore which
/// [HealthSample] subclass a query for it returns.
public enum HealthDataKind {

    /// A numeric measurement with a unit -- steps, heart rate, body mass.
    /// Produces [QuantitySample]. Maps to `HKQuantitySample` and to Health
    /// Connect's numeric record types.
    QUANTITY,

    /// An enumerated observation with no natural unit -- menstrual flow,
    /// a single sleep stage. Produces [CategorySample]. Maps to
    /// `HKCategorySample`.
    CATEGORY,

    /// A run of measurements sharing one record identity -- a
    /// beat-to-beat heart-rate trace. Produces [SeriesSample].
    ///
    /// This kind exists because the two platforms disagree: Health
    /// Connect's `HeartRateRecord` is one record containing N samples,
    /// while HealthKit returns N independent samples. See
    /// [SampleQuery#setFlattenSeries(boolean)].
    SERIES,

    /// A bounded activity with child data -- a workout, a night's sleep.
    /// Produces a [SessionSample] subclass.
    SESSION
}
