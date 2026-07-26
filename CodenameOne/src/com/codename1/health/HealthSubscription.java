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
    private HealthAnchor anchor;
    private long lastDeliveryMillis;
    private boolean active = true;

    /// Creates a handle. Called by [HealthStore].
    HealthSubscription(HealthStore store, String id,
            List<HealthDataType> types, boolean pushDelivery) {
        this.store = store;
        this.id = id;
        this.pushDelivery = pushDelivery;
        List<HealthDataType> copy = new ArrayList<HealthDataType>();
        if (types != null) {
            copy.addAll(types);
        }
        this.types = copy;
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
        return active;
    }

    /// Whether the operating system wakes the app when new data arrives.
    ///
    /// **`true` on iOS, `false` on Android.** Health Connect has no push
    /// mechanism at all -- Google's own guidance is to poll -- so on
    /// Android the framework drains changes at app start, on foreground,
    /// and whenever you call [HealthStore#drainChanges()]. Wire that last
    /// one into your background-fetch handler if you need anything better
    /// than foreground-only.
    ///
    /// This is exposed as a queryable fact rather than hidden, because an
    /// app that assumes push on Android will silently miss data for days
    /// and its authors will never know why.
    public boolean isPushDelivery() {
        return pushDelivery;
    }

    /// The cursor reached by the most recent delivery, or null before the
    /// first one.
    public HealthAnchor getAnchor() {
        return anchor;
    }

    /// When the last batch was delivered, epoch millis, or 0 if never.
    public long getLastDeliveryMillis() {
        return lastDeliveryMillis;
    }

    /// Cancels the subscription and discards its persisted cursor.
    /// Idempotent.
    ///
    /// Restarting later under the same id resynchronizes from scratch,
    /// because the cursor is gone. To pause without losing your place,
    /// simply stop calling [HealthStore#drainChanges()].
    public void stop() {
        if (active) {
            active = false;
            store.unsubscribe(id);
        }
    }

    /// Records progress. Called by [HealthStore] after a delivery.
    void noteDelivery(HealthAnchor anchor, long whenMillis) {
        this.anchor = anchor;
        this.lastDeliveryMillis = whenMillis;
    }

    /// Marks this handle inactive without re-entering
    /// [HealthStore#unsubscribe(String)]. Called by the store when it
    /// tears the subscription down itself.
    void markInactive() {
        active = false;
    }

    @Override
    public String toString() {
        return "HealthSubscription[" + id + " " + types.size() + " types"
                + (pushDelivery ? ", push" : ", poll")
                + (active ? "" : ", stopped") + "]";
    }
}
