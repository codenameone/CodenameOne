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

import com.codename1.bluetooth.BluetoothUuid;

/**
 * A scripted remote central connected to the app's simulated GATT server,
 * obtained via {@link SimulatedBluetoothStack#connectVirtualCentral()} (or
 * {@link BluetoothSimulator#connectVirtualCentral()}). Use it from tests
 * or the future debug UI to exercise the app's peripheral role: read and
 * write its characteristics, subscribe to its notifications and observe
 * {@code notifyValue} pushes.
 *
 * <p>All operations complete asynchronously on the stack's scheduler,
 * subject to the configured latency and scripting.</p>
 */
public final class VirtualCentral {

    /** Receives app {@code notifyValue} pushes while subscribed. */
    public interface NotificationListener {
        void valueChanged(BluetoothUuid serviceUuid,
                BluetoothUuid characteristicUuid, byte[] value);
    }

    private final SimulatedBluetoothStack stack;
    private final String address;

    VirtualCentral(SimulatedBluetoothStack stack, String address) {
        this.stack = stack;
        this.address = address;
    }

    /** The synthetic address of this central. */
    public String getAddress() {
        return address;
    }

    /**
     * Reads an app-served characteristic. Static values resolve directly;
     * otherwise the app's {@code GattServerListener} receives a read
     * request and its response resolves the callback.
     */
    public void readCharacteristic(BluetoothUuid serviceUuid,
            BluetoothUuid characteristicUuid,
            SimulatedBluetoothStack.Callback<byte[]> callback) {
        stack.centralRead(address, serviceUuid, characteristicUuid, callback);
    }

    /**
     * Writes an app-served characteristic; the app's
     * {@code GattServerListener} receives the write request and its
     * acknowledgement resolves the callback.
     */
    public void writeCharacteristic(BluetoothUuid serviceUuid,
            BluetoothUuid characteristicUuid, byte[] value,
            SimulatedBluetoothStack.Callback<Boolean> callback) {
        stack.centralWrite(address, serviceUuid, characteristicUuid, value,
                callback);
    }

    /**
     * Subscribes to an app-served characteristic; the app observes a
     * subscription change and subsequent {@code notifyValue} pushes reach
     * the listener.
     */
    public void subscribe(BluetoothUuid serviceUuid,
            BluetoothUuid characteristicUuid, NotificationListener listener,
            SimulatedBluetoothStack.Callback<Boolean> callback) {
        stack.centralSubscribe(address, serviceUuid, characteristicUuid,
                listener, true, callback);
    }

    /** Removes this central's subscription to the characteristic. */
    public void unsubscribe(BluetoothUuid serviceUuid,
            BluetoothUuid characteristicUuid,
            SimulatedBluetoothStack.Callback<Boolean> callback) {
        stack.centralSubscribe(address, serviceUuid, characteristicUuid,
                null, false, callback);
    }

    /** Disconnects this central; the app observes the disconnection. */
    public void disconnect() {
        stack.centralDisconnect(address);
    }

    public String toString() {
        return "VirtualCentral(" + address + ")";
    }
}
