/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.chain;

/// Lowercase hex, both ways, for the chain's byte strings.
public final class Hex32 {
    private static final char[] DIGITS = "0123456789abcdef".toCharArray();

    private Hex32() {
    }

    public static String encode(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int iter = 0; iter < bytes.length; iter++) {
            out[iter * 2] = DIGITS[(bytes[iter] >> 4) & 0xf];
            out[iter * 2 + 1] = DIGITS[bytes[iter] & 0xf];
        }
        return new String(out);
    }

    /// `hex` (no `0x`, even length, either case) as bytes.
    public static byte[] decode(String hex) {
        if ((hex.length() & 1) == 1) {
            throw new IllegalArgumentException("odd hex length");
        }
        byte[] out = new byte[hex.length() / 2];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = (byte) ((digit(hex.charAt(iter * 2)) << 4) | digit(hex.charAt(iter * 2 + 1)));
        }
        return out;
    }

    /// `hex` with an optional `0x`.
    public static byte[] decode0x(String hex) {
        return decode(hex.startsWith("0x") || hex.startsWith("0X") ? hex.substring(2) : hex);
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
        throw new IllegalArgumentException("not a hex digit: " + c);
    }
}
