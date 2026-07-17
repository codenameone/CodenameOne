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

import com.codename1.bluetooth.AdapterState;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.L2capServer;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.bluetooth.le.server.AdvertiseData;
import com.codename1.bluetooth.le.server.AdvertiseSettings;
import com.codename1.bluetooth.le.server.BleAdvertisement;
import com.codename1.bluetooth.le.server.GattServer;
import com.codename1.bluetooth.le.server.GattServerListener;
import com.codename1.util.AsyncResource;

import java.util.List;

/// The pluggable BLE engine seam. The simulator ships its own
/// {@code SimulatorBleBackend}; the real-radio {@link HelperBleBackend}
/// drives the host machine's adapter through the bundled
/// {@code cn1-ble-helper} process. Ports swap engines behind this interface.
public interface BleBackend {

    /// Receives the sightings of the single platform scan.
    interface ScanSink {
        void onResult(ScanResult result);

        void onFailed(BluetoothException reason);
    }

    /// Observes adapter state transitions of this backend.
    interface AdapterStateSink {
        void adapterStateChanged(AdapterState newState);
    }

    /// A stable identifier, e.g. {@code "simulator"} or {@code "native"}.
    String getName();

    boolean isLeSupported();

    boolean isPeripheralModeSupported();

    boolean isClassicSupported();

    boolean isL2capSupported();

    AdapterState getAdapterState();

    /// Registers the single adapter-state observer (the port owner).
    void setAdapterStateSink(AdapterStateSink sink);

    /// Starts the single platform scan; sightings flow into the sink.
    void startScan(ScanSink sink);

    /// Stops the platform scan.
    void stopScan();

    /// The canonical peripheral at the address, or {@code null}.
    BlePeripheral getPeripheral(String address);

    List<BlePeripheral> getConnectedPeripherals(BluetoothUuid serviceFilter);

    List<BlePeripheral> getBondedPeripherals();

    AsyncResource<GattServer> openGattServer(GattServerListener listener);

    AsyncResource<BleAdvertisement> startAdvertising(
            AdvertiseSettings settings, AdvertiseData data,
            AdvertiseData scanResponse);

    AsyncResource<L2capServer> openL2capServer(boolean secure);

    /// Releases backend resources when it is switched away.
    void shutdown();
}
