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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// An open L2CAP connection-oriented channel -- a raw bidirectional byte
/// stream to a peripheral, obtained via
/// [BlePeripheral#openL2capChannel(int, boolean)] on the central side or
/// accepted from an [L2capServer] on the peripheral side.
///
/// The streams **block** and must be consumed off the EDT; reads/writes
/// throw plain `java.io.IOException` on transport failure. Always
/// [#close()] the channel when done.
public abstract class L2capChannel {

    private final int psm;

    /// Constructed by ports; not application API.
    protected L2capChannel(int psm) {
        this.psm = psm;
    }

    /// The blocking input stream of the channel; consume off the EDT.
    public abstract InputStream getInputStream() throws IOException;

    /// The blocking output stream of the channel; use off the EDT.
    public abstract OutputStream getOutputStream() throws IOException;

    /// Closes the channel and both streams.
    public abstract void close() throws IOException;

    /// `true` while the channel is open.
    public abstract boolean isOpen();

    /// The Protocol/Service Multiplexer this channel is bound to.
    public int getPsm() {
        return psm;
    }
}
