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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.io.NetworkManager;
import com.codename1.io.NetworkTypePlatform;
import com.codename1.ui.CN;

/// Network-type tracking for Android. The Lollipop+ NetworkCallback path
/// lives in `AndroidNetworkTypePlatformLollipop` so it loads only on
/// devices that have those classes; this entry class works on KitKat with
/// the legacy `CONNECTIVITY_ACTION` broadcast.
public final class AndroidNetworkTypePlatform extends NetworkTypePlatform {
    private BroadcastReceiver legacyReceiver;
    private Object lollipopHandle; // ConnectivityManager.NetworkCallback when on L+

    @Override
    public int getCurrentNetworkType() {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return NetworkManager.NETWORK_TYPE_NONE;
        ConnectivityManager c = (ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (c == null) return NetworkManager.NETWORK_TYPE_NONE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return AndroidNetworkTypePlatformLollipop.getCurrentType(c);
        }
        NetworkInfo info = c.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            return NetworkManager.NETWORK_TYPE_NONE;
        }
        switch (info.getType()) {
            case ConnectivityManager.TYPE_WIFI: return NetworkManager.NETWORK_TYPE_WIFI;
            case ConnectivityManager.TYPE_MOBILE: return NetworkManager.NETWORK_TYPE_CELLULAR;
            case ConnectivityManager.TYPE_ETHERNET: return NetworkManager.NETWORK_TYPE_ETHERNET;
            case ConnectivityManager.TYPE_BLUETOOTH: return NetworkManager.NETWORK_TYPE_BLUETOOTH;
            default: return NetworkManager.NETWORK_TYPE_OTHER;
        }
    }

    @Override
    public void install(final NetworkManager target) {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            lollipopHandle = AndroidNetworkTypePlatformLollipop.install(target);
            return;
        }
        legacyReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                dispatch(target);
            }
        };
        ctx.registerReceiver(legacyReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public void uninstall(NetworkManager target) {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return;
        if (lollipopHandle != null) {
            AndroidNetworkTypePlatformLollipop.uninstall(lollipopHandle);
            lollipopHandle = null;
        }
        if (legacyReceiver != null) {
            try { ctx.unregisterReceiver(legacyReceiver); } catch (Throwable ignored) { }
            legacyReceiver = null;
        }
    }

    void dispatch(final NetworkManager target) {
        final int t = getCurrentNetworkType();
        final boolean vpn = target.isVPNActive();
        CN.callSerially(new Runnable() {
            @Override public void run() {
                target.fireNetworkTypeChange(t, vpn);
            }
        });
    }
}
