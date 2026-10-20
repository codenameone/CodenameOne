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

import com.codename1.impl.EcP256Codec;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// HKDF against RFC 5869's own vectors, P-256 ECDH and ECDSA, and the codec
/// the native ports convert keys through.
class KeyAgreementTest extends UITestBase {

    private static byte[] hex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    @Test
    void hkdfRfc5869Case1() {
        byte[] okm = Hkdf.derive("SHA-256", hex("000102030405060708090a0b0c"),
                hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"),
                hex("f0f1f2f3f4f5f6f7f8f9"), 42);
        assertArrayEquals(hex("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf"
                + "34007208d5b887185865"), okm);
        assertArrayEquals(hex("077709362c2e32df0ddc3f0dc47bba6390b6c73bb50f9c3122ec844ad7c2b3e5"),
                Hkdf.extract("SHA-256", hex("000102030405060708090a0b0c"),
                        hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b")));
    }

    @Test
    void hkdfRfc5869Case3EmptySaltAndInfo() {
        byte[] okm = Hkdf.derive("SHA-256", null,
                hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"), null, 42);
        assertArrayEquals(hex("8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d"
                + "9d201395faa4b61a96c8"), okm);
    }

    @Test
    void hkdfRejectsOverlongOutput() {
        assertThrows(CryptoException.class, () -> Hkdf.derive("SHA-256", null, new byte[16], null, 255 * 32 + 1));
    }

    @Test
    void ecdhBothSidesAgree() {
        KeyPair alice = KeyGenerator.ec(256);
        KeyPair bob = KeyGenerator.ec(256);
        byte[] a = KeyAgreement.ecdh(alice.getPrivateKey(), bob.getPublicKey());
        byte[] b = KeyAgreement.ecdh(bob.getPrivateKey(), alice.getPublicKey());
        assertEquals(32, a.length);
        assertArrayEquals(a, b);
        KeyPair eve = KeyGenerator.ec(256);
        assertFalse(java.util.Arrays.equals(a, KeyAgreement.ecdh(eve.getPrivateKey(), bob.getPublicKey())));
    }

    @Test
    void ecdhRejectsRsaKeys() {
        KeyPair ec = KeyGenerator.ec(256);
        PublicKey rsaLabelled = PublicKey.fromX509(PublicKey.RSA, ec.getPublicKey().getEncoded());
        assertThrows(CryptoException.class, () -> KeyAgreement.ecdh(ec.getPrivateKey(), rsaLabelled));
    }

    @Test
    void onlyP256IsOffered() {
        assertThrows(CryptoException.class, () -> KeyGenerator.ec(384));
    }

    @Test
    void ecdsaSignsWithGeneratedKeys() {
        KeyPair kp = KeyGenerator.ec(256);
        byte[] data = "signed prekey".getBytes();
        byte[] sig = Signature.sign(Signature.SHA256_WITH_ECDSA, kp.getPrivateKey(), data);
        assertTrue(Signature.verify(Signature.SHA256_WITH_ECDSA, kp.getPublicKey(), data, sig));
        data[0] ^= 1;
        assertFalse(Signature.verify(Signature.SHA256_WITH_ECDSA, kp.getPublicKey(), data, sig));
    }

    @Test
    void pointRoundTripsThroughThePublicKey() {
        KeyPair kp = KeyGenerator.ec(256);
        byte[] point = kp.getPublicKey().toEcPoint();
        assertEquals(65, point.length);
        assertEquals(0x04, point[0]);
        PublicKey back = PublicKey.ecFromPoint(point);
        assertArrayEquals(kp.getPublicKey().getEncoded(), back.getEncoded());
        assertThrows(CryptoException.class, () -> PublicKey.ecFromPoint(new byte[64]));
    }

    @Test
    void codecPkcs8CarriesThePointAndRoundTrips() {
        KeyPair kp = KeyGenerator.ec(256);
        byte[][] parts = EcP256Codec.fromPkcs8(kp.getPrivateKey().getEncoded());
        assertNotNull(parts[1], "generated keys carry their public point");
        assertArrayEquals(kp.getPublicKey().toEcPoint(), parts[1]);
        assertArrayEquals(kp.getPrivateKey().getEncoded(), EcP256Codec.pkcs8FromScalar(parts[0], parts[1]));
    }

    @Test
    void codecReadsTheJdksOwnPkcs8() throws Exception {
        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("EC");
        kpg.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
        java.security.KeyPair jdk = kpg.generateKeyPair();
        byte[] scalar = EcP256Codec.fromPkcs8(jdk.getPrivate().getEncoded())[0];
        java.math.BigInteger s = ((java.security.interfaces.ECPrivateKey) jdk.getPrivate()).getS();
        assertEquals(s, new java.math.BigInteger(1, scalar));
    }

    @Test
    void signatureFormsConvert() {
        KeyPair kp = KeyGenerator.ec(256);
        for (int i = 0; i < 20; i++) {
            byte[] der = Signature.sign(Signature.SHA256_WITH_ECDSA, kp.getPrivateKey(), new byte[] {(byte) i});
            byte[] raw = EcP256Codec.signatureDerToRaw(der);
            assertEquals(64, raw.length);
            assertArrayEquals(der, EcP256Codec.signatureRawToDer(raw));
        }
    }
}
