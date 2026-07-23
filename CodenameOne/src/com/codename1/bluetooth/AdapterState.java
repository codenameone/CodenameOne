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

/// Lifecycle states of the local Bluetooth adapter, a union of the Android
/// `BluetoothAdapter` states and the iOS `CBManagerState` values. Query via
/// [Bluetooth#getAdapterState()] and observe changes with
/// [Bluetooth#addAdapterStateListener(AdapterStateListener)].
public enum AdapterState {
    /// The state has not been determined yet -- e.g. iOS before the first
    /// CoreBluetooth manager callback fired.
    UNKNOWN,

    /// The device has no Bluetooth hardware, or the port does not implement
    /// Bluetooth at all. The fallback base [Bluetooth] class always reports
    /// this state.
    UNSUPPORTED,

    /// The app is not authorized to use Bluetooth (iOS privacy authorization
    /// denied / restricted, or missing runtime permissions on Android).
    UNAUTHORIZED,

    /// The adapter is off. Ask the user to enable it, optionally via
    /// [Bluetooth#requestEnable()].
    POWERED_OFF,

    /// The adapter is transitioning to [#POWERED_ON] (Android only; iOS
    /// reports the transition as a direct state change).
    TURNING_ON,

    /// The adapter is on and ready for scanning, connections and
    /// advertising.
    POWERED_ON,

    /// The adapter is transitioning to [#POWERED_OFF] (Android only).
    TURNING_OFF
}
