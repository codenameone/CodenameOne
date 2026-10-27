/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.hardware;

/// A CAN network interface, opened through [Hardware#openCan(String)].
@com.codename1.impl.SharedWithBackend
public final class CanBus {

    private final Hardware hw;
    private final int handle;
    private final String iface;
    private boolean closed;

    CanBus(Hardware hw, int handle, String iface) {
        this.hw = hw;
        this.handle = handle;
        this.iface = iface;
    }

    /// The interface name.
    public String getInterface() {
        return iface;
    }

    /// Sends one frame.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the send fails or the bus is closed
    public void send(CanFrame frame) throws HardwareException {
        checkOpen();
        hw.canSend(handle, frame.getId(), frame.isExtended(), frame.getData(), frame.getLength());
    }

    /// Waits up to `timeoutMs` for a frame (negative waits indefinitely) and
    /// returns it, or `null` on timeout. Call it off the event dispatch thread.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the receive fails or the bus is closed
    public CanFrame receive(int timeoutMs) throws HardwareException {
        checkOpen();
        int[] id = new int[2];
        byte[] data = new byte[8];
        int len = hw.canReceive(handle, id, data, timeoutMs);
        if (len < 0) {
            return null;
        }
        byte[] payload = new byte[len];
        System.arraycopy(data, 0, payload, 0, len);
        return new CanFrame(id[0], id[1] != 0, payload);
    }

    /// Closes the interface. Closing twice is harmless.
    public void close() {
        if (!closed) {
            closed = true;
            hw.close(handle, false);
        }
    }

    private void checkOpen() throws HardwareException {
        if (closed) {
            throw new HardwareException("CAN interface " + iface + " is closed");
        }
    }
}
