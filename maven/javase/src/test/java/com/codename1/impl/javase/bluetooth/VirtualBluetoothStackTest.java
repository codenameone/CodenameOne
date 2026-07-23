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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Registry, adapter gating, scan feeds and reset of the simulated stack --
 * fully deterministic on the manual scheduler.
 */
public class VirtualBluetoothStackTest extends AbstractVirtualStackTest {

    private static final class RecordingScanSink
            implements SimulatedBluetoothStack.ScanFeedSink {
        final List<String> sightings = new ArrayList<>();
        final List<Long> timestamps = new ArrayList<>();
        BluetoothError failure;

        public void onSighting(VirtualPeripheral peripheral, long timestamp) {
            sightings.add(peripheral.getAddress());
            timestamps.add(timestamp);
        }

        public void onScanFailed(BluetoothError error, String message) {
            failure = error;
        }
    }

    @Test
    public void registerRemoveAndClearPeripherals() {
        stack.addPeripheral(new VirtualPeripheral("AA:01"));
        stack.addPeripheral(new VirtualPeripheral("AA:02"));
        settle();
        Assertions.assertTrue(stack.isPeripheralRegistered("AA:01"));
        Assertions.assertTrue(stack.isPeripheralRegistered("AA:02"));
        Assertions.assertEquals(Arrays.asList("AA:01", "AA:02"),
                stack.getPeripheralAddresses());

        stack.removePeripheral("AA:01");
        settle();
        Assertions.assertFalse(stack.isPeripheralRegistered("AA:01"));
        Assertions.assertTrue(stack.isPeripheralRegistered("AA:02"));

        stack.clearPeripherals();
        settle();
        Assertions.assertTrue(stack.getPeripheralAddresses().isEmpty());
    }

    @Test
    public void adapterToggleGatesOperations() {
        stack.addPeripheral(new VirtualPeripheral("AA:01"));
        stack.setAdapterEnabled(false);
        settle();
        Assertions.assertFalse(stack.isAdapterEnabled());

        Result<Boolean> connect = new Result<>();
        stack.connect("AA:01", connect);
        RecordingScanSink scan = new RecordingScanSink();
        stack.startScanFeed(scan);
        settle();
        connect.assertFailure(BluetoothError.POWERED_OFF);
        Assertions.assertEquals(BluetoothError.POWERED_OFF, scan.failure);
        Assertions.assertTrue(scan.sightings.isEmpty());

        stack.setAdapterEnabled(true);
        Result<Boolean> connect2 = new Result<>();
        stack.connect("AA:01", connect2);
        settle();
        connect2.assertSuccess();
        Assertions.assertTrue(stack.isConnected("AA:01"));
    }

    @Test
    public void scanFeedEmitsRegisteredLePeripheralsInOrder() {
        stack.addPeripheral(new VirtualPeripheral("AA:01"));
        stack.addPeripheral(new VirtualPeripheral("AA:02"));
        // classic-only devices never show in an LE scan
        stack.addPeripheral(new VirtualPeripheral("AA:03")
                .setLe(false).setClassic(true));
        stack.addPeripheral(new VirtualPeripheral("AA:04"));
        settle();

        RecordingScanSink sink = new RecordingScanSink();
        stack.startScanFeed(sink);
        settle();

        Assertions.assertEquals(Arrays.asList("AA:01", "AA:02", "AA:04"),
                sink.sightings);
        Assertions.assertNull(sink.failure);
        // timestamps come from the stack's monotonic counter
        for (int i = 1; i < sink.timestamps.size(); i++) {
            Assertions.assertTrue(sink.timestamps.get(i)
                    > sink.timestamps.get(i - 1));
        }
    }

    @Test
    public void lateAddedPeripheralIsEmittedToActiveFeed() {
        stack.addPeripheral(new VirtualPeripheral("AA:01"));
        settle();

        RecordingScanSink sink = new RecordingScanSink();
        stack.startScanFeed(sink);
        settle();
        Assertions.assertEquals(Arrays.asList("AA:01"), sink.sightings);

        stack.addPeripheral(new VirtualPeripheral("AA:05"));
        settle();
        Assertions.assertEquals(Arrays.asList("AA:01", "AA:05"),
                sink.sightings);
    }

    @Test
    public void stoppedFeedReceivesNothing() {
        stack.addPeripheral(new VirtualPeripheral("AA:01"));
        settle();

        RecordingScanSink sink = new RecordingScanSink();
        Object token = stack.startScanFeed(sink);
        stack.stopScanFeed(token);
        settle();
        Assertions.assertTrue(sink.sightings.isEmpty());

        // and late-added peripherals do not reach it either
        stack.addPeripheral(new VirtualPeripheral("AA:06"));
        settle();
        Assertions.assertTrue(sink.sightings.isEmpty());
    }

    @Test
    public void eventLogSeesEveryOperation() {
        List<String> ops = new ArrayList<>();
        stack.addEventListener((op, detail) -> ops.add(op));
        stack.addPeripheral(new VirtualPeripheral("AA:01"));
        Result<Boolean> connect = new Result<>();
        stack.connect("AA:01", connect);
        settle();
        Assertions.assertTrue(ops.contains("registerPeripheral"));
        Assertions.assertTrue(ops.contains("connect"));
    }

    @Test
    public void resetRestoresPristineState() {
        stack.addPeripheral(heartRatePeripheral("AA:01"));
        stack.setAdapterEnabled(false);
        stack.setLatencyMillis(500);
        stack.failNext("connect", BluetoothError.BUSY, "scripted");
        settle();

        stack.reset();
        settle();

        Assertions.assertTrue(stack.isAdapterEnabled());
        Assertions.assertTrue(stack.getPeripheralAddresses().isEmpty());
        Assertions.assertEquals(SimulatedBluetoothStack.DEFAULT_LATENCY_MILLIS,
                stack.getLatencyMillis());
        Assertions.assertFalse(stack.isConnected("AA:01"));

        // the scripted failure is gone: a fresh connect succeeds
        stack.addPeripheral(new VirtualPeripheral("AA:01"));
        Result<Boolean> connect = new Result<>();
        stack.connect("AA:01", connect);
        settle();
        connect.assertSuccess();
    }
}
