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

/// Typed error codes carried by every [BluetoothException] fired through the
/// failure path of the asynchronous Bluetooth APIs. Callers branch on these
/// via [BluetoothException#getError()] instead of string-matching messages.
public enum BluetoothError {
    /// The requested feature is not available on this port or device.
    /// Capability queries such as [Bluetooth#isLeSupported()] let
    /// cross-platform code branch before ever seeing this code; the no-op
    /// fallback base classes fail every operation with it.
    NOT_SUPPORTED,

    /// The adapter is powered off or otherwise unavailable. Ask the user to
    /// enable Bluetooth, optionally via [Bluetooth#requestEnable()].
    POWERED_OFF,

    /// A required runtime permission or OS authorization is missing --
    /// Android 12+ `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`/`BLUETOOTH_ADVERTISE`
    /// runtime grants, location on older Android, or the iOS Bluetooth
    /// privacy authorization. See [Bluetooth#requestPermissions].
    UNAUTHORIZED,

    /// The OS refused to start a scan or aborted a running one (throttling,
    /// too many concurrent scans, hardware error).
    SCAN_FAILED,

    /// Advertising could not be started -- payload too large, too many
    /// concurrent advertisers, or hardware limitation.
    ADVERTISE_FAILED,

    /// A connection attempt failed or timed out before it was established.
    CONNECTION_FAILED,

    /// An established link dropped unexpectedly. Delivered as the reason of
    /// the disconnection event and used to fail operations that were
    /// in-flight when the link died.
    CONNECTION_LOST,

    /// A GATT operation was attempted while the peripheral is not connected.
    NOT_CONNECTED,

    /// ATT-level failure reported by the remote GATT server.
    /// [BluetoothException#getGattStatus()] carries the raw status code.
    GATT_ERROR,

    /// Bonding/pairing failed or was rejected by the remote device.
    BOND_FAILED,

    /// The platform never delivered a completion callback for an operation
    /// within the safety timeout. The internal per-peripheral operation
    /// queue fails the operation with this code and moves on rather than
    /// wedging.
    TIMEOUT,

    /// Resource conflict -- e.g. an RFCOMM channel is already in use or the
    /// adapter is busy with a conflicting operation.
    BUSY,

    /// Transport I/O failure on a non-stream operation. Stream reads/writes
    /// on RFCOMM/L2CAP throw plain `java.io.IOException` instead.
    IO_ERROR,

    /// The user dismissed a system dialog (enable request, pairing prompt,
    /// device chooser) or the operation was cancelled via
    /// `AsyncResource.cancel()`.
    USER_CANCELED,

    /// Unclassified failure; the exception message carries the details.
    UNKNOWN
}
