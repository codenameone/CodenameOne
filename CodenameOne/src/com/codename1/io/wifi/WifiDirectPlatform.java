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

/// Platform-supplied implementation of the WiFi-Direct API. Application
/// code talks to the static facade in `WiFiDirect`; that facade fetches
/// the active platform via `Display.getInstance().getWifiDirectPlatform()`
/// and dispatches each call here.
///
/// This is part of the framework's service-provider interface and not
/// intended for application use.
public abstract class WifiDirectPlatform {
    public boolean isSupported() {
        return false;
    }

    public void startDiscovery(WiFiDirectListener listener) {
        if (listener != null) {
            listener.onDiscoveryError(new UnsupportedOperationException(
                    "WiFi Direct is not supported on this platform"));
        }
    }

    public void stopDiscovery() {
    }

    public void connect(WiFiDirectPeer peer, WiFiConnectCallback callback) {
        if (callback != null) {
            callback.onConnectResult(false,
                    new UnsupportedOperationException(
                            "WiFi Direct is not supported on this platform"));
        }
    }

    public void disconnect() {
    }
}
