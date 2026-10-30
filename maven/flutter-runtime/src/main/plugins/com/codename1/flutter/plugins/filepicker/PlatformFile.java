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
package com.codename1.flutter.plugins.filepicker;

import dart.typed_data.Uint8List;

/** file_picker's {@code PlatformFile}: a picked file's path, name, size and, on request, bytes. */
public class PlatformFile {
    private String path;
    private String name;
    private long size;
    private Uint8List bytes;
    private String identifier;

    public PlatformFile() {
    }

    public void path(String v) {
        path = v;
    }

    public void name(String v) {
        name = v;
    }

    public void size(long v) {
        size = v;
    }

    public void bytes(Uint8List v) {
        bytes = v;
    }

    public void identifier(String v) {
        identifier = v;
    }

    public PlatformFile(String path, String name, long size, Uint8List bytes, Object readStream,
            String identifier) {
        this.path = path;
        this.name = name;
        this.size = size;
        this.bytes = bytes;
        this.identifier = identifier;
    }

    /** Accepted, not kept: files are read whole or by path. */
    public void readStream(Object v) {
    }

    public String path() {
        return path;
    }

    public String name() {
        return name;
    }

    public long size() {
        return size;
    }

    public Uint8List bytes() {
        return bytes;
    }

    public String identifier() {
        return identifier;
    }

    public String extension() {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot < 0 ? null : name.substring(dot + 1);
    }

    @Override
    public String toString() {
        return "PlatformFile(path: " + path + ", name: " + name + ", size: " + size + ")";
    }
}
