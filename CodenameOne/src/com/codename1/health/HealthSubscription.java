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

/// A live handle on a registered subscription.
public final class HealthSubscription {

    private final String id;
    private final List<HealthDataType> types;
    private final HealthStore store;
    private final boolean pushDelivery;
    private final boolean deliverSamples;
    private final boolean includeDeletions;
    private final int maxSamplesPerBatch;
    /// Guards the three mutable fields below, which cross threads: the
    /// cursor is written on the EDT -- by a delivery, or by a port
    /// seeding a starting cursor -- and read by `drainChanges()` on
    /// whatever thread the app called it from.
    ///
    /// A stale read here is not a stale number, it is lost data: the
    /// Android drain reads this anchor and takes a fresh baseline token
    /// when it finds null, so a cursor that had been seeded but not yet
    /// published meant a second token, and every change between the two
    /// permanently unreported. The synchronization the store does around
    /// its registry does not help -- the anchor is read before any of it,
    /// and a later monitor cannot make an earlier read current.
    ///
    /// A lock rather than `volatile`, which this codebase does not use.
    private final Object lock = new Object();
    /// Guarded by [#lock].
    private HealthAnchor anchor;
    /// Guarded by [#lock].
    private long lastDeliveryMillis;
    /// Guarded by [#lock].
    private boolean active = true;

    /// Creates a handle. Called by [HealthStore].
    HealthSubscription(HealthStore store, String id,
            List<HealthDataType> types, boolean pushDelivery) {
        this(store, id, types, pushDelivery, true, true, 0);
    }

    /// Creates a handle carrying the request's delivery options.
    HealthSubscription(HealthStore store, String id,
            List<HealthDataType> types, boolean pushDelivery,
            boolean deliverSamples, boolean includeDeletions,
            int maxSamplesPerBatch) {
        this.deliverSamples = deliverSamples;
        this.includeDeletions = includeDeletions;
        this.maxSamplesPerBatch = maxSamplesPerBatch;
        this.store = store;
        this.id = id;
        this.pushDelivery = pushDelivery;
        List<HealthDataType> copy = new ArrayList<HealthDataType>();
        if (types != null) {
            copy.addAll(types);
        }
        this.types = copy;
    }

    /// Seeds the cursor a previous launch persisted.
    ///
    /// Restoration without this leaves the handle with a null anchor, and
    /// both drain implementations read the handle rather than the store's
    /// preference, so Android would discard the persisted change token and
    /// start a fresh baseline -- losing everything that accumulated while
    /// the process was dead -- and iOS would resume with no window at all.
    void seedAnchor(HealthAnchor restored) {
        synchronized (lock) {
            this.anchor = restored;
        }
    }

    /// Whether batches carry sample payloads, from the request.
    boolean isDeliverSamples() {
        return deliverSamples;
    }

    /// Whether batches carry deleted ids, from the request.
    boolean isIncludeDeletions() {
        return includeDeletions;
    }

    /// The request's per-batch sample cap, or 0 for no cap.
    int getMaxSamplesPerBatch() {
        return maxSamplesPerBatch;
    }

    /// The identifier this subscription was registered under.
    public String getId() {
        return id;
    }

    /// The types being watched.
    public List<HealthDataType> getTypes() {
        return Collections.unmodifiableList(types);
    }

    /// `true` until [#stop()] is called.
    public boolean isActive() {
        synchronized (lock) {
            return active;
        }
    }

    /// Whether the operating system wakes the app when new data arrives.
    ///
    /// **`false` on every platform in this release.** Health Connect has
    /// no push mechanism at all -- Google's own guidance is to poll -- and
    /// while HealthKit does offer `HKObserverQuery`, this release registers
    /// none. Nothing here hooks the application lifecycle either, so
    /// changes arrive exactly when you call [HealthStore#drainChanges()]
    /// and at no other time: call it when you come to the foreground and
    /// from your background-fetch handler.
    ///
    /// This is exposed as a queryable fact rather than hidden, because an
    /// app that assumes push will silently miss data for days and its
    /// authors will never know why -- no changes and no new data look
    /// identical from the outside.
    public boolean isPushDelivery() {
        return pushDelivery;
    }

    /// The cursor reached by the most recent delivery, or null before the
    /// first one.
    public HealthAnchor getAnchor() {
        synchronized (lock) {
            return anchor;
        }
    }

    /// When the last batch was delivered, epoch millis, or 0 if never.
    public long getLastDeliveryMillis() {
        synchronized (lock) {
            return lastDeliveryMillis;
        }
    }

    /// Cancels the subscription and discards its persisted cursor.
    /// Idempotent.
    ///
    /// Restarting later under the same id resynchronizes from scratch,
    /// because the cursor is gone. To pause without losing your place,
    /// simply stop calling [HealthStore#drainChanges()].
    public void stop() {
        boolean wasActive;
        synchronized (lock) {
            wasActive = active;
            active = false;
        }
        if (wasActive) {
            // Outside the lock. unsubscribe() takes the store's registry
            // monitor and then reaches back here through markInactive();
            // holding this one across the call would take the two in the
            // opposite order from every other path, which is a deadlock.
            // This handle, not merely this id: stopped after the id had
            // been re-registered, cancelling by id alone removed and tore
            // down the replacement instead of the handle being stopped.
            store.unsubscribe(id, this);
        }
    }

    /// Records progress. Called by [HealthStore] after a delivery.
    void noteDelivery(HealthAnchor anchor, long whenMillis) {
        synchronized (lock) {
            this.anchor = anchor;
            this.lastDeliveryMillis = whenMillis;
        }
    }

    /// Marks this handle inactive without re-entering
    /// [HealthStore#unsubscribe(String)]. Called by the store when it
    /// tears the subscription down itself.
    void markInactive() {
        synchronized (lock) {
            active = false;
        }
    }

    @Override
    public String toString() {
        return "HealthSubscription[" + id + " " + types.size() + " types"
                + (pushDelivery ? ", push" : ", poll")
                + (isActive() ? "" : ", stopped") + "]";
    }
}
