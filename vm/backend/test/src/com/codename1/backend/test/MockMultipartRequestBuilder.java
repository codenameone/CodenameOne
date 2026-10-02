/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/// A `multipart/form-data` request, from [MockMvcRequestBuilders#multipart]:
/// files and fields, encoded as a browser's form post would be.
public final class MockMultipartRequestBuilder extends MockRequestBuilder {
    private static final String BOUNDARY = "cn1-mockmvc-boundary-7MA4YWxkTrZu0gW";
    private final List parts = new ArrayList();

    MockMultipartRequestBuilder(String method, String uriTemplate, Object[] uriVariables) {
        super(method, uriTemplate, uriVariables);
    }

    /// Adds a file part.
    public MockMultipartRequestBuilder file(MockMultipartFile file) {
        parts.add(file);
        return this;
    }

    /// Adds a file part with no filename.
    public MockMultipartRequestBuilder file(String name, byte[] content) {
        return file(new MockMultipartFile(name, content));
    }

    /// Adds form fields, as parts: what `@RequestParam` reads from a multipart body.
    @Override
    public MockMultipartRequestBuilder param(String name, String... values) {
        for (String value : values) {
            parts.add(new String[] {name, value});
        }
        return this;
    }

    @Override
    byte[] body() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Object entry : parts) {
            Object part = entry;
            write(out, "--" + BOUNDARY + "\r\n");
            if (part instanceof MockMultipartFile) {
                MockMultipartFile f = (MockMultipartFile) part;
                write(out, "Content-Disposition: form-data; name=\"" + f.getName() + "\""
                        + (f.getOriginalFilename() == null ? ""
                        : "; filename=\"" + f.getOriginalFilename() + "\"") + "\r\n");
                if (f.getContentType() != null) {
                    write(out, "Content-Type: " + f.getContentType() + "\r\n");
                }
                write(out, "\r\n");
                byte[] content = f.getBytes();
                out.write(content, 0, content.length);
            } else if (part instanceof String[]) {
                String[] field = (String[]) part;
                write(out, "Content-Disposition: form-data; name=\"" + field[0] + "\"\r\n\r\n");
                write(out, field[1]);
            }
            write(out, "\r\n");
        }
        write(out, "--" + BOUNDARY + "--\r\n");
        return out.toByteArray();
    }

    @Override
    String bodyType() {
        return "multipart/form-data; boundary=" + BOUNDARY;
    }

    private static void write(ByteArrayOutputStream out, String text) {
        byte[] bytes = utf8(text);
        out.write(bytes, 0, bytes.length);
    }
}
