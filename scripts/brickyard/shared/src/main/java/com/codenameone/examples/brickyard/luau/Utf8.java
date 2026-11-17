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
package com.codenameone.examples.brickyard.luau;

/// Luau strings are byte strings. Here each byte is one Java char in the
/// range 0-255, so UTF-8 text is held as its encoded bytes.
public final class Utf8 {
    private Utf8() {
    }

    static void encode(StringBuilder b, int cp) {
        if (cp < 0x80) {
            b.append((char) cp);
        } else if (cp < 0x800) {
            b.append((char) (0xC0 | (cp >> 6)));
            b.append((char) (0x80 | (cp & 0x3F)));
        } else if (cp < 0x10000) {
            b.append((char) (0xE0 | (cp >> 12)));
            b.append((char) (0x80 | ((cp >> 6) & 0x3F)));
            b.append((char) (0x80 | (cp & 0x3F)));
        } else {
            b.append((char) (0xF0 | (cp >> 18)));
            b.append((char) (0x80 | ((cp >> 12) & 0x3F)));
            b.append((char) (0x80 | ((cp >> 6) & 0x3F)));
            b.append((char) (0x80 | (cp & 0x3F)));
        }
    }

    /// Host text (UTF-16) to a Luau byte string.
    public static String fromJava(String s) {
        boolean ascii = true;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) >= 0x80) {
                ascii = false;
                break;
            }
        }
        if (ascii) {
            return s;
        }
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            if (c >= 0xD800 && c < 0xDC00 && i + 1 < s.length()) {
                int lo = s.charAt(i + 1);
                if (lo >= 0xDC00 && lo < 0xE000) {
                    c = 0x10000 + ((c - 0xD800) << 10) + (lo - 0xDC00);
                    i++;
                }
            }
            encode(b, c);
        }
        return b.toString();
    }

    /// A Luau byte string to host text; malformed bytes become U+FFFD.
    public static String toJava(String s) {
        boolean ascii = true;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) >= 0x80) {
                ascii = false;
                break;
            }
        }
        if (ascii) {
            return s;
        }
        StringBuilder b = new StringBuilder(s.length());
        int i = 0;
        int[] out = new int[1];
        while (i < s.length()) {
            int len = decode(s, i, out);
            int cp = len < 0 ? 0xFFFD : out[0];
            i += len < 0 ? 1 : len;
            if (cp >= 0x10000) {
                cp -= 0x10000;
                b.append((char) (0xD800 + (cp >> 10)));
                b.append((char) (0xDC00 + (cp & 0x3FF)));
            } else {
                b.append((char) cp);
            }
        }
        return b.toString();
    }

    /// Decodes one code point at `i` into `out[0]`; returns its byte length
    /// or -1 when the sequence is malformed.
    static int decode(String s, int i, int[] out) {
        int c = s.charAt(i);
        int len;
        int cp;
        if (c < 0x80) {
            out[0] = c;
            return 1;
        } else if (c >= 0xC2 && c < 0xE0) {
            len = 2;
            cp = c & 0x1F;
        } else if (c >= 0xE0 && c < 0xF0) {
            len = 3;
            cp = c & 0x0F;
        } else if (c >= 0xF0 && c < 0xF5) {
            len = 4;
            cp = c & 0x07;
        } else {
            return -1;
        }
        if (i + len > s.length()) {
            return -1;
        }
        for (int k = 1; k < len; k++) {
            int cc = s.charAt(i + k);
            if ((cc & 0xC0) != 0x80) {
                return -1;
            }
            cp = (cp << 6) | (cc & 0x3F);
        }
        if (len == 3 && cp < 0x800 || len == 4 && (cp < 0x10000 || cp > 0x10FFFF)
                || cp >= 0xD800 && cp < 0xE000) {
            return -1;
        }
        out[0] = cp;
        return len;
    }
}
