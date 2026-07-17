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
package com.codename1.bluetooth.helper;

import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattService;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.io.JSONParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * End-to-end replay of a real, PII-scrambled BLE capture through the exact
 * helper client stack the native Win32/Linux ports run --
 * {@link HelperBleBackend} + {@link HelperBlePeripheral} over a
 * {@link MockHelperTransport} (no subprocess, no radio). The mock data is
 * {@code bluetooth-fixtures/ambient-scan-2.json}, one of the scrambled
 * ambient scans captured on real hardware; each fixture device is turned
 * into a {@code scanResult} protocol event, so the assertions run against
 * the genuine captured device shapes. A synthetic connectable peripheral is
 * then driven through connect -> discover -> read to exercise the GATT path.
 */
public class FixtureReplayE2ETest {

    private static final long TIMEOUT_MS = 5000;

    private HelperBleBackend backend;
    private MockHelperTransport mock;

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

    /** Polls the backend's written commands until one matches {@code cmd}. */
    private long awaitCommandId(String cmd) throws Exception {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            String line = mock.takeWritten(200);
            if (line == null) {
                continue;
            }
            Map<String, Object> parsed = new JSONParser().parseJSON(
                    new java.io.StringReader(line));
            if (cmd.equals(String.valueOf(parsed.get("cmd")))) {
                return ((Number) parsed.get("id")).longValue();
            }
        }
        throw new AssertionError("timed out waiting for command " + cmd);
    }

    /** Records scan callbacks without failing on the reader thread. */
    private static final class RecordingScanSink
            implements BleBackend.ScanSink {
        final List<ScanResult> results = new CopyOnWriteArrayList<ScanResult>();
        volatile com.codename1.bluetooth.BluetoothException failure;

        public void onResult(ScanResult result) {
            results.add(result);
        }

        public void onFailed(com.codename1.bluetooth.BluetoothException reason) {
            failure = reason;
        }
    }

    private void newBackend() {
        mock = new MockHelperTransport();
        backend = new HelperBleBackend(new MockHelperTransportFactory(mock));
        // attaching a state sink starts the reader thread (ensureStarted),
        // exactly as HelperBluetooth's constructor does in the ports
        backend.setAdapterStateSink(new BleBackend.AdapterStateSink() {
            public void adapterStateChanged(
                    com.codename1.bluetooth.AdapterState newState) {
            }
        });
    }

    private void handshake() {
        mock.feedLine("{\"event\":\"capabilities\",\"version\":1,"
                + "\"descriptors\":true,\"bonding\":false}");
        mock.feedLine("{\"event\":\"stateChanged\",\"state\":\"poweredOn\"}");
        await("poweredOn", new Cond() {
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
        Assertions.assertNotNull(in, "fixture resource must be on the classpath");
        try {
            Map<String, Object> root = new JSONParser().parseJSON(
                    new InputStreamReader(in, "UTF-8"));
            return (List<Map<String, Object>>) root.get("devices");
        } finally {
            in.close();
        }
    }

    /** Builds a helper scanResult protocol line from a fixture device. */
    @SuppressWarnings("unchecked")
    private static String scanResultLine(Map<String, Object> device) {
        String id = String.valueOf(device.get("id"));
        Object connectable = device.get("connectable");
        int rssi = -128;
        List<Object> timeline = (List<Object>) device.get("rssiTimeline");
        if (timeline != null && !timeline.isEmpty()) {
            Map<String, Object> last =
                    (Map<String, Object>) timeline.get(timeline.size() - 1);
            rssi = ((Number) last.get("rssi")).intValue();
        }
        StringBuilder uuids = new StringBuilder("[");
        List<Object> svc = (List<Object>) device.get("serviceUuids");
        if (svc != null) {
            for (int i = 0; i < svc.size(); i++) {
                if (i > 0) {
                    uuids.append(',');
                }
                uuids.append('"').append(svc.get(i)).append('"');
            }
        }
        uuids.append(']');
        return "{\"event\":\"scanResult\",\"address\":\"" + id
                + "\",\"rssi\":" + rssi + ",\"connectable\":"
                + ("true".equals(String.valueOf(connectable))) + ","
                + "\"serviceUuids\":" + uuids + "}";
    }

    @Test
    public void everyScrambledDeviceSurfacesThroughTheHelperStack()
            throws Exception {
        List<Map<String, Object>> devices = loadFixtureDevices();
        Assertions.assertFalse(devices.isEmpty(), "fixture has devices");

        newBackend();
        handshake();

        final RecordingScanSink sink = new RecordingScanSink();
        backend.startScan(sink);
        awaitCommandId("scanStart");

        final List<String> expectedIds = new ArrayList<String>();
        for (int i = 0; i < devices.size(); i++) {
            expectedIds.add(String.valueOf(devices.get(i).get("id")));
            mock.feedLine(scanResultLine(devices.get(i)));
        }

        final int expected = devices.size();
        await("all scrambled devices sighted", new Cond() {
            public boolean met() {
                return sink.results.size() >= expected;
            }
        });
        Assertions.assertNull(sink.failure, "scan must not fail");

        List<String> seen = new ArrayList<String>();
        for (int i = 0; i < sink.results.size(); i++) {
            seen.add(sink.results.get(i).getPeripheral().getAddress());
        }
        for (int i = 0; i < expectedIds.size(); i++) {
            Assertions.assertTrue(seen.contains(expectedIds.get(i)),
                    "device " + expectedIds.get(i)
                            + " must surface through the helper stack; saw "
                            + seen);
        }
        // the scrambled addresses are the synthetic SC:RA:MB:* form -- proof
        // the mock data (not real MACs) flowed end to end
        Assertions.assertTrue(seen.get(0).startsWith("SC:RA:MB:"),
                "scrambled address preserved: " + seen.get(0));
    }

    @Test
    public void connectDiscoverReadOverTheHelperStack() throws Exception {
        newBackend();
        handshake();

        // sight one connectable device (reuse a scrambled id from the trace)
        final String address = "SC:RA:MB:D6:7C:F9";
        final RecordingScanSink sink = new RecordingScanSink();
        backend.startScan(sink);
        awaitCommandId("scanStart");
        mock.feedLine("{\"event\":\"scanResult\",\"address\":\"" + address
                + "\",\"rssi\":-61,\"connectable\":true,"
                + "\"serviceUuids\":[\"0000180d-0000-1000-8000-00805f9b34fb\"]}");
        await("device sighted", new Cond() {
            public boolean met() {
                return !sink.results.isEmpty();
            }
        });

        final BlePeripheral p = backend.getPeripheral(address);
        Assertions.assertNotNull(p, "peripheral resolvable by address");
        Assertions.assertEquals(address, p.getAddress());

        // connect
        final com.codename1.util.AsyncResource<BlePeripheral> connect =
                p.connect();
        long connectId = awaitCommandId("connect");
        mock.feedLine("{\"event\":\"connected\",\"requestId\":" + connectId
                + ",\"address\":\"" + address + "\"}");
        await("connected", new Cond() {
            public boolean met() {
                return connect.isDone();
            }
        });

        // discover a heart-rate service + measurement characteristic (the
        // protocol reports properties as a string array)
        final com.codename1.util.AsyncResource<List<GattService>> discover =
                p.discoverServices();
        long discoverId = awaitCommandId("discover");
        mock.feedLine("{\"event\":\"discovered\",\"requestId\":" + discoverId
                + ",\"address\":\"" + address + "\",\"services\":["
                + "{\"uuid\":\"0000180d-0000-1000-8000-00805f9b34fb\","
                + "\"primary\":true,\"characteristics\":["
                + "{\"uuid\":\"00002a37-0000-1000-8000-00805f9b34fb\","
                + "\"properties\":[\"read\",\"notify\"],"
                + "\"descriptors\":[]}]}]}");
        await("services discovered", new Cond() {
            public boolean met() {
                return discover.isDone();
            }
        });

        GattCharacteristic hr = p.getCharacteristic(
                BluetoothUuid.fromShort(0x180D), BluetoothUuid.fromShort(0x2A37));
        Assertions.assertNotNull(hr, "heart-rate characteristic discovered");
        Assertions.assertTrue(hr.canRead());
        Assertions.assertTrue(hr.canNotify());

        // read it -- value base64 "AEg=" for {0x00,0x48} = 72 bpm
        final com.codename1.util.AsyncResource<byte[]> read = hr.read();
        long readId = awaitCommandId("read");
        mock.feedLine("{\"event\":\"readResult\",\"requestId\":" + readId
                + ",\"value\":\"AEg=\"}");
        await("read completed", new Cond() {
            public boolean met() {
                return read.isDone();
            }
        });
        Assertions.assertArrayEquals(new byte[] {0x00, 0x48}, read.get(null),
                "characteristic value decoded through Wire base64");
    }
}
