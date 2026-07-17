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
package com.codename1.impl.javase;

import com.codename1.bluetooth.Bluetooth;
import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.components.ToastBar;
import com.codename1.impl.javase.bluetooth.BluetoothSimulator;
import com.codename1.impl.javase.bluetooth.JavaSEBluetooth;
import com.codename1.impl.javase.bluetooth.VirtualCharacteristic;
import com.codename1.impl.javase.bluetooth.VirtualDescriptor;
import com.codename1.impl.javase.bluetooth.VirtualPeripheral;
import com.codename1.impl.javase.bluetooth.VirtualService;
import com.codename1.ui.Display;

import java.util.List;

/**
 * Cross-platform scripting hooks of the simulated Bluetooth stack. Declared
 * in {@code META-INF/codenameone/simulator-hooks.properties} so the
 * simulator surfaces them as the "Bluetooth" menu and registers them with
 * {@link com.codename1.system.SimulatorHookExecutor} -- test code drives
 * them via {@code CN.execute("bluetooth:itemN")} on any platform.
 *
 * <p>All methods are {@code public static void} with no arguments (the
 * contract of {@link com.codename1.impl.javase.simulator.SimulatorHookLoader})
 * and are invoked on the Codename One EDT.</p>
 */
public final class BluetoothSimulatorHooks {

    /** Address of the canonical demo peripheral. */
    public static final String DEMO_ADDRESS = "AA:BB:CC:DD:EE:01";

    /** Name of the canonical demo peripheral. */
    public static final String DEMO_NAME = "SimulatedSensor";

    /** Service UUID (0x180D, Heart Rate) of the demo peripheral. */
    public static final BluetoothUuid DEMO_SERVICE =
            BluetoothUuid.fromShort(0x180D);

    /** The demo read/notify characteristic (0x2A37) with a CCCD. */
    public static final BluetoothUuid DEMO_NOTIFY_CHARACTERISTIC =
            BluetoothUuid.fromShort(0x2A37);

    /** The demo write characteristic (0x2A39). */
    public static final BluetoothUuid DEMO_WRITE_CHARACTERISTIC =
            BluetoothUuid.fromShort(0x2A39);

    private static int demoNotificationCounter;

    private BluetoothSimulatorHooks() {
    }

    /**
     * Builds the canonical demo device shared by the Simulate window's
     * "Add Demo Peripheral" button and the {@link #addDemoPeripheral()}
     * hook: address {@value #DEMO_ADDRESS}, name {@value #DEMO_NAME}, one
     * 0x180D service with a read/write/notify 0x2A37 characteristic
     * (carrying a CCCD) and a writable 0x2A39 characteristic.
     */
    public static VirtualPeripheral createDemoPeripheral() {
        return new VirtualPeripheral(DEMO_ADDRESS)
                .setName(DEMO_NAME)
                .addAdvertisedServiceUuid(DEMO_SERVICE)
                .withService(createDemoService());
    }

    /**
     * The demo GATT service alone, for the "Add Peripheral" dialog's
     * "include demo GATT service" option.
     */
    public static VirtualService createDemoService() {
        return new VirtualService(DEMO_SERVICE)
                .withCharacteristic(new VirtualCharacteristic(
                        DEMO_NOTIFY_CHARACTERISTIC,
                        GattCharacteristic.PROPERTY_READ
                                | GattCharacteristic.PROPERTY_WRITE
                                | GattCharacteristic.PROPERTY_NOTIFY,
                        new byte[] {0, 72})
                        .withDescriptor(new VirtualDescriptor(
                                BluetoothUuid.CCCD, new byte[] {0, 0})))
                .withCharacteristic(new VirtualCharacteristic(
                        DEMO_WRITE_CHARACTERISTIC,
                        GattCharacteristic.PROPERTY_WRITE,
                        new byte[] {0}));
    }

    /** Flips the simulated adapter between on and off. */
    public static void toggleAdapter() {
        boolean enable = !BluetoothSimulator.isAdapterEnabled();
        BluetoothSimulator.setAdapterEnabled(enable);
        toast("Bluetooth adapter " + (enable ? "enabled" : "disabled"));
    }

    /** Registers (or re-registers) the canonical demo peripheral. */
    public static void addDemoPeripheral() {
        BluetoothSimulator.addPeripheral(createDemoPeripheral());
        toast(DEMO_NAME + " (" + DEMO_ADDRESS + ") registered");
    }

    /** Drops every live connection from the remote side. */
    public static void disconnectAll() {
        List<String> connected =
                BluetoothSimulator.getStack().getConnectedAddresses();
        int size = connected.size();
        for (int i = 0; i < size; i++) {
            BluetoothSimulator.disconnectFromRemote(connected.get(i));
        }
        toast(size == 0 ? "No Bluetooth connections to drop"
                : "Dropped " + size + " Bluetooth connection(s)");
    }

    /**
     * Pushes a rolling one-byte notification from the demo peripheral's
     * 0x2A37 characteristic (delivered only while the app is connected and
     * subscribed).
     */
    public static void pushDemoNotification() {
        byte payload;
        synchronized (BluetoothSimulatorHooks.class) {
            payload = (byte) (demoNotificationCounter++ & 0xFF);
        }
        BluetoothSimulator.pushNotification(DEMO_ADDRESS, DEMO_SERVICE,
                DEMO_NOTIFY_CHARACTERISTIC, new byte[] {payload});
        toast("Pushed demo notification [" + (payload & 0xFF) + "]");
    }

    /** Removes every registered virtual peripheral. */
    public static void clearPeripherals() {
        BluetoothSimulator.clearPeripherals();
        toast("Cleared all virtual Bluetooth peripherals");
    }

    /** Switches the JavaSE port to the (future) native host-radio backend. */
    public static void switchToNativeBackend() {
        switchBackend(JavaSEBluetooth.BACKEND_NATIVE);
    }

    /** Switches the JavaSE port back to the simulated stack backend. */
    public static void switchToSimulatorBackend() {
        switchBackend(JavaSEBluetooth.BACKEND_SIMULATOR);
    }

    /**
     * Arms the next GATT read to fail with {@code GATT_ERROR}. API-only
     * hook (no menu label): drive it from tests via
     * {@code CN.execute("bluetooth:item8")}.
     */
    public static void primeReadFailure() {
        BluetoothSimulator.failNext("read", BluetoothError.GATT_ERROR,
                "Primed by simulator hook");
    }

    private static void switchBackend(String name) {
        Bluetooth bt = Bluetooth.getInstance();
        if (!(bt instanceof JavaSEBluetooth)) {
            toast("Bluetooth backend switching is unavailable here");
            return;
        }
        JavaSEBluetooth impl = (JavaSEBluetooth) bt;
        try {
            impl.switchBackend(name);
            toast("Bluetooth backend: " + impl.activeBackendName());
        } catch (RuntimeException ex) {
            toast("Cannot switch Bluetooth backend: " + ex.getMessage());
        }
    }

    /**
     * ToastBar feedback, guarded so API-driven invocations in minimal test
     * harnesses (no current form yet) degrade to a console line.
     */
    private static void toast(String message) {
        try {
            if (Display.isInitialized()
                    && Display.getInstance().getCurrent() != null) {
                ToastBar.showInfoMessage(message);
                return;
            }
        } catch (Throwable t) {
            // fall through to the console
        }
        System.out.println("Bluetooth simulation: " + message);
    }
}
