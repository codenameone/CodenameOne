/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.wayline;

import com.codename1.backend.Crypto;

import java.io.IOException;

/// Identifiers and secrets made of random bytes.
public final class Ids {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Ids() {
    }

    /// A new identifier: 32 hex digits from 16 random bytes. Random and not a
    /// counter, so the id of one ride says nothing about the id of another, and
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
