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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dart.core.MapEntry;
import dart.runtime.DartRuntime;

/// dio's `FormData`: a multipart/form-data body of fields and files.
public class FormData {
    private final List<String[]> fields = new ArrayList<String[]>();
    private final List<Object[]> files = new ArrayList<Object[]>();
    final String boundary = "--dio-boundary-" + Long.toString(System.currentTimeMillis(), 36)
            + Integer.toString((int) (Math.random() * 1e6), 36);

    public FormData() {
    }

    /// `FormData.fromMap({...})`: a MultipartFile value is a file part, a list
    /// repeats its key, anything else is a text field.
    public static FormData fromMap(Map<String, Object> map, Object collectionFormat, Object camelCaseContentDisposition) {
        FormData f = new FormData();
        if (map != null) {
            for (Map.Entry<String, Object> e : map.entrySet()) {
                f.addValue(e.getKey(), e.getValue());
            }
        }
        return f;
    }

    private void addValue(String key, Object v) {
        if (v instanceof MultipartFile) {
            files.add(new Object[] {key, v});
        } else if (v instanceof List) {
            for (Object o : (List<?>) v) {
                addValue(key, o);
            }
        } else if (v != null) {
            fields.add(new String[] {key, DartRuntime.str(v)});
        }
    }

    /// `formData.fields.add(MapEntry(k, v))` and `formData.files.add(MapEntry(k, file))`
    /// go through these.
    public void addField(MapEntry<String, String> e) {
        fields.add(new String[] {e.key(), e.value()});
    }

    public void addFile(MapEntry<String, MultipartFile> e) {
        files.add(new Object[] {e.key(), e.value()});
    }

    String contentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    byte[] encode() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (String[] f : fields) {
                write(out, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + f[0]
                        + "\"\r\n\r\n" + f[1] + "\r\n");
            }
            for (Object[] f : files) {
                MultipartFile mf = (MultipartFile) f[1];
                write(out, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + f[0]
                        + "\"; filename=\"" + (mf.filename == null ? "file" : mf.filename) + "\"\r\n"
                        + "Content-Type: " + mf.contentType + "\r\n\r\n");
                out.write(mf.bytes);
                write(out, "\r\n");
            }
            write(out, "--" + boundary + "--\r\n");
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private static void write(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes("UTF-8"));
    }

    public long length() {
        return encode().length;
    }
}
