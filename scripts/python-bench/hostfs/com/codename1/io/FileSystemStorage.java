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
package com.codename1.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// The benchmark's stand-in for the core class: the members the Python
/// runtime's `Cn1FileSystem` calls, over the host's files. A native
/// benchmark executable is the runtime without the core, whose real
/// `FileSystemStorage` would bring the whole UI and its natives with it.
public final class FileSystemStorage {
    private static final FileSystemStorage INSTANCE = new FileSystemStorage();
    private static final String SCHEME = "file://";

    private FileSystemStorage() {
    }

    public static FileSystemStorage getInstance() {
        return INSTANCE;
    }

    private static File file(String url) {
        return new File(url.startsWith(SCHEME) ? url.substring(SCHEME.length()) : url);
    }

    /// The directory the executable was started in.
    public String getAppHomePath() {
        String p = new File(".").getAbsolutePath();
        if (p.endsWith("/.")) {
            p = p.substring(0, p.length() - 2);
        }
        return SCHEME + p + "/";
    }

    public InputStream openInputStream(String url) throws IOException {
        return new FileInputStream(file(url));
    }

    public OutputStream openOutputStream(String url) throws IOException {
        return new FileOutputStream(file(url));
    }

    /// Appends when `offset` is the file's length, the only way the
    /// runtime calls it.
    public OutputStream openOutputStream(String url, int offset) throws IOException {
        return new FileOutputStream(file(url), offset > 0);
    }

    public boolean exists(String url) {
        return file(url).exists();
    }

    public boolean isDirectory(String url) {
        return file(url).isDirectory();
    }

    public String[] listFiles(String url) throws IOException {
        String[] r = file(url).list();
        if (r == null) {
            throw new IOException("not a directory: " + url);
        }
        return r;
    }

    public void mkdir(String url) {
        file(url).mkdir();
    }

    public void delete(String url) {
        file(url).delete();
    }

    /// Renames within the directory, as the core class does.
    public void rename(String url, String newName) {
        File f = file(url);
        f.renameTo(new File(f.getParent(), newName));
    }

    public long getLength(String url) {
        return file(url).length();
    }

    public long getLastModified(String url) {
        return file(url).lastModified();
    }
}
