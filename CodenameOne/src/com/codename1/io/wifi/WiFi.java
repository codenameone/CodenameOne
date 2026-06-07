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

/// Entry point for inspecting, scanning and connecting to WiFi networks.
///
/// The API is split into two tiers:
///
/// 1. **Information queries** -- `getCurrentSSID()`, `getBSSID()`,
///    `getGateway()`, `getIp()`. These methods read the device's current
///    network configuration. On modern Android (10+) and iOS the SSID/BSSID
///    queries require runtime permissions or entitlements that the build
///    pipeline injects automatically based on classpath scanning.
///
/// 2. **Active management** -- `scan()` and `connect(SSID, password, security)`.
///    Active management triggers a one-shot scan or attempts to associate the
///    device with a specific network. On Android 10+ this is implemented with
///    `NetworkSpecifier`; on iOS with `NEHotspotConfiguration`.
///
/// All callbacks are dispatched on the EDT so it is safe to update UI in
/// response to them.
///
/// #### Required permissions
///
/// The build pipeline injects the necessary permissions automatically when
/// `WiFi` is referenced anywhere in the application:
///
/// - **Android**: `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`,
///   `ACCESS_NETWORK_STATE`, `CHANGE_NETWORK_STATE`,
///   `ACCESS_FINE_LOCATION` (required for SSID readout on API 26+),
///   `NEARBY_WIFI_DEVICES` (API 33+).
/// - **iOS**: `com.apple.developer.networking.HotspotConfiguration` entitlement
///   for connect, `com.apple.developer.networking.wifi-info` entitlement for
///   the SSID/BSSID query, and `NSLocationWhenInUseUsageDescription`
///   (CoreLocation authorization is required since iOS 13 to read SSID).
public final class WiFi {
    private WiFi() {
    }

    private static WifiPlatform platform() {
        return Display.getInstance().getWifiPlatform();
    }

    /// `true` if the current platform can query WiFi information.
    public static boolean isInfoSupported() {
        return platform().isInfoSupported();
    }

    /// `true` if the current platform supports active scan / connect.
    public static boolean isManagementSupported() {
        return platform().isManagementSupported();
    }

    /// The SSID of the currently associated WiFi network, or `null` if not
    /// connected to WiFi or if permission was denied. On iOS 13+ the OS
    /// returns `null` unless the app has CoreLocation authorization.
    public static String getCurrentSSID() {
        return platform().getCurrentSSID();
    }

    /// The BSSID (MAC address of the access point) of the currently associated
    /// WiFi network, formatted as colon-separated lowercase hex
    /// (e.g. `aa:bb:cc:11:22:33`), or `null` if unavailable.
    public static String getBSSID() {
        return platform().getBSSID();
    }

    /// Default gateway IP address as a dotted quad (e.g. `192.168.1.1`), or
    /// `null` if no default gateway is configured.
    public static String getGateway() {
        return platform().getGateway();
    }

    /// Local IP address on the WiFi interface as a dotted quad
    /// (e.g. `192.168.1.42`), or `null` if WiFi is not active.
    public static String getIp() {
        return platform().getIp();
    }

    /// Triggers a one-shot WiFi scan and reports results to `callback` on the
    /// EDT. The callback receives an array of `WiFiNetwork` sorted by signal
    /// strength (strongest first). Pass `null` to cancel an in-progress scan.
    ///
    /// Behaviour:
    ///
    /// - **Android**: uses `WifiManager.startScan()`. On API 28+ the OS
    ///   throttles scans to 4 per 2 minutes per foreground app; throttled
    ///   scans return cached results.
    /// - **iOS**: not supported -- iOS does not expose a public WiFi scan
    ///   API. `callback` is invoked with `null` and `error` set.
    /// - **Simulator**: returns a small synthetic list and prints a warning
    ///   reminding the developer the data is fabricated.
    public static void scan(WiFiScanCallback callback) {
        platform().scan(callback);
    }

    /// Attempts to associate the device with `ssid`. `password` may be `null`
    /// for open networks. `security` must be one of the `WiFiSecurity`
    /// constants and **must** match the security mode the access point
    /// advertises -- a mismatch will cause `connect` to fail.
    ///
    /// Behaviour:
    ///
    /// - **Android 10+**: uses `WifiNetworkSpecifier` via
    ///   `ConnectivityManager.requestNetwork()`. The OS shows a system
    ///   dialog asking the user to approve the association; this dialog
    ///   can't be bypassed.
    /// - **Android 9 and below**: uses the legacy
    ///   `WifiConfiguration` API and `WifiManager.enableNetwork()`. The user
    ///   is not prompted but the call may be a no-op on OEM builds that
    ///   removed the API early.
    /// - **iOS 11+**: uses `NEHotspotConfiguration`. The user is shown a
    ///   system prompt the first time the app tries to join the SSID.
    /// - **Simulator**: logs the request and reports a failure.
    public static void connect(String ssid, String password,
                               WiFiSecurity security,
                               WiFiConnectCallback callback) {
        platform().connect(ssid, password, security, callback);
    }

    /// Disconnect the request made via `connect`. On Android 10+ this releases
    /// the `NetworkSpecifier`; on iOS it removes the hotspot configuration.
    /// Apps can't force-disconnect a network the user joined manually.
    public static void disconnect(String ssid) {
        platform().disconnect(ssid);
    }
}
