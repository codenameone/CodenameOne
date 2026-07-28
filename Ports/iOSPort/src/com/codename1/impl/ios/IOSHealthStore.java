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
package com.codename1.impl.ios;

import com.codename1.health.AggregateMetric;
import com.codename1.health.HealthAccess;
import com.codename1.health.HealthAuthorizationStatus;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthError;
import com.codename1.health.HealthAnchor;
import com.codename1.health.HealthChangeBatch;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthException;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthStore;
import com.codename1.health.HealthSubscription;
import com.codename1.health.HealthTimeRange;
import com.codename1.health.HealthWriteResult;
import com.codename1.health.SampleQuery;
import com.codename1.health.SamplePage;
import com.codename1.impl.health.HealthWire;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.List;

/// The HealthKit-backed store.
///
/// Samples cross the native boundary as tab-separated lines rather than
/// JSON -- see [HealthWire] -- because a year of heart rate is hundreds of
/// thousands of records and a JSON object graph of that size will exhaust
/// the ParparVM heap.
class IOSHealthStore extends HealthStore {

    /// How many records sharing a single millisecond the drain is willing
    /// to pull in one read -- see [#readTiedInstant]. Bounded rather than
    /// unlimited because a background delivery has a few seconds of wall
    /// clock and a ParparVM heap to stay inside; generous because the
    /// records beyond it cannot be paged to and are lost.
    private static final int TIED_INSTANT_LIMIT = 50000;

    private final IOSNative nativeInstance;

    IOSHealthStore(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
    }

    public boolean isSupported() {
        return nativeInstance.hkIsAvailable();
    }

    /// Asks the native type map rather than inferring from the portable
    /// type. Several quantity types carry a canonical unit but have no
    /// HealthKit identifier, and advertising those made them pass shared
    /// validation only for the query to fail as unsupported.
    public boolean isTypeSupported(HealthDataType type) {
        return type != null && isSupported()
                && nativeInstance.hkIsTypeSupported(type.getId());
    }

    public List<HealthDataType> getSupportedTypes() {
        List<HealthDataType> out = new ArrayList<HealthDataType>();
        for (HealthDataType t : HealthDataType.values()) {
            if (isTypeSupported(t)) {
                out.add(t);
            }
        }
        return out;
    }

    public boolean isWritable(HealthDataType type) {
        return isTypeSupported(type);
    }

    /// False: this build has no HealthKit delete native, so the store SPI
    /// falls through to NOT_SUPPORTED. Saying otherwise made a
    /// capability-gated delete fail for every valid input, which is worse
    /// than the caller knowing up front.
    public boolean isDeletable(HealthDataType type) {
        return false;
    }

    public List<AggregateMetric> getSupportedMetrics(HealthDataType type) {
        // Aggregation is done in shared code from raw samples so that the
        // bucket arithmetic -- daylight-saving boundaries, the
        // duration-weighted average -- has exactly one implementation
        // rather than one per platform that can drift apart.
        return new ArrayList<AggregateMetric>();
    }

    /// HealthKit can wake the app through `HKObserverQuery`, but this
    /// build does not register one yet, so nothing arrives while the app
    /// is closed. Reporting false is the honest answer: an app that
    /// branches on this needs to know it must drain for itself, and
    /// claiming push here would make it skip exactly that.
    public boolean isPushDelivery() {
        return false;
    }

    /// Changes are delivered by draining, not by the OS relaunching the
    /// app, so background delivery is not available on iOS in this build.
    public boolean isBackgroundDeliverySupported() {
        return false;
    }

    /// Truthful: HealthKit reports share (write) authorization.
    public HealthAuthorizationStatus getWriteAuthorizationStatus(
            HealthDataType type) {
        if (!isTypeSupported(type)) {
            return HealthAuthorizationStatus.NOT_SUPPORTED;
        }
        switch (nativeInstance.hkShareAuthorizationStatus(type.getId())) {
            case 2:
                return HealthAuthorizationStatus.AUTHORIZED;
            case 1:
                return HealthAuthorizationStatus.DENIED;
            default:
                return HealthAuthorizationStatus.NOT_DETERMINED;
        }
    }

