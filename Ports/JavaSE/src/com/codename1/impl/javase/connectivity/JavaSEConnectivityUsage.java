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

import java.util.HashSet;
import java.util.Set;

/// Tracks which connectivity APIs the running simulator app has touched
/// and prints a one-shot reminder of the permissions / entitlements
/// production builds need. A JVM shutdown hook summarises everything used
/// during the run.
public final class JavaSEConnectivityUsage {
    private static final Set<String> usedApis = new HashSet<String>();
    private static volatile boolean shutdownHookInstalled;

    private JavaSEConnectivityUsage() {
    }

    public static void noteUsage(String api) {
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
            System.out.println("  android: ACCESS_WIFI_STATE, ACCESS_NETWORK_STATE, ACCESS_FINE_LOCATION (injected automatically)");
            System.out.println("  ios: com.apple.developer.networking.wifi-info entitlement + NSLocationWhenInUseUsageDescription (injected automatically)");
        } else if ("WiFi.scan".equals(api)) {
            System.out.println("  android: ACCESS_WIFI_STATE, CHANGE_WIFI_STATE, ACCESS_FINE_LOCATION, NEARBY_WIFI_DEVICES (injected automatically)");
            System.out.println("  ios: not supported");
        } else if ("WiFi.connect".equals(api)) {
            System.out.println("  android: CHANGE_NETWORK_STATE, CHANGE_WIFI_STATE, ACCESS_WIFI_STATE (injected automatically)");
            System.out.println("  ios: com.apple.developer.networking.HotspotConfiguration entitlement (injected automatically)");
        } else if ("Bonjour".equals(api)) {
            System.out.println("  android: CHANGE_WIFI_MULTICAST_STATE (injected automatically)");
            System.out.println("  ios: NSLocalNetworkUsageDescription + NSBonjourServices in Info.plist (injected automatically)");
        } else if ("WiFiDirect".equals(api)) {
            System.out.println("  android: CHANGE_WIFI_STATE, ACCESS_FINE_LOCATION, NEARBY_WIFI_DEVICES (injected automatically)");
            System.out.println("  ios: not supported");
        } else if ("Usb".equals(api)) {
            System.out.println("  android: USB host feature (injected automatically); see Network-Connectivity.asciidoc for device_filter.xml");
            System.out.println("  ios: not supported");
        }
    }

    private static void installShutdownHook() {
        if (shutdownHookInstalled) return;
        synchronized (JavaSEConnectivityUsage.class) {
            if (shutdownHookInstalled) return;
            shutdownHookInstalled = true;
            try {
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    @Override public void run() {
                        printSummary();
                    }
                }, "CN1-connectivity-summary"));
            } catch (Throwable ignored) { }
        }
    }

    public static void printSummary() {
        synchronized (usedApis) {
            if (usedApis.isEmpty()) return;
            System.out.println("[CN1 simulator] Connectivity APIs used this session: " + usedApis);
        }
    }
}
