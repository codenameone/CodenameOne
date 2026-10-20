/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package dart.io;

import dart.async.Future;

/** Dart's {@code FileSystemEntity}: the part {@link File} and {@link Directory} share. */
public abstract class FileSystemEntity {

    final String path;

    FileSystemEntity(String path) {
        if (path == null) {
            throw new dart.core.ArgumentError("path must not be null");
        }
        this.path = path;
    }

    public String path() {
        return path;
    }

    public dart.core.DartUri uri() {
        return dart.core.DartUri.parse(path.startsWith("/") ? "file://" + path : path);
    }

    public boolean existsSync() {
        return Fs.get().exists(path);
    }

    public Future<Boolean> exists() {
        return Future.value(Boolean.valueOf(existsSync()));
    }

    public Directory parent() {
        return new Directory(Fs.parentOf(path));
    }

    /// The path {@code absolute} answers: a relative one joined to Directory.current,
    /// the same base every read and write resolves it against.
    /// Dart's {@code uri.toFilePath()} as {@code File.fromUri} uses it: only a file
    /// (or scheme-less) URI names a file, and its path is percent-decoded, so
    /// file:///tmp/a%20b is "/tmp/a b" rather than a file literally named a%20b.
    static String uriToFilePath(dart.core.DartUri uri) {
        String scheme = uri.scheme();
        if (scheme.length() != 0 && !"file".equals(scheme)) {
            throw new dart.core.UnsupportedError("Cannot extract a file path from a " + scheme + " URI");
        }
        if (uri.query().length() != 0) {
            throw new dart.core.UnsupportedError("Cannot extract a file path from a URI with a query component");
        }
        if (uri.fragment().length() != 0) {
            throw new dart.core.UnsupportedError("Cannot extract a file path from a URI with a fragment component");
        }
        return dart.core.DartUri.decodeComponent(uri.path());
    }

    static String absolutePath(String path) {
        if (path.startsWith("/") || path.startsWith("file:")) {
            return path;
        }
        String base = Directory.current.path();
        return (base.endsWith("/") ? base : base + "/") + path;
    }

    public boolean isAbsolute() {
        return path.startsWith("/") || path.startsWith("file:");
    }

    public void deleteSync() {
        deleteSync(Boolean.FALSE);
    }

    public void deleteSync(Boolean recursive) {
        Fs fs = Fs.get();
        if (!fs.exists(path)) {
            throw new FileSystemException("Deletion failed", path);
        }
        if (recursive != null && recursive.booleanValue() && fs.isDirectory(path)) {
            for (String child : fs.list(path)) {
                if (fs.isDirectory(child)) {
                    new Directory(child).deleteSync(Boolean.TRUE);
                } else {
                    fs.delete(child);
                }
            }
        }
        fs.delete(path);
    }

    public Future<FileSystemEntity> delete() {
        return delete(Boolean.FALSE);
    }

    public Future<FileSystemEntity> delete(Boolean recursive) {
        try {
            deleteSync(recursive);
            return Future.value(this);
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }

    /** Dart's {@code FileSystemEntity.isDirectorySync(path)}. */
    public static boolean isDirectorySync(String path) {
        return Fs.get().isDirectory(path);
    }

    public static Future<Boolean> isDirectory(String path) {
        return Future.value(Boolean.valueOf(isDirectorySync(path)));
    }

    public static boolean isFileSync(String path) {
        Fs fs = Fs.get();
        return fs.exists(path) && !fs.isDirectory(path);
    }

    public static Future<Boolean> isFile(String path) {
        return Future.value(Boolean.valueOf(isFileSync(path)));
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o.getClass() == getClass() && ((FileSystemEntity) o).path.equals(path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
