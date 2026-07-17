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

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothUuid;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * The GATT client path against a virtual peripheral: discovery,
 * characteristic/descriptor round trips, subscriptions with pushed
 * notifications, link loss and the auxiliary rssi/mtu/bond operations.
 */
public class VirtualGattTest extends AbstractVirtualStackTest {

    private static final String ADDR = "AA:BB:CC:DD:EE:01";

    private static final class RecordingSink
            implements SimulatedBluetoothStack.PeripheralSink {
        final List<byte[]> notifications = new ArrayList<>();
        final List<BluetoothUuid> notifiedCharacteristics = new ArrayList<>();
        BluetoothError lost;
        String lostMessage;

        public void onNotification(BluetoothUuid serviceUuid,
                BluetoothUuid characteristicUuid, byte[] value) {
            notifiedCharacteristics.add(characteristicUuid);
            notifications.add(value);
        }

        public void onConnectionLost(BluetoothError error, String message) {
            lost = error;
            lostMessage = message;
        }
    }

    @Test
    public void discoverySeesTheVirtualDatabase() {
        stack.addPeripheral(heartRatePeripheral(ADDR));
        Result<Boolean> connect = new Result<>();
        stack.connect(ADDR, connect);
        Result<List<VirtualService>> discover = new Result<>();
        stack.discoverServices(ADDR, discover);
        settle();

        connect.assertSuccess();
        discover.assertSuccess();
        Assertions.assertEquals(1, discover.value.size());
        VirtualService s = discover.value.get(0);
        Assertions.assertEquals(HR_SERVICE, s.getUuid());
        Assertions.assertEquals(2, s.getCharacteristics().size());
        Assertions.assertEquals(1, s.getCharacteristic(HR_MEASUREMENT)
                .getDescriptors().size());
    }

    @Test
    public void characteristicReadWriteRoundTrip() {
        VirtualPeripheral p = heartRatePeripheral(ADDR);
        stack.addPeripheral(p);
        connectAndDiscover(ADDR);

        Result<byte[]> read = new Result<>();
        stack.readCharacteristic(ADDR, HR_SERVICE, HR_MEASUREMENT, read);
        settle();
        read.assertSuccess();
        Assertions.assertArrayEquals(new byte[] {0, 72}, read.value);

        Result<Boolean> write = new Result<>();
        stack.writeCharacteristic(ADDR, HR_SERVICE, HR_CONTROL,
                new byte[] {9, 8, 7}, write);
        settle();
        write.assertSuccess();
        Assertions.assertArrayEquals(new byte[] {9, 8, 7},
                p.getCharacteristic(HR_SERVICE, HR_CONTROL).getValue());

        Result<byte[]> readBack = new Result<>();
        stack.readCharacteristic(ADDR, HR_SERVICE, HR_CONTROL, readBack);
        settle();
        readBack.assertSuccess();
        Assertions.assertArrayEquals(new byte[] {9, 8, 7}, readBack.value);
    }

    @Test
    public void descriptorReadWriteRoundTrip() {
        stack.addPeripheral(heartRatePeripheral(ADDR));
        connectAndDiscover(ADDR);

        Result<byte[]> read = new Result<>();
        stack.readDescriptor(ADDR, HR_SERVICE, HR_MEASUREMENT,
                USER_DESCRIPTION, read);
        settle();
        read.assertSuccess();
        Assertions.assertArrayEquals(new byte[] {'h', 'r'}, read.value);

        Result<Boolean> write = new Result<>();
        stack.writeDescriptor(ADDR, HR_SERVICE, HR_MEASUREMENT,
                USER_DESCRIPTION, new byte[] {'x'}, write);
        Result<byte[]> readBack = new Result<>();
        stack.readDescriptor(ADDR, HR_SERVICE, HR_MEASUREMENT,
                USER_DESCRIPTION, readBack);
        settle();
        write.assertSuccess();
        readBack.assertSuccess();
        Assertions.assertArrayEquals(new byte[] {'x'}, readBack.value);
    }

    @Test
    public void operationsRequireConnectionAndDiscovery() {
        stack.addPeripheral(heartRatePeripheral(ADDR));
        settle();

        Result<byte[]> notConnected = new Result<>();
        stack.readCharacteristic(ADDR, HR_SERVICE, HR_MEASUREMENT,
                notConnected);
        settle();
        notConnected.assertFailure(BluetoothError.NOT_CONNECTED);

        Result<Boolean> connect = new Result<>();
        stack.connect(ADDR, connect);
        Result<byte[]> notDiscovered = new Result<>();
        stack.readCharacteristic(ADDR, HR_SERVICE, HR_MEASUREMENT,
                notDiscovered);
        settle();
        connect.assertSuccess();
        notDiscovered.assertFailure(BluetoothError.GATT_ERROR);
    }

