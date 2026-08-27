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
 * Writes bytes to a file. Backed by C stdio through a native handle rather than by
 * any Codename One implementation, so it is available to a translated program that
 * has no platform layer at all - a server-side binary, for example.
 */
public class FileOutputStream extends OutputStream {
    private long handle;
    private boolean closed;

    public FileOutputStream(String name) throws FileNotFoundException {
        this(name, false);
    }

    public FileOutputStream(String name, boolean append) throws FileNotFoundException {
        if(name == null) {
            throw new NullPointerException();
        }
        handle = openImpl(name, append);
        if(handle == 0) {
            throw new FileNotFoundException(name);
        }
    }

    public FileOutputStream(File file) throws FileNotFoundException {
        this(file == null ? null : file.getPath(), false);
    }

    public FileOutputStream(File file, boolean append) throws FileNotFoundException {
        this(file == null ? null : file.getPath(), append);
    }

    public void write(int b) throws IOException {
        byte[] one = new byte[1];
        one[0] = (byte)b;
        write(one, 0, 1);
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b == null ? 0 : b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if(b == null) {
            throw new NullPointerException();
        }
        if(off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        checkOpen();
        if(len == 0) {
            return;
        }
        // A short write is a failure, not a partial success: OutputStream.write has
        // no way to report how much it managed, so the caller would silently lose
        // the tail.
        if(writeImpl(handle, b, off, len) != len) {
            throw new IOException("Write failed");
        }
    }

    public void flush() throws IOException {
        checkOpen();
        if(flushImpl(handle) != 0) {
            throw new IOException("Flush failed");
        }
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

    private static native long openImpl(String name, boolean append);
    private static native int writeImpl(long handle, byte[] buffer, int offset, int length);
    private static native int flushImpl(long handle);
    private static native int closeImpl(long handle);
}
