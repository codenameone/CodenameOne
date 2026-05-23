/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.android;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.RouteInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
// android.net.wifi.WifiNetworkSpecifier is API 29+ and the compile-SDK
// vendored in cn1-binaries is API 25. We instantiate it reflectively in
// connectWiFiQ so the build still passes on the legacy SDK while the
// runtime path still works on Android 10+.
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Looper;
import android.os.PatternMatcher;
import android.text.format.Formatter;
import android.util.Log;

import com.codename1.io.NetworkManager;
import com.codename1.io.bonjour.BonjourService;
import com.codename1.io.bonjour.BonjourServiceListener;
import com.codename1.io.wifi.WiFiConnectCallback;
import com.codename1.io.wifi.WiFiDirectListener;
import com.codename1.io.wifi.WiFiDirectPeer;
import com.codename1.io.wifi.WiFiNetwork;
import com.codename1.io.wifi.WiFiScanCallback;
import com.codename1.io.wifi.WiFiSecurity;
import com.codename1.ui.CN;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/// Houses the wifi / mDNS / wifi-direct / usb / network-type machinery for
/// the Android port. AndroidImplementation forwards every new connectivity
/// hook to a static method here so AndroidImplementation.java stays a thin
/// facade. The class deliberately uses the platform Context obtained via
/// AndroidImplementation.getContext(); it does NOT keep its own static
/// reference because the activity context churns across configuration changes.
public final class AndroidConnectivity {
    private static final String TAG = "CN1Connect";

    private AndroidConnectivity() {
    }

    // ---------------------------------------------------------------------
    // Network type tracking
    // ---------------------------------------------------------------------

    private static ConnectivityManager.NetworkCallback networkCallback;
    private static BroadcastReceiver networkReceiver;

    public static int getCurrentNetworkType() {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return NetworkManager.NETWORK_TYPE_NONE;
        ConnectivityManager cm = (ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return NetworkManager.NETWORK_TYPE_NONE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            return NetworkManager.NETWORK_TYPE_NONE;
        }
        switch (info.getType()) {
            case ConnectivityManager.TYPE_WIFI:
                return NetworkManager.NETWORK_TYPE_WIFI;
            case ConnectivityManager.TYPE_MOBILE:
                return NetworkManager.NETWORK_TYPE_CELLULAR;
            case ConnectivityManager.TYPE_ETHERNET:
                return NetworkManager.NETWORK_TYPE_ETHERNET;
            case ConnectivityManager.TYPE_BLUETOOTH:
                return NetworkManager.NETWORK_TYPE_BLUETOOTH;
            default:
                return NetworkManager.NETWORK_TYPE_OTHER;
        }
    }

