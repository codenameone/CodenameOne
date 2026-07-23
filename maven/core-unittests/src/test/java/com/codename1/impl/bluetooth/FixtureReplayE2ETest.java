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
package com.codename1.impl.bluetooth;

import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.io.JSONParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * End-to-end replay of a real, PII-scrambled BLE capture through the native
 * backend client stack -- {@link NativeBleBackend} + {@link FakeNativeBleBridge}
 * (no native engine, no radio). The mock data is
 * {@code bluetooth-fixtures/ambient-scan-2.json}, one of the scrambled ambient
 * scans captured on real hardware; each fixture device is turned into a
 * {@code scanResult} protocol event, so the assertions -- addresses, names,
 * RSSI and manufacturer data -- run against the genuine captured device shapes.
 */
public class FixtureReplayE2ETest {

    private static final long TIMEOUT_MS = 5000;

    private NativeBleBackend backend;
    private FakeNativeBleBridge bridge;

    @AfterEach
    void tearDown() {
        if (backend != null) {
            backend.shutdown();
            backend = null;
        }
    }

    private interface Cond {
        boolean met();
    }

    private static void await(String what, Cond cond) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (cond.met()) {
                return;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                Assertions.fail("interrupted waiting for " + what);
            }
        }
        Assertions.fail("timed out waiting for " + what);
    }

    /** Records scan callbacks without failing on the reader thread. */
    private static final class RecordingScanSink
            implements BleBackend.ScanSink {
        private final List<ScanResult> results =
                new CopyOnWriteArrayList<ScanResult>();
        private final AtomicReference<BluetoothException> failure =
                new AtomicReference<BluetoothException>();

        @Override
        public void onResult(ScanResult result) {
            results.add(result);
        }

        @Override
        public void onFailed(BluetoothException reason) {
            failure.set(reason);
        }
    }

    private void newBackend() {
        bridge = new FakeNativeBleBridge();
        backend = new NativeBleBackend(bridge);
        // attaching a state sink starts the reader thread (ensureStarted),
        // exactly as the port's Bluetooth activation does
        backend.setAdapterStateSink(new BleBackend.AdapterStateSink() {
            @Override
            public void adapterStateChanged(
                    com.codename1.bluetooth.AdapterState newState) {
            }
        });
    }

    private void handshake() {
        bridge.feed("{\"event\":\"capabilities\",\"version\":1,"
                + "\"descriptors\":true,\"bonding\":false}");
        bridge.feed("{\"event\":\"stateChanged\",\"state\":\"poweredOn\"}");
        await("poweredOn", new Cond() {
            @Override
            public boolean met() {
                return backend.getAdapterState()
                        == com.codename1.bluetooth.AdapterState.POWERED_ON;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> loadFixtureDevices()
            throws Exception {
        InputStream in = FixtureReplayE2ETest.class.getResourceAsStream(
                "/bluetooth-fixtures/ambient-scan-2.json");
        Assertions.assertNotNull(in,
                "fixture resource must be on the classpath");
        try {
            Map<String, Object> root = new JSONParser().parseJSON(
                    new InputStreamReader(in, "UTF-8"));
            return (List<Map<String, Object>>) root.get("devices");
        } finally {
            in.close();
        }
    }

    /** Builds a native scanResult event line from a fixture device. */
    @SuppressWarnings("unchecked")
    private static String scanResultLine(Map<String, Object> device) {
        String id = String.valueOf(device.get("id"));
        int rssi = -128;
        List<Object> timeline = (List<Object>) device.get("rssiTimeline");
        if (timeline != null && !timeline.isEmpty()) {
            Map<String, Object> last =
                    (Map<String, Object>) timeline.get(timeline.size() - 1);
            rssi = ((Number) last.get("rssi")).intValue();
        }
        StringBuilder sb = new StringBuilder(
                "{\"event\":\"scanResult\",\"address\":\"");
        sb.append(id).append("\",\"rssi\":").append(rssi);
        Object name = device.get("name");
        if (name != null) {
            sb.append(",\"name\":\"").append(name).append('"');
        }
        sb.append(",\"serviceUuids\":[");
        List<Object> svc = (List<Object>) device.get("serviceUuids");
        if (svc != null) {
            for (int i = 0; i < svc.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append('"').append(svc.get(i)).append('"');
            }
        }
        sb.append("],\"manufacturerData\":{");
        Map<String, Object> mfg =
                (Map<String, Object>) device.get("manufacturerData");
        if (mfg != null) {
            boolean first = true;
            for (Map.Entry<String, Object> e : mfg.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append('"').append(e.getKey()).append("\":\"")
                        .append(e.getValue()).append('"');
            }
        }
        sb.append("}}");
        return sb.toString();
    }

    private ScanResult resultFor(RecordingScanSink sink, String address) {
        for (int i = 0; i < sink.results.size(); i++) {
            ScanResult r = sink.results.get(i);
            if (address.equals(r.getPeripheral().getAddress())) {
                return r;
            }
        }
        return null;
    }

    @Test
    public void everyScrambledDeviceSurfacesThroughTheNativeStack()
            throws Exception {
        List<Map<String, Object>> devices = loadFixtureDevices();
        Assertions.assertFalse(devices.isEmpty(), "fixture has devices");

        newBackend();
        handshake();

        final RecordingScanSink sink = new RecordingScanSink();
        backend.startScan(sink);
        bridge.awaitCommandId("scanStart", TIMEOUT_MS);

        for (int i = 0; i < devices.size(); i++) {
            bridge.feed(scanResultLine(devices.get(i)));
        }

        final int expected = devices.size();
        await("all scrambled devices sighted", new Cond() {
            @Override
            public boolean met() {
                return sink.results.size() >= expected;
            }
        });
        Assertions.assertNull(sink.failure.get(), "scan must not fail");

        // every scrambled device surfaces, addresses preserved end to end
        for (int i = 0; i < devices.size(); i++) {
            String id = String.valueOf(devices.get(i).get("id"));
            Assertions.assertTrue(id.startsWith("SC:RA:MB:"),
                    "fixture address is scrambled: " + id);
            Assertions.assertNotNull(resultFor(sink, id),
                    "device " + id + " must surface through the native stack");
        }

        // spot-check a named device with a manufacturer-data payload and its
        // last-seen RSSI, decoded through the same base64 the backend uses
        ScanResult named = resultFor(sink, "SC:RA:MB:D5:45:FC");
        Assertions.assertNotNull(named);
        Assertions.assertEquals("Device-A739",
                named.getPeripheral().getName());
        Assertions.assertEquals(-79, named.getRssi());
        Assertions.assertArrayEquals(
                Json.decodeBase64("ZyqWc+hzQFFVqiqd4RLJHZse7dSGx2r/"),
                named.getAdvertisementData().getManufacturerData(117));

        // an unnamed device that advertises Apple (0x004C = 76) manufacturer
        // data at a different RSSI
        ScanResult apple = resultFor(sink, "SC:RA:MB:7F:B8:BD");
        Assertions.assertNotNull(apple);
        Assertions.assertEquals(-76, apple.getRssi());
        Assertions.assertArrayEquals(Json.decodeBase64("4KE39IvA1g=="),
                apple.getAdvertisementData().getManufacturerData(76));
    }
}
