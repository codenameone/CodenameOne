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
import java.util.List;

/// A service definition added to this device's [GattServer] via
/// [GattServer#addService(GattLocalService)].
public class GattLocalService {

    private final BluetoothUuid uuid;
    private final boolean primary;
    private final ArrayList<GattLocalCharacteristic> characteristics =
            new ArrayList<GattLocalCharacteristic>();

    /// Creates a primary service definition.
    public GattLocalService(BluetoothUuid uuid) {
        this(uuid, true);
    }

    /// Creates a service definition; `primary` is `false` for secondary
    /// services.
    public GattLocalService(BluetoothUuid uuid, boolean primary) {
        this.uuid = uuid;
        this.primary = primary;
    }

    /// Adds a characteristic definition; fluent.
    public GattLocalService addCharacteristic(GattLocalCharacteristic c) {
        if (c != null) {
            characteristics.add(c);
        }
        return this;
    }

    /// The UUID identifying this service.
    public BluetoothUuid getUuid() {
        return uuid;
    }

    /// `true` for a primary service.
    public boolean isPrimary() {
        return primary;
    }

    /// The characteristic definitions added via
    /// [#addCharacteristic(GattLocalCharacteristic)].
    public List<GattLocalCharacteristic> getCharacteristics() {
        return new ArrayList<GattLocalCharacteristic>(characteristics);
    }

    @Override
    public String toString() {
        return "GattLocalService(" + uuid + ")";
    }
}
