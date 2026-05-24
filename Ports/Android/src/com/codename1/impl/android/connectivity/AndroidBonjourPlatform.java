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
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.util.Log;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.io.bonjour.BonjourPlatform;
import com.codename1.io.bonjour.BonjourService;
import com.codename1.io.bonjour.BonjourServiceListener;
import com.codename1.ui.CN;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/// Android implementation of `BonjourPlatform` backed by `NsdManager`
/// (API 16+). NsdManager predates Lollipop so the entire class loads
/// safely on the minimum supported Android (KitKat / API 19).
public final class AndroidBonjourPlatform extends BonjourPlatform {
    private static final String TAG = "CN1Bonjour";

    @Override
    public boolean isSupported() {
        return AndroidImplementation.getContext() != null;
    }

    @Override
    public Object startBrowse(final String typeIn,
                              final BonjourServiceListener listener) {
        if (listener == null) return null;
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) {
            CN.callSerially(new Runnable() { @Override public void run() {
                listener.onBrowseError(new RuntimeException("No application context")); } });
            return null;
        }
        final NsdManager nsd = (NsdManager) ctx.getSystemService(Context.NSD_SERVICE);
        if (nsd == null) {
            CN.callSerially(new Runnable() { @Override public void run() {
                listener.onBrowseError(new RuntimeException("NsdManager unavailable")); } });
            return null;
        }
        final String type = trimTrailingDot(typeIn);
        final NsdManager.DiscoveryListener disc = new NsdManager.DiscoveryListener() {
            @Override public void onStartDiscoveryFailed(String s, int errorCode) {
                final int code = errorCode;
                CN.callSerially(new Runnable() { @Override public void run() {
                    listener.onBrowseError(new RuntimeException("startDiscovery failed: " + code)); } });
            }
            @Override public void onStopDiscoveryFailed(String s, int errorCode) { }
            @Override public void onDiscoveryStarted(String s) { }
            @Override public void onDiscoveryStopped(String s) { }
            @Override public void onServiceFound(NsdServiceInfo info) {
                nsd.resolveService(info, new NsdManager.ResolveListener() {
                    @Override public void onResolveFailed(NsdServiceInfo info, int errorCode) { }
                    @Override public void onServiceResolved(final NsdServiceInfo info) {
                        final BonjourService svc = nsdToBonjour(info);
                        CN.callSerially(new Runnable() { @Override public void run() {
                            listener.onServiceResolved(svc); } });
                    }
                });
            }
            @Override public void onServiceLost(NsdServiceInfo info) {
                final BonjourService svc = nsdToBonjour(info);
                CN.callSerially(new Runnable() { @Override public void run() {
                    listener.onServiceLost(svc); } });
            }
        };
        try {
            nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, disc);
        } catch (Throwable t) {
            final Throwable err = t;
            CN.callSerially(new Runnable() { @Override public void run() {
                listener.onBrowseError(err); } });
            return null;
        }
        return new Object[]{nsd, disc};
    }

    @Override
    public void stopBrowse(Object handle) {
        if (handle == null) return;
        Object[] arr = (Object[]) handle;
        NsdManager nsd = (NsdManager) arr[0];
        NsdManager.DiscoveryListener l = (NsdManager.DiscoveryListener) arr[1];
        try { nsd.stopServiceDiscovery(l); } catch (Throwable ignored) { }
    }

    @Override
    public Object startPublish(String name, String type, int port,
                               Map<String, String> txt) {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return null;
        NsdManager nsd = (NsdManager) ctx.getSystemService(Context.NSD_SERVICE);
        if (nsd == null) return null;
        NsdServiceInfo info = new NsdServiceInfo();
        info.setServiceName(name);
        info.setServiceType(trimTrailingDot(type));
        info.setPort(port);
        if (txt != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (Map.Entry<String, String> e : txt.entrySet()) {
                try { info.setAttribute(e.getKey(), e.getValue()); }
                catch (Throwable ignored) { }
            }
        }
        NsdManager.RegistrationListener listener = new NsdManager.RegistrationListener() {
            @Override public void onRegistrationFailed(NsdServiceInfo info, int errorCode) { }
            @Override public void onUnregistrationFailed(NsdServiceInfo info, int errorCode) { }
            @Override public void onServiceRegistered(NsdServiceInfo info) { }
            @Override public void onServiceUnregistered(NsdServiceInfo info) { }
        };
        try {
            nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener);
        } catch (Throwable t) {
            Log.w(TAG, "registerService failed", t);
            return null;
        }
        return new Object[]{nsd, listener};
    }

    @Override
    public void stopPublish(Object handle) {
        if (handle == null) return;
        Object[] arr = (Object[]) handle;
        NsdManager nsd = (NsdManager) arr[0];
        NsdManager.RegistrationListener l = (NsdManager.RegistrationListener) arr[1];
        try { nsd.unregisterService(l); } catch (Throwable ignored) { }
    }

    private static BonjourService nsdToBonjour(NsdServiceInfo info) {
        Map<String, String> txt = new HashMap<String, String>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && info.getAttributes() != null) {
            for (Map.Entry<String, byte[]> e : info.getAttributes().entrySet()) {
                txt.put(e.getKey(), e.getValue() == null
                        ? "" : new String(e.getValue(), Charset.forName("UTF-8")));
            }
        }
        String host = info.getHost() != null ? info.getHost().getHostAddress() : null;
        return new BonjourService(info.getServiceName(), info.getServiceType(),
                host, info.getPort(), txt);
    }

    private static String trimTrailingDot(String s) {
        if (s == null) return null;
        if (s.endsWith(".")) return s.substring(0, s.length() - 1);
        return s;
    }
}
