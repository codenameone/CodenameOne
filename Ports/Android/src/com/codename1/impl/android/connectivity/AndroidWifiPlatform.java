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

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.format.Formatter;
import android.util.Log;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.io.wifi.WiFiConnectCallback;
import com.codename1.io.wifi.WiFiNetwork;
import com.codename1.io.wifi.WiFiScanCallback;
import com.codename1.io.wifi.WiFiSecurity;
import com.codename1.io.wifi.WifiPlatform;
import com.codename1.ui.CN;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/// Android implementation of WiFi info + scan + connect. The class deliberately
/// imports only API 19-safe symbols. The API 29+ `WifiNetworkSpecifier` flow
/// and any other "modern" Android APIs are reached via reflection or split
/// into separate helpers so this class loads cleanly on KitKat (the minimum
/// SDK supported by the Codename One Android port).
public final class AndroidWifiPlatform extends WifiPlatform {
    private static final String TAG = "CN1WiFi";
    // Build.VERSION_CODES.Q (=29) isn't defined on the legacy compile SDK
    // shipped with cn1-binaries (API 25); use the literal value.
    private static final int SDK_Q = 29;
    private BroadcastReceiver scanReceiver;

    @Override
    public boolean isInfoSupported() {
        return true;
    }

    @Override
    public boolean isManagementSupported() {
        return true;
    }

