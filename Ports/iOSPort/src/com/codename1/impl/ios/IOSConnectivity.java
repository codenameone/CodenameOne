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

import com.codename1.io.NetworkManager;
import com.codename1.io.bonjour.BonjourService;
import com.codename1.io.bonjour.BonjourServiceListener;
import com.codename1.io.wifi.WiFiConnectCallback;
import com.codename1.ui.CN;

import java.util.HashMap;
import java.util.Map;

/// Java-side glue for the iOS connectivity layer. Native code in IOSNative.m
/// invokes the static `*Dispatch` methods here to deliver callbacks back to
/// the EDT.
public final class IOSConnectivity {
    private static final Map<Long, BonjourServiceListener> bonjourListeners
            = new HashMap<Long, BonjourServiceListener>();
    private static WiFiConnectCallback pendingConnect;
    private static NetworkManager networkManagerInstance;

    private IOSConnectivity() {
    }

    // ---------------------------------------------------------------------
    // Bonjour dispatch
    // ---------------------------------------------------------------------

    static void registerBonjour(long handle, BonjourServiceListener l) {
        bonjourListeners.put(Long.valueOf(handle), l);
    }

    static void unregisterBonjour(long handle) {
        bonjourListeners.remove(Long.valueOf(handle));
    }

    public static void bonjourResolveDispatch(final long handle,
                                              final String name,
                                              final String type,
                                              final String host, final int port,
                                              final String[] txtKeys,
                                              final String[] txtVals) {
        final BonjourServiceListener l = bonjourListeners.get(Long.valueOf(handle));
        if (l == null) return;
        Map<String, String> txt = new HashMap<String, String>();
        if (txtKeys != null) {
            for (int i = 0; i < txtKeys.length; i++) {
                txt.put(txtKeys[i], i < txtVals.length ? txtVals[i] : "");
            }
        }
        final BonjourService svc = new BonjourService(name, type, host, port, txt);
        CN.callSerially(new Runnable() {
            @Override public void run() {
                l.onServiceResolved(svc);
            }
        });
    }

    public static void bonjourLostDispatch(final long handle, final String name,
                                           final String type) {
        final BonjourServiceListener l = bonjourListeners.get(Long.valueOf(handle));
        if (l == null) return;
        final BonjourService svc = new BonjourService(name, type, null, 0, null);
        CN.callSerially(new Runnable() {
            @Override public void run() {
                l.onServiceLost(svc);
            }
        });
    }

    public static void bonjourErrorDispatch(final long handle, final String msg) {
        final BonjourServiceListener l = bonjourListeners.get(Long.valueOf(handle));
        if (l == null) return;
        CN.callSerially(new Runnable() {
            @Override public void run() {
                l.onBrowseError(new RuntimeException(msg));
            }
        });
    }

    // ---------------------------------------------------------------------
    // WiFi connect dispatch
    // ---------------------------------------------------------------------

    static void setPendingConnect(WiFiConnectCallback cb) {
        pendingConnect = cb;
    }

    public static void wifiConnectResult(final boolean ok, final String errMsg) {
        final WiFiConnectCallback cb = pendingConnect;
        pendingConnect = null;
        if (cb == null) return;
        CN.callSerially(new Runnable() {
            @Override public void run() {
                cb.onConnectResult(ok, ok ? null : new RuntimeException(errMsg));
            }
        });
    }

    // ---------------------------------------------------------------------
    // Network type change dispatch
    // ---------------------------------------------------------------------

    static void registerNetworkTypeTarget(NetworkManager nm) {
        networkManagerInstance = nm;
    }

    static void unregisterNetworkTypeTarget() {
        networkManagerInstance = null;
    }

    public static void networkTypeChangedDispatch(final int newType,
                                                  final boolean vpn) {
        final NetworkManager nm = networkManagerInstance;
        if (nm == null) return;
        CN.callSerially(new Runnable() {
            @Override public void run() {
                nm.fireNetworkTypeChange(newType, vpn);
            }
        });
    }
}
