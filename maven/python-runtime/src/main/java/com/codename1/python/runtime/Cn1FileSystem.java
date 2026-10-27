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
package com.codename1.python.runtime;

import com.codename1.io.FileSystemStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// The file system of a Codename One application: `FileSystemStorage`,
/// whose paths are `file://` URLs.
final class Cn1FileSystem extends PyFileSystem {
    private static final String SCHEME = "file://";

    private static FileSystemStorage fs() {
        return FileSystemStorage.getInstance();
    }

    private static String url(String path) {
        return path.startsWith(SCHEME) ? path : SCHEME + path;
    }

    @Override
    public String home() {
        String h = fs().getAppHomePath();
        if (h.startsWith(SCHEME)) {
            h = h.substring(SCHEME.length());
        }
        while (h.length() > 1 && h.endsWith("/")) {
            h = h.substring(0, h.length() - 1);
        }
        return h;
    }

    @Override
    public InputStream openInput(String path) throws IOException {
        return fs().openInputStream(url(path));
    }

    @Override
    public OutputStream openOutput(String path, boolean append) throws IOException {
        String u = url(path);
        if (append && fs().exists(u)) {
            return fs().openOutputStream(u, (int) fs().getLength(u));
        }
        return fs().openOutputStream(u);
    }

    @Override
    public boolean exists(String path) {
        return fs().exists(url(path));
    }

    @Override
    public boolean isDirectory(String path) {
        return fs().isDirectory(url(path));
    }

    @Override
    public String[] list(String path) throws IOException {
        String[] names = fs().listFiles(url(path));
        String[] r = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            String n = names[i];
            // Some ports mark a directory with a trailing separator.
            r[i] = n.endsWith("/") ? n.substring(0, n.length() - 1) : n;
        }
        return r;
    }

    @Override
    public void mkdir(String path) throws IOException {
        fs().mkdir(url(path));
        if (!fs().exists(url(path))) {
            throw new IOException("mkdir failed");
        }
    }

    @Override
    public void delete(String path) throws IOException {
        fs().delete(url(path));
    }

    @Override
    public void rename(String from, String to) throws IOException {
        int a = from.lastIndexOf('/');
        int b = to.lastIndexOf('/');
        if (a != b || !from.substring(0, a + 1).equals(to.substring(0, b + 1))) {
            // FileSystemStorage renames within a directory only; a move
            // is a copy and a delete.
            InputStream in = openInput(from);
            try {
                OutputStream out = openOutput(to, false);
                try {
                    byte[] buf = new byte[8192];
                    for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                        out.write(buf, 0, n);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
            delete(from);
            return;
        }
        if (fs().exists(url(to))) {
            fs().delete(url(to));
        }
        fs().rename(url(from), to.substring(b + 1));
    }

    @Override
    public long size(String path) {
        return fs().getLength(url(path));
    }

    @Override
    public long lastModified(String path) {
        return fs().getLastModified(url(path));
    }
}
