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
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattStatus;
import com.codename1.bluetooth.le.server.GattLocalCharacteristic;
import com.codename1.bluetooth.le.server.GattLocalService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * The peripheral role: the app publishes a GATT server + advertising in
 * the stack and a scripted {@link VirtualCentral} connects, reads, writes,
 * subscribes and receives {@code notifyValue} pushes.
 */
public class VirtualPeripheralModeTest extends AbstractVirtualStackTest {

    private static final BluetoothUuid SVC = BluetoothUuid.fromShort(0x1815);
    private static final BluetoothUuid STATIC_CHR =
            BluetoothUuid.fromShort(0x2A56);
    private static final BluetoothUuid DYNAMIC_CHR =
            BluetoothUuid.fromShort(0x2A57);

    private static final class RecordingAppSink
            implements SimulatedBluetoothStack.AppServerSink {
        final List<String> connected = new ArrayList<>();
        final List<String> disconnected = new ArrayList<>();
        final List<String> subscriptionEvents = new ArrayList<>();
        final List<SimulatedBluetoothStack.AppReadRequest> readRequests =
                new ArrayList<>();
        final List<SimulatedBluetoothStack.AppWriteRequest> writeRequests =
                new ArrayList<>();

        public void centralConnected(String centralAddress) {
            connected.add(centralAddress);
        }

        public void centralDisconnected(String centralAddress) {
            disconnected.add(centralAddress);
        }

        public void subscriptionChanged(String centralAddress,
                GattLocalCharacteristic characteristic, boolean subscribed) {
            subscriptionEvents.add(centralAddress + " "
                    + characteristic.getUuid() + " " + subscribed);
        }

        public void characteristicReadRequest(
                SimulatedBluetoothStack.AppReadRequest request) {
            readRequests.add(request);
        }

        public void characteristicWriteRequest(
                SimulatedBluetoothStack.AppWriteRequest request) {
            writeRequests.add(request);
        }
    }

    private RecordingAppSink appSink;
    private GattLocalCharacteristic staticChr;
    private GattLocalCharacteristic dynamicChr;

    @BeforeEach
    void publishAppServer() {
        appSink = new RecordingAppSink();
        staticChr = new GattLocalCharacteristic(STATIC_CHR,
                GattCharacteristic.PROPERTY_READ,
                GattLocalCharacteristic.PERMISSION_READ)
                .setValue(new byte[] {42});
        dynamicChr = new GattLocalCharacteristic(DYNAMIC_CHR,
                GattCharacteristic.PROPERTY_READ
                        | GattCharacteristic.PROPERTY_WRITE
                        | GattCharacteristic.PROPERTY_NOTIFY,
                GattLocalCharacteristic.PERMISSION_READ
                        | GattLocalCharacteristic.PERMISSION_WRITE);
        Result<Boolean> open = new Result<>();
        stack.openAppGattServer(appSink, open);
        Result<Boolean> add = new Result<>();
        stack.addAppService(new GattLocalService(SVC)
                .addCharacteristic(staticChr)
                .addCharacteristic(dynamicChr), add);
        settle();
        open.assertSuccess();
        add.assertSuccess();
    }

    @Test
    public void advertisingStateIsTracked() {
        Assertions.assertFalse(stack.isAdvertising());
        Result<Object> start = new Result<>();
        stack.startAppAdvertising("test-payload", start);
        settle();
        start.assertSuccess();
        Assertions.assertTrue(stack.isAdvertising());
        Assertions.assertTrue(stack.isAppAdvertising(start.value));

        stack.stopAppAdvertising(start.value);
        settle();
        Assertions.assertFalse(stack.isAdvertising());
    }

    @Test
    public void centralConnectionAndDisconnectionEventsFire() {
        VirtualCentral central = stack.connectVirtualCentral();
        settle();
        Assertions.assertEquals(1, appSink.connected.size());
        Assertions.assertEquals(central.getAddress(),
                appSink.connected.get(0));
        Assertions.assertEquals(1,
                stack.getConnectedCentralAddresses().size());

        central.disconnect();
        settle();
        Assertions.assertEquals(1, appSink.disconnected.size());
        Assertions.assertTrue(
                stack.getConnectedCentralAddresses().isEmpty());
    }

