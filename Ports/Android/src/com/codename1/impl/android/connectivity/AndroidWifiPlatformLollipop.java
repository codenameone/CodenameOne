/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.android.connectivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.RouteInfo;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.io.wifi.WiFiConnectCallback;
import com.codename1.io.wifi.WiFiSecurity;
import com.codename1.ui.CN;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/// API 21+ (Lollipop) helpers for AndroidWifiPlatform. Kept in its own
/// class so the API 21+ symbol references it imports (`Network`,
/// `LinkProperties`, `NetworkCallback`, `NetworkSpecifier`) only trigger
/// classloader verification on devices that actually support them. Callers
/// guard every invocation with `Build.VERSION.SDK_INT >= LOLLIPOP`; on
/// KitKat this class is never loaded.
final class AndroidWifiPlatformLollipop {
    // The cn1-binaries compile SDK predates WifiNetworkSpecifier.Builder
    // (API 29). The builder is reached reflectively in connectQ() below so
    // the file builds without an import; the runtime path is exercised
    // only when SDK_INT >= 29.

    private static final Map<String, ConnectivityManager.NetworkCallback> pendingConnects
            = new HashMap<String, ConnectivityManager.NetworkCallback>();

    private AndroidWifiPlatformLollipop() {
    }

    private static ConnectivityManager cm() {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return null;
        return (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    static String getGateway() {
        ConnectivityManager c = cm();
        if (c == null) return null;
        Network n = c.getActiveNetwork();
        if (n == null) return null;
        LinkProperties lp = c.getLinkProperties(n);
        if (lp == null) return null;
        for (RouteInfo r : lp.getRoutes()) {
            if (r.isDefaultRoute() && r.getGateway() instanceof Inet4Address) {
                return r.getGateway().getHostAddress();
            }
        }
        return null;
    }

    static String getWifiIp() {
        ConnectivityManager c = cm();
        if (c == null) return null;
        Network n = c.getActiveNetwork();
        if (n == null) return null;
        LinkProperties lp = c.getLinkProperties(n);
        if (lp == null) return null;
        for (LinkAddress la : lp.getLinkAddresses()) {
            InetAddress a = la.getAddress();
            if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                return a.getHostAddress();
            }
        }
        return null;
    }

    static void connectQ(Context ctx, final String ssid, String password,
                         WiFiSecurity security,
                         final WiFiConnectCallback cb) {
        NetworkSpecifier spec;
        try {
            Class<?> builderCls = Class.forName(
                    "android.net.wifi.WifiNetworkSpecifier$Builder");
            Object builder = builderCls.getConstructor().newInstance();
            builderCls.getMethod("setSsid", String.class)
                    .invoke(builder, ssid);
            if (password != null && password.length() > 0) {
                String setter = security == WiFiSecurity.WPA3_SAE
                        ? "setWpa3Passphrase" : "setWpa2Passphrase";
                builderCls.getMethod(setter, String.class)
                        .invoke(builder, password);
            }
            spec = (NetworkSpecifier) builderCls.getMethod("build")
                    .invoke(builder);
        } catch (Throwable t) {
            AndroidWifiPlatform.failConnect(cb,
                    "WifiNetworkSpecifier not available: " + t);
            return;
        }
        NetworkRequest req = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(spec)
                .build();
        ConnectivityManager c = (ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network n) {
                CN.callSerially(new Runnable() {
                    @Override public void run() {
                        if (cb != null) cb.onConnectResult(true, null);
                    }
                });
            }
            @Override
            public void onUnavailable() {
                CN.callSerially(new Runnable() {
                    @Override public void run() {
                        if (cb != null) cb.onConnectResult(false,
                                new RuntimeException("WiFi connect unavailable / rejected"));
                    }
                });
            }
        };
        pendingConnects.put(ssid, callback);
        try {
            c.requestNetwork(req, callback);
        } catch (Throwable t) {
            AndroidWifiPlatform.failConnect(cb, t.getMessage());
        }
    }

    static void disconnect(String ssid) {
        ConnectivityManager.NetworkCallback cb = pendingConnects.remove(ssid);
        if (cb == null) return;
        try {
            ConnectivityManager c = cm();
            if (c != null) c.unregisterNetworkCallback(cb);
        } catch (Throwable ignored) { }
    }
}
