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

/// Describes a read against [HealthStore]. Fluent setters return `this`.
///
/// ```java
/// SampleQuery q = new SampleQuery()
///         .addType(HealthDataType.HEART_RATE)
///         .setTimeRange(HealthTimeRange.lastHours(24))
///         .setSortDescending(true)
///         .setLimit(500);
/// ```
///
/// #### Always set a limit for high-frequency types
///
/// A year of continuous heart rate is on the order of half a million
/// samples. The default limit of 10,000 exists so that a naive query
/// cannot exhaust the heap on a phone; raise it deliberately, and prefer
/// paging through [HealthStore#readSamplePage(SampleQuery)] over asking
/// for everything at once.
public final class SampleQuery {

    /// The limit applied when none is set.
    public static final int DEFAULT_LIMIT = 10000;

    private final List<HealthDataType> types = new ArrayList<HealthDataType>();
    private final List<String> sources = new ArrayList<String>();
    private HealthTimeRange timeRange;
    private int limit = DEFAULT_LIMIT;
    private boolean sortDescending;
    private HealthUnit unit;
    private boolean flattenSeries = true;
    private long sleepSessionGapMillis = 15 * 60 * 1000L;
    private String pageToken;

    /// Adds a type to read. At least one is required.
    public SampleQuery addType(HealthDataType type) {
        if (type != null && !types.contains(type)) {
            types.add(type);
        }
        return this;
    }

    /// The types this query reads.
    public List<HealthDataType> getTypes() {
        return Collections.unmodifiableList(types);
    }

    /// Restricts the query to samples written by one app, identified by
    /// its bundle id or package name -- see [HealthSource#getBundleId()].
    /// Call more than once to allow several.
    ///
    /// Worth doing when a phone and a watch both record the same activity:
    /// see the double-counting warning on [AggregateQuery].
    public SampleQuery addSource(String bundleId) {
        if (bundleId != null && !sources.contains(bundleId)) {
            sources.add(bundleId);
        }
        return this;
    }

    /// The source filter, empty when unrestricted.
    public List<String> getSources() {
        return Collections.unmodifiableList(sources);
    }

    /// The span to read. Required.
    public SampleQuery setTimeRange(HealthTimeRange timeRange) {
        this.timeRange = timeRange;
        return this;
    }

    /// The span this query reads, or null when unset.
    public HealthTimeRange getTimeRange() {
        return timeRange;
    }

    /// Caps how many samples come back. Must be positive.
    public SampleQuery setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    /// The sample cap.
    public int getLimit() {
        return limit;
    }

    /// Returns the newest samples first. Default is oldest first.
    public SampleQuery setSortDescending(boolean sortDescending) {
        this.sortDescending = sortDescending;
        return this;
    }

    /// `true` when results come back newest first.
    public boolean isSortDescending() {
        return sortDescending;
    }

    /// Returns values in `unit` instead of the type's canonical unit.
    ///
    /// #### Throws
    ///
    /// The unit is validated when the query runs, not here: a unit that
    /// measures the wrong dimension fails with
    /// [HealthError#UNIT_MISMATCH] before the platform is touched.
    public SampleQuery setUnit(HealthUnit unit) {
        this.unit = unit;
        return this;
    }

    /// The requested unit, or null for each type's canonical unit.
    public HealthUnit getUnit() {
        return unit;
    }

    /// Whether to expand [SeriesSample] records into individual
    /// [QuantitySample] objects. Defaults to `true`.
    ///
    /// Leave it on and both platforms return the same thing, so your code
    /// is identical across them. Turn it off when you need a series'
    /// record identity -- to delete it, for instance -- and accept that
    /// iOS, which has no such grouping, returns series of size 1.
    public SampleQuery setFlattenSeries(boolean flattenSeries) {
        this.flattenSeries = flattenSeries;
        return this;
    }

    /// `true` when series are expanded into individual samples.
    public boolean isFlattenSeries() {
        return flattenSeries;
    }

    /// The gap that separates two sleep sessions, for the iOS port's
    /// session reassembly. Defaults to 15 minutes.
    ///
    /// HealthKit stores sleep as a run of category samples with no session
    /// object, so the port groups samples separated by less than this gap
    /// into one [SleepSample]. Raise it if your users nap; lower it if
    /// you would rather see brief wakings split the night. Ignored on
    /// Android, where sessions are stored natively.
    public SampleQuery setSleepSessionGapMillis(long gapMillis) {
        this.sleepSessionGapMillis = gapMillis;
        return this;
    }

    /// The sleep-session grouping gap in milliseconds.
    public long getSleepSessionGapMillis() {
        return sleepSessionGapMillis;
    }

    /// Continues a previous read from [SamplePage#getNextPageToken()].
    public SampleQuery setPageToken(String pageToken) {
        this.pageToken = pageToken;
        return this;
    }

    /// The continuation token, or null to start from the beginning.
    public String getPageToken() {
        return pageToken;
    }

    /// Validates the query and throws if it cannot be run.
    ///
    /// Called by [HealthStore] before the platform is touched, so a
    /// malformed query fails immediately and locally rather than as an
    /// opaque platform error later.
    ///
    /// #### Throws
    ///
    /// - `HealthException`: with [HealthError#INVALID_ARGUMENT] for a
    ///   missing type or range or a non-positive limit, and
    ///   [HealthError#UNIT_MISMATCH] when the requested unit does not
    ///   match a requested type's dimension.
    public void validate() throws HealthException {
        if (types.isEmpty()) {
            throw new HealthException(HealthError.INVALID_ARGUMENT,
                    "a sample query needs at least one data type");
        }
        if (timeRange == null) {
            throw new HealthException(HealthError.INVALID_ARGUMENT,
                    "a sample query needs a time range");
        }
        if (limit < 1) {
            throw new HealthException(HealthError.INVALID_ARGUMENT,
                    "limit must be positive, got " + limit);
        }
        if (unit != null) {
            for (HealthDataType t : types) {
                HealthUnit canonical = t.getCanonicalUnit();
                if (canonical == null) {
                    throw new HealthException(HealthError.UNIT_MISMATCH,
                            t.getId() + " has no numeric value, so the query"
                                    + " unit " + unit.getSymbol()
                                    + " does not apply to it");
                }
                if (!canonical.isCompatibleWith(unit)) {
                    throw new HealthException(HealthError.UNIT_MISMATCH,
                            t.getId() + " is measured in "
                                    + canonical.getDimension() + " but "
                                    + unit.getSymbol() + " measures "
                                    + unit.getDimension());
                }
            }
        }
    }
}
