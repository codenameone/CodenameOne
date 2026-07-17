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
package com.codename1.bluetooth;

/// Identity of a remote Bluetooth device, shared by the LE and classic
/// stacks. Ports construct concrete subclasses; application code receives
/// them from scans, discovery, and bonded-device listings.
///
/// Two devices are equal when their [#getAddress()] values are equal, so
/// instances can be used directly as map keys.
public abstract class BluetoothDevice {

    /// Ports construct subclasses; application code receives instances from
    /// the scanning and discovery APIs.
    protected BluetoothDevice() {
    }

    /// A stable identifier for this device: the MAC address on Android and
    /// the desktop ports (`AA:BB:CC:DD:EE:FF`), but the per-app CoreBluetooth
    /// identifier UUID string on iOS. The value is stable for this app on
    /// this device and safe to persist for reconnection **on the same
    /// install**, but it is NOT portable across phones or platforms -- never
    /// treat it as the peripheral's real hardware address.
    public abstract String getAddress();

    /// The advertised or cached device name; may be `null` when the device
    /// never advertised one.
    public abstract String getName();

    /// The transport family of this device; defaults to
    /// [DeviceType#UNKNOWN].
    public DeviceType getType() {
        return DeviceType.UNKNOWN;
    }

    /// The current pairing state of this device; defaults to
    /// [BondState#NONE]. iOS manages bonds internally and usually reports
    /// [BondState#NONE] -- see [BondState].
    public BondState getBondState() {
        return BondState.NONE;
    }

    /// Devices are equal when their [#getAddress()] values are equal.
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BluetoothDevice)) {
            return false;
        }
        String a = getAddress();
        return a != null && a.equals(((BluetoothDevice) o).getAddress());
    }

    public final int hashCode() {
        String a = getAddress();
        return a == null ? System.identityHashCode(this) : a.hashCode();
    }

    public String toString() {
        return "BluetoothDevice(" + getAddress() + ", " + getName() + ")";
    }
}
