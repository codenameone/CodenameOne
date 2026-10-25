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
package com.codename1.desktopcompat.shared.jdk.java.io;

import com.codename1.io.FileSystemStorage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// The file system of the `java.io` shims: `FileSystemStorage` paths, or
/// plain absolute paths, which are taken as `file://` paths.
final class Host {
    /// [#fileTest]: something is at the path.
    static final int FILE_EXISTS = 1;
    /// [#fileTest]: the path is a directory.
    static final int FILE_DIRECTORY = 2;
    /// [#fileTest]: the path is a file that is not a directory.
    static final int FILE_REGULAR = 3;
    /// [#fileTest] and [#fileSet]: the path may be read.
    static final int FILE_READABLE = 4;
    /// [#fileTest] and [#fileSet]: the path may be written.
    static final int FILE_WRITABLE = 5;
    /// [#fileTest] and [#fileSet]: the path may be executed or entered.
    static final int FILE_EXECUTABLE = 6;
    /// [#fileTest]: the path is hidden.
    static final int FILE_HIDDEN = 7;

    private Host() {
    }

    private static FileSystemStorage fs() {
        return FileSystemStorage.getInstance();
    }

    /// `path` as a `FileSystemStorage` path: `/x` and a drive path `C:/x`
    /// gain the `file://` scheme; anything with a scheme is left alone.
    static String storage(String path) {
        if (path.startsWith("/")) {
            return "file://" + path;
        }
        if (path.length() >= 3 && path.charAt(1) == ':' && path.charAt(2) == '/'
                && ((path.charAt(0) >= 'A' && path.charAt(0) <= 'Z')
                || (path.charAt(0) >= 'a' && path.charAt(0) <= 'z'))) {
            return "file:///" + path;
        }
        return path;
    }

    /// Whether `path` has the property `what`, one of the `FILE_` constants.
    /// A file system without permissions lets everything that exists be read
    /// and written, a directory be entered and nothing be run.
    static boolean fileTest(String path, int what) {
        String p = storage(path);
        FileSystemStorage fs = fs();
        if (!fs.exists(p)) {
            return false;
        }
        if (what == FILE_HIDDEN) {
            return fs.isHidden(p);
        }
        if (what == FILE_DIRECTORY || what == FILE_EXECUTABLE) {
            return fs.isDirectory(p);
        }
        if (what == FILE_REGULAR) {
            return !fs.isDirectory(p);
        }
        return true;
    }

    /// Nothing can be changed, so this answers whether the path already is
    /// as asked, by the rules of [#fileTest].
    static boolean fileSet(String path, int what, boolean on) {
        String p = storage(path);
        FileSystemStorage fs = fs();
        if (!fs.exists(p)) {
            return false;
        }
        if (what == FILE_EXECUTABLE) {
            return on == fs.isDirectory(p);
        }
        return on;
    }

    static long fileLength(String path) {
        String p = storage(path);
        FileSystemStorage fs = fs();
        return !fs.exists(p) || fs.isDirectory(p) ? 0L : fs.getLength(p);
    }

    static long fileLastModified(String path) {
        String p = storage(path);
        FileSystemStorage fs = fs();
        return fs.exists(p) ? fs.getLastModified(p) : 0L;
    }

    /// Check-then-create; not atomic.
    static boolean fileCreate(String path) throws IOException {
        if (fs().exists(storage(path))) {
            return false;
        }
        fileWrite(path, false).close();
        return true;
    }

    /// Always false; the caller copies.
    static boolean fileReplace(String from, String to) {
        return false;
    }

    /// Null: storage cannot resolve links, the caller resolves the dots.
    static String fileCanonicalPath(String path) {
        return null;
    }

    static String[] fileList(String path) throws IOException {
        path = storage(path);
        FileSystemStorage fs = fs();
        if (!fs.exists(path) || !fs.isDirectory(path)) {
            return null;
        }
        return fs.listFiles(path);
    }

    static InputStream fileRead(String path) throws IOException {
        path = storage(path);
        FileSystemStorage fs = fs();
        if (!fs.exists(path) || fs.isDirectory(path)) {
            throw new FileNotFoundException(path);
        }
        return fs.openInputStream(path);
    }

