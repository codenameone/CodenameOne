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

import com.codename1.bluetooth.AdapterState;
import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattNotificationListener;
import com.codename1.bluetooth.gatt.GattService;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.ConnectionState;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Drives {@link NativeBleBackend} end to end through a scripted
 * {@link FakeNativeBleBridge} -- exercising the reader thread, the
 * request/terminal-event correlation, the capabilities handshake, scanning,
 * the GATT client lifecycle (connect, discover, read, write, subscribe,
 * notify), crash-mid-flight handling and shutdown, all in-process with no
 * native {@code libcn1ble} engine.
 *
 * <p>Extends {@link UITestBase} so {@link com.codename1.ui.Display} is
 * initialised: connection-state and notification callbacks are dispatched on
 * the EDT, so the state transitions this suite awaits are delivered there.</p>
 */
public class NativeBleBackendFakeBridgeTest extends UITestBase {

    private static final long TIMEOUT_MS = 10000;
    private static final String HR_SERVICE =
            "0000180d-0000-1000-8000-00805f9b34fb";
    private static final String HR_MEASUREMENT =
            "00002a37-0000-1000-8000-00805f9b34fb";
    private static final String HR_CONTROL =
            "00002a39-0000-1000-8000-00805f9b34fb";
    private static final String CCCD =
            "00002902-0000-1000-8000-00805f9b34fb";

    private NativeBleBackend backend;
    private FakeNativeBleBridge bridge;

