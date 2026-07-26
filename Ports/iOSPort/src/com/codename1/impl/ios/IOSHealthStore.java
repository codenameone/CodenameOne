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
import com.codename1.health.HealthException;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthStore;
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

    /// HealthKit wakes the app when new data arrives.
    public boolean isPushDelivery() {
        return true;
    }

    public boolean isBackgroundDeliverySupported() {
        return isSupported();
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
}
