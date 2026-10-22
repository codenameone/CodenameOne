/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
 * Distributed under the same terms as the rest of Codename One.
 */
package com.codename1.security;

import com.codename1.util.BigInteger;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// The public point EcKeys derives, against the JDK's, for random keys and
/// for the scalars at the edges of the ladder.
class EcKeysLadderTest {
    private static final String N =
            "ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551";
    private static final String P =
            "ffffffff00000001000000000000000000000000ffffffffffffffffffffffff";
    private static final String GX =
            "6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296";
    private static final String GY =
            "4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5";

    @Test
    void matchesTheJdkForRandomKeys() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        for (int iter = 0; iter < 16; iter++) {
            KeyPair kp = kpg.generateKeyPair();
            java.math.BigInteger d = ((ECPrivateKey) kp.getPrivate()).getS();
            java.security.spec.ECPoint w = ((ECPublicKey) kp.getPublic()).getW();
            BigInteger[] q = EcKeys.multiply(new BigInteger(d.toString(16), 16));
            assertNotNull(q);
            assertEquals(w.getAffineX().toString(16), q[0].toString(16));
            assertEquals(w.getAffineY().toString(16), q[1].toString(16));
        }
    }

    @Test
    void handlesTheEdgesOfTheScalarRange() {
        BigInteger[] one = EcKeys.multiply(BigInteger.ONE);
        assertEquals(GX, pad(one[0]));
        assertEquals(GY, pad(one[1]));
        BigInteger n = new BigInteger(N, 16);
        BigInteger[] minusOne = EcKeys.multiply(n.subtract(BigInteger.ONE));
        assertEquals(GX, pad(minusOne[0]));
        assertEquals(pad(new BigInteger(P, 16).subtract(new BigInteger(GY, 16))),
                pad(minusOne[1]));
        assertNull(EcKeys.multiply(n), "n * G is the point at infinity");
        // 2G and 3G = 2G + G, which an affine ladder handles as special cases.
        BigInteger[] two = EcKeys.multiply(BigInteger.valueOf(2));
        assertEquals("7cf27b188d034f7e8a52380304b51ac3c08969e277f21b35a60b48fc47669978",
                pad(two[0]));
        BigInteger[] three = EcKeys.multiply(BigInteger.valueOf(3));
        assertEquals("5ecbe4d1a6330a44c8f7ef951d4bf165e6c6b721efada985fb41661bc6e7fd6c",
                pad(three[0]));
    }

    private static String pad(BigInteger v) {
        String hex = v.toString(16);
        while (hex.length() < 64) {
            hex = "0" + hex;
        }
        return hex;
    }
}
