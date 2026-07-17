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
package com.codename1.bluetooth.helper;

import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.BluetoothLE;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.bluetooth.le.server.AdvertiseData;
import com.codename1.bluetooth.le.server.AdvertiseSettings;
import com.codename1.bluetooth.le.server.BleAdvertisement;
import com.codename1.bluetooth.le.server.GattServer;
import com.codename1.bluetooth.le.server.GattServerListener;
import com.codename1.bluetooth.le.L2capServer;
import com.codename1.util.AsyncResource;

import java.util.List;

/// The BLE central role for ports backed by the `cn1-ble-helper`
/// subprocess. Delegates every [BluetoothLE] hook to a fixed
/// [BleBackend]; used by [HelperBluetooth] and shared across the native
/// Win32/Linux desktop ports.
class HelperBluetoothLE extends BluetoothLE {

    private final BleBackend backend;

    HelperBluetoothLE(BleBackend backend) {
        this.backend = backend;
    }

    @Override
    protected boolean isScanSupported() {
        return backend.isLeSupported();
    }

    @Override
    protected void startPlatformScan() {
        backend.startScan(new BleBackend.ScanSink() {
            @Override
            public void onResult(ScanResult result) {
                fireScanResult(result);
            }

            @Override
            public void onFailed(BluetoothException reason) {
                fireScanFailed(reason);
            }
        });
    }

    @Override
    protected void stopPlatformScan() {
        backend.stopScan();
    }

    @Override
    public BlePeripheral getPeripheral(String address) {
        return backend.getPeripheral(address);
    }

    @Override
    public List<BlePeripheral> getConnectedPeripherals(
            BluetoothUuid serviceFilter) {
        return backend.getConnectedPeripherals(serviceFilter);
    }

    @Override
    public List<BlePeripheral> getBondedPeripherals() {
        return backend.getBondedPeripherals();
    }

    @Override
    public AsyncResource<GattServer> openGattServer(
            GattServerListener listener) {
        return backend.openGattServer(listener);
    }

    @Override
    public AsyncResource<BleAdvertisement> startAdvertising(
            AdvertiseSettings settings, AdvertiseData data,
            AdvertiseData scanResponse) {
        return backend.startAdvertising(settings, data, scanResponse);
    }

    @Override
    public AsyncResource<L2capServer> openL2capServer(boolean secure) {
        return backend.openL2capServer(secure);
    }
}
