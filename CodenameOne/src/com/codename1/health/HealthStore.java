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
    /// Authorization waits on a person, not on a platform call.
    private int authorizationTimeout = DEFAULT_AUTHORIZATION_TIMEOUT;

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
            if (a.isWrite() && !isWritable(a.getType())) {
                // Presenting a write permission this store can never use
                // asks the user to grant something for nothing, and the
                // flow resolves successfully while every later write is
                // rejected locally.
                fail(out, HealthError.TYPE_NOT_SUPPORTED,
                        a.getType().getId()
                                + " cannot be written on this platform");
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
        // Authorization waits on a human reading a permission sheet, so
        // it gets its own much longer budget -- the operation timeout
        // would fail the request while the UI was still legitimately up.
        armTimeout(out, authorizationTimeout);
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
                            // Trim only when this is genuinely the end.
                            // Trimming a page that has a continuation
                            // token would discard samples the token
                            // resumes past, losing them for good; the
                            // caller asked for a limit, not for a hole.
                            //
                            // And never inside a record. One series record
                            // flattens into many samples sharing an id, so
                            // cutting through it drops measurements that no
                            // token and no repeat of this same query would
                            // bring back -- a heart-rate record holding
                            // more points than the limit would come back
                            // permanently docked. Whole records only: the
                            // last one is either kept entire, overshooting
                            // the limit, or dropped entire.
                            if (next == null && collected.size() > limit) {
                                trimToRecordBoundary(collected, limit);
                            }
                            out.complete(collected);
                            return;
                        }
                        readPageInto(query, collected, out, next);
                    }
                });
    }

    /// Drops whole records from the tail until at most `limit` samples
    /// remain, keeping the last record entire when cutting it would be
    /// the only way to reach the limit.
    ///
    /// Samples flattened out of one series record share its identifier,
    /// and nothing can return the half that a mid-record cut discards.
    private static void trimToRecordBoundary(List<HealthSample> collected,
            int limit) {
        while (collected.size() > limit) {
            String id = collected.get(collected.size() - 1).getId();
            int from = collected.size() - 1;
            while (from > 0 && sameRecord(collected.get(from - 1), id)) {
                from--;
            }
            if (from < limit) {
                // Dropping this record would take us under the limit, so
                // it is the record the cut would fall inside. Keep it.
                return;
            }
            while (collected.size() > from) {
                collected.remove(collected.size() - 1);
            }
        }
    }

    private static boolean sameRecord(HealthSample s, String id) {
        return id != null && id.equals(s.getId());
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
        armTimeout(raw);
        doReadSamples(query, raw);
        return out;
    }

    /// True while `sub` is still the registered, active instance.
    boolean isStillRegistered(HealthSubscription sub) {
        if (sub == null || !sub.isActive()) {
            return false;
        }
        synchronized (subscriptions) {
            // Identity on purpose: the question is whether *this* handle
            // is the one still registered, not whether an equal one is.
            return subscriptions.get(sub.getId()) == sub; //NOPMD CompareObjectsWithEquals
        }
    }

    /// Records that one queued delivery has finished.
    ///
    /// `abandoned` is how many later chunks will never be queued, which
    /// happens when a listener throws and the rest of the page is dropped.
    void noteDeliveryDone(int abandoned) {
        synchronized (subscriptions) {
            // This delivery, plus the chunks that will never be queued
            // because a listener threw. Counting only this one left the
            // gate waiting on deliveries that could never arrive, so the
            // drain never resolved at all.
            pendingDeliveries -= 1 + Math.max(0, abandoned);
            if (pendingDeliveries < 0) {
                pendingDeliveries = 0;
            }
        }
        releaseDrainGates();
    }

    /// Fails `resource` with [HealthError#TIMEOUT] if the platform never
    /// answers.
    ///
    /// [#getOperationTimeout()] was settable and documented and nothing
    /// ever armed a timer, so a native call that lost its callback left
    /// the operation pending forever -- which is precisely the case the
    /// setting exists for.
    protected final void armTimeout(final AsyncResource resource) {
        armTimeout(resource, operationTimeout);
    }

    protected final void armTimeout(final AsyncResource resource,
            int millis) {
        if (millis <= 0 || resource == null) {
            return;
        }
        // CLDC's Timer has no named/daemon constructor, and a Timer keeps
        // a live thread until it is cancelled -- so it is cancelled when
        // the operation finishes, not merely when the deadline fires.
        // Otherwise a burst of paged reads left one thread per call
        // sitting around for the whole timeout.
        final java.util.Timer timer = new java.util.Timer();
        timer.schedule(new TimeoutTask(resource, timer), millis);
        resource.onResult(new CancelTimer(timer));
    }

    /// Delivers the timeout failure on the EDT.
    private static final class FailTimedOut implements Runnable {
        private final AsyncResource resource;

        FailTimedOut(AsyncResource resource) {
            this.resource = resource;
        }

        @Override
        public void run() {
            if (resource.isDone()) {
                return;
            }
            resource.error(new HealthException(HealthError.TIMEOUT,
                    "the platform did not answer within the configured"
                            + " timeout"));
        }
    }

    /// Cancels a timeout timer once its operation has finished.
    private static final class CancelTimer implements AsyncResult {
        private final java.util.Timer timer;

        CancelTimer(java.util.Timer timer) {
            this.timer = timer;
        }

        @Override
        public void onReady(Object value, Throwable error) {
            timer.cancel();
        }
    }

    /// Named rather than anonymous so the timer holds no synthetic
    /// reference to the store.
    private static final class TimeoutTask extends java.util.TimerTask {
        private final AsyncResource resource;
        private final java.util.Timer timer;

        TimeoutTask(AsyncResource resource, java.util.Timer timer) {
            this.resource = resource;
            this.timer = timer;
        }

        @Override
        public void run() {
            timer.cancel();
            if (resource.isDone()) {
                return;
            }
            // Failed on the EDT like every other completion. Erroring from
            // the timer thread handed the application callback a thread it
            // is documented never to see.
            Display.getInstance().callSerially(new FailTimedOut(resource));
        }
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
        List<String> wanted = query.getSources();
        boolean flattened = false;
        for (HealthSample s : in) {
            // Applied here, not only in the ports, because a port that
            // cannot express the filter natively would otherwise return
            // every app's data and silently double-count phone and watch.
            // A port that does filter natively simply has nothing left to
            // drop.
            if (!wanted.isEmpty() && (s.getSource() == null
                    || !wanted.contains(s.getSource().getBundleId()))) {
                continue;
            }
            if (query.isFlattenSeries() && s instanceof SeriesSample) {
                SeriesSample series = (SeriesSample) s;
                HealthTimeRange asked = query.getTimeRange() == null ? null
                        : query.getTimeRange()
                                .resolve(System.currentTimeMillis());
                for (int j = 0; j < series.size(); j++) {
                    // Each measurement against the requested range, not
                    // just the record. A store matches the enclosing span,
                    // so a one-minute query over an hour-long heart-rate
                    // record returned the whole hour -- every point
                    // outside the range the caller asked for.
                    if (!inRange(asked, series.getSampleStartMillis(j),
                            series.getSampleEndMillis(j))) {
                        continue;
                    }
                    outSamples.add(normalize(series.toQuantitySample(j),
                            query));
                    flattened = true;
                }
            } else {
                outSamples.add(normalize(s, query));
            }
        }
        if (flattened) {
            // Sorted by measurement, because flattening changed what a
            // "sample" is. A port orders records, and a series carries its
            // own points in whatever order it was built with -- so an
            // expanded page followed neither the requested direction nor
            // any other, on the local store and on Health Connect alike.
            Collections.sort(outSamples,
                    new ByStart(query.isSortDescending()));
        }
        return new SamplePage(outSamples, page.getNextPageToken(),
                page.isTruncated());
    }

    /// Orders samples by when they were measured.
    private static final class ByStart
            implements java.util.Comparator<HealthSample> {
        private final boolean descending;

        ByStart(boolean descending) {
            this.descending = descending;
        }

        @Override
        public int compare(HealthSample a, HealthSample b) {
            long x = a.getStartMillis();
            long y = b.getStartMillis();
            if (x == y) {
                return 0;
            }
            return (x < y) != descending ? -1 : 1;
        }
    }

    /// Half-open membership, the same rule the stores match records by:
    /// an instant at the start is inside, an interval ending there is not.
    private static boolean inRange(HealthTimeRange range, long start,
            long end) {
        if (range == null) {
            return true;
        }
        if (start >= range.getEndMillis()) {
            return false;
        }
        return start == end ? end >= range.getStartMillis()
                : end > range.getStartMillis();
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private HealthSample normalize(HealthSample s, SampleQuery query)
            throws HealthException {
        if (s instanceof SeriesSample) {
            return normalizeSeries((SeriesSample) s, query);
        }
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
        // Metadata travels with the sample. Losing it merely because the
        // caller asked for pounds instead of kilograms would drop the
        // correlation identifier this API tells them to keep there.
        //
        // Copied entry by entry: getMetadata() hands back an unmodifiable
        // view, so putAll on it throws and would have failed the whole
        // write rather than merely losing the metadata.
        for (Map.Entry<String, String> e : q.getMetadata().entrySet()) {
            converted.putMetadata(e.getKey(), e.getValue());
        }
        return converted;
    }

    /// Converts a whole series into the query's unit.
    ///
    /// A series reaches here only when the caller turned flattening off,
    /// which is the one path where the values are not repackaged as
    /// [QuantitySample]s on the way out. Returning it untouched left
    /// `getUnit()` reporting whatever the platform stored while
    /// [#readSamplePage(SampleQuery)] promises the requested unit, so the
    /// same query answered in two different units depending on a flag that
    /// has nothing to do with units.
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static SeriesSample normalizeSeries(SeriesSample series,
            SampleQuery query) {
        HealthUnit target = query.getUnit();
        if (target == null) {
            target = series.getType().getCanonicalUnit();
        }
        if (target == null || series.getUnit() == target) {
            return series;
        }
        int n = series.size();
        long[] starts = new long[n];
        long[] ends = new long[n];
        double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            starts[i] = series.getSampleStartMillis(i);
            ends[i] = series.getSampleEndMillis(i);
            values[i] = series.getSampleValue(i, target);
        }
        SeriesSample converted = SeriesSample.create(series.getType(),
                series.getStartMillis(), series.getEndMillis(), starts, ends,
                values, target);
        converted.setId(series.getId());
        converted.setSource(series.getSource());
        converted.setRecordingMethod(series.getRecordingMethod());
        for (Map.Entry<String, String> e : series.getMetadata().entrySet()) {
            converted.putMetadata(e.getKey(), e.getValue());
        }
        return converted;
    }

    /// Checks and converts a series exactly as a scalar write is checked.
    ///
    /// A series used to be returned untouched, skipping both the
    /// dimension check and the conversion to the port's preferred write
    /// unit. The bridges read the value and ignore the unit that travels
    /// beside it, so a heart-rate series in `COUNT_PER_SECOND` was stored
    /// as the same numbers in `COUNT_PER_MINUTE` -- 2 Hz became 2 bpm
    /// rather than 120 -- and a series in an entirely wrong dimension was
    /// accepted without complaint.
    private SeriesSample validateSeriesForWrite(SeriesSample series,
            HealthDataType type) throws HealthException {
        // Every measurement, not only the enclosing span. The wire expands
        // a series point by point, so an interval-only type whose points
        // are instants produced zero-duration StepsRecords that Health
        // Connect rejects at runtime -- after the outer span had passed
        // the check just above.
        if (type.isIntervalOnly()) {
            for (int i = 0; i < series.size(); i++) {
                if (series.getSampleStartMillis(i)
                        >= series.getSampleEndMillis(i)) {
                    throw new HealthException(HealthError.INVALID_ARGUMENT,
                            type.getId() + " accumulates over time, but"
                                    + " measurement " + i + " of this series"
                                    + " marks a single instant");
                }
            }
        }
        HealthUnit preferred = getPreferredWriteUnit(type);
        if (preferred == null) {
            return series;
        }
        if (!series.getUnit().isCompatibleWith(preferred)) {
            throw new HealthException(HealthError.UNIT_MISMATCH,
                    type.getId() + " is measured in "
                            + preferred.getDimension() + " but the series is"
                            + " in " + series.getUnit().getSymbol());
        }
        // Units are interned, so identity is the intended test.
        if (series.getUnit() == preferred) { //NOPMD CompareObjectsWithEquals
            return series;
        }
        int n = series.size();
        long[] starts = new long[n];
        long[] ends = new long[n];
        double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            starts[i] = series.getSampleStartMillis(i);
            ends[i] = series.getSampleEndMillis(i);
            values[i] = series.getSampleValue(i, preferred);
        }
        SeriesSample converted = SeriesSample.create(type,
                series.getStartMillis(), series.getEndMillis(), starts, ends,
                values, preferred);
        converted.setId(series.getId());
        converted.setSource(series.getSource());
        converted.setRecordingMethod(series.getRecordingMethod());
        for (Map.Entry<String, String> e : series.getMetadata().entrySet()) {
            converted.putMetadata(e.getKey(), e.getValue());
        }
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
        List<Double> weights = new ArrayList<Double>();
        List<Double> durations = new ArrayList<Double>();
        // The clipped spans themselves, not a running total. DURATION is
        // documented as time covered, and two samples of one type really
        // do overlap -- a phone and a watch both recording, or a series
        // whose measurements are intervals. Added up, `[0,10]` beside
        // `[5,15]` reported 20ms of a 15ms bucket, and a bucket can no
        // more hold more time than it is wide than a night can hold more
        // sleep than it lasted.
        List<long[]> covered = new ArrayList<long[]>();
        int count = 0;
        for (HealthSample s : samples) {
            if (s.getType() != type) {
                continue;
            }
            // Half-open, matching the range contract. An interval whose
            // end lands exactly on this bucket's start has zero overlap
            // with it and belongs entirely to the previous bucket;
            // counting it in both double-counted every record sitting on a
            // boundary. An instant at the inclusive start still belongs
            // here, hence the isInstantaneous allowance.
            if (s.getStartMillis() >= end) {
                continue;
            }
            if (s.isInstantaneous() ? s.getEndMillis() < start
                    : s.getEndMillis() <= start) {
                continue;
            }
            // Overlap, matching sample queries. Filtering on start time
            // dropped an interval that began before the range and ended
            // inside it, so steps recorded 11:55-12:05 contributed nothing
            // to an aggregate starting at noon instead of their
            // proportional five minutes. The bucket arithmetic below
            // already clips, so keeping it here is safe.
            // Half-open, matching the bucket test above and the sample
            // query. A `<` test let an interval ending exactly at the
            // range start through: with a calendar bucket that begins
            // before the requested range -- a daily bucket on a
            // noon-to-midnight query -- it was counted, and contributed to
            // the average, minimum and maximum with zero overlap. An
            // instant at the start is inside; an interval ending there is
            // not.
            if ((s.isInstantaneous()
                        ? s.getEndMillis() < range.getStartMillis()
                        : s.getEndMillis() <= range.getStartMillis())
                    || s.getStartMillis() >= range.getEndMillis()) {
                continue;
            }
            if (!sourceAllowed(s, query)) {
                continue;
            }
            // Counted after the shape is expanded, not here. A series is
            // one record holding many measurements, and counting the
            // container reported COUNT as 1 for a three-point series while
            // the same data read through a flattening store reported 3.
            // Clip to the bucket. A two-hour workout spanning two hourly
            // buckets contributes one hour to each, not two hours to both;
            // adding the whole span to every bucket it touches inflates
            // every summary around a boundary.
            // Clipped to the bucket *and* the query range. A calendar
            // bucket can extend past the requested range -- a daily bucket
            // on a noon-to-midnight query starts at midnight -- so
            // clipping only to the bucket counted time the caller never
            // asked about.
            long from = Math.max(start, range.getStartMillis());
            long to = Math.min(end, range.getEndMillis());
            long overlap = Math.min(to, s.getEndMillis())
                    - Math.max(from, s.getStartMillis());
            if (overlap < 0) {
                overlap = 0;
            }
            long span = s.getDurationMillis();
            // An instantaneous sample covers no time. The one-millisecond
            // substitute below is an averaging weight, not a duration --
            // adding it here made "total time covered" grow with the
            // number of spot readings.
            //
            // A series is accounted for measurement by measurement in the
            // branch below, so its enclosing span must not be added here
            // as well.
            if (!(s instanceof SeriesSample) && span > 0 && overlap > 0) {
                covered.add(new long[] {
                        Math.max(from, s.getStartMillis()),
                        Math.min(to, s.getEndMillis()),
                });
            }
            if (s instanceof SeriesSample) {
                // A series reaches here whole -- the local and simulator
                // stores hold it that way and aggregate their records
                // directly, without the read path's flattening. Counting
                // it while contributing none of its measurements produced
                // a null AVERAGE, MINIMUM, MAXIMUM and LATEST for a record
                // full of values.
                SeriesSample series = (SeriesSample) s;
                boolean cumulativeSeries = type.getAggregationStyle()
                        == HealthAggregationStyle.CUMULATIVE;
                for (int i = 0; i < series.size(); i++) {
                    long at = series.getSampleStartMillis(i);
                    long until = series.getSampleEndMillis(i);
                    // Overlap, not start-only. A measurement may be an
                    // interval -- an interval-only type requires it -- and
                    // testing its start alone dropped one running
                    // 11:55-12:05 from a noon bucket entirely, while the
                    // same span as a scalar sample contributed its five
                    // minutes.
                    if (at == until
                            ? (at < from || at >= to)
                            : (until <= from || at >= to)) {
                        continue;
                    }
                    long pointOverlap = Math.min(to, until)
                            - Math.max(from, at);
                    if (pointOverlap < 0) {
                        pointOverlap = 0;
                    }
                    long pointSpan = until - at;
                    count++;
                    inBucket.add(series.toQuantitySample(i));
                    // Same rule as a scalar sample: a cumulative value
                    // straddling the boundary contributes in proportion, a
                    // discrete one counts whole.
                    weights.add(Double.valueOf(
                            cumulativeSeries && pointSpan > 0
                                    ? (double) pointOverlap
                                            / (double) pointSpan
                                    : 1.0));
                    durations.add(Double.valueOf(
                            pointSpan <= 0 ? 1 : pointOverlap));
                    if (pointSpan > 0 && pointOverlap > 0) {
                        covered.add(new long[] {
                                Math.max(from, at), Math.min(to, until),
                        });
                    }
                }
            } else {
                count++;
            }
            if (s instanceof QuantitySample) {
                inBucket.add((QuantitySample) s);
                // Cumulative quantities are divisible, so a sample that
                // straddles a boundary contributes in proportion. Discrete
                // ones are not -- half a heart rate is not a heart rate --
                // so they count whole and only weight the average.
                boolean cumulative = span > 0
                        && type.getAggregationStyle()
                                == HealthAggregationStyle.CUMULATIVE;
                weights.add(Double.valueOf(cumulative
                        ? (double) overlap / (double) span : 1.0));
                durations.add(Double.valueOf(span <= 0 ? 1 : overlap));
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
                bucket.put(type, metric,
                        new HealthQuantity(coveredMillis(covered),
                                HealthUnit.MILLISECOND));
                continue;
            }
            if (inBucket.isEmpty() || unit == null) {
                continue;
            }
            bucket.put(type, metric, new HealthQuantity(
                    compute(metric, inBucket, unit, weights, durations),
                    unit));
        }
    }

    /// Time actually covered by a set of spans, counting an overlap once.
    ///
    /// Both mobile stores fall back to this shared aggregation, so summing
    /// the spans instead put the error everywhere: two sources recording
    /// the same walk, or one series whose measurements are intervals, and
    /// DURATION exceeded the bucket it was measured over. Clamping to the
    /// bucket width would have hidden that rather than fixed it -- the
    /// figure would still be wrong for every bucket wide enough not to hit
    /// the clamp.
    private static long coveredMillis(List<long[]> spans) {
        if (spans.isEmpty()) {
            return 0;
        }
        Collections.sort(spans, ByFrom.INSTANCE);
        long total = 0;
        long openFrom = spans.get(0)[0];
        long openTo = spans.get(0)[1];
        for (int iter = 1; iter < spans.size(); iter++) {
            long[] span = spans.get(iter);
            if (span[0] > openTo) {
                total += openTo - openFrom;
                openFrom = span[0];
                openTo = span[1];
            } else if (span[1] > openTo) {
                openTo = span[1];
            }
        }
        return total + openTo - openFrom;
    }

    /// Sorts spans by start so the merge above only has to look at the one
    /// span it currently has open.
    private static final class ByFrom implements java.util.Comparator<long[]> {

        static final ByFrom INSTANCE = new ByFrom();

        @Override
        public int compare(long[] a, long[] b) {
            if (a[0] == b[0]) {
                return 0;
            }
            return a[0] < b[0] ? -1 : 1;
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
            List<QuantitySample> in, HealthUnit unit, List<Double> weights,
            List<Double> durations) {
        if (metric == AggregateMetric.TOTAL) {
            double sum = 0;
            for (int i = 0; i < in.size(); i++) {
                sum += in.get(i).getValue(unit)
                        * weights.get(i).doubleValue();
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
        // AVERAGE, weighted by the time each sample spent in this bucket.
        double weighted = 0;
        double totalWeight = 0;
        for (int i = 0; i < in.size(); i++) {
            double weight = Math.max(1, durations.get(i).doubleValue());
            weighted += in.get(i).getValue(unit) * weight;
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
        writeChunk(splitOversizedSeries(prepared,
                Math.max(1, getMaxWriteBatchSize())), 0,
                new HealthWriteResult(), out);
        return out;
    }

    /// One record per measurement, so the chunker's budget is honest.
    ///
    /// A series reaches the platform as one record per point -- see
    /// `HealthWire.encodeSamples` -- while the chunker counted it as a
    /// single sample. A 5,000-point series therefore went to Health
    /// Connect as 5,000 records in one call, past its 1,000-record cap,
    /// and the whole write was rejected even though `write` documents
    /// automatic chunking. Anything longer than a batch is split into
    /// several series first, so no single one can overflow a chunk on its
    /// own.
    ///
    /// The pieces share the original's identifier: they came from one
    /// record and there is nothing else to call them.
    private static List<HealthSample> splitOversizedSeries(
            List<HealthSample> samples, int max) {
        List<HealthSample> out = new ArrayList<HealthSample>();
        for (HealthSample sample : samples) {
            if (!(sample instanceof SeriesSample)
                    || ((SeriesSample) sample).size() <= max) {
                out.add(sample);
                continue;
            }
            SeriesSample series = (SeriesSample) sample;
            for (int from = 0; from < series.size(); from += max) {
                out.add(slice(series, from,
                        Math.min(series.size(), from + max)));
            }
        }
        return out;
    }

    /// Measurements `[from,to)` of `series` as a series of their own.
    private static SeriesSample slice(SeriesSample series, int from, int to) {
        int n = to - from;
        long[] starts = new long[n];
        long[] ends = new long[n];
        double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            starts[i] = series.getSampleStartMillis(from + i);
            ends[i] = series.getSampleEndMillis(from + i);
            values[i] = series.getSampleValue(from + i, series.getUnit());
        }
        // The span is the extent of the slice, not its first start and
        // last end. Nothing documents the arrays as chronological, and on
        // an unordered series those two are not the bounds -- the factory
        // would reject the slice outright when the last point is the
        // earliest, or accept a span that excludes points it contains.
        long spanStart = starts[0];
        long spanEnd = ends[0];
        for (int i = 1; i < n; i++) {
            spanStart = Math.min(spanStart, starts[i]);
            spanEnd = Math.max(spanEnd, ends[i]);
        }
        SeriesSample part = SeriesSample.create(series.getType(), spanStart,
                spanEnd, starts, ends, values, series.getUnit());
        part.setId(series.getId());
        part.setSource(series.getSource());
        part.setRecordingMethod(series.getRecordingMethod());
        for (Map.Entry<String, String> e : series.getMetadata().entrySet()) {
            part.putMetadata(e.getKey(), e.getValue());
        }
        return part;
    }

    /// How many platform records a sample becomes.
    private static int recordCost(HealthSample sample) {
        return sample instanceof SeriesSample
                ? Math.max(1, ((SeriesSample) sample).size()) : 1;
    }

    private void writeChunk(final List<HealthSample> all, final int from,
            final HealthWriteResult accumulated,
            final AsyncResource<HealthWriteResult> out) {
        if (from >= all.size()) {
            out.complete(accumulated);
            return;
        }
        // Counted in records rather than in samples, because that is what
        // the platform's batch cap counts.
        int max = Math.max(1, getMaxWriteBatchSize());
        int to = from;
        int cost = 0;
        while (to < all.size()) {
            int next = recordCost(all.get(to));
            if (to > from && cost + next > max) {
                break;
            }
            cost += next;
            to++;
        }
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
                            // Earlier chunks are already in the store.
                            // Reporting only the failure makes a caller
                            // retry the whole batch and duplicate them.
                            HealthException wrapped =
                                    err instanceof HealthException
                                    ? (HealthException) err
                                    : new HealthException(
                                            HealthError.UNKNOWN,
                                            "the write failed partway",
                                            err);
                            wrapped.setPartialResult(accumulated);
                            out.error(wrapped);
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
        armTimeout(chunkResult);
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
        if (sample instanceof SeriesSample) {
            return validateSeriesForWrite((SeriesSample) sample, type);
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
        // The source travels too, as it already does on the read-side
        // conversion. Dropping it here made the same measurement
        // filterable or not depending on which unit the caller happened
        // to write it in: a weight in kilograms kept its source and one
        // in pounds came back excluded from every addSource() query and
        // every source-filtered aggregate.
        converted.setSource(q.getSource());
        // Metadata travels with the sample. Losing it merely because the
        // caller asked for pounds instead of kilograms would drop the
        // correlation identifier this API tells them to keep there.
        //
        // Copied entry by entry: getMetadata() hands back an unmodifiable
        // view, so putAll on it throws and would have failed the whole
        // write rather than merely losing the metadata.
        for (Map.Entry<String, String> e : q.getMetadata().entrySet()) {
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
        armTimeout(out);
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
        // Reads validate this; subscriptions did not, so an unsupported
        // store handed back an active handle that delivered nothing
        // forever, and a supported one accepted a type whose every later
        // drain failed.
        if (!isSupported()) {
            throw new IllegalStateException(
                    "health data is not available on this platform");
        }
        for (HealthDataType t : request.getTypes()) {
            if (!isTypeSupported(t)) {
                throw new IllegalArgumentException(t.getId()
                        + " is not available on this platform, so a"
                        + " subscription for it could never deliver");
            }
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
                request.getId(), request.getTypes(), isPushDelivery(),
                request.isDeliverSamples(), request.isIncludeDeletions(),
                request.getMaxSamplesPerBatch());
        HealthAnchor stored = loadAnchor(request.getId());
        // The new handle carries the persisted cursor.
        //
        // The anchor used to reach doSubscribe() alone, which neither
        // mobile store overrides, while both drains read sub.getAnchor().
        // So re-registering an id -- replacing a listener, or restoring a
        // subscription at launch -- started the next drain from a fresh
        // baseline and silently discarded everything accumulated since the
        // last one.
        if (stored != null) {
            sub.noteDelivery(stored, 0L);
        }
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
        if (listener != null) {
            // An id previously bound to a background listener class keeps
            // that binding in Preferences, and restoration after a
            // relaunch resolves it. Replacing such a subscription with an
            // in-memory listener would then deliver its changes to the
            // very class the app had just replaced. The Class overload
            // writes the binding back immediately after this returns.
            Preferences.delete(PREF_LISTENER + request.getId());
        }
        rememberSubscription(request);
        if (isSupported()) {
            doSubscribe(request, stored);
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

    /// Drains pending changes for every active subscription, resolving
    /// with the number of batches delivered.
    ///
    /// **Your app decides when this happens.** Nothing here hooks the
    /// application lifecycle, so call it when you come to the foreground,
    /// and from your background-fetch handler -- Health Connect never
    /// wakes the app on its own, and this release registers no
    /// `HKObserverQuery` either, so nothing else will notice new data
    /// while your app is closed. [HealthSubscription#isPushDelivery()]
    /// answers false everywhere for the same reason.
    ///
    /// Overlapping calls are coalesced: a drain started while one is
    /// already running does not read the same change window a second
    /// time, it resolves alongside the one in flight.
    public final AsyncResource<Integer> drainChanges() {
        AsyncResource<Integer> out = new AsyncResource<Integer>();
        ensureSubscriptionsRestored();
        // A second drain would snapshot the same anchors and read the
        // same window, delivering every batch twice and letting two
        // callbacks persist cursors in whatever order they finished.
        synchronized (subscriptions) {
            if (drainInFlight) {
                pendingDrains.add(out);
                return out;
            }
            drainInFlight = true;
        }
        if (!isSupported()) {
            finishDrain(out, Integer.valueOf(0), null);
            return out;
        }
        List<HealthSubscription> subs = getSubscriptions();
        if (subs.isEmpty()) {
            finishDrain(out, Integer.valueOf(0), null);
            return out;
        }
        // The public resource resolves only once the deliveries this drain
        // queued have actually run. The ports complete their own resource
        // as soon as they have handed the batches over, but delivery hops
        // through callSerially -- so an app polling again from the
        // completion callback used to find the cursor unmoved and fetch
        // the same page a second time.
        AsyncResource<Integer> portResult = new AsyncResource<Integer>();
        portResult.onResult(new DrainGate(this, out));
        // Armed like every other platform call. Without it a delegate that
        // never calls back leaves DrainGate unrun and `drainInFlight` set
        // for the life of the process, so this drain and every coalesced
        // one behind it wait forever -- the coalescing turning one lost
        // callback into a permanently dead subscription.
        armTimeout(portResult);
        doDrainChanges(subs, portResult);
        return out;
    }

    /// Resolves this drain and every call that arrived while it ran.
    ///
    /// The coalesced callers get the same answer as the drain they waited
    /// on, because it is the same work: reading the window a second time
    /// would deliver every batch twice.
    void finishDrain(AsyncResource<Integer> out, Integer value,
            Throwable error) {
        List<AsyncResource<Integer>> waiting;
        synchronized (subscriptions) {
            drainInFlight = false;
            waiting = new ArrayList<AsyncResource<Integer>>(pendingDrains);
            pendingDrains.clear();
        }
        resolve(out, value, error);
        for (AsyncResource<Integer> pending : waiting) {
            resolve(pending, value, error);
        }
    }

    private static void resolve(AsyncResource<Integer> out, Integer value,
            Throwable error) {
        if (out.isDone()) {
            return;
        }
        if (error != null) {
            out.error(error);
        } else {
            out.complete(value);
        }
    }

    /// Holds a drain's completion until its queued deliveries have run.
    private static final class DrainGate
            implements AsyncResult<Integer> {
        private final HealthStore store;
        private final AsyncResource<Integer> out;

        DrainGate(HealthStore store, AsyncResource<Integer> out) {
            this.store = store;
            this.out = out;
        }

        @Override
        public void onReady(Integer value, Throwable error) {
            if (error != null) {
                store.finishDrain(out, null, error);
                return;
            }
            store.whenDeliveriesDrain(out, value);
        }
    }

    /// Completes `out` once no delivery is outstanding.
    void whenDeliveriesDrain(final AsyncResource<Integer> out,
            final Integer value) {
        boolean now;
        synchronized (subscriptions) {
            now = pendingDeliveries <= 0;
            if (!now) {
                drainGates.add(new Object[] { out, value });
            }
        }
        if (now) {
            finishDrain(out, value, null);
        }
    }

    /// Releases any drain waiting on the delivery queue.
    private void releaseDrainGates() {
        List<Object[]> ready = null;
        synchronized (subscriptions) {
            if (pendingDeliveries > 0 || drainGates.isEmpty()) {
                return;
            }
            ready = new ArrayList<Object[]>(drainGates);
            drainGates.clear();
        }
        for (Object[] g : ready) {
            // Through finishDrain, so the coalesced callers waiting behind
            // this drain are released with it.
            finishDrain((AsyncResource<Integer>) g[0], (Integer) g[1], null);
        }
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
    ///
    /// #### Returns
    ///
    /// How many deliveries were queued -- `0` when nothing was, and more
    /// than one when the subscription's per-batch cap split the page.
    /// Ports add this to the count they resolve `drainChanges` with, which
    /// is documented as a number of batches: returning a yes/no counted a
    /// page of 250 additions capped at 100 as one batch while the listener
    /// was called three times.
    protected final int fireChanges(final HealthChangeBatch batch) {
        if (batch == null) {
            return 0;
        }
        final String id = batch.getSubscriptionId();
        HealthChangeListener live;
        HealthSubscription sub;
        synchronized (subscriptions) {
            live = liveListeners.get(id);
            sub = subscriptions.get(id);
        }
        if (sub == null) {
            return 0;
        }
        final HealthChangeListener target = live != null ? live
                : resolveBackgroundListener(id);
        if (target == null) {
            // Nothing to hand it to -- a subscription restored after a
            // relaunch with no live listener and no persisted class. The
            // caller counts what it delivers, and a drain reporting
            // batches nobody received reads as handled when it was not.
            return 0;
        }
        final HealthSubscription subscription = sub;
        // A cap splits the batch into successive deliveries rather than
        // discarding the tail. Every chunk but the last carries no anchor,
        // so the cursor only advances once the whole platform page has
        // been handed over -- withholding it outright, as an earlier fix
        // did, simply re-read the same page forever and the samples past
        // the cap were never reachable at all.
        // Queued one at a time, not all at once. Enqueuing the whole
        // sequence up front meant a listener that threw on an early chunk
        // still had the final chunk run and persist the page anchor,
        // skipping the failed chunk for good.
        List<HealthChangeBatch> chunks = applyOptions(batch, sub);
        synchronized (subscriptions) {
            pendingDeliveries += chunks.size();
        }
        Display.getInstance().callSerially(
                makeDeliveryRunnable(this, target, chunks, 0,
                        subscription));
        return chunks.size();
    }

    /// Built in a static method so the `Runnable` carries no synthetic
    /// reference to the enclosing store (SpotBugs
    /// `SIC_INNER_SHOULD_BE_STATIC_ANON`).
    private static Runnable makeDeliveryRunnable(final HealthStore store,
            final HealthChangeListener listener,
            final List<HealthChangeBatch> chunks, final int index,
            final HealthSubscription subscription) {
        final HealthChangeBatch batch = chunks.get(index);
        return new Runnable() {
            @Override
            public void run() {
                boolean queuedNext = false;
                try {
                    queuedNext = runDelivery();
                } finally {
                    // Whatever stops this chunk -- a listener that threw,
                    // or a subscription cancelled before the chunk ran --
                    // also stops every chunk behind it, and those will
                    // never be queued. They are accounted for here or the
                    // drain waiting on them never resolves, and every
                    // later drain stays gated behind it too.
                    store.noteDeliveryDone(queuedNext ? 0
                            : chunks.size() - index - 1);
                }
            }

            private boolean runDelivery() {
                // The subscription can be stopped between queuing this and
                // running it. Delivering then would call a listener the
                // app has cancelled, and persisting the cursor would undo
                // what unsubscribe() promised to discard -- or overwrite
                // the cursor of a new subscription reusing the same id.
                //
                // Reported as "nothing queued", because nothing was: an
                // earlier version answered as though the tail were still
                // coming, and the chunks it never queued stayed counted
                // for the life of the process.
                if (!store.isStillRegistered(subscription)) {
                    return false;
                }
                try {
                    listener.healthDataChanged(batch);
                } catch (Throwable t) {
                    // A listener that throws must not cost us the cursor
                    // advance for every *later* batch, but it also must
                    // not advance past data it failed on -- so log and
                    // leave the anchor where it was. The rest of the page
                    // is abandoned too: delivering past a chunk the app
                    // could not handle would strand it permanently.
                    com.codename1.io.Log.e(t);
                    return false;
                }
                if (index + 1 < chunks.size()) {
                    Display.getInstance().callSerially(
                            makeDeliveryRunnable(store, listener, chunks,
                                    index + 1, subscription));
                }
                if (batch.isResyncRequired()) {
                    // The cursor the platform rejected is dropped here,
                    // after the listener has been told to resynchronise --
                    // not when the port noticed. Dropping it earlier means
                    // a listener that threw, or a process that died before
                    // the queued delivery ran, loses the notification *and*
                    // the expired token, so the next drain quietly starts a
                    // fresh baseline and the missed history is never read.
                    store.clearAnchor(batch.getSubscriptionId());
                } else if (batch.getAnchor() != null) {
                    store.storeAnchor(batch.getSubscriptionId(),
                            batch.getAnchor());
                    subscription.noteDelivery(batch.getAnchor(),
                            System.currentTimeMillis());
                } else {
                    // A batch with no anchor is one we deliberately did not
                    // advance past -- a truncated page. Overwriting the
                    // in-memory cursor with null would send the next drain
                    // back to a fresh baseline, which on Android means
                    // discarding the change token entirely.
                    subscription.noteDelivery(subscription.getAnchor(),
                            System.currentTimeMillis());
                }
                return true;
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
    /// Whether a drain has started and not yet resolved. Guarded by
    /// `subscriptions`.
    private boolean drainInFlight;
    /// Drains that arrived while one was already running.
    private final List<AsyncResource<Integer>> pendingDrains =
            new ArrayList<AsyncResource<Integer>>();
    /// Deliveries queued but not yet run; a drain waits for zero.
    private int pendingDeliveries;
    private final List<Object[]> drainGates = new ArrayList<Object[]>();

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

    /// Drops a subscription's cursor, in memory and on disk.
    ///
    /// For a cursor the platform has rejected outright -- a Health Connect
    /// change token that has aged out, say. Clearing only the persisted
    /// copy is not enough: the drains read the live handle, so the next
    /// one would resend the very token that just failed and keep failing
    /// identically forever.
    protected final void clearAnchor(String subscriptionId) {
        storeAnchor(subscriptionId, null);
        HealthSubscription sub;
        synchronized (subscriptions) {
            sub = subscriptions.get(subscriptionId);
        }
        if (sub != null) {
            sub.noteDelivery(null, System.currentTimeMillis());
        }
    }

    /// Applies the subscription's delivery options to a batch.
    ///
    /// These were settable on SubscriptionRequest and read by nothing, so
    /// a notify-only subscription still received full sample payloads, an
    /// excluded deletion was still delivered, and a per-batch cap was
    /// ignored. Applied here so every port obeys them without knowing they
    /// exist.
    private static List<HealthChangeBatch> applyOptions(
            HealthChangeBatch batch, HealthSubscription sub) {
        List<HealthChangeBatch> out = new ArrayList<HealthChangeBatch>();
        List<HealthSample> added = batch.getAdded();
        List<String> deleted = batch.getDeletedSampleIds();
        boolean changed = false;
        if (!sub.isDeliverSamples() && !added.isEmpty()) {
            added = new ArrayList<HealthSample>();
            changed = true;
        }
        if (!sub.isIncludeDeletions() && !deleted.isEmpty()) {
            deleted = new ArrayList<String>();
            changed = true;
        }
        int cap = sub.getMaxSamplesPerBatch();
        if (cap > 0 && added.size() > cap) {
            for (int i = 0; i < added.size(); i += cap) {
                int to = Math.min(added.size(), i + cap);
                boolean last = to == added.size();
                out.add(new HealthChangeBatch(batch.getSubscriptionId(),
                        batch.getTypes(),
                        new ArrayList<HealthSample>(added.subList(i, to)),
                        last ? deleted : new ArrayList<String>(),
                        batch.isResyncRequired(),
                        last ? batch.getAnchor() : null,
                        batch.getDeadlineMillis(),
                        !last || batch.hasMore()));
            }
            return out;
        }
        if (!changed) {
            out.add(batch);
            return out;
        }
        out.add(new HealthChangeBatch(batch.getSubscriptionId(),
                batch.getTypes(), added, deleted, batch.isResyncRequired(),
                batch.getAnchor(), batch.getDeadlineMillis(),
                batch.hasMore()));
        return out;
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
                    req.getId(), req.getTypes(), isPushDelivery(),
                    req.isDeliverSamples(), req.isIncludeDeletions(),
                    req.getMaxSamplesPerBatch());
            HealthAnchor restored = loadAnchor(req.getId());
            sub.seedAnchor(restored);
            synchronized (subscriptions) {
                subscriptions.put(req.getId(), sub);
            }
            if (isSupported()) {
                doSubscribe(req, restored);
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
            String part = parts.get(i);
            if (part.startsWith(OPTIONS_PREFIX)) {
                applyStoredOptions(req, part);
                continue;
            }
            HealthDataType t = HealthDataType.forId(part);
            // A type this build no longer knows is skipped rather than
            // failing the whole restore -- a downgraded app should keep
            // the subscriptions it still understands.
            if (t != null) {
                req.addType(t);
            }
        }
        return req.getTypes().isEmpty() ? null : req;
    }

    /// Marks the field carrying the delivery options.
    ///
    /// A prefix rather than a fixed position, so an entry written by a
    /// build that did not persist them still parses -- its types simply
    /// follow the id as before and the defaults apply.
    private static final String OPTIONS_PREFIX = "o:";

    private static void applyStoredOptions(SubscriptionRequest req,
            String field) {
        String v = field.substring(OPTIONS_PREFIX.length());
        if (v.length() < 2) {
            return;
        }
        req.setDeliverSamples(v.charAt(0) == '1');
        req.setIncludeDeletions(v.charAt(1) == '1');
        try {
            req.setMaxSamplesPerBatch(Integer.parseInt(v.substring(2)));
        } catch (NumberFormatException ex) {
            // Leave the default rather than failing the whole restore.
        }
    }

    private void rememberSubscription(SubscriptionRequest request) {
        StringBuilder sb = new StringBuilder();
        List<String> kept = readStoredEntries(request.getId());
        for (String keptItem : kept) {
            sb.append(keptItem).append('\n');
        }
        sb.append(request.getId());
        // The delivery options ride along in a field the older format did
        // not have. Without them a restored notify-only subscription began
        // delivering full payloads after a restart, excluded deletions
        // came back, and a configured cap was silently lost.
        sb.append('\t').append(OPTIONS_PREFIX)
          .append(request.isDeliverSamples() ? '1' : '0')
          .append(request.isIncludeDeletions() ? '1' : '0')
          .append(request.getMaxSamplesPerBatch());
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
                            range.getStartMillis(), range.getEndMillis()))
                    // Without this the fallback inherits SampleQuery's
                    // default page limit and quietly computes a month of
                    // heart rate from its first 10,000 samples, reporting
                    // a total that looks complete and is not.
                    .setLimit(Integer.MAX_VALUE);
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
        fail(out, HealthError.NOT_SUPPORTED,
                "health data is not available on this platform");
    }

    /// Fails inline, on the calling thread.
    ///
    /// Matches [com.codename1.impl.health.LocalHealthStore] rather than the
    /// mobile ports, which marshal to the EDT. Both are documented; see the
    /// note there for why the hop is not in place yet.
    private static void fail(AsyncResource out, HealthError error,
            String message) {
        out.error(new HealthException(error, message));
    }
}
