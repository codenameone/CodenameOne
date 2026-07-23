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

import com.codename1.bluetooth.BluetoothDevice;
import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.classic.RfcommConnection;
import com.codename1.bluetooth.classic.RfcommServer;
import com.codename1.util.AsyncResource;

/**
 * Simulator {@link RfcommServer} over the virtual stack's RFCOMM listener
 * registry; virtual clients arrive via
 * {@link SimulatedBluetoothStack#connectVirtualRfcommClient(BluetoothUuid)}.
 */
class JavaSERfcommServer extends RfcommServer {

    private final SimulatedBluetoothStack stack;

    JavaSERfcommServer(BluetoothUuid serviceUuid,
            SimulatedBluetoothStack stack) {
        super(serviceUuid);
        this.stack = stack;
    }

    public AsyncResource<RfcommConnection> accept() {
        final AsyncResource<RfcommConnection> out =
                new AsyncResource<RfcommConnection>();
        stack.acceptRfcomm(getServiceUuid(),
                new SimulatedBluetoothStack.Callback<SimStreamChannel>() {
                    public void onSuccess(SimStreamChannel value) {
                        if (!out.isDone()) {
                            out.complete(new JavaSERfcommConnection(
                                    new VirtualClientDevice(), value));
                        }
                    }

                    public void onError(BluetoothError error,
                            String message) {
                        if (!out.isDone()) {
                            out.error(new BluetoothException(error, message));
                        }
                    }
                });
        return out;
    }

    public void close() {
        stack.closeRfcommServer(getServiceUuid());
    }

    /** The synthetic remote device of an accepted virtual client. */
    private static final class VirtualClientDevice extends BluetoothDevice {
        public String getAddress() {
            return "F0:00:00:00:00:FE";
        }

        public String getName() {
            return "Virtual RFCOMM Client";
        }
    }
}
