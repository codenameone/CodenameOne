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
package com.codename1.desktopcompat.shared.jdk.java.nio.file;

import com.codename1.desktopcompat.shared.jdk.java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

/// A path in the platform's file system, the one `com.codename1.desktopcompat.shared.jdk.java.io.File` reaches.
///
/// #### What a path is here
///
/// A sequence of names separated by `/`, after an optional root. A backslash
/// is read as a separator. The root is `/`, or -- because Codename One names
/// its files `file:///...` and `com.codename1.desktopcompat.shared.jdk.java.io.File.getAbsolutePath()` answers that
/// form -- the `file:` scheme with the slashes after it. A relative path is
/// resolved against the application's home directory when it is used, which
/// is also what [#toAbsolutePath()] answers.
///
/// The methods that only rearrange names (`resolve`, `relativize`,
/// `normalize`, `getParent`, `startsWith`, ...) answer what the JDK answers
/// on a Unix file system.
///
/// #### What is not provided
///
/// `getFileSystem()` and `register(...)`: there is one file system and it
/// cannot be watched. [#toRealPath] is `File.getCanonicalPath()`: where the
/// file system cannot resolve symbolic links it answers the absolute path
/// with `.` and `..` resolved.
public interface Path extends Comparable<Path>, Iterable<Path> {

    /// The path `first`, with each of `more` appended as further names.
    static Path of(String first, String... more) {
        return PathImpl.of(first, more);
    }

    /// The path a `file:` URI names.
    static Path of(URI uri) {
        return PathImpl.of(uri);
    }

    boolean isAbsolute();

    Path getRoot();

    Path getFileName();

    Path getParent();

    int getNameCount();

    Path getName(int index);

    Path subpath(int beginIndex, int endIndex);

    boolean startsWith(Path other);

    boolean startsWith(String other);

    boolean endsWith(Path other);

    boolean endsWith(String other);

    Path normalize();

    Path resolve(Path other);

    Path resolve(String other);

    Path resolveSibling(Path other);

    Path resolveSibling(String other);

    Path relativize(Path other);

    URI toUri();

    Path toAbsolutePath();

    /// The absolute path with `.` and `..` resolved, and symbolic links where
    /// the file system can. Throws [NoSuchFileException] when nothing is there.
    Path toRealPath(LinkOption... options) throws IOException;

    File toFile();

    @Override
    Iterator<Path> iterator();

    @Override
    int compareTo(Path other);
}
