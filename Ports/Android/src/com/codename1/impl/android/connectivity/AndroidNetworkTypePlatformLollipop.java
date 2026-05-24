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
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.io.NetworkManager;
import com.codename1.io.NetworkTypePlatform;
import com.codename1.ui.CN;

/// API 21+ helpers for AndroidNetworkTypePlatform. Loaded only when
/// SDK_INT >= LOLLIPOP so KitKat clients never trigger verification of
/// `ConnectivityManager.NetworkCallback`, `Network`, `NetworkCapabilities`
/// or `NetworkRequest`.
final class AndroidNetworkTypePlatformLollipop {
    private static final String TAG = "CN1NetType";

    private AndroidNetworkTypePlatformLollipop() {
    }

    static int getCurrentType(ConnectivityManager cm) {
        Network active = cm.getActiveNetwork();
        if (active == null) return NetworkManager.NETWORK_TYPE_NONE;
        NetworkCapabilities caps = cm.getNetworkCapabilities(active);
        if (caps == null) return NetworkManager.NETWORK_TYPE_NONE;
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return NetworkManager.NETWORK_TYPE_WIFI;
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return NetworkManager.NETWORK_TYPE_CELLULAR;
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return NetworkManager.NETWORK_TYPE_ETHERNET;
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
            return NetworkManager.NETWORK_TYPE_BLUETOOTH;
        }
        return NetworkManager.NETWORK_TYPE_OTHER;
    }

    static Object install(final NetworkManager target) {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return null;
        final ConnectivityManager cm = (ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return null;
        NetworkRequest req = new NetworkRequest.Builder().build();
        ConnectivityManager.NetworkCallback cb = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network n) { fire(target); }
            @Override
            public void onLost(Network n) { fire(target); }
            @Override
            public void onCapabilitiesChanged(Network n, NetworkCapabilities c) { fire(target); }
        };
        try {
            cm.registerNetworkCallback(req, cb);
        } catch (Throwable t) {
            Log.w(TAG, "registerNetworkCallback failed", t);
            return null;
        }
        return cb;
    }

    static void uninstall(Object handle) {
        if (handle == null) return;
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return;
        try {
            ConnectivityManager c = (ConnectivityManager)
                    ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            c.unregisterNetworkCallback((ConnectivityManager.NetworkCallback) handle);
        } catch (Throwable ignored) { }
    }

    private static void fire(final NetworkManager target) {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return;
        ConnectivityManager cm = (ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        final int t = cm != null ? getCurrentType(cm) : NetworkManager.NETWORK_TYPE_NONE;
        final boolean vpn = target.isVPNActive();
        CN.callSerially(new Runnable() {
            @Override public void run() {
                target.fireNetworkTypeChange(t, vpn);
            }
        });
    }
}
