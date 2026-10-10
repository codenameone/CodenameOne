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
package com.codename1.compat.jdk;

import java.io.IOException;
import java.io.OutputStreamWriter;

/// `java.io.FileWriter` for the Codename One runtime. Writes UTF-8, Android's
/// default charset.
public class FileWriter extends OutputStreamWriter {

    public FileWriter(String fileName) throws IOException {
        super(new FileOutputStream(fileName), "UTF-8");
    }

    public FileWriter(String fileName, boolean append) throws IOException {
        super(new FileOutputStream(fileName, append), "UTF-8");
    }

    public FileWriter(File file) throws IOException {
        super(new FileOutputStream(file), "UTF-8");
    }

    public FileWriter(File file, boolean append) throws IOException {
        super(new FileOutputStream(file, append), "UTF-8");
    }
}
