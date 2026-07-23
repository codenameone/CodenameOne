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

/// A remote central connected to the local [GattServer] -- the mirror
/// image of `BlePeripheral` when this device acts as the peripheral.
public abstract class BleCentral {

    private int mtu = 23;

    /// Constructed by ports; not application API.
    protected BleCentral() {
    }

    /// A stable identifier of the connected central (platform-specific,
    /// same semantics as
    /// [com.codename1.bluetooth.BluetoothDevice#getAddress()]).
    public abstract String getAddress();

    /// The MTU negotiated with this central; `23` until negotiated
    /// higher.
    public int getMtu() {
        return mtu;
    }

    /// Records the negotiated MTU; called by ports.
    protected void setMtu(int mtu) {
        this.mtu = mtu;
    }
}
