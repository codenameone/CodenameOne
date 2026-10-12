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
package com.codename1.designer.css.raster;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/// A `data:` address taken apart: what it says it holds, and the bytes.
///
/// The bytes are base64 when the address says so. Otherwise they are
/// written out, with any of them as `%` and two hex digits: an escape is
/// the byte it names, `%89` the byte 0x89 of a picture, and not a
/// character to be read and written back as some other bytes. Anything not
/// escaped is text, and stands for the text's own bytes.
public final class DataUrl {
    private final String type;
    private final byte[] bytes;

    private DataUrl(String type, byte[] bytes) {
        this.type = type;
        this.bytes = bytes;
    }

    /// Whether `url` is a `data:` address at all.
    public static boolean is(String url) {
        return url != null && url.regionMatches(true, 0, "data:", 0, 5);
    }

    /// Takes a `data:` address apart, or answers null for one that is not
    /// one or has no comma between what it is and what it holds.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when it says base64 and is not
    public static DataUrl parse(String url) {
        if (!is(url)) {
            return null;
        }
        int comma = url.indexOf(',');
        if (comma < 0) {
            return null;
        }
        String kind = url.substring(5, comma);
        String payload = url.substring(comma + 1);
        if (kind.length() >= 7 && kind.regionMatches(true, kind.length() - 7, ";base64", 0, 7)) {
            return new DataUrl(kind, java.util.Base64.getMimeDecoder().decode(payload));
        }
        ByteArrayOutputStream octets = new ByteArrayOutputStream();
        for (int i = 0; i < payload.length(); i++) {
            char c = payload.charAt(i);
            int hi = c == '%' && i + 2 < payload.length() ? Character.digit(payload.charAt(i + 1), 16) : -1;
            int lo = hi >= 0 ? Character.digit(payload.charAt(i + 2), 16) : -1;
            if (hi >= 0 && lo >= 0) {
                octets.write((hi << 4) | lo);
                i += 2;
            } else {
                byte[] text = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                octets.write(text, 0, text.length);
            }
        }
        return new DataUrl(kind, octets.toByteArray());
    }

    /// Whether it says it holds an SVG.
    public boolean isSvg() {
        return type.regionMatches(true, 0, "image/svg", 0, 9);
    }

    /// What it holds.
    public byte[] getBytes() {
        return bytes.clone();
    }
}
