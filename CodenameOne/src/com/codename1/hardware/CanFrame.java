/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.hardware;

/// One classic CAN frame: an identifier and up to eight data bytes.
@com.codename1.impl.SharedWithBackend
public final class CanFrame {

    private final int id;
    private final boolean extended;
    private final byte[] data;

    /// Creates a frame.
    ///
    /// #### Parameters
    ///
    /// - `id`: an 11-bit identifier, or 29-bit when `extended`
    ///
    /// - `extended`: `true` for a 29-bit identifier
    ///
    /// - `data`: zero to eight payload bytes; the array is copied
    public CanFrame(int id, boolean extended, byte[] data) {
        int max = extended ? 0x1fffffff : 0x7ff;
        if (id < 0 || id > max) {
            throw new IllegalArgumentException("CAN identifier out of range: " + id);
        }
        if (data == null || data.length > 8) {
            throw new IllegalArgumentException("CAN payload must be 0..8 bytes");
        }
        this.id = id;
        this.extended = extended;
        this.data = new byte[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    /// The identifier.
    public int getId() {
        return id;
    }

    /// `true` for a 29-bit identifier.
    public boolean isExtended() {
        return extended;
    }

    /// A copy of the payload.
    public byte[] getData() {
        byte[] out = new byte[data.length];
        System.arraycopy(data, 0, out, 0, data.length);
        return out;
    }

    /// The payload length.
    public int getLength() {
        return data.length;
    }
}
