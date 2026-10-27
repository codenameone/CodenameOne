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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// The file system `open()`, `os` and `os.path` see.
///
/// Paths are POSIX-style absolute paths (`/a/b`); the caller has already
/// joined a relative path with the working directory. An application runs
/// on Codename One's `FileSystemStorage` unless something else is
/// installed, and the working directory starts at the application's home.
public abstract class PyFileSystem {
    private static PyFileSystem current = new Cn1FileSystem();

    /// The file system in use.
    public static PyFileSystem get() {
        return current;
    }

    /// Replaces the file system, for a host that is not a Codename One
    /// application (a test harness, a command line tool).
    public static void install(PyFileSystem fs) {
        current = fs;
    }

    /// The directory a relative path starts from when the program begins.
    public abstract String home();

    public abstract InputStream openInput(String path) throws IOException;

    public abstract OutputStream openOutput(String path, boolean append) throws IOException;

    public abstract boolean exists(String path);

    public abstract boolean isDirectory(String path);

    /// The names (not paths) of the entries of directory `path`.
    public abstract String[] list(String path) throws IOException;

    public abstract void mkdir(String path) throws IOException;

    /// Deletes a file or an empty directory.
    public abstract void delete(String path) throws IOException;

    public abstract void rename(String from, String to) throws IOException;

    public abstract long size(String path);

    /// Milliseconds since the epoch.
    public abstract long lastModified(String path);
}
