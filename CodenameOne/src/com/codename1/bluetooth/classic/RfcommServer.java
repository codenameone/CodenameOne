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
package com.codename1.bluetooth.classic;

import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.util.AsyncResource;

/// A listening RFCOMM endpoint (SPP server) registered with the local SDP
/// database via [BluetoothClassic#listen(String, BluetoothUuid, boolean)].
public abstract class RfcommServer {

    private final BluetoothUuid serviceUuid;

    /// Constructed by ports; not application API.
    protected RfcommServer(BluetoothUuid serviceUuid) {
        this.serviceUuid = serviceUuid;
    }

    /// Resolves with the next incoming [RfcommConnection]. Call again
    /// after each resolution to accept further clients.
    public abstract AsyncResource<RfcommConnection> accept();

    /// Stops listening; pending [#accept()] calls fail with
    /// [com.codename1.bluetooth.BluetoothError#IO_ERROR].
    public abstract void close();

    /// The service UUID this server was registered under.
    public BluetoothUuid getServiceUuid() {
        return serviceUuid;
    }
}
