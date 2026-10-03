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
package com.codename1.androidcompat.jdk;

import com.codename1.io.FileSystemStorage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/// `java.io.FileInputStream` for the Codename One runtime, reading through
/// `FileSystemStorage`.
public class FileInputStream extends InputStream {

    private final InputStream in;

    public FileInputStream(String name) throws FileNotFoundException {
        this(new File(name));
    }

    public FileInputStream(File file) throws FileNotFoundException {
        if (!file.isFile()) {
            throw new FileNotFoundException(file.getPath() + " (No such file or directory)");
        }
        try {
            in = FileSystemStorage.getInstance().openInputStream(file.storagePath());
        } catch (IOException e) {
            throw new FileNotFoundException(file.getPath() + " (" + e.getMessage() + ")");
        }
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
