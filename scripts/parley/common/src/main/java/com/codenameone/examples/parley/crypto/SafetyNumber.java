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
package com.codenameone.examples.parley.crypto;

import com.codename1.security.Hash;

/// The safety number two people compare to know nobody is in the middle.
///
/// Each side contributes 30 digits computed from its identity key and its
/// username (hashed a thousand times, so a key made to collide with a
/// given number costs a thousand times more to search for); the two halves
/// are joined in username order, so both phones show the same 60 digits.
/// It changes when either side's key does, which is the point: a changed
/// number on a chat that has not changed phones is worth a question.
///
/// This is Signal's displayable fingerprint in shape; the iteration count and
/// the version byte are this app's own, so the numbers do not match Signal's.
public final class SafetyNumber {
    private static final int ITERATIONS = 1024;

    private SafetyNumber() {
    }

    /// The 60 digits, grouped by five with spaces.
    public static String of(String me, byte[] myKey, String them, byte[] theirKey) {
        String mine = half(me, myKey);
        String theirs = half(them, theirKey);
        String joined = me.compareTo(them) <= 0 ? mine + theirs : theirs + mine;
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < joined.length(); iter += 5) {
            if (iter > 0) {
                out.append(' ');
            }
            out.append(joined.substring(iter, iter + 5));
        }
        return out.toString();
    }

    static String half(String username, byte[] key) {
        byte[] digest = Codec.concat(new byte[] {0, 1}, key, Codec.utf8(username));
        for (int iter = 0; iter < ITERATIONS; iter++) {
            digest = Hash.sha256(Codec.concat(digest, key));
        }
        StringBuilder out = new StringBuilder();
        for (int chunk = 0; chunk < 6; chunk++) {
            long value = 0;
            for (int b = 0; b < 5; b++) {
                value = (value << 8) | (digest[chunk * 5 + b] & 0xff);
            }
            String digits = String.valueOf(value % 100000);
            for (int pad = digits.length(); pad < 5; pad++) {
                out.append('0');
            }
            out.append(digits);
        }
        return out.toString();
    }
}