    /// Always UNKNOWN, and deliberately so.
    ///
    /// HealthKit does not expose read authorization at all, because an app
    /// able to distinguish "denied" from "no data" could infer that a user
    /// is hiding a pregnancy or a prescription. Returning anything else
    /// here would be inventing an answer.
    public HealthAuthorizationStatus getReadAuthorizationStatus(
            HealthDataType type) {
        return isSupported() ? HealthAuthorizationStatus.UNKNOWN
                : HealthAuthorizationStatus.NOT_SUPPORTED;
    }

    static HealthException toException(int code, String message) {
        HealthError error;
        switch (code) {
            case 1:
                error = HealthError.NOT_SUPPORTED;
                break;
            case 2:
                error = HealthError.UNAUTHORIZED;
                break;
            case 4:
                error = HealthError.INVALID_ARGUMENT;
                break;
            case 6:
                // The store is encrypted at rest and unreadable before
                // first unlock -- exactly when a background observer
                // fires. Retryable, and must never be collapsed into
                // "no data".
                error = HealthError.DATABASE_INACCESSIBLE;
                break;
            case 7:
                error = HealthError.USER_CANCELED;
                break;
            case 8:
                error = HealthError.NOT_SUPPORTED;
                break;
            case 9:
                error = HealthError.ANCHOR_EXPIRED;
                break;
            case 10:
                error = HealthError.RATE_LIMITED;
                break;
            default:
                error = HealthError.UNKNOWN;
                break;
        }
        return new HealthException(error,
                message == null ? "HealthKit call failed" : message);
    }

    protected void doRequestAuthorization(List<HealthAccess> access,
            AsyncResource<Boolean> out) {
        List<String> read = new ArrayList<String>();
        List<String> share = new ArrayList<String>();
        for (int i = 0; i < access.size(); i++) {
            HealthAccess a = access.get(i);
            if (a.isWrite()) {
                share.add(a.getType().getId());
            } else {
                read.add(a.getType().getId());
            }
        }
        int id = IOSHealth.takeId(out);
        nativeInstance.hkRequestAuthorization(id,
                read.toArray(new String[read.size()]),
                share.toArray(new String[share.size()]));
    }

    protected void doReadSamples(SampleQuery query,
            AsyncResource<SamplePage> out) {
        List<HealthDataType> types = query.getTypes();
        if (types.size() != 1) {
            // HKSampleQuery is per sample type. The shared layer could be
            // taught to fan out, but until it is, saying so beats silently
            // reading only the first type.
            out.error(new HealthException(HealthError.INVALID_ARGUMENT,
                    "HealthKit reads one data type per query; issue a"
                            + " separate query per type"));
            return;
        }
        HealthTimeRange range =
                query.getTimeRange().resolve(System.currentTimeMillis());
        // The limit is asked for one higher than the caller wanted, so a
        // full page can be told apart from a page that merely ended. There
        // is no HealthKit continuation token here, so the honest signal is
        // SamplePage.isTruncated() -- claiming a partial history was
        // complete is what silently lost everything past the first page.
        // Sources go to HealthKit, not to the shared post-filter. The
        // limit is applied by the query, so a page whose first records all
        // belong to another app was filtered down to nothing afterwards --
        // and with no continuation token there was no way to reach the
        // matching records behind them.
        StringBuilder sources = new StringBuilder();
        for (String bundleId : query.getSources()) {
            if (sources.length() > 0) {
                sources.append('\t');
            }
            sources.append(bundleId);
        }
        int id = IOSHealth.takeId(out, query.getLimit());
        nativeInstance.hkQuerySamples(id, types.get(0).getId(),
                range.getStartMillis(), range.getEndMillis(),
                query.getLimit() == Integer.MAX_VALUE ? query.getLimit()
                        : query.getLimit() + 1,
                !query.isSortDescending(), sources.toString());
    }

    protected void doWrite(List<HealthSample> samples,
            AsyncResource<HealthWriteResult> out) {
        HealthSample rejected = HealthWire.unsupportedForWrite(samples);
        if (rejected != null) {
            // The payload cannot carry this shape, and the native side
            // reports an empty batch as a successful write of nothing.
            out.error(new HealthException(HealthError.TYPE_NOT_SUPPORTED,
                    rejected.getType().getId() + " cannot be written to"
                            + " HealthKit through this API"));
            return;
        }
        int id = IOSHealth.takeId(out);
        nativeInstance.hkSaveSamples(id, HealthWire.encodeSamples(samples));
    }

