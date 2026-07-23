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

import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattService;
import com.codename1.bluetooth.le.BleScan;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.bluetooth.le.ScanSettings;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The threading contract: every application-facing callback of the
 * Bluetooth API is delivered on the EDT even though the fakes fire the
 * underlying events from the test thread. Each test records
 * {@code Display.isEdt()} inside the callback, flushes the EDT and asserts
 * both delivery and thread.
 */
class BluetoothEdtTest extends UITestBase {

    private boolean offEdt() {
        return !Display.getInstance().isEdt();
    }

    @Test
    void scanResultsAreDeliveredOnTheEdt() {
        assertTrue(offEdt(), "the test itself must run off the EDT");
        FakeBluetooth fake = new FakeBluetooth();
        implementation.setBluetooth(fake);
        FakeBluetoothLE le = fake.getFakeLE();

        final List<Boolean> onEdt = new ArrayList<Boolean>();
        BleScan scan = le.startScan(new ScanSettings(),
                result -> onEdt.add(Display.getInstance().isEdt()));
        le.injectScanResult(new ScanResult(
                new FakeBlePeripheral("AA:00", "x"), -40, null, true, 0));
        flushSerialCalls();

        assertEquals(1, onEdt.size());
        assertEquals(Boolean.TRUE, onEdt.get(0));
        scan.stop();
    }

    @Test
    void connectionEventsAreDeliveredOnTheEdt() {
        assertTrue(offEdt());
        FakeBlePeripheral p = new FakeBlePeripheral("AA:01", "p");
        final List<Boolean> onEdt = new ArrayList<Boolean>();
        p.addConnectionListener(
                event -> onEdt.add(Display.getInstance().isEdt()));

        p.connectNow();
        flushSerialCalls();

        assertEquals(2, onEdt.size(), "CONNECTING and CONNECTED events");
        assertEquals(Boolean.TRUE, onEdt.get(0));
        assertEquals(Boolean.TRUE, onEdt.get(1));
    }

    @Test
    void notificationsAreDeliveredOnTheEdt() {
        assertTrue(offEdt());
        FakeBlePeripheral p = new FakeBlePeripheral("AA:02", "p");
        GattService s = p.buildService(BluetoothUuid.fromShort(0x180D));
        GattCharacteristic c = p.buildCharacteristic(s,
                BluetoothUuid.fromShort(0x2A37),
                GattCharacteristic.PROPERTY_NOTIFY);
        p.connectNow();
        p.subscribe(c, (chr, value) -> { });

        final List<Boolean> onEdt = new ArrayList<Boolean>();
        p.subscribe(c, (chr, value) -> onEdt.add(
                Display.getInstance().isEdt()));
        p.completeNext(Boolean.TRUE);

        p.notifyValue(c, new byte[] {1});
        flushSerialCalls();

        assertEquals(1, onEdt.size());
        assertEquals(Boolean.TRUE, onEdt.get(0));
    }

    @Test
    void adapterStateChangesAreDeliveredOnTheEdt() {
        assertTrue(offEdt());
        FakeBluetooth fake = new FakeBluetooth();
        implementation.setBluetooth(fake);

        final List<Boolean> onEdt = new ArrayList<Boolean>();
        fake.addAdapterStateListener(
                state -> onEdt.add(Display.getInstance().isEdt()));
        fake.changeAdapterState(AdapterState.POWERED_OFF);
        flushSerialCalls();

        assertEquals(1, onEdt.size());
        assertEquals(Boolean.TRUE, onEdt.get(0));
    }

    @Test
    void asyncResourceCallbacksRegisteredOnTheEdtFireOnTheEdt() {
        assertTrue(offEdt());
        final FakeBlePeripheral p = new FakeBlePeripheral("AA:03", "p");
        p.connectNow();

        final AsyncResource<Integer> r = p.readRssi();
        final List<Boolean> onEdt = new ArrayList<Boolean>();
        final AtomicInteger value = new AtomicInteger();
        // register the callback ON the EDT...
        display.callSeriallyAndWait(() -> r.onResult((v, err) -> {
            onEdt.add(Display.getInstance().isEdt());
            if (v != null) {
                value.set(v.intValue());
            }
        }));
        // ...then complete the operation from the test thread
        p.completeNext(Integer.valueOf(-47));
        flushSerialCalls();

        assertEquals(1, onEdt.size());
        assertEquals(Boolean.TRUE, onEdt.get(0),
                "a callback registered on the EDT must be invoked on the EDT");
        assertEquals(-47, value.get());
    }
}
