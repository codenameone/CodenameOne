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
package java.security;

import com.codename1.impl.JdkHost;

import java.util.Random;

/// A cryptographically strong random number generator over the platform's
/// own source of secure random bytes, which `com.codename1.impl.JdkHost`
/// reaches: the one `com.codename1.security.SecureRandom` reads in a Codename
/// One application, `/dev/urandom` for a program translated without it.
///
/// A seed adds nothing: the platform's generator seeds itself, and
/// `setSeed` is accepted and ignored rather than made to weaken it.
public class SecureRandom extends Random {
    private static final long serialVersionUID = 1L;

    public SecureRandom() {
        super(0);
    }

    /// The seed is ignored; see the class description.
    public SecureRandom(byte[] seed) {
        super(0);
    }

    /// The platform's generator, which never blocks.
    public static SecureRandom getInstanceStrong() {
        return new SecureRandom();
    }

    @Override
    public void setSeed(long seed) {
        // Ignored: see the class description.
    }

    /// Ignored: see the class description.
    public void setSeed(byte[] seed) {
    }

    public void nextBytes(byte[] bytes) {
        JdkHost.get().randomBytes(bytes);
    }

    /// `numBytes` fresh random bytes, where the JDK answers seed material.
    public byte[] generateSeed(int numBytes) {
        if (numBytes < 0) {
            throw new IllegalArgumentException("numBytes cannot be negative");
        }
        byte[] out = new byte[numBytes];
        JdkHost.get().randomBytes(out);
        return out;
    }

    @Override
    protected int next(int bits) {
        byte[] four = new byte[4];
        JdkHost.get().randomBytes(four);
        int value = (four[0] & 0xFF) << 24 | (four[1] & 0xFF) << 16 | (four[2] & 0xFF) << 8 | (four[3] & 0xFF);
        return bits >= 32 ? value : value >>> (32 - bits);
    }

    public String getAlgorithm() {
        return "NativePRNG";
    }
}
