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
package java.net;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Encodes text in the application/x-www-form-urlencoded format, with the JDK's
 * rules: the letters, the digits and the four characters {@code . - * _} are
 * kept, a space becomes {@code +}, and every other character is converted to
 * bytes in the named charset and each byte written as {@code %XY} in upper case
 * hexadecimal.
 *
 * Pure Java on purpose. A server written against the JDK calls this class, and
 * the compiled backend has to answer the same bytes a JVM would, so nothing
 * here may depend on a native.
 */
public class URLEncoder {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private URLEncoder() {
    }

    /**
     * Encodes {@code s} as UTF-8, which is the platform charset on every
     * runtime this library runs on.
     *
     * @deprecated name the charset with {@link #encode(String, String)}, as the
     *     JDK asks.
     */
    public static String encode(String s) {
        try {
            return encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Encodes {@code s} with the bytes of the charset named {@code enc}.
     *
     * Charset support is platform specific; this class library is not the
     * JDK. Only UTF-8 and ISO-8859-1 are supported everywhere. Any other name,
     * including one the platform does not know, is whatever the platform's
     * conversion makes of it: it may convert, fall back or throw, and text
     * that needs no conversion is returned unchecked.
     *
     * @throws UnsupportedEncodingException when the charset is empty, or when the
     *     platform refuses it
     */
    public static String encode(String s, String enc) throws UnsupportedEncodingException {
        if (enc == null) {
            throw new NullPointerException("charsetName");
        }
        if (enc.length() == 0) {
            throw new UnsupportedEncodingException(enc);
        }
        return encodeWith(s, enc);
    }

    /**
     * Encodes {@code s} with the bytes of {@code charset}.
     */
    public static String encode(String s, Charset charset) {
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        try {
            return encodeWith(s, charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    private static String encodeWith(String s, String enc) throws UnsupportedEncodingException {
        int length = s.length();
        StringBuilder out = null;
        int i = 0;
        while (i < length) {
            char c = s.charAt(i);
            if (keep(c)) {
                if (out != null) {
                    out.append(c);
                }
                i++;
                continue;
            }
            if (out == null) {
                out = new StringBuilder(length + 16);
                out.append(s, 0, i);
            }
            if (c == ' ') {
                out.append('+');
                i++;
                continue;
            }
            // The whole run of characters that need encoding is converted at
            // once, as the JDK does, so a surrogate pair reaches the charset
            // together and becomes the four bytes of one code point.
            int start = i;
            while (i < length && !keep(s.charAt(i)) && s.charAt(i) != ' ') {
                i++;
            }
            byte[] bytes = bytes(s.substring(start, i), enc);
            for (int b = 0; b < bytes.length; b++) {
                int v = bytes[b] & 0xff;
                out.append('%').append(HEX[v >> 4]).append(HEX[v & 0xf]);
            }
        }
        return out == null ? s : out.toString();
    }

    private static boolean keep(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                || c == '.' || c == '-' || c == '*' || c == '_';
    }

    /**
     * UTF-8 is converted here rather than by the runtime's charset support, so
     * that a lone surrogate is written the way the JDK writes it -- as the
     * replacement byte {@code ?} -- whatever the native conversion would do.
     */
    private static byte[] bytes(String run, String enc) throws UnsupportedEncodingException {
        if (!"UTF-8".equalsIgnoreCase(enc) && !"UTF8".equalsIgnoreCase(enc)) {
            return run.getBytes(enc);
        }
        // At most three bytes per char: a pair is two chars and four bytes.
        byte[] out = new byte[run.length() * 3];
        int at = 0;
        int n = run.length();
        for (int i = 0; i < n; i++) {
            int c = run.charAt(i);
            if (c >= 0xD800 && c <= 0xDBFF && i + 1 < n
                    && run.charAt(i + 1) >= 0xDC00 && run.charAt(i + 1) <= 0xDFFF) {
                int cp = 0x10000 + ((c - 0xD800) << 10) + (run.charAt(i + 1) - 0xDC00);
                i++;
                out[at++] = (byte) (0xF0 | (cp >> 18));
                out[at++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
                out[at++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
                out[at++] = (byte) (0x80 | (cp & 0x3F));
            } else if (c >= 0xD800 && c <= 0xDFFF) {
                out[at++] = (byte) ('?');
            } else if (c < 0x80) {
                out[at++] = (byte) (c);
            } else if (c < 0x800) {
                out[at++] = (byte) (0xC0 | (c >> 6));
                out[at++] = (byte) (0x80 | (c & 0x3F));
            } else {
                out[at++] = (byte) (0xE0 | (c >> 12));
                out[at++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                out[at++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        byte[] result = new byte[at];
        System.arraycopy(out, 0, result, 0, at);
        return result;
    }
}
