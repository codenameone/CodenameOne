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
 * Decodes application/x-www-form-urlencoded text with the JDK's rules: a
 * {@code +} becomes a space, each run of {@code %XY} escapes is collected into
 * bytes and decoded in the named charset, and every other character is kept.
 * An escape that is cut short or is not hexadecimal is refused with an
 * {@link IllegalArgumentException}, as the JDK refuses it.
 *
 * Pure Java, for the reason {@link URLEncoder} gives.
 */
public class URLDecoder {
    private URLDecoder() {
    }

    /**
     * Decodes {@code s} as UTF-8.
     *
     * @deprecated name the charset with {@link #decode(String, String)}, as the
     *     JDK asks.
     */
    public static String decode(String s) {
        try {
            return decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Decodes {@code s}, reading escaped bytes in the charset named {@code enc}.
     *
     * @throws UnsupportedEncodingException when the charset is empty or not supported
     */
    public static String decode(String s, String enc) throws UnsupportedEncodingException {
        if (enc == null) {
            throw new NullPointerException("charsetName");
        }
        if (enc.length() == 0) {
            throw new UnsupportedEncodingException("URLDecoder: empty string enc parameter");
        }
        // Checked before anything is decoded, as the JDK does, so a charset
        // that is not supported is refused even for text with no escapes.
        return decodeWith(s, URLEncoder.charset(enc));
    }

    /**
     * Decodes {@code s}, reading escaped bytes in {@code charset}.
     */
    public static String decode(String s, Charset charset) {
        if (charset == null) {
            throw new NullPointerException("Charset");
        }
        try {
            return decodeWith(s, URLEncoder.charset(charset.name()));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    private static String decodeWith(String s, String enc) throws UnsupportedEncodingException {
        int length = s.length();
        StringBuilder out = null;
        int i = 0;
        while (i < length) {
            char c = s.charAt(i);
            if (c != '+' && c != '%') {
                if (out != null) {
                    out.append(c);
                }
                i++;
                continue;
            }
            if (out == null) {
                out = new StringBuilder(length);
                out.append(s, 0, i);
            }
            if (c == '+') {
                out.append(' ');
                i++;
                continue;
            }
            // A run of escapes is one byte sequence: a multi-byte character is
            // spread over several of them and only decodes as a whole.
            byte[] bytes = new byte[(length - i) / 3];
            int count = 0;
            while (i + 2 < length && s.charAt(i) == '%') {
                int high = Character.digit(s.charAt(i + 1), 16);
                int low = Character.digit(s.charAt(i + 2), 16);
                if (high < 0 || low < 0) {
                    throw new IllegalArgumentException(
                            "URLDecoder: Illegal hex characters in escape (%) pattern - "
                            + s.substring(i + 1, i + 3));
                }
                bytes[count++] = (byte) ((high << 4) | low);
                i += 3;
            }
            if (i < length && s.charAt(i) == '%') {
                throw new IllegalArgumentException(
                        "URLDecoder: Incomplete trailing escape (%) pattern");
            }
            text(out, bytes, count, enc);
        }
        return out == null ? s : out.toString();
    }

    /**
     * Appends {@code count} bytes decoded in {@code enc}, a canonical name from
     * {@link URLEncoder#charset(String)}. The two single byte charsets are
     * decoded here, so they answer the same on every runtime: ISO-8859-1 maps
     * each byte to the character of that number, and US-ASCII maps a byte
     * above 0x7F to U+FFFD, as the JDK's decoder replaces it.
     */
    private static void text(StringBuilder out, byte[] bytes, int count, String enc)
            throws UnsupportedEncodingException {
        if ("UTF-8".equals(enc)) {
            out.append(new String(bytes, 0, count, enc));
            return;
        }
        boolean ascii = "US-ASCII".equals(enc);
        for (int i = 0; i < count; i++) {
            int b = bytes[i] & 0xff;
            out.append(ascii && b > 0x7F ? (char) 0xFFFD : (char) b);
        }
    }
}
