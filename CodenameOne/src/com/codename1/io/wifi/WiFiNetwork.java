/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.io.wifi;

/// One entry in a WiFi scan result. Immutable.
public final class WiFiNetwork {
    private final String ssid;
    private final String bssid;
    private final int rssi;
    private final int frequency;
    private final WiFiSecurity security;

    public WiFiNetwork(String ssid, String bssid, int rssi, int frequency,
                       WiFiSecurity security) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.rssi = rssi;
        this.frequency = frequency;
        this.security = security;
    }

    /// Human-readable network name. May be empty for hidden networks.
    public String getSSID() {
        return ssid;
    }

    /// Access point MAC address (colon-separated lowercase hex).
    public String getBSSID() {
        return bssid;
    }

    /// Received signal strength in dBm (negative values; closer to zero is
    /// stronger). Typical range: -30 (excellent) to -90 (unusable).
    public int getRssi() {
        return rssi;
    }

    /// Channel frequency in MHz (e.g. 2412 for channel 1 on 2.4 GHz).
    public int getFrequency() {
        return frequency;
    }

    /// Security mode advertised by the AP. See `WiFiSecurity`.
    public WiFiSecurity getSecurity() {
        return security;
    }
}
