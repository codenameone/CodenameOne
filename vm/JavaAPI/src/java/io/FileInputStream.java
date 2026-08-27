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
package java.io;

/**
 * Reads bytes from a file. Backed by C stdio through a native handle rather than
 * by any Codename One implementation, so it is available to a translated program
 * that has no platform layer at all - a server-side binary, for example.
 */
public class FileInputStream extends InputStream {
    private long handle;
    private boolean closed;

    public FileInputStream(String name) throws FileNotFoundException {
        if(name == null) {
            throw new NullPointerException();
        }
        handle = openImpl(name);
        if(handle == 0) {
            throw new FileNotFoundException(name);
        }
    }

    public FileInputStream(File file) throws FileNotFoundException {
        this(file == null ? null : file.getPath());
    }

    public int read() throws IOException {
        byte[] one = new byte[1];
        int n = read(one, 0, 1);
        if(n <= 0) {
            return -1;
        }
        return one[0] & 0xff;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b == null ? 0 : b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if(b == null) {
            throw new NullPointerException();
        }
        if(off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        checkOpen();
        if(len == 0) {
            return 0;
        }
        int n = readImpl(handle, b, off, len);
        if(n < -1) {
            throw new IOException("Read failed");
        }
        return n;
    }

    public long skip(long n) throws IOException {
        checkOpen();
        if(n <= 0) {
            return 0;
        }
        long moved = skipImpl(handle, n);
        if(moved < 0) {
            throw new IOException("Seek failed");
        }
        return moved;
    }

    public int available() throws IOException {
        checkOpen();
        int a = availableImpl(handle);
        if(a < 0) {
            throw new IOException("Unable to determine available bytes");
        }
        return a;
    }

    public void close() throws IOException {
        if(closed) {
            return;
        }
        closed = true;
        long h = handle;
        handle = 0;
        if(closeImpl(h) != 0) {
            throw new IOException("Close failed");
        }
    }

    private void checkOpen() throws IOException {
        if(closed) {
            throw new IOException("Stream closed");
        }
    }

    private static native long openImpl(String name);
    private static native int readImpl(long handle, byte[] buffer, int offset, int length);
    private static native long skipImpl(long handle, long count);
    private static native int availableImpl(long handle);
    private static native int closeImpl(long handle);
}
