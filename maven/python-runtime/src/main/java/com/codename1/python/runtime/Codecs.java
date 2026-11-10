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
package com.codename1.python.runtime;

import java.io.ByteArrayOutputStream;

/// `str.encode` / `bytes.decode` for the encodings Codename One supports on
/// every platform: UTF-8, ASCII and Latin-1. Any other name is a
/// LookupError rather than a silent fallback, so a program that needs
/// another charset fails where it asks for it.
final class Codecs {
    private Codecs() {
    }

    /// A normalised codec name: `utf-8`, `ascii` or `latin-1`.
    static String normalize(String encoding) {
        String e = TypeMethods.asciiLower(encoding).replace('_', '-');
        if ("utf-8".equals(e) || "utf8".equals(e) || "u8".equals(e) || "utf".equals(e)) {
            return "utf-8";
        }
        if ("ascii".equals(e) || "us-ascii".equals(e) || "646".equals(e)) {
            return "ascii";
        }
        if ("latin-1".equals(e) || "latin1".equals(e) || "iso-8859-1".equals(e) || "iso8859-1".equals(e)
                || "l1".equals(e) || "8859".equals(e) || "cp819".equals(e)) {
            return "latin-1";
        }
        throw Py.error(Py.LOOKUP_ERROR, "unknown encoding: " + encoding);
    }

    static byte[] encode(String s, String encoding, String errors) {
        String enc = normalize(encoding);
        if ("utf-8".equals(enc)) {
            return Utf8.encode(s);
        }
        int limit = "ascii".equals(enc) ? 0x80 : 0x100;
        ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());
        int i = 0;
        while (i < s.length()) {
            int cp = Character.codePointAt(s, i);
            int pos = i;
            i += Character.charCount(cp);
            if (cp < limit) {
                out.write(cp);
            } else if ("ignore".equals(errors)) {
                continue;
            } else if ("replace".equals(errors)) {
                out.write('?');
            } else {
                throw Py.error(Py.UNICODE_ENCODE_ERROR, "'" + (limit == 0x80 ? "ascii" : "latin-1")
                        + "' codec can't encode character '" + escape(cp) + "' in position "
                        + PyStr.cpIndex(s, pos) + ": ordinal not in range(" + limit + ")");
            }
        }
        return out.toByteArray();
    }

    static String decode(byte[] b, String encoding, String errors) {
        String enc = normalize(encoding);
        if ("utf-8".equals(enc)) {
            return Utf8.decode(b, errors);
        }
        boolean ascii = "ascii".equals(enc);
        StringBuilder sb = new StringBuilder(b.length);
        for (int i = 0; i < b.length; i++) {
            int c = b[i] & 0xff;
            if (ascii && c >= 0x80) {
                if ("ignore".equals(errors)) {
                    continue;
                }
                if ("replace".equals(errors)) {
                    sb.append('\uFFFD');
                    continue;
                }
                throw Py.error(Py.UNICODE_DECODE_ERROR, "'ascii' codec can't decode byte 0x" + Integer.toHexString(c)
                        + " in position " + i + ": ordinal not in range(128)");
            }
            sb.append((char) c);
        }
        return sb.toString();
    }

    /// `repr`'s escape of a code point: backslash-x for Latin-1, backslash-u
    /// below 0x10000, backslash-U above.
    private static String escape(int cp) {
        if (cp < 0x100) {
            String h = Integer.toHexString(cp);
            return "\\x" + (h.length() < 2 ? "0" + h : h);
        }
        if (cp < 0x10000) {
            return "\\u" + hex4(cp);
        }
        String h = Integer.toHexString(cp);
        return "\\U" + "00000000".substring(h.length()) + h;
    }

    private static String hex4(int cp) {
        String h = Integer.toHexString(cp);
        StringBuilder sb = new StringBuilder();
        for (int k = h.length(); k < 4; k++) {
            sb.append('0');
        }
        return sb.append(h).toString();
    }
}
