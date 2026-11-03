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
package com.codename1.backend.security.crypto;

import com.codename1.backend.Crypto;
import com.codename1.security.Hash;
import java.io.UnsupportedEncodingException;

/// The hexadecimal SHA-256 of a value, unsalted and in one round. Registered as
/// `{sha256}`.
///
/// This exists to verify values another system stored this way, so that they
/// can be imported without asking every user for new ones: recovery codes, or
/// tokens that are long and random. A single unsalted digest is far too fast for
/// a password a person chose; never encode those with it.
///
/// [#matches(CharSequence, String)] compares in constant time and accepts upper
/// or lower case hexadecimal, and [#upgradeEncoding(String)] is always true.
public final class Sha256HexPasswordEncoder implements PasswordEncoder {
    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        return Hash.toHex(Crypto.sha256(utf8(rawPassword.toString())));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.length() != 64) {
            return false;
        }
        String actual = encode(rawPassword);
        int diff = 0;
        for (int iter = 0 ; iter < 64 ; iter++) {
            int c = encodedPassword.charAt(iter);
            // Upper case hex folded with arithmetic, so the time does not depend on it.
            int upper = ((c - 'A') >>> 31 ^ 1) & (('F' - c) >>> 31 ^ 1);
            c += upper * ('a' - 'A');
            diff |= c ^ actual.charAt(iter);
        }
        return diff == 0;
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return true;
    }

    private static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }
}
