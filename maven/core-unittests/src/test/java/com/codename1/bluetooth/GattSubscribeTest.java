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
import com.codename1.bluetooth.le.ConnectionState;
import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.codename1.bluetooth.BtTestUtil.bytes;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Notification subscription management: the CCCD is armed exactly once on
 * the zero-to-one listener transition, additional listeners piggyback,
 * values fan out to every listener on the EDT, the last unsubscribe disarms,
 * indications are chosen for indicate-only characteristics, and disconnect
 * clears the armed state so a re-subscribe after reconnect re-arms.
 */
class GattSubscribeTest extends UITestBase {

    private static final BluetoothUuid SVC = BluetoothUuid.fromShort(0x180D);

    private FakeBlePeripheral p;
    private GattCharacteristic notifying;
    private GattCharacteristic indicateOnly;
    private GattCharacteristic both;

    @BeforeEach
    void connectPeripheral() {
        p = new FakeBlePeripheral("AA:BB:CC:DD:EE:02", "notif");
        GattService s = p.buildService(SVC);
        notifying = p.buildCharacteristic(s, BluetoothUuid.fromShort(0x2A37),
                GattCharacteristic.PROPERTY_NOTIFY);
        indicateOnly = p.buildCharacteristic(s,
                BluetoothUuid.fromShort(0x2A38),
                GattCharacteristic.PROPERTY_INDICATE);
        both = p.buildCharacteristic(s, BluetoothUuid.fromShort(0x2A39),
                GattCharacteristic.PROPERTY_NOTIFY
                        | GattCharacteristic.PROPERTY_INDICATE);
        p.connectNow();
    }

    @Test
    void firstSubscribeArmsExactlyOnceAndSecondListenerPiggybacks() {
        List<byte[]> got1 = new ArrayList<byte[]>();
        List<byte[]> got2 = new ArrayList<byte[]>();

        AsyncResource<Boolean> s1 = p.subscribe(notifying,
                (c, v) -> got1.add(v));
        assertEquals(1, p.pendingCount(),
                "first subscribe triggers the CCCD write");
        FakeBlePeripheral.PendingOp arm = p.peekNext();
        assertEquals(FakeBlePeripheral.OpKind.SET_NOTIFICATIONS, arm.kind);
        assertSame(notifying, arm.characteristic);
        assertTrue(arm.enable);
        assertFalse(arm.indication,
                "a NOTIFY characteristic arms notifications");
        assertFalse(s1.isDone(), "not armed until the platform confirms");
        assertFalse(p.isSubscribed(notifying));

        // a second listener while the arm is still in flight shares it
        AsyncResource<Boolean> s2 = p.subscribe(notifying,
                (c, v) -> got2.add(v));
        assertEquals(1, p.pendingCount(),
                "no second CCCD write for the second listener");
        assertFalse(s2.isDone());

        p.completeNext(Boolean.TRUE);
        assertEquals(Boolean.TRUE, s1.get());
        assertEquals(Boolean.TRUE, s2.get());
        assertTrue(p.isSubscribed(notifying));
        assertTrue(notifying.isSubscribed());

        // a third listener after arming resolves immediately, still no write
        AsyncResource<Boolean> s3 = p.subscribe(notifying, (c, v) -> { });
        assertTrue(s3.isDone());
        assertEquals(Boolean.TRUE, s3.get());
        assertEquals(0, p.pendingCount());
    }

    @Test
    void notificationsFanOutToEveryListenerOnTheEdt() {
        List<byte[]> got1 = new ArrayList<byte[]>();
        List<byte[]> got2 = new ArrayList<byte[]>();
        p.subscribe(notifying, (c, v) -> got1.add(v));
        p.subscribe(notifying, (c, v) -> got2.add(v));
        p.completeNext(Boolean.TRUE);

        p.notifyValue(notifying, bytes(0x42, 0x43));
        flushSerialCalls();

        assertEquals(1, got1.size());
        assertEquals(1, got2.size());
        assertArrayEquals(bytes(0x42, 0x43), got1.get(0));
        assertArrayEquals(bytes(0x42, 0x43), got2.get(0));
    }

    @Test
    void unsubscribeOfOneListenerKeepsNotificationsArmed() {
        List<byte[]> got1 = new ArrayList<byte[]>();
        List<byte[]> got2 = new ArrayList<byte[]>();
        com.codename1.bluetooth.gatt.GattNotificationListener l1 =
                (c, v) -> got1.add(v);
        p.subscribe(notifying, l1);
        p.subscribe(notifying, (c, v) -> got2.add(v));
        p.completeNext(Boolean.TRUE);

        AsyncResource<Boolean> u = p.unsubscribe(notifying, l1);
        assertTrue(u.isDone());
        assertTrue(p.isSubscribed(notifying), "one listener remains armed");
        assertEquals(0, p.pendingCount(), "no CCCD disarm yet");

        p.notifyValue(notifying, bytes(0x01));
        flushSerialCalls();
        assertEquals(0, got1.size(),
                "removed listener no longer receives values");
        assertEquals(1, got2.size());
    }

