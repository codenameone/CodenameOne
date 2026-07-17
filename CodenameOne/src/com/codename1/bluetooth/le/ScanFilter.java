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
package com.codename1.bluetooth.le;

import com.codename1.bluetooth.BluetoothUuid;

/// A single scan filter -- all criteria set on one filter are AND-combined;
/// multiple filters added to a [ScanSettings] are OR-combined. Fluent
/// setters return `this` for chaining:
///
/// ```java
/// new ScanFilter()
///     .setServiceUuid(BluetoothUuid.fromShort(0x180D))
///     .setNamePrefix("Polar");
/// ```
public class ScanFilter {

    private BluetoothUuid serviceUuid;
    private String name;
    private String namePrefix;
    private String address;
    private int manufacturerId = -1;
    private byte[] manufacturerData;
    private byte[] manufacturerDataMask;

    /// Matches devices advertising the given service UUID.
    public ScanFilter setServiceUuid(BluetoothUuid uuid) {
        this.serviceUuid = uuid;
        return this;
    }

    /// Matches devices advertising exactly this local name.
    public ScanFilter setName(String exactName) {
        this.name = exactName;
        return this;
    }

    /// Matches devices whose advertised local name starts with the given
    /// prefix.
    public ScanFilter setNamePrefix(String prefix) {
        this.namePrefix = prefix;
        return this;
    }

    /// Matches the device with the given address (see
    /// [com.codename1.bluetooth.BluetoothDevice#getAddress()] for the
    /// per-platform address semantics).
    public ScanFilter setAddress(String address) {
        this.address = address;
        return this;
    }

    /// Matches devices whose advertisement carries manufacturer data for
    /// `companyId` whose leading bytes equal `data` under `mask` (a `null`
    /// mask compares all of `data` exactly). Pass `null` data to match any
    /// payload for the company.
    public ScanFilter setManufacturerData(int companyId, byte[] data,
            byte[] mask) {
        this.manufacturerId = companyId;
        this.manufacturerData = data;
        this.manufacturerDataMask = mask;
        return this;
    }

    /// `true` when the given scan result satisfies every criterion of this
    /// filter. Used by the core scan demultiplexer; ports may also use it
    /// to pre-filter.
    public boolean matches(ScanResult result) {
        AdvertisementData ad = result.getAdvertisementData();
        if (address != null) {
            if (!address.equals(result.getPeripheral().getAddress())) {
                return false;
            }
        }
        if (name != null) {
            String n = ad == null ? null : ad.getLocalName();
            if (n == null) {
                n = result.getPeripheral().getName();
            }
            if (!name.equals(n)) {
                return false;
            }
        }
        if (namePrefix != null) {
            String n = ad == null ? null : ad.getLocalName();
            if (n == null) {
                n = result.getPeripheral().getName();
            }
            if (n == null || !n.startsWith(namePrefix)) {
                return false;
            }
        }
        if (serviceUuid != null) {
            if (ad == null || !ad.getServiceUuids().contains(serviceUuid)) {
                return false;
            }
        }
        if (manufacturerId >= 0) {
            byte[] payload = ad == null
                    ? null : ad.getManufacturerData(manufacturerId);
            if (payload == null) {
                return false;
            }
            if (manufacturerData != null) {
                if (payload.length < manufacturerData.length) {
                    return false;
                }
                for (int i = 0; i < manufacturerData.length; i++) {
                    byte m = manufacturerDataMask == null
                            || i >= manufacturerDataMask.length
                            ? (byte) 0xFF : manufacturerDataMask[i];
                    if ((payload[i] & m) != (manufacturerData[i] & m)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