    public static void installNetworkTypeListener(final NetworkManager target) {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return;
        final ConnectivityManager cm = (ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            NetworkRequest req = new NetworkRequest.Builder().build();
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network n) {
                    dispatchChange(target);
                }
                @Override
                public void onLost(Network n) {
                    dispatchChange(target);
                }
                @Override
                public void onCapabilitiesChanged(Network n, NetworkCapabilities c) {
                    dispatchChange(target);
                }
            };
            try {
                cm.registerNetworkCallback(req, networkCallback);
            } catch (Throwable t) {
                Log.w(TAG, "registerNetworkCallback failed", t);
            }
        } else {
            networkReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent i) {
                    dispatchChange(target);
                }
            };
            ctx.registerReceiver(networkReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    private static void dispatchChange(final NetworkManager target) {
        final int t = getCurrentNetworkType();
        final boolean vpn = target.isVPNActive();
        CN.callSerially(new Runnable() {
            @Override public void run() {
                target.fireNetworkTypeChange(t, vpn);
            }
        });
    }

    public static void uninstallNetworkTypeListener(NetworkManager target) {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return;
        if (networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                ConnectivityManager cm = (ConnectivityManager)
                        ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
                cm.unregisterNetworkCallback(networkCallback);
            } catch (Throwable ignored) { }
            networkCallback = null;
        }
        if (networkReceiver != null) {
            try {
                ctx.unregisterReceiver(networkReceiver);
            } catch (Throwable ignored) { }
            networkReceiver = null;
        }
    }

    // ---------------------------------------------------------------------
    // WiFi information
    // ---------------------------------------------------------------------

    private static WifiManager wifi() {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return null;
        // Use applicationContext explicitly per Android docs to avoid leaking
        // the activity when held by the singleton WifiManager.
        return (WifiManager) ctx.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
    }

    private static ConnectivityManager cm() {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return null;
        return (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static String getWiFiSSID() {
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

    public static String getWiFiBSSID() {
        WifiManager wm = wifi();
        if (wm == null) return null;
        WifiInfo info = wm.getConnectionInfo();
        if (info == null) return null;
        String s = info.getBSSID();
        if (s == null || "02:00:00:00:00:00".equals(s)) {
            return null;
        }
        return s.toLowerCase();
    }

    public static String getWiFiGateway() {
        WifiManager wm = wifi();
        if (wm == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cm() != null) {
            Network n = cm().getActiveNetwork();
            if (n != null) {
                LinkProperties lp = cm().getLinkProperties(n);
                if (lp != null) {
                    for (RouteInfo r : lp.getRoutes()) {
                        if (r.isDefaultRoute() && r.getGateway() instanceof Inet4Address) {
                            return r.getGateway().getHostAddress();
                        }
                    }
                }
            }
        }
        try {
            int g = wm.getDhcpInfo().gateway;
            return Formatter.formatIpAddress(g);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String getWiFiIp() {
        WifiManager wm = wifi();
        if (wm == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cm() != null) {
            Network n = cm().getActiveNetwork();
            if (n != null) {
                LinkProperties lp = cm().getLinkProperties(n);
                if (lp != null) {
                    for (LinkAddress la : lp.getLinkAddresses()) {
                        InetAddress a = la.getAddress();
                        if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                            return a.getHostAddress();
                        }
                    }
                }
            }
        }
        WifiInfo info = wm.getConnectionInfo();
        if (info == null) return null;
        int ip = info.getIpAddress();
        if (ip == 0) return null;
        return Formatter.formatIpAddress(ip);
    }

    // ---------------------------------------------------------------------
    // WiFi scan
    // ---------------------------------------------------------------------

    private static BroadcastReceiver scanReceiver;

    public static void scanWiFi(final WiFiScanCallback cb) {
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
                            r.BSSID != null ? r.BSSID.toLowerCase() : null,
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
        String c = capabilities.toUpperCase();
        if (c.contains("WPA3") || c.contains("SAE")) return WiFiSecurity.WPA3_SAE;
        if (c.contains("WPA")) return WiFiSecurity.WPA_PSK;
        if (c.contains("WEP")) return WiFiSecurity.WEP;
        if (c.contains("EAP")) return WiFiSecurity.EAP;
        if (c.contains("ESS")) return WiFiSecurity.OPEN;
        return WiFiSecurity.UNKNOWN;
    }

    // ---------------------------------------------------------------------
    // WiFi connect
    // ---------------------------------------------------------------------

    private static final Map<String, ConnectivityManager.NetworkCallback> pendingConnects
            = new HashMap<String, ConnectivityManager.NetworkCallback>();

    // SDK_INT thresholds. Build.VERSION_CODES.Q (=29) is not present in the
    // legacy compile SDK, so we use the integer constant directly.
    private static final int SDK_Q = 29;

    public static void connectWiFi(final String ssid, final String password,
                                   final WiFiSecurity security,
                                   final WiFiConnectCallback cb) {
        Context ctx = AndroidImplementation.getContext();
        WifiManager wm = wifi();
        if (ctx == null || wm == null) {
            failConnect(cb, "WiFi unavailable");
            return;
        }
        if (Build.VERSION.SDK_INT >= SDK_Q) {
            connectWiFiQ(ctx, ssid, password, security, cb);
        } else {
            connectWiFiLegacy(wm, ssid, password, security, cb);
        }
    }

    private static void connectWiFiQ(Context ctx, final String ssid,
                                     String password, WiFiSecurity security,
                                     final WiFiConnectCallback cb) {
        // WifiNetworkSpecifier.Builder is API 29+. Reach it reflectively so
        // this file compiles against the legacy android.jar shipped in
        // cn1-binaries while the code path still runs on Android 10+.
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
            failConnect(cb, "WifiNetworkSpecifier not available: " + t);
            return;
        }
        NetworkRequest req = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(spec)
                .build();
        ConnectivityManager cm = (ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network n) {
                CN.callSerially(new Runnable() {
                    @Override public void run() { cb.onConnectResult(true, null); }
                });
            }
            @Override
            public void onUnavailable() {
                CN.callSerially(new Runnable() {
                    @Override public void run() {
                        cb.onConnectResult(false,
                                new RuntimeException("WiFi connect unavailable / rejected"));
                    }
                });
            }
        };
        pendingConnects.put(ssid, callback);
        try {
            cm.requestNetwork(req, callback);
        } catch (Throwable t) {
            failConnect(cb, t.getMessage());
        }
    }

    private static void connectWiFiLegacy(WifiManager wm, String ssid,
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

    public static void disconnectWiFi(String ssid) {
        ConnectivityManager.NetworkCallback cb = pendingConnects.remove(ssid);
        if (cb != null && Build.VERSION.SDK_INT >= SDK_Q) {
            try {
                ConnectivityManager c = cm();
                if (c != null) c.unregisterNetworkCallback(cb);
            } catch (Throwable ignored) { }
        }
    }

    // ---------------------------------------------------------------------
    // Bonjour
    // ---------------------------------------------------------------------

    public static boolean isBonjourSupported() {
        return AndroidImplementation.getContext() != null;
    }

    public static Object startBonjourBrowse(final String typeIn,
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
                CN.callSerially(new Runnable() { @Override public void run() {
                    listener.onBrowseError(new RuntimeException("startDiscovery failed: " + errorCode)); } });
            }
            @Override public void onStopDiscoveryFailed(String s, int errorCode) {
            }
            @Override public void onDiscoveryStarted(String s) {
            }
            @Override public void onDiscoveryStopped(String s) {
            }
            @Override public void onServiceFound(NsdServiceInfo info) {
                nsd.resolveService(info, new NsdManager.ResolveListener() {
                    @Override public void onResolveFailed(NsdServiceInfo info, int errorCode) {
                    }
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
            CN.callSerially(new Runnable() { @Override public void run() {
                listener.onBrowseError(t); } });
            return null;
        }
        Object[] handle = new Object[]{nsd, disc};
        return handle;
    }

    public static void stopBonjourBrowse(Object handle) {
        if (handle == null) return;
        Object[] arr = (Object[]) handle;
        NsdManager nsd = (NsdManager) arr[0];
        NsdManager.DiscoveryListener l = (NsdManager.DiscoveryListener) arr[1];
        try { nsd.stopServiceDiscovery(l); } catch (Throwable ignored) { }
    }

    public static Object startBonjourPublish(String name, String type, int port,
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

    public static void stopBonjourPublish(Object handle) {
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
                txt.put(e.getKey(), e.getValue() == null ? "" : new String(e.getValue()));
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

    // ---------------------------------------------------------------------
    // WiFi Direct (Wi-Fi P2P)
    // ---------------------------------------------------------------------

    private static WifiP2pManager p2pManager;
    private static WifiP2pManager.Channel p2pChannel;
    private static BroadcastReceiver p2pReceiver;
    private static WiFiDirectListener p2pListener;

    public static boolean isWiFiDirectSupported() {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return false;
        return ctx.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT);
    }

    public static void startWiFiDirectDiscovery(final WiFiDirectListener listener) {
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
                CN.callSerially(new Runnable() {
                    @Override public void run() {
                        listener.onDiscoveryError(
                                new RuntimeException("discoverPeers failed: " + reason));
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

    public static void stopWiFiDirectDiscovery() {
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

    public static void connectWiFiDirect(WiFiDirectPeer peer,
                                         final WiFiConnectCallback cb) {
        if (p2pManager == null || p2pChannel == null) {
            failConnect(cb, "WiFi Direct discovery not started");
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
                CN.callSerially(new Runnable() {
                    @Override public void run() {
                        if (cb != null) cb.onConnectResult(false,
                                new RuntimeException("connect failed: " + reason));
                    }
                });
            }
        });
    }

    public static void disconnectWiFiDirect() {
        if (p2pManager != null && p2pChannel != null) {
            try { p2pManager.removeGroup(p2pChannel, null); } catch (Throwable ignored) { }
        }
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private static boolean checkPermission(String perm) {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return false;
        return ctx.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED;
    }

    private static void fail(final WiFiScanCallback cb, final String msg) {
        CN.callSerially(new Runnable() {
            @Override public void run() {
                cb.onScanComplete(null, new RuntimeException(msg));
            }
        });
    }

    private static void failConnect(final WiFiConnectCallback cb, final String msg) {
        if (cb == null) return;
        CN.callSerially(new Runnable() {
            @Override public void run() {
                cb.onConnectResult(false, new RuntimeException(msg));
            }
        });
    }
}