    @Test
    void lastUnsubscribeDisarmsNotifications() {
        com.codename1.bluetooth.gatt.GattNotificationListener l1 =
                (c, v) -> { };
        com.codename1.bluetooth.gatt.GattNotificationListener l2 =
                (c, v) -> { };
        p.subscribe(notifying, l1);
        p.subscribe(notifying, l2);
        p.completeNext(Boolean.TRUE);

        p.unsubscribe(notifying, l1);
        assertEquals(0, p.pendingCount());

        AsyncResource<Boolean> u = p.unsubscribe(notifying, l2);
        assertEquals(1, p.pendingCount(),
                "last unsubscribe writes the CCCD disable");
        FakeBlePeripheral.PendingOp disarm = p.peekNext();
        assertEquals(FakeBlePeripheral.OpKind.SET_NOTIFICATIONS, disarm.kind);
        assertFalse(disarm.enable);
        assertFalse(p.isSubscribed(notifying));
        p.completeNext(Boolean.TRUE);
        assertEquals(Boolean.TRUE, u.get());
    }

    @Test
    void indicationIsChosenWhenTheCharacteristicOnlySupportsIndicate() {
        p.subscribe(indicateOnly, (c, v) -> { });
        FakeBlePeripheral.PendingOp arm = p.peekNext();
        assertTrue(arm.enable);
        assertTrue(arm.indication,
                "an INDICATE-only characteristic arms indications");
        p.completeNext(Boolean.TRUE);
    }

    @Test
    void notificationIsPreferredWhenBothModesAreSupported() {
        p.subscribe(both, (c, v) -> { });
        assertFalse(p.peekNext().indication);
        p.completeNext(Boolean.TRUE);
    }

    @Test
    void subscribeWithoutListenerFailsWithoutTouchingTheSpi() {
        AsyncResource<Boolean> s = p.subscribe(notifying, null);
        assertTrue(s.isDone());
        assertNotNull(BtTestUtil.errorOf(s));
        assertEquals(0, p.pendingCount());
    }

    @Test
    void failedArmSurfacesToEveryWaiterAndAllowsRetry() {
        AsyncResource<Boolean> s1 = p.subscribe(notifying, (c, v) -> { });
        AsyncResource<Boolean> s2 = p.subscribe(notifying, (c, v) -> { });
        p.failNext(new BluetoothException(BluetoothError.GATT_ERROR,
                "CCCD write rejected", 0x03));

        BtTestUtil.assertFailedWith(s1, BluetoothError.GATT_ERROR);
        BtTestUtil.assertFailedWith(s2, BluetoothError.GATT_ERROR);
        assertFalse(p.isSubscribed(notifying));
    }

    @Test
    void disconnectClearsArmedStateAndResubscribeAfterReconnectRearms() {
        List<byte[]> got = new ArrayList<byte[]>();
        com.codename1.bluetooth.gatt.GattNotificationListener l =
                (c, v) -> got.add(v);
        p.subscribe(notifying, l);
        p.completeNext(Boolean.TRUE);
        assertTrue(p.isSubscribed(notifying));

        p.fireState(ConnectionState.DISCONNECTED, new BluetoothException(
                BluetoothError.CONNECTION_LOST, "gone"));
        assertFalse(p.isSubscribed(notifying),
                "disconnect must clear the armed state");
        assertFalse(notifying.isSubscribed());

        // reconnect and re-subscribe: a fresh CCCD write is required
        p.connectNow();
        AsyncResource<Boolean> again = p.subscribe(notifying, l);
        assertEquals(1, p.pendingCount(),
                "re-subscribe after reconnect must re-arm");
        FakeBlePeripheral.PendingOp arm = p.peekNext();
        assertEquals(FakeBlePeripheral.OpKind.SET_NOTIFICATIONS, arm.kind);
        assertTrue(arm.enable);
        p.completeNext(Boolean.TRUE);
        assertEquals(Boolean.TRUE, again.get());
        assertTrue(p.isSubscribed(notifying));

        p.notifyValue(notifying, bytes(0x09));
        flushSerialCalls();
        assertEquals(1, got.size());
    }
}
