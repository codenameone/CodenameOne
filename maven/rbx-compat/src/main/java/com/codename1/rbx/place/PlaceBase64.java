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
package com.codename1.rbx.place;

/// The base64 codec place files use for binary properties and shared strings.
/// The engine keeps its own rather than core's `Base64` so that it compiles
/// against nothing of the client framework and runs on a server.
final class PlaceBase64 {
    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    private PlaceBase64() {
    }

    static String encode(byte[] in) {
        StringBuilder sb = new StringBuilder((in.length + 2) / 3 * 4);
        int i = 0;
        while (i + 2 < in.length) {
            int v = (in[i] & 0xff) << 16 | (in[i + 1] & 0xff) << 8 | in[i + 2] & 0xff;
            sb.append(ALPHABET[v >> 18]).append(ALPHABET[v >> 12 & 63])
                    .append(ALPHABET[v >> 6 & 63]).append(ALPHABET[v & 63]);
            i += 3;
        }
        int rest = in.length - i;
        if (rest == 1) {
            int v = (in[i] & 0xff) << 16;
            sb.append(ALPHABET[v >> 18]).append(ALPHABET[v >> 12 & 63]).append("==");
        } else if (rest == 2) {
            int v = (in[i] & 0xff) << 16 | (in[i + 1] & 0xff) << 8;
            sb.append(ALPHABET[v >> 18]).append(ALPHABET[v >> 12 & 63])
                    .append(ALPHABET[v >> 6 & 63]).append('=');
        }
        return sb.toString();
    }

    /// Decodes, skipping whitespace and anything outside the alphabet, and
    /// stopping at the first padding character.
    static byte[] decode(String s) {
        byte[] out = new byte[s.length() * 3 / 4 + 3];
        int n = 0;
        int acc = 0;
        int bits = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '=') {
                break;
            }
            int d = value(c);
            if (d < 0) {
                continue;
            }
            acc = acc << 6 | d;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out[n++] = (byte) (acc >> bits);
            }
        }
        byte[] r = new byte[n];
        System.arraycopy(out, 0, r, 0, n);
        return r;
    }

    private static int value(char c) {
        if (c >= 'A' && c <= 'Z') {
            return c - 'A';
        }
        if (c >= 'a' && c <= 'z') {
            return c - 'a' + 26;
        }
        if (c >= '0' && c <= '9') {
            return c - '0' + 52;
        }
        if (c == '+') {
            return 62;
        }
        return c == '/' ? 63 : -1;
    }
}
