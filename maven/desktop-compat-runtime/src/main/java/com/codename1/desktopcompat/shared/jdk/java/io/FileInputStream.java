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
package com.codename1.desktopcompat.shared.jdk.java.io;

import java.io.*;


/// Reads the bytes of a file, from the platform's file system through
/// `FileSystemStorage`.
///
/// Like most streams, it is for one thread at a time; closing it twice, or
/// from another thread, is safe.
public class FileInputStream extends InputStream {

    private final InputStream in;
    private boolean closed;

    /// Opens the file at `name`.
    ///
    /// #### Throws
    ///
    /// - `FileNotFoundException`: when it is not there, is a directory or
    ///   cannot be read
    public FileInputStream(String name) throws FileNotFoundException {
        this(new File(name));
    }

    /// Opens `file`.
    ///
    /// #### Throws
    ///
    /// - `FileNotFoundException`: when it is not there, is a directory or
    ///   cannot be read
    public FileInputStream(File file) throws FileNotFoundException {
        if (file == null) {
            throw new NullPointerException();
        }
        try {
            in = Host.fileRead(file.hostPath());
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new FileNotFoundException(file.getPath() + " (" + e.getMessage() + ")");
        }
        if (in == null) {
            throw new FileNotFoundException(file.getPath());
        }
    }

    private void checkOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }

    @Override
    public int read() throws IOException {
        checkOpen();
        return in.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        checkOpen();
        if (len == 0) {
            return 0;
        }
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        checkOpen();
        return n <= 0 ? 0 : in.skip(n);
    }

    @Override
    public int available() throws IOException {
        checkOpen();
        return in.available();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        in.close();
    }
}
