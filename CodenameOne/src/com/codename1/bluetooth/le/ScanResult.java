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
package com.codename1.bluetooth.le;

/// One advertisement sighting delivered to a
/// [ScanListener] during a scan. Carries the [BlePeripheral] handle used
/// to connect, the signal strength and the parsed advertisement payload.
///
/// Instances are constructed by ports; application code never creates
/// them.
public class ScanResult {

    private final BlePeripheral peripheral;
    private final int rssi;
    private final AdvertisementData advertisementData;
    private final boolean connectable;
    private final long timestamp;

    /// Constructed by ports when a scan sighting arrives; not application
    /// API.
    public ScanResult(BlePeripheral peripheral, int rssi,
            AdvertisementData advertisementData, boolean connectable,
            long timestamp) {
        this.peripheral = peripheral;
        this.rssi = rssi;
        this.advertisementData = advertisementData == null
                ? new AdvertisementData() : advertisementData;
        this.connectable = connectable;
        this.timestamp = timestamp;
    }

    /// The discovered peripheral; call [BlePeripheral#connect()] on it to
    /// establish a connection.
    public BlePeripheral getPeripheral() {
        return peripheral;
    }

    /// The received signal strength of this sighting in dBm.
    public int getRssi() {
        return rssi;
    }

    /// The parsed advertisement payload; never `null`.
    public AdvertisementData getAdvertisementData() {
        return advertisementData;
    }

    /// `true` when the advertisement indicates the peripheral accepts
    /// connections.
    public boolean isConnectable() {
        return connectable;
    }

    /// The sighting time -- `System.currentTimeMillis()` terms on device
    /// ports; the simulator's virtual stack uses a monotonic counter, so
    /// treat the value as ordered rather than as a wall-clock time.
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "ScanResult(" + peripheral.getAddress() + ", rssi=" + rssi + ")";
    }
}
