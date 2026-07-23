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

import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.CharacteristicRecord;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.DescriptorRecord;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.Device;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.RssiSample;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.ServiceRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * The {@link FixtureScrambler} contract: deterministic under a seed,
 * stable per identifier within a trace, structure-preserving (lengths,
 * company ids, SIG UUIDs), a pure function of its input, and -- the whole
 * point -- no original identity survives into the output. Plus the
 * {@link BluetoothFixture} JSON round-trip.
 */
public class FixtureScramblerTest {

    private static final BluetoothUuid HR_SERVICE =
            BluetoothUuid.fromShort(0x180D);
    private static final BluetoothUuid HR_MEASUREMENT =
            BluetoothUuid.fromShort(0x2A37);
    private static final BluetoothUuid CCCD = BluetoothUuid.fromShort(0x2902);
    private static final BluetoothUuid CUSTOM_SERVICE =
            BluetoothUuid.fromString("5f47a3c0-1234-4e6b-9d00-000000000001");
    private static final BluetoothUuid CUSTOM_DATA =
            BluetoothUuid.fromString("5f47a3c0-1234-4e6b-9d00-000000000002");

    /** A two-device trace exercising every scrambled field. */
    private static BluetoothFixture sampleFixture() {
        BluetoothFixture fixture = new BluetoothFixture()
                .setPlatform("unit test");

        Device hrm = new Device("AA:BB:CC:DD:EE:01");
        hrm.name = "Zephyr HRM Alpha";
        hrm.connectable = true;
        hrm.txPower = Integer.valueOf(4);
        hrm.rssiTimeline.add(new RssiSample(0, -40));
        hrm.rssiTimeline.add(new RssiSample(500, -55));
        hrm.serviceUuids.add(HR_SERVICE);
        hrm.serviceUuids.add(CUSTOM_SERVICE);
        hrm.manufacturerData.put(Integer.valueOf(0x004C),
                new byte[] {1, 2, 3});
        hrm.serviceData.put(CUSTOM_DATA, new byte[] {9, 9});
        ServiceRecord hrService = new ServiceRecord(HR_SERVICE);
        CharacteristicRecord measurement = new CharacteristicRecord(
                HR_MEASUREMENT, GattCharacteristic.PROPERTY_READ
                        | GattCharacteristic.PROPERTY_NOTIFY);
        measurement.value = new byte[] {0, 72};
        measurement.descriptors.add(new DescriptorRecord(CCCD));
        hrService.characteristics.add(measurement);
        hrm.gatt.add(hrService);
        ServiceRecord customService = new ServiceRecord(CUSTOM_SERVICE);
        CharacteristicRecord customChar = new CharacteristicRecord(
                CUSTOM_DATA, GattCharacteristic.PROPERTY_WRITE);
        customService.characteristics.add(customChar);
        hrm.gatt.add(customService);
        fixture.addDevice(hrm);

        Device beacon = new Device("AA:BB:CC:DD:EE:02");
        beacon.name = "Tag"; // short-name class
        beacon.connectable = false;
        beacon.rssiTimeline.add(new RssiSample(120, -88));
        beacon.manufacturerData.put(Integer.valueOf(0x0075),
                new byte[] {5, 6, 7, 8});
        fixture.addDevice(beacon);
        return fixture;
    }

    @Test
    public void sameSeedYieldsIdenticalOutput() {
        BluetoothFixture fixture = sampleFixture();
        String a = FixtureScrambler.scramble(fixture, 42).toJson();
        String b = FixtureScrambler.scramble(fixture, 42).toJson();
        Assertions.assertEquals(a, b);
    }

    @Test
    public void differentSeedsYieldDifferentIdentities() {
        BluetoothFixture fixture = sampleFixture();
        BluetoothFixture a = FixtureScrambler.scramble(fixture, 42);
        BluetoothFixture b = FixtureScrambler.scramble(fixture, 43);
        Assertions.assertNotEquals(a.getDevices().get(0).id,
                b.getDevices().get(0).id);
        Assertions.assertNotEquals(a.getDevices().get(0).name,
                b.getDevices().get(0).name);
    }

    @Test
    public void scramblingIsAPureFunction() {
        BluetoothFixture fixture = sampleFixture();
        String before = fixture.toJson();
        FixtureScrambler.scramble(fixture, 42);
        Assertions.assertEquals(before, fixture.toJson(),
                "scramble must not mutate its input");
    }

    @Test
    public void identifiersAreStableWithinTheTrace() {
        BluetoothFixture scrambled =
                FixtureScrambler.scramble(sampleFixture(), 42);
        Device hrm = scrambled.getDevices().get(0);
        // the custom UUID occurs as an advertised uuid, a service-data
        // key, a GATT service uuid -- all must map identically
        BluetoothUuid advertised = hrm.serviceUuids.get(1);
        Assertions.assertNotEquals(CUSTOM_SERVICE, advertised);
        Assertions.assertEquals(advertised, hrm.gatt.get(1).uuid);
        // ... and CUSTOM_DATA maps consistently between service data and
        // the characteristic that carries it
        BluetoothUuid dataKey =
                hrm.serviceData.keySet().iterator().next();
        Assertions.assertNotEquals(CUSTOM_DATA, dataKey);
        Assertions.assertEquals(dataKey,
                hrm.gatt.get(1).characteristics.get(0).uuid);
    }

