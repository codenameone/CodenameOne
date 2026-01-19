/*
 * Copyright (c) 2024, Codename One and/or its affiliates. All rights reserved.
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

package java.io;

public class FileOutputStream extends OutputStream {
    private long handle;
    private boolean closed;

    public FileOutputStream(String name) throws IOException {
        this(name, false);
    }

    public FileOutputStream(String name, boolean append) throws IOException {
        this(name == null ? null : new File(name), append);
    }

    public FileOutputStream(File file) throws IOException {
        this(file, false);
    }

    public FileOutputStream(File file, boolean append) throws IOException {
        if (file == null) {
            throw new NullPointerException();
        }
        this.handle = openImpl(file.getPath(), append);
        if (this.handle == 0) {
            throw new IOException("Unable to open file for writing: " + file.getPath());
        }
    }

    @Override
    public void write(int b) throws IOException {
        byte[] single = new byte[] {(byte)b};
        write(single, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return;
        }
        writeImpl(handle, b, off, len);
    }

    @Override
    public void flush() throws IOException {
        ensureOpen();
        flushImpl(handle);
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            closeImpl(handle);
            handle = 0;
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }

    private static native long openImpl(String path, boolean append) throws IOException;
    private static native void writeImpl(long handle, byte[] b, int off, int len) throws IOException;
    private static native void flushImpl(long handle) throws IOException;
    private static native void closeImpl(long handle) throws IOException;
}
