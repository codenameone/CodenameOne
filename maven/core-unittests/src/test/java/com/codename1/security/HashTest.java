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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Test vectors come from the FIPS / RFC reference documents — these are
/// short, public, deterministic, and let us be confident the hash output
/// matches every other compliant implementation byte-for-byte.
class HashTest {

    private static byte[] ascii(String s) {
        try {
            return s.getBytes("US-ASCII");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void md5_emptyAndAbc() {
        // RFC 1321 appendix A.5
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", Hash.toHex(Hash.md5(new byte[0])));
        assertEquals("900150983cd24fb0d6963f7d28e17f72", Hash.toHex(Hash.md5(ascii("abc"))));
    }

    @Test
    void sha1_abc() {
        // FIPS 180-4 A.1
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d",
                Hash.toHex(Hash.sha1(ascii("abc"))));
    }

    @Test
    void sha256_abc() {
        // FIPS 180-4 B.1
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                Hash.toHex(Hash.sha256(ascii("abc"))));
    }

    @Test
    void sha384_abc() {
        // FIPS 180-4 D.1
        assertEquals(
            "cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7",
            Hash.toHex(Hash.sha384(ascii("abc"))));
    }

    @Test
    void sha512_abc() {
        // FIPS 180-4 C.1
        assertEquals(
            "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            Hash.toHex(Hash.sha512(ascii("abc"))));
    }

    @Test
    void streamingProducesSameResultAsOneShot() {
        byte[] data = ascii("The quick brown fox jumps over the lazy dog");
        Hash h = Hash.create(Hash.SHA256);
        for (int i = 0; i < data.length; i++) h.update(data[i]);
        assertArrayEquals(Hash.sha256(data), h.digest());
    }

    @Test
    void multiBlockMessage() {
        // length-pad correctness across block boundaries: 1MB of 'a'.
        byte[] data = new byte[1_000_000];
        for (int i = 0; i < data.length; i++) data[i] = 'a';
        // SHA-256("a" * 1_000_000) — well-known vector
        assertEquals("cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
                Hash.toHex(Hash.sha256(data)));
    }

    @Test
    void hexRoundTrip() {
        byte[] random = new byte[] { (byte)0x00, (byte)0x7f, (byte)0x80, (byte)0xff, 0x12, 0x34 };
        assertArrayEquals(random, Hash.fromHex(Hash.toHex(random)));
    }

    // ----------------------------------------------------------------
    // Keccak-256 (Ethereum) and SHA3-256 (FIPS 202) share the sponge and
    // differ only in the padding byte, so each gets its own known answers.

    @Test
    void keccak256_emptyAndShort() {
        // the Keccak-256 of the empty string every Ethereum client prints
        assertEquals("c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470",
                Hash.toHex(Hash.keccak256(new byte[0])));
        assertEquals("4e03657aea45a94fc7d47ba826c8d667c0d1e6e33a64a036ec44f58fa12d6c45",
                Hash.toHex(Hash.keccak256(ascii("abc"))));
        assertEquals("5f16f4c7f149ac4f9510d9cf8cf384038ad348b3bcdc01915f95de12df9d1b02",
                Hash.toHex(Hash.keccak256(ascii("testing"))));
        assertEquals("4d741b6f1eb29cb2a9b9911c82f56fa8d73b04959d3d9d222895df6c0b28aa15",
                Hash.toHex(Hash.keccak256(ascii("The quick brown fox jumps over the lazy dog"))));
    }

    @Test
    void keccak256_isNotSha3() {
        // FIPS 202 appendix vectors: the 0x06 domain byte changes everything
        assertEquals("a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a",
                Hash.toHex(Hash.create(Hash.SHA3_256).digest(new byte[0])));
        assertEquals("3a985da74fe225b2045c172d6bd390bd855f086e3e9d525b46bfe24511431532",
                Hash.toHex(Hash.create(Hash.SHA3_256).digest(ascii("abc"))));
        assertEquals(32, Hash.create(Hash.KECCAK256).digestLength());
    }

    @Test
    void sha3_256_millionA_multiBlock() {
        byte[] data = new byte[1_000_000];
        for (int i = 0; i < data.length; i++) data[i] = 'a';
        // NIST SHA3-256("a" * 1_000_000): over seven thousand sponge blocks
        assertEquals("5c8875ae474a3634ba4fd55ec85bffd661f32aca75c6d699d0cdcb6c115891c1",
                Hash.toHex(Hash.create(Hash.SHA3_256).digest(data)));
    }

    @Test
    void keccak256_streamingMatchesOneShotAcrossBlockBoundaries() {
        // 1000 bytes is seven full 136-byte blocks and a partial one; feed it
        // in chunk sizes that straddle every boundary
        byte[] data = new byte[1000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i * 7 + 3);
        byte[] expected = Hash.keccak256(data);
        for (int chunk : new int[] { 1, 5, 135, 136, 137, 300 }) {
            Hash h = Hash.create(Hash.KECCAK256);
            for (int off = 0; off < data.length; off += chunk) {
                h.update(data, off, Math.min(chunk, data.length - off));
            }
            assertArrayEquals(expected, h.digest(), "chunk " + chunk);
        }
        Hash byteAtATime = Hash.create(Hash.KECCAK256);
        for (byte b : data) byteAtATime.update(b);
        assertArrayEquals(expected, byteAtATime.digest());
    }

    @Test
    void hmacKeccak256_usesTheSpongeRate() {
        // HMAC over a sponge pads the key to the rate (136 bytes); a key of
        // exactly that length is used as it is, one byte longer is hashed first
        byte[] key136 = new byte[136];
        byte[] key137 = new byte[137];
        for (int i = 0; i < key137.length; i++) key137[i] = (byte) i;
        System.arraycopy(key137, 0, key136, 0, 136);
        byte[] msg = ascii("message");
        byte[] direct = Hmac.create(Hash.KECCAK256, key136).doFinal(msg);
        byte[] viaHashedKey = Hmac.create(Hash.KECCAK256, Hash.keccak256(key137)).doFinal(msg);
        assertArrayEquals(viaHashedKey, Hmac.create(Hash.KECCAK256, key137).doFinal(msg));
        assertFalse(java.util.Arrays.equals(direct, viaHashedKey));
    }
}
