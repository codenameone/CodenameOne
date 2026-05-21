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

/// A symmetric secret key. Used with [Cipher] (AES) and [Hmac] (via raw
/// bytes). For asymmetric algorithms use [PublicKey] / [PrivateKey].
///
/// Keys carry the raw key material plus the algorithm name they are intended
/// for. They do not enforce length or strength -- that is the caller's
/// responsibility, although [KeyGenerator] will produce keys of standard
/// lengths.
public final class SecretKey {
    private final byte[] key;
    private final String algorithm;

    /// Wraps existing key material.
    ///
    /// #### Parameters
    ///
    /// - `algorithm`: algorithm identifier (e.g. "AES")
    ///
    /// - `keyBytes`: raw key material -- defensively copied
    public SecretKey(String algorithm, byte[] keyBytes) {
        if (algorithm == null) {
            throw new CryptoException("algorithm must not be null");
        }
        if (keyBytes == null) {
            throw new CryptoException("keyBytes must not be null");
        }
        this.algorithm = algorithm;
        this.key = new byte[keyBytes.length];
        System.arraycopy(keyBytes, 0, this.key, 0, keyBytes.length);
    }

    /// Returns a fresh copy of the raw key material.
    public byte[] getEncoded() {
        byte[] copy = new byte[key.length];
        System.arraycopy(key, 0, copy, 0, key.length);
        return copy;
    }

    /// Returns the algorithm this key is intended for.
    public String getAlgorithm() {
        return algorithm;
    }

    /// Returns the length of the key in bits.
    public int getBitLength() {
        return key.length * 8;
    }
}
