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

/// Known answers for secp256k1: the generator multiples every reference lists,
/// the RFC 6979 vectors the Bitcoin test suites share (private key, message
/// hashed once with SHA-256, compact r || s), and the address Ethereum
/// derives from a published test key. The rest checks the algebra against
/// itself: recover(sign(h)) is the key, a tampered signature fails, the
/// high-S form is rejected and normalised, and BIP32-style tweaks agree on
/// both sides of the curve.
class Secp256k1Test {

    private static final String GX = "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
    private static final String GY = "483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8";
    private static final String N_MINUS_1 =
            "fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364140";

    private static byte[] hex(String s) {
        return Hash.fromHex(s);
    }

    private static String hex(byte[] b) {
        return Hash.toHex(b);
    }

    private static byte[] key(long v) {
        byte[] k = new byte[32];
        for (int i = 0; i < 8; i++) {
            k[31 - i] = (byte) (v >>> (8 * i));
        }
        return k;
    }

    private static byte[] ascii(String s) {
        try {
            return s.getBytes("US-ASCII");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    // ----------------------------------------------------------------
    // keys

    @Test
    void publicKeyOfOneIsTheGenerator() {
        assertEquals("04" + GX + GY, hex(Secp256k1.publicKey(key(1), false)));
        assertEquals("02" + GX, hex(Secp256k1.publicKey(key(1), true)));
    }

    @Test
    void publicKeyOfTwoAndThree() {
        assertEquals("04c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5"
                + "1ae168fea63dc339a3c58419466ceaeef7f632653266d0e1236431a950cfe52a",
                hex(Secp256k1.publicKey(key(2), false)));
        assertEquals("04f9308a019258c31049344f85f89d5229b531c845836f99b08601f113bce036f9"
                + "388f7b0f632de8140fe337e62a37f3566500a99934c2231b6cb9fd7584b8e672",
                hex(Secp256k1.publicKey(key(3), false)));
    }

    @Test
    void publicKeyOfNMinusOneIsMinusG() {
        // -G has G's x and p - G.y, which is odd, so the compressed prefix is 03
        assertEquals("04" + GX + "b7c52588d95c3b9aa25b0403f1eef75702e84bb7597aabe663b82f6f04ef2777",
                hex(Secp256k1.publicKey(hex(N_MINUS_1), false)));
        assertEquals("03" + GX, hex(Secp256k1.publicKey(hex(N_MINUS_1), true)));
    }

    @Test
    void ethereumAddressOfPublishedTestKey() {
        byte[] priv = hex("4c0883a69102937d6231471b5dbb6204fe5129617082792ae468d01a3f362318");
        byte[] pub = Secp256k1.publicKey(priv, false);
        assertEquals(65, pub.length);
        byte[] xy = new byte[64];
        System.arraycopy(pub, 1, xy, 0, 64);
        byte[] digest = Hash.keccak256(xy);
        byte[] address = new byte[20];
        System.arraycopy(digest, 12, address, 0, 20);
        assertEquals("2c7536e3605d9c16a7a3d7b1898e529396a65c23", hex(address));
    }

    @Test
    void privateKeyValidity() {
        assertFalse(Secp256k1.isValidPrivateKey(null));
        assertFalse(Secp256k1.isValidPrivateKey(new byte[31]));
        assertFalse(Secp256k1.isValidPrivateKey(new byte[32]));
        assertTrue(Secp256k1.isValidPrivateKey(key(1)));
        assertTrue(Secp256k1.isValidPrivateKey(hex(N_MINUS_1)));
        assertFalse(Secp256k1.isValidPrivateKey(
                hex("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141")));
        assertFalse(Secp256k1.isValidPrivateKey(
                hex("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")));
        assertThrows(CryptoException.class, () -> Secp256k1.publicKey(new byte[32], true));
        assertThrows(CryptoException.class, () -> Secp256k1.publicKey(
                hex("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141"), true));
        assertThrows(CryptoException.class, () -> Secp256k1.sign(new byte[32], new byte[32]));
    }

    @Test
    void publicKeyParsingAndConversion() {
        byte[] priv = hex("4c0883a69102937d6231471b5dbb6204fe5129617082792ae468d01a3f362318");
        byte[] full = Secp256k1.publicKey(priv, false);
        byte[] compressed = Secp256k1.publicKey(priv, true);
        assertTrue(Secp256k1.isValidPublicKey(full));
        assertTrue(Secp256k1.isValidPublicKey(compressed));
        assertArrayEquals(compressed, Secp256k1.convertPublicKey(full, true));
        assertArrayEquals(full, Secp256k1.convertPublicKey(compressed, false));

        // a point off the curve is refused in both forms
        byte[] offCurve = full.clone();
        offCurve[64] ^= 1;
        assertFalse(Secp256k1.isValidPublicKey(offCurve));
        assertThrows(CryptoException.class, () -> Secp256k1.convertPublicKey(offCurve, true));
        byte[] noSuchX = compressed.clone();
        noSuchX[0] = 2;
        for (int i = 1; i < 33; i++) {
            noSuchX[i] = 0;
        }
        noSuchX[32] = 5; // x = 5: 5^3 + 7 = 132 is not a square mod p
        assertFalse(Secp256k1.isValidPublicKey(noSuchX));
        assertFalse(Secp256k1.isValidPublicKey(null));
        assertFalse(Secp256k1.isValidPublicKey(new byte[33]));
        assertFalse(Secp256k1.isValidPublicKey(new byte[64]));
    }

    // ----------------------------------------------------------------
    // RFC 6979 vectors (secp256k1, SHA-256, from the Bitcoin test suites)

    private static void checkRfc6979(String privHex, String message, String rs) {
        byte[] priv = hex(privHex);
        byte[] hash = Hash.sha256(ascii(message));
        Secp256k1.EcdsaSignature sig = Secp256k1.sign(hash, priv);
        assertEquals(rs, hex(sig.toCompact()), message);
        assertTrue(sig.isLowS());
        byte[] pub = Secp256k1.publicKey(priv, true);
        assertTrue(Secp256k1.verify(hash, sig, pub), message);
        assertArrayEquals(pub, Secp256k1.recoverPublicKey(hash, sig, true), message);
    }

    @Test
    void rfc6979_keyOne_satoshiNakamoto() {
        checkRfc6979("0000000000000000000000000000000000000000000000000000000000000001",
                "Satoshi Nakamoto",
                "934b1ea10a4b3c1757e2b0c017d0b6143ce3c9a7e6a4a49860d7a6ab210ee3d8"
                + "2442ce9d2b916064108014783e923ec36b49743e2ffa1c4496f01a512aafd9e5");
    }

    @Test
    void rfc6979_keyNMinusOne_satoshiNakamoto() {
        checkRfc6979(N_MINUS_1,
                "Satoshi Nakamoto",
                "fd567d121db66e382991534ada77a6bd3106f0a1098c231e47993447cd6af2d0"
                + "6b39cd0eb1bc8603e159ef5c20a5c8ad685a45b06ce9bebed3f153d10d93bed5");
    }

    @Test
    void rfc6979_keyOne_bladeRunner() {
        checkRfc6979("0000000000000000000000000000000000000000000000000000000000000001",
                "All those moments will be lost in time, like tears in rain. Time to die...",
                "8600dbd41e348fe5c9465ab92d23e3db8b98b873beecd930736488696438cb6b"
                + "547fe64427496db33bf66019dacbf0039c04199abb0122918601db38a72cfc21");
    }

    @Test
    void rfc6979_keyOne_einstein() {
        checkRfc6979("0000000000000000000000000000000000000000000000000000000000000001",
                "Everything should be made as simple as possible, but not simpler.",
                "33a69cd2065432a30f3d1ce4eb0d59b8ab58c74f27c41a7fdb5696ad4e6108c9"
                + "6f807982866f785d3f6418d24163ddae117b7db4d5fdf0071de069fa54342262");
    }

    @Test
    void rfc6979_alanTuring() {
        checkRfc6979("f8b8af8ce3c7cca5e300d33939540c10d45ce001b8f252bfbc57ba0342904181",
                "Alan Turing",
                "7063ae83e7f62bbb171798131b4a0564b956930092b33b07b395615d9ec7e15c"
                + "58dfcc1e00a35e1572f366ffe34ba0fc47db1e7189759b9fb233c5b05ab388ea");
    }

    // ----------------------------------------------------------------
    // sign / verify / recover against each other

    @Test
    void signIsDeterministicAndRecoverable() {
        byte[] priv = hex("4c0883a69102937d6231471b5dbb6204fe5129617082792ae468d01a3f362318");
        byte[] pub = Secp256k1.publicKey(priv, false);
        for (int i = 0; i < 8; i++) {
            byte[] hash = Hash.keccak256(new byte[] { (byte) i, 42, (byte) (i * 31) });
            Secp256k1.EcdsaSignature sig = Secp256k1.sign(hash, priv);
            assertArrayEquals(sig.toCompact(), Secp256k1.sign(hash, priv).toCompact());
            assertEquals(sig.recoveryId(), Secp256k1.sign(hash, priv).recoveryId());
            assertTrue(sig.recoveryId() == 0 || sig.recoveryId() == 1);
            assertTrue(sig.isLowS());
            assertTrue(Secp256k1.verify(hash, sig, pub));
            assertTrue(Secp256k1.verify(hash, sig, Secp256k1.convertPublicKey(pub, true)));
            assertArrayEquals(pub, Secp256k1.recoverPublicKey(hash, sig, false));
            // the other recovery id names the other candidate key, not ours
            Secp256k1.EcdsaSignature other = new Secp256k1.EcdsaSignature(
                    sig.r(), sig.s(), sig.recoveryId() ^ 1);
            byte[] otherKey = Secp256k1.recoverPublicKey(hash, other, false);
            if (otherKey != null) {
                assertFalse(java.util.Arrays.equals(pub, otherKey));
            }
        }
    }

    @Test
    void verifyRejectsTampering() {
        byte[] priv = key(12345);
        byte[] pub = Secp256k1.publicKey(priv, true);
        byte[] hash = Hash.sha256(ascii("pay 1 ETH to Alice"));
        Secp256k1.EcdsaSignature sig = Secp256k1.sign(hash, priv);
        assertTrue(Secp256k1.verify(hash, sig, pub));

        byte[] r = sig.r();
        r[31] ^= 1;
        assertFalse(Secp256k1.verify(hash, new Secp256k1.EcdsaSignature(r, sig.s(), -1), pub));
        byte[] s = sig.s();
        s[0] ^= 1;
        assertFalse(Secp256k1.verify(hash, new Secp256k1.EcdsaSignature(sig.r(), s, -1), pub));
        byte[] otherHash = Hash.sha256(ascii("pay 2 ETH to Alice"));
        assertFalse(Secp256k1.verify(otherHash, sig, pub));
        assertFalse(Secp256k1.verify(hash, sig, Secp256k1.publicKey(key(12346), true)));

        // r or s outside [1, n-1] can never verify
        assertFalse(Secp256k1.verify(hash, new Secp256k1.EcdsaSignature(new byte[32], sig.s(), -1), pub));
        assertFalse(Secp256k1.verify(hash, new Secp256k1.EcdsaSignature(sig.r(), new byte[32], -1), pub));
        assertFalse(Secp256k1.verify(hash, new Secp256k1.EcdsaSignature(
                sig.r(), hex("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141"), -1), pub));
        assertNull(Secp256k1.recoverPublicKey(hash,
                new Secp256k1.EcdsaSignature(new byte[32], sig.s(), 0), true));

        assertThrows(CryptoException.class, () -> Secp256k1.verify(new byte[31], sig, pub));
        assertThrows(CryptoException.class, () -> Secp256k1.verify(hash, sig, new byte[33]));
        assertThrows(CryptoException.class, () -> Secp256k1.recoverPublicKey(hash,
                new Secp256k1.EcdsaSignature(sig.r(), sig.s(), -1), true));
    }

    @Test
    void highSIsRejectedByVerifyAcceptedByRecoverAndNormalised() {
        byte[] priv = key(777);
        byte[] pub = Secp256k1.publicKey(priv, true);
        byte[] hash = Hash.sha256(ascii("high s"));
        Secp256k1.EcdsaSignature low = Secp256k1.sign(hash, priv);
        assertTrue(low.isLowS());
        assertSame(low, low.normalize());

        // s' = n - s is the same signature in its other form
        byte[] n = hex("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141");
        byte[] s = low.s();
        byte[] high = new byte[32];
        int borrow = 0;
        for (int i = 31; i >= 0; i--) {
            int d = (n[i] & 0xff) - (s[i] & 0xff) - borrow;
            high[i] = (byte) d;
            borrow = d < 0 ? 1 : 0;
        }
        Secp256k1.EcdsaSignature highSig = new Secp256k1.EcdsaSignature(low.r(), high, low.recoveryId() ^ 1);
        assertFalse(highSig.isLowS());
        assertFalse(Secp256k1.verify(hash, highSig, pub));
        assertArrayEquals(pub, Secp256k1.recoverPublicKey(hash, highSig, true));

        Secp256k1.EcdsaSignature normalised = highSig.normalize();
        assertTrue(normalised.isLowS());
        assertArrayEquals(low.toCompact(), normalised.toCompact());
        assertEquals(low.recoveryId(), normalised.recoveryId());
        assertTrue(Secp256k1.verify(hash, normalised, pub));
        assertEquals(-1, new Secp256k1.EcdsaSignature(low.r(), high, -1).normalize().recoveryId());
    }

    @Test
    void signatureEncoding() {
        Secp256k1.EcdsaSignature sig = Secp256k1.sign(Hash.sha256(ascii("x")), key(9));
        Secp256k1.EcdsaSignature again = Secp256k1.EcdsaSignature.fromCompact(sig.toCompact(), sig.recoveryId());
        assertArrayEquals(sig.r(), again.r());
        assertArrayEquals(sig.s(), again.s());
        assertEquals(sig.recoveryId(), again.recoveryId());
        assertThrows(CryptoException.class, () -> new Secp256k1.EcdsaSignature(new byte[31], new byte[32], 0));
        assertThrows(CryptoException.class, () -> new Secp256k1.EcdsaSignature(new byte[32], new byte[32], 4));
        assertThrows(CryptoException.class, () -> Secp256k1.EcdsaSignature.fromCompact(new byte[63], 0));
        // accessors hand out copies
        byte[] r = sig.r();
        r[0] ^= 0x55;
        assertFalse(java.util.Arrays.equals(r, sig.r()));
    }

    // ----------------------------------------------------------------
    // tweaks and point addition (BIP32)

    @Test
    void tweaksAgreeOnBothSidesOfTheCurve() {
        assertArrayEquals(key(2), Secp256k1.privateKeyTweakAdd(key(1), key(1)));
        assertArrayEquals(Secp256k1.publicKey(key(2), true),
                Secp256k1.publicKeyTweakAdd(Secp256k1.publicKey(key(1), true), key(1), true));
        assertArrayEquals(Secp256k1.publicKey(key(3), false),
                Secp256k1.publicKeyAdd(Secp256k1.publicKey(key(1), true), Secp256k1.publicKey(key(2), false), false));

        byte[] priv = hex("4c0883a69102937d6231471b5dbb6204fe5129617082792ae468d01a3f362318");
        byte[] tweak = Hash.sha256(ascii("child 0"));
        byte[] childPriv = Secp256k1.privateKeyTweakAdd(priv, tweak);
        assertArrayEquals(Secp256k1.publicKey(childPriv, true),
                Secp256k1.publicKeyTweakAdd(Secp256k1.publicKey(priv, false), tweak, true));

        // wrapping past n
        assertArrayEquals(key(1), Secp256k1.privateKeyTweakAdd(hex(N_MINUS_1), key(2)));
        assertThrows(CryptoException.class, () -> Secp256k1.privateKeyTweakAdd(hex(N_MINUS_1), key(1)));
        assertThrows(CryptoException.class, () -> Secp256k1.publicKeyTweakAdd(
                Secp256k1.publicKey(hex(N_MINUS_1), true), key(1), true));
        assertThrows(CryptoException.class, () -> Secp256k1.publicKeyAdd(
                Secp256k1.publicKey(key(1), true), Secp256k1.publicKey(hex(N_MINUS_1), true), true));
        assertThrows(CryptoException.class, () -> Secp256k1.privateKeyTweakAdd(key(1),
                hex("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141")));
    }
}
