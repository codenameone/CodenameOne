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

import java.nio.charset.Charset;

/// Writes text to a file, in UTF-8 unless a charset is given.
public class FileWriter extends OutputStreamWriter {

    /// Opens the file at `fileName`, emptying it.
    public FileWriter(String fileName) throws IOException {
        super(new FileOutputStream(fileName));
    }

    /// Opens the file at `fileName`, at its end when `append` is true.
    public FileWriter(String fileName, boolean append) throws IOException {
        super(new FileOutputStream(fileName, append));
    }

    /// Opens `file`, emptying it.
    public FileWriter(File file) throws IOException {
        super(new FileOutputStream(file));
    }

    /// Opens `file`, at its end when `append` is true.
    public FileWriter(File file, boolean append) throws IOException {
        super(new FileOutputStream(file, append));
    }

    /// Opens the file at `fileName`, emptying it, writing `charset`.
    public FileWriter(String fileName, Charset charset) throws IOException {
        super(new FileOutputStream(fileName), charset.name());
    }

    /// Opens the file at `fileName`, at its end when `append` is true,
    /// writing `charset`.
    public FileWriter(String fileName, Charset charset, boolean append) throws IOException {
        super(new FileOutputStream(fileName, append), charset.name());
    }

    /// Opens `file`, emptying it, writing `charset`.
    public FileWriter(File file, Charset charset) throws IOException {
        super(new FileOutputStream(file), charset.name());
    }

    /// Opens `file`, at its end when `append` is true, writing `charset`.
    public FileWriter(File file, Charset charset, boolean append) throws IOException {
        super(new FileOutputStream(file, append), charset.name());
    }
}
