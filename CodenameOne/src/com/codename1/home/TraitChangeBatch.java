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
package com.codename1.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A coalesced set of trait changes, delivered to a [HomeChangeListener] on
/// the EDT.
///
/// #### One entry per trait, not one per change
///
/// Within the subscription's window -- see
/// [SubscriptionRequest#setMinIntervalMillis(int)] -- changes are collapsed
/// per accessory, service and trait, keeping only the newest value. Dragging a
/// dimmer produces one reading showing where it ended up, not forty showing
/// the journey.
///
/// That means a batch is a **state update, not an event log**: you cannot
/// count changes from it and you cannot see intermediate values. If you need
/// every step, set the window to zero and accept the cost.
public final class TraitChangeBatch {

    private final String subscriptionId;
    private final List<TraitReading> readings;
    private final boolean initialDelivery;
    private final boolean resyncRequired;

    /// Creates a batch. Called by the ports and by the local home.
    ///
    /// #### Parameters
    ///
    /// - `subscriptionId`: which subscription produced this
    ///
    /// - `readings`: the coalesced changes; `null` becomes empty
    ///
    /// - `initialDelivery`: whether this is the up-front delivery of current
    ///   values requested by
    ///   [SubscriptionRequest#setDeliverInitialValues(boolean)]
    ///
    /// - `resyncRequired`: whether changes were missed
    public TraitChangeBatch(String subscriptionId, List<TraitReading> readings,
            boolean initialDelivery, boolean resyncRequired) {
        this.subscriptionId = subscriptionId;
        if (readings == null || readings.isEmpty()) {
            this.readings = Collections.<TraitReading>emptyList();
        } else {
            this.readings = Collections.unmodifiableList(
                    new ArrayList<TraitReading>(readings));
        }
        this.initialDelivery = initialDelivery;
        this.resyncRequired = resyncRequired;
    }

    /// Which subscription produced this batch.
    ///
    /// Matches [TraitSubscription#getId()]. Worth checking when one listener
    /// serves several subscriptions.
    ///
    /// #### Returns
    ///
    /// the subscription identifier, or `null`
    public String getSubscriptionId() {
        return subscriptionId;
    }

    /// The changed values, one per trait that moved.
    ///
    /// A reading here can have no value or carry an error, exactly as one from
    /// a read can -- an accessory going unreachable is a change worth
    /// delivering. See [TraitReading].
    ///
    /// #### Returns
    ///
    /// an immutable list, possibly empty
    public List<TraitReading> getReadings() {
        return readings;
    }

    /// Whether this is the up-front delivery of current values rather than a
    /// report of something that just changed.
    ///
    /// #### Returns
    ///
    /// `true` for the initial delivery
    public boolean isInitialDelivery() {
        return initialDelivery;
    }

    /// Whether changes were missed and the values you hold cannot be trusted.
    ///
    /// The platform dropped its notification stream -- the app was backgrounded
    /// long enough, the connection to a hub was rebuilt, the accessory
    /// rejoined. The readings in this batch are still good; everything else
    /// you were tracking through this subscription is stale.
    ///
    /// Re-read the traits you care about with
    /// [SmartHome#read(TraitReadRequest)]. Ignoring this leaves a UI showing
    /// values from before the gap, indefinitely, with nothing to indicate it.
    ///
    /// #### Returns
    ///
    /// `true` when a full re-read is needed
    public boolean isResyncRequired() {
        return resyncRequired;
    }

    /// Whether this batch carries no readings.
    ///
    /// An empty batch is delivered only when [#isResyncRequired()] is `true`
    /// -- a resync is worth telling you about even with nothing to show.
    ///
    /// #### Returns
    ///
    /// `true` when there are no readings
    public boolean isEmpty() {
        return readings.isEmpty();
    }

    @Override
    public String toString() {
        return "TraitChangeBatch[" + readings.size() + " readings"
                + (initialDelivery ? " initial" : "")
                + (resyncRequired ? " resync-required" : "") + "]";
    }
}
