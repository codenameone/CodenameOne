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

    private final IOSNative nativeInstance;

    IOSHealthStore(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
    }

    public boolean isSupported() {
        return nativeInstance.hkIsAvailable();
    }

    public boolean isTypeSupported(HealthDataType type) {
        return type != null && isSupported()
                && type.getCanonicalUnit() != null;
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

    public boolean isDeletable(HealthDataType type) {
        return isTypeSupported(type);
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
        int id = IOSHealth.takeId(out);
        nativeInstance.hkQuerySamples(id, types.get(0).getId(),
                range.getStartMillis(), range.getEndMillis(),
                query.getLimit(), !query.isSortDescending());
    }

    protected void doWrite(List<HealthSample> samples,
            AsyncResource<HealthWriteResult> out) {
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

    private void drainFrom(final List<HealthSubscription> subs,
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
        if (types.isEmpty() || since >= now) {
            drainFrom(subs, index + 1, delivered, out);
            return;
        }
        readTypes(subs, index, delivered, out, types, 0,
                new ArrayList<HealthSample>(), since, now);
    }

    /// HealthKit queries one type at a time, so a subscription over three
    /// types is three reads accumulated into one batch.
    private void readTypes(final List<HealthSubscription> subs,
            final int index, final int delivered,
            final AsyncResource<Integer> out,
            final List<HealthDataType> types, final int typeIndex,
            final List<HealthSample> collected, final long since,
            final long now) {
        if (typeIndex >= types.size()) {
            HealthSubscription sub = subs.get(index);
            fireChanges(new HealthChangeBatch(sub.getId(), sub.getTypes(),
                    collected, null, false,
                    HealthAnchor.of(String.valueOf(now)), 0L, false));
            drainFrom(subs, index + 1, delivered + 1, out);
            return;
        }
        SampleQuery q = new SampleQuery()
                .addType(types.get(typeIndex))
                .setTimeRange(HealthTimeRange.between(since, now));
        readSamples(q).onResult(new ChangeRead(this, subs, index, delivered,
                out, types, typeIndex, collected, since, now));
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
            implements com.codename1.util.AsyncResult<List<HealthSample>> {

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

        ChangeRead(IOSHealthStore store, List<HealthSubscription> subs,
                int index, int delivered, AsyncResource<Integer> out,
                List<HealthDataType> types, int typeIndex,
                List<HealthSample> collected, long since, long now) {
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
        }

        public void onReady(List<HealthSample> page, Throwable error) {
            // An unreadable type must not strand the rest of the batch.
            // The window stays open, so the next drain retries it.
            if (page != null && error == null) {
                collected.addAll(page);
            }
            store.readTypes(subs, index, delivered, out, types,
                    typeIndex + 1, collected, since, now);
        }
    }
}
