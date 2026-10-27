/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.transpiler;

import com.codename1.python.runtime.PyFileSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// The host file system under one root directory, for programs that
/// `open()` files in the conformance run. Loaded in each program's own
/// class loader, so it is public with a String constructor.
public final class JvmFileSystem extends PyFileSystem {
    private final File root;

    public JvmFileSystem(String root) {
        this.root = new File(root);
    }

    private File f(String path) {
        return new File(root, path);
    }

    @Override
    public String home() {
        return "/home";
    }

    @Override
    public InputStream openInput(String path) throws IOException {
        return new FileInputStream(f(path));
    }

    @Override
    public OutputStream openOutput(String path, boolean append) throws IOException {
        return new FileOutputStream(f(path), append);
    }

    @Override
    public boolean exists(String path) {
        return f(path).exists();
    }

    @Override
    public boolean isDirectory(String path) {
        return f(path).isDirectory();
    }

    @Override
    public String[] list(String path) throws IOException {
        String[] r = f(path).list();
        if (r == null) {
            throw new IOException("not a directory");
        }
        java.util.Arrays.sort(r);
        return r;
    }

    @Override
    public void mkdir(String path) throws IOException {
        if (!f(path).mkdir()) {
            throw new IOException("mkdir failed");
        }
    }

    @Override
    public void delete(String path) throws IOException {
        if (!f(path).delete()) {
            throw new IOException("delete failed");
        }
    }

    @Override
    public void rename(String from, String to) throws IOException {
        File t = f(to);
        if (t.exists() && !t.isDirectory() && !t.delete()) {
            throw new IOException("rename failed");
        }
        if (!f(from).renameTo(t)) {
            throw new IOException("rename failed");
        }
    }

    @Override
    public long size(String path) {
        return f(path).length();
    }

    @Override
    public long lastModified(String path) {
        return f(path).lastModified();
    }
}
