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

/// A descriptor definition of a local [GattLocalCharacteristic] served by
/// this device's [GattServer]. Note that the Client Characteristic
/// Configuration descriptor is managed by the platform stack -- do not add
/// it manually.
public class GattLocalDescriptor {

    private final BluetoothUuid uuid;
    private final int permissions;
    private byte[] value;

    /// Creates a descriptor definition with
    /// [GattLocalCharacteristic]`.PERMISSION_*` bits.
    public GattLocalDescriptor(BluetoothUuid uuid, int permissions) {
        this.uuid = uuid;
        this.permissions = permissions;
    }

    /// Serves a static value without routing read requests to the
    /// [GattServerListener].
    public GattLocalDescriptor setValue(byte[] staticValue) {
        this.value = staticValue;
        return this;
    }

    /// The UUID identifying this descriptor.
    public BluetoothUuid getUuid() {
        return uuid;
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
}
