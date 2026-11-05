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
package com.codename1.flutter;

/// Flutter's FileImage: a picture read from a dart:io File. The bytes come from
/// the same file layer File.readAsBytesSync uses (FileSystemStorage on a device)
/// and are then drawn exactly as a MemoryImage's are.
public class FileImage extends ImageProvider {
    private final Object file;
    private double scale = 1.0;

    public FileImage(Object file) {
        this.file = file;
    }

    public void scale(double v) {
        this.scale = v;
    }

    public double getScale() {
        return scale;
    }

    public Object file() {
        return file;
    }

    private String path() {
        if (file instanceof dart.io.FileSystemEntity) {
            return ((dart.io.FileSystemEntity) file).path();
        }
        return file == null ? null : file.toString();
    }

    /// The file's bytes, or null when it cannot be read.
    public byte[] readBytes() {
        String p = path();
        if (p == null) {
            return null;
        }
        try {
            dart.typed_data.Uint8List data = new dart.io.File(p).readAsBytesSync();
            return data == null ? null : data.toBytes();
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public String sourceKey() {
        return "file:" + path() + "@" + scale;
    }
}