    private WifiManager wifi() {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return null;
        // applicationContext per Android docs to avoid leaking the activity
        // through the singleton WifiManager.
        return (WifiManager) ctx.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public String getCurrentSSID() {
        WifiManager wm = wifi();
        if (wm == null) return null;
        WifiInfo info = wm.getConnectionInfo();
        if (info == null) return null;
        String s = info.getSSID();
        if (s == null) return null;
        // Android wraps SSID in quotes and returns "<unknown ssid>" when
        // permission has not been granted.
        if (s.length() > 1 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        if ("<unknown ssid>".equals(s) || s.length() == 0) {
            return null;
        }
        return s;
    }

    @Override
    public String getBSSID() {
        WifiManager wm = wifi();
        if (wm == null) return null;
        WifiInfo info = wm.getConnectionInfo();
        if (info == null) return null;
        String s = info.getBSSID();
        if (s == null || "02:00:00:00:00:00".equals(s)) {
            return null;
        }
        return s.toLowerCase(Locale.US);
    }

    @Override
    public String getGateway() {
        // On Lollipop+ we'd prefer LinkProperties for the real default
        // route. Read it through a helper that's only loaded on Lollipop+
        // so this class verifies on KitKat.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String gw = AndroidWifiPlatformLollipop.getGateway();
            if (gw != null) return gw;
        }
        WifiManager wm = wifi();
        if (wm == null) return null;
        try {
            return Formatter.formatIpAddress(wm.getDhcpInfo().gateway);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public String getIp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String ip = AndroidWifiPlatformLollipop.getWifiIp();
            if (ip != null) return ip;
        }
        WifiManager wm = wifi();
        if (wm == null) return null;
        WifiInfo info = wm.getConnectionInfo();
        if (info == null) return null;
        int ip = info.getIpAddress();
        if (ip == 0) return null;
        return Formatter.formatIpAddress(ip);
    }

    @Override
    public void scan(final WiFiScanCallback cb) {
        if (cb == null) return;
        final Context ctx = AndroidImplementation.getContext();
        final WifiManager wm = wifi();
        if (ctx == null || wm == null) {
            fail(cb, "WiFi unavailable");
            return;
        }
        if (!checkPermission(Manifest.permission.ACCESS_WIFI_STATE)) {
            fail(cb, "ACCESS_WIFI_STATE not granted");
            return;
        }
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        if (scanReceiver != null) {
            try { ctx.unregisterReceiver(scanReceiver); } catch (Throwable t) { /* ignore */ }
        }
        scanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                try { c.unregisterReceiver(this); } catch (Throwable t) { /* ignore */ }
                scanReceiver = null;
                final List<ScanResult> results = wm.getScanResults();
                final WiFiNetwork[] mapped = new WiFiNetwork[results.size()];
                for (int j = 0; j < results.size(); j++) {
                    ScanResult r = results.get(j);
                    mapped[j] = new WiFiNetwork(
                            r.SSID,
                            r.BSSID != null ? r.BSSID.toLowerCase(Locale.US) : null,
                            r.level,
                            r.frequency,
                            mapAndroidSecurity(r.capabilities));
                }
                java.util.Arrays.sort(mapped, new Comparator<WiFiNetwork>() {
                    @Override public int compare(WiFiNetwork a, WiFiNetwork b) {
                        return b.getRssi() - a.getRssi();
                    }
                });
                CN.callSerially(new Runnable() {
                    @Override public void run() {
                        cb.onScanComplete(mapped, null);
                    }
                });
            }
        };
        ctx.registerReceiver(scanReceiver, filter);
        boolean started = wm.startScan();
        if (!started) {
            // On API 28+ the OS throttles; deliver cached results.
            CN.callSerially(new Runnable() {
                @Override public void run() {
                    BroadcastReceiver r = scanReceiver;
                    if (r != null) {
                        r.onReceive(ctx, new Intent(
                                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                    }
                }
            });
        }
    }

    private static WiFiSecurity mapAndroidSecurity(String capabilities) {
        if (capabilities == null) return WiFiSecurity.UNKNOWN;
        String c = capabilities.toUpperCase(Locale.US);
        if (c.contains("WPA3") || c.contains("SAE")) return WiFiSecurity.WPA3_SAE;
        if (c.contains("WPA")) return WiFiSecurity.WPA_PSK;
        if (c.contains("WEP")) return WiFiSecurity.WEP;
        if (c.contains("EAP")) return WiFiSecurity.EAP;
        if (c.contains("ESS")) return WiFiSecurity.OPEN;
        return WiFiSecurity.UNKNOWN;
    }

    @Override
    public void connect(final String ssid, final String password,
                        final WiFiSecurity security,
                        final WiFiConnectCallback cb) {
        Context ctx = AndroidImplementation.getContext();
        WifiManager wm = wifi();
        if (ctx == null || wm == null) {
            failConnect(cb, "WiFi unavailable");
            return;
        }
        if (Build.VERSION.SDK_INT >= SDK_Q) {
            AndroidWifiPlatformLollipop.connectQ(ctx, ssid, password,
                    security, cb);
        } else {
            connectLegacy(wm, ssid, password, security, cb);
        }
    }

    private static void connectLegacy(WifiManager wm, String ssid,
                                      String password, WiFiSecurity security,
                                      final WiFiConnectCallback cb) {
        try {
            WifiConfiguration cfg = new WifiConfiguration();
            cfg.SSID = "\"" + ssid + "\"";
            if (security == WiFiSecurity.OPEN || password == null) {
                cfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            } else if (security == WiFiSecurity.WEP) {
                cfg.wepKeys[0] = "\"" + password + "\"";
                cfg.wepTxKeyIndex = 0;
                cfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                cfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            } else {
                cfg.preSharedKey = "\"" + password + "\"";
            }
            int id = wm.addNetwork(cfg);
            if (id < 0) { failConnect(cb, "addNetwork failed"); return; }
            wm.disconnect();
            boolean ok = wm.enableNetwork(id, true) && wm.reconnect();
            final boolean done = ok;
            CN.callSerially(new Runnable() {
                @Override public void run() {
                    cb.onConnectResult(done,
                            done ? null : new RuntimeException("Legacy enableNetwork failed"));
                }
            });
        } catch (Throwable t) {
            failConnect(cb, t.getMessage());
        }
    }

    @Override
    public void disconnect(String ssid) {
        if (Build.VERSION.SDK_INT >= SDK_Q) {
            AndroidWifiPlatformLollipop.disconnect(ssid);
        }
    }

    private static boolean checkPermission(String perm) {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ctx.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED;
        }
        return ctx.checkPermission(perm, android.os.Process.myPid(),
                android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }

    private static void fail(final WiFiScanCallback cb, final String msg) {
        CN.callSerially(new Runnable() {
            @Override public void run() {
                cb.onScanComplete(null, new RuntimeException(msg));
            }
        });
    }

    static void failConnect(final WiFiConnectCallback cb, final String msg) {
        if (cb == null) return;
        CN.callSerially(new Runnable() {
            @Override public void run() {
                cb.onConnectResult(false, new RuntimeException(msg));
            }
        });
    }
}
