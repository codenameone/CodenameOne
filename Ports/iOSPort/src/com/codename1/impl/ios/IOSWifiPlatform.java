/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.ios;

import com.codename1.io.wifi.WiFiConnectCallback;
import com.codename1.io.wifi.WiFiScanCallback;
import com.codename1.io.wifi.WiFiSecurity;
import com.codename1.io.wifi.WifiPlatform;
import com.codename1.ui.CN;

/// iOS-side `WifiPlatform`. Delegates to `IOSNative` for the
/// CaptiveNetwork / NEHotspotConfiguration native code. The native side
/// gates each entry point behind a CN1_INCLUDE_* define so apps that
/// don't reference `WiFi` ship without the matching framework symbols.
public final class IOSWifiPlatform extends WifiPlatform {
    @Override
    public boolean isInfoSupported() { return true; }

    @Override
    public boolean isManagementSupported() { return true; }

    @Override
    public String getCurrentSSID() { return IOSImplementation.nativeInstance.wifiCurrentSSID(); }

    @Override
    public String getBSSID() { return IOSImplementation.nativeInstance.wifiCurrentBSSID(); }

    @Override
    public String getGateway() { return IOSImplementation.nativeInstance.wifiGateway(); }

    @Override
    public String getIp() { return IOSImplementation.nativeInstance.wifiIpAddress(); }

    @Override
    public void scan(final WiFiScanCallback cb) {
        // iOS doesn't expose a public scan API.
        if (cb != null) {
            CN.callSerially(new Runnable() {
                @Override public void run() {
                    cb.onScanComplete(null,
                            new UnsupportedOperationException(
                                    "iOS does not expose a WiFi scan API"));
                }
            });
        }
    }

    @Override
    public void connect(String ssid, String password, WiFiSecurity security,
                        WiFiConnectCallback cb) {
        IOSConnectivity.setPendingConnect(cb);
        int sec = security == null ? 0 : security.ordinal();
        IOSImplementation.nativeInstance.wifiConnect(ssid, password, sec);
    }

    @Override
    public void disconnect(String ssid) {
        IOSImplementation.nativeInstance.wifiDisconnect(ssid);
    }
}
