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
package com.codename1.impl.xml;

import java.io.IOException;
import java.io.OutputStream;

/// Writes XML as UTF-8 bytes, straight into the entry of an archive.
public final class XmlOut {
    private final OutputStream out;
    private final byte[] buffer = new byte[8192];
    private int size;

    public XmlOut(OutputStream out) {
        this.out = out;
    }

    private void put(int b) throws IOException {
        if (size == buffer.length) {
            out.write(buffer, 0, size);
            size = 0;
        }
        buffer[size++] = (byte) b;
    }

    private void putChar(String s, int i) throws IOException {
        char c = s.charAt(i);
        if (c < 0x80) {
            put(c);
        } else if (c < 0x800) {
            put(0xc0 | c >> 6);
            put(0x80 | c & 0x3f);
        } else if (c >= 0xd800 && c <= 0xdbff && i + 1 < s.length() && s.charAt(i + 1) >= 0xdc00
                && s.charAt(i + 1) <= 0xdfff) {
            int code = 0x10000 + (c - 0xd800 << 10) + (s.charAt(i + 1) - 0xdc00);
            put(0xf0 | code >> 18);
            put(0x80 | code >> 12 & 0x3f);
            put(0x80 | code >> 6 & 0x3f);
            put(0x80 | code & 0x3f);
        } else if (c >= 0xd800 && c <= 0xdfff) {
            // half of a pair by itself is not a character
            put('?');
        } else {
            put(0xe0 | c >> 12);
            put(0x80 | c >> 6 & 0x3f);
            put(0x80 | c & 0x3f);
        }
    }

    private static boolean isLowSurrogateOfPair(String s, int i) {
        return i > 0 && s.charAt(i) >= 0xdc00 && s.charAt(i) <= 0xdfff && s.charAt(i - 1) >= 0xd800
                && s.charAt(i - 1) <= 0xdbff;
    }

    /// Writes markup as it is.
    public XmlOut raw(String s) throws IOException {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            if (!isLowSurrogateOfPair(s, i)) {
                putChar(s, i);
            }
        }
        return this;
    }

    public XmlOut raw(int value) throws IOException {
        return raw(Integer.toString(value));
    }

    /// Writes text or an attribute value, with what XML reserves escaped. A character XML
    /// cannot hold at all -- most of the control characters -- is written `_x0008_`, the
    /// convention of the Excel format, when `excel` is set and is left out otherwise.
    public XmlOut text(String s, boolean excel) throws IOException {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    raw("&amp;");
                    break;
                case '<':
                    raw("&lt;");
                    break;
                case '>':
                    raw("&gt;");
                    break;
                case '"':
                    raw("&quot;");
                    break;
                case '\n':
                    raw("&#10;");
                    break;
                case '\t':
                    raw("&#9;");
                    break;
                default:
                    if (c < 0x20 || c == 0xfffe || c == 0xffff) {
                        if (excel) {
                            raw("_x");
                            String hex = Integer.toHexString(c);
                            for (int k = hex.length(); k < 4; k++) {
                                put('0');
                            }
                            raw(hex);
                            put('_');
                        }
                    } else if (excel && c == '_' && isEscape(s, i)) {
                        // text that happens to look like an escape is escaped itself
                        raw("_x005F_");
                    } else if (!isLowSurrogateOfPair(s, i)) {
                        putChar(s, i);
                    }
                    break;
            }
        }
        return this;
    }

    /// Whether text at a position reads as `_xHHHH_`.
    public static boolean isEscape(String s, int i) {
        if (i + 7 > s.length() || s.charAt(i) != '_' || s.charAt(i + 1) != 'x' || s.charAt(i + 6) != '_') {
            return false;
        }
        for (int k = i + 2; k < i + 6; k++) {
            char c = s.charAt(k);
            if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F')) {
                return false;
            }
        }
        return true;
    }

    /// Writes ` name="value"`.
    public XmlOut attr(String name, String value) throws IOException {
        put(' ');
        raw(name);
        raw("=\"");
        text(value, false);
        put('"');
        return this;
    }

    public XmlOut attr(String name, int value) throws IOException {
        put(' ');
        raw(name);
        raw("=\"");
        raw(Integer.toString(value));
        put('"');
        return this;
    }

    public void flush() throws IOException {
        if (size > 0) {
            out.write(buffer, 0, size);
            size = 0;
        }
    }
}
