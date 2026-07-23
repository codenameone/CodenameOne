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

/**
 * The scriptable facade of the JavaSE simulator's virtual Bluetooth world
 * -- the API tests, sample apps and the future
 * Simulate&nbsp;&rarr;&nbsp;Bluetooth menu use to stage devices and
 * events:
 *
 * <pre>
 * BluetoothSimulator.addPeripheral(
 *         new VirtualPeripheral("AA:BB:CC:DD:EE:01")
 *                 .setName("Heart Rate Strap")
 *                 .addAdvertisedServiceUuid(BluetoothUuid.fromShort(0x180D))
 *                 .withService(new VirtualService(
 *                         BluetoothUuid.fromShort(0x180D))
 *                         .withCharacteristic(new VirtualCharacteristic(
 *                                 BluetoothUuid.fromShort(0x2A37),
 *                                 GattCharacteristic.PROPERTY_READ
 *                                         | GattCharacteristic.PROPERTY_NOTIFY,
 *                                 new byte[] {0, 72}))));
 * </pre>
 *
 * <p>It is a thin static wrapper over one shared
 * {@link SimulatedBluetoothStack} running on an {@link AutoScheduler};
 * {@link JavaSEBluetooth} routes the app-facing API onto the same
 * instance. Tests that need full determinism construct their own stack
 * with a {@link ManualScheduler} instead of going through this class.</p>
 */
public final class BluetoothSimulator {

    private static SimulatedBluetoothStack stack;

    private BluetoothSimulator() {
    }

    /** The shared virtual stack (created lazily), for advanced use. */
    public static synchronized SimulatedBluetoothStack getStack() {
        if (stack == null) {
            stack = new SimulatedBluetoothStack(new AutoScheduler());
        }
        return stack;
    }

    /** Restores the pristine simulation state. */
    public static void reset() {
        getStack().reset();
    }

    /** Registers a virtual peripheral apps can discover and connect to. */
    public static void addPeripheral(VirtualPeripheral peripheral) {
        getStack().addPeripheral(peripheral);
    }

    /** Removes a virtual peripheral. */
    public static void removePeripheral(String address) {
        getStack().removePeripheral(address);
    }

    /** Removes every virtual peripheral. */
    public static void clearPeripherals() {
        getStack().clearPeripherals();
    }

    public static boolean isPeripheralRegistered(String address) {
        return getStack().isPeripheralRegistered(address);
    }

    /** Turns the simulated adapter on or off. */
    public static void setAdapterEnabled(boolean enabled) {
        getStack().setAdapterEnabled(enabled);
    }

    public static boolean isAdapterEnabled() {
        return getStack().isAdapterEnabled();
    }

    /** Sets the artificial latency applied to every async completion. */
    public static void setLatencyMillis(long millis) {
        getStack().setLatencyMillis(millis);
    }

    /**
     * Scripts the next occurrence of an operation to fail -- see
     * {@link SimulatedBluetoothStack#failNext(String, BluetoothError,
     * String)} for the operation keys.
     */
    public static void failNext(String op, BluetoothError error,
            String message) {
        getStack().failNext(op, error, message);
    }

    /**
     * Pushes a notification from a virtual peripheral to the app (only
     * delivered while connected and subscribed).
     */
    public static void pushNotification(String address,
            BluetoothUuid serviceUuid, BluetoothUuid characteristicUuid,
            byte[] value) {
        getStack().pushNotification(address, serviceUuid, characteristicUuid,
                value);
    }

    /** Simulates the remote peripheral dropping the link. */
    public static void disconnectFromRemote(String address) {
        getStack().disconnectFromRemote(address);
    }

    /**
     * Registers a virtual remote RFCOMM endpoint the app can connect to as
     * a client.
     */
    public static void addRfcommEndpoint(BluetoothUuid serviceUuid,
            SimStreamHandler handler) {
        getStack().addRfcommEndpoint(serviceUuid, handler);
    }

    /**
     * Connects a virtual RFCOMM client to the app's listener and returns
     * the remote side of the channel.
     */
    public static SimStreamChannel connectVirtualRfcommClient(
            BluetoothUuid serviceUuid) {
        return getStack().connectVirtualRfcommClient(serviceUuid);
    }

    /**
     * Connects a scripted virtual central to the app's GATT server; use
     * the returned handle to read/write/subscribe against the services the
     * app published.
     */
    public static VirtualCentral connectVirtualCentral() {
        return getStack().connectVirtualCentral();
    }

    /**
     * Loads a recorded fixture (see {@link BluetoothFixture}) from its
     * JSON form and replays it into the shared stack -- devices appear at
     * their recorded first-sighting times and their RSSI timelines replay
     * on the stack's scheduler. Returns the parsed fixture.
     */
    public static BluetoothFixture loadFixture(java.io.InputStream in)
            throws java.io.IOException {
        BluetoothFixture fixture = BluetoothFixture.fromJson(in);
        getStack().loadFixture(fixture);
        return fixture;
    }

    /** Replays an in-memory fixture into the shared stack. */
    public static void loadFixture(BluetoothFixture fixture) {
        getStack().loadFixture(fixture);
    }

    /** Registers an event-log listener (for the debug UI). */
    public static void addEventListener(StackEventListener listener) {
        getStack().addEventListener(listener);
    }

    /** Removes an event-log listener. */
    public static void removeEventListener(StackEventListener listener) {
        getStack().removeEventListener(listener);
    }
}
