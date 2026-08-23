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
package com.codename1.impl.nearby;

import com.codename1.impl.async.EdtResult;
import com.codename1.impl.async.PendingMap;
import com.codename1.nearby.spi.NearbyBridge;
import com.codename1.ui.Display;

import java.util.concurrent.atomic.AtomicInteger;

/// The bits every `com.codename1.nearby` facade needs and none of them owns:
/// the bridge lookup, one request-id counter for the whole family, and the
/// EDT hop that unsolicited native events take.
///
/// @hidden not part of the public API.
public final class NearbyRequests {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private static NearbyBridge testBridge;

    /// Permission requests in flight, from EVERY facade.
    ///
    /// One map rather than one per facade, because there is one answer path:
    /// a port reports the outcome through
    /// `com.codename1.nearby.ranging.Ranging#deliverPermissionResult`
    /// whichever entry point asked. A per-facade map meant a request opened by
    /// `NearbyTransport.requestPermissions` was looked for in the ranging map,
    /// not found, and dropped -- leaving the caller holding a resource that
    /// never settled, which is precisely the failure the SPI documentation
    /// calls worse than an outright error.
    ///
    /// Safe to share because request ids come from one counter, so an id is in
    /// at most one map and an answer cannot be matched to the wrong operation.
    private static final PendingMap<Boolean> PERMISSIONS =
            new PendingMap<Boolean>();

    private NearbyRequests() {
    }

    /// The active port's bridge, or `null` where no port implements one.
    ///
    /// Guarded on `Display.isInitialized()` rather than on the instance being
    /// non-null: `Display.getInstance()` hands back its singleton long before
    /// `Display.init` has given it an implementation, and asking that for a
    /// bridge throws. A unit test and an app that touches a facade from a
    /// static initializer both reach this that way.
    ///
    /// #### Returns
    ///
    /// the bridge, or null
    public static synchronized NearbyBridge bridge() {
        if (testBridge != null) {
            return testBridge;
        }
        if (!Display.isInitialized()) {
            return null;
        }
        return Display.getInstance().getNearbyBridge();
    }

    /// Installs a bridge and clears every facade's static state, so one test
    /// cannot see the sessions, listeners or in-flight requests of the test
    /// that ran before it.
    ///
    /// The facades are static -- there is no instance for a test to throw
    /// away -- which makes shared state order-dependent: a listener a
    /// previous test forgot to remove fires during this one, and the failure
    /// looks like a bug in whichever test happened to run second. This is the
    /// same arrangement `com.codename1.home.SmartHome` uses, for the same
    /// reason.
    ///
    /// Passing `null` gives a bridgeless framework without waiting for
    /// `Display` to be absent, which is what the degradation tests need.
    ///
    /// @hidden not part of the public API; test-only.
    ///
    /// #### Parameters
    ///
    /// - `bridge`: the bridge to install, or null for none
    public static void resetForTest(NearbyBridge bridge) {
        synchronized (NearbyRequests.class) {
            testBridge = bridge;
        }
        com.codename1.nearby.ranging.Ranging.resetForTest();
        com.codename1.nearby.ranging.RangingSession.resetForTest();
        com.codename1.nearby.companion.CompanionDevices.resetForTest();
        com.codename1.nearby.transport.NearbyTransport.resetForTest();
    }

    /// The next request id.
    ///
    /// Ids come from one counter shared by ranging, companion and transport
    /// so that an id lives in exactly one `PendingMap` and an answer can
    /// never be matched against the wrong operation.
    ///
    /// #### Returns
    ///
    /// a request id no other in-flight operation is using
    public static int nextId() {
        return NEXT_ID.getAndIncrement();
    }

    /// Registers a permission request and returns the resource its answer
    /// will complete.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id the port will answer with
    ///
    /// #### Returns
    ///
    /// the resource to hand to the caller
    public static EdtResult<Boolean> openPermissionRequest(int requestId) {
        return PERMISSIONS.open(requestId);
    }

    /// Claims a permission request's resource, removing it.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id being answered
    ///
    /// #### Returns
    ///
    /// the resource, or null when nothing is waiting on that id
    public static EdtResult<Boolean> takePermissionRequest(int requestId) {
        return PERMISSIONS.take(requestId);
    }

    /// Fails every permission request in flight.
    ///
    /// #### Parameters
    ///
    /// - `failure`: what to fail them with
    public static void failPermissionRequests(Throwable failure) {
        PERMISSIONS.failAll(failure);
    }

    /// Runs something on the EDT, immediately when already there.
    ///
    /// Ports call the `deliver...` entry points from whatever thread the
    /// native callback arrived on, so this is what makes the public
    /// contract -- every callback on the EDT -- true.
    ///
    /// #### Parameters
    ///
    /// - `r`: what to run
    public static void onEdt(Runnable r) {
        if (!Display.isInitialized()) {
            r.run();
            return;
        }
        Display d = Display.getInstance();
        if (d.isEdt()) {
            r.run();
        } else {
            d.callSerially(r);
        }
    }
}
