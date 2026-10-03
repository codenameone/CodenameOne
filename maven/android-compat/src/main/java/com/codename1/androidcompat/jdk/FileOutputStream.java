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
import java.io.OutputStream;

/// `java.io.FileOutputStream` for the Codename One runtime, writing through
/// `FileSystemStorage`. Appending opens the storage stream at the file's
/// current length.
public class FileOutputStream extends OutputStream {

    private final OutputStream out;

    public FileOutputStream(String name) throws FileNotFoundException {
        this(new File(name), false);
    }

    public FileOutputStream(String name, boolean append) throws FileNotFoundException {
        this(new File(name), append);
    }

    public FileOutputStream(File file) throws FileNotFoundException {
        this(file, false);
    }

    public FileOutputStream(File file, boolean append) throws FileNotFoundException {
        if (file.isDirectory()) {
            throw new FileNotFoundException(file.getPath() + " (Is a directory)");
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            throw new FileNotFoundException(file.getPath() + " (No such file or directory)");
        }
        try {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            String p = file.storagePath();
            if (append && fs.exists(p)) {
                out = fs.openOutputStream(p, (int) fs.getLength(p));
            } else {
                out = fs.openOutputStream(p);
            }
        } catch (IOException e) {
            throw new FileNotFoundException(file.getPath() + " (" + e.getMessage() + ")");
        }
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
