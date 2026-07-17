/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase.bluetooth;

import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.CharacteristicRecord;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.Device;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.ServiceRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Drives {@link FixtureRecorder} against a real subprocess running the
 * scripted {@link FakeBleHelper} (the {@link NativeBleBackendFakeHelperTest}
 * pattern), asserting that the recorded fixture reflects the scripted
 * sightings and GATT database, that scrambling applies, and that the
 * recorded trace replays through the full deterministic stack.
 */
public class FixtureRecorderTest {

    private static final BluetoothUuid HR_SERVICE =
            BluetoothUuid.fromString(FakeBleHelper.HR_SERVICE);
    private static final BluetoothUuid HR_MEASUREMENT =
            BluetoothUuid.fromString(FakeBleHelper.HR_MEASUREMENT);
    private static final BluetoothUuid HR_CONTROL =
            BluetoothUuid.fromString(FakeBleHelper.HR_CONTROL);
    private static final BluetoothUuid CCCD =
            BluetoothUuid.fromString(FakeBleHelper.CCCD);

    private NativeBleBackend backend;

    @AfterEach
    void tearDown() {
        if (backend != null) {
            backend.shutdown();
            backend = null;
        }
    }

    private FixtureRecorder recorder(String scenario) {
        backend = new NativeBleBackend(
                NativeBleBackendFakeHelperTest.fakeHelperCommand(scenario));
        return new FixtureRecorder(backend);
    }

    @Test
    public void recordsScriptedSightingsAndGattDatabase()
            throws BluetoothException {
        BluetoothFixture fixture = recorder("happy").record(500,
                Arrays.asList("aa:01"), false);

        Assertions.assertEquals(2, fixture.getDevices().size());
        Device monitor = fixture.getDevice("aa:01");
        Assertions.assertNotNull(monitor);
        Assertions.assertEquals("Heart Monitor", monitor.name);
        Assertions.assertFalse(monitor.rssiTimeline.isEmpty());
        Assertions.assertEquals(-42, monitor.rssiTimeline.get(0).rssi);
        Assertions.assertEquals(Integer.valueOf(4), monitor.txPower);
        Assertions.assertEquals(Collections.singletonList(HR_SERVICE),
                monitor.serviceUuids);
        Assertions.assertArrayEquals(new byte[] {1, 2},
                monitor.manufacturerData.get(Integer.valueOf(76)));
        Assertions.assertTrue(monitor.serviceData.isEmpty());

        Device thermometer = fixture.getDevice("aa:02");
        Assertions.assertNotNull(thermometer);
        Assertions.assertEquals("Thermometer", thermometer.name);
        Assertions.assertEquals(-77, thermometer.rssiTimeline.get(0).rssi);
        Assertions.assertNull(thermometer.txPower);
        Assertions.assertFalse(thermometer.hasGatt(),
                "no GATT capture was requested for aa:02");

        // the captured GATT database mirrors the scripted discovery
        Assertions.assertTrue(monitor.hasGatt());
        Assertions.assertEquals(1, monitor.gatt.size());
        ServiceRecord hr = monitor.gatt.get(0);
        Assertions.assertEquals(HR_SERVICE, hr.uuid);
        Assertions.assertTrue(hr.primary);
        Assertions.assertEquals(2, hr.characteristics.size());
        CharacteristicRecord measurement = hr.characteristics.get(0);
        Assertions.assertEquals(HR_MEASUREMENT, measurement.uuid);
        Assertions.assertEquals(GattCharacteristic.PROPERTY_READ
                | GattCharacteristic.PROPERTY_NOTIFY,
                measurement.properties);
        // the readable characteristic's value was captured (fake reads
        // answer base64 "AQI=" == {1, 2})
        Assertions.assertArrayEquals(new byte[] {1, 2}, measurement.value);
        Assertions.assertEquals(1, measurement.descriptors.size());
        Assertions.assertEquals(CCCD,
                measurement.descriptors.get(0).uuid);
        CharacteristicRecord control = hr.characteristics.get(1);
        Assertions.assertEquals(HR_CONTROL, control.uuid);
        Assertions.assertEquals(GattCharacteristic.PROPERTY_WRITE,
                control.properties);
        Assertions.assertNull(control.value,
                "write-only characteristics have no captured value");

        Assertions.assertNotNull(fixture.getPlatform());
    }

    @Test
    public void recordScrambledAppliesTheScrambler()
            throws BluetoothException {
        FixtureRecorder recorder = recorder("happy");
        BluetoothFixture raw = recorder.record(500,
                Arrays.asList("aa:01"), false);
        BluetoothFixture scrambled = FixtureScrambler.scramble(raw, 42);

        Assertions.assertEquals(raw.getDevices().size(),
                scrambled.getDevices().size());
        Device monitor = scrambled.getDevices().get(0);
        Assertions.assertTrue(monitor.id.startsWith("SC:RA:MB:"),
                monitor.id);
        Assertions.assertNotEquals("Heart Monitor", monitor.name);
        Assertions.assertTrue(monitor.name.startsWith("Device-"));
        // company id kept, payload length preserved
        Assertions.assertEquals(2, monitor.manufacturerData
                .get(Integer.valueOf(76)).length);
        // SIG uuids kept
        Assertions.assertEquals(HR_SERVICE, monitor.serviceUuids.get(0));
        Assertions.assertEquals(HR_SERVICE, monitor.gatt.get(0).uuid);
        // captured value scrambled length-preserving
        Assertions.assertEquals(2,
                monitor.gatt.get(0).characteristics.get(0).value.length);
        Assertions.assertEquals(Collections.emptyList(),
                FixtureScrambler.findLeaks(raw, scrambled));
        // recordScrambled is exactly record + scramble under the seed
        Assertions.assertEquals(scrambled.toJson(), FixtureScrambler
                .scramble(raw, 42).toJson());
    }

    @Test
    public void recordedTraceReplaysThroughTheFullStack()
            throws BluetoothException {
        // record from the fake radio, scramble, then replay the fixture
        // into a deterministic ManualScheduler stack and walk the GATT
        // database through the core API -- the full record->replay loop
        BluetoothFixture fixture = recorder("happy").recordScrambled(500,
                Arrays.asList("aa:01"), false, 42);
        RecordedTraceReplayTest.replayFixture(fixture);
    }

    @Test
    public void gattStrongestPicksTheStrongestConnectableDevice()
            throws BluetoothException {
        // aa:01 advertises at -42, aa:02 at -77: strongest wins
        BluetoothFixture fixture = recorder("happy").record(500, null,
                true);
        Device monitor = fixture.getDevice("aa:01");
        Assertions.assertNotNull(monitor);
        Assertions.assertTrue(monitor.hasGatt());
        Assertions.assertFalse(fixture.getDevice("aa:02").hasGatt());
    }

    @Test
    public void gattFailureDegradesToScanOnly() throws BluetoothException {
        // the crash-on-connect helper dies when the GATT walk starts:
        // the capture must survive with a scan-only record, not hang or
        // throw
        BluetoothFixture fixture = recorder("crash-on-connect").record(500,
                Arrays.asList("aa:01"), false);
        Assertions.assertEquals(2, fixture.getDevices().size());
        Assertions.assertFalse(fixture.getDevice("aa:01").hasGatt());
    }
}
