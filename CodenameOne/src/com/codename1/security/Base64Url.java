/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security;

/// URL-safe Base64 (RFC 4648 sec5) with the trailing `=` padding stripped -- the
/// encoding used by JWTs and most modern web token formats.
///
/// This is a thin wrapper around [com.codename1.util.Base64] that swaps `+/`
/// for `-_` and drops padding.
public final class Base64Url {
    private Base64Url() {}

    /// Encodes the bytes as a URL-safe Base64 string with no padding.
    public static String encode(byte[] data) {
        String s = com.codename1.util.Base64.encodeNoNewline(data);
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '=') {
                continue;
            }
            if (c == '+') {
                c = '-';
            } else if (c == '/') {
                c = '_';
            }
            b.append(c);
        }
        return b.toString();
    }

    /// Decodes a URL-safe Base64 string. Padding is optional.
    public static byte[] decode(String s) {
        if (s == null) {
            return new byte[0];
        }
        StringBuilder b = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '-') {
                c = '+';
            } else if (c == '_') {
                c = '/';
            }
            b.append(c);
        }
        int pad = (4 - (b.length() & 3)) & 3;
        for (int i = 0; i < pad; i++) {
            b.append('=');
        }
        try {
            return com.codename1.util.Base64.decode(b.toString().getBytes("UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            throw new CryptoException("UTF-8 not supported", e);
        }
    }
}
