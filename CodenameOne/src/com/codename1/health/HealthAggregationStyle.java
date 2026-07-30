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

/// How a [HealthDataType] combines over a time bucket. Determines which
/// [AggregateMetric] values are meaningful for the type.
public enum HealthAggregationStyle {

    /// Values accumulate over an interval and are summed -- steps,
    /// distance, energy burned. [AggregateMetric#TOTAL] is meaningful;
    /// averaging raw samples is not.
    ///
    /// Cumulative types are always interval samples: a step count belongs
    /// to a span of time, never to an instant. See
    /// [HealthDataType#isIntervalOnly()].
    CUMULATIVE,

    /// Values are point-in-time observations that are averaged or reduced
    /// -- heart rate, body mass, blood glucose. [AggregateMetric#AVERAGE],
    /// [AggregateMetric#MINIMUM], [AggregateMetric#MAXIMUM] and
    /// [AggregateMetric#LATEST] are meaningful; totalling them is not.
    DISCRETE,

    /// The type does not aggregate numerically at all -- sleep sessions,
    /// workouts, category observations. Only [AggregateMetric#COUNT] and
    /// [AggregateMetric#DURATION] apply.
    NONE
}
