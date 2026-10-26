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

/// Standard Base64 (RFC 4648) with padding: the text form of PSBTs and Solana transactions.
@com.codename1.impl.SharedWithBackend
public final class Base64 {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    private Base64() {
    }

    /// Encodes bytes.
    public static String encode(byte[] in) {
        StringBuilder out = new StringBuilder((in.length + 2) / 3 * 4);
        for (int iter = 0; iter < in.length; iter += 3) {
            int b0 = in[iter] & 0xff;
            int b1 = iter + 1 < in.length ? in[iter + 1] & 0xff : 0;
            int b2 = iter + 2 < in.length ? in[iter + 2] & 0xff : 0;
            out.append(ALPHABET.charAt(b0 >> 2));
            out.append(ALPHABET.charAt(((b0 & 3) << 4) | (b1 >> 4)));
            out.append(iter + 1 < in.length ? ALPHABET.charAt(((b1 & 15) << 2) | (b2 >> 6)) : '=');
            out.append(iter + 2 < in.length ? ALPHABET.charAt(b2 & 63) : '=');
        }
        return out.toString();
    }

    /// Decodes text; whitespace is ignored.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for malformed Base64
    public static byte[] decode(String text) {
        StringBuilder clean = new StringBuilder();
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                clean.append(c);
            }
        }
        String s = clean.toString();
        if (s.length() % 4 != 0) {
            throw new IllegalArgumentException("bad Base64 length");
        }
        int pad = s.endsWith("==") ? 2 : (s.endsWith("=") ? 1 : 0);
        byte[] out = new byte[s.length() / 4 * 3 - pad];
        int o = 0;
        for (int iter = 0; iter < s.length(); iter += 4) {
            int v = 0;
            for (int k = 0; k < 4; k++) {
                char c = s.charAt(iter + k);
                int d;
                if (c == '=') {
                    if (iter + 4 != s.length() || k < 4 - pad) {
                        throw new IllegalArgumentException("bad Base64 padding");
                    }
                    d = 0;
                } else {
                    d = ALPHABET.indexOf(c);
                    if (d < 0) {
                        throw new IllegalArgumentException("bad Base64 character");
                    }
                }
                v = (v << 6) | d;
            }
            for (int k = 0; k < 3 && o < out.length; k++) {
                out[o++] = (byte) (v >> (16 - 8 * k));
            }
        }
        return out;
    }
}
