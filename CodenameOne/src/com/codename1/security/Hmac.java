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

/// Keyed-hash message authentication (HMAC, RFC 2104) on top of any hash
/// algorithm supported by [Hash]. Use HMAC whenever you need to prove that a
/// message came from somebody who shares a secret key with you and has not been
/// modified in transit (signatures of API requests, session cookies, JWTs with
/// the HS family, TOTP tokens, etc.).
///
/// #### Quick example
///
/// ```java
/// byte[] tag = Hmac.sha256(secret, message);
///
/// // streaming
/// Hmac h = Hmac.create(Hash.SHA256, secret);
/// h.update(part1);
/// h.update(part2);
/// byte[] tag2 = h.doFinal();
/// ```
///
/// Compare authentication tags with [#constantTimeEquals(byte[], byte[])] --
/// using `java.util.Arrays.equals` or `==` opens you up to timing attacks.
public final class Hmac {

    private final MessageDigestImpl hash;
    private final byte[] outerKey;
    private final byte[] innerKey;
    private boolean started;

    private Hmac(MessageDigestImpl hash, byte[] outerKey, byte[] innerKey) {
        this.hash = hash;
        this.outerKey = outerKey;
        this.innerKey = innerKey;
        reset();
    }

    /// Creates a streaming HMAC.
    ///
    /// #### Parameters
    ///
    /// - `algorithm`: any algorithm accepted by [Hash#create(String)]
    ///
    /// - `key`: secret key. Keys longer than the hash block size are hashed
    ///   down per RFC 2104; keys shorter than the block are zero-padded. There
    ///   is no enforced minimum but for security 128-256 bits of entropy is
    ///   recommended.
    public static Hmac create(String algorithm, byte[] key) {
        MessageDigestImpl h = MessageDigestImpl.create(algorithm);
        int blockSize = blockSizeFor(algorithm);

        byte[] k = key;
        if (k.length > blockSize) {
            h.update(k, 0, k.length);
            k = h.digest();
            h.reset();
        }
        byte[] paddedKey = new byte[blockSize];
        System.arraycopy(k, 0, paddedKey, 0, k.length);

        byte[] inner = new byte[blockSize];
        byte[] outer = new byte[blockSize];
        for (int i = 0; i < blockSize; i++) {
            inner[i] = (byte) (paddedKey[i] ^ 0x36);
            outer[i] = (byte) (paddedKey[i] ^ 0x5c);
        }
        return new Hmac(h, outer, inner);
    }

    private static int blockSizeFor(String algorithm) {
        String a = MessageDigestImpl.normalise(algorithm);
        if ("MD5".equals(a) || "SHA1".equals(a) || "SHA224".equals(a) || "SHA256".equals(a)) {
            return 64;
        }
        if ("SHA384".equals(a) || "SHA512".equals(a)) {
            return 128;
        }
        throw new CryptoException("unsupported HMAC algorithm: " + algorithm);
    }

    /// Resets the running HMAC so the instance can be reused with the same key.
    public void reset() {
        hash.reset();
        started = false;
    }

    /// Appends bytes to the message being authenticated.
    public void update(byte[] data) {
        update(data, 0, data.length);
    }

    /// Appends a slice of bytes to the message being authenticated.
    public void update(byte[] data, int offset, int length) {
        if (!started) {
            hash.update(innerKey, 0, innerKey.length);
            started = true;
        }
        hash.update(data, offset, length);
    }

    /// Finalises and returns the authentication tag. The instance is reset and
    /// can be reused for another message with the same key.
    public byte[] doFinal() {
        if (!started) {
            hash.update(innerKey, 0, innerKey.length);
            started = true;
        }
        byte[] inner = hash.digest();
        hash.update(outerKey, 0, outerKey.length);
        hash.update(inner, 0, inner.length);
        byte[] tag = hash.digest();
        started = false;
        return tag;
    }

    /// One-shot convenience.
    public byte[] doFinal(byte[] data) {
        update(data, 0, data.length);
        return doFinal();
    }

    /// Number of bytes in the authentication tag produced by this HMAC.
    public int tagLength() {
        return hash.digestLength();
    }

    // ----------------------------------------------------------------
    // one-shot entry points

    /// One-shot HMAC-MD5. Legacy interop only -- prefer HMAC-SHA-256.
    public static byte[] md5(byte[] key, byte[] data) { return create(Hash.MD5, key).doFinal(data); }

    /// One-shot HMAC-SHA-1. Legacy interop only -- prefer HMAC-SHA-256.
    public static byte[] sha1(byte[] key, byte[] data) { return create(Hash.SHA1, key).doFinal(data); }

    /// One-shot HMAC-SHA-224.
    public static byte[] sha224(byte[] key, byte[] data) { return create(Hash.SHA224, key).doFinal(data); }

    /// One-shot HMAC-SHA-256 (recommended default).
    public static byte[] sha256(byte[] key, byte[] data) { return create(Hash.SHA256, key).doFinal(data); }

    /// One-shot HMAC-SHA-384.
    public static byte[] sha384(byte[] key, byte[] data) { return create(Hash.SHA384, key).doFinal(data); }

    /// One-shot HMAC-SHA-512.
    public static byte[] sha512(byte[] key, byte[] data) { return create(Hash.SHA512, key).doFinal(data); }

    // ----------------------------------------------------------------

    /// Constant-time comparison of two byte arrays. Returns false if the
    /// arrays differ in length. Use this when comparing authentication tags,
    /// session tokens, or any other secret value -- `Arrays.equals` short
    /// circuits and is vulnerable to timing attacks.
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= (a[i] ^ b[i]);
        }
        return diff == 0;
    }
}