    // ==================================================================
    // change draining
    // ==================================================================

    /// Drains by re-reading each subscription's types over the window that
    /// has elapsed since its last drain, with the anchor holding that
    /// timestamp.
    ///
    /// This is not `HKAnchoredObjectQuery`: a real anchor would also
    /// report deletions and would not re-read samples already seen. It is
    /// what makes subscriptions work at all on iOS today, and the window
    /// is closed only after the batch has been handled, so a crash re-reads
    /// rather than skips. Deletions are not reported --
    /// [#isPushDelivery()] and [#isBackgroundDeliverySupported()] both
    /// answer false so an app can tell how much this is worth.
    protected void doDrainChanges(List<HealthSubscription> subscriptions,
            AsyncResource<Integer> out) {
        drainFrom(new ArrayList<HealthSubscription>(subscriptions), 0, 0,
                out);
    }

    void drainFrom(final List<HealthSubscription> subs,
            final int index, final int delivered,
            final AsyncResource<Integer> out) {
        if (index >= subs.size()) {
            out.complete(Integer.valueOf(delivered));
            return;
        }
        final HealthSubscription sub = subs.get(index);
        final long now = System.currentTimeMillis();
        long since = anchorMillis(sub, now);
        List<HealthDataType> types = sub.getTypes();
        if (types.isEmpty()) {
            drainFrom(subs, index + 1, delivered, out);
            return;
        }
        if (since >= now) {
            // A subscription with no cursor yet. Fire an empty batch so the
            // anchor gets persisted and the next drain has a window to read
            // over -- without this the branch repeats forever and the
            // subscription never delivers anything at all.
            boolean sent = fireChanges(new HealthChangeBatch(sub.getId(), sub.getTypes(),
                    null, null, false,
                    // Foreground: no OS deadline. Reporting 0 made a listener
                    // that budgets against getDeadlineMillis() treat every
                    // ordinary drain as already out of time.
                    HealthAnchor.of(String.valueOf(now)), Long.MAX_VALUE, false));;
            // Counted only when a listener actually received it: a
            // subscription restored with no live listener and no
            // persisted class queues nothing, and a drain reporting
            // batches nobody got reads as handled.
            drainFrom(subs, index + 1, sent ? delivered + 1 : delivered,
                    out);
            return;
        }
        readTypes(subs, index, delivered, out, types, 0,
                new ArrayList<HealthSample>(), since, now, now);
    }

    /// HealthKit queries one type at a time, so a subscription over three
    /// types is three reads accumulated into one batch.
    ///
    /// `safeUntil` is how far the cursor may move. It is `now` until a
    /// type comes back truncated, at which point it drops to the last
    /// sample that type actually produced -- see [#advanceTo(long, long,
    /// SamplePage)].
    private void readTypes(final List<HealthSubscription> subs,
            final int index, final int delivered,
            final AsyncResource<Integer> out,
            final List<HealthDataType> types, final int typeIndex,
            final List<HealthSample> collected, final long since,
            final long now, final long safeUntil) {
        if (typeIndex >= types.size()) {
            HealthSubscription sub = subs.get(index);
            // The cursor moves to the end of what was read, not to the end
            // of the window that was asked for. HealthKit hands back no
            // continuation token, so a window holding more samples than one
            // query returns is only ever read once -- anchoring at `now`
            // regardless meant everything past the limit was skipped for
            // good, silently, on exactly the busiest subscriptions.
            boolean sent = fireChanges(new HealthChangeBatch(sub.getId(), sub.getTypes(),
                    collected, null, false,
                    // Foreground: no OS deadline. Reporting 0 made a listener
                    // that budgets against getDeadlineMillis() treat every
                    // ordinary drain as already out of time.
                    HealthAnchor.of(String.valueOf(safeUntil)), Long.MAX_VALUE,
                    safeUntil < now));;
            // Counted only when a listener actually received it: a
            // subscription restored with no live listener and no
            // persisted class queues nothing, and a drain reporting
            // batches nobody got reads as handled.
            drainFrom(subs, index + 1, sent ? delivered + 1 : delivered,
                    out);
            return;
        }
        SampleQuery q = new SampleQuery()
                .addType(types.get(typeIndex))
                .setTimeRange(HealthTimeRange.between(since, now));
        // The page, not the accumulating read: only the page carries the
        // truncation flag, and that flag is the whole point here.
        readSamplePage(q).onResult(new ChangeRead(this, subs, index,
                delivered, out, types, typeIndex, collected, since, now,
                safeUntil));
    }

