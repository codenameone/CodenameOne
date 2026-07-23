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
 * {@code failNext} scripting: the scripted error surfaces on exactly the
 * next occurrence of the operation -- with the exact error code and
 * message -- and the following call succeeds again.
 */
public class FailureScriptTest extends AbstractVirtualStackTest {

    private static final String ADDR = "AA:BB:CC:DD:EE:33";

    @Test
    public void connectFailsExactlyOnce() {
        stack.addPeripheral(heartRatePeripheral(ADDR));
        stack.failNext("connect", BluetoothError.CONNECTION_FAILED, "boom");

        Result<Boolean> first = new Result<>();
        stack.connect(ADDR, first);
        settle();
        first.assertFailure(BluetoothError.CONNECTION_FAILED);
        Assertions.assertEquals("boom", first.message);
        Assertions.assertFalse(stack.isConnected(ADDR));

        Result<Boolean> second = new Result<>();
        stack.connect(ADDR, second);
        settle();
        second.assertSuccess();
    }

    @Test
    public void discoverReadWriteAndSubscribeAreScriptable() {
        stack.addPeripheral(heartRatePeripheral(ADDR));
        Result<Boolean> connect = new Result<>();
        stack.connect(ADDR, connect);
        settle();
        connect.assertSuccess();

        stack.failNext("discover", BluetoothError.GATT_ERROR, "no db");
        Result<List<VirtualService>> discoverFail = new Result<>();
        stack.discoverServices(ADDR, discoverFail);
        settle();
        discoverFail.assertFailure(BluetoothError.GATT_ERROR);
        Assertions.assertEquals("no db", discoverFail.message);

        Result<List<VirtualService>> discover = new Result<>();
        stack.discoverServices(ADDR, discover);
        settle();
        discover.assertSuccess();

        stack.failNext("read", BluetoothError.IO_ERROR, "read glitch");
        Result<byte[]> readFail = new Result<>();
        stack.readCharacteristic(ADDR, HR_SERVICE, HR_MEASUREMENT, readFail);
        Result<byte[]> readOk = new Result<>();
        stack.readCharacteristic(ADDR, HR_SERVICE, HR_MEASUREMENT, readOk);
        settle();
        readFail.assertFailure(BluetoothError.IO_ERROR);
        Assertions.assertEquals("read glitch", readFail.message);
        readOk.assertSuccess();

        stack.failNext("write", BluetoothError.GATT_ERROR, "write glitch");
        Result<Boolean> writeFail = new Result<>();
        stack.writeCharacteristic(ADDR, HR_SERVICE, HR_CONTROL,
                new byte[] {1}, writeFail);
        Result<Boolean> writeOk = new Result<>();
        stack.writeCharacteristic(ADDR, HR_SERVICE, HR_CONTROL,
                new byte[] {2}, writeOk);
        settle();
        writeFail.assertFailure(BluetoothError.GATT_ERROR);
        writeOk.assertSuccess();

        stack.failNext("subscribe", BluetoothError.GATT_ERROR, "cccd");
        Result<Boolean> subFail = new Result<>();
        stack.setNotifications(ADDR, HR_SERVICE, HR_MEASUREMENT, true,
                subFail);
        Result<Boolean> subOk = new Result<>();
        stack.setNotifications(ADDR, HR_SERVICE, HR_MEASUREMENT, true,
                subOk);
        settle();
        subFail.assertFailure(BluetoothError.GATT_ERROR);
        subOk.assertSuccess();
    }

