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
import com.codename1.bluetooth.gatt.GattCharacteristic;

import java.util.ArrayList;
import java.util.List;

/// A characteristic definition of a local [GattLocalService] served by
/// this device's [GattServer]. Properties use the
/// [GattCharacteristic]`.PROPERTY_*` bits; access permissions use the
/// `PERMISSION_*` bits defined here.
public class GattLocalCharacteristic {

    /// The characteristic may be read (bit `0x01`).
    public static final int PERMISSION_READ = 0x01;
    /// The characteristic may be read over an encrypted link only (bit
    /// `0x02`).
    public static final int PERMISSION_READ_ENCRYPTED = 0x02;
    /// The characteristic may be written (bit `0x10`).
    public static final int PERMISSION_WRITE = 0x10;
    /// The characteristic may be written over an encrypted link only (bit
    /// `0x20`).
    public static final int PERMISSION_WRITE_ENCRYPTED = 0x20;

    private final BluetoothUuid uuid;
    private final int properties;
    private final int permissions;
    private byte[] value;
    private final ArrayList<GattLocalDescriptor> descriptors =
            new ArrayList<GattLocalDescriptor>();

    /// Creates a characteristic definition. `properties` uses
    /// [GattCharacteristic]`.PROPERTY_*` bits; `permissions` uses the
    /// `PERMISSION_*` bits of this class.
    public GattLocalCharacteristic(BluetoothUuid uuid, int properties,
            int permissions) {
        this.uuid = uuid;
        this.properties = properties;
        this.permissions = permissions;
    }

    /// Serves a static value without routing read requests to the
    /// [GattServerListener]. Without a static value, every read arrives
    /// as a [GattReadRequest].
    public GattLocalCharacteristic setValue(byte[] staticValue) {
        this.value = staticValue;
        return this;
    }

    /// Adds a descriptor definition.
    public GattLocalCharacteristic addDescriptor(GattLocalDescriptor d) {
        if (d != null) {
            descriptors.add(d);
        }
        return this;
    }

    /// The UUID identifying this characteristic.
    public BluetoothUuid getUuid() {
        return uuid;
    }

    /// The [GattCharacteristic]`.PROPERTY_*` bitmask.
    public int getProperties() {
        return properties;
    }

    /// The `PERMISSION_*` bitmask.
    public int getPermissions() {
        return permissions;
    }

    /// The static value, or `null` when reads are served via the
    /// [GattServerListener].
    public byte[] getValue() {
        return value;
    }

    /// The descriptor definitions added via
    /// [#addDescriptor(GattLocalDescriptor)].
    public List<GattLocalDescriptor> getDescriptors() {
        return new ArrayList<GattLocalDescriptor>(descriptors);
    }

    public String toString() {
        return "GattLocalCharacteristic(" + uuid + ")";
    }
}
