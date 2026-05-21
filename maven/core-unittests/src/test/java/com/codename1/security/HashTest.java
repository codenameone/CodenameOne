/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
 * Distributed under the same terms as the rest of Codename One.
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
}
