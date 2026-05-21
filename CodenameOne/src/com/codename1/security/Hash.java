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

/// Streaming and one-shot cryptographic hash (message digest) functions. The
/// supported algorithms are exposed as constants on this class:
///
/// - [#MD5]              -- 128 bit, legacy interop only (broken collision resistance)
/// - [#SHA1]             -- 160 bit, legacy interop only (broken collision resistance)
/// - [#SHA224]           -- 224 bit (SHA-2 family)
/// - [#SHA256]           -- 256 bit (SHA-2 family, recommended general-purpose hash)
/// - [#SHA384]           -- 384 bit (SHA-2 family)
/// - [#SHA512]           -- 512 bit (SHA-2 family)
///
/// #### Quick example
///
/// ```java
/// byte[] digest = Hash.sha256("hello".getBytes("UTF-8"));
/// String hex    = Hash.toHex(digest);
///
/// // streaming
/// Hash h = Hash.create(Hash.SHA256);
/// h.update("hello".getBytes("UTF-8"));
/// h.update(" world".getBytes("UTF-8"));
/// byte[] out = h.digest();
/// ```
///
/// The implementations are written entirely in portable Java so they are
/// available on every supported platform. They produce identical output to
/// the equivalent algorithm in the standard JDK.
public final class Hash {

    /// MD5 algorithm identifier (128-bit digest). Provided for legacy interop --
    /// MD5 is no longer considered collision resistant and should not be used
    /// for new security-sensitive code.
    public static final String MD5 = "MD5";

    /// SHA-1 algorithm identifier (160-bit digest). Provided for legacy interop
    /// -- SHA-1 is no longer considered collision resistant and should not be
    /// used for new security-sensitive code.
    public static final String SHA1 = "SHA-1";

    /// SHA-224 algorithm identifier (224-bit digest, SHA-2 family).
    public static final String SHA224 = "SHA-224";

    /// SHA-256 algorithm identifier (256-bit digest, SHA-2 family). Recommended
    /// default for general-purpose hashing.
    public static final String SHA256 = "SHA-256";

    /// SHA-384 algorithm identifier (384-bit digest, SHA-2 family).
    public static final String SHA384 = "SHA-384";

    /// SHA-512 algorithm identifier (512-bit digest, SHA-2 family).
    public static final String SHA512 = "SHA-512";

    private final MessageDigestImpl impl;

    private Hash(MessageDigestImpl impl) {
        this.impl = impl;
    }

    /// Creates a streaming hash for the given algorithm.
    ///
    /// #### Parameters
    ///
    /// - `algorithm`: one of [#MD5], [#SHA1], [#SHA224], [#SHA256], [#SHA384],
    ///   [#SHA512] -- case insensitive, with or without the dash
    ///
    /// #### Returns
    ///
    /// a new Hash instance with zero bytes consumed
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if the algorithm is not recognised
    public static Hash create(String algorithm) {
        return new Hash(MessageDigestImpl.create(algorithm));
    }

    /// Feeds the entire array into the running hash.
    ///
    /// #### Parameters
    ///
    /// - `data`: bytes to append to the running digest
    public void update(byte[] data) {
        impl.update(data, 0, data.length);
    }

    /// Feeds a slice of the array into the running hash.
    ///
    /// #### Parameters
    ///
    /// - `data`: bytes to append to the running digest
    ///
    /// - `offset`: index of the first byte to read
    ///
    /// - `length`: number of bytes to read
    public void update(byte[] data, int offset, int length) {
        impl.update(data, offset, length);
    }

    /// Feeds a single byte into the running hash.
    public void update(byte b) {
        impl.update(b);
    }

    /// Finalises the running hash and returns the digest. The hash is reset
    /// after this call so the same instance may be reused for another message.
    ///
    /// #### Returns
    ///
    /// the raw digest bytes (algorithm specific length)
    public byte[] digest() {
        return impl.digest();
    }

    /// Convenience: feed `data` then return the digest.
    public byte[] digest(byte[] data) {
        impl.update(data, 0, data.length);
        return impl.digest();
    }

    /// Number of bytes in the digest produced by this hash.
    public int digestLength() {
        return impl.digestLength();
    }

    /// Resets the running digest so the instance can be reused.
    public void reset() {
        impl.reset();
    }

    // ----------------------------------------------------------------
    // one-shot convenience entry points

    /// One-shot MD5 hash.
    public static byte[] md5(byte[] data) { return create(MD5).digest(data); }

    /// One-shot SHA-1 hash.
    public static byte[] sha1(byte[] data) { return create(SHA1).digest(data); }

    /// One-shot SHA-224 hash.
    public static byte[] sha224(byte[] data) { return create(SHA224).digest(data); }

    /// One-shot SHA-256 hash (recommended general-purpose hash).
    public static byte[] sha256(byte[] data) { return create(SHA256).digest(data); }

    /// One-shot SHA-384 hash.
    public static byte[] sha384(byte[] data) { return create(SHA384).digest(data); }

    /// One-shot SHA-512 hash.
    public static byte[] sha512(byte[] data) { return create(SHA512).digest(data); }

    // ----------------------------------------------------------------
    // hex helpers -- handy for displaying digests and writing test vectors

    /// Encodes the bytes as a lowercase hex string (two characters per byte).
    public static String toHex(byte[] data) {
        if (data == null) {
            return null;
        }
        StringBuilder b = new StringBuilder(data.length * 2);
        for (byte d : data) {
            int v = d & 0xff;
            b.append(HEX[v >>> 4]);
            b.append(HEX[v & 0x0f]);
        }
        return b.toString();
    }

    /// Decodes a hex string back into bytes. The string must contain an even
    /// number of hex characters (whitespace and the `0x` prefix are not
    /// stripped -- pass cleaned input).
    public static byte[] fromHex(String hex) {
        if (hex == null) {
            return null;
        }
        int len = hex.length();
        if ((len & 1) != 0) {
            throw new CryptoException("hex string must have even length");
        }
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = nibble(hex.charAt(i));
            int lo = nibble(hex.charAt(i + 1));
            out[i / 2] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static int nibble(char c) {
        if (c >= '0' && c <= '9') { return c - '0'; }
        if (c >= 'a' && c <= 'f') { return c - 'a' + 10; }
        if (c >= 'A' && c <= 'F') { return c - 'A' + 10; }
        throw new CryptoException("invalid hex digit");
    }

    private static final char[] HEX = {
            '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'
    };
}