    static OutputStream fileWrite(String path, boolean append) throws IOException {
        path = storage(path);
        FileSystemStorage fs = fs();
        if (fs.exists(path) && fs.isDirectory(path)) {
            throw new FileNotFoundException(path + " (Is a directory)");
        }
        if (append && fs.exists(path)) {
            long length = fs.getLength(path);
            if (length > Integer.MAX_VALUE) {
                // The storage API takes the offset as an int. Narrowed, it
                // wraps, and the write would land inside the file.
                throw new IOException(path + " (Too large to append to)");
            }
            return fs.openOutputStream(path, (int) length);
        }
        return fs.openOutputStream(path);
    }

    static boolean fileMkdir(String path) {
        path = storage(path);
        FileSystemStorage fs = fs();
        if (fs.exists(path)) {
            return false;
        }
        // mkdir(2) creates one level; a storage that creates the missing
        // parents too would make File.mkdir answer true where the JDK does not.
        String parent = parentOf(path);
        if (parent != null && !(fs.exists(parent) && fs.isDirectory(parent))) {
            return false;
        }
        fs.mkdir(path);
        return fs.exists(path) && fs.isDirectory(path);
    }

    static boolean fileDelete(String path) {
        path = storage(path);
        FileSystemStorage fs = fs();
        if (!fs.exists(path)) {
            return false;
        }
        if (fs.isDirectory(path)) {
            // The JDK deletes only an empty directory; some storages delete
            // the whole tree.
            String[] entries;
            try {
                entries = fs.listFiles(path);
            } catch (IOException e) {
                return false;
            }
            if (entries != null && entries.length > 0) {
                return false;
            }
        }
        fs.delete(path);
        return !fs.exists(path);
    }

    /// Renames within one directory, which is all `FileSystemStorage` can do;
    /// a move to another directory answers false and the caller copies.
    static boolean fileRename(String from, String to) {
        from = storage(from);
        to = storage(to);
        FileSystemStorage fs = fs();
        int fromSlash = lastSeparator(from);
        int toSlash = lastSeparator(to);
        if (fromSlash < 0 || toSlash < 0 || fs.exists(to) || !fs.exists(from)
                || !from.substring(0, fromSlash).equals(to.substring(0, toSlash))) {
            return false;
        }
        fs.rename(from, to.substring(toSlash + 1));
        return fs.exists(to) && !fs.exists(from);
    }

    /// The parent of the storage path `path`, or null at a root.
    private static String parentOf(String path) {
        String p = path;
        while (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        int slash = lastSeparator(p);
        if (slash <= 0) {
            return null;
        }
        String parent = p.substring(0, slash);
        // "file://" itself is no directory: the parent of /x is the root.
        return parent.endsWith(":/") || parent.endsWith(":") ? parent + "/" : parent;
    }

    private static int lastSeparator(String path) {
        int slash = path.lastIndexOf('/');
        char sep = fs().getFileSystemSeparator();
        return sep == '/' ? slash : Math.max(slash, path.lastIndexOf(sep));
    }

    static String appHome() {
        return fs().getAppHomePath();
    }

    static String cachesDir() {
        FileSystemStorage fs = fs();
        return fs.hasCachesDir() ? fs.getCachesDir() : null;
    }

    static String[] fileRoots() {
        return fs().getRoots();
    }

    static long fileTotalSpace(String path) {
        String root = rootOf(storage(path));
        return root == null ? 0L : fs().getRootSizeBytes(root);
    }

    static long fileUsableSpace(String path) {
        String root = rootOf(storage(path));
        return root == null ? 0L : fs().getRootAvailableSpace(root);
    }

    static long fileFreeSpace(String path) {
        return fileUsableSpace(path);
    }

    /// The longest root `path` lies under, else the first root, else null.
    private static String rootOf(String path) {
        String[] roots = fs().getRoots();
        if (roots == null || roots.length == 0) {
            return null;
        }
        String best = null;
        for (String r : roots) {
            if (r != null && path.startsWith(r) && (best == null || r.length() > best.length())) {
                best = r;
            }
        }
        return best != null ? best : roots[0];
    }
}
