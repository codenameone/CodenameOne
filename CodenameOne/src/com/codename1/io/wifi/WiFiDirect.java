/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.io.wifi;

import com.codename1.ui.Display;

/// WiFi Direct (Wi-Fi P2P) discovery and grouping.
///
/// WiFi Direct lets two devices form a peer-to-peer link without going through
/// an access point. Use it for ad-hoc file transfer, multi-player games on a
/// local network, or any other scenario where a router is unavailable.
///
/// #### Platform support
///
/// - **Android**: full support via `android.net.wifi.p2p.WifiP2pManager`.
///   The build pipeline injects `CHANGE_WIFI_STATE`, `ACCESS_WIFI_STATE`,
///   `ACCESS_NETWORK_STATE`, `ACCESS_FINE_LOCATION` (API 26+) and
///   `NEARBY_WIFI_DEVICES` (API 33+) when this class is referenced.
/// - **iOS**: not supported. iOS uses MultipeerConnectivity for similar
///   scenarios; that API is intentionally out of scope here.
/// - **Simulator**: stubbed. Discovery returns no peers and `connect`
///   reports failure.
public final class WiFiDirect {
    private WiFiDirect() {
    }

    private static WifiDirectPlatform platform() {
        return Display.getInstance().getWifiDirectPlatform();
    }

    /// `true` if the current platform implements WiFi Direct.
    public static boolean isSupported() {
        return platform().isSupported();
    }

    /// Starts peer discovery. `listener` is invoked on the EDT for every peer
    /// list change. Call `stopDiscovery()` to release radio resources when
    /// you're done.
    public static void startDiscovery(WiFiDirectListener listener) {
        platform().startDiscovery(listener);
    }

    /// Stops peer discovery and detaches all listeners.
    public static void stopDiscovery() {
        platform().stopDiscovery();
    }

    /// Forms a P2P group with `peer`. The user is shown a confirmation prompt
    /// on both devices the first time they connect; subsequent connections
    /// reuse the cached pairing where possible.
    public static void connect(WiFiDirectPeer peer,
                               WiFiConnectCallback callback) {
        platform().connect(peer, callback);
    }

    /// Drops the current group, if any.
    public static void disconnect() {
        platform().disconnect();
    }
}
