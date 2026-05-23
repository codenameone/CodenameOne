/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.io;

/// Receives notifications when the device's active network changes type
/// (WiFi <-> Cellular <-> Ethernet <-> None <-> VPN). Register with
/// `NetworkManager.addNetworkTypeListener(NetworkTypeListener)`.
///
/// Implementations are invoked on the EDT.
public interface NetworkTypeListener {
    /// Called when the platform transitions between network classes.
    ///
    /// #### Parameters
    ///
    /// - `oldType`: one of `NetworkManager.NETWORK_TYPE_*`
    /// - `newType`: one of `NetworkManager.NETWORK_TYPE_*`
    /// - `vpnActive`: `true` if the platform reports a VPN tunnel on top of
    ///   the active network. May be `false` on platforms where VPN detection
    ///   is unsupported (see `NetworkManager.isVPNDetectionSupported()`).
    void onNetworkTypeChanged(int oldType, int newType, boolean vpnActive);
}
