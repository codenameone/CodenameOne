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
package com.codename1.bluetooth.le.server;

import com.codename1.bluetooth.BluetoothUuid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The payload to advertise via `BluetoothLE.startAdvertising`. Keep it
/// small -- a legacy advertisement carries at most 31 bytes; oversized
/// payloads fail with
/// [com.codename1.bluetooth.BluetoothError#ADVERTISE_FAILED]. Fluent
/// setters return `this` for chaining.
public class AdvertiseData {

    private final ArrayList<BluetoothUuid> serviceUuids =
            new ArrayList<BluetoothUuid>();
    private final HashMap<Integer, byte[]> manufacturerData =
            new HashMap<Integer, byte[]>();
    private final HashMap<BluetoothUuid, byte[]> serviceData =
            new HashMap<BluetoothUuid, byte[]>();
    private boolean includeDeviceName;
    private boolean includeTxPower;

    /// Advertises the given service UUID so filtered scans can find this
    /// peripheral.
    public AdvertiseData addServiceUuid(BluetoothUuid uuid) {
        if (uuid != null && !serviceUuids.contains(uuid)) {
            serviceUuids.add(uuid);
        }
        return this;
    }

    /// Includes the device name in the advertisement (off by default --
    /// names use up scarce advertisement bytes).
    public AdvertiseData setIncludeDeviceName(boolean include) {
        this.includeDeviceName = include;
        return this;
    }

    /// Includes the TX power level in the advertisement.
    public AdvertiseData setIncludeTxPower(boolean include) {
        this.includeTxPower = include;
        return this;
    }

    /// Adds manufacturer-specific data for the given company identifier.
    public AdvertiseData addManufacturerData(int companyId, byte[] data) {
        manufacturerData.put(Integer.valueOf(companyId), data);
        return this;
    }

    /// Adds service data for the given service UUID.
    public AdvertiseData addServiceData(BluetoothUuid uuid, byte[] data) {
        serviceData.put(uuid, data);
        return this;
    }

    /// The service UUIDs to advertise.
    public List<BluetoothUuid> getServiceUuids() {
        return new ArrayList<BluetoothUuid>(serviceUuids);
    }

    /// Whether the device name is included.
    public boolean isIncludeDeviceName() {
        return includeDeviceName;
    }

    /// Whether the TX power level is included.
    public boolean isIncludeTxPower() {
        return includeTxPower;
    }

    /// The manufacturer data entries, keyed by company identifier.
    public Map<Integer, byte[]> getManufacturerData() {
        return new HashMap<Integer, byte[]>(manufacturerData);
    }

    /// The service data entries, keyed by service UUID.
    public Map<BluetoothUuid, byte[]> getServiceData() {
        return new HashMap<BluetoothUuid, byte[]>(serviceData);
    }
}
