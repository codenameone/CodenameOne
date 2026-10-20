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
package com.codename1.security;

/// HMAC-based key derivation (HKDF), per RFC 5869.
///
/// HKDF turns input keying material that is not uniformly random -- the
/// shared secret of a [KeyAgreement], say -- into one or more keys of any
/// length. It runs in two steps: *extract* concentrates the input into a
/// pseudorandom key, and *expand* stretches that into as many bytes as are
/// asked for, bound to an `info` label so that keys derived for different
/// purposes never coincide.
///
/// ```java
/// byte[] shared = KeyAgreement.ecdh(mine.getPrivateKey(), theirs);
/// byte[] keys = Hkdf.derive("SHA-256", salt, shared, info, 64);
/// ```
///
/// It is pure Java over [Hmac], so it behaves identically on every port and on
/// the server.
@com.codename1.impl.SharedWithBackend
public final class Hkdf {
    private Hkdf() {}

    /// The extract step: `HMAC(salt, ikm)`. A null or empty `salt` is
    /// replaced by a string of zero bytes the length of the hash, as the RFC
    /// specifies.
    ///
    /// #### Parameters
    ///
    /// - `algorithm`: the hash, any name [Hmac#create(String, byte[])] accepts
    ///
    /// - `salt`: optional salt
    ///
    /// - `ikm`: the input keying material
    public static byte[] extract(String algorithm, byte[] salt, byte[] ikm) {
        if (ikm == null) {
            throw new CryptoException("HKDF input keying material must not be null");
        }
        byte[] s = salt;
        if (s == null || s.length == 0) {
            s = new byte[Hmac.create(algorithm, new byte[0]).tagLength()];
        }
        return Hmac.create(algorithm, s).doFinal(ikm);
    }

    /// The expand step: derives `length` bytes from a pseudorandom key.
    ///
    /// #### Parameters
    ///
    /// - `algorithm`: the hash
    ///
    /// - `prk`: the pseudorandom key, usually the result of [#extract]
    ///
    /// - `info`: context label; may be null
    ///
    /// - `length`: bytes wanted, at most 255 times the hash length
    public static byte[] expand(String algorithm, byte[] prk, byte[] info, int length) {
        if (prk == null) {
            throw new CryptoException("HKDF pseudorandom key must not be null");
        }
        Hmac mac = Hmac.create(algorithm, prk);
        int hashLength = mac.tagLength();
        if (length < 0 || length > 255 * hashLength) {
            throw new CryptoException("HKDF output length out of range: " + length);
        }
        byte[] out = new byte[length];
        byte[] previous = new byte[0];
        byte[] counter = new byte[1];
        int written = 0;
        int block = 1;
        while (written < length) {
            mac.update(previous);
            if (info != null) {
                mac.update(info);
            }
            counter[0] = (byte) block;
            mac.update(counter);
            previous = mac.doFinal();
            int n = Math.min(previous.length, length - written);
            System.arraycopy(previous, 0, out, written, n);
            written += n;
            block++;
        }
        return out;
    }

    /// Extract then expand in one call.
    ///
    /// #### Parameters
    ///
    /// - `algorithm`: the hash, for example `"SHA-256"`
    ///
    /// - `salt`: optional salt
    ///
    /// - `ikm`: the input keying material
    ///
    /// - `info`: context label; may be null
    ///
    /// - `length`: bytes wanted
    public static byte[] derive(String algorithm, byte[] salt, byte[] ikm, byte[] info, int length) {
        return expand(algorithm, extract(algorithm, salt, ikm), info, length);
    }
}
