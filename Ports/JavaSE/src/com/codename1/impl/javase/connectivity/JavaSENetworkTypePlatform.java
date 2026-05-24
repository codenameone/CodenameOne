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

import com.codename1.io.NetworkManager;
import com.codename1.io.NetworkTypePlatform;

import java.net.NetworkInterface;
import java.util.Locale;

/// Network-type tracker for the JavaSE simulator. Derives the active
/// network class from `NetworkInterface.getDisplayName` heuristics. There
/// is no transition listener -- the simulator never synthesizes type
/// changes.
public final class JavaSENetworkTypePlatform extends NetworkTypePlatform {
    @Override
    public int getCurrentNetworkType() {
        try {
            NetworkInterface ni = JavaSEWifiPlatform.primaryInterface();
            if (ni == null) return NetworkManager.NETWORK_TYPE_NONE;
            String name = ni.getName() == null ? "" : ni.getName().toLowerCase(Locale.US);
            String display = ni.getDisplayName() == null ? ""
                    : ni.getDisplayName().toLowerCase(Locale.US);
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
}
