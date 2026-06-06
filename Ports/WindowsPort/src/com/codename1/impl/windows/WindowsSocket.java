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
package com.codename1.impl.windows;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Thin Java wrapper over a native WinSock TCP socket peer. Holds the native
 * handle and exposes both the raw read/write the Codename One socket SPI needs
 * and blocking {@link InputStream}/{@link OutputStream} adapters the port's
 * WebSocket client runs over.
 */
public final class WindowsSocket {
    private long handle;

    /** Connects to host:port; {@link #isConnected()} reports the outcome. */
    public WindowsSocket(String host, int port, int timeoutMillis) {
        handle = WindowsNative.socketConnect(host, port, timeoutMillis);
    }

    public boolean isConnected() {
        return handle != 0 && WindowsNative.socketConnected(handle);
    }

    public int available() {
        return handle == 0 ? 0 : WindowsNative.socketAvailable(handle);
    }

    /** Reads a chunk (blocking); returns null on EOF/error. Used by the socket SPI. */
    public byte[] readChunk() {
        if (handle == 0) {
            return null;
        }
        int avail = WindowsNative.socketAvailable(handle);
        byte[] buffer = new byte[avail > 0 ? avail : 8192];
        int read = WindowsNative.socketRead(handle, buffer, 0, buffer.length);
        if (read < 0) {
            return null;
        }
        if (read == buffer.length) {
            return buffer;
        }
        byte[] trimmed = new byte[read];
        System.arraycopy(buffer, 0, trimmed, 0, read);
        return trimmed;
    }

    public void write(byte[] data, int offset, int length) {
        if (handle != 0) {
            WindowsNative.socketWrite(handle, data, offset, length);
        }
    }

    public int getErrorCode() {
        return handle == 0 ? -1 : WindowsNative.socketErrorCode(handle);
    }

    public String getErrorMessage() {
        int code = getErrorCode();
        return code <= 0 ? null : "WinSock error " + code;
    }

    public void close() {
        if (handle != 0) {
            WindowsNative.socketClose(handle);
            handle = 0;
        }
    }

    public InputStream getInputStream() {
        return new SocketInput();
    }

    public OutputStream getOutputStream() {
        return new SocketOutput();
    }

    /** Blocking input stream backed by socketRead, with a small carry buffer. */
    private final class SocketInput extends InputStream {
        private byte[] buffer;
        private int pos;
        private int len;

        @Override
        public int read() throws IOException {
            if (!fill()) {
                return -1;
            }
            return buffer[pos++] & 0xff;
        }

        @Override
        public int read(byte[] b, int off, int length) throws IOException {
            if (length == 0) {
                return 0;
            }
            if (!fill()) {
                return -1;
            }
            int n = Math.min(length, len - pos);
            System.arraycopy(buffer, pos, b, off, n);
            pos += n;
            return n;
        }

        private boolean fill() throws IOException {
            if (buffer != null && pos < len) {
                return true;
            }
            if (handle == 0) {
                return false;
            }
            byte[] chunk = new byte[8192];
            int n = WindowsNative.socketRead(handle, chunk, 0, chunk.length);
            if (n < 0) {
                return false;
            }
            if (n == 0) {
                // Avoid a busy spin if the native ever returns 0 without EOF.
                return fill();
            }
            buffer = chunk;
            pos = 0;
            len = n;
            return true;
        }
    }

    private final class SocketOutput extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            write(new byte[] { (byte) b }, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int length) throws IOException {
            if (handle == 0) {
                throw new IOException("socket closed");
            }
            if (WindowsNative.socketWrite(handle, b, off, length) < 0) {
                throw new IOException("socket write failed: " + getErrorMessage());
            }
        }
    }
}
