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

import com.codename1.impl.JdkHost;

/// Writes the bytes of a file, to the platform's file system through
/// `com.codename1.impl.JdkHost`. The file is created when it is not there.
///
/// Like most streams, it is for one thread at a time; closing it twice, or
/// from another thread, is safe.
public class FileOutputStream extends OutputStream {

    private final OutputStream out;
    private boolean closed;

    /// Opens the file at `name`, emptying it.
    ///
    /// #### Throws
    ///
    /// - `FileNotFoundException`: when it is a directory or cannot be written
    public FileOutputStream(String name) throws FileNotFoundException {
        this(new File(name), false);
    }

    /// Opens the file at `name`, at its end when `append` is true and emptied
    /// otherwise.
    ///
    /// #### Throws
    ///
    /// - `FileNotFoundException`: when it is a directory or cannot be written
    public FileOutputStream(String name, boolean append) throws FileNotFoundException {
        this(new File(name), append);
    }

    /// Opens `file`, emptying it.
    ///
    /// #### Throws
    ///
    /// - `FileNotFoundException`: when it is a directory or cannot be written
    public FileOutputStream(File file) throws FileNotFoundException {
        this(file, false);
    }

    /// Opens `file`, at its end when `append` is true and emptied otherwise.
    ///
    /// #### Throws
    ///
    /// - `FileNotFoundException`: when it is a directory or cannot be written
    public FileOutputStream(File file, boolean append) throws FileNotFoundException {
        if (file == null) {
            throw new NullPointerException();
        }
        try {
            out = JdkHost.get().fileWrite(file.hostPath(), append);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new FileNotFoundException(file.getPath() + " (" + e.getMessage() + ")");
        }
        if (out == null) {
            throw new FileNotFoundException(file.getPath());
        }
    }

    private void checkOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }

    @Override
    public void write(int b) throws IOException {
        checkOpen();
        out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
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
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        checkOpen();
        out.flush();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        out.close();
    }
}
