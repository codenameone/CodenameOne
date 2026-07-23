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
package com.codename1.bluetooth.le.server;

import com.codename1.bluetooth.gatt.GattStatus;

/// A write request from a connected central, delivered to
/// [GattServerListener#characteristicWriteRequest(GattWriteRequest)] or
/// [GattServerListener#descriptorWriteRequest(GattWriteRequest)]. When
/// [#isResponseRequired()] answer promptly with [#respond()] or
/// [#reject(GattStatus)].
public abstract class GattWriteRequest {

    private final BleCentral central;
    private final GattLocalCharacteristic characteristic;
    private final GattLocalDescriptor descriptor;
    private final byte[] value;
    private final int offset;
    private final boolean responseRequired;

    /// Constructed by ports; not application API.
    protected GattWriteRequest(BleCentral central,
            GattLocalCharacteristic characteristic,
            GattLocalDescriptor descriptor, byte[] value, int offset,
            boolean responseRequired) {
        this.central = central;
        this.characteristic = characteristic;
        this.descriptor = descriptor;
        this.value = value;
        this.offset = offset;
        this.responseRequired = responseRequired;
    }

    /// The central issuing the request.
    public BleCentral getCentral() {
        return central;
    }

    /// The written characteristic, or `null` for a descriptor write.
    public GattLocalCharacteristic getCharacteristic() {
        return characteristic;
    }

    /// The written descriptor, or `null` for a characteristic write.
    public GattLocalDescriptor getDescriptor() {
        return descriptor;
    }

    /// The written bytes.
    public byte[] getValue() {
        return value;
    }

    /// The write offset for long writes; `0` for plain writes.
    public int getOffset() {
        return offset;
    }

    /// `true` when the central expects an acknowledgement ([#respond()] or
    /// [#reject(GattStatus)]); `false` for write-without-response.
    public boolean isResponseRequired() {
        return responseRequired;
    }

    /// Acknowledges the write. May be called from any thread; call
    /// exactly once when [#isResponseRequired()].
    public abstract void respond();

    /// Rejects the write with the given ATT status.
    public abstract void reject(GattStatus status);
}
