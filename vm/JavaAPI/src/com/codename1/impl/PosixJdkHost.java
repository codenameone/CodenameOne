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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PosixFileAccess;

/// The [JdkHost] of a program translated without Codename One -- the
/// self-hosted translator, the backend, a VM test -- over the native file
/// system.
///
/// It reaches the file natives through `java.io.PosixFileAccess`, never
/// through `java.io.File` or the file streams, which themselves go through
/// the installed host. Only such a program installs it: an application never
/// names this class, so the translator's reachability keeps those natives out
/// of watch, TV and JavaScript builds, which do not link them. Installed with
/// `JdkHost.install(new PosixJdkHost())` from the program's own start-up;
/// without it `java.io.File` sees the empty file system of the defaults.
///
/// Preferences stay the in-memory defaults; a program that needs them kept
/// stores them itself.
public class PosixJdkHost extends JdkHost {
    @Override
    public FileStat fileStat(String path) {
        if (!PosixFileAccess.exists(path)) {
            return null;
        }
        boolean directory = PosixFileAccess.isDirectory(path);
        return new FileStat(directory, directory ? 0L : PosixFileAccess.length(path),
                PosixFileAccess.lastModified(path));
    }

    @Override
    public String[] fileList(String path) throws IOException {
        return PosixFileAccess.isDirectory(path) ? PosixFileAccess.list(path) : null;
    }

    @Override
    public InputStream fileRead(String path) throws IOException {
        if (PosixFileAccess.isDirectory(path)) {
            throw new FileNotFoundException(path + " (Is a directory)");
        }
        return PosixFileAccess.openRead(path);
    }

    @Override
    public OutputStream fileWrite(String path, boolean append) throws IOException {
        if (PosixFileAccess.isDirectory(path)) {
            throw new FileNotFoundException(path + " (Is a directory)");
        }
        return PosixFileAccess.openWrite(path, append);
    }

    @Override
    public boolean fileMkdir(String path) {
        return PosixFileAccess.mkdir(path);
    }

    @Override
    public boolean fileDelete(String path) {
        return PosixFileAccess.delete(path);
    }

    @Override
    public boolean fileRename(String from, String to) {
        // rename(2) replaces an existing destination; this contract does not.
        return !PosixFileAccess.exists(to) && PosixFileAccess.rename(from, to);
    }

    @Override
    public boolean fileReplace(String from, String to) {
        return PosixFileAccess.rename(from, to);
    }

    @Override
    public boolean fileTest(String path, int what) {
        switch (what) {
            case FILE_EXISTS:
                return PosixFileAccess.exists(path);
            case FILE_DIRECTORY:
                return PosixFileAccess.isDirectory(path);
            case FILE_REGULAR:
                return PosixFileAccess.isFile(path);
            case FILE_READABLE:
                return PosixFileAccess.canRead(path);
            case FILE_WRITABLE:
                return PosixFileAccess.canWrite(path);
            case FILE_EXECUTABLE:
                return PosixFileAccess.canExecute(path);
            case FILE_HIDDEN:
                return PosixFileAccess.isHidden(path);
            default:
                return false;
        }
    }

    @Override
    public boolean fileSet(String path, int what, boolean on) {
        switch (what) {
            case FILE_READABLE:
                return PosixFileAccess.setReadable(path, on);
            case FILE_WRITABLE:
                return PosixFileAccess.setWritable(path, on);
            case FILE_EXECUTABLE:
                return PosixFileAccess.setExecutable(path, on);
            default:
                return false;
        }
    }

    @Override
    public long fileLength(String path) {
        return PosixFileAccess.length(path);
    }

    @Override
    public long fileLastModified(String path) {
        return PosixFileAccess.lastModified(path);
    }

    /// Atomic: the native creates with `O_CREAT | O_EXCL`.
    @Override
    public boolean fileCreate(String path) throws IOException {
        return PosixFileAccess.createNewFile(path);
    }

    @Override
    public String fileCanonicalPath(String path) throws IOException {
        return PosixFileAccess.canonicalPath(path);
    }

    @Override
    public String appHome() {
        // The working directory. Resolving "." and dropping the "." keeps it
        // independent of whether a target's native also normalizes the path.
        String abs = PosixFileAccess.absolutePath(".");
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
        return PosixFileAccess.totalSpace(path);
    }

    @Override
    public long fileUsableSpace(String path) {
        return PosixFileAccess.usableSpace(path);
    }

    @Override
    public long fileFreeSpace(String path) {
        return PosixFileAccess.freeSpace(path);
    }

    /// Reads `/dev/urandom`, which every POSIX system the translator and the
    /// backend run on provides.
    @Override
    public void randomBytes(byte[] out) {
        try {
            InputStream in = PosixFileAccess.openRead("/dev/urandom");
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
