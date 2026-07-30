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

/// What to compute over an aggregation bucket. Which metrics are
/// meaningful depends on the type's [HealthAggregationStyle]; asking for a
/// nonsensical combination -- the total of a body-mass series, say --
/// fails with [HealthError#INVALID_ARGUMENT] rather than returning a
/// number nobody should trust.
public enum AggregateMetric {

    /// The sum across the bucket. Meaningful for
    /// [HealthAggregationStyle#CUMULATIVE] types only.
    TOTAL,

    /// The mean across the bucket. Meaningful for
    /// [HealthAggregationStyle#DISCRETE] types.
    ///
    /// Interval samples are weighted by duration and instantaneous samples
    /// are weighted equally, so a heart rate held for ten minutes counts
    /// for more than a single spot reading.
    AVERAGE,

    /// The smallest value in the bucket. [HealthAggregationStyle#DISCRETE]
    /// types.
    MINIMUM,

    /// The largest value in the bucket. [HealthAggregationStyle#DISCRETE]
    /// types.
    MAXIMUM,

    /// How many samples fell in the bucket. Meaningful for every type.
    COUNT,

    /// The total time covered by samples in the bucket, in milliseconds.
    /// Meaningful for every type; the natural metric for sessions.
    DURATION,

    /// The most recent value in the bucket.
    /// [HealthAggregationStyle#DISCRETE] types.
    LATEST
}
