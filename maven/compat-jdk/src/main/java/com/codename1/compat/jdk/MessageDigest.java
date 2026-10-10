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
package com.codename1.compat.jdk;

import com.codename1.security.Hash;

/// `java.security.MessageDigest` over Codename One's own digests: MD5,
/// SHA-1, SHA-224, SHA-256, SHA-384 and SHA-512. Any other algorithm is a
/// `NoSuchAlgorithmException`, as it is on a JDK without a provider for it.
///
/// There are no providers here: `getInstance` with a provider argument,
/// `getProvider` and a subclass of this class are not offered.
public final class MessageDigest {
    private final String algorithm;
    private final Hash hash;

    private MessageDigest(String algorithm, Hash hash) {
        this.algorithm = algorithm;
        this.hash = hash;
    }

    public static MessageDigest getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        String name = null;
        if ("MD5".equalsIgnoreCase(algorithm)) {
            name = Hash.MD5;
        } else if ("SHA-1".equalsIgnoreCase(algorithm) || "SHA1".equalsIgnoreCase(algorithm)
                || "SHA".equalsIgnoreCase(algorithm)) {
            name = Hash.SHA1;
        } else if ("SHA-224".equalsIgnoreCase(algorithm) || "SHA224".equalsIgnoreCase(algorithm)) {
            name = Hash.SHA224;
        } else if ("SHA-256".equalsIgnoreCase(algorithm) || "SHA256".equalsIgnoreCase(algorithm)) {
            name = Hash.SHA256;
        } else if ("SHA-384".equalsIgnoreCase(algorithm) || "SHA384".equalsIgnoreCase(algorithm)) {
            name = Hash.SHA384;
        } else if ("SHA-512".equalsIgnoreCase(algorithm) || "SHA512".equalsIgnoreCase(algorithm)) {
            name = Hash.SHA512;
        }
        if (name == null) {
            throw new NoSuchAlgorithmException(algorithm + " MessageDigest not available");
        }
        return new MessageDigest(algorithm, Hash.create(name));
    }

    public void update(byte input) {
        hash.update(input);
    }

    public void update(byte[] input) {
        hash.update(input, 0, input.length);
    }

    public void update(byte[] input, int offset, int len) {
        if (input == null) {
            throw new IllegalArgumentException("No input buffer given");
        }
        if (offset < 0 || len < 0 || input.length - offset < len) {
            throw new IllegalArgumentException("Input buffer too short");
        }
        hash.update(input, offset, len);
    }

    /// The digest of everything given so far; the next one starts empty.
    public byte[] digest() {
        byte[] out = hash.digest();
        hash.reset();
        return out;
    }

    public byte[] digest(byte[] input) {
        update(input);
        return digest();
    }

    public void reset() {
        hash.reset();
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getDigestLength() {
        return hash.digestLength();
    }

    /// Whether two digests are the same, in a time that does not depend
    /// on where they first differ.
    public static boolean isEqual(byte[] a, byte[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (b.length == 0) {
            return a.length == 0;
        }
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i % b.length];
        }
        return diff == 0;
    }

    @Override
    public String toString() {
        return algorithm + " Message Digest from Codename One, <in progress>\n";
    }
}
