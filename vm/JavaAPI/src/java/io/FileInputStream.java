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

public class FileInputStream extends InputStream {
    private long handle;
    private boolean closed;

    public FileInputStream(String name) throws IOException {
        this(name == null ? null : new File(name));
    }

    public FileInputStream(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException();
        }
        this.handle = openImpl(file.getPath());
        if (this.handle == 0) {
            throw new IOException("Unable to open file for reading: " + file.getPath());
        }
    }

    @Override
    public int read() throws IOException {
        byte[] buffer = new byte[1];
        int count = read(buffer, 0, 1);
        if (count <= 0) {
            return -1;
        }
        return buffer[0] & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        return readImpl(handle, b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        ensureOpen();
        if (n <= 0) {
            return 0;
        }
        return skipImpl(handle, n);
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        return availableImpl(handle);
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            closeImpl(handle);
            handle = 0;
        }
    }

    @Override
    public synchronized void mark(int readlimit) {
        // mark/reset not supported
    }

    @Override
    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }

    private static native long openImpl(String path) throws IOException;
    private static native int readImpl(long handle, byte[] b, int off, int len) throws IOException;
    private static native long skipImpl(long handle, long n) throws IOException;
    private static native int availableImpl(long handle) throws IOException;
    private static native void closeImpl(long handle) throws IOException;
}
