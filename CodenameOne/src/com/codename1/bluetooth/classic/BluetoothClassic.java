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

import com.codename1.bluetooth.BluetoothDevice;
import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.List;

/// The classic Bluetooth (BR/EDR) role: device discovery, bonding and
/// RFCOMM stream connections. Obtain via
/// `Bluetooth.getInstance().getClassic()`; the instance is owned by the
/// active port and never `null`.
///
/// Classic Bluetooth is available on Android, the JavaSE simulator and
/// desktop targets. iOS does not expose it to applications -- branch via
/// `Bluetooth.getInstance().isClassicSupported()`; on unsupported ports
/// every operation fails fast with [BluetoothError#NOT_SUPPORTED].
public class BluetoothClassic {

    /// Ports construct subclasses. Application code obtains the active
    /// instance via `Bluetooth.getInstance().getClassic()`.
    protected BluetoothClassic() {
    }

    /// Starts an inquiry scan (~12 seconds); the listener fires on the
    /// EDT per sighting. Returns a live [ClassicDiscovery] handle that
    /// resolves when the inquiry ends. On ports without classic Bluetooth
    /// the handle is already failed with
    /// [BluetoothError#NOT_SUPPORTED].
    public ClassicDiscovery startDiscovery(ClassicDiscoveryListener listener) {
        ClassicDiscovery d = new ClassicDiscovery();
        d.error(notSupported());
        return d;
    }

    /// The classic devices bonded with this adapter. Empty on ports
    /// without classic Bluetooth.
    public List<BluetoothDevice> getBondedDevices() {
        return new ArrayList<BluetoothDevice>();
    }

    /// Initiates bonding with the given device, prompting the user where
    /// the platform requires it. Resolves `true` once bonded.
    public AsyncResource<Boolean> createBond(BluetoothDevice device) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.error(notSupported());
        return r;
    }

    /// Asks the user to make this device discoverable for the given
    /// duration (the Android system dialog). Resolves `true` when
    /// granted; `false` when declined or unsupported.
    public AsyncResource<Boolean> requestDiscoverable(int durationSeconds) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.complete(Boolean.FALSE);
        return r;
    }

    /// Opens an RFCOMM connection to the given device's service --
    /// [BluetoothUuid#SPP] for classic serial-port devices. `secure`
    /// requests an authenticated, encrypted link (pairing if needed).
    public AsyncResource<RfcommConnection> connect(BluetoothDevice device,
            BluetoothUuid serviceUuid, boolean secure) {
        AsyncResource<RfcommConnection> r =
                new AsyncResource<RfcommConnection>();
        r.error(notSupported());
        return r;
    }

    /// Opens an RFCOMM connection to the device with the given address
    /// (persisted earlier) without a prior discovery.
    public AsyncResource<RfcommConnection> connect(String address,
            BluetoothUuid serviceUuid, boolean secure) {
        AsyncResource<RfcommConnection> r =
                new AsyncResource<RfcommConnection>();
        r.error(notSupported());
        return r;
    }

    /// Registers a listening RFCOMM endpoint (SPP server) under the given
    /// name and service UUID with the local SDP database. Resolves with
    /// the [RfcommServer]; call [RfcommServer#accept()] to take clients.
    public AsyncResource<RfcommServer> listen(String serviceName,
            BluetoothUuid serviceUuid, boolean secure) {
        AsyncResource<RfcommServer> r = new AsyncResource<RfcommServer>();
        r.error(notSupported());
        return r;
    }

    private static BluetoothException notSupported() {
        return new BluetoothException(BluetoothError.NOT_SUPPORTED,
                "Classic Bluetooth is not supported on this platform");
    }
}
