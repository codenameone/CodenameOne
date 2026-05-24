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

/// Platform-supplied implementation of the WiFi APIs.
///
/// Application code talks to the static facade in `WiFi`; that facade
/// fetches the active platform via `Display.getInstance().getWifiPlatform()`
/// and dispatches each call here. Codename One platform ports
/// (Android / iOS / JavaSE / future) supply a subclass; the no-op default
/// returned by `CodenameOneImplementation` keeps stub builds compiling.
///
/// This is part of the framework's service-provider interface and not
/// intended for application use.
public abstract class WifiPlatform {
    public boolean isInfoSupported() {
        return false;
    }

    public boolean isManagementSupported() {
        return false;
    }

    public String getCurrentSSID() {
        return null;
    }

    public String getBSSID() {
        return null;
    }

    public String getGateway() {
        return null;
    }

    public String getIp() {
        return null;
    }

    public void scan(WiFiScanCallback callback) {
        if (callback != null) {
            callback.onScanComplete(null,
                    new UnsupportedOperationException(
                            "WiFi scan is not supported on this platform"));
        }
    }

    public void connect(String ssid, String password, WiFiSecurity security,
                        WiFiConnectCallback callback) {
        if (callback != null) {
            callback.onConnectResult(false,
                    new UnsupportedOperationException(
                            "WiFi connect is not supported on this platform"));
        }
    }

    public void disconnect(String ssid) {
    }
}
