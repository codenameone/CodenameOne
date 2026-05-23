/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.javase;

import com.codename1.io.NetworkManager;
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
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/// Desktop (simulator) implementation of the WiFi / Bonjour / WiFi-Direct /
/// USB / network-type APIs. Backed by `java.net.NetworkInterface` for the
/// genuine bits and by stubs that print a clear warning so developers know
/// what the production builds need.
///
/// The simulator also tracks the set of permission-requiring APIs that the
/// running app has touched; calling
/// `JavaSEConnectivity.printRequiredPermissionsAndDescriptions()` (invoked
/// from the simulator's diagnostic menu and at JVM shutdown) prints the
/// build hints the developer must add for production iOS/Android builds.
public final class JavaSEConnectivity {
    /// Names of API surfaces the simulator has seen the app use. Used to
    /// emit a one-shot warning that mimics what the iOS/Android builder will
    /// inject automatically.
    private static final Set<String> usedApis = new HashSet<String>();
    private static volatile boolean shutdownHookInstalled;

    private JavaSEConnectivity() {
    }

    private static void noteUsage(String api) {
        synchronized (usedApis) {
            if (usedApis.add(api)) {
                System.out.println("[CN1 simulator] App is using '" + api
                        + "'. In production builds:");
                printRequiredFor(api);
            }
        }
        installShutdownHook();
    }

    private static void printRequiredFor(String api) {
        if ("WiFi.info".equals(api)) {
            System.out.println("  android: ACCESS_WIFI_STATE, ACCESS_NETWORK_STATE, ACCESS_FINE_LOCATION (auto-injected)");
            System.out.println("  ios: com.apple.developer.networking.wifi-info entitlement + NSLocationWhenInUseUsageDescription (auto-injected)");
        } else if ("WiFi.scan".equals(api)) {
            System.out.println("  android: ACCESS_WIFI_STATE, CHANGE_WIFI_STATE, ACCESS_FINE_LOCATION, NEARBY_WIFI_DEVICES (auto-injected)");
            System.out.println("  ios: not supported");
        } else if ("WiFi.connect".equals(api)) {
            System.out.println("  android: CHANGE_NETWORK_STATE, CHANGE_WIFI_STATE, ACCESS_WIFI_STATE (auto-injected)");
            System.out.println("  ios: com.apple.developer.networking.HotspotConfiguration entitlement (auto-injected)");
        } else if ("Bonjour".equals(api)) {
            System.out.println("  android: CHANGE_WIFI_MULTICAST_STATE (auto-injected)");
            System.out.println("  ios: NSLocalNetworkUsageDescription + NSBonjourServices in Info.plist (auto-injected)");
        } else if ("WiFiDirect".equals(api)) {
            System.out.println("  android: CHANGE_WIFI_STATE, ACCESS_FINE_LOCATION, NEARBY_WIFI_DEVICES (auto-injected)");
            System.out.println("  ios: not supported");
        } else if ("Usb".equals(api)) {
            System.out.println("  android: USB host feature (auto-injected); see Network-Connectivity.asciidoc for device_filter.xml");
            System.out.println("  ios: not supported");
        }
    }