    @Test
    public void scanFailureIsScriptable() {
        stack.addPeripheral(heartRatePeripheral(ADDR));
        stack.failNext("scan", BluetoothError.SCAN_FAILED, "throttled");

        List<String> sightings = new ArrayList<>();
        BluetoothError[] failed = new BluetoothError[1];
        String[] failedMessage = new String[1];
        stack.startScanFeed(new SimulatedBluetoothStack.ScanFeedSink() {
            public void onSighting(VirtualPeripheral p, long timestamp) {
                sightings.add(p.getAddress());
            }

            public void onScanFailed(BluetoothError error, String message) {
                failed[0] = error;
                failedMessage[0] = message;
            }
        });
        settle();
        Assertions.assertEquals(BluetoothError.SCAN_FAILED, failed[0]);
        Assertions.assertEquals("throttled", failedMessage[0]);
        Assertions.assertTrue(sightings.isEmpty());

        // the next scan runs clean
        stack.startScanFeed(new SimulatedBluetoothStack.ScanFeedSink() {
            public void onSighting(VirtualPeripheral p, long timestamp) {
                sightings.add(p.getAddress());
            }

            public void onScanFailed(BluetoothError error, String message) {
                Assertions.fail("second scan must not fail: " + error);
            }
        });
        settle();
        Assertions.assertEquals(1, sightings.size());
    }

    @Test
    public void rssiMtuBondAndRfcommConnectAreScriptable() {
        stack.addPeripheral(heartRatePeripheral(ADDR));
        Result<Boolean> connect = new Result<>();
        stack.connect(ADDR, connect);
        settle();

        stack.failNext("rssi", BluetoothError.TIMEOUT, "rssi lost");
        Result<Integer> rssiFail = new Result<>();
        stack.readRssi(ADDR, rssiFail);
        Result<Integer> rssiOk = new Result<>();
        stack.readRssi(ADDR, rssiOk);
        settle();
        rssiFail.assertFailure(BluetoothError.TIMEOUT);
        rssiOk.assertSuccess();

        stack.failNext("mtu", BluetoothError.GATT_ERROR, "mtu refused");
        Result<Integer> mtuFail = new Result<>();
        stack.requestMtu(ADDR, 100, mtuFail);
        Result<Integer> mtuOk = new Result<>();
        stack.requestMtu(ADDR, 100, mtuOk);
        settle();
        mtuFail.assertFailure(BluetoothError.GATT_ERROR);
        mtuOk.assertSuccess();

        stack.failNext("bond", BluetoothError.BOND_FAILED, "pin mismatch");
        Result<Boolean> bondFail = new Result<>();
        stack.bond(ADDR, bondFail);
        Result<Boolean> bondOk = new Result<>();
        stack.bond(ADDR, bondOk);
        settle();
        bondFail.assertFailure(BluetoothError.BOND_FAILED);
        bondOk.assertSuccess();

        stack.addRfcommEndpoint(BluetoothUuid.SPP, echoHandler());
        stack.failNext("rfcommConnect", BluetoothError.BUSY, "channel busy");
        Result<SimStreamChannel> rfFail = new Result<>();
        stack.connectRfcomm(BluetoothUuid.SPP, rfFail);
        Result<SimStreamChannel> rfOk = new Result<>();
        stack.connectRfcomm(BluetoothUuid.SPP, rfOk);
        settle();
        rfFail.assertFailure(BluetoothError.BUSY);
        Assertions.assertEquals("channel busy", rfFail.message);
        rfOk.assertSuccess();
        rfOk.value.close();
    }

    @Test
    public void queuedFailuresApplyInFifoOrder() {
        stack.addPeripheral(heartRatePeripheral(ADDR));
        stack.failNext("connect", BluetoothError.BUSY, "first");
        stack.failNext("connect", BluetoothError.TIMEOUT, "second");

        Result<Boolean> first = new Result<>();
        stack.connect(ADDR, first);
        Result<Boolean> second = new Result<>();
        stack.connect(ADDR, second);
        Result<Boolean> third = new Result<>();
        stack.connect(ADDR, third);
        settle();

        first.assertFailure(BluetoothError.BUSY);
        Assertions.assertEquals("first", first.message);
        second.assertFailure(BluetoothError.TIMEOUT);
        Assertions.assertEquals("second", second.message);
        third.assertSuccess();
    }
}
