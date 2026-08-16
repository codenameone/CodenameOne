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

/// A live watch on a set of traits, returned by
/// [SmartHome#subscribe(SubscriptionRequest, HomeChangeListener)].
///
/// **Hold on to it and [#stop()] it.** A subscription the caller has dropped
/// keeps its listener reachable and keeps the platform delivering, so a form
/// that subscribes on show and never unsubscribes leaks a listener per visit
/// and eventually delivers the same change a dozen times. Stopping on
/// `Form.removeNotify` or in the screen's teardown is the habit to build.
///
/// Unlike a health subscription, this does not survive the process: it lives
/// entirely in memory and is gone when the app is. There is no cursor to
/// persist because there is nothing to catch up on -- see [#isPushDelivery()].
public final class TraitSubscription {

    private final String id;
    private final SmartHome owner;
    private final boolean pushDelivery;
    private boolean active = true;

    /// Created by [SmartHome]; not part of the public surface.
    TraitSubscription(String id, SmartHome owner, boolean pushDelivery) {
        this.id = id;
        this.owner = owner;
        this.pushDelivery = pushDelivery;
    }

    /// The identifier this subscription is known by, matching
    /// [TraitChangeBatch#getSubscriptionId()].
    ///
    /// #### Returns
    ///
    /// the identifier, never `null`
    public String getId() {
        return id;
    }

    /// Whether this subscription is still delivering.
    ///
    /// #### Returns
    ///
    /// `true` until [#stop()] is called
    public boolean isActive() {
        return active;
    }

    /// Whether the platform pushes changes as they happen, or whether they
    /// only arrive when you ask.
    ///
    /// **`true` only on HomeKit, and only while your app is in the
    /// foreground.** Everywhere else -- Google Home, the local simulated home
    /// -- changes are gathered and handed over when you call
    /// [SmartHome#drainChanges()], and a subscription on its own will never
    /// fire.
    ///
    /// So an app that watches a sensor has to do one of two things: call
    /// `drainChanges()` when it comes to the foreground and on whatever
    /// cadence suits it, or check this and tell the user plainly that live
    /// updates are not available here. Assuming push and getting none is the
    /// mistake this method exists to prevent, and it looks exactly like a
    /// sensor that never triggers.
    ///
    /// #### Returns
    ///
    /// `true` when changes arrive without being asked for
    public boolean isPushDelivery() {
        return pushDelivery;
    }

    /// Stops delivering, detaches the listener and releases the platform
    /// registration.
    ///
    /// Idempotent; calling it on a stopped subscription does nothing.
    public void stop() {
        if (!active) {
            return;
        }
        active = false;
        owner.unsubscribeInternal(this);
    }

    @Override
    public String toString() {
        return "TraitSubscription[" + id + (active ? "" : " stopped")
                + (pushDelivery ? " push" : " drain") + "]";
    }
}
