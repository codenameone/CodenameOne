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
import com.codename1.health.HealthAnchor;
import com.codename1.health.HealthChangeBatch;
import com.codename1.health.HealthAuthorizationStatus;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthDeleteRequest;
import com.codename1.health.HealthError;
import com.codename1.health.HealthException;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthStore;
import com.codename1.health.HealthSubscription;
import com.codename1.health.HealthWriteResult;
import com.codename1.health.SampleQuery;
import com.codename1.health.SamplePage;
import com.codename1.impl.health.HealthChangePage;
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

    /// Refreshed after every authorization flow, and once at startup
    /// from Health Connect itself.
    private final Set<String> granted = new HashSet<String>();

    /// True once the delegate has answered, so an empty set can be told
    /// apart from "nothing granted".
    private boolean grantsLoaded;
    private boolean grantsRequested;

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

    /// Narrower than readable: the series-shaped types have no
    /// single-value write form in the bridge.
    public boolean isWritable(HealthDataType type) {
        return isTypeSupported(type) && HealthWire.isAndroidWritable(type);
    }

    /// Deletion is not writing.
    ///
    /// `deleteRecords` removes by record class and identifier or range,
    /// and the bridge maps a record class for every readable type -- so
    /// the four series-shaped types have no single-value write form yet
    /// are perfectly deletable. Answering from the write table made
    /// capability-gated code refuse to delete exactly the record
    /// identities `setFlattenSeries(false)` exists to hand out.
    public boolean isDeletable(HealthDataType type) {
        return isTypeSupported(type) && HealthWire.isAndroidDeletable(type);
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

    /// False, like [#isPushDelivery()].
    ///
    /// The base method's contract is that changes arrive without the app
    /// asking. Nothing here does that: Health Connect has no push at all,
    /// and no lifecycle hook drains on the app's behalf, so a subscription
    /// delivers exactly when `drainChanges()` is called and never
    /// otherwise. Answering true let an app branch away from scheduling
    /// its own drain and then receive nothing -- the same silence as
    /// having no data, which is the confusion this API exists to avoid.
    public boolean isBackgroundDeliverySupported() {
        return false;
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
        // Capability first. One Health Connect permission maps back to
        // several portable tokens, so a granted READ_DISTANCE made
        // DISTANCE_CYCLING look AUTHORIZED even though the bridge has no
        // record class for it and every read of it is refused -- and the
        // readable-but-unwritable series types answered about a write they
        // could never perform.
        if (write ? !isWritable(type) : !isTypeSupported(type)) {
            return HealthAuthorizationStatus.NOT_SUPPORTED;
        }
        refreshGrants();
        synchronized (granted) {
            if (!grantsLoaded) {
                // The delegate has not answered yet. Saying DENIED here
                // would be a guess; NOT_DETERMINED is the honest state.
                return HealthAuthorizationStatus.NOT_DETERMINED;
            }
            return granted.contains((write ? "w:" : "r:") + type.getId())
                    ? HealthAuthorizationStatus.AUTHORIZED
                    : HealthAuthorizationStatus.DENIED;
        }
    }

    /// Asks Health Connect what is already granted, once per process.
    ///
    /// Without this the set starts empty after every restart and the
    /// synchronous status API reports NOT_DETERMINED for permissions the
    /// user granted long ago -- becoming accurate only after the app
    /// presents the authorization flow again.
    private void refreshGrants() {
        HealthConnectDelegate d = delegate();
        synchronized (granted) {
            if (grantsRequested || d == null) {
                return;
            }
            grantsRequested = true;
        }
        d.grantedPermissions(new GrantsCallback(this));
    }

    /// Refreshes the grant snapshot and resolves `out` once it has landed.
    ///
    /// Falls back to resolving immediately when there is no delegate to
    /// ask: the flow did complete, and blocking on a refresh that can
    /// never arrive would be worse than an unrefreshed cache.
    private void refreshGrantsThen(final AsyncResource<Boolean> out) {
        HealthConnectDelegate d = delegate();
        if (d == null) {
            out.complete(Boolean.TRUE);
            return;
        }
        synchronized (granted) {
            grantsRequested = true;
        }
        d.grantedPermissions(new GrantsThenComplete(this, out));
    }

    /// Records the refreshed grants, then resolves the authorization.
    ///
    /// Named rather than anonymous so it carries no synthetic reference
    /// (SpotBugs `SIC_INNER_SHOULD_BE_STATIC_ANON`).
    private static final class GrantsThenComplete
            implements HealthConnectDelegate.Callback {
        private final AndroidHealthStore store;
        private final AsyncResource<Boolean> out;

        GrantsThenComplete(AndroidHealthStore store,
                AsyncResource<Boolean> out) {
            this.store = store;
            this.out = out;
        }

        public void onSuccess(final String payload) {
            AndroidHealth.onEdt(new Runnable() {
                public void run() {
                    store.rememberGrants(payload);
                    store.clearGrantsRequest();
                    out.complete(Boolean.TRUE);
                }
            });
        }

        public void onError(final int code, final String message) {
            AndroidHealth.onEdt(new Runnable() {
                public void run() {
                    // The sheet still completed. An unreadable grant list
                    // leaves the cache as it was rather than failing an
                    // authorization the user did go through.
                    store.clearGrantsRequest();
                    out.complete(Boolean.TRUE);
                }
            });
        }
    }

    private void rememberGrants(String csv) {
        synchronized (granted) {
            granted.clear();
            grantsLoaded = true;
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

    /// Receives the startup grant snapshot.
    ///
    /// Named rather than anonymous so SpotBugs does not flag an inner
    /// class holding the enclosing reference.
    private static final class GrantsCallback
            implements HealthConnectDelegate.Callback {
        private final AndroidHealthStore store;

        GrantsCallback(AndroidHealthStore store) {
            this.store = store;
        }

        public void onSuccess(String payload) {
            store.rememberGrants(payload);
        }

        public void onError(int code, String message) {
            // Leave the set unloaded so a later call retries rather than
            // reporting DENIED for a grant we simply failed to read.
            store.clearGrantsRequest();
        }
    }

    void clearGrantsRequest() {
        synchronized (granted) {
            grantsRequested = false;
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
                            if (resultCode == android.app.Activity
                                    .RESULT_CANCELED) {
                                // Dismissal is not completion. Reporting
                                // success here left callers unable to tell
                                // a backed-out sheet from a finished flow,
                                // which is the one distinction this API
                                // promises to make on Android.
                                out.error(new HealthException(
                                        HealthError.USER_CANCELED,
                                        "the Health Connect permission "
                                                + "screen was dismissed"));
                                return;
                            }
                            // The result only lists what this intent
                            // asked for. Replacing the cached snapshot
                            // with it made a previously granted type read
                            // as DENIED after a second, unrelated
                            // authorization -- so re-read the full set
                            // from Health Connect instead.
                            d.parsePermissionResult(resultCode, data);
                            clearGrantsRequest();
                            synchronized (granted) {
                                grantsLoaded = false;
                            }
                            // Resolved only once the refreshed snapshot has
                            // landed. Completing first meant an app that
                            // checked getReadAuthorizationStatus from this
                            // very callback saw NOT_DETERMINED -- the cache
                            // having just been cleared -- with nothing to
                            // tell it when to look again. True because the
                            // flow completed, matching the documented
                            // contract, not because anything was granted.
                            refreshGrantsThen(out);
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

    // doAggregate is deliberately not overridden. The bridge returned an
    // empty payload here and relied on the base class to aggregate the raw
    // samples instead -- but an empty payload decodes to buckets with no
    // values, so the fallback never ran and every aggregate came back
    // empty. Inheriting the base implementation is what actually gets the
    // shared arithmetic, and it keeps one implementation of it rather than
    // two that can disagree.

    protected void doWrite(List<HealthSample> samples,
            AsyncResource<HealthWriteResult> out) {
        if (failIfNoBridge(out)) {
            return;
        }
        HealthSample rejected = HealthWire.unsupportedForWrite(samples);
        if (rejected != null) {
            // The payload cannot carry this shape, and the bridge reports
            // an empty batch as a successful insert of nothing.
            out.error(new HealthException(HealthError.TYPE_NOT_SUPPORTED,
                    rejected.getType().getId() + " cannot be written to"
                            + " Health Connect through this API"));
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

    // ==================================================================
    // change draining
    // ==================================================================

    /// Health Connect never wakes the app, so a subscription is only ever
    /// as current as the last drain. Subscriptions are drained one at a
    /// time rather than concurrently: each owns a change token that the
    /// base class persists once its batch has been handled, and running
    /// them in parallel would interleave those writes.
    protected void doDrainChanges(List<HealthSubscription> subscriptions,
            AsyncResource<Integer> out) {
        if (failIfNoBridge(out)) {
            return;
        }
        drainFrom(new ArrayList<HealthSubscription>(subscriptions), 0, 0,
                out);
    }

    private void drainFrom(List<HealthSubscription> subs, int index,
            int delivered, AsyncResource<Integer> out) {
        if (index >= subs.size()) {
            out.complete(Integer.valueOf(delivered));
            return;
        }
        HealthSubscription sub = subs.get(index);
        HealthAnchor anchor = sub.getAnchor();
        String token = anchor == null ? null : anchor.toStorableString();
        if (token == null || token.length() == 0) {
            // The first drain only establishes a baseline. A Health
            // Connect token describes changes from the moment it is
            // issued, so there is nothing yet to report; firing the empty
            // batch is what gets the token persisted for the next poll.
            delegate().getChangesToken(typesCsv(sub),
                    new DrainStep(subs, index, delivered, out, true));
            return;
        }
        delegate().getChanges(token,
                new DrainStep(subs, index, delivered, out, false));
    }

    private static String typesCsv(HealthSubscription sub) {
        StringBuilder sb = new StringBuilder();
        List<HealthDataType> types = sub.getTypes();
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(types.get(i).getId());
        }
        return sb.toString();
    }

    /// One step of the sequential drain: handles a single subscription's
    /// bridge reply and then moves on to the next one.
    private final class DrainStep
            implements HealthConnectDelegate.Callback {

        private final List<HealthSubscription> subs;
        private final int index;
        private final int delivered;
        private final AsyncResource<Integer> out;
        private final boolean baseline;

        DrainStep(List<HealthSubscription> subs, int index, int delivered,
                AsyncResource<Integer> out, boolean baseline) {
            this.subs = subs;
            this.index = index;
            this.delivered = delivered;
            this.out = out;
            this.baseline = baseline;
        }

        public void onSuccess(final String payload) {
            AndroidHealth.onEdt(new DeliverBatch(this, payload));
        }

        public void onError(final int code, final String message) {
            if (code == HealthConnectDelegate.ERR_TOKEN_EXPIRED) {
                // Not transient. A Health Connect token ages out after
                // about 30 days, and retrying it fails identically
                // forever -- so treating it like any other error left the
                // subscription retrying an unusable token and silently
                // never advancing again. Tell the app to resynchronise and
                // start a fresh baseline, which is what
                // isResyncRequired() exists to say.
                AndroidHealth.onEdt(new ResyncOne(this));
                return;
            }
            // One failing subscription must not strand the others, and it
            // must not advance its own cursor. Skipping it leaves the
            // token where it was so the next drain retries the same range.
            AndroidHealth.onEdt(new SkipOne(this));
        }

        void resync() {
            HealthSubscription sub = subs.get(index);
            // No anchor: the batch carries the resync flag and nothing
            // else, and the cursor is dropped so the next drain asks for a
            // fresh token rather than resending the expired one.
            // The cursor is dropped by the shared delivery path once the
            // listener has actually been told to resynchronise; doing it
            // here would lose the expired token to a listener that threw.
            fireChanges(new HealthChangeBatch(sub.getId(), sub.getTypes(),
                    null, null, true, null, Long.MAX_VALUE, false));
            drainFrom(subs, index + 1, delivered + 1, out);
        }

        void deliver(String payload) {
            int count = delivered;
            HealthSubscription sub = subs.get(index);
            if (baseline) {
                fireChanges(new HealthChangeBatch(sub.getId(),
                        sub.getTypes(), null, null, false,
                        // Foreground: no OS deadline. Reporting 0 made a listener
                        // that budgets against getDeadlineMillis() treat every
                        // ordinary drain as already out of time.
                        HealthAnchor.of(payload.trim()), Long.MAX_VALUE, false));
                // Counted, like every other delivery. drainChanges()
                // documents its result as the number of batches delivered,
                // and the listener did receive this one -- the iOS
                // baseline path has always counted it.
                count++;
            } else {
                HealthChangePage page =
                        HealthWire.decodeChangePage(payload);
                if (page == null) {
                    // An unreadable reply leaves the cursor alone rather
                    // than advancing past changes that were never seen.
                    drainFrom(subs, index + 1, count, out);
                    return;
                }
                fireChanges(new HealthChangeBatch(sub.getId(),
                        sub.getTypes(), page.getAdded(),
                        page.getDeletedIds(), page.isExpired(),
                        // Foreground: no OS deadline. Reporting 0 made a listener
                        // that budgets against getDeadlineMillis() treat every
                        // ordinary drain as already out of time.
                        HealthAnchor.of(page.getNextToken()), Long.MAX_VALUE,
                        page.hasMore()));
                count++;
            }
            drainFrom(subs, index + 1, count, out);
        }

        void skip() {
            drainFrom(subs, index + 1, delivered, out);
        }
    }

    /// Named rather than anonymous so the EDT hop carries no implicit
    /// reference to the enclosing store.
    private static final class DeliverBatch implements Runnable {
        private final DrainStep step;
        private final String payload;

        DeliverBatch(DrainStep step, String payload) {
            this.step = step;
            this.payload = payload;
        }

        public void run() {
            step.deliver(payload);
        }
    }

    /// Reports an expired token and clears the cursor.
    ///
    /// Named rather than anonymous so it carries no synthetic reference
    /// (SpotBugs `SIC_INNER_SHOULD_BE_STATIC_ANON`).
    private static final class ResyncOne implements Runnable {
        private final ChangeRead read;

        ResyncOne(ChangeRead read) {
            this.read = read;
        }

        @Override
        public void run() {
            read.resync();
        }
    }

    private static final class SkipOne implements Runnable {
        private final DrainStep step;

        SkipOne(DrainStep step) {
            this.step = step;
        }

        public void run() {
            step.skip();
        }
    }
}
