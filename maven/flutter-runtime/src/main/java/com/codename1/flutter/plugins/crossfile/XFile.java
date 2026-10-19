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
package com.codename1.flutter.plugins.crossfile;

import dart.async.Future;
import dart.io.File;
import dart.typed_data.Uint8List;

/**
 * package:cross_file's {@code XFile}, which image_picker returns and share_plus takes:
 * a file by path, read through dart:io's File. A file made from bytes is held in
 * memory.
 */
public class XFile {
    private final String path;
    private String mimeType;
    private String name;
    private final Uint8List bytes;

    public XFile(String path, String mimeType, String name, Object length, Uint8List bytes,
            Object lastModified) {
        this.path = path;
        this.mimeType = mimeType;
        this.name = name;
        this.bytes = bytes;
    }

    public XFile(String path) {
        this(path, null, null, null, null, null);
    }

    /** {@code XFile.fromData(bytes, {mimeType, name, length, lastModified, path})}. */
    public static XFile fromData(Uint8List bytes, String mimeType, String name, Object length,
            Object lastModified, String path) {
        return new XFile(path == null ? "" : path, mimeType, name, length, bytes, lastModified);
    }

    /** The stub's named {@code mimeType:} argument. */
    public void mimeType(String v) {
        mimeType = v;
    }

    /** The stub's named {@code name:} argument. */
    public void name(String v) {
        name = v;
    }

    public String path() {
        return path;
    }

    public String name() {
        if (name != null) {
            return name;
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash < 0 ? path : path.substring(slash + 1);
    }

    public String mimeType() {
        return mimeType;
    }

    public Future<Uint8List> readAsBytes() {
        return bytes != null ? Future.value(bytes) : new File(path).readAsBytes();
    }

    public Future<String> readAsString() {
        if (bytes != null) {
            try {
                return Future.value(new String(bytes.toBytes(), "UTF-8"));
            } catch (java.io.UnsupportedEncodingException e) {
                return Future.error(e);
            }
        }
        return new File(path).readAsString();
    }

    public Future<Long> length() {
        return bytes != null ? Future.value(Long.valueOf(bytes.length())) : new File(path).length();
    }

    public Future<Object> saveTo(String target) {
        final File out = new File(target);
        return readAsBytes().then(new dart.runtime.Funcs.Func1<Uint8List, Object>() {
            @Override
            public Object call(Uint8List b) {
                return out.writeAsBytes(b);
            }
        });
    }
}