    @Test
    public void structureIsPreserved() {
        BluetoothFixture original = sampleFixture();
        BluetoothFixture scrambled = FixtureScrambler.scramble(original, 42);
        Assertions.assertEquals(2, scrambled.getDevices().size());
        Device hrm = scrambled.getDevices().get(0);
        Device beacon = scrambled.getDevices().get(1);

        // ids become synthetic addresses
        Assertions.assertTrue(hrm.id.startsWith("SC:RA:MB:"), hrm.id);
        Assertions.assertTrue(beacon.id.startsWith("SC:RA:MB:"), beacon.id);
        Assertions.assertNotEquals(hrm.id, beacon.id);

        // name length classes: long stays long-form, short stays short
        Assertions.assertTrue(hrm.name.startsWith("Device-"), hrm.name);
        Assertions.assertTrue(beacon.name.startsWith("Dev-"), beacon.name);
        Assertions.assertFalse(beacon.name.startsWith("Device-"));

        // non-identity fields pass through untouched
        Assertions.assertTrue(hrm.connectable);
        Assertions.assertFalse(beacon.connectable);
        Assertions.assertEquals(Integer.valueOf(4), hrm.txPower);
        Assertions.assertEquals(2, hrm.rssiTimeline.size());
        Assertions.assertEquals(500, hrm.rssiTimeline.get(1).relTimeMs);
        Assertions.assertEquals(-55, hrm.rssiTimeline.get(1).rssi);

        // SIG uuids kept verbatim, custom uuids remapped off-SIG
        Assertions.assertEquals(HR_SERVICE, hrm.serviceUuids.get(0));
        Assertions.assertFalse(hrm.serviceUuids.get(1).isShortUuid());
        Assertions.assertEquals(HR_SERVICE, hrm.gatt.get(0).uuid);
        Assertions.assertEquals(HR_MEASUREMENT,
                hrm.gatt.get(0).characteristics.get(0).uuid);
        Assertions.assertEquals(CCCD, hrm.gatt.get(0).characteristics
                .get(0).descriptors.get(0).uuid);

        // company ids kept, payload lengths preserved, bytes replaced
        Assertions.assertEquals(3,
                hrm.manufacturerData.get(Integer.valueOf(0x004C)).length);
        Assertions.assertEquals(4,
                beacon.manufacturerData.get(Integer.valueOf(0x0075)).length);
        Assertions.assertFalse(java.util.Arrays.equals(new byte[] {1, 2, 3},
                hrm.manufacturerData.get(Integer.valueOf(0x004C))));

        // service data: payload length kept
        Assertions.assertEquals(2,
                hrm.serviceData.values().iterator().next().length);

        // characteristic values: same length, properties kept
        CharacteristicRecord measurement =
                hrm.gatt.get(0).characteristics.get(0);
        Assertions.assertEquals(2, measurement.value.length);
        Assertions.assertEquals(GattCharacteristic.PROPERTY_READ
                | GattCharacteristic.PROPERTY_NOTIFY,
                measurement.properties);
        Assertions.assertNull(
                hrm.gatt.get(1).characteristics.get(0).value);
    }

    @Test
    public void noOriginalIdentitySurvives() {
        BluetoothFixture original = sampleFixture();
        BluetoothFixture scrambled = FixtureScrambler.scramble(original, 42);
        Assertions.assertEquals(java.util.Collections.emptyList(),
                FixtureScrambler.findLeaks(original, scrambled));
        String json = scrambled.toJson();
        Assertions.assertFalse(json.contains("AA:BB:CC:DD:EE:01"));
        Assertions.assertFalse(json.contains("Zephyr"));
        Assertions.assertFalse(json.contains("Tag"));
        Assertions.assertFalse(json.contains(CUSTOM_SERVICE.toString()));
        Assertions.assertFalse(json.contains(CUSTOM_DATA.toString()));
    }

    @Test
    public void findLeaksCatchesAnUnscrambledFixture() {
        BluetoothFixture original = sampleFixture();
        List<String> leaks = FixtureScrambler.findLeaks(original, original);
        Assertions.assertTrue(leaks.contains("AA:BB:CC:DD:EE:01"));
        Assertions.assertTrue(leaks.contains("Zephyr HRM Alpha"));
        Assertions.assertTrue(leaks.contains(CUSTOM_SERVICE.toString()));
    }

    @Test
    public void jsonRoundTripIsLossless() throws IOException {
        BluetoothFixture scrambled =
                FixtureScrambler.scramble(sampleFixture(), 42);
        String json = scrambled.toJson();
        BluetoothFixture reparsed = BluetoothFixture.fromJson(
                new ByteArrayInputStream(json.getBytes("UTF-8")));
        Assertions.assertEquals(json, reparsed.toJson());
        Assertions.assertEquals(BluetoothFixture.FORMAT_VERSION,
                reparsed.getVersion());
        Assertions.assertEquals(scrambled.getPlatform(),
                reparsed.getPlatform());
    }

    @Test
    public void unsupportedVersionIsRejected() {
        String json = "{\"version\": 99, \"devices\": []}";
        Assertions.assertThrows(IOException.class,
                () -> BluetoothFixture.fromJson(json));
    }

    @Test
    public void shippedFixturesParseCleanAndCarryNoRawIdentities() throws
            IOException {
        for (String name : RecordedTraceReplayTest.SHIPPED_FIXTURES) {
            BluetoothFixture fixture = RecordedTraceReplayTest.load(name);
            Assertions.assertFalse(fixture.getDevices().isEmpty(), name);
            for (Device d : fixture.getDevices()) {
                Assertions.assertTrue(d.id.startsWith("SC:RA:MB:"),
                        name + ": unscrambled device id " + d.id);
                if (d.name != null) {
                    Assertions.assertTrue(d.name.startsWith("Device-")
                            || d.name.startsWith("Dev-"),
                            name + ": unscrambled name " + d.name);
                }
                Assertions.assertFalse(d.rssiTimeline.isEmpty(),
                        name + ": " + d.id + " has no sightings");
            }
        }
    }
}
