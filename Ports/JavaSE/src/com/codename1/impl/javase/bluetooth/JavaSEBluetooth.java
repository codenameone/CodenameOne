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

import com.codename1.bluetooth.AdapterState;
import com.codename1.bluetooth.Bluetooth;
import com.codename1.bluetooth.BluetoothPermission;
import com.codename1.impl.bluetooth.BleBackend;
import com.codename1.impl.bluetooth.NativeBleBackend;
import com.codename1.util.AsyncResource;

/**
 * JavaSE-simulator backing for {@link Bluetooth}. The heavy lifting lives
 * behind the {@link BleBackend} seam: by default the scriptable
 * {@link SimulatorBleBackend} over {@link BluetoothSimulator}'s shared
 * virtual stack; {@link #switchBackend(String)} is the mount point for the
 * future native (host radio) backend. The initial backend comes from the
 * {@code cn1.bluetooth.backend} system property (default
 * {@code "simulator"}).
 *
 * <p>Simulation toggles follow the {@code JavaSENfc} convention of public
 * static flags the Simulate menu can flip.</p>
 */
public class JavaSEBluetooth extends Bluetooth {

    /**
     * Toggle from the future Simulate menu: when {@code true} (the
     * default) permission checks and requests are granted; when
     * {@code false} they are denied.
     */
    public static volatile boolean simGrantPermissions = true;

    /** The system property selecting the initial backend. */
    public static final String BACKEND_PROPERTY = "cn1.bluetooth.backend";

    /** {@link #switchBackend(String)} name of the simulated stack. */
    public static final String BACKEND_SIMULATOR = SimulatorBleBackend.NAME;

    /** {@link #switchBackend(String)} name of the real-radio backend. */
    public static final String BACKEND_NATIVE = "native";

    private volatile BleBackend backend;
    private final JavaSEBluetoothLE le = new JavaSEBluetoothLE(this);
    private final JavaSEBluetoothClassic classic =
            new JavaSEBluetoothClassic(this);

    public JavaSEBluetooth() {
        String name = System.getProperty(BACKEND_PROPERTY,
                BACKEND_SIMULATOR);
        try {
            switchBackend(name);
        } catch (RuntimeException ex) {
            // unknown/unavailable backend requested -- fall back to the
            // simulator so the API stays usable
            switchBackend(BACKEND_SIMULATOR);
        }
    }

    /** The current backend; never {@code null}. */
    BleBackend backend() {
        return backend;
    }

    /**
     * Switches the BLE engine. {@code "simulator"} activates the shared
     * virtual stack; {@code "native"} activates the host machine's real
     * radio in-process through the bundled {@code libcn1ble} library and
     * throws {@code IllegalStateException} when that library could not be
     * loaded (see the cn1.bluetooth.libraryPath system property). Unknown
     * names throw {@code IllegalArgumentException}.
     */
    public synchronized void switchBackend(String name) {
        if (BACKEND_SIMULATOR.equals(name)) {
            installBackend(new SimulatorBleBackend(
                    BluetoothSimulator.getStack()));
            return;
        }
        if (BACKEND_NATIVE.equals(name)) {
            if (!JniBleBridge.isLibraryAvailable()) {
                throw new IllegalStateException(
                        "The native Bluetooth backend is not available: the "
                                + "libcn1ble library could not be loaded. "
                                + "Tried: " + JniBleBridge.describeResolution());
            }
            installBackend(new NativeBleBackend(new JniBleBridge()));
            return;
        }
        throw new IllegalArgumentException("Unknown Bluetooth backend: "
                + name);
    }

    private void installBackend(BleBackend newBackend) {
        BleBackend old = backend;
        if (old != null) {
            old.setAdapterStateSink(null);
            old.shutdown();
        }
        backend = newBackend;
        newBackend.setAdapterStateSink(new BleBackend.AdapterStateSink() {
            public void adapterStateChanged(AdapterState newState) {
                fireAdapterStateChanged(newState);
            }
        });
    }

    /** The name of the active backend, e.g. {@code "simulator"}. */
    public String activeBackendName() {
        return backend.getName();
    }

    public boolean isSupported() {
        return true;
    }

    public boolean isLeSupported() {
        return backend.isLeSupported();
    }

    public boolean isClassicSupported() {
        // classic Bluetooth is simulator-only; the native seam is BLE
        return BACKEND_SIMULATOR.equals(activeBackendName())
                && backend.isClassicSupported();
    }

    public boolean isPeripheralModeSupported() {
        return backend.isPeripheralModeSupported();
    }

    public boolean isL2capSupported() {
        return backend.isL2capSupported();
    }

    public AdapterState getAdapterState() {
        return backend.getAdapterState();
    }

    public AsyncResource<Boolean> requestEnable() {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        BleBackend b = backend;
        if (!(b instanceof SimulatorBleBackend)) {
            // no programmatic enable flow on the native backend
            out.complete(Boolean.FALSE);
            return out;
        }
        SimulatedBluetoothStack stack = ((SimulatorBleBackend) b).getStack();
        stack.setAdapterEnabled(true);
        // resolve once the enable request went through the stack scheduler
        stack.logEvent("adapter", "requestEnable granted");
        stack.getScheduler().post(new Runnable() {
            public void run() {
                if (!out.isDone()) {
                    out.complete(Boolean.TRUE);
                }
            }
        });
        return out;
    }

    public boolean hasPermission(BluetoothPermission permission) {
        return simGrantPermissions;
    }

    public AsyncResource<Boolean> requestPermissions(
            BluetoothPermission... permissions) {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        out.complete(Boolean.valueOf(simGrantPermissions));
        return out;
    }

    public com.codename1.bluetooth.le.BluetoothLE getLE() {
        return le;
    }

    public com.codename1.bluetooth.classic.BluetoothClassic getClassic() {
        return classic;
    }
}