    /// Re-reads the single instant a page turned out to be entirely made
    /// of, so the cursor can step over it without losing what did not fit.
    ///
    /// The window is read oldest-first, so a truncated page whose *last*
    /// record starts at the cursor is a page in which every record starts
    /// there. That is the one shape a timestamp cursor cannot page
    /// through: the resume point is the last record read, which is where
    /// the cursor already was, so the next drain gets the identical prefix
    /// and the one after that too.
    ///
    /// The floor of `since + 1` used to break that loop by stepping over
    /// the whole instant -- discarding every record in it that had not
    /// been read, silently and for good. Draining the instant first makes
    /// the same step safe, because nothing is left behind to skip.
    ///
    /// A large instant is still deliverable: the base class splits a batch
    /// over the subscription's cap into cap-sized deliveries and anchors
    /// only the last, so this arrives as several handled deliveries rather
    /// than one unbounded payload.
    void readTiedInstant(final List<HealthSubscription> subs,
            final int index, final int delivered,
            final AsyncResource<Integer> out,
            final List<HealthDataType> types, final int typeIndex,
            final List<HealthSample> collected, final long since,
            final long now, final long safeUntil) {
        SampleQuery q = new SampleQuery()
                .addType(types.get(typeIndex))
                .setTimeRange(HealthTimeRange.between(since, since + 1))
                .setLimit(TIED_INSTANT_LIMIT);
        readSamplePage(q).onResult(new TiedInstantRead(this, subs, index,
                delivered, out, types, typeIndex, collected, since, now,
                safeUntil));
    }

    /// `true` when a truncated page holds nothing but records starting at
    /// the cursor, which is what makes the page unpageable.
    private static boolean isWhollyTied(SamplePage page, long since) {
        if (page == null || !page.isTruncated() || page.isEmpty()) {
            return false;
        }
        List<HealthSample> samples = page.getSamples();
        return samples.get(samples.size() - 1).getStartMillis() <= since;
    }

    /// How far the cursor may move given one type's page.
    ///
    /// A complete page leaves it where it was. A truncated one pulls it
    /// back to the last sample read, so the next drain resumes there
    /// rather than past the samples that did not fit. The window is read
    /// oldest-first, so the last sample is the newest one delivered.
    ///
    /// The resume point is that sample's **start**, not its end, because
    /// start is what HealthKit sorted the page by. A steps interval
    /// running 10:00-12:00 can be the last one returned while a record
    /// starting at 10:30 sorts after it and did not fit; resuming at 12:00
    /// would step straight over that record and lose it for good.
    /// Resuming at the start re-reads the samples already delivered in
    /// that final instant, which is the same duplicate-rather-than-skip
    /// trade the drain makes everywhere else.
    ///
    /// Never earlier than `since + 1`: a window whose very first sample
    /// overflows the limit would otherwise re-read the same page forever.
    /// That floor is a backstop only -- the case it guards against is
    /// handled before this is reached, by [#readTiedInstant], which drains
    /// the instant so that stepping over it loses nothing.
    private static long advanceTo(long safeUntil, long since,
            SamplePage page) {
        if (page == null || !page.isTruncated() || page.isEmpty()) {
            return safeUntil;
        }
        List<HealthSample> samples = page.getSamples();
        long last = samples.get(samples.size() - 1).getStartMillis();
        if (last <= since) {
            last = since + 1;
        }
        return Math.min(safeUntil, last);
    }

    private static long anchorMillis(HealthSubscription sub, long now) {
        HealthAnchor anchor = sub.getAnchor();
        if (anchor == null) {
            return now;
        }
        try {
            return Long.parseLong(anchor.toStorableString().trim());
        } catch (NumberFormatException ex) {
            // A cursor written by an earlier build in another shape. Start
            // the window here rather than re-reading the whole history.
            return now;
        }
    }

