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
package com.codename1.impl.android;

import com.codename1.health.AggregateMetric;
import com.codename1.health.AggregateQuery;
import com.codename1.health.AggregateResult;
import com.codename1.health.HealthAccess;
import com.codename1.health.HealthAuthorizationStatus;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthDeleteRequest;
import com.codename1.health.HealthError;
import com.codename1.health.HealthException;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthStore;
import com.codename1.health.HealthWriteResult;
import com.codename1.health.SampleQuery;
import com.codename1.health.SamplePage;
import com.codename1.impl.health.HealthWire;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// The Health Connect-backed store, driving the injected
/// [HealthConnectDelegate].
///
/// Everything crossing the bridge is a String or a primitive, so no
/// AndroidX or Kotlin type reaches the port -- see
/// [HealthConnectDelegate] for why. Results come back on a coroutine
/// dispatcher and are marshalled onto the EDT here, so the shared
/// [HealthStore] contract holds on Android as everywhere else.
class AndroidHealthStore extends HealthStore {

    /// Refreshed after every authorization flow; empty until then.
    private final Set<String> granted = new HashSet<String>();

    private HealthConnectDelegate delegate() {
        return AndroidHealthSupport.getDelegate();
    }

    public boolean isSupported() {
        HealthConnectDelegate d = delegate();
        return d != null
                && d.sdkStatus() == HealthConnectDelegate.SDK_AVAILABLE;
    }

