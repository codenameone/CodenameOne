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
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.ConnectionEvent;
import com.codename1.bluetooth.le.ConnectionState;
import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.codename1.bluetooth.BtTestUtil.assertFailedWith;
import static com.codename1.bluetooth.BtTestUtil.bytes;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@link BlePeripheral} connection lifecycle and GATT-database caching:
 * state transitions and their events, connect-coalescing, fail-fast of
 * operations while disconnected, service discovery caching/invalidations,
 * and the fate of in-flight and queued operations on disconnect.
 */
class GattConnectionTest extends UITestBase {

    private static final BluetoothUuid SVC = BluetoothUuid.fromShort(0x180D);
    private static final BluetoothUuid CHR = BluetoothUuid.fromShort(0x2A37);

    private FakeBlePeripheral p;
    private List<ConnectionEvent> events;

    @BeforeEach
    void createPeripheral() {
        p = new FakeBlePeripheral("AA:BB:CC:DD:EE:FF", "HRM");
        events = new ArrayList<ConnectionEvent>();
        p.addConnectionListener(events::add);
    }

    private List<GattService> discoveredDb(GattCharacteristic[] chrOut) {
        GattService s = p.buildService(SVC);
        chrOut[0] = p.buildCharacteristic(s, CHR,
                GattCharacteristic.PROPERTY_READ
                        | GattCharacteristic.PROPERTY_WRITE);
        return Arrays.asList(s);
    }

    @Test
    void connectMovesThroughConnectingToConnectedAndFiresBothEvents() {
        assertEquals(ConnectionState.DISCONNECTED, p.getConnectionState());
        AsyncResource<BlePeripheral> r = p.connect();
        assertEquals(ConnectionState.CONNECTING, p.getConnectionState());
        assertFalse(r.isDone());
        FakeBlePeripheral.PendingOp op = p.peekNext();
        assertEquals(FakeBlePeripheral.OpKind.CONNECT, op.kind);

        p.completeNext(p);
        assertEquals(ConnectionState.CONNECTED, p.getConnectionState());
        assertTrue(r.isDone());
        assertSame(p, r.get());

        flushSerialCalls();
        assertEquals(2, events.size());
        assertEquals(ConnectionState.CONNECTING, events.get(0).getState());
        assertEquals(ConnectionState.CONNECTED, events.get(1).getState());
        assertSame(p, events.get(1).getPeripheral());
        assertNull(events.get(1).getReason());
    }

    @Test
    void connectWhileConnectingReturnsTheSameAttemptHandle() {
        AsyncResource<BlePeripheral> first = p.connect();
        AsyncResource<BlePeripheral> second = p.connect();
        assertSame(first, second);
        assertEquals(1, p.pendingCount(),
                "only one platform connect attempt may be started");
        p.completeNext(p);
        assertSame(p, first.get());
    }

    @Test
    void connectWhenConnectedResolvesImmediately() {
        p.connectNow();
        AsyncResource<BlePeripheral> again = p.connect();
        assertTrue(again.isDone());
        assertSame(p, again.get());
        assertEquals(0, p.pendingCount(),
                "no new platform attempt when already connected");
    }

    @Test
    void connectFailureSurfacesTheErrorAndReturnsToDisconnected() {
        AsyncResource<BlePeripheral> r = p.connect();
        p.failNext(new BluetoothException(BluetoothError.CONNECTION_FAILED,
                "unreachable"));
        assertFailedWith(r, BluetoothError.CONNECTION_FAILED);
        assertEquals(ConnectionState.DISCONNECTED, p.getConnectionState());
    }

    @Test
    void disconnectMovesThroughDisconnectingAndFailsNothingWhenIdle() {
        p.connectNow();
        p.disconnect();
        assertEquals(ConnectionState.DISCONNECTING, p.getConnectionState());
        FakeBlePeripheral.PendingOp op = p.completeNext(null);
        assertEquals(FakeBlePeripheral.OpKind.DISCONNECT, op.kind);
        assertEquals(ConnectionState.DISCONNECTED, p.getConnectionState());

        flushSerialCalls();
        assertEquals(4, events.size());
        assertEquals(ConnectionState.DISCONNECTING, events.get(2).getState());
        assertEquals(ConnectionState.DISCONNECTED, events.get(3).getState());

        // a second disconnect while already disconnected is a no-op
        p.disconnect();
        assertEquals(0, p.pendingCount());
    }

    @Test
    void disconnectDuringConnectAbortsTheAttemptWithUserCanceled() {
        AsyncResource<BlePeripheral> r = p.connect();
        p.disconnect();
        assertFailedWith(r, BluetoothError.USER_CANCELED);
        assertEquals(ConnectionState.DISCONNECTED, p.getConnectionState());
    }

