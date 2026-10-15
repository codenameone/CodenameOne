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
package java.io;

/// The native file system: the C natives behind `java.io.File` and the file
/// streams, for `com.codename1.impl.PosixJdkHost`.
///
/// Internal API of the ParparVM class library; applications compile against
/// a class library that does not have it. `java.io.File` and the streams go
/// through `com.codename1.impl.JdkHost`, and only `PosixJdkHost` -- installed
/// by a program translated without Codename One -- reaches the natives,
/// through this class. That keeps them out of the reach of an application,
/// whose watch, TV and JavaScript builds do not link them.
///
/// Paths are passed to the natives as they are, except that a `file:` scheme
/// is dropped.
public final class PosixFileAccess {

    private PosixFileAccess() {
    }

    private static String p(String path) {
        if (path.startsWith("file:")) {
            int i = 5;
            while (i + 1 < path.length() && path.charAt(i) == '/' && path.charAt(i + 1) == '/') {
                i++;
            }
            return path.substring(i);
        }
        return path;
    }

    /// Whether something is at `path`.
    public static boolean exists(String path) {
        return File.nExists(p(path));
    }

    /// Whether `path` is a directory.
    public static boolean isDirectory(String path) {
        return File.nIsDirectory(p(path));
    }

    /// Whether `path` is a regular file.
    public static boolean isFile(String path) {
        return File.nIsFile(p(path));
    }

    /// Whether `path` is hidden.
    public static boolean isHidden(String path) {
        return File.nIsHidden(p(path));
    }

    /// When `path` was last modified, in milliseconds since the epoch.
    public static long lastModified(String path) {
        return File.nLastModified(p(path));
    }

    /// The length of the file `path` in bytes.
    public static long length(String path) {
        return File.nLength(p(path));
    }

    /// Creates the empty file `path` atomically; false when something was
    /// there or it could not be created.
    public static boolean createNewFile(String path) {
        return File.nCreateNewFile(p(path));
    }

    /// Deletes the file or empty directory `path`.
    public static boolean delete(String path) {
        return File.nDelete(p(path));
    }

    /// The names of the entries of the directory `path`, or null.
    public static String[] list(String path) {
        return File.nList(p(path));
    }

    /// Creates the directory `path`.
    public static boolean mkdir(String path) {
        return File.nMkdir(p(path));
    }

    /// Moves `from` to `to` with rename(2), replacing what is there.
    public static boolean rename(String from, String to) {
        return File.nRename(p(from), p(to));
    }

    /// Makes `path` read-only.
    public static boolean setReadOnly(String path) {
        return File.nSetReadOnly(p(path));
    }

    /// Grants or takes away writing `path`.
    public static boolean setWritable(String path, boolean on) {
        return File.nSetWritable(p(path), on);
    }

    /// Grants or takes away reading `path`.
    public static boolean setReadable(String path, boolean on) {
        return File.nSetReadable(p(path), on);
    }

    /// Grants or takes away executing `path`.
    public static boolean setExecutable(String path, boolean on) {
        return File.nSetExecutable(p(path), on);
    }

    /// Whether `path` may be read.
    public static boolean canRead(String path) {
        return File.nCanRead(p(path));
    }

    /// Whether `path` may be written.
    public static boolean canWrite(String path) {
        return File.nCanWrite(p(path));
    }

    /// Whether `path` may be executed.
    public static boolean canExecute(String path) {
        return File.nCanExecute(p(path));
    }

    /// The size of the volume holding `path`.
    public static long totalSpace(String path) {
        return File.nTotalSpace(p(path));
    }

    /// The bytes free on the volume holding `path`.
    public static long freeSpace(String path) {
        return File.nFreeSpace(p(path));
    }

    /// The bytes this program may use on the volume holding `path`.
    public static long usableSpace(String path) {
        return File.nUsableSpace(p(path));
    }

    /// `path` resolved against the working directory.
    public static String absolutePath(String path) {
        return File.nAbsolutePath(p(path));
    }

    /// `path` with symbolic links, `.` and `..` resolved, where the file
    /// system can.
    public static String canonicalPath(String path) {
        return File.nCanonicalPath(p(path));
    }

    /// Opens the file `path` for reading.
    public static InputStream openRead(String path) throws FileNotFoundException {
        return new NativeFileInputStream(p(path));
    }

    /// Opens the file `path` for writing, at its end when `append` is true.
    public static OutputStream openWrite(String path, boolean append) throws FileNotFoundException {
        return new NativeFileOutputStream(p(path), append);
    }
}
