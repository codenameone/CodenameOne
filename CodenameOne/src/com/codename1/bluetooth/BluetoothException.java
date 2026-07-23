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

/// Thrown via the failure path of every `AsyncResource` returned by the
/// Bluetooth APIs. [#getError()] returns a typed [BluetoothError] so callers
/// can react without string-matching the message; for
/// [BluetoothError#GATT_ERROR] failures [#getGattStatus()] additionally
/// exposes the raw ATT status code reported by the remote device.
public class BluetoothException extends Exception {

    private final BluetoothError error;
    private final int gattStatus;

    public BluetoothException(BluetoothError error) {
        super(error == null ? "UNKNOWN" : error.name());
        this.error = error == null ? BluetoothError.UNKNOWN : error;
        this.gattStatus = -1;
    }

    public BluetoothException(BluetoothError error, String message) {
        super(message);
        this.error = error == null ? BluetoothError.UNKNOWN : error;
        this.gattStatus = -1;
    }

    public BluetoothException(BluetoothError error, String message,
            Throwable cause) {
        super(message, cause);
        this.error = error == null ? BluetoothError.UNKNOWN : error;
        this.gattStatus = -1;
    }

    public BluetoothException(BluetoothError error, String message,
            int gattStatus) {
        super(message);
        this.error = error == null ? BluetoothError.UNKNOWN : error;
        this.gattStatus = gattStatus;
    }

    /// Typed error code describing the failure. Never `null`.
    public BluetoothError getError() {
        return error;
    }

    /// The raw ATT status code reported by the remote GATT server when
    /// [#getError()] is [BluetoothError#GATT_ERROR]; `-1` otherwise.
    public int getGattStatus() {
        return gattStatus;
    }
}
