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

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.List;

/// The local GATT server, opened via `BluetoothLE.openGattServer` when
/// this device acts as a BLE peripheral. Add [GattLocalService]
/// definitions, respond to central requests through the
/// [GattServerListener] and push value changes with
/// [#notifyValue(GattLocalCharacteristic, byte[])].
///
/// All listener callbacks are delivered on the EDT; the `fire*` methods
/// ports call are safe from any thread.
public abstract class GattServer {

    private final GattServerListener listener;

    /// Ports construct subclasses with the listener the application
    /// passed to `openGattServer`.
    protected GattServer(GattServerListener listener) {
        this.listener = listener;
    }

    /// Publishes a service definition. Resolves `true` once the platform
    /// registered the service.
    public final AsyncResource<Boolean> addService(GattLocalService service) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        if (service == null) {
            out.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "addService requires a service"));
            return out;
        }
        try {
            doAddService(service, out);
        } catch (RuntimeException ex) {
            if (!out.isDone()) {
                out.error(new BluetoothException(BluetoothError.UNKNOWN,
                        "addService failed: " + ex, ex));
            }
        }
        return out;
    }

    /// Removes a previously added service.
    public abstract void removeService(GattLocalService service);

    /// Shuts the server down and disconnects its centrals.
    public abstract void close();

    /// The centrals currently connected to this server.
    public abstract List<BleCentral> getConnectedCentrals();

    /// Notifies every subscribed central of a new characteristic value.
    /// Resolves once the controller accepted the notification(s).
    public final AsyncResource<Boolean> notifyValue(
            GattLocalCharacteristic characteristic, byte[] value) {
        return notifyCentral(null, characteristic, value, false);
    }

    /// Notifies one central -- or all subscribed centrals when `central`
    /// is `null`. With `confirm` an *indication* is sent and the resource
    /// resolves on the central's acknowledgement.
    public final AsyncResource<Boolean> notifyCentral(BleCentral central,
            GattLocalCharacteristic characteristic, byte[] value,
            boolean confirm) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        if (characteristic == null) {
            out.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "notify requires a characteristic"));
            return out;
        }
        try {
            doNotify(central, characteristic, value, confirm, out);
        } catch (RuntimeException ex) {
            if (!out.isDone()) {
                out.error(new BluetoothException(BluetoothError.UNKNOWN,
                        "notify failed: " + ex, ex));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // port SPI
    // ------------------------------------------------------------------

    /// Registers the service with the platform stack and completes `out`.
    /// Ports must serialize consecutive service additions where the
    /// platform requires it (Android).
    protected abstract void doAddService(GattLocalService service,
            AsyncResource<Boolean> out);

    /// Sends a notification/indication to `central` (or all subscribed
    /// centrals when `null`) and completes `out`. Ports must serialize
    /// notifications where the platform requires it (Android's
    /// `onNotificationSent`).
    protected abstract void doNotify(BleCentral central,
            GattLocalCharacteristic characteristic, byte[] value,
            boolean confirm, AsyncResource<Boolean> out);

    // ------------------------------------------------------------------
    // port event entry points -- safe to call from any thread
    // ------------------------------------------------------------------

    /// Routes a characteristic read request to the listener on the EDT.
    protected final void fireCharacteristicReadRequest(
            final GattReadRequest request) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                listener.characteristicReadRequest(request);
            }
        });
    }

    /// Routes a characteristic write request to the listener on the EDT.
    protected final void fireCharacteristicWriteRequest(
            final GattWriteRequest request) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                listener.characteristicWriteRequest(request);
            }
        });
    }

    /// Routes a descriptor read request to the listener on the EDT.
    protected final void fireDescriptorReadRequest(
            final GattReadRequest request) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                listener.descriptorReadRequest(request);
            }
        });
    }

    /// Routes a descriptor write request to the listener on the EDT.
    protected final void fireDescriptorWriteRequest(
            final GattWriteRequest request) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                listener.descriptorWriteRequest(request);
            }
        });
    }

    /// Reports a subscription change to the listener on the EDT.
    protected final void fireSubscriptionChanged(final BleCentral central,
            final GattLocalCharacteristic characteristic,
            final boolean subscribed) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                listener.subscriptionChanged(central, characteristic,
                        subscribed);
            }
        });
    }

    /// Reports a central connection to the listener on the EDT.
    protected final void fireCentralConnected(final BleCentral central) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                listener.centralConnected(central);
            }
        });
    }

    /// Reports a central disconnection to the listener on the EDT.
    protected final void fireCentralDisconnected(final BleCentral central) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                listener.centralDisconnected(central);
            }
        });
    }
}
