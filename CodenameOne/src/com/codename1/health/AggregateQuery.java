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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Describes a bucketed summary read against [HealthStore].
///
/// ```java
/// AggregateQuery q = new AggregateQuery()
///         .addType(HealthDataType.STEPS)
///         .addMetric(AggregateMetric.TOTAL)
///         .setTimeRange(HealthTimeRange.calendarDays(7, ZoneId.systemDefault()))
///         .setBucket(HealthInterval.calendarDays(1, ZoneId.systemDefault()));
/// ```
///
/// #### Overlapping sources are counted twice, on every platform
///
/// When a phone and a watch both record steps for the same walk, the store
/// holds two overlapping sets of samples, and a total over them counts the
/// walk twice.
///
/// **This includes iOS.** HealthKit's statistics engine does de-duplicate
/// overlapping sources -- but no port uses it in this release. Every
/// metric here is computed by shared code from raw samples read back
/// through an ordinary query, so that the bucket arithmetic has one
/// implementation rather than one per platform that can drift. iOS
/// therefore double-counts exactly as Android does. A port that grows a
/// native aggregate path would change that, and this note with it.
///
/// This API does not paper over the overlap with a heuristic
/// de-duplicator -- guessing which of two overlapping sources is
/// authoritative is exactly the kind of silent wrongness health data
/// cannot afford. Use [#addSource(String)] to pin the query to the source
/// you trust, and tell the user which device a figure came from.
public final class AggregateQuery {

    private final List<HealthDataType> types = new ArrayList<HealthDataType>();
    private final List<AggregateMetric> metrics =
            new ArrayList<AggregateMetric>();
    private final List<String> sources = new ArrayList<String>();
    private HealthTimeRange timeRange;
    private HealthInterval bucket;
    private HealthUnit unit;

    /// Adds a type to summarize. At least one is required.
    public AggregateQuery addType(HealthDataType type) {
        if (type != null && !types.contains(type)) {
            types.add(type);
        }
        return this;
    }

    /// The types this query summarizes.
    public List<HealthDataType> getTypes() {
        return Collections.unmodifiableList(types);
    }

    /// Adds a metric to compute. At least one is required.
    public AggregateQuery addMetric(AggregateMetric metric) {
        if (metric != null && !metrics.contains(metric)) {
            metrics.add(metric);
        }
        return this;
    }

    /// The metrics this query computes.
    public List<AggregateMetric> getMetrics() {
        return Collections.unmodifiableList(metrics);
    }

    /// Restricts the summary to one writing app -- see the double-counting
    /// warning on this class.
    public AggregateQuery addSource(String bundleId) {
        if (bundleId != null && !sources.contains(bundleId)) {
            sources.add(bundleId);
        }
        return this;
    }

    /// The source filter, empty when unrestricted.
    public List<String> getSources() {
        return Collections.unmodifiableList(sources);
    }

    /// The span to summarize. Required.
    public AggregateQuery setTimeRange(HealthTimeRange timeRange) {
        this.timeRange = timeRange;
        return this;
    }

    /// The span this query summarizes.
    public HealthTimeRange getTimeRange() {
        return timeRange;
    }

    /// Splits the range into buckets of this width. Leave unset for a
    /// single bucket covering the whole range.
    ///
    /// Prefer [HealthInterval#calendarDays(int,java.time.ZoneId)] over a
    /// fixed 24-hour width whenever the buckets are labelled with dates in
    /// your UI -- see [HealthInterval].
    public AggregateQuery setBucket(HealthInterval bucket) {
        this.bucket = bucket;
        return this;
    }

    /// The bucket width, or null for one bucket over the whole range.
    public HealthInterval getBucket() {
        return bucket;
    }

    /// Returns aggregated values in `unit` rather than each type's
    /// canonical unit.
    public AggregateQuery setUnit(HealthUnit unit) {
        this.unit = unit;
        return this;
    }

    /// The requested unit, or null for canonical units.
    public HealthUnit getUnit() {
        return unit;
    }

    /// Validates the query and throws if it cannot be run.
    ///
    /// #### Throws
    ///
    /// - `HealthException`: [HealthError#INVALID_ARGUMENT] for a missing
    ///   type, metric or range, or for a metric that is meaningless for
    ///   the requested type; [HealthError#UNIT_MISMATCH] for an
    ///   incompatible unit.
    public void validate() throws HealthException {
        if (types.isEmpty()) {
            throw new HealthException(HealthError.INVALID_ARGUMENT,
                    "an aggregate query needs at least one data type");
        }
        if (metrics.isEmpty()) {
            throw new HealthException(HealthError.INVALID_ARGUMENT,
                    "an aggregate query needs at least one metric");
        }
        if (timeRange == null) {
            throw new HealthException(HealthError.INVALID_ARGUMENT,
                    "an aggregate query needs a time range");
        }
        for (HealthDataType t : types) {
            for (AggregateMetric m : metrics) {
                if (!isMeaningful(t, m)) {
                    throw new HealthException(HealthError.INVALID_ARGUMENT,
                            m + " is not meaningful for " + t.getId()
                                    + ", which aggregates as "
                                    + t.getAggregationStyle());
                }
            }
            if (unit != null) {
                HealthUnit canonical = t.getCanonicalUnit();
                if (canonical == null || !canonical.isCompatibleWith(unit)) {
                    throw new HealthException(HealthError.UNIT_MISMATCH,
                            "cannot express " + t.getId() + " in "
                                    + unit.getSymbol());
                }
            }
        }
    }

    /// Whether `metric` says anything true about `type`.
    ///
    /// [AggregateMetric#COUNT] and [AggregateMetric#DURATION] apply to
    /// everything. Summing a discrete series -- the total of every body
    /// mass ever recorded -- and averaging a cumulative one -- the mean of
    /// arbitrarily-chunked step totals -- are both meaningless, so they
    /// are rejected rather than answered.
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public static boolean isMeaningful(HealthDataType type,
            AggregateMetric metric) {
        if (type == null || metric == null) {
            return false;
        }
        if (metric == AggregateMetric.COUNT
                || metric == AggregateMetric.DURATION) {
            return true;
        }
        // Composite types carry more than one number, so there is no single
        // value to average or compare. Accepting the metric and returning
        // null looked like "no data" for a bucket that genuinely had
        // readings, which is the worse of the two answers.
        if (type.getKind() == HealthDataKind.COMPOSITE) {
            return false;
        }
        HealthAggregationStyle style = type.getAggregationStyle();
        if (style == HealthAggregationStyle.CUMULATIVE) {
            return metric == AggregateMetric.TOTAL;
        }
        if (style == HealthAggregationStyle.DISCRETE) {
            return metric == AggregateMetric.AVERAGE
                    || metric == AggregateMetric.MINIMUM
                    || metric == AggregateMetric.MAXIMUM
                    || metric == AggregateMetric.LATEST;
        }
        return false;
    }
}