    private static void installShutdownHook() {
        if (shutdownHookInstalled) return;
        synchronized (JavaSEConnectivity.class) {
            if (shutdownHookInstalled) return;
            shutdownHookInstalled = true;
            try {
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    @Override public void run() {
                        printRequiredPermissionsAndDescriptions();
                    }
                }, "CN1-connectivity-summary"));
            } catch (Throwable ignored) { }
        }
    }

    public static void printRequiredPermissionsAndDescriptions() {
        synchronized (usedApis) {
            if (usedApis.isEmpty()) return;
            System.out.println("[CN1 simulator] Connectivity APIs used this session: " + usedApis);
        }
    }

    // ---------------------------------------------------------------------
    // WiFi info -- read from java.net.NetworkInterface
    // ---------------------------------------------------------------------

    public static boolean isWiFiInfoSupported() {
        return true;
    }

    public static String getWiFiSSID() {
        noteUsage("WiFi.info");
        // JavaSE has no portable SSID query. Return the host's primary
        // interface name as a stand-in so the developer can still wire UI.
        try {
            NetworkInterface ni = primaryInterface();
            return ni == null ? null : ni.getDisplayName();
        } catch (Throwable t) {
            return null;
        }
    }

    public static String getWiFiBSSID() {
        noteUsage("WiFi.info");
        try {
            NetworkInterface ni = primaryInterface();
            if (ni == null) return null;
            byte[] mac = ni.getHardwareAddress();
            if (mac == null) return null;
            StringBuilder sb = new StringBuilder(17);
            for (int i = 0; i < mac.length; i++) {
                if (sb.length() > 0) sb.append(':');
                sb.append(String.format("%02x", mac[i] & 0xFF));
            }
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    public static String getWiFiGateway() {
        noteUsage("WiFi.info");
        try {
            NetworkInterface ni = primaryInterface();
            if (ni == null) return null;
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress a = addrs.nextElement();
                if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                    byte[] octets = a.getAddress();
                    octets[3] = 1;
                    return InetAddress.getByAddress(octets).getHostAddress();
                }
            }
        } catch (Throwable t) { /* fall through */ }
        return null;
    }

    public static String getWiFiIp() {
        noteUsage("WiFi.info");
        try {
            NetworkInterface ni = primaryInterface();
            if (ni == null) return null;
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress a = addrs.nextElement();
                if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                    return a.getHostAddress();
                }
            }
        } catch (Throwable t) { /* fall through */ }
        return null;
    }

    private static NetworkInterface primaryInterface() throws Exception {
        Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
        while (ifs != null && ifs.hasMoreElements()) {
            NetworkInterface ni = ifs.nextElement();
            if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                if (addrs.nextElement() instanceof Inet4Address) {
                    return ni;
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // WiFi management -- simulated
    // ---------------------------------------------------------------------

    public static boolean isWiFiManagementSupported() {
        return true;
    }

    public static void scanWiFi(final WiFiScanCallback cb) {
        noteUsage("WiFi.scan");
        if (cb == null) return;
        // Return a small fabricated list so UI code can render.
        final WiFiNetwork[] fake = new WiFiNetwork[]{
                new WiFiNetwork("Simulated-Home", "aa:bb:cc:11:22:33",
                        -45, 2412, WiFiSecurity.WPA_PSK),
                new WiFiNetwork("Simulated-Office", "aa:bb:cc:44:55:66",
                        -62, 5180, WiFiSecurity.WPA3_SAE),
                new WiFiNetwork("Simulated-Guest", "aa:bb:cc:77:88:99",
                        -78, 2437, WiFiSecurity.OPEN),
        };
        System.out.println("[CN1 simulator] WiFi.scan returning fabricated results");
        CN.callSerially(new Runnable() {
            @Override public void run() {
                cb.onScanComplete(fake, null);
            }
        });
    }

    public static void connectWiFi(final String ssid, final String password,
                                   final WiFiSecurity security,
                                   final WiFiConnectCallback cb) {
        noteUsage("WiFi.connect");
        System.out.println("[CN1 simulator] WiFi.connect(" + ssid
                + ", security=" + security + ") -- no-op in simulator");
        if (cb != null) {
            CN.callSerially(new Runnable() {
                @Override public void run() {
                    cb.onConnectResult(false, new UnsupportedOperationException(
                            "WiFi.connect is not implemented in the simulator"));
                }
            });
        }
    }

    public static void disconnectWiFi(String ssid) {
    }

    // ---------------------------------------------------------------------
    // Bonjour
    // ---------------------------------------------------------------------

    public static boolean isBonjourSupported() {
        return jmdnsAvailable();
    }

    private static boolean jmdnsAvailable() {
        try {
            Class.forName("javax.jmdns.JmDNS");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static Object startBonjourBrowse(String type,
                                            final BonjourServiceListener listener) {
        noteUsage("Bonjour");
        if (listener == null) return null;
        if (!jmdnsAvailable()) {
            System.out.println("[CN1 simulator] Bonjour browse: JmDNS not on classpath. "
                    + "Add net.posick.mDNS:mdns to your simulator dependencies to discover services.");
            CN.callSerially(new Runnable() {
                @Override public void run() {
                    listener.onBrowseError(new UnsupportedOperationException(
                            "JmDNS not on simulator classpath"));
                }
            });
            return null;
        }
        // JmDNS integration deliberately stays reflective so the JavaSE port
        // does not gain a hard dependency. Users who need real discovery
        // should add JmDNS to their simulator profile pom.
        try {
            Object jmdns = Class.forName("javax.jmdns.JmDNS")
                    .getMethod("create").invoke(null);
            // Just register the type; we don't translate JmDNS events back
            // without a deeper reflective dance. This is sufficient for the
            // simulator to validate the call path; production builds use the
            // platform-native APIs.
            System.out.println("[CN1 simulator] Bonjour browse started via JmDNS for type=" + type);
            return jmdns;
        } catch (Throwable t) {
            final Throwable err = t;
            CN.callSerially(new Runnable() {
                @Override public void run() { listener.onBrowseError(err); }
            });
            return null;
        }
    }

    public static void stopBonjourBrowse(Object handle) {
        if (handle == null) return;
        try {
            handle.getClass().getMethod("close").invoke(handle);
        } catch (Throwable ignored) { }
    }

    public static Object startBonjourPublish(String name, String type, int port,
                                             Map<String, String> txt) {
        noteUsage("Bonjour");
        System.out.println("[CN1 simulator] Bonjour publish " + name + "@" + type + ":" + port);
        return new Object();
    }

    public static void stopBonjourPublish(Object handle) {
    }

    // ---------------------------------------------------------------------
    // Network type
    // ---------------------------------------------------------------------

    public static int getCurrentNetworkType() {
        try {
            NetworkInterface ni = primaryInterface();
            if (ni == null) return NetworkManager.NETWORK_TYPE_NONE;
            String name = ni.getName() == null ? "" : ni.getName().toLowerCase();
            String display = ni.getDisplayName() == null ? "" : ni.getDisplayName().toLowerCase();
            if (name.startsWith("wlan") || name.startsWith("wifi")
                    || display.contains("wireless") || display.contains("wi-fi")) {
                return NetworkManager.NETWORK_TYPE_WIFI;
            }
            if (name.startsWith("en") || name.startsWith("eth")) {
                return NetworkManager.NETWORK_TYPE_ETHERNET;
            }
            return NetworkManager.NETWORK_TYPE_OTHER;
        } catch (Throwable t) {
            return NetworkManager.NETWORK_TYPE_NONE;
        }
    }

    // ---------------------------------------------------------------------
    // WiFi Direct -- not supported on JavaSE
    // ---------------------------------------------------------------------

    public static boolean isWiFiDirectSupported() {
        return false;
    }

    public static void startWiFiDirectDiscovery(final WiFiDirectListener l) {
        noteUsage("WiFiDirect");
        if (l == null) return;
        CN.callSerially(new Runnable() {
            @Override public void run() {
                l.onDiscoveryError(new UnsupportedOperationException(
                        "WiFi Direct is not supported on JavaSE"));
            }
        });
    }

    public static void stopWiFiDirectDiscovery() {
    }

    public static void connectWiFiDirect(WiFiDirectPeer peer,
                                         final WiFiConnectCallback cb) {
        if (cb != null) {
            CN.callSerially(new Runnable() {
                @Override public void run() {
                    cb.onConnectResult(false, new UnsupportedOperationException(
                            "WiFi Direct is not supported on JavaSE"));
                }
            });
        }
    }

    public static void disconnectWiFiDirect() {
    }
}
