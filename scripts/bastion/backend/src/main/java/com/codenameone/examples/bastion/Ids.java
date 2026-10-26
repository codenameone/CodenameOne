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
package com.codenameone.examples.bastion;

import com.codename1.backend.Crypto;

import java.io.IOException;

/// Identifiers and the hashes the ledger and the audit chain are built on.
public final class Ids {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Ids() {
    }

    /// 128 random bits as hex. Every id in this server is one: nothing is a
    /// counter, so no id says how many customers or transfers there are, and
    /// none can be guessed from another.
    public static String next() throws IOException {
        return hex(Crypto.randomBytes(16));
    }

    public static String hex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int iter = 0; iter < bytes.length; iter++) {
            out[iter * 2] = HEX[(bytes[iter] >> 4) & 0xf];
            out[iter * 2 + 1] = HEX[bytes[iter] & 0xf];
        }
        return new String(out);
    }

    public static String sha256(String text) throws IOException {
        return hex(Crypto.sha256(text.getBytes("UTF-8")));
    }
}
