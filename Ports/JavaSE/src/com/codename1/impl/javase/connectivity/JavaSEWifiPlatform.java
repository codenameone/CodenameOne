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
import com.codename1.io.wifi.WiFiNetwork;
import com.codename1.io.wifi.WiFiScanCallback;
import com.codename1.io.wifi.WiFiSecurity;
import com.codename1.io.wifi.WifiPlatform;
import com.codename1.ui.CN;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;

/// Best-effort WiFi platform for the JavaSE simulator. SSID/BSSID come
/// from `NetworkInterface`; scan returns a small fabricated list; connect
/// reports an unsupported error and logs the call.
public final class JavaSEWifiPlatform extends WifiPlatform {
    @Override
    public boolean isInfoSupported() { return true; }

    @Override
    public boolean isManagementSupported() { return true; }

    @Override
    public String getCurrentSSID() {
        JavaSEConnectivityUsage.noteUsage("WiFi.info");
        try {
            NetworkInterface ni = primaryInterface();
            return ni == null ? null : ni.getDisplayName();
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public String getBSSID() {
        JavaSEConnectivityUsage.noteUsage("WiFi.info");
        try {
            NetworkInterface ni = primaryInterface();
            if (ni == null) return null;
            byte[] mac = ni.getHardwareAddress();
            if (mac == null) return null;
            StringBuilder sb = new StringBuilder(17);
            for (int i = 0; i < mac.length; i++) {
                if (sb.length() > 0) sb.append(':');
                sb.append(String.format(Locale.US, "%02x", mac[i] & 0xFF));
            }
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public String getGateway() {
        JavaSEConnectivityUsage.noteUsage("WiFi.info");
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

    @Override
    public String getIp() {
        JavaSEConnectivityUsage.noteUsage("WiFi.info");
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

    static NetworkInterface primaryInterface() throws Exception {
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

    @Override
    public void scan(final WiFiScanCallback cb) {
        JavaSEConnectivityUsage.noteUsage("WiFi.scan");
        if (cb == null) return;
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

    @Override
    public void connect(final String ssid, final String password,
                        final WiFiSecurity security,
                        final WiFiConnectCallback cb) {
        JavaSEConnectivityUsage.noteUsage("WiFi.connect");
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

    @Override
    public void disconnect(String ssid) {
    }
}