    /// Named rather than anonymous so the callback carries no implicit
    /// reference to the enclosing store.
    private static final class ChangeRead
            implements com.codename1.util.AsyncResult<SamplePage> {

        private final IOSHealthStore store;
        private final List<HealthSubscription> subs;
        private final int index;
        private final int delivered;
        private final AsyncResource<Integer> out;
        private final List<HealthDataType> types;
        private final int typeIndex;
        private final List<HealthSample> collected;
        private final long since;
        private final long now;
        private final long safeUntil;

        ChangeRead(IOSHealthStore store, List<HealthSubscription> subs,
                int index, int delivered, AsyncResource<Integer> out,
                List<HealthDataType> types, int typeIndex,
                List<HealthSample> collected, long since, long now,
                long safeUntil) {
            this.store = store;
            this.subs = subs;
            this.index = index;
            this.delivered = delivered;
            this.out = out;
            this.types = types;
            this.typeIndex = typeIndex;
            this.collected = collected;
            this.since = since;
            this.now = now;
            this.safeUntil = safeUntil;
        }

        public void onReady(SamplePage page, Throwable error) {
            if (error != null) {
                // Abandon this subscription's drain without firing a
                // batch. Continuing would deliver a partial result
                // anchored at `now`, and persisting that anchor would skip
                // everything the failed type produced in this window --
                // permanently. HealthKit reporting
                // HKErrorDatabaseInaccessible because the device is locked
                // is exactly when this happens, and it is retryable.
                store.drainFrom(subs, index + 1, delivered, out);
                return;
            }
            if (isWhollyTied(page, since)) {
                // Every record in a truncated page shares the cursor
                // instant, so paging cannot get past it -- see
                // readTiedInstant. The page is discarded rather than
                // collected because the re-read returns a superset of it.
                store.readTiedInstant(subs, index, delivered, out, types,
                        typeIndex, collected, since, now, safeUntil);
                return;
            }
            if (page != null) {
                collected.addAll(page.getSamples());
            }
            store.readTypes(subs, index, delivered, out, types,
                    typeIndex + 1, collected, since, now,
                    advanceTo(safeUntil, since, page));
        }
    }

    /// Collects one instant's records after a page turned out to hold
    /// nothing else, then lets the cursor step over that instant.
    private static final class TiedInstantRead
            implements com.codename1.util.AsyncResult<SamplePage> {

        private final IOSHealthStore store;
        private final List<HealthSubscription> subs;
        private final int index;
        private final int delivered;
        private final AsyncResource<Integer> out;
        private final List<HealthDataType> types;
        private final int typeIndex;
        private final List<HealthSample> collected;
        private final long since;
        private final long now;
        private final long safeUntil;

        TiedInstantRead(IOSHealthStore store, List<HealthSubscription> subs,
                int index, int delivered, AsyncResource<Integer> out,
                List<HealthDataType> types, int typeIndex,
                List<HealthSample> collected, long since, long now,
                long safeUntil) {
            this.store = store;
            this.subs = subs;
            this.index = index;
            this.delivered = delivered;
            this.out = out;
            this.types = types;
            this.typeIndex = typeIndex;
            this.collected = collected;
            this.since = since;
            this.now = now;
            this.safeUntil = safeUntil;
        }

        public void onReady(SamplePage page, Throwable error) {
            if (error != null) {
                // Same reasoning as the ordinary page read: firing a
                // partial batch would persist an anchor past records this
                // read never returned.
                store.drainFrom(subs, index + 1, delivered, out);
                return;
            }
            long cursor = since + 1;
            if (page != null) {
                collected.addAll(page.getSamples());
                if (page.isTruncated()) {
                    // Beyond TIED_INSTANT_LIMIT records at one millisecond
                    // the cursor has to step over the remainder anyway --
                    // the alternative is a drain that never terminates.
                    // Said out loud, because it is data loss, and because
                    // no device produces this without something writing
                    // every record under a single stamped timestamp.
                    com.codename1.io.Log.p("CN1 Health: more than "
                            + TIED_INSTANT_LIMIT + " records share the"
                            + " timestamp " + since + " for "
                            + types.get(typeIndex).getId() + "; the"
                            + " remainder cannot be paged and is skipped");
                }
            }
            store.readTypes(subs, index, delivered, out, types,
                    typeIndex + 1, collected, since, now,
                    Math.min(safeUntil, cursor));
        }
    }
}
