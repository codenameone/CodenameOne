/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.io.wifi;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the no-op {@link WifiPlatform} base class returned on platforms
 * without WiFi support: capability flags are false, info getters are null, and
 * the scan/connect callbacks receive an {@code UnsupportedOperationException}.
 * The base class is instantiated directly (no Display needed).
 */
class WifiPlatformTest {

    @Test
    void capabilitiesAreUnsupported() {
        WifiPlatform p = new WifiPlatform();
        assertFalse(p.isInfoSupported());
        assertFalse(p.isManagementSupported());
    }

    @Test
    void infoGettersReturnNull() {
        WifiPlatform p = new WifiPlatform();
        assertNull(p.getCurrentSSID());
        assertNull(p.getBSSID());
        assertNull(p.getGateway());
        assertNull(p.getIp());
    }

    @Test
    void scanReportsUnsupportedToCallback() {
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        final AtomicReference<WiFiNetwork[]> nets = new AtomicReference<WiFiNetwork[]>();
        new WifiPlatform().scan(new WiFiScanCallback() {
            public void onScanComplete(WiFiNetwork[] networks, Throwable error) {
                nets.set(networks);
                err.set(error);
            }
        });
        assertNull(nets.get());
        assertTrue(err.get() instanceof UnsupportedOperationException);
    }

    @Test
    void connectReportsUnsupportedToCallback() {
        final AtomicReference<Boolean> connected = new AtomicReference<Boolean>();
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        new WifiPlatform().connect("ssid", "pw", WiFiSecurity.WPA_PSK, new WiFiConnectCallback() {
            public void onConnectResult(boolean ok, Throwable error) {
                connected.set(ok);
                err.set(error);
            }
        });
        assertEquals(Boolean.FALSE, connected.get());
        assertTrue(err.get() instanceof UnsupportedOperationException);
    }

    @Test
    void scanConnectAndDisconnectTolerateNullCallbacks() {
        WifiPlatform p = new WifiPlatform();
        p.scan(null);
        p.connect("ssid", null, WiFiSecurity.OPEN, null);
        p.disconnect("ssid");
    }
}
