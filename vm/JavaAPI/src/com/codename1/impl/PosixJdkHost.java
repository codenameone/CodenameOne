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
package com.codename1.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// The [JdkHost] of a program translated without Codename One -- the
/// self-hosted translator, the backend, a VM test -- over the native file
/// system.
///
/// It reaches the file natives through `java.io.File` and the file streams,
/// whose behaviour it leaves exactly as it is. Only such a program installs
/// it: an application never names this class, so the translator's
/// reachability keeps those natives out of watch, TV and JavaScript builds,
/// which do not link them. Installed with
/// `JdkHost.install(new PosixJdkHost())` from the program's own start-up.
///
/// Preferences stay the in-memory defaults; a program that needs them kept
/// stores them itself.
public class PosixJdkHost extends JdkHost {
    @Override
    public FileStat fileStat(String path) {
        File f = new File(path);
        if (!f.exists()) {
            return null;
        }
        boolean directory = f.isDirectory();
        return new FileStat(directory, directory ? 0L : f.length(), f.lastModified());
    }

    @Override
    public String[] fileList(String path) throws IOException {
        File f = new File(path);
        return f.isDirectory() ? f.list() : null;
    }

    @Override
    public InputStream fileRead(String path) throws IOException {
        return new FileInputStream(path);
    }

    @Override
    public OutputStream fileWrite(String path, boolean append) throws IOException {
        return new FileOutputStream(path, append);
    }

    @Override
    public boolean fileMkdir(String path) {
        return new File(path).mkdir();
    }

    @Override
    public boolean fileDelete(String path) {
        return new File(path).delete();
    }

    @Override
    public boolean fileRename(String from, String to) {
        File dest = new File(to);
        // rename(2) replaces an existing destination; this contract does not.
        return !dest.exists() && new File(from).renameTo(dest);
    }

    @Override
    public String appHome() {
        // The working directory. Resolving "." and dropping the "." keeps it
        // independent of whether a target's native also normalizes the path.
        String abs = new File(".").getAbsolutePath();
        if (abs.endsWith("/.") || abs.endsWith("\\.")) {
            abs = abs.substring(0, abs.length() - 2);
        }
        return abs.length() == 0 || ".".equals(abs) ? "/" : abs;
    }

    /// `/tmp`. Not the `java.io.tmpdir` property: `System` answers that
    /// property from this method, so reading it here would ask itself.
    /// `TMPDIR` is honoured when the environment sets it.
    @Override
    public String cachesDir() {
        String tmp = System.getenv("TMPDIR");
        return tmp == null || tmp.length() == 0 ? "/tmp" : tmp;
    }

    @Override
    public String[] fileRoots() {
        return new String[] {"/"};
    }

    @Override
    public long fileTotalSpace(String path) {
        return new File(path).getTotalSpace();
    }

    @Override
    public long fileUsableSpace(String path) {
        return new File(path).getUsableSpace();
    }

    /// Reads `/dev/urandom`, which every POSIX system the translator and the
    /// backend run on provides.
    @Override
    public void randomBytes(byte[] out) {
        try {
            InputStream in = new FileInputStream("/dev/urandom");
            try {
                int off = 0;
                while (off < out.length) {
                    int n = in.read(out, off, out.length - off);
                    if (n < 0) {
                        throw new IllegalStateException("/dev/urandom ended");
                    }
                    off += n;
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("No secure random source: " + e.getMessage());
        }
    }
}
