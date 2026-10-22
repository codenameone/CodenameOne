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
package com.codename1.flutter.plugins.dio;

import dart.async.Future;
import dart.io.File;

/// dio's `MultipartFile`: a file part of a FormData upload.
public class MultipartFile {
    final byte[] bytes;
    final String filename;
    final String contentType;

    MultipartFile(byte[] bytes, String filename, String contentType) {
        this.bytes = bytes;
        this.filename = filename;
        this.contentType = contentType == null ? "application/octet-stream" : contentType;
    }

    public static Future<MultipartFile> fromFile(String path, String filename, Object contentType,
            Object headers) {
        try {
            return Future.value(fromFileSync(path, filename, contentType, headers));
        } catch (RuntimeException e) {
            return Future.<MultipartFile>error(e);
        }
    }

    public static MultipartFile fromFileSync(String path, String filename, Object contentType, Object headers) {
        byte[] data = new File(path).readAsBytesSync().toBytes();
        String name = filename;
        if (name == null) {
            int slash = path.lastIndexOf('/');
            name = slash >= 0 ? path.substring(slash + 1) : path;
        }
        return new MultipartFile(data, name, contentType == null ? null : String.valueOf(contentType));
    }

    public static MultipartFile fromBytes(java.util.List<?> value, String filename, Object contentType,
            Object headers) {
        byte[] data = new byte[value.size()];
        for (int i = 0; i < data.length; i++) {
            Object o = value.get(i);
            data[i] = (byte) (o instanceof Number ? ((Number) o).intValue() : 0);
        }
        return new MultipartFile(data, filename, contentType == null ? null : String.valueOf(contentType));
    }

    public static MultipartFile fromString(String value, String filename, Object contentType, Object headers) {
        byte[] data;
        try {
            data = value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            data = value.getBytes();
        }
        return new MultipartFile(data, filename, contentType == null ? "text/plain" : String.valueOf(contentType));
    }

    public String filename() {
        return filename;
    }

    public long length() {
        return bytes.length;
    }
}
