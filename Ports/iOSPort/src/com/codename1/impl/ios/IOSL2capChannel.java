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

import com.codename1.bluetooth.le.L2capChannel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An open CBL2CAPChannel. The native side wraps the channel's
 * NSInput/NSOutputStream pair behind an opaque long handle; the blocking
 * btL2capRead / btL2capWrite natives poll those streams off the Bluetooth
 * dispatch queue, so the streams here must be consumed off the EDT (as the
 * core API requires).
 *
 * <p>Native return convention: byte count on success, -1 on orderly end of
 * stream, -2 on error / closed handle.</p>
 */
class IOSL2capChannel extends L2capChannel {

    private final IOSBluetooth bt;
    private final long handle;
    private volatile boolean open = true;

    private final InputStream in = new InputStream() {
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n < 0 ? -1 : one[0] & 0xFF;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new IOException("null buffer");
            }
            if (len == 0) {
                return 0;
            }
            if (off < 0 || len < 0 || off + len > b.length) {
                throw new IOException("invalid offset/length");
            }
            if (!open) {
                return -1;
            }
            int n = bt.nativeInstance.btL2capRead(handle, b, off, len);
            if (n == -2) {
                throw new IOException("L2CAP read failed");
            }
            return n;
        }
    };

    private final OutputStream out = new OutputStream() {
        public void write(int b) throws IOException {
            write(new byte[] {(byte) b}, 0, 1);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new IOException("null buffer");
            }
            if (off < 0 || len < 0 || off + len > b.length) {
                throw new IOException("invalid offset/length");
            }
            while (len > 0) {
                if (!open) {
                    throw new IOException("L2CAP channel is closed");
                }
                int n = bt.nativeInstance.btL2capWrite(handle, b, off, len);
                if (n < 0) {
                    throw new IOException("L2CAP write failed");
                }
                off += n;
                len -= n;
            }
        }
    };

    IOSL2capChannel(IOSBluetooth bt, int psm, long handle) {
        super(psm);
        this.bt = bt;
        this.handle = handle;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return in;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return out;
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            if (!open) {
                return;
            }
            open = false;
        }
        bt.nativeInstance.btL2capClose(handle);
    }

    @Override
    public boolean isOpen() {
        return open;
    }
}
