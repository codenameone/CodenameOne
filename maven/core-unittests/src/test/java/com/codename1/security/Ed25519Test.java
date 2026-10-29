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

/// Ed25519 against RFC 8032 section 7.1, plus the encodings the RFC requires
/// a verifier to refuse.
class Ed25519Test {

    private static void vector(String secret, String pub, String msg, String sig) {
        byte[] sk = Hash.fromHex(secret);
        byte[] m = Hash.fromHex(msg);
        assertEquals(pub, Hash.toHex(Ed25519.publicKey(sk)));
        assertEquals(sig, Hash.toHex(Ed25519.sign(sk, m)));
        assertTrue(Ed25519.verify(m, Hash.fromHex(sig), Hash.fromHex(pub)));
    }

    @Test
    void rfc8032Test1EmptyMessage() {
        vector("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60",
                "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a",
                "",
                "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b");
    }

    @Test
    void rfc8032Test2OneByte() {
        vector("4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb",
                "3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c",
                "72",
                "92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da085ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00");
    }

    @Test
    void tamperingAndNonCanonicalEncodingsAreRefused() {
        byte[] sk = Hash.fromHex("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60");
        byte[] pub = Ed25519.publicKey(sk);
        byte[] msg = {1, 2, 3};
        byte[] sig = Ed25519.sign(sk, msg);
        assertTrue(Ed25519.verify(msg, sig, pub));
        byte[] other = {1, 2, 4};
        assertFalse(Ed25519.verify(other, sig, pub));
        // S + L verifies the same equation and must still be refused
        byte[] malleated = sig.clone();
        int carry = 0;
        byte[] l = Hash.fromHex("edd3f55c1a631258d69cf7a2def9de1400000000000000000000000000000010");
        for (int i = 0; i < 32; i++) {
            int v = (malleated[32 + i] & 0xff) + (l[i] & 0xff) + carry;
            malleated[32 + i] = (byte) v;
            carry = v >> 8;
        }
        assertFalse(Ed25519.verify(msg, malleated, pub));
        // a key whose y is p itself, not reduced
        byte[] yp = Hash.fromHex("edffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f");
        assertFalse(Ed25519.verify(msg, sig, yp));
    }
}
