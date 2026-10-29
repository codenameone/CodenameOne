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

import com.codename1.security.Hash;

/// Base58Check: Base58 text of a payload followed by the first four bytes of its
/// double SHA-256 digest. Used for Bitcoin legacy addresses and WIF keys.
@com.codename1.impl.SharedWithBackend
public final class Base58Check {

    private Base58Check() {
    }

    /// Appends the checksum to a payload and encodes the result.
    ///
    /// #### Parameters
    ///
    /// - `payload`: the version byte(s) and data
    ///
    /// #### Returns
    ///
    /// the Base58Check string
    public static String encode(byte[] payload) {
        byte[] sum = checksum(payload, payload.length);
        byte[] all = new byte[payload.length + 4];
        System.arraycopy(payload, 0, all, 0, payload.length);
        System.arraycopy(sum, 0, all, payload.length, 4);
        return Base58.encode(all);
    }

    /// Decodes Base58Check text, verifies the checksum and strips it.
    ///
    /// #### Parameters
    ///
    /// - `text`: the Base58Check string
    ///
    /// #### Returns
    ///
    /// the payload without the checksum
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the text is malformed, too short or the checksum is wrong
    public static byte[] decode(String text) {
        byte[] all = Base58.decode(text);
        if (all.length < 4) {
            throw new IllegalArgumentException("Base58Check input too short");
        }
        int n = all.length - 4;
        byte[] sum = checksum(all, n);
        int diff = 0;
        for (int i = 0; i < 4; i++) {
            diff |= sum[i] ^ all[n + i];
        }
        if (diff != 0) {
            throw new IllegalArgumentException("Bad Base58Check checksum");
        }
        byte[] out = new byte[n];
        System.arraycopy(all, 0, out, 0, n);
        return out;
    }

    private static byte[] checksum(byte[] data, int len) {
        byte[] part = data;
        if (len != data.length) {
            part = new byte[len];
            System.arraycopy(data, 0, part, 0, len);
        }
        return Hash.sha256(Hash.sha256(part));
    }
}