    @Test
    public void subscriptionDeliversPushedNotifications() {
        stack.addPeripheral(heartRatePeripheral(ADDR));
        RecordingSink sink = new RecordingSink();
        stack.setPeripheralSink(ADDR, sink);
        connectAndDiscover(ADDR);

        // a push before subscribing is dropped
        stack.pushNotification(ADDR, HR_SERVICE, HR_MEASUREMENT,
                new byte[] {1});
        settle();
        Assertions.assertTrue(sink.notifications.isEmpty());

        Result<Boolean> subscribe = new Result<>();
        stack.setNotifications(ADDR, HR_SERVICE, HR_MEASUREMENT, true,
                subscribe);
        settle();
        subscribe.assertSuccess();
        Assertions.assertTrue(stack.isSubscribed(ADDR, HR_SERVICE,
                HR_MEASUREMENT));

        stack.pushNotification(ADDR, HR_SERVICE, HR_MEASUREMENT,
                new byte[] {0, 99});
        settle();
        Assertions.assertEquals(1, sink.notifications.size());
        Assertions.assertArrayEquals(new byte[] {0, 99},
                sink.notifications.get(0));
        Assertions.assertEquals(HR_MEASUREMENT,
                sink.notifiedCharacteristics.get(0));

        // unsubscribing stops delivery
        Result<Boolean> unsubscribe = new Result<>();
        stack.setNotifications(ADDR, HR_SERVICE, HR_MEASUREMENT, false,
                unsubscribe);
        settle();
        unsubscribe.assertSuccess();
        stack.pushNotification(ADDR, HR_SERVICE, HR_MEASUREMENT,
                new byte[] {5});
        settle();
        Assertions.assertEquals(1, sink.notifications.size());
    }

    @Test
    public void remoteDisconnectFiresConnectionLost() {
        stack.addPeripheral(heartRatePeripheral(ADDR));
        RecordingSink sink = new RecordingSink();
        stack.setPeripheralSink(ADDR, sink);
        connectAndDiscover(ADDR);

        stack.disconnectFromRemote(ADDR);
        settle();
        Assertions.assertEquals(BluetoothError.CONNECTION_LOST, sink.lost);
        Assertions.assertFalse(stack.isConnected(ADDR));

        // repeated remote disconnects while down do not re-fire
        sink.lost = null;
        stack.disconnectFromRemote(ADDR);
        settle();
        Assertions.assertNull(sink.lost);
    }

    @Test
    public void rssiMtuAndBondOperations() {
        stack.addPeripheral(heartRatePeripheral(ADDR));
        connectAndDiscover(ADDR);

        Result<Integer> rssi = new Result<>();
        stack.readRssi(ADDR, rssi);
        settle();
        rssi.assertSuccess();
        Assertions.assertEquals(-42, rssi.value.intValue());

        Result<Integer> mtu = new Result<>();
        stack.requestMtu(ADDR, 185, mtu);
        settle();
        mtu.assertSuccess();
        Assertions.assertEquals(185, mtu.value.intValue());
        Assertions.assertEquals(185, stack.getMtu(ADDR));

        // the grant clamps to the BLE valid range
        Result<Integer> huge = new Result<>();
        stack.requestMtu(ADDR, 10000, huge);
        Result<Integer> tiny = new Result<>();
        stack.requestMtu(ADDR, 5, tiny);
        settle();
        Assertions.assertEquals(517, huge.value.intValue());
        Assertions.assertEquals(23, tiny.value.intValue());

        Assertions.assertFalse(stack.isBonded(ADDR));
        Result<Boolean> bond = new Result<>();
        stack.bond(ADDR, bond);
        settle();
        bond.assertSuccess();
        Assertions.assertTrue(stack.isBonded(ADDR));
        Assertions.assertEquals(1, stack.getBondedAddresses().size());
    }

    @Test
    public void appDisconnectClearsSubscriptionsAndMtu() {
        stack.addPeripheral(heartRatePeripheral(ADDR));
        connectAndDiscover(ADDR);
        Result<Boolean> subscribe = new Result<>();
        stack.setNotifications(ADDR, HR_SERVICE, HR_MEASUREMENT, true,
                subscribe);
        Result<Integer> mtu = new Result<>();
        stack.requestMtu(ADDR, 200, mtu);
        settle();

        Result<Boolean> disconnect = new Result<>();
        stack.disconnect(ADDR, disconnect);
        settle();
        disconnect.assertSuccess();
        Assertions.assertFalse(stack.isConnected(ADDR));
        Assertions.assertFalse(stack.isSubscribed(ADDR, HR_SERVICE,
                HR_MEASUREMENT));
        Assertions.assertEquals(23, stack.getMtu(ADDR));
    }
}
