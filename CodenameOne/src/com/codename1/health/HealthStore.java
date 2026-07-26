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

import com.codename1.io.Preferences;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.util.AsyncResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Reads, writes and watches the platform health store -- HealthKit on
/// iOS, Health Connect on Android, a local store elsewhere.
///
/// Obtain one from [Health#getStore()]; it is never null. On a port with
/// no health support this base class is returned as-is and every operation
/// fails fast with [HealthError#NOT_SUPPORTED].
///
/// #### Division of labour
///
/// The public methods here are `final`. They validate requests, normalize
/// units, page through results, compute bucket boundaries, own the
/// subscription registry and persist cursors -- all of it once, in shared
/// code. Ports implement only the `do*` methods, which receive
/// already-validated input and hand back raw platform data.
///
/// That split is deliberate: it is why a malformed query fails the same
/// way on every platform, why unit conversion cannot drift between ports,
/// and why no port can forget to check that the health provider is
/// actually available.
///
/// #### Threading
///
/// Every method may be called from the EDT and returns immediately. Every
/// callback arrives on the EDT. Post-processing of large result sets --
/// unit conversion across a hundred thousand samples -- happens on a
/// shared background thread, so nothing here blocks the UI.
public class HealthStore {

    private static final String PREF_SUBS = "cn1$health$subs";
    private static final String PREF_ANCHOR = "cn1$health$anchor$";
    private static final String PREF_LISTENER = "cn1$health$listener$";

    /// Default per-operation safety timeout for reads and writes.
    protected static final int DEFAULT_OPERATION_TIMEOUT = 60000;

    /// Default safety timeout for authorization flows, which involve a
    /// user tapping through a sheet.
    protected static final int DEFAULT_AUTHORIZATION_TIMEOUT = 300000;

    private final Map<String, HealthSubscription> subscriptions =
            new HashMap<String, HealthSubscription>();
    private final Map<String, HealthChangeListener> liveListeners =
            new HashMap<String, HealthChangeListener>();
    private int operationTimeout = DEFAULT_OPERATION_TIMEOUT;

    /// Ports construct subclasses. Application code obtains the active
    /// store from [Health#getStore()].
    protected HealthStore() {
    }

    // ==================================================================
    // capability queries -- ports override
    // ==================================================================

    /// `true` when this store can do anything at all. `false` on the
    /// fallback base class.
    public boolean isSupported() {
        return false;
    }

    /// Whether this platform exposes `type`. Even a fully supported
    /// platform covers a subset of [HealthDataType#values()].
    public boolean isTypeSupported(HealthDataType type) {
        return false;
    }

    /// The types this platform can read. Empty on the fallback.
    public List<HealthDataType> getSupportedTypes() {
        return Collections.emptyList();
    }

    /// Whether this app may write `type`. Some types are read-only on
    /// some platforms -- derived metrics the OS computes itself.
    public boolean isWritable(HealthDataType type) {
        return false;
    }

    /// Whether samples of `type` can be deleted. Both platforms restrict
    /// deletion to data this app wrote.
    public boolean isDeletable(HealthDataType type) {
        return false;
    }

    /// The metrics this platform can compute natively for `type`. Metrics
    /// outside this set are computed in shared code from raw samples,
    /// which is slower but portable.
    public List<AggregateMetric> getSupportedMetrics(HealthDataType type) {
        return Collections.emptyList();
    }

    /// Whether this platform can deliver changes without the app asking.
    public boolean isBackgroundDeliverySupported() {
        return false;
    }

    /// Whether the OS wakes the app on new data -- see
    /// [HealthSubscription#isPushDelivery()].
    public boolean isPushDelivery() {
        return false;
    }

    /// The largest number of samples one platform write call accepts.
    /// Health Connect caps this at 1000; the base class chunks larger
    /// writes automatically.
    public int getMaxWriteBatchSize() {
        return 1000;
    }

    /// The unit this platform prefers to receive `type` in. The base class
    /// converts before calling [#doWrite(List,AsyncResource)], so ports
    /// never do unit arithmetic.
    public HealthUnit getPreferredWriteUnit(HealthDataType type) {
        return type == null ? null : type.getCanonicalUnit();
    }

    // ==================================================================
    // authorization
    // ==================================================================

    /// Presents the platform authorization UI for the requested access.
    ///
    /// **Resolving `true` means the flow completed, not that access was
    /// granted.** On iOS the sheet completes identically whether the user
    /// enabled every switch or none of them, and HealthKit will not say
    /// which. Treat `true` as "the user has now been asked".
    ///
    /// Only Android distinguishes an explicit dismissal, which fails with
    /// [HealthError#USER_CANCELED].
    public final AsyncResource<Boolean> requestAuthorization(
            HealthAccess... access) {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        if (failIfUnsupported(out)) {
            return out;
        }
        if (access == null || access.length == 0) {
            fail(out, HealthError.INVALID_ARGUMENT,
                    "requestAuthorization needs at least one access");
            return out;
        }
        List<HealthAccess> deduped = new ArrayList<HealthAccess>();
        for (HealthAccess a : access) {
            if (a == null) {
                continue;
            }
            if (!isTypeSupported(a.getType())) {
                fail(out, HealthError.TYPE_NOT_SUPPORTED,
                        a.getType().getId()
                                + " is not available on this platform");
                return out;
            }
            if (!deduped.contains(a)) {
                deduped.add(a);
            }
        }
        if (deduped.isEmpty()) {
            fail(out, HealthError.INVALID_ARGUMENT,
                    "requestAuthorization needs at least one access");
            return out;
        }
        doRequestAuthorization(deduped, out);
        return out;
    }

