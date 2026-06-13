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

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the static {@link WiFi} facade against the no-op
 * {@link WifiPlatform} the test platform supplies: capability probes, the null
 * info getters, and the scan/connect/disconnect delegation (the callbacks
 * report the platform as unsupported).
 */
class WiFiTest extends UITestBase {

    @Test
    void capabilitiesAreUnsupportedOnTestPlatform() {
        assertFalse(WiFi.isInfoSupported());
        assertFalse(WiFi.isManagementSupported());
    }

    @Test
    void infoGettersDelegateAndReturnNull() {
        assertNull(WiFi.getCurrentSSID());
        assertNull(WiFi.getBSSID());
        assertNull(WiFi.getGateway());
        assertNull(WiFi.getIp());
    }

    @Test
    void scanDelegatesAndReportsUnsupported() {
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        WiFi.scan(new WiFiScanCallback() {
            public void onScanComplete(WiFiNetwork[] networks, Throwable error) {
                err.set(error);
            }
        });
        assertTrue(err.get() instanceof UnsupportedOperationException);
    }

    @Test
    void connectDelegatesAndReportsUnsupported() {
        final AtomicReference<Boolean> ok = new AtomicReference<Boolean>();
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        WiFi.connect("ssid", "pw", WiFiSecurity.WPA3_SAE, new WiFiConnectCallback() {
            public void onConnectResult(boolean connected, Throwable error) {
                ok.set(connected);
                err.set(error);
            }
        });
        assertEquals(Boolean.FALSE, ok.get());
        assertTrue(err.get() instanceof UnsupportedOperationException);
    }

    @Test
    void disconnectDelegatesWithoutThrowing() {
        WiFi.disconnect("ssid");
    }
}
