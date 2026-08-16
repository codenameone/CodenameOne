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
package com.codename1.wearable;

/// A device on the other end of the link: the watch as seen from the phone, or the phone as seen
/// from the watch.
///
/// Apple pairs a phone with exactly one watch at a time, so there is at most one node there. Wear OS
/// allows several watches paired to one phone, so a phone app can see more than one -- send to all
/// of them unless you have a reason to pick.
public class WearableNode {
    private final String id;
    private final String displayName;
    private final boolean nearby;

    /// Creates a node description. Called by the platform ports; application code obtains nodes from
    /// [WearableConnection#getConnectedNodes()].
    ///
    /// #### Parameters
    ///
    /// - `id`: the platform's opaque identifier for the device
    /// - `displayName`: the device name a person would recognize
    /// - `nearby`: true when the device is directly connected rather than reachable over the cloud
    public WearableNode(String id, String displayName, boolean nearby) {
        this.id = id;
        this.displayName = displayName;
        this.nearby = nearby;
    }

    /// Returns the platform's opaque identifier for this device, stable for as long as the pairing
    /// lasts.
    ///
    /// #### Returns
    ///
    /// the node id
    public String getId() {
        return id;
    }

    /// Returns the device name a person would recognize, suitable for showing in a UI.
    ///
    /// #### Returns
    ///
    /// the display name
    public String getDisplayName() {
        return displayName;
    }

    /// Returns true when the device is directly connected (Bluetooth or the same network) rather
    /// than merely reachable through the cloud.
    ///
    /// Informational, not a delivery rule. A cloud-routed peer still receives messages, requests
    /// and replicated data -- it appears in the connected set, which is what "connected" means --
    /// so [WearableConnection#isReachable] and message delivery do not consult this. Expect higher
    /// latency, and an actual failure is reported through the send's own error path rather than
    /// predicted from the transport.
    ///
    /// #### Returns
    ///
    /// true if the node is directly connected
    public boolean isNearby() {
        return nearby;
    }

    @Override
    public String toString() {
        return "WearableNode[" + displayName + (nearby ? ", nearby]" : "]");
    }
}
