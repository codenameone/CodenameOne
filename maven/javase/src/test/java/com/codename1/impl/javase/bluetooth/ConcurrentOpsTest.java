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
 * Interleaved operations through the manual scheduler complete in
 * deterministic issue order -- across multiple peripherals and when
 * follow-up operations are issued from completion callbacks.
 */
public class ConcurrentOpsTest extends AbstractVirtualStackTest {

    private static final String A = "AA:00:00:00:00:0A";
    private static final String B = "AA:00:00:00:00:0B";

    private SimulatedBluetoothStack.Callback<byte[]> record(
            List<String> order, String tag) {
        return new SimulatedBluetoothStack.Callback<byte[]>() {
            public void onSuccess(byte[] value) {
                order.add(tag);
            }

            public void onError(BluetoothError error, String message) {
                order.add(tag + "!" + error);
            }
        };
    }

    @Test
    public void interleavedOpsCompleteInIssueOrder() {
        stack.addPeripheral(heartRatePeripheral(A));
        stack.addPeripheral(heartRatePeripheral(B));
        connectAndDiscover(A);
        connectAndDiscover(B);

        List<String> order = new ArrayList<>();
        stack.readCharacteristic(A, HR_SERVICE, HR_MEASUREMENT,
                record(order, "readA1"));
        stack.writeCharacteristic(B, HR_SERVICE, HR_CONTROL, new byte[] {1},
                new SimulatedBluetoothStack.Callback<Boolean>() {
                    public void onSuccess(Boolean value) {
                        order.add("writeB");
                    }

                    public void onError(BluetoothError error,
                            String message) {
                        order.add("writeB!" + error);
                    }
                });
        stack.readRssi(A, new SimulatedBluetoothStack.Callback<Integer>() {
            public void onSuccess(Integer value) {
                order.add("rssiA");
            }

            public void onError(BluetoothError error, String message) {
                order.add("rssiA!" + error);
            }
        });
        stack.readCharacteristic(B, HR_SERVICE, HR_MEASUREMENT,
                record(order, "readB"));
        stack.readCharacteristic(A, HR_SERVICE, HR_CONTROL,
                record(order, "readA2"));
        settle();

        Assertions.assertEquals(Arrays.asList("readA1", "writeB", "rssiA",
                "readB", "readA2"), order);
    }

    @Test
    public void twoRunsProduceIdenticalOrdering() {
        List<String> first = runScriptedSequence();
        // fresh stack, same script
        scheduler = new ManualScheduler();
        stack = new SimulatedBluetoothStack(scheduler);
        List<String> second = runScriptedSequence();
        Assertions.assertEquals(first, second);
        Assertions.assertFalse(first.isEmpty());
    }

    private List<String> runScriptedSequence() {
        stack.addPeripheral(heartRatePeripheral(A));
        stack.addPeripheral(heartRatePeripheral(B));
        connectAndDiscover(A);
        connectAndDiscover(B);
        List<String> order = new ArrayList<>();
        stack.readCharacteristic(A, HR_SERVICE, HR_MEASUREMENT,
                record(order, "1"));
        stack.readCharacteristic(B, HR_SERVICE, HR_MEASUREMENT,
                record(order, "2"));
        stack.readCharacteristic(A, HR_SERVICE, HR_CONTROL,
                record(order, "3"));
        stack.readCharacteristic(B, HR_SERVICE, HR_CONTROL,
                record(order, "4"));
        settle();
        return order;
    }

    @Test
    public void chainedOpsIssuedFromCallbacksStayOrdered() {
        stack.addPeripheral(heartRatePeripheral(A));
        connectAndDiscover(A);

        List<String> order = new ArrayList<>();
        stack.readCharacteristic(A, HR_SERVICE, HR_MEASUREMENT,
                new SimulatedBluetoothStack.Callback<byte[]>() {
                    public void onSuccess(byte[] value) {
                        order.add("first");
                        stack.readCharacteristic(A, HR_SERVICE, HR_CONTROL,
                                record(order, "chained"));
                    }

                    public void onError(BluetoothError error,
                            String message) {
                        order.add("first!" + error);
                    }
                });
        stack.readRssi(A, new SimulatedBluetoothStack.Callback<Integer>() {
            public void onSuccess(Integer value) {
                order.add("second");
            }

            public void onError(BluetoothError error, String message) {
                order.add("second!" + error);
            }
        });
        settle();

        Assertions.assertEquals(Arrays.asList("first", "second", "chained"),
                order);
    }
}
