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
package com.codename1.impl.javase.bluetooth;

import com.codename1.bluetooth.BluetoothUuid;

import java.util.ArrayList;
import java.util.List;

/**
 * A GATT service of a {@link VirtualPeripheral} in the simulated Bluetooth
 * stack.
 */
public final class VirtualService {

    private final BluetoothUuid uuid;
    private boolean primary = true;
    private final ArrayList<VirtualCharacteristic> characteristics =
            new ArrayList<VirtualCharacteristic>();

    public VirtualService(BluetoothUuid uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid is required");
        }
        this.uuid = uuid;
    }

    public BluetoothUuid getUuid() {
        return uuid;
    }

    /** Marks this service secondary/primary; fluent. Defaults to primary. */
    public synchronized VirtualService setPrimary(boolean primary) {
        this.primary = primary;
        return this;
    }

    public synchronized boolean isPrimary() {
        return primary;
    }

    /** Adds a characteristic; fluent. */
    public synchronized VirtualService withCharacteristic(
            VirtualCharacteristic characteristic) {
        if (characteristic != null) {
            characteristics.add(characteristic);
        }
        return this;
    }

    /** The characteristics in registration order (a snapshot). */
    public synchronized List<VirtualCharacteristic> getCharacteristics() {
        return new ArrayList<VirtualCharacteristic>(characteristics);
    }

    /** The first characteristic with the given UUID, or {@code null}. */
    public synchronized VirtualCharacteristic getCharacteristic(
            BluetoothUuid uuid) {
        int size = characteristics.size();
        for (int i = 0; i < size; i++) {
            VirtualCharacteristic c = characteristics.get(i);
            if (c.getUuid().equals(uuid)) {
                return c;
            }
        }
        return null;
    }

    public String toString() {
        return "VirtualService(" + uuid + ")";
    }
}
