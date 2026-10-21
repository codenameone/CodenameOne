/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.security;

import org.junit.jupiter.api.Test;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// The curve arithmetic against RFC 6979 and the JDK.
class P256Test {
    private static byte[] hex(String text) {
        byte[] out = new byte[text.length() / 2];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = (byte) Integer.parseInt(text.substring(iter * 2, iter * 2 + 2), 16);
        }
        return out;
    }

    /// RFC 6979 A.2.5, P-256 with SHA-256, message "sample".
    @Test
    void rfc6979Vector() throws Exception {
        byte[] x = hex("c9afa9d845ba75166b5c215767b1d6934e50c3db36e89b127b8a622b120f6721");
        byte[] spki = P256.publicKey(x);
        assertArrayEquals(hex("60fed4ba255a9d31c961eb74c6356d68c049b8923b61fa6ce669622e60f29fb6"),
                Arrays.copyOfRange(spki, spki.length - 64, spki.length - 32));
        assertArrayEquals(hex("7903fe1008b8bc99a41ae9e95628bc64f2f1b20c2d7e9f5177a3c294d4462299"),
                Arrays.copyOfRange(spki, spki.length - 32, spki.length));
        byte[] sig = P256.sign(x, "sample".getBytes("UTF-8"));
        assertArrayEquals(hex("efd48b2aacb6a8fd1140dd9cd45e81d69d2c877b56aaf991c34d0ea84eaf3716"
                + "f7cb1c942d657c41d436c7a1b6e29f65f3e900dbb9aff4064dc4ab2f843acda8"), sig);
    }

    /// A key the JDK made has the public point this class computes, and the
    /// SubjectPublicKeyInfo this class writes is one the JDK reads.
    @Test
    void agreesWithTheJdk() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        for (int round = 0; round < 5; round++) {
            java.security.KeyPair pair = generator.generateKeyPair();
            byte[] d = toFixed(((ECPrivateKey) pair.getPrivate()).getS().toByteArray());
            byte[] mine = P256.publicKey(d);
            assertArrayEquals(pair.getPublic().getEncoded(), mine);
            ECPublicKey read = (ECPublicKey) KeyFactory.getInstance("EC")
                    .generatePublic(new X509EncodedKeySpec(mine));
            assertEquals(((ECPublicKey) pair.getPublic()).getW(), read.getW());
        }
    }

    private static byte[] toFixed(byte[] raw) {
        byte[] out = new byte[32];
        int copy = Math.min(raw.length, 32);
        System.arraycopy(raw, raw.length - copy, out, 32 - copy, copy);
        return out;
    }
}
