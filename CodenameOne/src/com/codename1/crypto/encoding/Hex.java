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
package com.codename1.crypto.encoding;

/// Hexadecimal encoding and decoding. Output is always lowercase; input may be
/// upper or lower case and may start with a `0x` or `0X` prefix.
@com.codename1.impl.SharedWithBackend
public final class Hex {

    private static final char[] DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private Hex() {
    }

    /// Encodes bytes as a lowercase hex string without a prefix.
    ///
    /// #### Parameters
    ///
    /// - `data`: the bytes to encode
    ///
    /// #### Returns
    ///
    /// two hex digits per byte
    public static String encode(byte[] data) {
        char[] out = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int v = data[i] & 0xff;
            out[i * 2] = DIGITS[v >>> 4];
            out[i * 2 + 1] = DIGITS[v & 15];
        }
        return new String(out);
    }

    /// Decodes a hex string, accepting an optional `0x`/`0X` prefix and either case.
    ///
    /// #### Parameters
    ///
    /// - `hex`: the text to decode
    ///
    /// #### Returns
    ///
    /// the decoded bytes
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the length is odd or a character is not a hex digit
    public static byte[] decode(String hex) {
        int start = 0;
        int n = hex.length();
        if (n >= 2 && hex.charAt(0) == '0' && (hex.charAt(1) == 'x' || hex.charAt(1) == 'X')) {
            start = 2;
        }
        int len = n - start;
        if ((len & 1) != 0) {
            throw new IllegalArgumentException("Odd hex length");
        }
        byte[] out = new byte[len / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = digit(hex.charAt(start + i * 2));
            int lo = digit(hex.charAt(start + i * 2 + 1));
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static int digit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        throw new IllegalArgumentException("Invalid hex digit");
    }
}
