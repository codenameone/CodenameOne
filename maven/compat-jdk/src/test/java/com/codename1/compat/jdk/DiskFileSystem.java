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
package com.codename1.compat.jdk;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.io.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// A Codename One file system with directories, lengths and time stamps, for
/// the tests that need one: every `file:///...` path is a file under a
/// directory on this machine's disk.
///
/// The headless implementation's own file system is a map of the files
/// written, which is enough to see what path was addressed and not enough to
/// list a directory.
final class DiskFileSystem extends HeadlessImplementation {

    private final java.io.File root;

    private DiskFileSystem(java.io.File root) {
        this.root = root;
    }

    /// Starts Codename One and points its file system at `root`.
    static void install(java.io.File root) {
        HeadlessImplementation.install();
        new java.io.File(root, "home").mkdirs();
        Util.setImplementation(new DiskFileSystem(root));
    }

    private java.io.File disk(Object path) {
        String p = String.valueOf(path);
        if (p.startsWith("file:")) {
            p = p.substring(5);
        }
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return new java.io.File(root, p);
    }

    @Override
    public OutputStream openOutputStream(Object path) throws IOException {
        return new java.io.FileOutputStream(disk(path));
    }

    @Override
    public OutputStream openOutputStream(Object path, int offset) throws IOException {
        java.io.File f = disk(path);
        if (offset != f.length()) {
            throw new IOException("Only appending is supported, at " + f.length() + " not " + offset);
        }
        return new java.io.FileOutputStream(f, true);
    }

    @Override
    public InputStream openInputStream(Object path) throws IOException {
        return new java.io.FileInputStream(disk(path));
    }

    @Override
    public String[] listFiles(String directory) throws IOException {
        java.io.File[] files = disk(directory).listFiles();
        if (files == null) {
            return new String[0];
        }
        String[] out = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            // A directory is listed with a trailing separator on a device.
            out[i] = files[i].isDirectory() ? files[i].getName() + "/" : files[i].getName();
        }
        return out;
    }

    @Override
    public void mkdir(String directory) {
        disk(directory).mkdir();
    }

    @Override
    public void deleteFile(String file) {
        disk(file).delete();
    }

    @Override
    public boolean isDirectory(String file) {
        return disk(file).isDirectory();
    }

    @Override
    public boolean exists(String file) {
        return disk(file).exists();
    }

    @Override
    public long getFileLength(String file) {
        return disk(file).length();
    }

    @Override
    public long getFileLastModified(String file) {
        return disk(file).lastModified();
    }

    @Override
    public void rename(String file, String newName) {
        java.io.File f = disk(file);
        f.renameTo(new java.io.File(f.getParentFile(), newName));
    }
}
