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

import java.util.ArrayList;
import java.util.List;

/// A characteristic of a remote [GattService] -- the unit of data exchange
/// with a BLE peripheral. Read, write and subscribe operations are routed
/// through the owning peripheral's internal operation queue, so any number
/// of calls may be issued concurrently; they execute in order.
///
/// Instances are constructed by the port during service discovery;
/// application code never creates them.
public class GattCharacteristic {

    /// The characteristic supports broadcast (bit `0x01`).
    public static final int PROPERTY_BROADCAST = 0x01;
    /// The characteristic can be read (bit `0x02`).
    public static final int PROPERTY_READ = 0x02;
    /// The characteristic can be written without response (bit `0x04`).
    public static final int PROPERTY_WRITE_WITHOUT_RESPONSE = 0x04;
    /// The characteristic can be written with response (bit `0x08`).
    public static final int PROPERTY_WRITE = 0x08;
    /// The characteristic supports notifications (bit `0x10`).
    public static final int PROPERTY_NOTIFY = 0x10;
    /// The characteristic supports indications (bit `0x20`).
    public static final int PROPERTY_INDICATE = 0x20;
    /// The characteristic supports signed writes (bit `0x40`).
    public static final int PROPERTY_SIGNED_WRITE = 0x40;
    /// The characteristic has extended properties (bit `0x80`).
    public static final int PROPERTY_EXTENDED_PROPS = 0x80;

    private final GattService service;
    private final BluetoothUuid uuid;
    private final int properties;
    private final int instanceId;
    private final ArrayList<GattDescriptor> descriptors =
            new ArrayList<GattDescriptor>();

    /// Constructed by ports during service discovery; not application API.
    public GattCharacteristic(GattService service, BluetoothUuid uuid,
            int properties, int instanceId) {
        this.service = service;
        this.uuid = uuid;
        this.properties = properties;
        this.instanceId = instanceId;
    }

    /// The UUID identifying this characteristic.
    public BluetoothUuid getUuid() {
        return uuid;
    }

    /// The service this characteristic belongs to.
    public GattService getService() {
        return service;
    }

    /// The raw `PROPERTY_*` bitmask as advertised by the peripheral.
    public int getProperties() {
        return properties;
    }

    /// Disambiguates multiple instances of the same characteristic UUID
    /// within one service.
    public int getInstanceId() {
        return instanceId;
    }

    /// `true` when [#PROPERTY_READ] is set.
    public boolean canRead() {
        return (properties & PROPERTY_READ) != 0;
    }

    /// `true` when [#PROPERTY_WRITE] is set.
    public boolean canWrite() {
        return (properties & PROPERTY_WRITE) != 0;
    }

    /// `true` when [#PROPERTY_WRITE_WITHOUT_RESPONSE] is set.
    public boolean canWriteWithoutResponse() {
        return (properties & PROPERTY_WRITE_WITHOUT_RESPONSE) != 0;
    }

    /// `true` when [#PROPERTY_NOTIFY] is set.
    public boolean canNotify() {
        return (properties & PROPERTY_NOTIFY) != 0;
    }

    /// `true` when [#PROPERTY_INDICATE] is set.
    public boolean canIndicate() {
        return (properties & PROPERTY_INDICATE) != 0;
    }

    /// The descriptors of this characteristic, in discovery order.
    public List<GattDescriptor> getDescriptors() {
        return new ArrayList<GattDescriptor>(descriptors);
    }

    /// The first descriptor with the given UUID, or `null`.
    public GattDescriptor getDescriptor(BluetoothUuid uuid) {
        int size = descriptors.size();
        for (int i = 0; i < size; i++) {
            GattDescriptor d = descriptors.get(i);
            if (d.getUuid().equals(uuid)) {
                return d;
            }
        }
        return null;
    }

    /// Called by ports while building the discovered GATT database; not
    /// application API.
    public void addDescriptor(GattDescriptor descriptor) {
        descriptors.add(descriptor);
    }

    /// Reads the current value. Resolves with the raw bytes on the EDT or
    /// fails with a [com.codename1.bluetooth.BluetoothException].
    public AsyncResource<byte[]> read() {
        return service.getPeripheral().readCharacteristic(this);
    }

    /// Writes the value with response. Resolves `true` on the EDT once the
    /// peripheral acknowledged the write.
    public AsyncResource<Boolean> write(byte[] value) {
        return service.getPeripheral().writeCharacteristic(this, value, true);
    }

    /// Writes the value without response. Resolves `true` once the value
    /// was queued to the controller -- there is no remote acknowledgement.
    public AsyncResource<Boolean> writeWithoutResponse(byte[] value) {
        return service.getPeripheral().writeCharacteristic(this, value, false);
    }

    /// Subscribes to value changes. The first listener triggers the CCCD
    /// write that arms notifications (indications are used when the
    /// characteristic only supports [#PROPERTY_INDICATE]); the returned
    /// resource resolves `true` once armed. Additional listeners resolve
    /// immediately. Values stream to the listener on the EDT until
    /// [#unsubscribe(GattNotificationListener)] or disconnection.
    public AsyncResource<Boolean> subscribe(GattNotificationListener l) {
        return service.getPeripheral().subscribe(this, l);
    }

    /// Removes a listener added via
    /// [#subscribe(GattNotificationListener)]. When the last listener is
    /// removed the CCCD is written to disarm notifications; the returned
    /// resource resolves once done.
    public AsyncResource<Boolean> unsubscribe(GattNotificationListener l) {
        return service.getPeripheral().unsubscribe(this, l);
    }

    /// `true` while notifications/indications are armed for this
    /// characteristic.
    public boolean isSubscribed() {
        return service.getPeripheral().isSubscribed(this);
    }

    public String toString() {
        return "GattCharacteristic(" + uuid + ")";
    }
}
