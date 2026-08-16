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

/// Told when a watched [Trait] changes value.
///
/// Attached by [SmartHome#subscribe(SubscriptionRequest, HomeChangeListener)].
/// Deliveries arrive on the EDT, so a listener may touch components directly.
///
/// #### Nothing wakes your app for these
///
/// On every backend, changes arrive only while your app is running. HomeKit
/// delivers `HMAccessoryDelegate` callbacks to a foreground app, and the
/// Google Home APIs need a live signed-in client -- it is the home hub, not
/// your app, that reacts to a sensor while the phone is asleep. Where
/// [TraitSubscription#isPushDelivery()] answers `false`, changes arrive only
/// when you call [SmartHome#drainChanges()].
public interface HomeChangeListener {

    /// One or more watched traits changed.
    ///
    /// Check [TraitChangeBatch#isResyncRequired()] before trusting values
    /// this batch does not mention.
    ///
    /// #### Parameters
    ///
    /// - `batch`: the coalesced changes, never `null`
    void traitsChanged(TraitChangeBatch batch);
}