    /// This app's write authorization for `type`. Truthful on both
    /// platforms.
    public HealthAuthorizationStatus getWriteAuthorizationStatus(
            HealthDataType type) {
        return HealthAuthorizationStatus.NOT_SUPPORTED;
    }

    /// This app's read authorization for `type`.
    ///
    /// **Returns [HealthAuthorizationStatus#UNKNOWN] on iOS in every
    /// case.** HealthKit deliberately refuses to disclose read
    /// authorization, because an app that could tell the difference
    /// between "denied" and "no data" could infer that a user is hiding a
    /// pregnancy or a prescription. Android answers truthfully, because
    /// its read permissions are ordinary runtime grants.
    ///
    /// The Android port does not paper over the difference by pretending
    /// iOS behaves the same way, and neither should your UI: never tell a
    /// user "you denied access" on the strength of this. Say "no data
    /// available" and offer [Health#openHealthSettings()].
    ///
    /// See [#hasAnyData(HealthDataType,HealthTimeRange)] for the only
    /// honest probe.
    public HealthAuthorizationStatus getReadAuthorizationStatus(
            HealthDataType type) {
        return HealthAuthorizationStatus.NOT_SUPPORTED;
    }

    /// Whether presenting the authorization sheet would show the user
    /// anything -- see [HealthRequestStatus]. Useful for deciding whether
    /// to show your own explainer screen first.
    public final AsyncResource<HealthRequestStatus>
            getAuthorizationRequestStatus(HealthAccess... access) {
        AsyncResource<HealthRequestStatus> out =
                new AsyncResource<HealthRequestStatus>();
        if (!isSupported()) {
            out.complete(HealthRequestStatus.UNKNOWN);
            return out;
        }
        List<HealthAccess> list = new ArrayList<HealthAccess>();
        if (access != null) {
            for (HealthAccess a : access) {
                if (a != null) {
                    list.add(a);
                }
            }
        }
        if (list.isEmpty()) {
            out.complete(HealthRequestStatus.UNNECESSARY);
            return out;
        }
        doGetAuthorizationRequestStatus(list, out);
        return out;
    }

