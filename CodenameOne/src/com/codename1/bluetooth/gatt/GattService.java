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
import com.codename1.bluetooth.le.BlePeripheral;

import java.util.ArrayList;
import java.util.List;

/// A service discovered on a remote GATT server via
/// [BlePeripheral#discoverServices()]. Groups the
/// [GattCharacteristic]s the peripheral exposes.
///
/// Instances are constructed by the port during service discovery;
/// application code never creates them.
public class GattService {

    private final BlePeripheral peripheral;
    private final BluetoothUuid uuid;
    private final boolean primary;
    private final int instanceId;
    private final ArrayList<GattCharacteristic> characteristics =
            new ArrayList<GattCharacteristic>();
    private final ArrayList<GattService> includedServices =
            new ArrayList<GattService>();

    /// Constructed by ports during service discovery; not application API.
    public GattService(BlePeripheral peripheral, BluetoothUuid uuid,
            boolean primary, int instanceId) {
        this.peripheral = peripheral;
        this.uuid = uuid;
        this.primary = primary;
        this.instanceId = instanceId;
    }

    /// The UUID identifying this service.
    public BluetoothUuid getUuid() {
        return uuid;
    }

    /// `true` for a primary service, `false` for a secondary one.
    public boolean isPrimary() {
        return primary;
    }

    /// Disambiguates multiple instances of the same service UUID on one
    /// peripheral.
    public int getInstanceId() {
        return instanceId;
    }

    /// The peripheral this service belongs to.
    public BlePeripheral getPeripheral() {
        return peripheral;
    }

    /// The characteristics of this service, in discovery order.
    public List<GattCharacteristic> getCharacteristics() {
        return new ArrayList<GattCharacteristic>(characteristics);
    }

    /// The first characteristic with the given UUID, or `null` when the
    /// service has none.
    public GattCharacteristic getCharacteristic(BluetoothUuid uuid) {
        int size = characteristics.size();
        for (int i = 0; i < size; i++) {
            GattCharacteristic c = characteristics.get(i);
            if (c.getUuid().equals(uuid)) {
                return c;
            }
        }
        return null;
    }

    /// Secondary services included by this service; usually empty.
    public List<GattService> getIncludedServices() {
        return new ArrayList<GattService>(includedServices);
    }

    /// Called by ports while building the discovered GATT database; not
    /// application API.
    public void addCharacteristic(GattCharacteristic characteristic) {
        characteristics.add(characteristic);
    }

    /// Called by ports while building the discovered GATT database; not
    /// application API.
    public void addIncludedService(GattService service) {
        includedServices.add(service);
    }

    @Override
    public String toString() {
        return "GattService(" + uuid + ")";
    }
}
