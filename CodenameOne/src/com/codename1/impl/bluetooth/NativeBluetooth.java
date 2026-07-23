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
package com.codename1.impl.bluetooth;

import com.codename1.bluetooth.AdapterState;
import com.codename1.bluetooth.Bluetooth;
import com.codename1.bluetooth.BluetoothPermission;
import com.codename1.bluetooth.le.BluetoothLE;
import com.codename1.util.AsyncResource;

/// A [Bluetooth] entry point backed by the in-process `libcn1ble` engine --
/// the shared implementation for the native Win32 and Linux desktop ports.
/// Those ports reach the shared library through ParparVM `nativeSources`
/// (see the port's [NativeBleBridge] binding); this class wires it into a
/// [NativeBleBackend] and exposes real BLE central (scan / connect / GATT
/// client / notifications). btleplug -- the engine's backend -- is
/// central-only, so peripheral mode, L2CAP and classic Bluetooth report
/// unsupported.
///
/// A port creates one instance in its `getBluetooth()` and calls
/// [#shutdown()] on app exit.
///
/// Internal to the native-backend implementation -- not a public API.
public class NativeBluetooth extends Bluetooth {

    private final NativeBleBackend backend;
    private final BluetoothLE le;

    public NativeBluetooth(NativeBleBridge bridge) {
        this.backend = new NativeBleBackend(bridge);
        this.le = new NativeBluetoothLE(backend);
        this.backend.setAdapterStateSink(new BleBackend.AdapterStateSink() {
            @Override
            public void adapterStateChanged(AdapterState newState) {
                fireAdapterStateChanged(newState);
            }
        });
    }

    @Override
    public boolean isSupported() {
        return backend.isLeSupported();
    }

    @Override
    public boolean isLeSupported() {
        return backend.isLeSupported();
    }

    @Override
    public boolean isClassicSupported() {
        return backend.isClassicSupported();
    }

    @Override
    public boolean isPeripheralModeSupported() {
        return backend.isPeripheralModeSupported();
    }

    @Override
    public boolean isL2capSupported() {
        return backend.isL2capSupported();
    }

    @Override
    public AdapterState getAdapterState() {
        return backend.getAdapterState();
    }

    @Override
    public BluetoothLE getLE() {
        return le;
    }

    /// Desktop Linux/Windows have no app-level runtime Bluetooth permission
    /// (access is governed by the OS/user session), so permissions are
    /// reported granted and requests resolve `true`.
    @Override
    public boolean hasPermission(BluetoothPermission permission) {
        return true;
    }

    @Override
    public AsyncResource<Boolean> requestPermissions(
            BluetoothPermission... permissions) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.complete(Boolean.TRUE);
        return r;
    }

    /// There is no programmatic adapter-enable on the desktop native path;
    /// resolves `true` when the adapter is already powered on, else `false`.
    @Override
    public AsyncResource<Boolean> requestEnable() {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.complete(getAdapterState() == AdapterState.POWERED_ON
                ? Boolean.TRUE : Boolean.FALSE);
        return r;
    }

    /// Shuts down the native engine. Call from the port on app exit.
    public void shutdown() {
        backend.shutdown();
    }
}
