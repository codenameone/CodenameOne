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
package com.codename1.bluetooth.le;

/// Options for [BlePeripheral#connect(ConnectionOptions)]. Fluent setters
/// return `this` for chaining.
public class ConnectionOptions {

    private boolean autoConnect;
    private int timeout;

    /// When `true`, asks the platform to reconnect automatically whenever
    /// the peripheral comes back into range (Android `autoConnect`; iOS
    /// re-issues the connect request, which never times out there).
    /// Defaults to `false`: a single direct connection attempt.
    public ConnectionOptions setAutoConnect(boolean auto) {
        this.autoConnect = auto;
        return this;
    }

    /// Fails the connection attempt with
    /// [com.codename1.bluetooth.BluetoothError#TIMEOUT] after the given
    /// number of milliseconds. `0` (the default) uses the platform's own
    /// timeout behavior -- note that iOS connect requests never time out
    /// on their own.
    public ConnectionOptions setTimeout(int millis) {
        this.timeout = millis;
        return this;
    }

    /// The configured auto-connect flag.
    public boolean isAutoConnect() {
        return autoConnect;
    }

    /// The configured timeout in milliseconds; `0` means platform
    /// default.
    public int getTimeout() {
        return timeout;
    }
}
