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
import com.codename1.bluetooth.gatt.GattNotificationListener;
import com.codename1.bluetooth.gatt.GattService;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.ConnectionState;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.impl.bluetooth.BleBackend;
import com.codename1.impl.bluetooth.NativeBleBackend;
import com.codename1.util.AsyncResource;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Drives the ENTIRE real native stack -- the bundled {@code libcn1ble} loaded
 * through JNI, its event channel, the wire JSON shapes and the
 * {@link NativeBleBackend} that decodes them -- through a full GATT client
 * lifecycle, deterministically and on any host. It uses the library's
 * scripted-responder test mode ({@link JniBleBridge#enableTestMode()}), which
 * services one fixed virtual peripheral in-library with no radio, no BlueZ and
 * no CoreBluetooth, so scan / connect / discover / read / write / subscribe /
 * notify all round-trip through the genuine native boundary rather than a
 * Java-side fake. The scenario constants below MUST match the responder built
 * into libcn1ble.
 */
public class NativeBleScriptedEngineTest {

    private static final long TIMEOUT_MILLIS = 15000;

    private static final String ADDRESS = "aa:bb:cc:dd:ee:01";
    private static final String NAME = "CN1 Test HRM";
    private static final int RSSI = -55;
    private static final int MFR_COMPANY = 76;
    private static final byte[] MFR_DATA = {1, 2, 3};
    private static final String HR_SERVICE =
            "0000180d-0000-1000-8000-00805f9b34fb";
    private static final String MEASUREMENT =
            "00002a37-0000-1000-8000-00805f9b34fb";
    private static final String CONTROL =
            "00002a39-0000-1000-8000-00805f9b34fb";
    private static final String CCCD =
            "00002902-0000-1000-8000-00805f9b34fb";
    private static final byte[] READ_VALUE = {6, 72};
    private static final byte[] NOTIFY_VALUE = {6, 80};

    private interface Cond {
        boolean ready();
    }

    private static void await(String what, Cond cond)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (cond.ready()) {
                return;
            }
            Thread.sleep(20);
        }
        fail("timed out waiting for " + what);
    }

    private static BluetoothUuid uuid(String s) {
        return BluetoothUuid.fromString(s);
    }

    private static final class RecordingScanSink
            implements BleBackend.ScanSink {
        private final List<ScanResult> results =
                new CopyOnWriteArrayList<ScanResult>();
        private final AtomicReference<Throwable> failure =
                new AtomicReference<Throwable>();

        @Override
        public void onResult(ScanResult result) {
            results.add(result);
        }

        @Override
        public void onFailed(com.codename1.bluetooth.BluetoothException r) {
            failure.set(r);
        }
    }

    private static final class RecordingNotifyListener
            implements GattNotificationListener {
        private final List<byte[]> values =
                new CopyOnWriteArrayList<byte[]>();

        @Override
        public void valueChanged(GattCharacteristic characteristic,
                byte[] value) {
            values.add(value);
        }
    }

    @Test
    public void fullGattLifecycleThroughTheRealNativeEngine()
            throws InterruptedException {
        assertTrue(JniBleBridge.isLibraryAvailable(),
                "libcn1ble must be bundled and loadable: "
                        + JniBleBridge.describeResolution());
        JniBleBridge bridge = new JniBleBridge();
        JniBleBridge.enableTestMode();
        NativeBleBackend backend = new NativeBleBackend(bridge);
        try {
            // scan -> the scripted peripheral is sighted, advertisement fields
            // decoded from the real event JSON
            RecordingScanSink sink = new RecordingScanSink();
            backend.startScan(sink);
            await("scan sighting", () -> !sink.results.isEmpty());
            assertNull(sink.failure.get(),
                    "scan failed: " + sink.failure.get());
            ScanResult sighting = sink.results.get(0);
            assertEquals(ADDRESS, sighting.getPeripheral().getAddress());
            assertEquals(NAME, sighting.getAdvertisementData().getLocalName());
            assertEquals(RSSI, sighting.getRssi());
            assertTrue(sighting.getAdvertisementData().getServiceUuids()
                    .contains(uuid(HR_SERVICE)));
            assertArrayEquals(MFR_DATA, sighting.getAdvertisementData()
                    .getManufacturerData(MFR_COMPANY));
            backend.stopScan();

            final BlePeripheral p = backend.getPeripheral(ADDRESS);
            assertNotNull(p);

            // connect
            AsyncResource<BlePeripheral> connect = p.connect();
            await("connect", connect::isDone);
            await("connected state", () -> p.getConnectionState()
                    == ConnectionState.CONNECTED);

            // discover -> the GATT DB is decoded from the real discovered event
            AsyncResource<List<GattService>> discover = p.discoverServices();
            await("discover", discover::isDone);
            GattService hr = p.getService(uuid(HR_SERVICE));
            assertNotNull(hr, "heart-rate service not discovered");
            GattCharacteristic measurement =
                    hr.getCharacteristic(uuid(MEASUREMENT));
            assertNotNull(measurement);
            assertTrue(measurement.canRead());
            assertTrue(measurement.canNotify());
            assertNotNull(measurement.getDescriptor(uuid(CCCD)));
            GattCharacteristic control = hr.getCharacteristic(uuid(CONTROL));
            assertNotNull(control);
            assertTrue(control.canWrite());

            // read -> canned value comes back base64-decoded
            AsyncResource<byte[]> read = measurement.read();
            await("read", read::isDone);
            assertArrayEquals(READ_VALUE, read.get(null));

            // write
            AsyncResource<Boolean> write = control.write(new byte[] {9});
            await("write", write::isDone);
            assertEquals(Boolean.TRUE, write.get(null));

            // subscribe -> notification
            RecordingNotifyListener listener = new RecordingNotifyListener();
            AsyncResource<Boolean> subscribe = measurement.subscribe(listener);
            await("subscribe", subscribe::isDone);
            await("notification", () -> !listener.values.isEmpty());
            assertArrayEquals(NOTIFY_VALUE, listener.values.get(0));

            // disconnect
            p.disconnect();
            await("disconnected", () -> p.getConnectionState()
                    == ConnectionState.DISCONNECTED);
        } finally {
            backend.shutdown();
        }
    }
}
