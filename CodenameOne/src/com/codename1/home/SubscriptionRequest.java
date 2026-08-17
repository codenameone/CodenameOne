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

/// Which traits to watch, and how often you are willing to hear about them.
///
/// ```java
/// SubscriptionRequest req = new SubscriptionRequest()
///         .add(lamp, lamp.getPrimaryService(), Trait.ON_OFF)
///         .add(lamp, lamp.getPrimaryService(), Trait.BRIGHTNESS)
///         .setDeliverInitialValues(true);
/// TraitSubscription sub = SmartHome.getInstance().subscribe(req, listener);
/// ```
public final class SubscriptionRequest {

    /// The default coalescing window, in milliseconds.
    ///
    /// A fifth of a second: fast enough that a light responding to a switch
    /// looks instant, slow enough that dragging a dimmer does not put a
    /// hundred deliveries through the EDT.
    public static final int DEFAULT_MIN_INTERVAL_MILLIS = 200;

    private final List<String> accessoryIds = new ArrayList<String>();
    private final List<String> serviceIds = new ArrayList<String>();
    private final List<Trait> traits = new ArrayList<Trait>();
    private int minIntervalMillis = DEFAULT_MIN_INTERVAL_MILLIS;
    private boolean deliverInitialValues;

    /// Watches one trait on one service.
    ///
    /// #### Parameters
    ///
    /// - `accessory`: the accessory to watch
    ///
    /// - `service`: the service on it
    ///
    /// - `trait`: the trait to watch
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when any argument is `null`
    public SubscriptionRequest add(Accessory accessory,
            AccessoryService service, Trait trait) {
        if (accessory == null) {
            throw new IllegalArgumentException("accessory is required");
        }
        if (service == null) {
            throw new IllegalArgumentException("service is required");
        }
        return add(accessory.getId(), service.getId(), trait);
    }

    /// Watches one trait on one service, by identifier.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the accessory to watch
    ///
    /// - `serviceId`: the service on it
    ///
    /// - `trait`: the trait to watch
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when any argument is `null` or empty
    public SubscriptionRequest add(String accessoryId, String serviceId,
            Trait trait) {
        if (accessoryId == null || accessoryId.length() == 0) {
            throw new IllegalArgumentException("accessory id is required");
        }
        if (serviceId == null || serviceId.length() == 0) {
            throw new IllegalArgumentException("service id is required");
        }
        if (trait == null) {
            throw new IllegalArgumentException("trait is required");
        }
        accessoryIds.add(accessoryId);
        serviceIds.add(serviceId);
        traits.add(trait);
        return this;
    }

    /// How long to gather changes before delivering them, in milliseconds.
    ///
    /// Within the window, changes are coalesced per accessory, service and
    /// trait, keeping only the newest value; the batch then crosses to the
    /// EDT once. Dragging a dimmer emits a notification per step, and a home
    /// with a hundred watched accessories can produce a steady stream, so the
    /// window is what keeps the event loop usable.
    ///
    /// Zero delivers every change as it arrives. That is occasionally right --
    /// a diagnostic view, a trace -- and is a decision to make deliberately
    /// rather than a default to inherit.
    ///
    /// #### Parameters
    ///
    /// - `minIntervalMillis`: the window; zero to deliver everything
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the value is negative
    public SubscriptionRequest setMinIntervalMillis(int minIntervalMillis) {
        if (minIntervalMillis < 0) {
            throw new IllegalArgumentException(
                    "the coalescing window cannot be negative, got "
                            + minIntervalMillis);
        }
        this.minIntervalMillis = minIntervalMillis;
        return this;
    }

    /// The coalescing window in milliseconds.
    ///
    /// #### Returns
    ///
    /// the window, [#DEFAULT_MIN_INTERVAL_MILLIS] unless set
    public int getMinIntervalMillis() {
        return minIntervalMillis;
    }

    /// Whether to deliver each watched trait's current value once, up front,
    /// as though it had just changed.
    ///
    /// `false` by default. Turn it on when the subscription is what populates
    /// a screen, so the same listener that keeps the screen live also fills
    /// it, and there is no separate read whose result can race the first
    /// change. The batch is marked with
    /// [TraitChangeBatch#isInitialDelivery()].
    ///
    /// #### Parameters
    ///
    /// - `deliverInitialValues`: whether to send current values first
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    public SubscriptionRequest setDeliverInitialValues(
            boolean deliverInitialValues) {
        this.deliverInitialValues = deliverInitialValues;
        return this;
    }

    /// Whether current values are delivered up front.
    ///
    /// #### Returns
    ///
    /// `true` when they are
    public boolean isDeliverInitialValues() {
        return deliverInitialValues;
    }

    /// How many traits this subscription watches.
    ///
    /// #### Returns
    ///
    /// the count, zero for an empty request
    public int size() {
        return traits.size();
    }

    /// Whether this request watches nothing.
    ///
    /// #### Returns
    ///
    /// `true` when nothing has been added
    public boolean isEmpty() {
        return traits.isEmpty();
    }

    /// The accessory ids, positionally aligned with [#getServiceIds()] and
    /// [#getTraits()].
    ///
    /// #### Returns
    ///
    /// an immutable list
    public List<String> getAccessoryIds() {
        return Collections.unmodifiableList(accessoryIds);
    }

    /// The service ids, positionally aligned with [#getAccessoryIds()] and
    /// [#getTraits()].
    ///
    /// #### Returns
    ///
    /// an immutable list
    public List<String> getServiceIds() {
        return Collections.unmodifiableList(serviceIds);
    }

    /// The traits, positionally aligned with [#getAccessoryIds()] and
    /// [#getServiceIds()].
    ///
    /// #### Returns
    ///
    /// an immutable list
    public List<Trait> getTraits() {
        return Collections.unmodifiableList(traits);
    }
}
