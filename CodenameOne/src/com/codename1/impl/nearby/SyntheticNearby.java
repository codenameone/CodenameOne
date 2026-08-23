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

/// The cast of devices a [LocalNearbyBridge] starts with, so the simulator
/// and the desktop ports have something to find without every app writing
/// its own fixture.
///
/// The line-up is deliberately awkward rather than tidy, for the reason
/// `com.codename1.impl.home.SyntheticHome` is: a fixture where every device
/// has a name, an address and a service is a fixture that never exercises
/// the branches an app needs for the ones that do not. So there is a device
/// with no advertised service, one whose name collides on a prefix with
/// another, and an endpoint whose name is long enough to overflow a label.
///
/// @hidden not part of the public API.
public final class SyntheticNearby {

    /// The BLE heart-rate service, the one most likely to appear in an
    /// example filter.
    public static final String HEART_RATE_SERVICE = "180D";

    private SyntheticNearby() {
    }

    /// Fills a bridge with the default cast.
    ///
    /// #### Parameters
    ///
    /// - `bridge`: the bridge to populate
    public static void populate(LocalNearbyBridge bridge) {
        // Association candidates. Order matters: the first is what a filter
        // -free request returns.
        bridge.addCandidate("Simulated Watch", "00:11:22:33:44:01", null);
        bridge.addCandidate("Simulated Heart Rate Strap", "00:11:22:33:44:02",
                HEART_RATE_SERVICE);
        bridge.addCandidate("Simulated Heart Rate Strap Mk II",
                "00:11:22:33:44:03", HEART_RATE_SERVICE);
        // No service, so a service filter must not match it and a name
        // filter must.
        bridge.addCandidate("Simulated Tag", "00:11:22:33:44:04", null);

        // Transport endpoints.
        bridge.addEndpoint("sim-endpoint-1", "Simulated Phone");
        bridge.addEndpoint("sim-endpoint-2",
                "Simulated Phone With A Deliberately Long Advertised Name");
    }
}
