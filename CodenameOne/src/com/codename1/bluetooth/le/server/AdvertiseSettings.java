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

/// Options for `BluetoothLE.startAdvertising`. Fluent setters return
/// `this` for chaining.
public class AdvertiseSettings {

    private AdvertiseMode mode = AdvertiseMode.BALANCED;
    private TxPowerLevel txPower = TxPowerLevel.MEDIUM;
    private boolean connectable = true;
    private int timeout;

    /// The advertising-interval trade-off; defaults to
    /// [AdvertiseMode#BALANCED].
    public AdvertiseSettings setMode(AdvertiseMode mode) {
        this.mode = mode == null ? AdvertiseMode.BALANCED : mode;
        return this;
    }

    /// The transmit power; defaults to [TxPowerLevel#MEDIUM].
    public AdvertiseSettings setTxPower(TxPowerLevel level) {
        this.txPower = level == null ? TxPowerLevel.MEDIUM : level;
        return this;
    }

    /// Whether centrals may connect (the default) or the advertisement is
    /// broadcast-only (beacons).
    public AdvertiseSettings setConnectable(boolean connectable) {
        this.connectable = connectable;
        return this;
    }

    /// Stops advertising automatically after the given number of
    /// milliseconds; `0` (the default) advertises until
    /// [BleAdvertisement#stop()].
    public AdvertiseSettings setTimeout(int millis) {
        this.timeout = millis;
        return this;
    }

    /// The configured advertise mode.
    public AdvertiseMode getMode() {
        return mode;
    }

    /// The configured transmit power.
    public TxPowerLevel getTxPower() {
        return txPower;
    }

    /// Whether the advertisement accepts connections.
    public boolean isConnectable() {
        return connectable;
    }

    /// The configured timeout in milliseconds; `0` means until stopped.
    public int getTimeout() {
        return timeout;
    }
}
