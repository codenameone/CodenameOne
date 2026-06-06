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
import java.io.OutputStream;

/**
 * Output stream backed by a native peer: either a Win32 file handle or the
 * request body of a WinHTTP connection, selected by the {@code http} flag.
 * Writes delegate to {@link WindowsNative}; an HTTP stream does not close its
 * connection (the implementation owns that through {@code cleanup}).
 */
public final class WindowsOutputStream extends OutputStream {
    private final long peer;
    private final boolean http;
    private boolean closed;

    WindowsOutputStream(long peer, boolean http) {
        this.peer = peer;
        this.http = http;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[] { (byte) b }, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed || len == 0) {
            return;
        }
        if (http) {
            WindowsNative.httpWriteBody(peer, b, off, len);
        } else {
            WindowsNative.fileWrite(peer, b, off, len);
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (!http) {
            WindowsNative.fileClose(peer);
        }
    }
}