    @Test
    public void staticValueReadsResolveWithoutTheAppListener() {
        VirtualCentral central = stack.connectVirtualCentral();
        Result<byte[]> read = new Result<>();
        central.readCharacteristic(SVC, STATIC_CHR, read);
        settle();
        read.assertSuccess();
        Assertions.assertArrayEquals(new byte[] {42}, read.value);
        Assertions.assertTrue(appSink.readRequests.isEmpty());
    }

    @Test
    public void dynamicReadsRouteThroughTheAppAndRespond() {
        VirtualCentral central = stack.connectVirtualCentral();
        Result<byte[]> read = new Result<>();
        central.readCharacteristic(SVC, DYNAMIC_CHR, read);
        settle();
        Assertions.assertEquals(1, appSink.readRequests.size());
        Assertions.assertEquals(0, read.successCount);

        appSink.readRequests.get(0).respond(new byte[] {7, 7});
        settle();
        read.assertSuccess();
        Assertions.assertArrayEquals(new byte[] {7, 7}, read.value);
    }

    @Test
    public void rejectedReadsSurfaceAsGattErrors() {
        VirtualCentral central = stack.connectVirtualCentral();
        Result<byte[]> read = new Result<>();
        central.readCharacteristic(SVC, DYNAMIC_CHR, read);
        settle();
        appSink.readRequests.get(0).reject(GattStatus.READ_NOT_PERMITTED);
        settle();
        read.assertFailure(BluetoothError.GATT_ERROR);
    }

    @Test
    public void writesRouteThroughTheAppAndAcknowledge() {
        VirtualCentral central = stack.connectVirtualCentral();
        Result<Boolean> write = new Result<>();
        central.writeCharacteristic(SVC, DYNAMIC_CHR, new byte[] {1, 2},
                write);
        settle();
        Assertions.assertEquals(1, appSink.writeRequests.size());
        SimulatedBluetoothStack.AppWriteRequest request =
                appSink.writeRequests.get(0);
        Assertions.assertArrayEquals(new byte[] {1, 2}, request.getValue());
        Assertions.assertEquals(central.getAddress(),
                request.getCentralAddress());

        request.respond();
        settle();
        write.assertSuccess();
    }

    @Test
    public void subscriptionsDeliverNotifyValuePushes() {
        VirtualCentral central = stack.connectVirtualCentral();
        List<byte[]> received = new ArrayList<>();
        Result<Boolean> subscribe = new Result<>();
        central.subscribe(SVC, DYNAMIC_CHR,
                (svc, chr, value) -> received.add(value), subscribe);
        settle();
        subscribe.assertSuccess();
        Assertions.assertEquals(1, appSink.subscriptionEvents.size());
        Assertions.assertEquals(central.getAddress() + " " + DYNAMIC_CHR
                + " true", appSink.subscriptionEvents.get(0));

        Result<Boolean> notify = new Result<>();
        stack.notifyAppValue(dynamicChr, new byte[] {3, 2, 1}, null, notify);
        settle();
        notify.assertSuccess();
        Assertions.assertEquals(1, received.size());
        Assertions.assertArrayEquals(new byte[] {3, 2, 1}, received.get(0));

        Result<Boolean> unsubscribe = new Result<>();
        central.unsubscribe(SVC, DYNAMIC_CHR, unsubscribe);
        settle();
        unsubscribe.assertSuccess();
        Assertions.assertEquals(2, appSink.subscriptionEvents.size());

        stack.notifyAppValue(dynamicChr, new byte[] {9}, null, null);
        settle();
        Assertions.assertEquals(1, received.size());
    }

    @Test
    public void notifyValueTargetsASingleCentralWhenAddressed() {
        VirtualCentral first = stack.connectVirtualCentral();
        VirtualCentral second = stack.connectVirtualCentral();
        List<byte[]> firstReceived = new ArrayList<>();
        List<byte[]> secondReceived = new ArrayList<>();
        first.subscribe(SVC, DYNAMIC_CHR,
                (svc, chr, value) -> firstReceived.add(value), null);
        second.subscribe(SVC, DYNAMIC_CHR,
                (svc, chr, value) -> secondReceived.add(value), null);
        settle();

        stack.notifyAppValue(dynamicChr, new byte[] {1},
                second.getAddress(), null);
        settle();
        Assertions.assertTrue(firstReceived.isEmpty());
        Assertions.assertEquals(1, secondReceived.size());
    }
}
