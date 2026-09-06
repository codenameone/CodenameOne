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
 * Standard base64 (RFC 4648 section 4), with padding.
 *
 * Not {@link Base64Url}: that one is the URL-safe alphabet with the padding
 * stripped, because that is what JWT specifies. These two alphabets are not
 * interchangeable, and the protocols that need this one -- SCRAM-SHA-256 in the
 * PostgreSQL handshake, and AWS request signing -- reject the other.
 *
 * Decoding is strict about length and alphabet. A lenient decoder is how a
 * signature comparison ends up accepting more than one encoding of the same
 * bytes; see the note in {@link Base64Url}.
 */
public final class Base64 {
    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    private static final int[] REVERSE = new int[128];

    static {
        for(int iter = 0 ; iter < REVERSE.length ; iter++) {
            REVERSE[iter] = -1;
        }
        for(int iter = 0 ; iter < ALPHABET.length ; iter++) {
            REVERSE[ALPHABET[iter]] = iter;
        }
    }

    private Base64() {
    }

    public static String encode(byte[] data) {
        if(data == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(((data.length + 2) / 3) * 4);
        int iter = 0;
        while(iter + 2 < data.length) {
            int block = ((data[iter] & 0xff) << 16) | ((data[iter + 1] & 0xff) << 8)
                    | (data[iter + 2] & 0xff);
            out.append(ALPHABET[(block >> 18) & 0x3f]).append(ALPHABET[(block >> 12) & 0x3f])
               .append(ALPHABET[(block >> 6) & 0x3f]).append(ALPHABET[block & 0x3f]);
            iter += 3;
        }
        int remaining = data.length - iter;
        if(remaining == 1) {
            int block = (data[iter] & 0xff) << 16;
            out.append(ALPHABET[(block >> 18) & 0x3f]).append(ALPHABET[(block >> 12) & 0x3f])
               .append('=').append('=');
        } else if(remaining == 2) {
            int block = ((data[iter] & 0xff) << 16) | ((data[iter + 1] & 0xff) << 8);
            out.append(ALPHABET[(block >> 18) & 0x3f]).append(ALPHABET[(block >> 12) & 0x3f])
               .append(ALPHABET[(block >> 6) & 0x3f]).append('=');
        }
        return out.toString();
    }

    /** The decoded bytes, or null when the input is not valid base64. */
    public static byte[] decode(String value) {
        if(value == null || (value.length() % 4) != 0) {
            return null;
        }
        int padding = 0;
        int length = value.length();
        while(padding < 2 && length - padding > 0 && value.charAt(length - padding - 1) == '=') {
            padding++;
        }
        int bytes = (length / 4) * 3 - padding;
        byte[] out = new byte[bytes];
        int at = 0;
        for(int iter = 0 ; iter < length ; iter += 4) {
            int block = 0;
            for(int part = 0 ; part < 4 ; part++) {
                char c = value.charAt(iter + part);
                if(c == '=') {
                    // Padding is only legal in the final group, and only where the
                    // length says it should be.
                    if(iter + 4 != length || part < 2) {
                        return null;
                    }
                    block <<= 6;
                    continue;
                }
                if(c >= REVERSE.length || REVERSE[c] < 0) {
                    return null;
                }
                block = (block << 6) | REVERSE[c];
            }
            for(int part = 16 ; part >= 0 && at < bytes ; part -= 8) {
                out[at++] = (byte)((block >> part) & 0xff);
            }
        }
        return out;
    }
}
