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

import com.codename1.bluetooth.AdapterState;
import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattService;
import com.codename1.bluetooth.helper.BleBackend;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.ConnectionState;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Drives {@link NativeBleBackend} against a real subprocess running the
 * scripted {@link FakeBleHelper}, so the reader/writer threads, the
 * request/terminal-event correlation, the crash handling and the shutdown
 * grace path are all exercised deterministically -- no Bluetooth hardware,
 * no OS permissions.
 */
public class NativeBleBackendFakeHelperTest {

    private static final long TIMEOUT_MS = 30000;

    private NativeBleBackend backend;

    @AfterEach
    void tearDown() {
        if (backend != null) {
            backend.shutdown();
            backend = null;
        }
    }

    private interface Condition {
        boolean isMet();
    }

    private static void await(String what, Condition condition) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.isMet()) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                Assertions.fail("interrupted while waiting for " + what);
            }
        }
        Assertions.fail("timed out waiting for " + what);
    }

    static List<String> fakeHelperCommand(String scenario) {
        String exe = File.separatorChar == '\\' ? "java.exe" : "java";
        File javaBin = new File(new File(
                System.getProperty("java.home"), "bin"), exe);
        String classpath;
        try {
            classpath = new File(FakeBleHelper.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI())
                    .getAbsolutePath();
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "cannot locate test-classes for FakeBleHelper", ex);
        }
        return Arrays.asList(javaBin.getAbsolutePath(), "-cp", classpath,
                FakeBleHelper.class.getName(), scenario);
    }

    private NativeBleBackend start(String scenario) {
        backend = new NativeBleBackend(fakeHelperCommand(scenario));
        Assertions.assertTrue(backend.isAvailable());
        return backend;
    }

    /** Records adapter transitions and boots the helper (like installBackend). */
    private List<AdapterState> attachStateSink(final NativeBleBackend b) {
        final List<AdapterState> states =
                new CopyOnWriteArrayList<AdapterState>();
        b.setAdapterStateSink(new BleBackend.AdapterStateSink() {
            public void adapterStateChanged(AdapterState newState) {
                states.add(newState);
            }
        });
        return states;
    }

    private static final class RecordingScanSink
            implements BleBackend.ScanSink {
        final List<ScanResult> results =
                new CopyOnWriteArrayList<ScanResult>();
        volatile BluetoothException failure;

        public void onResult(ScanResult result) {
            results.add(result);
        }

        public void onFailed(BluetoothException reason) {
            failure = reason;
        }
    }

    /** Scans until the two scripted peripherals were sighted. */
    private RecordingScanSink scanUntilSighted(final NativeBleBackend b) {
        final RecordingScanSink sink = new RecordingScanSink();
        b.startScan(sink);
        await("two scan sightings", new Condition() {
            public boolean isMet() {
                return sink.results.size() >= 2;
            }
        });
        return sink;
    }

    private static Throwable errorOf(AsyncResource<?> res) {
        final AtomicReference<Throwable> out =
                new AtomicReference<Throwable>();
        res.except(t -> out.set(t));
        return out.get();
    }

    @Test
    public void handshakeReportsAdapterStateAndCapabilities() {
        NativeBleBackend b = start("happy");
        final List<AdapterState> states = attachStateSink(b);
        await("poweredOn handshake", new Condition() {
            public boolean isMet() {
                return b.getAdapterState() == AdapterState.POWERED_ON;
            }
        });
        Assertions.assertTrue(states.contains(AdapterState.POWERED_ON));
        Assertions.assertTrue(b.helperSupports("descriptors"));
        Assertions.assertFalse(b.helperSupports("bonding"));
        // capability caps of the native backend
        Assertions.assertTrue(b.isLeSupported());
        Assertions.assertFalse(b.isPeripheralModeSupported());
        Assertions.assertFalse(b.isClassicSupported());
        Assertions.assertFalse(b.isL2capSupported());
        Assertions.assertTrue(b.getBondedPeripherals().isEmpty());
        Assertions.assertNotNull(
                errorOf(b.openGattServer(null)));
        Assertions.assertNotNull(errorOf(b.openL2capServer(false)));
    }

    @Test
    public void scanDeliversResultsAndCachesCanonicalPeripherals() {
        NativeBleBackend b = start("happy");
        RecordingScanSink sink = scanUntilSighted(b);
        Assertions.assertNull(sink.failure);
        ScanResult first = sink.results.get(0);
        BlePeripheral p1 = first.getPeripheral();
        Assertions.assertEquals("aa:01", p1.getAddress());
        Assertions.assertEquals("Heart Monitor", p1.getName());
        Assertions.assertEquals(-42, first.getRssi());
        Assertions.assertEquals("Heart Monitor",
                first.getAdvertisementData().getLocalName());
        Assertions.assertTrue(first.getAdvertisementData().getServiceUuids()
                .contains(BluetoothUuid.fromString(
                        FakeBleHelper.HR_SERVICE)));
        Assertions.assertArrayEquals(new byte[] {1, 2},
                first.getAdvertisementData().getManufacturerData(76));
        Assertions.assertEquals(Integer.valueOf(4),
                first.getAdvertisementData().getTxPowerLevel());
        // canonical cache: the scan peripheral IS the lookup peripheral
        Assertions.assertSame(p1, b.getPeripheral("aa:01"));
        Assertions.assertNotNull(b.getPeripheral("aa:02"));
        Assertions.assertNull(b.getPeripheral("zz:99"));
        b.stopScan();
    }

    @Test
    public void gattLifecycleAgainstScriptedHelper() {
        NativeBleBackend b = start("happy");
        scanUntilSighted(b);
        final BlePeripheral p = b.getPeripheral("aa:01");

        final AsyncResource<BlePeripheral> connect = p.connect();
        await("connect completion", new Condition() {
            public boolean isMet() {
                return connect.isDone();
            }
        });
        Assertions.assertNull(errorOf(connect));
        Assertions.assertEquals(ConnectionState.CONNECTED,
                p.getConnectionState());
        Assertions.assertEquals(1,
                b.getConnectedPeripherals(null).size());
        Assertions.assertTrue(b.getConnectedPeripherals(
                BluetoothUuid.fromString(FakeBleHelper.HR_SERVICE))
                .isEmpty()); // services not yet discovered

        final AsyncResource<List<GattService>> discover =
                p.discoverServices();
        await("service discovery", new Condition() {
            public boolean isMet() {
                return discover.isDone();
            }
        });
        Assertions.assertNull(errorOf(discover));
        GattService hr = p.getService(
                BluetoothUuid.fromString(FakeBleHelper.HR_SERVICE));
        Assertions.assertNotNull(hr);
        GattCharacteristic measurement = hr.getCharacteristic(
                BluetoothUuid.fromString(FakeBleHelper.HR_MEASUREMENT));
        Assertions.assertNotNull(measurement);
        Assertions.assertTrue(measurement.canRead());
        Assertions.assertTrue(measurement.canNotify());
        Assertions.assertFalse(measurement.canWrite());
        Assertions.assertNotNull(measurement.getDescriptor(
                BluetoothUuid.fromString(FakeBleHelper.CCCD)));
        // with the discovered database the service filter now matches
        Assertions.assertEquals(1, b.getConnectedPeripherals(
                BluetoothUuid.fromString(FakeBleHelper.HR_SERVICE)).size());

        final AsyncResource<byte[]> read = measurement.read();
        await("characteristic read", new Condition() {
            public boolean isMet() {
                return read.isDone();
            }
        });
        Assertions.assertArrayEquals(new byte[] {1, 2}, read.get(null));

        GattCharacteristic control = hr.getCharacteristic(
                BluetoothUuid.fromString(FakeBleHelper.HR_CONTROL));
        final AsyncResource<Boolean> write = control.write(
                new byte[] {9});
        await("characteristic write", new Condition() {
            public boolean isMet() {
                return write.isDone();
            }
        });
        Assertions.assertEquals(Boolean.TRUE, write.get(null));

        final AsyncResource<byte[]> descRead = measurement.getDescriptor(
                BluetoothUuid.fromString(FakeBleHelper.CCCD)).read();
        await("descriptor read", new Condition() {
            public boolean isMet() {
                return descRead.isDone();
            }
        });
        Assertions.assertArrayEquals(new byte[] {(byte) 0x92},
                descRead.get(null));

        // arm notifications: completes when the helper answers
        // "subscribed"; the fake's follow-up notification targets the
        // control characteristic, which has no listeners, so the routing
        // path is exercised without any EDT dispatch
        com.codename1.bluetooth.gatt.GattNotificationListener listener =
                (c, v) -> { };
        final AsyncResource<Boolean> sub = p.subscribe(measurement,
                listener);
        await("subscription arming", new Condition() {
            public boolean isMet() {
                return sub.isDone();
            }
        });
        Assertions.assertEquals(Boolean.TRUE, sub.get(null));
        Assertions.assertTrue(p.isSubscribed(measurement));
        final AsyncResource<Boolean> unsub = p.unsubscribe(measurement,
                listener);
        await("subscription disarming", new Condition() {
            public boolean isMet() {
                return unsub.isDone();
            }
        });
        Assertions.assertFalse(p.isSubscribed(measurement));

        final AsyncResource<Integer> rssi = p.readRssi();
        await("rssi read", new Condition() {
            public boolean isMet() {
                return rssi.isDone();
            }
        });
        Assertions.assertEquals(Integer.valueOf(-55), rssi.get(null));

        final AsyncResource<Integer> mtu = p.requestMtu(185);
        await("mtu request", new Condition() {
            public boolean isMet() {
                return mtu.isDone();
            }
        });
        Assertions.assertEquals(Integer.valueOf(23), mtu.get(null));

        final AsyncResource<Boolean> bond = p.createBond();
        await("bond attempt", new Condition() {
            public boolean isMet() {
                return bond.isDone();
            }
        });
        Throwable bondFailure = errorOf(bond);
        Assertions.assertTrue(bondFailure instanceof BluetoothException);
        Assertions.assertEquals(BluetoothError.BOND_FAILED,
                ((BluetoothException) bondFailure).getError());

        Throwable l2cap = errorOf(p.openL2capChannel(0x80, false));
        Assertions.assertTrue(l2cap instanceof BluetoothException);
        Assertions.assertEquals(BluetoothError.NOT_SUPPORTED,
                ((BluetoothException) l2cap).getError());

        p.disconnect();
        await("disconnect", new Condition() {
            public boolean isMet() {
                return p.getConnectionState()
                        == ConnectionState.DISCONNECTED;
            }
        });
        await("connected registry empties", new Condition() {
            public boolean isMet() {
                return b.getConnectedPeripherals(null).isEmpty();
            }
        });
    }

    @Test
    public void helperCrashFailsInFlightOpsTypedAndKillsTheBackend() {
        NativeBleBackend b = start("crash-on-connect");
        final List<AdapterState> states = attachStateSink(b);
        scanUntilSighted(b);
        final BlePeripheral p = b.getPeripheral("aa:01");

        final AsyncResource<BlePeripheral> connect = p.connect();
        await("connect failure after crash", new Condition() {
            public boolean isMet() {
                return connect.isDone();
            }
        });
        Throwable failure = errorOf(connect);
        Assertions.assertTrue(failure instanceof BluetoothException,
                "expected a typed BluetoothException, got " + failure);
        Assertions.assertEquals(BluetoothError.IO_ERROR,
                ((BluetoothException) failure).getError());
        Assertions.assertEquals(ConnectionState.DISCONNECTED,
                p.getConnectionState());
        await("adapter reports UNSUPPORTED", new Condition() {
            public boolean isMet() {
                return b.getAdapterState() == AdapterState.UNSUPPORTED;
            }
        });
        Assertions.assertTrue(states.contains(AdapterState.UNSUPPORTED));
        // a crashed helper stays dead: follow-up ops fail typed, no restart
        final AsyncResource<BlePeripheral> again = p.connect();
        await("post-crash connect failure", new Condition() {
            public boolean isMet() {
                return again.isDone();
            }
        });
        Assertions.assertTrue(errorOf(again) instanceof BluetoothException);
    }

    @Test
    public void shutdownFailsInFlightOpsInsteadOfHanging() {
        NativeBleBackend b = start("hang-on-connect");
        scanUntilSighted(b);
        final BlePeripheral p = b.getPeripheral("aa:01");
        final AsyncResource<BlePeripheral> connect = p.connect();
        Assertions.assertFalse(connect.isDone());
        b.shutdown();
        await("in-flight connect fails on shutdown", new Condition() {
            public boolean isMet() {
                return connect.isDone();
            }
        });
        Throwable failure = errorOf(connect);
        Assertions.assertTrue(failure instanceof BluetoothException);
        Assertions.assertEquals(BluetoothError.IO_ERROR,
                ((BluetoothException) failure).getError());
    }

    @Test
    public void rssiUnsupportedFallsBackToLastScanSighting() {
        NativeBleBackend b = start("rssi-unsupported");
        scanUntilSighted(b);
        final BlePeripheral p = b.getPeripheral("aa:01");
        final AsyncResource<BlePeripheral> connect = p.connect();
        await("connect", new Condition() {
            public boolean isMet() {
                return connect.isDone();
            }
        });
        final AsyncResource<Integer> rssi = p.readRssi();
        await("rssi fallback", new Condition() {
            public boolean isMet() {
                return rssi.isDone();
            }
        });
        // the helper answered notSupported; the backend falls back to the
        // -42 sighting recorded during the scan
        Assertions.assertEquals(Integer.valueOf(-42), rssi.get(null));
    }
}
