/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.bluetooth;

import com.codename1.util.AsyncResource;

/**
 * Scripted {@link Bluetooth} port used by the unit tests. Capabilities and
 * the adapter state are configurable; the LE entry point is a
 * {@link FakeBluetoothLE} whose platform scan is fully test-driven.
 * Installed into the test implementation via
 * {@code TestCodenameOneImplementation.setBluetooth(...)}.
 */
public class FakeBluetooth extends Bluetooth {

    private boolean supported = true;
    private boolean leSupported = true;
    private boolean classicSupported;
    private boolean peripheralModeSupported;
    private boolean l2capSupported;
    private AdapterState adapterState = AdapterState.POWERED_ON;
    private final FakeBluetoothLE le = new FakeBluetoothLE();
    private boolean permissionGranted;
    private boolean enableGranted;

    public FakeBluetooth setSupported(boolean supported) {
        this.supported = supported;
        return this;
    }

    public FakeBluetooth setLeSupported(boolean leSupported) {
        this.leSupported = leSupported;
        return this;
    }

    public FakeBluetooth setClassicSupported(boolean classicSupported) {
        this.classicSupported = classicSupported;
        return this;
    }

    public FakeBluetooth setPeripheralModeSupported(boolean supported) {
        this.peripheralModeSupported = supported;
        return this;
    }

    public FakeBluetooth setL2capSupported(boolean l2capSupported) {
        this.l2capSupported = l2capSupported;
        return this;
    }

    /**
     * Scripts the result of {@link #requestPermissions(BluetoothPermission...)}.
     */
    public FakeBluetooth setPermissionGranted(boolean granted) {
        this.permissionGranted = granted;
        return this;
    }

    /**
     * Scripts the result of {@link #requestEnable()}.
     */
    public FakeBluetooth setEnableGranted(boolean granted) {
        this.enableGranted = granted;
        return this;
    }

    /**
     * Sets the adapter state without notifying listeners.
     */
    public FakeBluetooth setAdapterState(AdapterState state) {
        this.adapterState = state;
        return this;
    }

    /**
     * Simulates a platform adapter state transition: updates the state and
     * dispatches the registered listeners on the EDT.
     */
    public void changeAdapterState(AdapterState newState) {
        this.adapterState = newState;
        fireAdapterStateChanged(newState);
    }

    @Override
    public boolean isSupported() {
        return supported;
    }

    @Override
    public boolean isLeSupported() {
        return leSupported;
    }

    @Override
    public boolean isClassicSupported() {
        return classicSupported;
    }

    @Override
    public boolean isPeripheralModeSupported() {
        return peripheralModeSupported;
    }

    @Override
    public boolean isL2capSupported() {
        return l2capSupported;
    }

    @Override
    public AdapterState getAdapterState() {
        return adapterState;
    }

    @Override
    public com.codename1.bluetooth.le.BluetoothLE getLE() {
        return le;
    }

    /**
     * The scripted LE stack, typed for test convenience.
     */
    public FakeBluetoothLE getFakeLE() {
        return le;
    }

    @Override
    public boolean hasPermission(BluetoothPermission permission) {
        return permissionGranted;
    }

    @Override
    public AsyncResource<Boolean> requestPermissions(
            BluetoothPermission... permissions) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.complete(Boolean.valueOf(permissionGranted));
        return r;
    }

    @Override
    public AsyncResource<Boolean> requestEnable() {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.complete(Boolean.valueOf(enableGranted));
        return r;
    }
}
