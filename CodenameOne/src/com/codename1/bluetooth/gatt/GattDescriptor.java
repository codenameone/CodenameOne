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
package com.codename1.bluetooth.gatt;

import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.util.AsyncResource;

/// A descriptor of a remote [GattCharacteristic]. Note that the Client
/// Characteristic Configuration descriptor ([BluetoothUuid#CCCD]) is
/// managed automatically by
/// [GattCharacteristic#subscribe(GattNotificationListener)] -- there is no
/// need to write it manually.
///
/// Instances are constructed by the port during service discovery;
/// application code never creates them.
public class GattDescriptor {

    private final GattCharacteristic characteristic;
    private final BluetoothUuid uuid;

    /// Constructed by ports during service discovery; not application API.
    public GattDescriptor(GattCharacteristic characteristic,
            BluetoothUuid uuid) {
        this.characteristic = characteristic;
        this.uuid = uuid;
    }

    /// The UUID identifying this descriptor.
    public BluetoothUuid getUuid() {
        return uuid;
    }

    /// The characteristic this descriptor belongs to.
    public GattCharacteristic getCharacteristic() {
        return characteristic;
    }

    /// Reads the descriptor value. Resolves with the raw bytes on the EDT
    /// or fails with a
    /// [com.codename1.bluetooth.BluetoothException].
    public AsyncResource<byte[]> read() {
        return characteristic.getService().getPeripheral()
                .readDescriptor(this);
    }

    /// Writes the descriptor value. Resolves `true` on the EDT once the
    /// remote acknowledged the write.
    public AsyncResource<Boolean> write(byte[] value) {
        return characteristic.getService().getPeripheral()
                .writeDescriptor(this, value);
    }

    public String toString() {
        return "GattDescriptor(" + uuid + ")";
    }
}
