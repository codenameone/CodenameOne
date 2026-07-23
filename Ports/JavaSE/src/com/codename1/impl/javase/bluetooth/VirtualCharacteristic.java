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
import com.codename1.bluetooth.gatt.GattCharacteristic;

import java.util.ArrayList;
import java.util.List;

/**
 * A characteristic of a {@link VirtualService} on a simulated remote
 * peripheral. Properties use the core
 * {@link GattCharacteristic}{@code .PROPERTY_*} bits. The value is mutable
 * so it can be edited live (from tests or the future Simulate menu UI)
 * while an app is connected.
 */
public final class VirtualCharacteristic {

    private final BluetoothUuid uuid;
    private final int properties;
    private byte[] value;
    private final ArrayList<VirtualDescriptor> descriptors =
            new ArrayList<VirtualDescriptor>();

    public VirtualCharacteristic(BluetoothUuid uuid, int properties) {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid is required");
        }
        this.uuid = uuid;
        this.properties = properties;
    }

    public VirtualCharacteristic(BluetoothUuid uuid, int properties,
            byte[] value) {
        this(uuid, properties);
        setValue(value);
    }

    public BluetoothUuid getUuid() {
        return uuid;
    }

    /** The {@link GattCharacteristic}{@code .PROPERTY_*} bitmask. */
    public int getProperties() {
        return properties;
    }

    public boolean canRead() {
        return (properties & GattCharacteristic.PROPERTY_READ) != 0;
    }

    public boolean canWrite() {
        return (properties & (GattCharacteristic.PROPERTY_WRITE
                | GattCharacteristic.PROPERTY_WRITE_WITHOUT_RESPONSE)) != 0;
    }

    public boolean canNotifyOrIndicate() {
        return (properties & (GattCharacteristic.PROPERTY_NOTIFY
                | GattCharacteristic.PROPERTY_INDICATE)) != 0;
    }

    /** Sets the characteristic value (copied); fluent. */
    public synchronized VirtualCharacteristic setValue(byte[] value) {
        this.value = ByteArrays.copy(value);
        return this;
    }

    /** The current characteristic value (a copy); never {@code null}. */
    public synchronized byte[] getValue() {
        return ByteArrays.copy(value);
    }

    /** Adds a descriptor; fluent. */
    public synchronized VirtualCharacteristic withDescriptor(
            VirtualDescriptor descriptor) {
        if (descriptor != null) {
            descriptors.add(descriptor);
        }
        return this;
    }

    /** The descriptors in registration order (a snapshot). */
    public synchronized List<VirtualDescriptor> getDescriptors() {
        return new ArrayList<VirtualDescriptor>(descriptors);
    }

    /** The first descriptor with the given UUID, or {@code null}. */
    public synchronized VirtualDescriptor getDescriptor(BluetoothUuid uuid) {
        int size = descriptors.size();
        for (int i = 0; i < size; i++) {
            VirtualDescriptor d = descriptors.get(i);
            if (d.getUuid().equals(uuid)) {
                return d;
            }
        }
        return null;
    }

    public String toString() {
        return "VirtualCharacteristic(" + uuid + ")";
    }
}
