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
package java.io;

/// Writes bytes to a file through C stdio and a native handle rather than
/// through any Codename One implementation, so it serves a translated program
/// that has no platform layer at all -- a server-side binary, for example.
/// Only [PosixFileAccess] creates one.
final class NativeFileOutputStream extends OutputStream {
    private long handle;
    /* volatile + synchronized close: two threads closing concurrently could both
       read closed == false and hand the SAME FILE* to fclose twice, which is
       undefined and crashes the translated process rather than merely erroring.
       Idempotent now.

       NOT a claim of thread safety. A write racing a close on the same stream can
       still reach the native call with a handle this method is closing -- the JDK
       buys that with a lock on every operation, and these streams are not worth
       that cost. Like most java.io streams they are for one thread at a time; what
       is guaranteed here is that closing twice, or closing from another thread, is
       safe rather than fatal. */
    private volatile boolean closed;

    NativeFileOutputStream(String name, boolean append) throws FileNotFoundException {
        if (name == null) {
            throw new NullPointerException();
        }
        handle = FileOutputStream.openImpl(name, append);
        if (handle == 0) {
            throw new FileNotFoundException(name);
        }
    }

    @Override
    public void write(int b) throws IOException {
        byte[] one = new byte[1];
        one[0] = (byte) b;
        write(one, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        checkOpen();
        if (len == 0) {
            return;
        }
        // A short write is a failure, not a partial success: OutputStream.write has
        // no way to report how much it managed, so the caller would silently lose
        // the tail.
        if (FileOutputStream.writeImpl(handle, b, off, len) != len) {
            throw new IOException("Write failed");
        }
    }

    @Override
    public void flush() throws IOException {
        checkOpen();
        if (FileOutputStream.flushImpl(handle) != 0) {
            throw new IOException("Flush failed");
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        long h = handle;
        handle = 0;
        if (FileOutputStream.closeImpl(h) != 0) {
            throw new IOException("Close failed");
        }
    }

    /// Releases the native `FILE*` if the caller never closed the stream, which
    /// also has the C runtime write what is still buffered.
    ///
    /// Deliberately silent: a finalizer has nobody to report to, and throwing
    /// from one is worse than the leak it is cleaning up.
    @Override
    protected void finalize() {
        // Same lock as close(): a finalizer running while another thread closes
        // would otherwise be the two-fclose case this synchronization exists for.
        synchronized (this) {
            if (!closed && handle != 0) {
                closed = true;
                long h = handle;
                handle = 0;
                FileOutputStream.closeImpl(h);
            }
        }
    }

    private void checkOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }
}
