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
import com.codename1.bluetooth.gatt.GattService;
import com.codename1.bluetooth.le.AdvertisementData;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.CharacteristicRecord;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.Device;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.ServiceRecord;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Replays the SHIPPED fixture files (real traces captured from live
 * hardware via {@link FixtureRecorder} and scrambled with
 * {@link FixtureScrambler}) into a fully deterministic
 * {@link ManualScheduler} stack, asserting that the scan feed delivers
 * every recorded device with its advertisement payload -- through the
 * raw stack feed and through the core-API {@link SimulatorBleBackend}
 * ({@link ScanResult} level). Fixtures that carry a captured GATT
 * database are additionally connected, discovered and read through the
 * full stack, comparing values against the fixture.
 *
 * <p>Both shipped fixtures are scan-only ambient captures from two
 * different moments: every connectable device in the recording
 * environment (Apple/Samsung random-address peripherals) refused GATT
 * connections, the documented common case. The GATT replay path is
 * covered end-to-end by {@link FixtureRecorderTest}, which records a
 * GATT-bearing fixture from the scripted fake helper and replays it the
 * same way (see {@code replayFixture(...)} here, shared by both tests).
 * </p>
 */
public class RecordedTraceReplayTest {

    /** The committed traces under {@code /bluetooth-fixtures/}. */
    static final String[] SHIPPED_FIXTURES = {
        "ambient-scan.json", "ambient-scan-2.json"
    };

    /** Loads a shipped fixture from the test classpath. */
    static BluetoothFixture load(String name) throws IOException {
        InputStream in = RecordedTraceReplayTest.class.getResourceAsStream(
                "/bluetooth-fixtures/" + name);
        Assertions.assertNotNull(in, "missing fixture resource " + name);
        try {
            return BluetoothFixture.fromJson(in);
        } finally {
            in.close();
        }
    }

    @Test
    public void ambientScanReplaysEveryDevice() throws IOException {
        replayShipped("ambient-scan.json");
    }

    @Test
    public void secondAmbientScanReplaysEveryDevice() throws IOException {
        replayShipped("ambient-scan-2.json");
    }

    @Test
    public void reloadAfterResetWorks() throws IOException {
        BluetoothFixture fixture = load("ambient-scan.json");
        ManualScheduler scheduler = new ManualScheduler();
        SimulatedBluetoothStack stack =
                new SimulatedBluetoothStack(scheduler);
        stack.loadFixture(fixture);
        scheduler.advance(replayWindow(fixture));
        Assertions.assertEquals(fixture.getDevices().size(),
                stack.getPeripheralAddresses().size());
        stack.reset();
        scheduler.advance(1000);
        Assertions.assertTrue(stack.getPeripheralAddresses().isEmpty());
        stack.loadFixture(fixture);
        scheduler.advance(replayWindow(fixture));
        Assertions.assertEquals(fixture.getDevices().size(),
                stack.getPeripheralAddresses().size());
    }

    private void replayShipped(String name) throws IOException {
        BluetoothFixture fixture = load(name);
        Assertions.assertFalse(fixture.getDevices().isEmpty());
        replayFixture(fixture);
    }

    /**
     * The full replay assertion, shared with {@link FixtureRecorderTest}:
     * loads the fixture into a fresh ManualScheduler stack, advances the
     * virtual clock across the recorded timeline and asserts that
     * <ul>
     *   <li>the raw stack scan feed and the core {@link ScanResult} feed
     *       deliver every fixture device exactly once,</li>
     *   <li>each advertisement carries the recorded name, service UUIDs,
     *       manufacturer data, service data and TX power,</li>
     *   <li>every GATT-bearing device connects, discovers the recorded
     *       database and serves the captured characteristic values.</li>
     * </ul>
     */
    static void replayFixture(BluetoothFixture fixture) {
        ManualScheduler scheduler = new ManualScheduler();
        SimulatedBluetoothStack stack =
                new SimulatedBluetoothStack(scheduler);
        SimulatorBleBackend backend = new SimulatorBleBackend(stack);

        final List<VirtualPeripheral> rawSightings =
                new ArrayList<VirtualPeripheral>();
        stack.startScanFeed(new SimulatedBluetoothStack.ScanFeedSink() {
            public void onSighting(VirtualPeripheral peripheral,
                    long timestamp) {
                rawSightings.add(peripheral);
            }

            public void onScanFailed(com.codename1.bluetooth.BluetoothError
                    error, String message) {
                Assertions.fail("scan failed: " + error + " " + message);
            }
        });
        final Map<String, ScanResult> results =
                new HashMap<String, ScanResult>();
        backend.startScan(new BleBackend.ScanSink() {
            public void onResult(ScanResult result) {
                results.put(result.getPeripheral().getAddress(), result);
            }

            public void onFailed(BluetoothException reason) {
                Assertions.fail("core scan failed: " + reason);
            }
        });

        stack.loadFixture(fixture);
        scheduler.advance(replayWindow(fixture));

        List<Device> devices = fixture.getDevices();
        Assertions.assertEquals(devices.size(), rawSightings.size(),
                "raw feed must deliver every fixture device exactly once");
        Assertions.assertEquals(devices.size(), results.size(),
                "core feed must deliver every fixture device exactly once");
        for (Device d : devices) {
            assertAdvertisement(d, results.get(d.id));
            assertRssiTimelineApplied(d, stack);
            if (d.hasGatt()) {
                assertGattReplay(d, backend, scheduler);
            }
        }
    }