    @Test
    void operationsBeforeConnectFailNotConnectedWithoutTouchingTheSpi() {
        GattCharacteristic[] chr = new GattCharacteristic[1];
        discoveredDb(chr);
        assertFailedWith(p.discoverServices(), BluetoothError.NOT_CONNECTED);
        assertFailedWith(p.readCharacteristic(chr[0]),
                BluetoothError.NOT_CONNECTED);
        assertFailedWith(p.writeCharacteristic(chr[0], bytes(1), true),
                BluetoothError.NOT_CONNECTED);
        assertFailedWith(p.readRssi(), BluetoothError.NOT_CONNECTED);
        assertFailedWith(p.requestMtu(185), BluetoothError.NOT_CONNECTED);
        assertFailedWith(p.subscribe(chr[0], (c, v) -> { }),
                BluetoothError.NOT_CONNECTED);
        assertEquals(0, p.pendingCount(),
                "fail-fast paths must not reach the platform SPI");
    }

    @Test
    void discoverServicesCachesTheDatabase() {
        p.connectNow();
        GattCharacteristic[] chr = new GattCharacteristic[1];
        List<GattService> db = discoveredDb(chr);

        assertTrue(p.getServices().isEmpty(), "no cache before discovery");
        AsyncResource<List<GattService>> r = p.discoverServices();
        assertEquals(FakeBlePeripheral.OpKind.DISCOVER_SERVICES,
                p.peekNext().kind);
        p.completeNext(db);

        assertTrue(r.isDone());
        assertEquals(1, r.get().size());
        assertEquals(1, p.getServices().size());
        assertEquals(SVC, p.getServices().get(0).getUuid());
        assertNotNull(p.getService(SVC));
        assertNull(p.getService(BluetoothUuid.fromShort(0x1800)));
        assertSame(chr[0], p.getCharacteristic(SVC, CHR));
        assertNull(p.getCharacteristic(SVC, BluetoothUuid.fromShort(0x2A38)));
        assertNull(p.getCharacteristic(BluetoothUuid.fromShort(0x1800), CHR));
    }

    @Test
    void servicesInvalidationClearsTheCache() {
        p.connectNow();
        GattCharacteristic[] chr = new GattCharacteristic[1];
        p.discoverServices();
        p.completeNext(discoveredDb(chr));
        assertEquals(1, p.getServices().size());

        p.invalidateServices();
        assertTrue(p.getServices().isEmpty());
        assertNull(p.getService(SVC));
        assertNull(p.getCharacteristic(SVC, CHR));
    }

    @Test
    void appRequestedDisconnectFailsInFlightAndQueuedOpsNotConnected() {
        p.connectNow();
        GattCharacteristic[] chr = new GattCharacteristic[1];
        p.discoverServices();
        p.completeNext(discoveredDb(chr));

        AsyncResource<byte[]> inFlight = p.readCharacteristic(chr[0]);
        AsyncResource<Integer> queued = p.readRssi();
        assertEquals(1, p.pendingCount(),
                "the queue serializes: only the first op reaches the SPI");

        p.disconnect();
        // drain the abandoned in-flight read, then complete the disconnect
        assertEquals(FakeBlePeripheral.OpKind.READ_CHARACTERISTIC,
                p.takeNext().kind);
        p.completeNext(null);

        assertFailedWith(inFlight, BluetoothError.NOT_CONNECTED);
        assertFailedWith(queued, BluetoothError.NOT_CONNECTED);
    }

    @Test
    void linkLossFailsInFlightAndQueuedOpsWithConnectionLost() {
        p.connectNow();
        GattCharacteristic[] chr = new GattCharacteristic[1];
        p.discoverServices();
        p.completeNext(discoveredDb(chr));

        AsyncResource<byte[]> inFlight = p.readCharacteristic(chr[0]);
        AsyncResource<Boolean> queued =
                p.writeCharacteristic(chr[0], bytes(7), true);

        p.fireState(ConnectionState.DISCONNECTED, new BluetoothException(
                BluetoothError.CONNECTION_LOST, "link supervision timeout"));

        assertFailedWith(inFlight, BluetoothError.CONNECTION_LOST);
        assertFailedWith(queued, BluetoothError.CONNECTION_LOST);
        assertEquals(ConnectionState.DISCONNECTED, p.getConnectionState());

        flushSerialCalls();
        ConnectionEvent last = events.get(events.size() - 1);
        assertEquals(ConnectionState.DISCONNECTED, last.getState());
        assertNotNull(last.getReason());
        assertEquals(BluetoothError.CONNECTION_LOST,
                last.getReason().getError());
    }

    @Test
    void reconnectAfterDisconnectWorks() {
        p.connectNow();
        p.disconnect();
        p.completeNext(null);
        assertEquals(ConnectionState.DISCONNECTED, p.getConnectionState());

        AsyncResource<BlePeripheral> r = p.connectNow();
        assertSame(p, r.get());
        assertEquals(ConnectionState.CONNECTED, p.getConnectionState());
    }

    @Test
    void mtuIsTrackedFromSuccessfulRequests() {
        p.connectNow();
        assertEquals(23, p.getMtu(), "BLE default MTU");
        AsyncResource<Integer> r = p.requestMtu(247);
        FakeBlePeripheral.PendingOp op = p.peekNext();
        assertEquals(FakeBlePeripheral.OpKind.REQUEST_MTU, op.kind);
        assertEquals(247, op.intArg);
        p.completeNext(Integer.valueOf(185));
        assertEquals(Integer.valueOf(185), r.get());
        assertEquals(185, p.getMtu());
    }
}
