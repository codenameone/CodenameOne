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
package com.codename1.bluetooth.classic;

import com.codename1.bluetooth.BluetoothDevice;

/// One device sighting during a classic discovery, delivered to a
/// [ClassicDiscoveryListener].
///
/// Instances are constructed by ports; application code never creates
/// them.
public class ClassicScanResult {

    /// [#getRssi()] value when the platform did not report a signal
    /// strength.
    public static final int RSSI_UNKNOWN = Short.MIN_VALUE;

    private final BluetoothDevice device;
    private final int rssi;
    private final int majorDeviceClass;
    private final int deviceClass;

    /// Constructed by ports when a discovery sighting arrives; not
    /// application API.
    public ClassicScanResult(BluetoothDevice device, int rssi,
            int majorDeviceClass, int deviceClass) {
        this.device = device;
        this.rssi = rssi;
        this.majorDeviceClass = majorDeviceClass;
        this.deviceClass = deviceClass;
    }

    /// The discovered device; connect via
    /// [BluetoothClassic#connect(BluetoothDevice,
    /// com.codename1.bluetooth.BluetoothUuid, boolean)].
    public BluetoothDevice getDevice() {
        return device;
    }

    /// The signal strength in dBm, or [#RSSI_UNKNOWN].
    public int getRssi() {
        return rssi;
    }

    /// The major device class from the Bluetooth Class-of-Device field
    /// (computer, phone, audio, ...).
    public int getMajorDeviceClass() {
        return majorDeviceClass;
    }

    /// The full device class from the Class-of-Device field.
    public int getDeviceClass() {
        return deviceClass;
    }

    public String toString() {
        return "ClassicScanResult(" + device.getAddress() + ")";
    }
}
