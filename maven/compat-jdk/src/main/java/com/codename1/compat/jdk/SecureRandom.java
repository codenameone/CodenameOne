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

import java.util.Random;

/// `java.security.SecureRandom` over the platform's own source of secure
/// random bytes, the one `com.codename1.security.SecureRandom` reads.
///
/// A seed adds nothing: the platform's generator seeds itself, and
/// `setSeed` is accepted and ignored rather than made to weaken it.
public class SecureRandom extends Random {

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
        com.codename1.security.SecureRandom.fill(bytes);
    }

    /// `numBytes` fresh random bytes, where the JDK answers seed material.
    public byte[] generateSeed(int numBytes) {
        return com.codename1.security.SecureRandom.bytes(numBytes);
    }

    @Override
    protected int next(int bits) {
        byte[] four = com.codename1.security.SecureRandom.bytes(4);
        int value = (four[0] & 0xFF) << 24 | (four[1] & 0xFF) << 16 | (four[2] & 0xFF) << 8 | (four[3] & 0xFF);
        return bits >= 32 ? value : value >>> (32 - bits);
    }

    public String getAlgorithm() {
        return "NativePRNG";
    }
}