    @AfterEach
    void tearDownBackend() {
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
                Assertions.fail("interrupted while waiting for " + what);
            }
        }
        Assertions.fail("timed out waiting for " + what);
    }

    private NativeBleBackend newBackend() {
        bridge = new FakeNativeBleBridge();
        backend = new NativeBleBackend(bridge);
        return backend;
    }

    private long awaitCommand(String cmd) {
        return bridge.awaitCommandId(cmd, TIMEOUT_MS);
    }

    private List<AdapterState> attachStateSink(final NativeBleBackend b) {
        final List<AdapterState> states =
                new CopyOnWriteArrayList<AdapterState>();
        b.setAdapterStateSink(new BleBackend.AdapterStateSink() {
            @Override
            public void adapterStateChanged(AdapterState newState) {
                states.add(newState);
            }
        });
        return states;
    }

    /** Feeds the capabilities + poweredOn handshake and waits for it. */
    private void handshake(final NativeBleBackend b) {
        bridge.feed("{\"event\":\"capabilities\",\"version\":1,"
                + "\"descriptors\":true,\"bonding\":false}");
        bridge.feed("{\"event\":\"stateChanged\",\"state\":\"poweredOn\"}");
        await("poweredOn handshake", new Cond() {
            @Override
            public boolean met() {
                return b.getAdapterState() == AdapterState.POWERED_ON;
            }
        });
    }

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

    private static final class RecordingNotifyListener
            implements GattNotificationListener {
        private final List<byte[]> values = new CopyOnWriteArrayList<byte[]>();

        @Override
        public void valueChanged(GattCharacteristic characteristic,
                byte[] value) {
            values.add(value);
        }
    }

    /** Boots, handshakes, scans and delivers one aa:01 sighting. */
    private RecordingScanSink bootAndSight(NativeBleBackend b) {
        attachStateSink(b);
        handshake(b);
        RecordingScanSink sink = new RecordingScanSink();
        b.startScan(sink);
        awaitCommand("scanStart");
        bridge.feed("{\"event\":\"scanResult\",\"address\":\"aa:01\","
                + "\"name\":\"Heart Monitor\",\"rssi\":-42,"
                + "\"serviceUuids\":[\"" + HR_SERVICE + "\"],"
                + "\"manufacturerData\":{\"76\":\"AQI=\"},"
                + "\"serviceData\":{},\"txPower\":4}");
        await("scan sighting", () -> sink.results.size() >= 1);
        return sink;
    }

    private static Throwable errorOf(AsyncResource<?> res) {
        final AtomicReference<Throwable> out = new AtomicReference<Throwable>();
        res.except(t -> out.set(t));
        return out.get();
    }

    private static BluetoothUuid uuid(String s) {
        return BluetoothUuid.fromString(s);
    }

    @Test
    public void handshakeReportsAdapterStateAndCapabilities() {
        NativeBleBackend b = newBackend();
        List<AdapterState> states = attachStateSink(b);
        handshake(b);
        Assertions.assertTrue(states.contains(AdapterState.POWERED_ON));
        Assertions.assertTrue(b.engineSupports("descriptors"));
        Assertions.assertFalse(b.engineSupports("bonding"));
        Assertions.assertEquals(NativeBleBackend.NAME, b.getName());
        Assertions.assertTrue(b.isLeSupported());
        Assertions.assertFalse(b.isPeripheralModeSupported());
        Assertions.assertFalse(b.isClassicSupported());
        Assertions.assertFalse(b.isL2capSupported());
        Assertions.assertTrue(b.getBondedPeripherals().isEmpty());
        Assertions.assertNotNull(errorOf(b.openGattServer(null)));
        Assertions.assertNotNull(errorOf(b.openL2capServer(false)));
    }

    @Test
    public void scanDeliversResultsAndCachesCanonicalPeripherals() {
        NativeBleBackend b = newBackend();
        RecordingScanSink sink = bootAndSight(b);
        Assertions.assertNull(sink.failure.get());
        ScanResult first = sink.results.get(0);
        BlePeripheral p1 = first.getPeripheral();
        Assertions.assertEquals("aa:01", p1.getAddress());
        Assertions.assertEquals("Heart Monitor", p1.getName());
        Assertions.assertEquals(-42, first.getRssi());
        Assertions.assertEquals("Heart Monitor",
                first.getAdvertisementData().getLocalName());
        Assertions.assertTrue(first.getAdvertisementData().getServiceUuids()
                .contains(uuid(HR_SERVICE)));
        Assertions.assertArrayEquals(new byte[] {1, 2},
                first.getAdvertisementData().getManufacturerData(76));
        Assertions.assertEquals(Integer.valueOf(4),
                first.getAdvertisementData().getTxPowerLevel());
        Assertions.assertSame(p1, b.getPeripheral("aa:01"));
        Assertions.assertNull(b.getPeripheral("zz:99"));
    }

    @Test
    public void gattLifecycleOverTheNativeBridge() {
        NativeBleBackend b = newBackend();
        bootAndSight(b);
        final BlePeripheral p = b.getPeripheral("aa:01");

        final AsyncResource<BlePeripheral> connect = p.connect();
        long connectId = awaitCommand("connect");
        bridge.feed("{\"event\":\"connected\",\"requestId\":" + connectId
                + ",\"address\":\"aa:01\",\"name\":\"Heart Monitor\"}");
        await("connect completion", connect::isDone);
        Assertions.assertNull(errorOf(connect));
        // getConnectionState() flips to CONNECTED on the EDT (via the connect
        // future's completion callback), after isDone() is already true --
        // await it rather than reading it synchronously.
        await("peripheral reports connected",
                () -> p.getConnectionState() == ConnectionState.CONNECTED);
        await("connected registry populated",
                () -> b.getConnectedPeripherals(null).size() == 1);

        final AsyncResource<List<GattService>> discover =
                p.discoverServices();
        long discoverId = awaitCommand("discover");
        bridge.feed("{\"event\":\"discovered\",\"requestId\":" + discoverId
                + ",\"services\":[{\"uuid\":\"" + HR_SERVICE
                + "\",\"primary\":true,\"characteristics\":["
                + "{\"uuid\":\"" + HR_MEASUREMENT
                + "\",\"properties\":[\"read\",\"notify\"],"
                + "\"descriptors\":[{\"uuid\":\"" + CCCD + "\"}]},"
                + "{\"uuid\":\"" + HR_CONTROL
                + "\",\"properties\":[\"write\"],\"descriptors\":[]}]}]}");
        await("service discovery", discover::isDone);
        Assertions.assertNull(errorOf(discover));
        GattService hr = p.getService(uuid(HR_SERVICE));
        Assertions.assertNotNull(hr);
        GattCharacteristic measurement =
                hr.getCharacteristic(uuid(HR_MEASUREMENT));
        Assertions.assertNotNull(measurement);
        Assertions.assertTrue(measurement.canRead());
        Assertions.assertTrue(measurement.canNotify());
        Assertions.assertFalse(measurement.canWrite());
        Assertions.assertNotNull(measurement.getDescriptor(uuid(CCCD)));
        Assertions.assertEquals(1,
                b.getConnectedPeripherals(uuid(HR_SERVICE)).size());

        final AsyncResource<byte[]> read = measurement.read();
        long readId = awaitCommand("read");
        bridge.feed("{\"event\":\"readResult\",\"requestId\":" + readId
                + ",\"value\":\"AQI=\"}");
        await("characteristic read", read::isDone);
        Assertions.assertArrayEquals(new byte[] {1, 2}, read.get(null));

        GattCharacteristic control = hr.getCharacteristic(uuid(HR_CONTROL));
        final AsyncResource<Boolean> write = control.write(new byte[] {9});
        long writeId = awaitCommand("write");
        bridge.feed("{\"event\":\"writeResult\",\"requestId\":" + writeId
                + "}");
        await("characteristic write", write::isDone);
        Assertions.assertEquals(Boolean.TRUE, write.get(null));

        // subscribe -> notification: arms the CCCD then delivers a value
        final RecordingNotifyListener listener = new RecordingNotifyListener();
        final AsyncResource<Boolean> subscribe = measurement.subscribe(listener);
        long subscribeId = awaitCommand("subscribe");
        bridge.feed("{\"event\":\"subscribed\",\"requestId\":" + subscribeId
                + ",\"address\":\"aa:01\"}");
        await("subscribe", subscribe::isDone);
        Assertions.assertNull(errorOf(subscribe));
        bridge.feed("{\"event\":\"notification\",\"address\":\"aa:01\","
                + "\"service\":\"" + HR_SERVICE + "\",\"characteristic\":\""
                + HR_MEASUREMENT + "\",\"value\":\"AQI=\"}");
        await("notification delivered", () -> !listener.values.isEmpty());
        Assertions.assertArrayEquals(new byte[] {1, 2},
                listener.values.get(0));

        p.disconnect();
        long disconnectId = awaitCommand("disconnect");
        bridge.feed("{\"event\":\"disconnected\",\"requestId\":"
                + disconnectId + ",\"address\":\"aa:01\",\"reason\":\"\"}");
        await("disconnect", () -> p.getConnectionState()
                == ConnectionState.DISCONNECTED);
        await("connected registry empties",
                () -> b.getConnectedPeripherals(null).isEmpty());
    }

    @Test
    public void engineCrashFailsInFlightOpsTypedAndKillsTheBackend() {
        NativeBleBackend b = newBackend();
        List<AdapterState> states = attachStateSink(b);
        handshake(b);
        // cache aa:01 via a sighting so we can connect
        b.startScan(new RecordingScanSink());
        awaitCommand("scanStart");
        bridge.feed("{\"event\":\"scanResult\",\"address\":\"aa:01\","
                + "\"name\":\"HR\",\"rssi\":-42}");
        await("sighting", () -> b.getPeripheral("aa:01") != null);
        final BlePeripheral p = b.getPeripheral("aa:01");

        final AsyncResource<BlePeripheral> connect = p.connect();
        awaitCommand("connect");
        // the engine dies mid-flight without answering
        bridge.crash();
        await("connect failure after crash", connect::isDone);
        Throwable failure = errorOf(connect);
        Assertions.assertTrue(failure instanceof BluetoothException,
                "expected a typed BluetoothException, got " + failure);
        Assertions.assertEquals(BluetoothError.IO_ERROR,
                ((BluetoothException) failure).getError());
        // The connect future errors from the reader thread, but the CONNECTING
        // -> DISCONNECTED transition is delivered on the EDT, so await it
        // rather than reading it synchronously.
        await("peripheral disconnects after crash",
                () -> p.getConnectionState() == ConnectionState.DISCONNECTED);
        await("adapter reports UNSUPPORTED",
                () -> b.getAdapterState() == AdapterState.UNSUPPORTED);
        Assertions.assertTrue(states.contains(AdapterState.UNSUPPORTED));
        // a crashed engine stays dead: follow-up ops fail typed, no restart
        final AsyncResource<BlePeripheral> again = p.connect();
        await("post-crash connect failure", again::isDone);
        Assertions.assertTrue(errorOf(again) instanceof BluetoothException);
    }

    @Test
    public void shutdownFailsInFlightOpsInsteadOfHanging() {
        NativeBleBackend b = newBackend();
        attachStateSink(b);
        handshake(b);
        b.startScan(new RecordingScanSink());
        awaitCommand("scanStart");
        bridge.feed("{\"event\":\"scanResult\",\"address\":\"aa:01\","
                + "\"name\":\"HR\",\"rssi\":-42}");
        await("sighting", () -> b.getPeripheral("aa:01") != null);
        final BlePeripheral p = b.getPeripheral("aa:01");
        final AsyncResource<BlePeripheral> connect = p.connect();
        awaitCommand("connect");
        Assertions.assertFalse(connect.isDone());
        b.shutdown();
        await("in-flight connect fails on shutdown", connect::isDone);
        Throwable failure = errorOf(connect);
        Assertions.assertTrue(failure instanceof BluetoothException);
        Assertions.assertEquals(BluetoothError.IO_ERROR,
                ((BluetoothException) failure).getError());
    }

    @Test
    public void rssiUnsupportedFallsBackToLastScanSighting() {
        NativeBleBackend b = newBackend();
        bootAndSight(b);
        final BlePeripheral p = b.getPeripheral("aa:01");
        final AsyncResource<BlePeripheral> connect = p.connect();
        long connectId = awaitCommand("connect");
        bridge.feed("{\"event\":\"connected\",\"requestId\":" + connectId
                + ",\"address\":\"aa:01\"}");
        await("connect", connect::isDone);
        await("connected", () -> p.getConnectionState()
                == ConnectionState.CONNECTED);

        final AsyncResource<Integer> rssi = p.readRssi();
        long rssiId = awaitCommand("readRssi");
        bridge.feed("{\"event\":\"error\",\"requestId\":" + rssiId
                + ",\"code\":\"notSupported\",\"message\":\"no rssi\"}");
        await("rssi fallback", rssi::isDone);
        // the engine answered notSupported; the backend falls back to the -42
        // sighting recorded during the scan
        Assertions.assertEquals(Integer.valueOf(-42), rssi.get(null));
    }
}
