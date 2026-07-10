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

    /**
     * A single read buffer, reused across every blocking read on this socket and held as a
     * DIRECT GC root (see {@link #PINNED_READ_BUFFERS}) for as long as the socket is open.
     *
     * <p>The concurrent conservative collector can otherwise sweep a freshly-allocated read
     * array while this thread is parked inside the native blocking {@code recv()}: platforms
     * without signal-stop (Windows) rely purely on the cooperative stack scan and can miss the
     * parked reader, and marking the buffer indirectly through the impl-&gt;stream-&gt;buffer
     * chain depends on mark-completeness that has proven fragile under load. A stable,
     * directly-rooted buffer removes both the per-read allocation window and the chain
     * dependency, so it can never be reclaimed mid-read regardless of scan timing.
     */
    private byte[] readBuffer;

    /** Directly-rooted set of live per-socket read buffers; scanned by markStatics. */
    private static final java.util.Set<byte[]> PINNED_READ_BUFFERS =
            java.util.Collections.synchronizedSet(new java.util.HashSet<byte[]>());

    /** Lazily allocates and pins this socket's reusable read buffer. */
    private byte[] readBuffer() {
        byte[] b = readBuffer;
        if (b == null) {
            b = new byte[8192];
            PINNED_READ_BUFFERS.add(b);
            readBuffer = b;
        }
        return b;
    }

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
        // Read into the pinned, reusable buffer so the array can never be swept while this
        // thread is parked in the native blocking recv, then copy out a right-sized result
        // (allocated after recv returns, so it is never held across a blocking call).
        byte[] scratch = readBuffer();
        int read = WindowsNative.socketRead(handle, scratch, 0, scratch.length);
        if (read < 0) {
            return null;
        }
        byte[] result = new byte[read];
        System.arraycopy(scratch, 0, result, 0, read);
        return result;
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
        byte[] b = readBuffer;
        if (b != null) {
            PINNED_READ_BUFFERS.remove(b);
            readBuffer = null;
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
            // Reuse the socket's pinned read buffer so it can never be swept while this thread
            // is parked in the native blocking recv (see WindowsSocket.readBuffer).
            byte[] chunk = readBuffer();
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