    public boolean isTypeSupported(HealthDataType type) {
        return type != null && isSupported()
                && HealthWire.isAndroidSupported(type);
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

    /// Health Connect computes these natively; anything else is derived
    /// from raw samples by the shared base class.
    public List<AggregateMetric> getSupportedMetrics(HealthDataType type) {
        List<AggregateMetric> out = new ArrayList<AggregateMetric>();
        if (!isTypeSupported(type)) {
            return out;
        }
        out.add(AggregateMetric.COUNT);
        out.add(AggregateMetric.DURATION);
        switch (type.getAggregationStyle()) {
            case CUMULATIVE:
                out.add(AggregateMetric.TOTAL);
                break;
            case DISCRETE:
                out.add(AggregateMetric.AVERAGE);
                out.add(AggregateMetric.MINIMUM);
                out.add(AggregateMetric.MAXIMUM);
                break;
            default:
                break;
        }
        return out;
    }

    /// Health Connect caps a single insert at 1000 records.
    public int getMaxWriteBatchSize() {
        return 1000;
    }

    /// Health Connect never wakes the app -- there is no push mechanism at
    /// all, so subscriptions are drained when the app runs.
    public boolean isPushDelivery() {
        return false;
    }

    public boolean isBackgroundDeliverySupported() {
        return isSupported();
    }

    /// Unlike iOS, Android can answer this honestly: read access is an
    /// ordinary runtime grant.
    public HealthAuthorizationStatus getReadAuthorizationStatus(
            HealthDataType type) {
        return authStatus(type, false);
    }

    public HealthAuthorizationStatus getWriteAuthorizationStatus(
            HealthDataType type) {
        return authStatus(type, true);
    }

    private HealthAuthorizationStatus authStatus(HealthDataType type,
            boolean write) {
        if (!isSupported() || type == null) {
            return HealthAuthorizationStatus.NOT_SUPPORTED;
        }
        synchronized (granted) {
            if (granted.isEmpty()) {
                return HealthAuthorizationStatus.NOT_DETERMINED;
            }
            return granted.contains((write ? "w:" : "r:") + type.getId())
                    ? HealthAuthorizationStatus.AUTHORIZED
                    : HealthAuthorizationStatus.DENIED;
        }
    }

    private void rememberGrants(String csv) {
        synchronized (granted) {
            granted.clear();
            if (csv == null) {
                return;
            }
            String[] parts = csv.split(",");
            for (int i = 0; i < parts.length; i++) {
                String t = parts[i].trim();
                if (t.length() > 0) {
                    granted.add(t);
                }
            }
        }
    }

    /// Maps a bridge error code onto the portable vocabulary.
    private static HealthException toException(int code, String message) {
        HealthError error;
        switch (code) {
            case HealthConnectDelegate.ERR_AUTH_DENIED:
                error = HealthError.UNAUTHORIZED;
                break;
            case HealthConnectDelegate.ERR_PROVIDER:
                error = HealthError.PROVIDER_UNAVAILABLE;
                break;
            case HealthConnectDelegate.ERR_INVALID_ARGUMENT:
                error = HealthError.INVALID_ARGUMENT;
                break;
            case HealthConnectDelegate.ERR_TOKEN_EXPIRED:
                error = HealthError.ANCHOR_EXPIRED;
                break;
            default:
                error = HealthError.UNKNOWN;
                break;
        }
        return new HealthException(error, message == null
                ? "Health Connect call failed" : message);
    }

    /// Bridges one asynchronous call, marshalling the outcome to the EDT.
    private abstract static class Bridged<T>
            implements HealthConnectDelegate.Callback {
        private final AsyncResource<T> out;

        Bridged(AsyncResource<T> out) {
            this.out = out;
        }

        abstract T convert(String payload) throws Exception;

        public void onSuccess(final String payload) {
            AndroidHealth.onEdt(new Runnable() {
                public void run() {
                    try {
                        out.complete(convert(payload));
                    } catch (Exception ex) {
                        out.error(new HealthException(
                                HealthError.INVALID_DATA,
                                "could not decode the Health Connect "
                                        + "response", ex));
                    }
                }
            });
        }

        public void onError(final int code, final String message) {
            AndroidHealth.onEdt(new Runnable() {
                public void run() {
                    out.error(toException(code, message));
                }
            });
        }
    }

    private boolean failIfNoBridge(AsyncResource out) {
        if (delegate() != null) {
            return false;
        }
        out.error(new HealthException(HealthError.NOT_SUPPORTED,
                "Health Connect is not available on this device"));
        return true;
    }

    // ------------------------------------------------------------------

    protected void doRequestAuthorization(final List<HealthAccess> access,
            final AsyncResource<Boolean> out) {
        if (failIfNoBridge(out)) {
            return;
        }
        final HealthConnectDelegate d = delegate();
        final StringBuilder csv = new StringBuilder();
        for (int i = 0; i < access.size(); i++) {
            HealthAccess a = access.get(i);
            if (csv.length() > 0) {
                csv.append(',');
            }
            csv.append(a.isWrite() ? "w:" : "r:").append(
                    a.getType().getId());
        }
        // Health permissions go through Health Connect's own UI, not
        // ActivityCompat.requestPermissions. startActivityForResult must be
        // called from the EDT.
        AndroidHealth.onEdt(new Runnable() {
            public void run() {
                try {
                    AndroidNativeUtil.startActivityForResult(
                            d.permissionIntent(csv.toString()),
                            new IntentResultListener() {
                        public void onActivityResult(int requestCode,
                                int resultCode, android.content.Intent data) {
                            String grants = d.parsePermissionResult(
                                    resultCode, data);
                            rememberGrants(grants);
                            // True because the flow completed, matching the
                            // documented contract -- not because everything
                            // was granted.
                            out.complete(Boolean.TRUE);
                        }
                    });
                } catch (Throwable t) {
                    out.error(new HealthException(HealthError.UNKNOWN,
                            "could not present the Health Connect "
                                    + "permission screen", t));
                }
            }
        });
    }

    protected void doReadSamples(SampleQuery query,
            AsyncResource<SamplePage> out) {
        if (failIfNoBridge(out)) {
            return;
        }
        delegate().readRecords(HealthWire.encodeSampleQuery(query),
                new Bridged<SamplePage>(out) {
                    SamplePage convert(String payload) throws Exception {
                        return HealthWire.decodeSamplePage(payload);
                    }
                });
    }

    protected void doAggregate(final AggregateQuery query,
            final long[] boundaries,
            AsyncResource<List<AggregateResult>> out) {
        if (failIfNoBridge(out)) {
            return;
        }
        delegate().aggregate(
                HealthWire.encodeAggregateQuery(query, boundaries),
                new Bridged<List<AggregateResult>>(out) {
                    List<AggregateResult> convert(String payload)
                            throws Exception {
                        return HealthWire.decodeAggregates(payload, query,
                                boundaries);
                    }
                });
    }

    protected void doWrite(List<HealthSample> samples,
            AsyncResource<HealthWriteResult> out) {
        if (failIfNoBridge(out)) {
            return;
        }
        delegate().insertRecords(HealthWire.encodeSamples(samples),
                new Bridged<HealthWriteResult>(out) {
                    HealthWriteResult convert(String payload) {
                        return HealthWire.decodeWriteResult(payload);
                    }
                });
    }

    protected void doDelete(HealthDeleteRequest request,
            AsyncResource<Integer> out) {
        if (failIfNoBridge(out)) {
            return;
        }
        delegate().deleteRecords(HealthWire.encodeDeleteRequest(request),
                new Bridged<Integer>(out) {
                    Integer convert(String payload) {
                        try {
                            return Integer.valueOf(payload.trim());
                        } catch (NumberFormatException ex) {
                            return Integer.valueOf(0);
                        }
                    }
                });
    }
}
