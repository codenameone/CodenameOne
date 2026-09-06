/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend;

/**
 * Base64url without padding, as JSON Web Tokens use it. Separate from any general
 * base64 because the alphabet differs ('-' and '_' for '+' and '/') and a token
 * encoded with the wrong one is rejected by every other implementation.
 */
public final class Base64Url {
    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();

    private Base64Url() {
    }

    public static String encode(byte[] data) {
        if(data == null) {
            return null;
        }
        StringBuilder out = new StringBuilder((data.length + 2) / 3 * 4);
        int iter = 0;
        while(iter + 2 < data.length) {
            int n = ((data[iter] & 0xff) << 16) | ((data[iter + 1] & 0xff) << 8) | (data[iter + 2] & 0xff);
            out.append(ALPHABET[(n >>> 18) & 63]).append(ALPHABET[(n >>> 12) & 63])
               .append(ALPHABET[(n >>> 6) & 63]).append(ALPHABET[n & 63]);
            iter += 3;
        }
        int remaining = data.length - iter;
        if(remaining == 1) {
            int n = (data[iter] & 0xff) << 16;
            out.append(ALPHABET[(n >>> 18) & 63]).append(ALPHABET[(n >>> 12) & 63]);
        } else if(remaining == 2) {
            int n = ((data[iter] & 0xff) << 16) | ((data[iter + 1] & 0xff) << 8);
            out.append(ALPHABET[(n >>> 18) & 63]).append(ALPHABET[(n >>> 12) & 63])
               .append(ALPHABET[(n >>> 6) & 63]);
        }
        return out.toString();
    }

    /** Null for anything that is not valid base64url, rather than a partial result. */
    public static byte[] decode(String value) {
        if(value == null) {
            return null;
        }
        int length = value.length();
        int fullGroups = length / 4;
        int remaining = length % 4;
        if(remaining == 1) {
            return null; // no valid encoding leaves a single character over
        }
        int size = fullGroups * 3 + (remaining == 0 ? 0 : remaining - 1);
        byte[] out = new byte[size];
        int outPos = 0;
        int buffer = 0;
        int bits = 0;
        for(int iter = 0 ; iter < length ; iter++) {
            int v = valueOf(value.charAt(iter));
            if(v < 0) {
                return null;
            }
            buffer = (buffer << 6) | v;
            bits += 6;
            if(bits >= 8) {
                bits -= 8;
                if(outPos >= size) {
                    return null;
                }
                out[outPos++] = (byte)((buffer >>> bits) & 0xff);
            }
        }
        // The leftover bits of the final character must be zero. Accepting a
        // non-canonical encoding means several distinct strings decode to the same
        // bytes -- for a JWT that is token malleability: an attacker can hand back
        // a different-looking token that still verifies, which breaks anything
        // keyed on the token string, a revocation list most of all.
        if(bits > 0 && (buffer & ((1 << bits) - 1)) != 0) {
            return null;
        }
        return outPos == size ? out : null;
    }

    private static int valueOf(char c) {
        if(c >= 'A' && c <= 'Z') {
            return c - 'A';
        }
        if(c >= 'a' && c <= 'z') {
            return c - 'a' + 26;
        }
        if(c >= '0' && c <= '9') {
            return c - '0' + 52;
        }
        if(c == '-') {
            return 62;
        }
        if(c == '_') {
            return 63;
        }
        return -1;
    }
}
