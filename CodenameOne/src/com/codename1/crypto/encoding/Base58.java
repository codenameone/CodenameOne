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

/// Base58 encoding with the Bitcoin alphabet. Each leading zero byte is written
/// as a leading `1` and vice versa. The conversion works on an int array of
/// limbs, so it is quadratic in the length but has no big-number allocation.
@com.codename1.impl.SharedWithBackend
public final class Base58 {

    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final int[] INDEXES = new int[128];

    static {
        for (int i = 0; i < INDEXES.length; i++) {
            INDEXES[i] = -1;
        }
        for (int i = 0; i < ALPHABET.length(); i++) {
            INDEXES[ALPHABET.charAt(i)] = i;
        }
    }

    private Base58() {
    }

    /// Encodes bytes as Base58 text.
    ///
    /// #### Parameters
    ///
    /// - `input`: the bytes to encode
    ///
    /// #### Returns
    ///
    /// the Base58 string; empty input gives an empty string
    public static String encode(byte[] input) {
        int zeros = 0;
        while (zeros < input.length && input[zeros] == 0) {
            zeros++;
        }
        // limbs are base 58^5 (656356768 < 2^31), little endian
        int[] limbs = new int[input.length / 3 + 2];
        int used = 0;
        for (int i = zeros; i < input.length; i++) {
            long carry = input[i] & 0xff;
            for (int j = 0; j < used; j++) {
                carry += ((long) limbs[j]) << 8;
                limbs[j] = (int) (carry % 656356768L);
                carry /= 656356768L;
            }
            while (carry > 0) {
                limbs[used++] = (int) (carry % 656356768L);
                carry /= 656356768L;
            }
        }
        char[] out = new char[zeros + used * 5];
        int pos = out.length;
        for (int j = 0; j < used; j++) {
            int v = limbs[j];
            for (int k = 0; k < 5; k++) {
                out[--pos] = ALPHABET.charAt(v % 58);
                v /= 58;
            }
        }
        // strip padding zeros ('1') produced by the top limb, keep the real leading ones
        int firstDigit = zeros;
        int end = out.length;
        while (firstDigit < end && out[firstDigit] == '1') {
            firstDigit++;
        }
        StringBuilder sb = new StringBuilder(zeros + end - firstDigit);
        for (int i = 0; i < zeros; i++) {
            sb.append('1');
        }
        sb.append(out, firstDigit, end - firstDigit);
        return sb.toString();
    }

    /// Decodes Base58 text.
    ///
    /// #### Parameters
    ///
    /// - `input`: the Base58 string
    ///
    /// #### Returns
    ///
    /// the decoded bytes
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the text holds a character outside the alphabet
    public static byte[] decode(String input) {
        int n = input.length();
        int[] digits = new int[n];
        for (int i = 0; i < n; i++) {
            char c = input.charAt(i);
            int d = c < 128 ? INDEXES[c] : -1;
            if (d < 0) {
                throw new IllegalArgumentException("Invalid Base58 character at " + i);
            }
            digits[i] = d;
        }
        int zeros = 0;
        while (zeros < n && digits[zeros] == 0) {
            zeros++;
        }
        // limbs are base 2^32 held in longs, little endian
        int[] limbs = new int[n * 733 / 1000 / 4 + 2];
        int used = 0;
        for (int i = zeros; i < n; i++) {
            long carry = digits[i];
            for (int j = 0; j < used; j++) {
                carry += (limbs[j] & 0xffffffffL) * 58L;
                limbs[j] = (int) carry;
                carry >>>= 32;
            }
            while (carry > 0) {
                limbs[used++] = (int) carry;
                carry >>>= 32;
            }
        }
        int byteLen = used * 4;
        byte[] tmp = new byte[byteLen];
        for (int j = 0; j < used; j++) {
            int v = limbs[j];
            tmp[byteLen - 1 - j * 4] = (byte) v;
            tmp[byteLen - 2 - j * 4] = (byte) (v >>> 8);
            tmp[byteLen - 3 - j * 4] = (byte) (v >>> 16);
            tmp[byteLen - 4 - j * 4] = (byte) (v >>> 24);
        }
        int skip = 0;
        while (skip < byteLen && tmp[skip] == 0) {
            skip++;
        }
        byte[] out = new byte[zeros + byteLen - skip];
        System.arraycopy(tmp, skip, out, zeros, byteLen - skip);
        return out;
    }
}
