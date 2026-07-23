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

import com.codename1.bluetooth.classic.BluetoothClassic;
import com.codename1.bluetooth.classic.ClassicDiscovery;
import com.codename1.bluetooth.le.BleScan;
import com.codename1.bluetooth.le.BluetoothLE;
import com.codename1.bluetooth.le.ScanSettings;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import static com.codename1.bluetooth.BtTestUtil.assertFailedWith;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The no-op contract of the fallback base classes returned on ports without
 * Bluetooth: {@code TestCodenameOneImplementation.getBluetooth()} returns
 * {@code null} by default, so {@link Bluetooth#getInstance()} must substitute
 * a stable non-null instance whose capability queries are all {@code false}
 * and whose operations fail fast with {@link BluetoothError#NOT_SUPPORTED}.
 */
class BluetoothFallbackTest extends UITestBase {

    @Test
    void getInstanceIsNeverNullAndStableWhenPortHasNoBluetooth() {
        assertNull(display.getBluetooth(),
                "test impl should report no port Bluetooth by default");
        Bluetooth a = Bluetooth.getInstance();
        Bluetooth b = Bluetooth.getInstance();
        assertNotNull(a);
        assertSame(a, b);
    }

    @Test
    void fallbackCapabilityQueriesAreAllFalse() {
        Bluetooth bt = Bluetooth.getInstance();
        assertFalse(bt.isSupported());
        assertFalse(bt.isLeSupported());
        assertFalse(bt.isClassicSupported());
        assertFalse(bt.isPeripheralModeSupported());
        assertFalse(bt.isL2capSupported());
        assertFalse(bt.hasPermission(BluetoothPermission.SCAN));
        assertEquals(AdapterState.UNSUPPORTED, bt.getAdapterState());
        assertFalse(bt.isEnabled());
    }

    @Test
    void roleEntryPointsAreNeverNullAndStable() {
        Bluetooth bt = Bluetooth.getInstance();
        BluetoothLE le = bt.getLE();
        BluetoothClassic classic = bt.getClassic();
        assertNotNull(le);
        assertNotNull(classic);
        assertSame(le, bt.getLE());
        assertSame(classic, bt.getClassic());
    }

    @Test
    void fallbackStartScanReturnsAnAlreadyFailedHandle() {
        BleScan scan = Bluetooth.getInstance().getLE()
                .startScan(new ScanSettings(), result -> fail(
                        "fallback scan must never deliver results"));
        assertNotNull(scan);
        assertFalse(scan.isActive());
        assertFailedWith(scan, BluetoothError.NOT_SUPPORTED);
    }

    @Test
    void fallbackLeQueriesAreEmptyAndPeripheralRoleFailsNotSupported() {
        BluetoothLE le = Bluetooth.getInstance().getLE();
        assertNull(le.getPeripheral("00:11:22:33:44:55"));
        assertTrue(le.getConnectedPeripherals(null).isEmpty());
        assertTrue(le.getBondedPeripherals().isEmpty());
        assertFailedWith(le.openGattServer(null),
                BluetoothError.NOT_SUPPORTED);
        assertFailedWith(le.startAdvertising(null, null, null),
                BluetoothError.NOT_SUPPORTED);
        assertFailedWith(le.openL2capServer(false),
                BluetoothError.NOT_SUPPORTED);
    }

    @Test
    void fallbackClassicOperationsFailNotSupported() {
        BluetoothClassic classic = Bluetooth.getInstance().getClassic();
        ClassicDiscovery d = classic.startDiscovery(result -> fail(
                "fallback discovery must never deliver results"));
        assertNotNull(d);
        assertFailedWith(d, BluetoothError.NOT_SUPPORTED);
        assertTrue(classic.getBondedDevices().isEmpty());
        assertFailedWith(classic.createBond(null),
                BluetoothError.NOT_SUPPORTED);
        assertFailedWith(classic.connect("00:11:22:33:44:55",
                BluetoothUuid.SPP, true), BluetoothError.NOT_SUPPORTED);
        assertFailedWith(classic.listen("svc", BluetoothUuid.SPP, true),
                BluetoothError.NOT_SUPPORTED);
        assertEquals(Boolean.FALSE, classic.requestDiscoverable(120).get());
    }

    @Test
    void fallbackRequestEnableAndPermissionsResolveFalseRatherThanFail() {
        Bluetooth bt = Bluetooth.getInstance();
        assertEquals(Boolean.FALSE, bt.requestEnable().get());
        assertEquals(Boolean.FALSE, bt.requestPermissions(
                BluetoothPermission.SCAN, BluetoothPermission.CONNECT).get());
    }

    @Test
    void fallbackAdapterListenerRegistrationIsANoOp() {
        Bluetooth bt = Bluetooth.getInstance();
        AdapterStateListener l = state -> fail(
                "fallback never fires adapter changes");
        // none of these should throw, including null tolerance
        bt.addAdapterStateListener(l);
        bt.removeAdapterStateListener(l);
        bt.addAdapterStateListener(null);
        bt.removeAdapterStateListener(null);
    }
}
