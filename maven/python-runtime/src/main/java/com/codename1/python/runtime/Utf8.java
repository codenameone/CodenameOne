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

/// UTF-8 coding with Python's strict error behaviour.
final class Utf8 {

    private Utf8() {
    }

    static byte[] encode(String s) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(s.length());
        for (int i = 0; i < s.length(); i++) {
            int c = s.codePointAt(i);
            if (c > 0xffff) {
                i++;
            }
            if (c < 0x80) {
                out.write(c);
            } else if (c < 0x800) {
                out.write(0xc0 | c >> 6);
                out.write(0x80 | c & 0x3f);
            } else if (c < 0x10000) {
                out.write(0xe0 | c >> 12);
                out.write(0x80 | c >> 6 & 0x3f);
                out.write(0x80 | c & 0x3f);
            } else {
                out.write(0xf0 | c >> 18);
                out.write(0x80 | c >> 12 & 0x3f);
                out.write(0x80 | c >> 6 & 0x3f);
                out.write(0x80 | c & 0x3f);
            }
        }
        return out.toByteArray();
    }

    static String decode(byte[] b) {
        return decode(b, "strict");
    }

    /// Decodes UTF-8 under a Python error handler: `strict` raises,
    /// `replace` substitutes U+FFFD for each bad byte, `ignore` drops it.
    static String decode(byte[] b, String errors) {
        boolean strict = !"replace".equals(errors) && !"ignore".equals(errors);
        boolean replace = "replace".equals(errors);
        StringBuilder sb = new StringBuilder(b.length);
        int i = 0;
        while (i < b.length) {
            int c = b[i] & 0xff;
            int need;
            int cp;
            if (c < 0x80) {
                sb.append((char) c);
                i++;
                continue;
            } else if (c >= 0xc2 && c < 0xe0) {
                need = 1;
                cp = c & 0x1f;
            } else if (c >= 0xe0 && c < 0xf0) {
                need = 2;
                cp = c & 0x0f;
            } else if (c >= 0xf0 && c < 0xf5) {
                need = 3;
                cp = c & 0x07;
            } else {
                if (strict) {
                    throw decodeError(c, i);
                }
                if (replace) {
                    sb.append('\ufffd');
                }
                i++;
                continue;
            }
            boolean bad = false;
            if (i + need >= b.length) {
                if (strict) {
                    throw Py.error(Py.UNICODE_DECODE_ERROR, "'utf-8' codec can't decode bytes in position " + i
                            + "-" + (b.length - 1) + ": unexpected end of data");
                }
                bad = true;
            } else {
                for (int k = 1; k <= need; k++) {
                    int cc = b[i + k] & 0xff;
                    if ((cc & 0xc0) != 0x80) {
                        if (strict) {
                            throw decodeError(c, i);
                        }
                        bad = true;
                        break;
                    }
                    cp = cp << 6 | cc & 0x3f;
                }
            }
            if (bad) {
                if (replace) {
                    sb.append('\ufffd');
                }
                i++;
                continue;
            }
            sb.appendCodePoint(cp);
            i += need + 1;
        }
        return sb.toString();
    }

    private static PyException decodeError(int c, int pos) {
        return Py.error(Py.UNICODE_DECODE_ERROR, "'utf-8' codec can't decode byte 0x" + Integer.toHexString(c)
                + " in position " + pos + ": invalid start byte");
    }
}
