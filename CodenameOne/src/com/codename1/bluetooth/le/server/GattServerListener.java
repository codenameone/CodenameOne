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

/// Receives the events of a local [GattServer]: requests from centrals,
/// subscription changes and central connections. All methods are invoked
/// on the EDT. Extend [GattServerAdapter] to override only the events you
/// care about.
public interface GattServerListener {

    /// A central reads a characteristic without a static value; answer
    /// via [GattReadRequest#respond(byte[])] or
    /// [GattReadRequest#reject].
    void characteristicReadRequest(GattReadRequest request);

    /// A central writes a characteristic; acknowledge via
    /// [GattWriteRequest#respond()] when required.
    void characteristicWriteRequest(GattWriteRequest request);

    /// A central reads a descriptor without a static value.
    void descriptorReadRequest(GattReadRequest request);

    /// A central writes a descriptor.
    void descriptorWriteRequest(GattWriteRequest request);

    /// A central subscribed to or unsubscribed from a characteristic's
    /// notifications.
    void subscriptionChanged(BleCentral central,
            GattLocalCharacteristic characteristic, boolean subscribed);

    /// A central connected to the local server.
    void centralConnected(BleCentral central);

    /// A central disconnected from the local server.
    void centralDisconnected(BleCentral central);
}
