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

/// The runtime permissions the Bluetooth API may need, abstracted across
/// platforms. Query with [Bluetooth#hasPermission(BluetoothPermission)] and
/// request with [Bluetooth#requestPermissions(BluetoothPermission...)].
///
/// Mapping per platform:
/// - **Android 12+** -- `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` /
///   `BLUETOOTH_ADVERTISE` runtime permissions respectively.
/// - **Android 6-11** -- [#SCAN] maps to `ACCESS_FINE_LOCATION` (required
///   for BLE scan results); [#CONNECT] and [#ADVERTISE] need no runtime
///   grant.
/// - **iOS** -- a single Bluetooth privacy authorization covers all three.
public enum BluetoothPermission {
    /// Discovering nearby devices (BLE scanning, classic discovery).
    SCAN,

    /// Connecting to devices and using GATT / RFCOMM / L2CAP.
    CONNECT,

    /// Advertising and operating a local GATT server.
    ADVERTISE
}