    /** Far enough past the last recorded timestamp for all completions. */
    private static long replayWindow(BluetoothFixture fixture) {
        long max = 0;
        for (Device d : fixture.getDevices()) {
            int size = d.rssiTimeline.size();
            for (int i = 0; i < size; i++) {
                max = Math.max(max, d.rssiTimeline.get(i).relTimeMs);
            }
        }
        return max + 10000;
    }

    private static void assertAdvertisement(Device d, ScanResult result) {
        Assertions.assertNotNull(result, "no sighting of " + d.id);
        Assertions.assertEquals(d.id,
                result.getPeripheral().getAddress());
        Assertions.assertEquals(d.connectable, result.isConnectable());
        Assertions.assertFalse(d.rssiTimeline.isEmpty());
        Assertions.assertEquals(d.rssiTimeline.get(0).rssi,
                result.getRssi(),
                d.id + ": the sighting carries the first recorded RSSI");
        AdvertisementData ad = result.getAdvertisementData();
        Assertions.assertNotNull(ad);
        Assertions.assertEquals(d.name, ad.getLocalName(), d.id + " name");
        Assertions.assertEquals(d.serviceUuids, ad.getServiceUuids(),
                d.id + " advertised service uuids");
        Assertions.assertEquals(d.manufacturerData.size(),
                ad.getManufacturerIds().length, d.id + " manufacturer ids");
        for (Map.Entry<Integer, byte[]> e : d.manufacturerData.entrySet()) {
            Assertions.assertArrayEquals(e.getValue(),
                    ad.getManufacturerData(e.getKey().intValue()),
                    d.id + " manufacturer data " + e.getKey());
        }
        Assertions.assertEquals(d.serviceData.size(),
                ad.getServiceDataUuids().size(), d.id + " service data");
        for (Map.Entry<BluetoothUuid, byte[]> e
                : d.serviceData.entrySet()) {
            Assertions.assertArrayEquals(e.getValue(),
                    ad.getServiceData(e.getKey()),
                    d.id + " service data " + e.getKey());
        }
        Assertions.assertEquals(d.txPower, ad.getTxPowerLevel(),
                d.id + " tx power");
    }

    /** After the window, the peripheral holds the LAST timeline RSSI. */
    private static void assertRssiTimelineApplied(Device d,
            SimulatedBluetoothStack stack) {
        VirtualPeripheral p = stack.getPeripheral(d.id);
        Assertions.assertNotNull(p, d.id + " must be registered");
        Assertions.assertEquals(
                d.rssiTimeline.get(d.rssiTimeline.size() - 1).rssi,
                p.getRssi(), d.id + " final RSSI after timeline replay");
    }

    /** Connect + discover + read through the core API. */
    private static void assertGattReplay(Device d,
            SimulatorBleBackend backend, ManualScheduler scheduler) {
        BlePeripheral p = backend.getPeripheral(d.id);
        Assertions.assertNotNull(p, d.id + " peripheral lookup");
        AsyncResource<BlePeripheral> connect = p.connect();
        scheduler.advance(1000);
        Assertions.assertTrue(connect.isDone(), d.id + " connect");
        AsyncResource<List<GattService>> discover = p.discoverServices();
        scheduler.advance(1000);
        Assertions.assertTrue(discover.isDone(), d.id + " discover");
        List<GattService> services = discover.get(null);
        Assertions.assertNotNull(services);
        Assertions.assertEquals(d.gatt.size(), services.size(),
                d.id + " service count");
        for (ServiceRecord sr : d.gatt) {
            GattService service = p.getService(sr.uuid);
            Assertions.assertNotNull(service,
                    d.id + " service " + sr.uuid);
            Assertions.assertEquals(sr.primary, service.isPrimary());
            Assertions.assertEquals(sr.characteristics.size(),
                    service.getCharacteristics().size());
            for (CharacteristicRecord cr : sr.characteristics) {
                GattCharacteristic c = service.getCharacteristic(cr.uuid);
                Assertions.assertNotNull(c, d.id + " characteristic "
                        + cr.uuid);
                Assertions.assertEquals(cr.properties, c.getProperties());
                Assertions.assertEquals(cr.descriptors.size(),
                        c.getDescriptors().size());
                if (cr.value != null && c.canRead()) {
                    AsyncResource<byte[]> read = c.read();
                    scheduler.advance(1000);
                    Assertions.assertTrue(read.isDone(),
                            d.id + " read " + cr.uuid);
                    Assertions.assertArrayEquals(cr.value, read.get(null),
                            d.id + " captured value of " + cr.uuid);
                }
            }
        }
        p.disconnect();
        scheduler.advance(1000);
    }
}
