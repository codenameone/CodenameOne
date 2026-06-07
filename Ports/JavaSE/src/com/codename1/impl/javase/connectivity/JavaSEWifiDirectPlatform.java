/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.javase.connectivity;

import com.codename1.io.wifi.WiFiConnectCallback;
import com.codename1.io.wifi.WiFiDirectListener;
import com.codename1.io.wifi.WiFiDirectPeer;
import com.codename1.io.wifi.WifiDirectPlatform;
import com.codename1.ui.CN;

/// JavaSE stub for WiFi Direct -- the desktop has no equivalent so we
/// just report unsupported and log usage so production-build issues
/// surface during simulator testing.
public final class JavaSEWifiDirectPlatform extends WifiDirectPlatform {
    @Override
    public boolean isSupported() { return false; }

    @Override
    public void startDiscovery(final WiFiDirectListener l) {
        JavaSEConnectivityUsage.noteUsage("WiFiDirect");
        if (l == null) return;
        CN.callSerially(new Runnable() {
            @Override public void run() {
                l.onDiscoveryError(new UnsupportedOperationException(
                        "WiFi Direct is not supported on JavaSE"));
            }
        });
    }

    @Override
    public void stopDiscovery() { }

    @Override
    public void connect(WiFiDirectPeer peer, final WiFiConnectCallback cb) {
        if (cb != null) {
            CN.callSerially(new Runnable() {
                @Override public void run() {
                    cb.onConnectResult(false, new UnsupportedOperationException(
                            "WiFi Direct is not supported on JavaSE"));
                }
            });
        }
    }

    @Override
    public void disconnect() { }
}
