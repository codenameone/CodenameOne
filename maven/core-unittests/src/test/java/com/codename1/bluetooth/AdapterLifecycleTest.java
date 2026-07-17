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
package com.codename1.bluetooth;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adapter state plumbing on the {@link Bluetooth} facade: listener
 * registration/removal, EDT dispatch of state transitions fired from a
 * port thread, and the {@code isEnabled()} derivation.
 */
class AdapterLifecycleTest extends UITestBase {

    private FakeBluetooth fake;

    @BeforeEach
    void installFake() {
        fake = new FakeBluetooth();
        implementation.setBluetooth(fake);
    }

    @Test
    void stateChangesDispatchToRegisteredListeners() {
        List<AdapterState> seen = new ArrayList<AdapterState>();
        fake.addAdapterStateListener(seen::add);

        fake.changeAdapterState(AdapterState.TURNING_OFF);
        fake.changeAdapterState(AdapterState.POWERED_OFF);
        flushSerialCalls();

        assertEquals(2, seen.size());
        assertEquals(AdapterState.TURNING_OFF, seen.get(0));
        assertEquals(AdapterState.POWERED_OFF, seen.get(1));
    }

    @Test
    void removedListenersNoLongerReceiveStateChanges() {
        List<AdapterState> seen1 = new ArrayList<AdapterState>();
        List<AdapterState> seen2 = new ArrayList<AdapterState>();
        AdapterStateListener l1 = seen1::add;
        fake.addAdapterStateListener(l1);
        fake.addAdapterStateListener(seen2::add);

        fake.changeAdapterState(AdapterState.POWERED_OFF);
        flushSerialCalls();
        assertEquals(1, seen1.size());
        assertEquals(1, seen2.size());

        fake.removeAdapterStateListener(l1);
        fake.changeAdapterState(AdapterState.POWERED_ON);
        flushSerialCalls();
        assertEquals(1, seen1.size(),
                "removed listener must not be notified");
        assertEquals(2, seen2.size());
    }

    @Test
    void duplicateListenerRegistrationNotifiesOnce() {
        List<AdapterState> seen = new ArrayList<AdapterState>();
        AdapterStateListener l = seen::add;
        fake.addAdapterStateListener(l);
        fake.addAdapterStateListener(l);

        fake.changeAdapterState(AdapterState.POWERED_OFF);
        flushSerialCalls();
        assertEquals(1, seen.size());
    }

    @Test
    void nullListenerRegistrationIsIgnored() {
        fake.addAdapterStateListener(null);
        fake.removeAdapterStateListener(null);
        // must not throw when a change fires with no valid listeners
        fake.changeAdapterState(AdapterState.POWERED_OFF);
        flushSerialCalls();
    }

    @Test
    void isEnabledDerivesFromTheAdapterState() {
        fake.setAdapterState(AdapterState.POWERED_ON);
        assertTrue(fake.isEnabled());
        for (AdapterState state : AdapterState.values()) {
            if (state != AdapterState.POWERED_ON) {
                fake.setAdapterState(state);
                assertFalse(fake.isEnabled(),
                        "isEnabled must be false for " + state);
            }
        }
    }

    @Test
    void facadeExposesTheScriptedAdapterState() {
        fake.setAdapterState(AdapterState.TURNING_ON);
        assertEquals(AdapterState.TURNING_ON,
                Bluetooth.getInstance().getAdapterState());
        assertFalse(Bluetooth.getInstance().isEnabled());
        fake.setAdapterState(AdapterState.POWERED_ON);
        assertTrue(Bluetooth.getInstance().isEnabled());
    }

    @Test
    void scriptedPermissionAndEnableFlowsResolveAsConfigured() {
        fake.setPermissionGranted(true).setEnableGranted(true);
        assertTrue(fake.hasPermission(BluetoothPermission.CONNECT));
        assertEquals(Boolean.TRUE, Bluetooth.getInstance()
                .requestPermissions(BluetoothPermission.SCAN).get());
        assertEquals(Boolean.TRUE,
                Bluetooth.getInstance().requestEnable().get());
    }
}
