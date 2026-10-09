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
package com.codename1.compat.jdk;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/// `java.net.URLEncoder`: the `application/x-www-form-urlencoded` form of a
/// string. Letters, digits and `.-*_` stay, a space is a plus, and every
/// other character is the bytes of its encoding as `%XX`.
public final class URLEncoder {
    private static final String HEX = "0123456789ABCDEF";

    private URLEncoder() {
    }

    static String charsetName(String enc) throws UnsupportedEncodingException {
        if (enc == null) {
            throw new NullPointerException("charsetName");
        }
        try {
            return JdkCharsets.name(JdkCharsets.forName(enc));
        } catch (IllegalArgumentException e) {
            throw new UnsupportedEncodingException(enc);
        }
    }

    private static boolean plain(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                || c == '.' || c == '-' || c == '*' || c == '_';
    }

    /// Encodes in UTF-8, where the JDK uses the platform's default.
    public static String encode(String s) {
        return encodeAs(s, "UTF-8");
    }

    public static String encode(String s, String enc) throws UnsupportedEncodingException {
        return encodeAs(s, charsetName(enc));
    }

    public static String encode(String s, Charset charset) {
        return encodeAs(s, JdkCharsets.name(charset));
    }

    private static String encodeAs(String s, String charset) {
        int n = s.length();
        StringBuilder out = new StringBuilder(n + 16);
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (plain(c)) {
                out.append(c);
                i++;
            } else if (c == ' ') {
                out.append('+');
                i++;
            } else {
                // The run of characters to escape is encoded as one piece,
                // so the two halves of a surrogate pair stay together.
                int start = i;
                while (i < n && !plain(s.charAt(i)) && s.charAt(i) != ' ') {
                    i++;
                }
                byte[] bytes;
                try {
                    bytes = s.substring(start, i).getBytes(charset);
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException(charset);
                }
                for (int b = 0; b < bytes.length; b++) {
                    out.append('%').append(HEX.charAt((bytes[b] >> 4) & 0xF)).append(HEX.charAt(bytes[b] & 0xF));
                }
            }
        }
        return out.toString();
    }
}
