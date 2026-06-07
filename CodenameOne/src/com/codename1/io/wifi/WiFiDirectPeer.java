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

/// One peer discovered by `WiFiDirect.startDiscovery(...)`. Immutable.
public final class WiFiDirectPeer {
    /// Discovery state: peer has not been contacted.
    public static final int STATE_AVAILABLE = 0;
    /// Discovery state: pairing in progress.
    public static final int STATE_INVITED = 1;
    /// Discovery state: pairing established.
    public static final int STATE_CONNECTED = 2;
    /// Discovery state: peer transitioned out of range or refused pairing.
    public static final int STATE_FAILED = 3;
    /// Discovery state: peer no longer responding to discovery probes.
    public static final int STATE_UNAVAILABLE = 4;

    private final String deviceName;
    private final String deviceAddress;
    private final int state;

    public WiFiDirectPeer(String deviceName, String deviceAddress, int state) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
        this.state = state;
    }

    /// User-visible device name (e.g. "Pixel 9").
    public String getDeviceName() {
        return deviceName;
    }

    /// Stable peer identifier (MAC address on Android).
    public String getDeviceAddress() {
        return deviceAddress;
    }

    /// One of the `STATE_*` constants.
    public int getState() {
        return state;
    }
}
