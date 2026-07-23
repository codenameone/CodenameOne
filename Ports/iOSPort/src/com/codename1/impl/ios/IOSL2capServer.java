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
package com.codename1.impl.ios;

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.le.L2capChannel;
import com.codename1.bluetooth.le.L2capServer;
import com.codename1.util.AsyncResource;

import java.util.LinkedList;

/**
 * A published CBPeripheralManager L2CAP endpoint. Incoming channels from
 * peripheralManager:didOpenL2CAPChannel: are queued until the app calls
 * {@link #accept()}; pending accepts are resolved in FIFO order.
 */
class IOSL2capServer extends L2capServer {

    private final IOSBluetooth bt;
    private final int psm;
    private final LinkedList<IOSL2capChannel> pendingChannels =
            new LinkedList<IOSL2capChannel>();
    private final LinkedList<AsyncResource<L2capChannel>> pendingAccepts =
            new LinkedList<AsyncResource<L2capChannel>>();
    private boolean closed;

    IOSL2capServer(IOSBluetooth bt, int psm) {
        this.bt = bt;
        this.psm = psm;
    }

    @Override
    public int getPsm() {
        return psm;
    }

    @Override
    public AsyncResource<L2capChannel> accept() {
        AsyncResource<L2capChannel> r = new AsyncResource<L2capChannel>();
        IOSL2capChannel ready = null;
        synchronized (this) {
            if (closed) {
                r.error(new BluetoothException(BluetoothError.IO_ERROR,
                        "L2CAP server is closed"));
                return r;
            }
            if (!pendingChannels.isEmpty()) {
                ready = pendingChannels.removeFirst();
            } else {
                pendingAccepts.add(r);
            }
        }
        if (ready != null) {
            r.complete(ready);
        }
        return r;
    }

    @Override
    public void close() {
        Object[] toFail = null;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            if (!pendingAccepts.isEmpty()) {
                toFail = pendingAccepts.toArray();
                pendingAccepts.clear();
            }
            pendingChannels.clear();
        }
        IOSBluetooth.unregisterL2capServer(psm);
        bt.nativeInstance.btUnpublishL2cap(psm);
        if (toFail != null) {
            BluetoothException ex = new BluetoothException(
                    BluetoothError.IO_ERROR, "L2CAP server closed");
            for (int i = 0; i < toFail.length; i++) {
                AsyncResource<L2capChannel> r =
                        (AsyncResource<L2capChannel>) toFail[i];
                if (!r.isDone()) {
                    r.error(ex);
                }
            }
        }
    }

    /** Incoming channel from the native side; safe to call from any
     * thread. */
    void incomingFromNative(IOSL2capChannel channel) {
        AsyncResource<L2capChannel> waiting = null;
        synchronized (this) {
            if (closed) {
                try {
                    channel.close();
                } catch (java.io.IOException ignore) {
                }
                return;
            }
            if (!pendingAccepts.isEmpty()) {
                waiting = pendingAccepts.removeFirst();
            } else {
                pendingChannels.add(channel);
            }
        }
        if (waiting != null && !waiting.isDone()) {
            waiting.complete(channel);
        }
    }
}
