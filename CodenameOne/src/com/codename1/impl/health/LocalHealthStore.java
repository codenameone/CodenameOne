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
package com.codename1.impl.health;

import com.codename1.health.AggregateMetric;
import com.codename1.health.AggregateQuery;
import com.codename1.health.AggregateResult;
import com.codename1.health.HealthAggregationStyle;
import com.codename1.health.HealthAuthorizationStatus;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthDeleteRequest;
import com.codename1.health.HealthQuantity;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthStore;
import com.codename1.health.HealthTimeRange;
import com.codename1.health.HealthUnit;
import com.codename1.health.HealthWriteResult;
import com.codename1.health.QuantitySample;
import com.codename1.health.SampleQuery;
import com.codename1.health.SamplePage;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// A health store held in the app's own process, for ports with no
/// platform health provider -- the desktop ports, the JavaScript port, and
/// the simulator's starting state.
///
/// #### What this is and is not
///
/// It is a real store: reads, writes, deletes and aggregates all work, and
/// on ports that persist it the data survives a restart. That makes it
/// genuinely useful -- aggregation logic, chart code and unit handling can
/// all be developed and unit-tested on a laptop.
///
/// It is **not** a platform health store, and it reports
/// [com.codename1.health.HealthAvailability#LOCAL_ONLY] so apps can tell.
/// Nothing else writes into it: no watch, no scale, no other app. An app
/// whose whole purpose is reading what other apps recorded should tell the
/// user the feature needs a phone rather than showing an empty chart.
///
/// Subclasses supply persistence; this base keeps everything in memory,
/// which is the right behaviour for a simulator that should start clean.
public class LocalHealthStore extends HealthStore {

    private final List<HealthSample> samples = new ArrayList<HealthSample>();
    private long nextId = 1;

    /// Creates an empty local store.
    public LocalHealthStore() {
    }

    public boolean isSupported() {
        return true;
    }

    /// Every type is available locally: there is no platform to restrict
    /// what can be stored.
    public boolean isTypeSupported(HealthDataType type) {
        return type != null;
    }

    public List<HealthDataType> getSupportedTypes() {
        return HealthDataType.values();
    }

    public boolean isWritable(HealthDataType type) {
        return type != null;
    }

    public boolean isDeletable(HealthDataType type) {
        return type != null;
    }

    /// Every metric is computed here in shared code rather than delegated
    /// to a platform engine.
    public List<AggregateMetric> getSupportedMetrics(HealthDataType type) {
        List<AggregateMetric> out = new ArrayList<AggregateMetric>();
        if (type == null) {
            return out;
        }
        out.add(AggregateMetric.COUNT);
        out.add(AggregateMetric.DURATION);
        if (type.getAggregationStyle() == HealthAggregationStyle.CUMULATIVE) {
            out.add(AggregateMetric.TOTAL);
        } else if (type.getAggregationStyle()
                == HealthAggregationStyle.DISCRETE) {
            out.add(AggregateMetric.AVERAGE);
            out.add(AggregateMetric.MINIMUM);
            out.add(AggregateMetric.MAXIMUM);
            out.add(AggregateMetric.LATEST);
        }
        return out;
    }

    /// There is no permission model on a local store, so both directions
    /// are honestly reported as authorized rather than as unknown.
    public HealthAuthorizationStatus getReadAuthorizationStatus(
            HealthDataType type) {
        return HealthAuthorizationStatus.AUTHORIZED;
    }

    public HealthAuthorizationStatus getWriteAuthorizationStatus(
            HealthDataType type) {
        return HealthAuthorizationStatus.AUTHORIZED;
    }

    protected void doRequestAuthorization(
            List<com.codename1.health.HealthAccess> access,
            AsyncResource<Boolean> out) {
        out.complete(Boolean.TRUE);
    }

    // ------------------------------------------------------------------
    // reads
    // ------------------------------------------------------------------

    protected void doReadSamples(SampleQuery query,
            AsyncResource<SamplePage> out) {
        HealthTimeRange range =
                query.getTimeRange().resolve(System.currentTimeMillis());
        List<HealthSample> matched = new ArrayList<HealthSample>();
        synchronized (samples) {
            for (int i = 0; i < samples.size(); i++) {
                HealthSample s = samples.get(i);
                if (matches(s, query, range)) {
                    matched.add(s);
                }
            }
        }
        sort(matched, query.isSortDescending());
        boolean truncated = matched.size() > query.getLimit();
        while (matched.size() > query.getLimit()) {
            matched.remove(matched.size() - 1);
        }
        // Everything is in memory, so a single page always satisfies the
        // query; there is no continuation token to hand back.
        out.complete(new SamplePage(matched, null, truncated));
    }