    /// Runs a bounded query and reports whether any sample came back.
    ///
    /// This measures **data presence, not permission**, and the name says
    /// so on purpose. On iOS a `false` means "denied, or genuinely no
    /// data" and the two are indistinguishable -- that is a platform
    /// privacy guarantee, not a gap in this API.
    ///
    /// Note also that on iOS your own writes remain readable to you even
    /// when read access was denied, so `true` does not prove you can see
    /// *other* apps' data.
    public final AsyncResource<Boolean> hasAnyData(final HealthDataType type,
            HealthTimeRange range) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        if (failIfUnsupported(out)) {
            return out;
        }
        SampleQuery q = new SampleQuery().addType(type)
                .setTimeRange(range).setLimit(1);
        readSamplePage(q).onResult(makeAnyDataCallback(out));
        return out;
    }

    /// Built in a static method so the callback carries no synthetic
    /// reference to the enclosing store (SpotBugs
    /// `SIC_INNER_SHOULD_BE_STATIC_ANON`).
    private static AsyncResult<SamplePage>
            makeAnyDataCallback(final AsyncResource<Boolean> out) {
        return new AsyncResult<SamplePage>() {
            @Override
            public void onReady(SamplePage value, Throwable err) {
                if (err != null) {
                    out.error(err);
                } else {
                    out.complete(Boolean.valueOf(!value.isEmpty()));
                }
            }
        };
    }

    // ==================================================================
    // reads
    // ==================================================================

    /// Reads samples, following paging until the query's limit is reached
    /// or the data runs out.
    ///
    /// Results are normalized: values arrive in the query's unit or the
    /// type's canonical unit, and series are flattened unless
    /// [SampleQuery#setFlattenSeries(boolean)] says otherwise.
    public final AsyncResource<List<HealthSample>> readSamples(
            final SampleQuery query) {
        final AsyncResource<List<HealthSample>> out =
                new AsyncResource<List<HealthSample>>();
        if (failIfUnsupported(out)) {
            return out;
        }
        try {
            query.validate();
            requireSupportedTypes(query.getTypes());
        } catch (HealthException ex) {
            out.error(ex);
            return out;
        }
        final List<HealthSample> collected = new ArrayList<HealthSample>();
        // Page through a copy. Paging has to carry a token on the query,
        // and mutating the caller's object would leave the last token
        // stuck on it -- so a query reused for a second read would
        // silently resume mid-way through the first.
        readPageInto(copyForPaging(query), collected, out, null);
        return out;
    }

    /// A shallow copy of `query` that paging may mutate freely.
    private static SampleQuery copyForPaging(SampleQuery query) {
        SampleQuery copy = new SampleQuery()
                .setTimeRange(query.getTimeRange())
                .setLimit(query.getLimit())
                .setSortDescending(query.isSortDescending())
                .setUnit(query.getUnit())
                .setFlattenSeries(query.isFlattenSeries())
                .setSleepSessionGapMillis(query.getSleepSessionGapMillis());
        List<HealthDataType> types = query.getTypes();
        for (HealthDataType type : types) {
            copy.addType(type);
        }
        List<String> sources = query.getSources();
        for (String source : sources) {
            copy.addSource(source);
        }
        return copy;
    }

    private void readPageInto(final SampleQuery query,
            final List<HealthSample> collected,
            final AsyncResource<List<HealthSample>> out, String pageToken) {
        query.setPageToken(pageToken);
        readSamplePage(query).onResult(
                new AsyncResult<SamplePage>() {
                    @Override
                    public void onReady(SamplePage page, Throwable err) {
                        if (err != null) {
                            out.error(err);
                            return;
                        }
                        collected.addAll(page.getSamples());
                        String next = page.getNextPageToken();
                        int limit = query.getLimit();
                        if (next == null || collected.size() >= limit) {
                            // Trim rather than hand back more than
                            // was asked for: a caller that sized a
                            // buffer from the limit would otherwise
                            // overflow it. Flattening a series can push
                            // the count past the limit even when the
                            // platform honoured it.
                            while (collected.size() > limit) {
                                collected.remove(collected.size() - 1);
                            }
                            out.complete(collected);
                            return;
                        }
                        readPageInto(query, collected, out, next);
                    }
                });
    }

    /// Reads a single page of samples. Prefer this over
    /// [#readSamples(SampleQuery)] for high-frequency types, so peak
    /// memory stays bounded regardless of how much history exists.
    public final AsyncResource<SamplePage> readSamplePage(
            final SampleQuery query) {
        final AsyncResource<SamplePage> out = new AsyncResource<SamplePage>();
        if (failIfUnsupported(out)) {
            return out;
        }
        try {
            query.validate();
            requireSupportedTypes(query.getTypes());
        } catch (HealthException ex) {
            out.error(ex);
            return out;
        }
        final AsyncResource<SamplePage> raw = new AsyncResource<SamplePage>();
        raw.onResult(new AsyncResult<SamplePage>() {
            @Override
            public void onReady(SamplePage page, Throwable err) {
                if (err != null) {
                    out.error(err);
                    return;
                }
                try {
                    out.complete(postProcess(page, query));
                } catch (HealthException ex) {
                    out.error(ex);
                }
            }
        });
        doReadSamples(query, raw);
        return out;
    }

    /// Normalizes a raw platform page: flattens series when asked, and
    /// converts every value into the requested or canonical unit.
    ///
    /// Doing this in shared code is what makes the two ports return
    /// byte-identical objects. Left to the ports, HealthKit would return
    /// whatever unit the query happened to ask for and Health Connect its
    /// own fixed unit, and the difference would surface as a chart that
    /// looks right in the simulator and wrong on a device.
    private SamplePage postProcess(SamplePage page, SampleQuery query)
            throws HealthException {
        List<HealthSample> in = page.getSamples();
        List<HealthSample> outSamples = new ArrayList<HealthSample>(in.size());
        for (HealthSample s : in) {
            if (query.isFlattenSeries() && s instanceof SeriesSample) {
                SeriesSample series = (SeriesSample) s;
                for (int j = 0; j < series.size(); j++) {
                    outSamples.add(normalize(series.toQuantitySample(j),
                            query));
                }
            } else {
                outSamples.add(normalize(s, query));
            }
        }
        return new SamplePage(outSamples, page.getNextPageToken(),
                page.isTruncated());
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private HealthSample normalize(HealthSample s, SampleQuery query)
            throws HealthException {
        if (!(s instanceof QuantitySample)) {
            return s;
        }
        QuantitySample q = (QuantitySample) s;
        HealthUnit target = query.getUnit();
        if (target == null) {
            target = q.getType().getCanonicalUnit();
        }
        if (target == null || q.getQuantity().getUnit() == target) {
            return s;
        }
        QuantitySample converted = q.isInstantaneous()
                ? QuantitySample.create(q.getType(),
                        q.getQuantity().in(target), q.getStartMillis())
                : QuantitySample.create(q.getType(),
                        q.getQuantity().in(target), q.getStartMillis(),
                        q.getEndMillis());
        converted.setId(q.getId());
        converted.setSource(q.getSource());
        converted.setRecordingMethod(q.getRecordingMethod());
        return converted;
    }

    // ==================================================================
    // aggregates
    // ==================================================================

    /// Summarizes data into time buckets.
    ///
    /// Returns one [AggregateResult] per bucket, in chronological order,
    /// including buckets that held no data -- those come back empty rather
    /// than being omitted, so a chart can render a gap where a gap
    /// belongs. Read the double-counting warning on [AggregateQuery]
    /// before trusting a cross-platform total.
    public final AsyncResource<List<AggregateResult>> aggregate(
            AggregateQuery query) {
        AsyncResource<List<AggregateResult>> out =
                new AsyncResource<List<AggregateResult>>();
        if (failIfUnsupported(out)) {
            return out;
        }
        try {
            query.validate();
            requireSupportedTypes(query.getTypes());
        } catch (HealthException ex) {
            out.error(ex);
            return out;
        }
        doAggregate(query, bucketBoundaries(query), out);
        return out;
    }

    /// Aggregates raw samples into the query's buckets.
    ///
    /// This is the one implementation of the arithmetic. Ports that have a
    /// native aggregation API can override [#doAggregate] and use it; ports
    /// that do not get this for free, which is what stops a total computed
    /// on iOS from disagreeing with the same total computed on Android
    /// because two implementations rounded differently.
    protected final List<AggregateResult> aggregateSamples(
            AggregateQuery query, long[] boundaries,
            List<HealthSample> samples) {
        HealthTimeRange range =
                query.getTimeRange().resolve(System.currentTimeMillis());
        List<AggregateResult> results = new ArrayList<AggregateResult>();
        for (int b = 0; b + 1 < boundaries.length; b++) {
            long start = boundaries[b];
            long end = boundaries[b + 1];
            AggregateResult bucket = new AggregateResult(start, end);
            for (HealthDataType type : query.getTypes()) {
                aggregateInto(bucket, query, type, start, end, range,
                        samples);
            }
            results.add(bucket);
        }
        return results;
    }

    /// Computes one type's metrics over one bucket.
    ///
    /// Buckets with no data are left empty rather than filled with zeros,
    /// which is what lets a chart draw a gap where the user's phone was in
    /// a drawer instead of a flat line at zero.
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static void aggregateInto(AggregateResult bucket,
            AggregateQuery query, HealthDataType type, long start, long end,
            HealthTimeRange range, List<HealthSample> samples) {
        HealthUnit unit = query.getUnit() != null ? query.getUnit()
                : type.getCanonicalUnit();
        List<QuantitySample> inBucket = new ArrayList<QuantitySample>();
        long durationMillis = 0;
        int count = 0;
        for (HealthSample s : samples) {
            if (s.getType() != type) {
                continue;
            }
            if (s.getStartMillis() >= end || s.getEndMillis() < start) {
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
        bucket.setSampleCount(type, count);
        if (count == 0) {
            return;
        }
        List<AggregateMetric> metrics = query.getMetrics();
        for (AggregateMetric metric : metrics) {
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

    private static boolean sourceAllowed(HealthSample s,
            AggregateQuery query) {
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
            for (QuantitySample inItem : in) {
                sum += inItem.getValue(unit);
            }
            return sum;
        }
        if (metric == AggregateMetric.MINIMUM) {
            double min = Double.MAX_VALUE;
            for (QuantitySample inItem : in) {
                min = Math.min(min, inItem.getValue(unit));
            }
            return min;
        }
        if (metric == AggregateMetric.MAXIMUM) {
            double max = -Double.MAX_VALUE;
            for (QuantitySample inItem : in) {
                max = Math.max(max, inItem.getValue(unit));
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
        for (QuantitySample s : in) {
            double weight = Math.max(1, s.getDurationMillis());
            weighted += s.getValue(unit) * weight;
            totalWeight += weight;
        }
        return totalWeight == 0 ? 0 : weighted / totalWeight;
    }

    /// Computes the bucket boundaries for a query, as `n+1` timestamps
    /// bounding `n` buckets.
    ///
    /// Exposed to ports because both platforms want the boundaries up
    /// front, and because getting daylight-saving right exactly once is
    /// better than getting it wrong twice.
    protected final long[] bucketBoundaries(AggregateQuery query) {
        HealthTimeRange range =
                query.getTimeRange().resolve(System.currentTimeMillis());
        HealthInterval bucket = query.getBucket();
        if (bucket == null) {
            return new long[] { range.getStartMillis(),
                    range.getEndMillis() };
        }
        List<Long> bounds = new ArrayList<Long>();
        long cursor = bucket.bucketStart(range.getStartMillis(),
                range.getStartMillis());
        bounds.add(Long.valueOf(cursor));
        while (cursor < range.getEndMillis()) {
            long next = bucket.nextBoundary(cursor);
            if (next <= cursor) {
                // A calendar interval that fails to advance would spin
                // forever; stop rather than hang the app.
                break;
            }
            cursor = next;
            bounds.add(Long.valueOf(cursor));
        }
        long[] out = new long[bounds.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = bounds.get(i).longValue();
        }
        return out;
    }

    // ==================================================================
    // writes
    // ==================================================================

    /// Writes one sample.
    public final AsyncResource<HealthWriteResult> write(HealthSample sample) {
        List<HealthSample> one = new ArrayList<HealthSample>(1);
        one.add(sample);
        return write(one);
    }

    /// Writes several samples, chunking to [#getMaxWriteBatchSize()].
    ///
    /// Rejects before touching the platform: types this app cannot write,
    /// instantaneous samples of interval-only types such as
    /// [HealthDataType#STEPS], and quantities whose unit measures the
    /// wrong dimension. Catching these here turns what would be an opaque
    /// platform exception into a message that names the offending sample.
    public final AsyncResource<HealthWriteResult> write(
            final List<HealthSample> samples) {
        final AsyncResource<HealthWriteResult> out =
                new AsyncResource<HealthWriteResult>();
        if (failIfUnsupported(out)) {
            return out;
        }
        if (samples == null || samples.isEmpty()) {
            fail(out, HealthError.INVALID_ARGUMENT,
                    "write needs at least one sample");
            return out;
        }
        List<HealthSample> prepared = new ArrayList<HealthSample>();
        try {
            for (HealthSample sample : samples) {
                prepared.add(validateForWrite(sample));
            }
        } catch (HealthException ex) {
            out.error(ex);
            return out;
        }
        writeChunk(prepared, 0, new HealthWriteResult(), out);
        return out;
    }

    private void writeChunk(final List<HealthSample> all, final int from,
            final HealthWriteResult accumulated,
            final AsyncResource<HealthWriteResult> out) {
        if (from >= all.size()) {
            out.complete(accumulated);
            return;
        }
        int to = Math.min(all.size(), from + Math.max(1,
                getMaxWriteBatchSize()));
        List<HealthSample> chunk = new ArrayList<HealthSample>(
                all.subList(from, to));
        final int nextFrom = to;
        AsyncResource<HealthWriteResult> chunkResult =
                new AsyncResource<HealthWriteResult>();
        chunkResult.onResult(
                new AsyncResult<HealthWriteResult>() {
                    @Override
                    public void onReady(HealthWriteResult value,
                            Throwable err) {
                        if (err != null) {
                            out.error(err);
                            return;
                        }
                        List<String> ids = value.getSampleIds();
                        for (String id : ids) {
                            accumulated.addSampleId(id);
                        }
                        List<String> rejects = value.getRejections();
                        for (String reject : rejects) {
                            accumulated.addRejection(reject);
                        }
                        writeChunk(all, nextFrom, accumulated, out);
                    }
                });
        doWrite(chunk, chunkResult);
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private HealthSample validateForWrite(HealthSample sample)
            throws HealthException {
        if (sample == null) {
            throw new HealthException(HealthError.INVALID_ARGUMENT,
                    "cannot write a null sample");
        }
        HealthDataType type = sample.getType();
        if (!isTypeSupported(type)) {
            throw new HealthException(HealthError.TYPE_NOT_SUPPORTED,
                    type.getId() + " is not available on this platform");
        }
        if (!isWritable(type)) {
            throw new HealthException(HealthError.UNAUTHORIZED,
                    type.getId() + " is read-only on this platform");
        }
        if (type.isIntervalOnly() && sample.isInstantaneous()) {
            throw new HealthException(HealthError.INVALID_ARGUMENT,
                    type.getId() + " accumulates over time and needs a"
                            + " start and an end, but this sample marks a"
                            + " single instant");
        }
        if (!(sample instanceof QuantitySample)) {
            return sample;
        }
        QuantitySample q = (QuantitySample) sample;
        HealthUnit preferred = getPreferredWriteUnit(type);
        if (preferred == null) {
            return sample;
        }
        if (!q.getQuantity().getUnit().isCompatibleWith(preferred)) {
            throw new HealthException(HealthError.UNIT_MISMATCH,
                    type.getId() + " is measured in "
                            + preferred.getDimension() + " but the sample is"
                            + " in " + q.getQuantity().getUnit().getSymbol());
        }
        if (q.getQuantity().getUnit() == preferred) {
            return sample;
        }
        QuantitySample converted = q.isInstantaneous()
                ? QuantitySample.create(type, q.getQuantity().in(preferred),
                        q.getStartMillis())
                : QuantitySample.create(type, q.getQuantity().in(preferred),
                        q.getStartMillis(), q.getEndMillis());
        converted.setRecordingMethod(q.getRecordingMethod());
        Map<String, String> meta = q.getMetadata();
        for (Map.Entry<String, String> e : meta.entrySet()) {
            converted.putMetadata(e.getKey(), e.getValue());
        }
        return converted;
    }

    /// Deletes samples this app wrote. See [HealthDeleteRequest].
    public final AsyncResource<Integer> delete(HealthDeleteRequest request) {
        AsyncResource<Integer> out = new AsyncResource<Integer>();
        if (failIfUnsupported(out)) {
            return out;
        }
        if (request == null) {
            fail(out, HealthError.INVALID_ARGUMENT,
                    "delete needs a request");
            return out;
        }
        try {
            request.validate();
            requireSupportedTypes(request.getTypes());
        } catch (HealthException ex) {
            out.error(ex);
            return out;
        }
        doDelete(request, out);
        return out;
    }

    // ==================================================================
    // subscriptions
    // ==================================================================

    /// Subscribes with a listener that survives the app being killed.
    ///
    /// `backgroundListenerClass` must be a public top-level class with a
    /// public no-argument constructor implementing
    /// [HealthBackgroundListener] -- see that interface for the full
    /// contract.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the class is null or does not
    ///   implement [HealthBackgroundListener]. This is checked eagerly,
    ///   because the alternative is discovering the mistake weeks later
    ///   when a background relaunch silently does nothing.
    public final HealthSubscription subscribe(SubscriptionRequest request,
            Class backgroundListenerClass) {
        if (backgroundListenerClass == null) {
            throw new IllegalArgumentException(
                    "subscribe needs a background listener class");
        }
        if (!HealthBackgroundListener.class
                .isAssignableFrom(backgroundListenerClass)) {
            throw new IllegalArgumentException(backgroundListenerClass.getName()
                    + " does not implement HealthBackgroundListener");
        }
        HealthSubscription sub = register(request, null);
        Preferences.set(PREF_LISTENER + request.getId(),
                backgroundListenerClass.getName());
        return sub;
    }

    /// Subscribes with an in-memory listener, dropped when the process
    /// ends. Use the `Class` overload if you need delivery after the app
    /// has been killed.
    public final HealthSubscription subscribe(SubscriptionRequest request,
            HealthChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException(
                    "subscribe needs a listener");
        }
        return register(request, listener);
    }

    private HealthSubscription register(SubscriptionRequest request,
            HealthChangeListener listener) {
        ensureSubscriptionsRestored();
        if (request == null) {
            throw new IllegalArgumentException(
                    "subscribe needs a request");
        }
        try {
            request.validate();
        } catch (HealthException ex) {
            IllegalArgumentException wrapped =
                    new IllegalArgumentException(ex.getMessage());
            wrapped.initCause(ex);
            throw wrapped;
        }
        HealthSubscription sub = new HealthSubscription(this,
                request.getId(), request.getTypes(), isPushDelivery());
        synchronized (subscriptions) {
            HealthSubscription existing = subscriptions.get(request.getId());
            if (existing != null) {
                existing.markInactive();
            }
            subscriptions.put(request.getId(), sub);
            if (listener == null) {
                liveListeners.remove(request.getId());
            } else {
                liveListeners.put(request.getId(), listener);
            }
        }
        rememberSubscription(request);
        if (isSupported()) {
            doSubscribe(request, loadAnchor(request.getId()));
        }
        return sub;
    }

    /// Restores persisted subscriptions once, before anything reads or
    /// mutates the registry.
    ///
    /// Deliberately lazy rather than eager: restoring calls into
    /// `doSubscribe`, so it cannot run until the port is fully built.
    private void ensureSubscriptionsRestored() {
        synchronized (subscriptions) {
            if (subscriptionsRestored) {
                return;
            }
            subscriptionsRestored = true;
        }
        restoreSubscriptions();
    }

    /// Cancels a subscription and discards its persisted cursor.
    /// Idempotent.
    public final void unsubscribe(String subscriptionId) {
        if (subscriptionId == null) {
            return;
        }
        ensureSubscriptionsRestored();
        HealthSubscription sub;
        synchronized (subscriptions) {
            sub = subscriptions.remove(subscriptionId);
            liveListeners.remove(subscriptionId);
        }
        if (sub != null) {
            sub.markInactive();
        }
        forgetSubscription(subscriptionId);
        Preferences.delete(PREF_ANCHOR + subscriptionId);
        Preferences.delete(PREF_LISTENER + subscriptionId);
        if (isSupported()) {
            doUnsubscribe(subscriptionId);
        }
    }

    /// Every currently registered subscription.
    public final List<HealthSubscription> getSubscriptions() {
        ensureSubscriptionsRestored();
        synchronized (subscriptions) {
            return new ArrayList<HealthSubscription>(subscriptions.values());
        }
    }

    /// Immediately drains pending changes for every active subscription,
    /// resolving with the number of batches delivered.
    ///
    /// Called automatically at app start and when the app returns to the
    /// foreground. **On Android you should also call it from your
    /// background-fetch handler** -- Health Connect never wakes the app on
    /// its own, so nothing else will notice new data while your app is
    /// closed.
    public final AsyncResource<Integer> drainChanges() {
        AsyncResource<Integer> out = new AsyncResource<Integer>();
        ensureSubscriptionsRestored();
        if (!isSupported()) {
            out.complete(Integer.valueOf(0));
            return out;
        }
        List<HealthSubscription> subs = getSubscriptions();
        if (subs.isEmpty()) {
            out.complete(Integer.valueOf(0));
            return out;
        }
        doDrainChanges(subs, out);
        return out;
    }

    // ==================================================================
    // protected helpers for ports
    // ==================================================================

    /// Delivers a batch to whichever listener the subscription registered,
    /// on the EDT.
    ///
    /// **The cursor is persisted only after the listener returns.** That
    /// ordering means a crash inside a listener costs one redelivered
    /// batch rather than losing the data permanently -- the opposite
    /// ordering would advance past data the app never actually processed.
    protected final void fireChanges(final HealthChangeBatch batch) {
        if (batch == null) {
            return;
        }
        final String id = batch.getSubscriptionId();
        HealthChangeListener live;
        HealthSubscription sub;
        synchronized (subscriptions) {
            live = liveListeners.get(id);
            sub = subscriptions.get(id);
        }
        if (sub == null) {
            return;
        }
        final HealthChangeListener target = live != null ? live
                : resolveBackgroundListener(id);
        if (target == null) {
            return;
        }
        final HealthSubscription subscription = sub;
        Display.getInstance().callSerially(
                makeDeliveryRunnable(this, target, batch, subscription));
    }

    /// Built in a static method so the `Runnable` carries no synthetic
    /// reference to the enclosing store (SpotBugs
    /// `SIC_INNER_SHOULD_BE_STATIC_ANON`).
    private static Runnable makeDeliveryRunnable(final HealthStore store,
            final HealthChangeListener listener, final HealthChangeBatch batch,
            final HealthSubscription subscription) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    listener.healthDataChanged(batch);
                } catch (Throwable t) {
                    // A listener that throws must not cost us the cursor
                    // advance for every *later* batch, but it also must
                    // not advance past data it failed on -- so log and
                    // leave the anchor where it was.
                    com.codename1.io.Log.e(t);
                    return;
                }
                if (batch.getAnchor() != null) {
                    store.storeAnchor(batch.getSubscriptionId(),
                            batch.getAnchor());
                }
                subscription.noteDelivery(batch.getAnchor(),
                        System.currentTimeMillis());
            }
        };
    }

    private HealthChangeListener resolveBackgroundListener(String id) {
        String className = Preferences.get(PREF_LISTENER + id, null);
        if (className == null) {
            return null;
        }
        HealthBackgroundListenerFactory f = backgroundListenerFactory;
        if (f == null) {
            // No generated bindings in this build. Nothing is lost: the
            // caller returns without advancing the anchor, so the changes
            // are redelivered once a listener is registered.
            com.codename1.io.Log.p("Health: subscription " + id + " has a"
                    + " background listener but this build contains no"
                    + " generated bindings, so the delivery is deferred."
                    + " This is expected in the simulator and in unit"
                    + " tests; on device the build server generates them.");
            return null;
        }
        final HealthBackgroundListener bg = f.create(className);
        if (bg == null) {
            com.codename1.io.Log.p("Health: no generated binding for"
                    + " background listener " + className + " (subscription "
                    + id + "). It must be a public top-level class with a"
                    + " public no-argument constructor implementing"
                    + " HealthBackgroundListener.");
            return null;
        }
        return adaptBackgroundListener(bg);
    }

    /// Wraps a background listener as a change listener from a static
    /// method, so the adapter holds no reference to the store (SpotBugs
    /// `SIC_INNER_SHOULD_BE_STATIC_ANON`).
    private static HealthChangeListener adaptBackgroundListener(
            final HealthBackgroundListener bg) {
        return new HealthChangeListener() {
            @Override
            public void healthDataChanged(HealthChangeBatch batch) {
                bg.healthDataChanged(batch);
            }
        };
    }

    /// Guards the one-shot restore in [#ensureSubscriptionsRestored].
    private boolean subscriptionsRestored;

    /// Volatile because the build-generated factory is installed once
    /// during startup and then read from whatever thread the platform
    /// delivers a background batch on. A lock here would serialise
    /// delivery for a field that is written exactly once.
    @SuppressWarnings("PMD.AvoidUsingVolatile")
    private static volatile HealthBackgroundListenerFactory
            backgroundListenerFactory;

    /// Installs the build-generated factory that constructs background
    /// listeners after a process relaunch.
    ///
    /// Called by code the build server injects into app startup, in the
    /// same way the Android port's native bridges are registered. There is
    /// deliberately no reflective fallback -- see
    /// [HealthBackgroundListenerFactory] for why resolving a class by name
    /// is the wrong mechanism on these targets.
    public static void setBackgroundListenerFactory(
            HealthBackgroundListenerFactory factory) {
        backgroundListenerFactory = factory;
    }

    /// The persisted cursor for a subscription, or null to start fresh.
    protected final HealthAnchor loadAnchor(String subscriptionId) {
        return HealthAnchor.fromStorableString(
                Preferences.get(PREF_ANCHOR + subscriptionId, null));
    }

    /// Persists a cursor. Called by [#fireChanges(HealthChangeBatch)]
    /// after successful delivery; ports rarely need it directly.
    protected final void storeAnchor(String subscriptionId,
            HealthAnchor anchor) {
        if (anchor == null) {
            Preferences.delete(PREF_ANCHOR + subscriptionId);
        } else {
            Preferences.set(PREF_ANCHOR + subscriptionId,
                    anchor.toStorableString());
        }
    }

    /// Re-registers every subscription persisted by a previous launch.
    ///
    /// This is what makes a subscription survive the process being killed:
    /// without it, an app relaunched into the background has no idea it was
    /// ever watching anything.
    ///
    /// It runs on its own, the first time anything touches subscriptions --
    /// see [#ensureSubscriptionsRestored]. It used to be documented as
    /// something ports had to call from their constructor, which no port
    /// did, so persisted subscriptions were silently never restored on any
    /// platform. A constructor could not have called it safely anyway: it
    /// dispatches to `doSubscribe`, which a subclass has not finished
    /// initialising at that point.
    protected final void restoreSubscriptions() {
        String stored = Preferences.get(PREF_SUBS, "");
        if (stored.length() == 0) {
            return;
        }
        String[] entries = com.codename1.util.StringUtil
                .tokenize(stored, '\n').toArray(new String[0]);
        for (String entrie : entries) {
            SubscriptionRequest req = parseSubscription(entrie);
            if (req == null) {
                continue;
            }
            HealthSubscription sub = new HealthSubscription(this,
                    req.getId(), req.getTypes(), isPushDelivery());
            synchronized (subscriptions) {
                subscriptions.put(req.getId(), sub);
            }
            if (isSupported()) {
                doSubscribe(req, loadAnchor(req.getId()));
            }
        }
    }

    private SubscriptionRequest parseSubscription(String entry) {
        if (entry == null || entry.trim().length() == 0) {
            return null;
        }
        List<String> parts = com.codename1.util.StringUtil
                .tokenize(entry, '\t');
        if (parts.isEmpty()) {
            return null;
        }
        SubscriptionRequest req;
        try {
            req = new SubscriptionRequest(parts.get(0));
        } catch (IllegalArgumentException ex) {
            return null;
        }
        for (int i = 1; i < parts.size(); i++) {
            HealthDataType t = HealthDataType.forId(parts.get(i));
            // A type this build no longer knows is skipped rather than
            // failing the whole restore -- a downgraded app should keep
            // the subscriptions it still understands.
            if (t != null) {
                req.addType(t);
            }
        }
        return req.getTypes().isEmpty() ? null : req;
    }

    private void rememberSubscription(SubscriptionRequest request) {
        StringBuilder sb = new StringBuilder();
        List<String> kept = readStoredEntries(request.getId());
        for (String keptItem : kept) {
            sb.append(keptItem).append('\n');
        }
        sb.append(request.getId());
        List<HealthDataType> types = request.getTypes();
        for (HealthDataType type : types) {
            sb.append('\t').append(type.getId());
        }
        Preferences.set(PREF_SUBS, sb.toString());
    }

    private void forgetSubscription(String subscriptionId) {
        List<String> kept = readStoredEntries(subscriptionId);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kept.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(kept.get(i));
        }
        Preferences.set(PREF_SUBS, sb.toString());
    }

    private List<String> readStoredEntries(String excludingId) {
        List<String> kept = new ArrayList<String>();
        String stored = Preferences.get(PREF_SUBS, "");
        if (stored.length() == 0) {
            return kept;
        }
        List<String> entries = com.codename1.util.StringUtil
                .tokenize(stored, '\n');
        for (String e : entries) {
            if (e.trim().length() == 0) {
                continue;
            }
            if (e.equals(excludingId) || e.startsWith(excludingId + "\t")) {
                continue;
            }
            kept.add(e);
        }
        return kept;
    }

    /// Sets the per-operation safety timeout in milliseconds. Ports use it
    /// so that a platform callback that never arrives surfaces as
    /// [HealthError#TIMEOUT] instead of an operation that hangs forever.
    protected final void setOperationTimeout(int millis) {
        this.operationTimeout = millis;
    }

    /// The current per-operation safety timeout.
    protected final int getOperationTimeout() {
        return operationTimeout;
    }

    // ==================================================================
    // port SPI -- every method defaults to NOT_SUPPORTED
    // ==================================================================

    /// Reads one page of raw platform samples. Input is already validated;
    /// units and series flattening are handled by the caller.
    protected void doReadSamples(SampleQuery query,
            AsyncResource<SamplePage> out) {
        failNotSupported(out);
    }

    /// Computes bucketed aggregates. `boundaries` holds `n+1` timestamps
    /// bounding `n` buckets, already daylight-saving correct.
    ///
    /// Unlike the rest of the port SPI this does **not** default to
    /// NOT_SUPPORTED. Neither HealthKit nor Health Connect gives us an
    /// aggregation we can use directly for every metric this API exposes,
    /// so the default reads the raw samples and runs the shared arithmetic
    /// in [#aggregateSamples]. A port only overrides this when its native
    /// aggregation is genuinely better -- and if it does, it owes the same
    /// answers.
    protected void doAggregate(AggregateQuery query, long[] boundaries,
            AsyncResource<List<AggregateResult>> out) {
        aggregateByReadingSamples(query, boundaries, out);
    }

    /// The fallback aggregation: read every requested type over the query's
    /// range, then bucket the samples locally.
    ///
    /// Reads one type at a time because HealthKit accepts only one type per
    /// query, and it is the narrower contract of the two.
    protected final void aggregateByReadingSamples(AggregateQuery query,
            long[] boundaries, AsyncResource<List<AggregateResult>> out) {
        if (boundaries.length < 2) {
            out.complete(new ArrayList<AggregateResult>());
            return;
        }
        HealthTimeRange range =
                query.getTimeRange().resolve(System.currentTimeMillis());
        new AggregateFallback(this, query, boundaries, out,
                new ArrayList<HealthDataType>(query.getTypes()), range)
                .next();
    }

    /// Drives the fallback one type at a time, accumulating as it goes.
    ///
    /// Named rather than anonymous so the EDT hop carries no implicit
    /// reference to the enclosing store.
    private static final class AggregateFallback
            implements AsyncResult<List<HealthSample>> {

        private final HealthStore store;
        private final AggregateQuery query;
        private final long[] boundaries;
        private final AsyncResource<List<AggregateResult>> out;
        private final List<HealthDataType> types;
        private final HealthTimeRange range;
        private final List<HealthSample> collected =
                new ArrayList<HealthSample>();
        private int index;

        AggregateFallback(HealthStore store, AggregateQuery query,
                long[] boundaries, AsyncResource<List<AggregateResult>> out,
                List<HealthDataType> types, HealthTimeRange range) {
            this.store = store;
            this.query = query;
            this.boundaries = boundaries;
            this.out = out;
            this.types = types;
            this.range = range;
        }

        void next() {
            if (index >= types.size()) {
                out.complete(store.aggregateSamples(query, boundaries,
                        collected));
                return;
            }
            SampleQuery q = new SampleQuery()
                    .addType(types.get(index))
                    .setTimeRange(HealthTimeRange.between(
                            range.getStartMillis(), range.getEndMillis()));
            for (String source : query.getSources()) {
                q.addSource(source);
            }
            index++;
            store.readSamples(q).onResult(this);
        }

        @Override
        public void onReady(List<HealthSample> value,
                Throwable error) {
            if (error != null) {
                // One unreadable type must not silently produce a total
                // that looks complete. The whole aggregate fails.
                out.error(error);
                return;
            }
            if (value != null) {
                collected.addAll(value);
            }
            next();
        }
    }

    /// Writes a chunk no larger than [#getMaxWriteBatchSize()], already
    /// validated and converted into [#getPreferredWriteUnit(HealthDataType)].
    protected void doWrite(List<HealthSample> samples,
            AsyncResource<HealthWriteResult> out) {
        failNotSupported(out);
    }

    /// Deletes samples matching an already-validated request.
    protected void doDelete(HealthDeleteRequest request,
            AsyncResource<Integer> out) {
        failNotSupported(out);
    }

    /// Presents the platform authorization UI.
    protected void doRequestAuthorization(List<HealthAccess> access,
            AsyncResource<Boolean> out) {
        failNotSupported(out);
    }

    /// Reports whether the authorization sheet would show anything.
    protected void doGetAuthorizationRequestStatus(List<HealthAccess> access,
            AsyncResource<HealthRequestStatus> out) {
        out.complete(HealthRequestStatus.UNKNOWN);
    }

    /// Registers a platform observer, resuming from `anchor` when one was
    /// persisted. Called on registration and again on
    /// [#restoreSubscriptions()].
    protected void doSubscribe(SubscriptionRequest request,
            HealthAnchor anchor) {
    }

    /// Tears down a platform observer.
    protected void doUnsubscribe(String subscriptionId) {
    }

    /// Polls for pending changes and delivers them through
    /// [#fireChanges(HealthChangeBatch)], resolving with the number of
    /// batches delivered.
    protected void doDrainChanges(List<HealthSubscription> subscriptions,
            AsyncResource<Integer> out) {
        out.complete(Integer.valueOf(0));
    }

    // ==================================================================
    // internals
    // ==================================================================

    private void requireSupportedTypes(List<HealthDataType> types)
            throws HealthException {
        for (HealthDataType t : types) {
            if (!isTypeSupported(t)) {
                throw new HealthException(HealthError.TYPE_NOT_SUPPORTED,
                        t.getId() + " is not available on this platform");
            }
        }
    }

    private boolean failIfUnsupported(AsyncResource out) {
        if (isSupported()) {
            return false;
        }
        failNotSupported(out);
        return true;
    }

    private static void failNotSupported(AsyncResource out) {
        out.error(new HealthException(HealthError.NOT_SUPPORTED,
                "health data is not available on this platform"));
    }

    private static void fail(AsyncResource out, HealthError error,
            String message) {
        out.error(new HealthException(error, message));
    }
}
