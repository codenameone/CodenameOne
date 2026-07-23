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

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.le.L2capChannel;
import com.codename1.bluetooth.le.L2capServer;
import com.codename1.util.AsyncResource;

/**
 * Simulator {@link L2capServer} over the virtual stack's published-PSM
 * registry; virtual clients arrive via
 * {@link SimulatedBluetoothStack#connectVirtualL2capClient(int)}.
 */
class SimL2capServer extends L2capServer {

    private final SimulatedBluetoothStack stack;
    private final int psm;

    SimL2capServer(SimulatedBluetoothStack stack, int psm) {
        this.stack = stack;
        this.psm = psm;
    }

    public int getPsm() {
        return psm;
    }

    public AsyncResource<L2capChannel> accept() {
        final AsyncResource<L2capChannel> out =
                new AsyncResource<L2capChannel>();
        stack.acceptAppL2cap(psm,
                new SimulatedBluetoothStack.Callback<SimStreamChannel>() {
                    public void onSuccess(SimStreamChannel value) {
                        if (!out.isDone()) {
                            out.complete(new SimL2capChannel(psm, value));
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
        stack.closeAppL2capServer(psm);
    }
}
