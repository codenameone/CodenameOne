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
package com.codename1.impl.javase.bluetooth;

import com.codename1.bluetooth.BluetoothUuid;

/**
 * A descriptor of a {@link VirtualCharacteristic} on a simulated remote
 * peripheral. The value is mutable so it can be edited live (from tests or
 * the future Simulate menu UI) while an app is connected.
 */
public final class VirtualDescriptor {

    private final BluetoothUuid uuid;
    private byte[] value;

    public VirtualDescriptor(BluetoothUuid uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid is required");
        }
        this.uuid = uuid;
    }

    public VirtualDescriptor(BluetoothUuid uuid, byte[] value) {
        this(uuid);
        setValue(value);
    }

    public BluetoothUuid getUuid() {
        return uuid;
    }

    /** Sets the descriptor value (copied); fluent. */
    public synchronized VirtualDescriptor setValue(byte[] value) {
        this.value = ByteArrays.copy(value);
        return this;
    }

    /** The current descriptor value (a copy); never {@code null}. */
    public synchronized byte[] getValue() {
        return ByteArrays.copy(value);
    }

    public String toString() {
        return "VirtualDescriptor(" + uuid + ")";
    }
}
