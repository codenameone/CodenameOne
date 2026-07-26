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

/// A set of changes since the last time a subscription was drained.
/// Delivered to [HealthChangeListener] in the foreground and to
/// [HealthBackgroundListener] after a background relaunch.
public final class HealthChangeBatch {

    private final String subscriptionId;
    private final List<HealthDataType> types;
    private final List<HealthSample> added;
    private final List<String> deletedSampleIds;
    private final boolean resyncRequired;
    private final HealthAnchor anchor;
    private final long deadlineMillis;
    private final boolean more;

    /// Creates a batch. Called by [HealthStore]; the lists are copied
    /// defensively.
    public HealthChangeBatch(String subscriptionId,
            List<HealthDataType> types, List<HealthSample> added,
            List<String> deletedSampleIds, boolean resyncRequired,
            HealthAnchor anchor, long deadlineMillis, boolean more) {
        this.subscriptionId = subscriptionId;
        this.types = copyTypes(types);
        this.added = copySamples(added);
        this.deletedSampleIds = copyIds(deletedSampleIds);
        this.resyncRequired = resyncRequired;
        this.anchor = anchor;
        this.deadlineMillis = deadlineMillis;
        this.more = more;
    }

    private static List<HealthDataType> copyTypes(List<HealthDataType> in) {
        List<HealthDataType> out = new ArrayList<HealthDataType>();
        if (in != null) {
            out.addAll(in);
        }
        return out;
    }

    private static List<HealthSample> copySamples(List<HealthSample> in) {
        List<HealthSample> out = new ArrayList<HealthSample>();
        if (in != null) {
            out.addAll(in);
        }
        return out;
    }

    private static List<String> copyIds(List<String> in) {
        List<String> out = new ArrayList<String>();
        if (in != null) {
            out.addAll(in);
        }
        return out;
    }

    /// The subscription this batch belongs to.
    public String getSubscriptionId() {
        return subscriptionId;
    }

    /// The types covered by this batch.
    public List<HealthDataType> getTypes() {
        return Collections.unmodifiableList(types);
    }

    /// Samples added or updated since the last drain. Empty when the
    /// subscription set [SubscriptionRequest#setDeliverSamples(boolean)]
    /// to false, and empty when [#isResyncRequired()].
    public List<HealthSample> getAdded() {
        return Collections.unmodifiableList(added);
    }

    /// Identifiers of samples removed since the last drain.
    public List<String> getDeletedSampleIds() {
        return Collections.unmodifiableList(deletedSampleIds);
    }

    /// `true` when the platform cursor was lost and incremental delivery
    /// cannot continue -- a Health Connect change token older than 30
    /// days, or an iOS anchor rejected after a restore from backup.
    ///
    /// [#getAdded()] is empty in that case. The app must do a full
    /// time-range read to catch up; nothing else will deliver the missed
    /// data.
    public boolean isResyncRequired() {
        return resyncRequired;
    }

    /// The cursor after this batch. Persisted by [HealthStore]
    /// automatically; exposed for apps that checkpoint against their own
    /// server -- see [HealthAnchor].
    public HealthAnchor getAnchor() {
        return anchor;
    }

    /// Roughly how much wall-clock time the listener has before the OS may
    /// suspend the process, in milliseconds. About 5000 on an iOS
    /// background relaunch; `Long.MAX_VALUE` in the foreground.
    ///
    /// Do cheap bookkeeping inside a background delivery -- accumulate a
    /// counter, stash an identifier -- and leave network calls for the
    /// next foreground.
    public long getDeadlineMillis() {
        return deadlineMillis;
    }

    /// `true` when more changes were available than
    /// [SubscriptionRequest#getMaxSamplesPerBatch()] allowed. The
    /// remainder arrives in the next delivery, or immediately on
    /// [HealthStore#drainChanges()].
    public boolean hasMore() {
        return more;
    }

    /// `true` when nothing changed and no resync is needed.
    public boolean isEmpty() {
        return !resyncRequired && added.isEmpty()
                && deletedSampleIds.isEmpty();
    }

    @Override
    public String toString() {
        return "HealthChangeBatch[" + subscriptionId + " +" + added.size()
                + " -" + deletedSampleIds.size()
                + (resyncRequired ? " RESYNC" : "") + "]";
    }
}
