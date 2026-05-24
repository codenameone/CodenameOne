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
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.io.wifi.WiFiConnectCallback;
import com.codename1.io.wifi.WiFiDirectListener;
import com.codename1.io.wifi.WiFiDirectPeer;
import com.codename1.io.wifi.WifiDirectPlatform;
import com.codename1.ui.CN;

import java.util.ArrayList;

/// Android implementation of Wi-Fi Direct via `WifiP2pManager` (API 14+).
/// All symbols touched here exist on the minimum supported Android
/// (KitKat / API 19) so the class verifies without conditional loading.
public final class AndroidWifiDirectPlatform extends WifiDirectPlatform {
    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel p2pChannel;
    private BroadcastReceiver p2pReceiver;
    private WiFiDirectListener p2pListener;

    @Override
    public boolean isSupported() {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return false;
        return ctx.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT);
    }

    @Override
    public void startDiscovery(final WiFiDirectListener listener) {
        if (listener == null) return;
        final Context ctx = AndroidImplementation.getContext();
        if (ctx == null) {
            listener.onDiscoveryError(new RuntimeException("No application context"));
            return;
        }
        p2pManager = (WifiP2pManager) ctx.getSystemService(Context.WIFI_P2P_SERVICE);
        if (p2pManager == null) {
            listener.onDiscoveryError(new RuntimeException("WifiP2pManager unavailable"));
            return;
        }
        p2pChannel = p2pManager.initialize(ctx, Looper.getMainLooper(), null);
        p2pListener = listener;
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        p2pReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                if (!WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
                        .equals(i.getAction())) return;
                p2pManager.requestPeers(p2pChannel,
                        new WifiP2pManager.PeerListListener() {
                    @Override public void onPeersAvailable(WifiP2pDeviceList list) {
                        ArrayList<WiFiDirectPeer> peers = new ArrayList<WiFiDirectPeer>();
                        for (WifiP2pDevice d : list.getDeviceList()) {
                            peers.add(new WiFiDirectPeer(d.deviceName,
                                    d.deviceAddress, mapP2pStatus(d.status)));
                        }
                        final WiFiDirectPeer[] arr = peers.toArray(new WiFiDirectPeer[peers.size()]);
                        CN.callSerially(new Runnable() {
                            @Override public void run() {
                                p2pListener.onPeersAvailable(arr);
                            }
                        });
                    }
                });
            }
        };
        ctx.registerReceiver(p2pReceiver, filter);
        p2pManager.discoverPeers(p2pChannel, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() { }
            @Override public void onFailure(final int reason) {
                final int r = reason;
                CN.callSerially(new Runnable() {
                    @Override public void run() {
                        listener.onDiscoveryError(
                                new RuntimeException("discoverPeers failed: " + r));
                    }
                });
            }
        });
    }

    private static int mapP2pStatus(int status) {
        switch (status) {
            case WifiP2pDevice.AVAILABLE: return WiFiDirectPeer.STATE_AVAILABLE;
            case WifiP2pDevice.INVITED: return WiFiDirectPeer.STATE_INVITED;
            case WifiP2pDevice.CONNECTED: return WiFiDirectPeer.STATE_CONNECTED;
            case WifiP2pDevice.FAILED: return WiFiDirectPeer.STATE_FAILED;
            case WifiP2pDevice.UNAVAILABLE: return WiFiDirectPeer.STATE_UNAVAILABLE;
            default: return WiFiDirectPeer.STATE_AVAILABLE;
        }
    }

    @Override
    public void stopDiscovery() {
        Context ctx = AndroidImplementation.getContext();
        if (ctx != null && p2pReceiver != null) {
            try { ctx.unregisterReceiver(p2pReceiver); } catch (Throwable ignored) { }
        }
        if (p2pManager != null && p2pChannel != null) {
            try { p2pManager.stopPeerDiscovery(p2pChannel, null); } catch (Throwable ignored) { }
        }
        p2pReceiver = null;
        p2pListener = null;
    }

    @Override
    public void connect(WiFiDirectPeer peer, final WiFiConnectCallback cb) {
        if (p2pManager == null || p2pChannel == null) {
            if (cb != null) {
                CN.callSerially(new Runnable() {
                    @Override public void run() {
                        cb.onConnectResult(false,
                                new RuntimeException("WiFi Direct discovery not started"));
                    }
                });
            }
            return;
        }
        WifiP2pConfig cfg = new WifiP2pConfig();
        cfg.deviceAddress = peer.getDeviceAddress();
        p2pManager.connect(p2pChannel, cfg, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() {
                CN.callSerially(new Runnable() {
                    @Override public void run() {
                        if (cb != null) cb.onConnectResult(true, null);
                    }
                });
            }
            @Override public void onFailure(final int reason) {
                final int r = reason;
                CN.callSerially(new Runnable() {
                    @Override public void run() {
                        if (cb != null) cb.onConnectResult(false,
                                new RuntimeException("connect failed: " + r));
                    }
                });
            }
        });
    }

    @Override
    public void disconnect() {
        if (p2pManager != null && p2pChannel != null) {
            try { p2pManager.removeGroup(p2pChannel, null); } catch (Throwable ignored) { }
        }
    }
}