    private boolean matches(HealthSample s, SampleQuery query,
            HealthTimeRange range) {
        if (!query.getTypes().contains(s.getType())) {
            return false;
        }
        // A sample belongs to the range when it overlaps it, so an
        // interval straddling the boundary is not silently dropped.
        if (s.getEndMillis() < range.getStartMillis()
                || s.getStartMillis() >= range.getEndMillis()) {
            return false;
        }
        List<String> sources = query.getSources();
        if (!sources.isEmpty()) {
            if (s.getSource() == null
                    || !sources.contains(s.getSource().getBundleId())) {
                return false;
            }
        }
        return true;
    }

    private static void sort(List<HealthSample> list,
            final boolean descending) {
        Collections.sort(list, new Comparator<HealthSample>() {
            public int compare(HealthSample a, HealthSample b) {
                long d = a.getStartMillis() - b.getStartMillis();
                int r = d < 0 ? -1 : (d > 0 ? 1 : 0);
                return descending ? -r : r;
            }
        });
    }

    // ------------------------------------------------------------------
    // aggregates
    // ------------------------------------------------------------------

    protected void doAggregate(AggregateQuery query, long[] boundaries,
            AsyncResource<List<AggregateResult>> out) {
        HealthTimeRange range =
                query.getTimeRange().resolve(System.currentTimeMillis());
        List<AggregateResult> results = new ArrayList<AggregateResult>();
        for (int b = 0; b + 1 < boundaries.length; b++) {
            long start = boundaries[b];
            long end = boundaries[b + 1];
            AggregateResult bucket = new AggregateResult(start, end);
            for (int t = 0; t < query.getTypes().size(); t++) {
                aggregateInto(bucket, query, query.getTypes().get(t), start,
                        end, range);
            }
            results.add(bucket);
        }
        out.complete(results);
    }

    /// Computes one type's metrics over one bucket.
    ///
    /// Buckets with no data are left empty rather than filled with zeros,
    /// which is what lets a chart draw a gap where the user's phone was in
    /// a drawer instead of a flat line at zero.
    private void aggregateInto(AggregateResult bucket, AggregateQuery query,
            HealthDataType type, long start, long end,
            HealthTimeRange range) {
        HealthUnit unit = query.getUnit() != null ? query.getUnit()
                : type.getCanonicalUnit();
        List<QuantitySample> inBucket = new ArrayList<QuantitySample>();
        long durationMillis = 0;
        int count = 0;
        synchronized (samples) {
            for (int i = 0; i < samples.size(); i++) {
                HealthSample s = samples.get(i);
                if (s.getType() != type) {
                    continue;
                }
                if (s.getStartMillis() >= end
                        || s.getEndMillis() < start) {
                    continue;
                }
                if (s.getStartMillis() < range.getStartMillis()
                        || s.getStartMillis() >= range.getEndMillis()) {
                    continue;
                }
                if (!sourceAllowed(s, query)) {
                    continue;
                }
                count++;
                durationMillis += Math.max(1, s.getDurationMillis());
                if (s instanceof QuantitySample) {
                    inBucket.add((QuantitySample) s);
                }
            }
        }
        bucket.setSampleCount(type, count);
        if (count == 0) {
            return;
        }
        List<AggregateMetric> metrics = query.getMetrics();
        for (int m = 0; m < metrics.size(); m++) {
            AggregateMetric metric = metrics.get(m);
            if (metric == AggregateMetric.COUNT) {
                bucket.put(type, metric,
                        new HealthQuantity(count, HealthUnit.COUNT));
                continue;
            }
            if (metric == AggregateMetric.DURATION) {
                bucket.put(type, metric, new HealthQuantity(durationMillis,
                        HealthUnit.MILLISECOND));
                continue;
            }
            if (inBucket.isEmpty() || unit == null) {
                continue;
            }
            bucket.put(type, metric,
                    new HealthQuantity(compute(metric, inBucket, unit), unit));
        }
    }

    private boolean sourceAllowed(HealthSample s, AggregateQuery query) {
        List<String> sources = query.getSources();
        if (sources.isEmpty()) {
            return true;
        }
        return s.getSource() != null
                && sources.contains(s.getSource().getBundleId());
    }

