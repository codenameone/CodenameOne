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
package com.codenameone.examples.parley.bridge;

import com.codename1.backend.Crypto;

import java.nio.charset.StandardCharsets;

/// The comparisons the webhooks prove themselves with, all in constant time:
/// a comparison that stops at the first wrong byte tells whoever is guessing
/// how many they had right.
final class Signatures {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Signatures() {
    }

    /// Whether `given` is `expected`, with neither one's length or content
    /// leaking through the time taken.
    static boolean same(String expected, String given) {
        if (expected == null || expected.length() == 0 || given == null) {
            return false;
        }
        return Crypto.equalsConstantTime(expected.getBytes(StandardCharsets.UTF_8),
                given.getBytes(StandardCharsets.UTF_8));
    }

    /// `HMAC-SHA256(key, body)` as lower-case hex.
    static String hmacHex(String key, byte[] body) {
        byte[] mac = Crypto.hmacSha256(key.getBytes(StandardCharsets.UTF_8), body);
        char[] out = new char[mac.length * 2];
        for (int iter = 0; iter < mac.length; iter++) {
            out[iter * 2] = HEX[(mac[iter] >> 4) & 15];
            out[iter * 2 + 1] = HEX[mac[iter] & 15];
        }
        return new String(out);
    }
}
