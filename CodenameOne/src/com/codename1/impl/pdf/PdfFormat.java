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

/// Lexical formatting of PDF tokens. Everything here is ASCII by
/// specification, so it is written by hand: no `String.format`, no locale,
/// and no `Double.toString`, whose exponent form (`1.0E-5`) is not a PDF
/// number.
@com.codename1.impl.SharedWithBackend
public final class PdfFormat {
    private static final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private PdfFormat() {
    }

    /// Appends `value` as a PDF real with at most four decimals and no
    /// trailing zeros. NaN and infinities, which PDF cannot express, are
    /// written as `0`.
    public static void number(StringBuilder out, double value) {
        if (value != value || value > 3.0e38 || value < -3.0e38) {
            out.append('0');
            return;
        }
        long scaled = Math.round(value * 10000.0);
        if (scaled < 0) {
            out.append('-');
            scaled = -scaled;
        }
        long whole = scaled / 10000;
        long frac = scaled % 10000;
        out.append(whole);
        if (frac != 0) {
            out.append('.');
            long div = 1000;
            while (frac != 0) {
                long digit = frac / div;
                out.append((char) ('0' + digit));
                frac -= digit * div;
                div /= 10;
            }
        }
    }

    /// Returns `value` formatted as by [#number(StringBuilder, double)].
    public static String number(double value) {
        StringBuilder sb = new StringBuilder();
        number(sb, value);
        return sb.toString();
    }

    /// Appends a name object (`/Name`), escaping every byte outside the
    /// regular-character set as `#xx`.
    public static void name(StringBuilder out, String name) {
        out.append('/');
        int len = name.length();
        for (int i = 0; i < len; i++) {
            char c = name.charAt(i);
            if (c > 0x20 && c < 0x7f && c != '#' && !isDelimiter(c)) {
                out.append(c);
            } else {
                int b = c & 0xff;
                out.append('#').append(HEX[b >> 4]).append(HEX[b & 0xf]);
            }
        }
    }

    /// Appends `bytes` as a hexadecimal string, `<...>`.
    public static void hex(StringBuilder out, byte[] bytes) {
        out.append('<');
        for (int i = 0; i < bytes.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            int b = bytes[i] & 0xff;
            out.append(HEX[b >> 4]).append(HEX[b & 0xf]);
        }
        out.append('>');
    }

    /// Appends a text string (outside a content stream): pure ASCII
    /// strings are written as a literal, anything else as UTF-16BE with a
    /// byte order mark, which every reader accepts for text strings.
    public static void textString(StringBuilder out, String text) {
        boolean ascii = true;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            if (text.charAt(i) > 0x7e) {
                ascii = false;
                break;
            }
        }
        if (ascii) {
            literal(out, text);
            return;
        }
        byte[] b = new byte[2 + len * 2];
        b[0] = (byte) 0xfe;
        b[1] = (byte) 0xff;
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            b[2 + i * 2] = (byte) (c >> 8);
            b[3 + i * 2] = (byte) c;
        }
        hex(out, b);
    }

    /// Appends a literal string `(...)` whose characters are single bytes
    /// (each char is taken modulo 256), escaping the delimiters.
    public static void literal(StringBuilder out, String bytes) {
        out.append('(');
        int len = bytes.length();
        for (int i = 0; i < len; i++) {
            char c = (char) (bytes.charAt(i) & 0xff);
            switch (c) {
                case '(':
                case ')':
                case '\\':
                    out.append('\\').append(c);
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                default:
                    if (c < 0x20 || c > 0x7e) {
                        out.append('\\');
                        out.append((char) ('0' + ((c >> 6) & 7)));
                        out.append((char) ('0' + ((c >> 3) & 7)));
                        out.append((char) ('0' + (c & 7)));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append(')');
    }

    /// True for the PDF delimiter characters.
    public static boolean isDelimiter(int c) {
        return c == '(' || c == ')' || c == '<' || c == '>' || c == '['
                || c == ']' || c == '{' || c == '}' || c == '/' || c == '%';
    }

    /// True for the PDF white-space characters.
    public static boolean isWhitespace(int c) {
        return c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == '\f' || c == 0;
    }

    /// Returns the low byte of every char of an ASCII string.
    public static byte[] ascii(String s) {
        int len = s.length();
        byte[] b = new byte[len];
        for (int i = 0; i < len; i++) {
            b[i] = (byte) s.charAt(i);
        }
        return b;
    }
}
