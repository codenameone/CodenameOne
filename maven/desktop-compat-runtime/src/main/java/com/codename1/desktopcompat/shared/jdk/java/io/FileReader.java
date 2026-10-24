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

import java.io.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/// Reads the text of a file, in UTF-8 unless a charset is given.
public class FileReader extends InputStreamReader {

    /// Opens the file at `fileName`.
    ///
    /// #### Throws
    ///
    /// - `FileNotFoundException`: when it is not there, is a directory or
    ///   cannot be read
    public FileReader(String fileName) throws FileNotFoundException {
        super(new FileInputStream(fileName), StandardCharsets.UTF_8);
    }

    /// Opens `file`.
    ///
    /// #### Throws
    ///
    /// - `FileNotFoundException`: when it is not there, is a directory or
    ///   cannot be read
    public FileReader(File file) throws FileNotFoundException {
        super(new FileInputStream(file), StandardCharsets.UTF_8);
    }

    /// Opens the file at `fileName`, reading `charset`.
    public FileReader(String fileName, Charset charset) throws IOException {
        super(new FileInputStream(fileName), charset);
    }

    /// Opens `file`, reading `charset`.
    public FileReader(File file, Charset charset) throws IOException {
        super(new FileInputStream(file), charset);
    }
}
