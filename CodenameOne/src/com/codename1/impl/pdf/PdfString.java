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
package com.codename1.impl.pdf;

/// A parsed string object: its bytes, literal and hexadecimal strings
/// alike.
@com.codename1.impl.SharedWithBackend
public final class PdfString {
    /// The bytes of the string, after decryption.
    public byte[] bytes;

    /// A string of `bytes`.
    public PdfString(byte[] bytes) {
        this.bytes = bytes;
    }

    /// Decodes the string as a text string: UTF-16BE or UTF-8 when it
    /// starts with a byte order mark, PDFDocEncoding otherwise (taken as
    /// Latin-1, which it matches for every printable character but a few
    /// typographic ones in 0x18-0x1F and 0x80-0x9F).
    public String toText() {
        byte[] b = bytes;
        StringBuilder sb = new StringBuilder(b.length);
        if (b.length >= 2 && (b[0] & 0xff) == 0xfe && (b[1] & 0xff) == 0xff) {
            for (int i = 2; i + 1 < b.length; i += 2) {
                sb.append((char) (((b[i] & 0xff) << 8) | (b[i + 1] & 0xff)));
            }
            return sb.toString();
        }
        if (b.length >= 3 && (b[0] & 0xff) == 0xef && (b[1] & 0xff) == 0xbb && (b[2] & 0xff) == 0xbf) {
            return utf8(b, 3);
        }
        for (int i = 0; i < b.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            int c = b[i] & 0xff;
            if (c >= 0x80 && c <= 0x9f) {
                c = pdfDoc(c);
            }
            sb.append((char) c);
        }
        return sb.toString();
    }

    private static final char[] PDF_DOC_80 = {
        0x2022, 0x2020, 0x2021, 0x2026, 0x2014, 0x2013, 0x0192, 0x2044,
        0x2039, 0x203a, 0x2212, 0x2030, 0x201e, 0x201c, 0x201d, 0x2018,
        0x2019, 0x201a, 0x2122, 0xfb01, 0xfb02, 0x0141, 0x0152, 0x0160,
        0x0178, 0x017d, 0x0131, 0x0142, 0x0153, 0x0161, 0x017e, 0xfffd};

    private static int pdfDoc(int c) {
        return PDF_DOC_80[c - 0x80];
    }

    static String utf8(byte[] b, int start) {
        StringBuilder sb = new StringBuilder(b.length);
        int i = start;
        while (i < b.length) {
            int c = b[i] & 0xff;
            if (c < 0x80) {
                sb.append((char) c);
                i++;
            } else if (c >= 0xc0 && c < 0xe0 && i + 1 < b.length) {
                sb.append((char) (((c & 0x1f) << 6) | (b[i + 1] & 0x3f)));
                i += 2;
            } else if (c >= 0xe0 && c < 0xf0 && i + 2 < b.length) {
                sb.append((char) (((c & 0x0f) << 12) | ((b[i + 1] & 0x3f) << 6) | (b[i + 2] & 0x3f)));
                i += 3;
            } else if (c >= 0xf0 && i + 3 < b.length) {
                int cp = ((c & 0x07) << 18) | ((b[i + 1] & 0x3f) << 12) | ((b[i + 2] & 0x3f) << 6)
                        | (b[i + 3] & 0x3f);
                cp -= 0x10000;
                sb.append((char) (0xd800 + (cp >> 10))).append((char) (0xdc00 + (cp & 0x3ff)));
                i += 4;
            } else {
                sb.append((char) 0xfffd);
                i++;
            }
        }
        return sb.toString();
    }
}
