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
package com.codename1.androidcompat.jdk;

/// `javax.crypto.spec.SecretKeySpec` for the Codename One runtime: raw key
/// bytes under an algorithm name, exportable like the JDK's.
public class SecretKeySpec implements java.security.spec.KeySpec, javax.crypto.SecretKey {
    private static final long serialVersionUID = 1L;

    private final byte[] key;
    private final String algorithm;

    public SecretKeySpec(byte[] key, String algorithm) {
        this(key, 0, key == null ? 0 : key.length, algorithm);
    }

    public SecretKeySpec(byte[] key, int offset, int len, String algorithm) {
        if (key == null || algorithm == null) {
            throw new IllegalArgumentException("Missing argument");
        }
        if (len == 0) {
            throw new IllegalArgumentException("Empty key");
        }
        if (offset < 0 || len < 0 || offset + len > key.length) {
            throw new IllegalArgumentException("Invalid offset/length combination");
        }
        this.key = new byte[len];
        System.arraycopy(key, offset, this.key, 0, len);
        this.algorithm = algorithm;
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public String getFormat() {
        return "RAW";
    }

    @Override
    public byte[] getEncoded() {
        byte[] out = new byte[key.length];
        System.arraycopy(key, 0, out, 0, key.length);
        return out;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int i = 1; i < key.length; i++) {
            h += key[i] * i;
        }
        return h ^ algorithm.hashCode();
    }

    /// Constant-time over the key bytes, like the JDK's.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecretKeySpec)) {
            return false;
        }
        SecretKeySpec other = (SecretKeySpec) o;
        if (!algorithm.equalsIgnoreCase(other.algorithm) || key.length != other.key.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < key.length; i++) {
            diff |= key[i] ^ other.key[i];
        }
        return diff == 0;
    }
}
