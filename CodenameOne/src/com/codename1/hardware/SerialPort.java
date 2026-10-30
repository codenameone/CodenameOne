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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// A serial port in raw mode, opened through
/// [Hardware#openSerial(String, int, int, int, int)].
///
/// Reads block; call them off the event dispatch thread, or use
/// [#read(byte[], int, int, int)] with a timeout.
@com.codename1.impl.SharedWithBackend
public final class SerialPort {

    /// No parity bit.
    public static final int PARITY_NONE = 0;
    /// Odd parity.
    public static final int PARITY_ODD = 1;
    /// Even parity.
    public static final int PARITY_EVEN = 2;

    private final Hardware hw;
    private final int handle;
    private final String device;
    private boolean closed;
    private InputStream in;
    private OutputStream out;

    SerialPort(Hardware hw, int handle, String device) {
        this.hw = hw;
        this.handle = handle;
        this.device = device;
    }

    /// The device path this port was opened on.
    public String getDevice() {
        return device;
    }

    /// Reads up to `len` bytes, waiting at most `timeoutMs` for the first
    /// (negative waits indefinitely). Returns the number read, `0` on timeout.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the read fails or the port is closed
    public int read(byte[] buf, int off, int len, int timeoutMs) throws HardwareException {
        checkOpen();
        return hw.serialRead(handle, buf, off, len, timeoutMs);
    }

    /// Writes all of `buf[off..off+len)`.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the write fails or the port is closed
    public void write(byte[] buf, int off, int len) throws HardwareException {
        checkOpen();
        hw.serialWrite(handle, buf, off, len);
    }

    /// A blocking stream over this port. Its `read` waits until data arrives
    /// and returns `-1` only after [#close()].
    public InputStream getInputStream() {
        if (in == null) {
            in = new InputStream() {
                @Override
                public int read() throws IOException {
                    byte[] one = new byte[1];
                    int n = read(one, 0, 1);
                    return n <= 0 ? -1 : one[0] & 0xff;
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    if (len == 0) {
                        return 0;
                    }
                    while (!closed) {
                        int n = hw.serialRead(handle, b, off, len, 250);
                        if (n > 0) {
                            return n;
                        }
                    }
                    return -1;
                }
            };
        }
        return in;
    }

    /// A stream that writes to this port.
    public OutputStream getOutputStream() {
        if (out == null) {
            out = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    write(new byte[] {(byte) b}, 0, 1);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    SerialPort.this.write(b, off, len);
                }
            };
        }
        return out;
    }

    /// Closes the port. Closing twice is harmless.
    public void close() {
        if (!closed) {
            closed = true;
            hw.close(handle, false);
        }
    }

    private void checkOpen() throws HardwareException {
        if (closed) {
            throw new HardwareException("Serial port " + device + " is closed");
        }
    }
}
