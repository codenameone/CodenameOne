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
package com.codename1.flutter.plugins.firebaseai;

import java.util.LinkedHashMap;
import java.util.Map;

import com.codename1.util.Base64;

import dart.typed_data.Uint8List;

/** firebase_ai's {@code InlineDataPart}: bytes and their MIME type, sent base64 encoded. */
public class InlineDataPart extends Part {
    private final String mimeType;
    private final Uint8List bytes;

    public InlineDataPart(String mimeType, Uint8List bytes) {
        this.mimeType = mimeType;
        this.bytes = bytes;
    }

    /** {@code willContinue}: accepted, not sent. */
    public void willContinue(Boolean v) {
    }

    public String mimeType() {
        return mimeType;
    }

    public Uint8List bytes() {
        return bytes;
    }

    @Override
    public Object toJson() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("mimeType", mimeType);
        data.put("data", bytes == null ? "" : Base64.encodeNoNewline(bytes.toBytes()));
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("inlineData", data);
        return m;
    }
}
