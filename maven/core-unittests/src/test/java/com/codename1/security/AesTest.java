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

import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// NIST SP 800-38A, F.2.1 and F.2.5 (CBC-AES128 and CBC-AES256), through the
/// installed engine and through the portable implementation.
class AesTest {
    private static final byte[] IV = Hash.fromHex("000102030405060708090a0b0c0d0e0f");
    private static final byte[] PLAIN = Hash.fromHex("6bc1bee22e409f96e93d7e117393172aae2d8a571e03ac9c9eb76fac45af8e51");

    private static void vectors() {
        byte[] k128 = Hash.fromHex("2b7e151628aed2a6abf7158809cf4f3c");
        byte[] c128 = Hash.fromHex("7649abac8119b246cee98e9b12e9197d5086cb9b507219ee95db113a917678b2");
        assertArrayEquals(c128, Aes.cbcEncrypt(k128, IV, PLAIN));
        assertArrayEquals(PLAIN, Aes.cbcDecrypt(k128, IV, c128));
        byte[] k256 = Hash.fromHex("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4");
        byte[] c256 = Hash.fromHex("f58c4c04d6e5f1ba779eabfb5f7bfbd69cfc4e967edb808d679f777bc6702c7d");
        assertArrayEquals(c256, Aes.cbcEncrypt(k256, IV, PLAIN));
        assertArrayEquals(PLAIN, Aes.cbcDecrypt(k256, IV, c256));
    }

    @Test
    void installedEngine() {
        vectors();
    }

    @Test
    void portableImplementation() {
        CryptoEngine was = CryptoEngine.get();
        CryptoEngine.install(null);
        try {
            vectors();
        } finally {
            CryptoEngine.install(was);
        }
    }

    @Test
    void zeroIvChainsFromZeros() {
        // FIPS 197 with an all-zero key: one block of zeros, so CBC from a zero
        // IV is the bare block cipher; the second block checks the chaining.
        byte[] key = new byte[16];
        byte[] plain = new byte[32];
        byte[] sealed = Aes.cbcEncryptZeroIv(key, plain);
        assertArrayEquals(Hash.fromHex("66e94bd4ef8a2c3b884cfa59ca342b2e"), Arrays.copyOf(sealed, 16));
        assertArrayEquals(Aes.cbcEncrypt(key, Arrays.copyOf(sealed, 16), new byte[16]),
                Arrays.copyOfRange(sealed, 16, 32));
        assertArrayEquals(plain, Aes.cbcDecryptZeroIv(key, sealed));
        assertThrows(IllegalArgumentException.class, () -> Aes.cbcEncryptZeroIv(key, new byte[15]));
    }

    @Test
    void rejectsPartialBlocksAndBadKeys() {
        assertThrows(IllegalArgumentException.class, () -> Aes.cbcDecrypt(new byte[16], IV, new byte[15]));
        assertThrows(IllegalArgumentException.class, () -> Aes.cbcEncrypt(new byte[15], IV, new byte[16]));
    }
}
