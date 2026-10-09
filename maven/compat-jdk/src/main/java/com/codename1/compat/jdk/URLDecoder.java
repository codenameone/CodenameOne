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

/// `java.net.URLDecoder`: a string out of its
/// `application/x-www-form-urlencoded` form. A plus is a space and a run of
/// `%XX` is the bytes of some characters.
public final class URLDecoder {

    private URLDecoder() {
    }

    /// Decodes as UTF-8, where the JDK uses the platform's default.
    public static String decode(String s) {
        return decodeAs(s, "UTF-8");
    }

    public static String decode(String s, String enc) throws UnsupportedEncodingException {
        if (enc == null) {
            throw new NullPointerException("charsetName");
        }
        if (enc.length() == 0) {
            throw new UnsupportedEncodingException("URLDecoder: empty string enc parameter");
        }
        return decodeAs(s, URLEncoder.charsetName(enc));
    }

    public static String decode(String s, Charset charset) {
        return decodeAs(s, JdkCharsets.name(charset));
    }

    private static int hex(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    private static String decodeAs(String s, String charset) {
        int n = s.length();
        StringBuilder out = new StringBuilder(n);
        byte[] bytes = null;
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (c == '+') {
                out.append(' ');
                i++;
            } else if (c == '%') {
                if (bytes == null) {
                    bytes = new byte[(n - i) / 3];
                }
                int count = 0;
                while (i + 2 < n && c == '%') {
                    int high = hex(s.charAt(i + 1));
                    int low = hex(s.charAt(i + 2));
                    if (high < 0 || low < 0) {
                        throw new IllegalArgumentException(
                                "URLDecoder: Illegal hex characters in escape (%) pattern - Error at index "
                                + (high < 0 ? 0 : 1) + " in: \"" + s.substring(i + 1, i + 3) + "\"");
                    }
                    bytes[count++] = (byte) (high * 16 + low);
                    i += 3;
                    if (i < n) {
                        c = s.charAt(i);
                    }
                }
                if (i < n && c == '%') {
                    throw new IllegalArgumentException("URLDecoder: Incomplete trailing escape (%) pattern");
                }
                try {
                    out.append(new String(bytes, 0, count, charset));
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException(charset);
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }
}