    /// Applies one metric to the samples in a bucket.
    ///
    /// The average is duration-weighted, so a heart rate held across ten
    /// minutes counts for more than a single spot reading -- an
    /// unweighted mean over irregularly-sampled data is the classic way to
    /// make a chart disagree with the platform's own summary.
    private static double compute(AggregateMetric metric,
            List<QuantitySample> in, HealthUnit unit) {
        if (metric == AggregateMetric.TOTAL) {
            double sum = 0;
            for (int i = 0; i < in.size(); i++) {
                sum += in.get(i).getValue(unit);
            }
            return sum;
        }
        if (metric == AggregateMetric.MINIMUM) {
            double min = Double.MAX_VALUE;
            for (int i = 0; i < in.size(); i++) {
                min = Math.min(min, in.get(i).getValue(unit));
            }
            return min;
        }
        if (metric == AggregateMetric.MAXIMUM) {
            double max = -Double.MAX_VALUE;
            for (int i = 0; i < in.size(); i++) {
                max = Math.max(max, in.get(i).getValue(unit));
            }
            return max;
        }
        if (metric == AggregateMetric.LATEST) {
            QuantitySample latest = in.get(0);
            for (int i = 1; i < in.size(); i++) {
                if (in.get(i).getStartMillis() > latest.getStartMillis()) {
                    latest = in.get(i);
                }
            }
            return latest.getValue(unit);
        }
        // AVERAGE, duration-weighted.
        double weighted = 0;
        double totalWeight = 0;
        for (int i = 0; i < in.size(); i++) {
            QuantitySample s = in.get(i);
            double weight = Math.max(1, s.getDurationMillis());
            weighted += s.getValue(unit) * weight;
            totalWeight += weight;
        }
        return totalWeight == 0 ? 0 : weighted / totalWeight;
    }

    // ------------------------------------------------------------------
    // writes
    // ------------------------------------------------------------------

    protected void doWrite(List<HealthSample> toWrite,
            AsyncResource<HealthWriteResult> out) {
        HealthWriteResult result = new HealthWriteResult();
        synchronized (samples) {
            for (int i = 0; i < toWrite.size(); i++) {
                HealthSample s = toWrite.get(i);
                String id = "local-" + (nextId++);
                s.setId(id);
                samples.add(s);
                result.addSampleId(id);
            }
        }
        persist();
        out.complete(result);
    }

    protected void doDelete(HealthDeleteRequest request,
            AsyncResource<Integer> out) {
        int removed = 0;
        synchronized (samples) {
            if (request.isById()) {
                List<String> ids = request.getSampleIds();
                for (int i = samples.size() - 1; i >= 0; i--) {
                    if (ids.contains(samples.get(i).getId())) {
                        samples.remove(i);
                        removed++;
                    }
                }
            } else {
                HealthTimeRange range = request.getTimeRange()
                        .resolve(System.currentTimeMillis());
                for (int i = samples.size() - 1; i >= 0; i--) {
                    HealthSample s = samples.get(i);
                    if (request.getTypes().contains(s.getType())
                            && s.getStartMillis() >= range.getStartMillis()
                            && s.getStartMillis() < range.getEndMillis()) {
                        samples.remove(i);
                        removed++;
                    }
                }
            }
        }
        if (removed > 0) {
            persist();
        }
        out.complete(Integer.valueOf(removed));
    }

    // ------------------------------------------------------------------
    // extension points
    // ------------------------------------------------------------------

    /// Called after every mutation. The in-memory base does nothing;
    /// subclasses that persist override it.
    protected void persist() {
    }

    /// Inserts a sample without going through validation, for a subclass
    /// restoring persisted data or a simulator seeding a scripted dataset.
    protected final void addSampleDirect(HealthSample sample) {
        if (sample == null) {
            return;
        }
        synchronized (samples) {
            if (sample.getId() == null) {
                sample.setId("local-" + (nextId++));
            }
            samples.add(sample);
        }
    }

    /// Every stored sample, for subclasses persisting the store.
    protected final List<HealthSample> getAllSamples() {
        synchronized (samples) {
            return new ArrayList<HealthSample>(samples);
        }
    }

    /// Discards everything.
    public final void clear() {
        synchronized (samples) {
            samples.clear();
        }
        persist();
    }
}
