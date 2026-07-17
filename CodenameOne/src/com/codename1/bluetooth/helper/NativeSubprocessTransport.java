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
package com.codename1.bluetooth.helper;

import java.io.IOException;
import java.util.List;

/// A [HelperTransport] over a native child process, for the ports that have
/// no `java.lang.ProcessBuilder` (the native Win32 and Linux desktop ports).
/// This class owns the line framing -- assembling `readLine()` from raw byte
/// reads and UTF-8-encoding `writeLine()` with a trailing newline -- over a
/// handful of raw process primitives each port implements with its own
/// `posix_spawn`/`CreateProcess` native bridge. The subclass supplies only
/// those raw calls; all protocol/threading logic lives above this in
/// [HelperBleBackend].
public abstract class NativeSubprocessTransport implements HelperTransport {

    private final List<String> configuredCommand;
    private long handle;
    private final byte[] readChunk = new byte[4096];
    private byte[] lineBuffer = new byte[256];
    private int lineLen;
    private final byte[] writeNewline = new byte[] {(byte) '\n'};

    /// @param configuredCommand the helper launch command this transport
    ///                          runs when [#start(List)] is passed `null`
    ///                          (the resolved helper path + args)
    protected NativeSubprocessTransport(List<String> configuredCommand) {
        this.configuredCommand = configuredCommand;
    }

    // ------------------------------------------------------------------
    // raw process primitives -- implemented per port over its Native class
    // ------------------------------------------------------------------

    /// Spawns `argv[0]` with `argv` as arguments and its stdin/stdout wired
    /// to pipes; returns an opaque handle, or `0` on failure.
    protected abstract long rawSpawn(String[] argv);

    /// Blocking read of up to `len` bytes from the child's stdout into `buf`
    /// at `off`; returns bytes read, `0` on EOF, `-1` on error.
    protected abstract int rawRead(long handle, byte[] buf, int off, int len);

    /// Writes `len` bytes from `buf` at `off` to the child's stdin; returns
    /// bytes written or `-1`.
    protected abstract int rawWrite(long handle, byte[] buf, int off, int len);

    /// Closes the child's stdin (EOF) without terminating it.
    protected abstract void rawCloseStdin(long handle);

    /// Terminates the child, closes pipes and frees the handle.
    protected abstract void rawClose(long handle);

    /// `1` when the child is still running, `0` when it has exited.
    protected abstract int rawIsAlive(long handle);

    // ------------------------------------------------------------------
    // HelperTransport
    // ------------------------------------------------------------------

    @Override
    public void start(List<String> command) throws IOException {
        if (handle != 0) {
            throw new IOException("transport already started");
        }
        List<String> launch = command != null ? command : configuredCommand;
        if (launch == null || launch.isEmpty()) {
            throw new IOException("no cn1-ble-helper launch command");
        }
        String[] argv = new String[launch.size()];
        for (int i = 0; i < argv.length; i++) {
            argv[i] = launch.get(i);
        }
        long h = rawSpawn(argv);
        if (h == 0) {
            throw new IOException("failed to spawn helper: " + argv[0]);
        }
        handle = h;
    }

    @Override
    public String readLine() throws IOException {
        long h = handle;
        if (h == 0) {
            return null;
        }
        // drain any complete line already buffered, otherwise read more
        while (true) {
            int nl = indexOfNewline();
            if (nl >= 0) {
                String line = utf8(lineBuffer, 0, nl);
                int rest = lineLen - (nl + 1);
                if (rest > 0) {
                    System.arraycopy(lineBuffer, nl + 1, lineBuffer, 0, rest);
                }
                lineLen = rest;
                return line;
            }
            int n = rawRead(h, readChunk, 0, readChunk.length);
            if (n < 0) {
                throw new IOException("helper read error");
            }
            if (n == 0) {
                // EOF: emit any trailing partial line, then signal end
                if (lineLen > 0) {
                    String line = utf8(lineBuffer, 0, lineLen);
                    lineLen = 0;
                    return line;
                }
                return null;
            }
            appendToLine(readChunk, n);
        }
    }

    @Override
    public void writeLine(String line) throws IOException {
        long h = handle;
        if (h == 0) {
            throw new IOException("transport not started");
        }
        byte[] data = utf8Bytes(line);
        writeFully(h, data, data.length);
        writeFully(h, writeNewline, 1);
    }

    @Override
    public void close() {
        long h = handle;
        handle = 0;
        if (h != 0) {
            rawClose(h);
        }
    }

    @Override
    public boolean isAlive() {
        long h = handle;
        return h != 0 && rawIsAlive(h) != 0;
    }

    // ------------------------------------------------------------------
    // internals
    // ------------------------------------------------------------------

    private void writeFully(long h, byte[] data, int len) throws IOException {
        int off = 0;
        while (off < len) {
            int w = rawWrite(h, data, off, len - off);
            if (w < 0) {
                throw new IOException("helper write error");
            }
            if (w == 0) {
                throw new IOException("helper stdin closed");
            }
            off += w;
        }
    }

    private int indexOfNewline() {
        for (int i = 0; i < lineLen; i++) {
            if (lineBuffer[i] == (byte) '\n') {
                return i;
            }
        }
        return -1;
    }

    private void appendToLine(byte[] src, int n) {
        if (lineLen + n > lineBuffer.length) {
            int cap = lineBuffer.length * 2;
            while (cap < lineLen + n) {
                cap *= 2;
            }
            byte[] grown = new byte[cap];
            System.arraycopy(lineBuffer, 0, grown, 0, lineLen);
            lineBuffer = grown;
        }
        System.arraycopy(src, 0, lineBuffer, lineLen, n);
        lineLen += n;
    }

    private static String utf8(byte[] b, int off, int len) {
        // trim a trailing CR so CRLF frames decode cleanly
        int end = len;
        if (end > 0 && b[off + end - 1] == (byte) '\r') {
            end--;
        }
        try {
            return new String(b, off, end, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is unavailable", e);
        }
    }

    private static byte[] utf8Bytes(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is unavailable", e);
        }
    }
}
