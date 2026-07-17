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

/// A read request from a connected central, delivered to
/// [GattServerListener#characteristicReadRequest(GattReadRequest)] or
/// [GattServerListener#descriptorReadRequest(GattReadRequest)]. Answer
/// promptly with [#respond(byte[])] or [#reject(GattStatus)] -- centrals
/// time out unanswered requests.
public abstract class GattReadRequest {

    private final BleCentral central;
    private final GattLocalCharacteristic characteristic;
    private final GattLocalDescriptor descriptor;
    private final int offset;

    /// Constructed by ports; not application API.
    protected GattReadRequest(BleCentral central,
            GattLocalCharacteristic characteristic,
            GattLocalDescriptor descriptor, int offset) {
        this.central = central;
        this.characteristic = characteristic;
        this.descriptor = descriptor;
        this.offset = offset;
    }

    /// The central issuing the request.
    public BleCentral getCentral() {
        return central;
    }

    /// The requested characteristic, or `null` for a descriptor read.
    public GattLocalCharacteristic getCharacteristic() {
        return characteristic;
    }

    /// The requested descriptor, or `null` for a characteristic read.
    public GattLocalDescriptor getDescriptor() {
        return descriptor;
    }

    /// The read offset for long reads; `0` for plain reads.
    public int getOffset() {
        return offset;
    }

    /// Sends the value to the central. May be called from any thread;
    /// call exactly once per request.
    public abstract void respond(byte[] value);

    /// Rejects the request with the given ATT status.
    public abstract void reject(GattStatus status);
}
