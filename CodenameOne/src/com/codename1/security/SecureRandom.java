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
package com.codename1.security;

import com.codename1.io.Util;

/// Cryptographically secure random number generator. Delegates to the
/// platform's native CSPRNG (`/dev/urandom` on Unix-style systems, the
/// Windows BCryptGenRandom on Windows, `SecRandomCopyBytes` on iOS, the
/// hardware RNG on devices that expose one).
///
/// Use this class -- not `java.util.Random` or `Math.random()` -- whenever the
/// output is going to be used as a key, nonce, salt, session token, password
/// reset code or any other security-sensitive value.
///
/// #### Example
///
/// ```java
/// byte[] iv = SecureRandom.bytes(12); // AES-GCM nonce
/// int code  = SecureRandom.intBelow(1_000_000); // 6-digit code
/// ```
public final class SecureRandom {

    private SecureRandom() {}

    /// Returns a fresh `length`-byte array filled with secure random bytes.
    public static byte[] bytes(int length) {
        byte[] out = new byte[length];
        fill(out);
        return out;
    }

    /// Fills `out` with secure random bytes.
    public static void fill(byte[] out) {
        if (out == null) {
            throw new CryptoException("out must not be null");
        }
        try {
            Util.secureRandomBytes(out);
        } catch (RuntimeException re) {
            throw new CryptoException(re.getMessage(), re);
        }
    }

    /// Returns a uniformly distributed random int in `[0, bound)`. `bound`
    /// must be positive.
    public static int intBelow(int bound) {
        if (bound <= 0) {
            throw new CryptoException("bound must be positive");
        }
        // Rejection sampling to avoid modulo bias.
        byte[] buf = new byte[4];
        while (true) {
            fill(buf);
            int v = ((buf[0] & 0x7f) << 24)
                    | ((buf[1] & 0xff) << 16)
                    | ((buf[2] & 0xff) << 8)
                    | (buf[3] & 0xff);
            int m = v % bound;
            if (v - m + (bound - 1) >= 0) {
                return m;
            }
        }
    }

    /// Returns a uniformly distributed random long in `[0, bound)`. `bound`
    /// must be positive.
    public static long longBelow(long bound) {
        if (bound <= 0) {
            throw new CryptoException("bound must be positive");
        }
        byte[] buf = new byte[8];
        while (true) {
            fill(buf);
            long v = ((long) (buf[0] & 0x7f) << 56)
                    | ((long) (buf[1] & 0xff) << 48)
                    | ((long) (buf[2] & 0xff) << 40)
                    | ((long) (buf[3] & 0xff) << 32)
                    | ((long) (buf[4] & 0xff) << 24)
                    | ((long) (buf[5] & 0xff) << 16)
                    | ((long) (buf[6] & 0xff) << 8)
                    | (long) (buf[7] & 0xff);
            long m = v % bound;
            if (v - m + (bound - 1) >= 0) {
                return m;
            }
        }
    }
}
