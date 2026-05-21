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

/// Base32 encoder/decoder per RFC 4648. Mostly useful for OTP shared secrets,
/// which are conventionally distributed as Base32 strings (the format embedded
/// in QR codes by authenticator apps).
///
/// #### Example
///
/// ```java
/// byte[] secret = Base32.decode("JBSWY3DPEHPK3PXP");
/// String enc    = Base32.encode(secret);
/// ```
public final class Base32 {
    private Base32() {}

    private static final char[] ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final int[] DECODE = new int[128];
    static {
        for (int i = 0; i < DECODE.length; i++) DECODE[i] = -1;
        for (int i = 0; i < ALPHABET.length; i++) DECODE[ALPHABET[i]] = i;
        // common lowercase variant
        for (int i = 0; i < ALPHABET.length; i++) {
            char lc = (char) (ALPHABET[i] | 0x20);
            if (lc != ALPHABET[i]) DECODE[lc] = i;
        }
    }

    /// Encodes the bytes as a Base32 string (uppercase, with `=` padding).
    public static String encode(byte[] data) {
        if (data == null || data.length == 0) return "";
        int output = ((data.length + 4) / 5) * 8;
        StringBuilder b = new StringBuilder(output);
        int bits = 0;
        int value = 0;
        for (int i = 0; i < data.length; i++) {
            value = (value << 8) | (data[i] & 0xff);
            bits += 8;
            while (bits >= 5) {
                b.append(ALPHABET[(value >>> (bits - 5)) & 0x1f]);
                bits -= 5;
            }
        }
        if (bits > 0) {
            b.append(ALPHABET[(value << (5 - bits)) & 0x1f]);
        }
        while (b.length() < output) b.append('=');
        return b.toString();
    }

    /// Decodes a Base32 string. Padding and whitespace are tolerated; mixed
    /// case is accepted.
    public static byte[] decode(String s) {
        if (s == null) return new byte[0];
        // strip padding and whitespace
        StringBuilder cleaned = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '=' || c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == '-') continue;
            cleaned.append(c);
        }
        int len = cleaned.length();
        byte[] out = new byte[len * 5 / 8];
        int bits = 0;
        int value = 0;
        int pos = 0;
        for (int i = 0; i < len; i++) {
            char c = cleaned.charAt(i);
            if (c >= DECODE.length || DECODE[c] < 0) {
                throw new CryptoException("invalid Base32 character: " + c);
            }
            value = (value << 5) | DECODE[c];
            bits += 5;
            if (bits >= 8) {
                out[pos++] = (byte) ((value >>> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return out;
    }
}
