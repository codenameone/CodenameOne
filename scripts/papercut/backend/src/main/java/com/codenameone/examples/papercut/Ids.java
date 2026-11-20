/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut;

import com.codename1.backend.Crypto;

import java.io.IOException;

/// Identifiers and secrets made of random bytes.
public final class Ids {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Ids() {
    }

    /// A new identifier: 32 hex digits from 16 random bytes. Random and not a
    /// counter, so the id of one delivery says nothing about the id of another, and
    /// so no table needs a generated key, which every database spells its own way.
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

    /// The SHA-256 of `text`, in hex. What is stored in place of a secret that
    /// only ever needs to be compared.
    public static String sha256(String text) throws IOException {
        return hex(Crypto.sha256(text.getBytes("UTF-8")));
    }
}
