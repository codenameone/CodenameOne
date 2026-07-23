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

/// ATT protocol status codes, used both to interpret remote GATT failures
/// and to respond to requests when acting as a GATT server. [#getAttCode()]
/// returns the raw code from the Bluetooth specification.
public enum GattStatus {
    /// The operation completed successfully.
    SUCCESS(0x00),

    /// The attribute handle is invalid on this server.
    INVALID_HANDLE(0x01),

    /// The attribute cannot be read.
    READ_NOT_PERMITTED(0x02),

    /// The attribute cannot be written.
    WRITE_NOT_PERMITTED(0x03),

    /// The request requires authentication (bonding/pairing) first.
    INSUFFICIENT_AUTHENTICATION(0x05),

    /// The server does not support the request.
    REQUEST_NOT_SUPPORTED(0x06),

    /// The offset of a long read/write is past the end of the attribute.
    INVALID_OFFSET(0x07),

    /// The request requires an encrypted link.
    INSUFFICIENT_ENCRYPTION(0x0F),

    /// The value length is invalid for this attribute.
    INVALID_ATTRIBUTE_VALUE_LENGTH(0x0D),

    /// Unlikely, unclassified error.
    UNLIKELY_ERROR(0x0E);

    private final int attCode;

    GattStatus(int attCode) {
        this.attCode = attCode;
    }

    /// The raw ATT status code from the Bluetooth specification.
    public int getAttCode() {
        return attCode;
    }

    /// Maps a raw ATT status code to its enum constant, falling back to
    /// [#UNLIKELY_ERROR] for codes without a dedicated constant.
    public static GattStatus fromAttCode(int code) {
        for (GattStatus s : values()) {
            if (s.attCode == code) {
                return s;
            }
        }
        return UNLIKELY_ERROR;
    }
}
