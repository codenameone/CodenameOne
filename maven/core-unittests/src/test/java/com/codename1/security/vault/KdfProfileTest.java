/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security.vault;

import com.codename1.junit.UITestBase;
import com.codename1.security.Hash;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Test vectors for the portable password KDF, and the bounds that stop a stored envelope
/// choosing how hard it is to attack.
///
/// The vectors matter more than they look. The whole claim of this package is that a password
/// typed on Android and the same password typed in a browser derive the same key; the only way to
/// hold that claim is to pin the bytes and check every implementation against them. The four
/// PBKDF2-HMAC-SHA256 vectors below are the standard published ones and were cross-checked
/// against an independent implementation (Python's `hashlib.pbkdf2_hmac`) before being written
/// here, so a port that matches them matches the rest of the world and not just us.
///
/// A port implementing [com.codename1.impl.CodenameOneImplementation#pbkdf2] should run these
/// same inputs through its native path and compare.
class KdfProfileTest extends UITestBase {

    private static String hex(byte[] data) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < data.length; iter++) {
            int v = data[iter] & 0xff;
            b.append("0123456789abcdef".charAt(v >>> 4));
            b.append("0123456789abcdef".charAt(v & 0x0f));
        }
        return b.toString();
    }

    @Test
    void rfc8018VectorOneIteration() {
        assertEquals("120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b",
                hex(KdfProfile.pbkdf2Portable("password".getBytes(), "salt".getBytes(), 1, 32)));
    }

    @Test
    void rfc8018VectorTwoIterations() {
        assertEquals("ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43",
                hex(KdfProfile.pbkdf2Portable("password".getBytes(), "salt".getBytes(), 2, 32)));
    }

    @Test
    void rfc8018VectorManyIterations() {
        assertEquals("c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a",
                hex(KdfProfile.pbkdf2Portable("password".getBytes(), "salt".getBytes(), 4096, 32)));
    }

    @Test
    void rfc8018VectorLongOutputSpansBlocks() {
        // Forty bytes is more than one SHA-256 block, which exercises the block counter. An
        // implementation that got the counter's byte order wrong passes every 32 byte vector and
        // fails this one.
        assertEquals("348c89dbcbd32b2f32d814b8116e84cf2b17347ebc1800181c4e2a1fb8dd53e1"
                        + "c635518c7dac47e9",
                hex(KdfProfile.pbkdf2Portable("passwordPASSWORDpassword".getBytes(),
                        "saltSALTsaltSALTsaltSALTsaltSALTsalt".getBytes(), 4096, 40)));
    }

    @Test
    void platformDerivationAgreesWithThePortableOne() {
        // A port that supplies a native PBKDF2 must produce the same bytes as the fallback, or a
        // vault written on it cannot be opened anywhere else. When the port has no native path
        // this returns null and there is nothing to compare, which is itself the documented
        // contract.
        byte[] platform = com.codename1.io.Util.pbkdf2(Hash.SHA256, "password".getBytes(),
                "salt".getBytes(), 4096, 32);
        if (platform != null) {
            assertEquals("c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a",
                    hex(platform));
        }
    }

    @Test
    void unicodePasswordEncodingIsPinned() {
        // The encoding of a non-ASCII password is part of the wire contract, not an
        // implementation detail: a platform whose UTF-8 differs derives a different key and the
        // user simply cannot log in on their other device. The expected value was produced from
        // the same code points encoded as UTF-8 by an independent implementation.
        // Written as escapes because a Java source file in this tree must be ASCII: the string
        // is "p", U+00E4, "ssw", U+00F6, "rd", then U+1F512 as its surrogate pair.
        char[] password = {'p', '\u00e4', 's', 's', 'w', '\u00f6', 'r', 'd', '\ud83d', '\udd12'};
        byte[] salt = "saltsaltsaltsalt".getBytes();
        assertEquals("a4399b80b4e16c0f4bd67ae67bc03782dad8d1abf530fea4f32689e440ee690e",
                hex(KdfProfile.pbkdf2(KdfProfile.MIN_ITERATIONS).derive(password, salt, 32)));
    }

    @Test
    void constructorClampsUpwardsNotDown() {
        assertEquals(KdfProfile.MIN_ITERATIONS, KdfProfile.pbkdf2(1000).getIterations());
        assertEquals(KdfProfile.MAX_ITERATIONS,
                KdfProfile.pbkdf2(Integer.MAX_VALUE).getIterations());
        assertEquals(KdfProfile.DEFAULT_ITERATIONS, KdfProfile.current().getIterations());
    }

    @Test
    void storedProfileIsValidatedNotClamped() {
        // The asymmetry that matters: a caller that asks for too few iterations gets corrected
        // upward, and a stored envelope that declares too few is refused. Clamping the stored one
        // would mean an attacker who rewrote the count to 1 got a fast derivation and a working
        // decryption.
        try {
            KdfProfile.forStored(KdfProfile.PBKDF2_HMAC_SHA256, 1000);
            fail("expected a stored profile below the floor to be refused");
        } catch (VaultException e) {
            assertEquals(VaultError.UNSUPPORTED_FORMAT, e.getError());
        }
        try {
            KdfProfile.forStored(KdfProfile.PBKDF2_HMAC_SHA256, KdfProfile.MAX_ITERATIONS + 1);
            fail("expected a stored profile above the ceiling to be refused");
        } catch (VaultException e) {
            assertEquals(VaultError.UNSUPPORTED_FORMAT, e.getError());
        }
    }

    @Test
    void unknownKdfIsRefused() {
        try {
            KdfProfile.forStored(42, 600000);
            fail("expected an unknown KDF id to be refused");
        } catch (VaultException e) {
            assertEquals(VaultError.UNSUPPORTED_FORMAT, e.getError());
        }
    }

    @Test
    void anEmptyPasswordDerivesInsteadOfThrowing() {
        // Nothing in Vault or KdfProfile rejects an empty password, so the derivation has to
        // define it. RFC 2104 zero-pads a short HMAC key, which the portable implementation
        // does, and routing an empty password there is what keeps the JavaSE and Android native
        // paths -- where JCE refuses a zero-length key outright -- from being reached at all.
        //
        // What this does NOT cover, measured rather than assumed: the native path. This module
        // runs against the base CodenameOneImplementation, whose pbkdf2 answers null, so
        // derive() already fell back to the portable code here before that guard existed and
        // this test passed against the unfixed tree. The guard itself is covered by
        // JavaSEPortPbkdf2Test, beside the port that has the defect.
        byte[] derived = KdfProfile.pbkdf2(KdfProfile.MIN_ITERATIONS)
                .derive(new char[0], "salt".getBytes(), 32);
        assertEquals(32, derived.length);

        // And the answer is the portable one on every port, so a vault enrolled with an empty
        // password on one still opens on another.
        byte[] portable = KdfProfile.pbkdf2Portable(new byte[0], "salt".getBytes(),
                KdfProfile.MIN_ITERATIONS, 32);
        assertEquals(hex(portable), hex(derived));
    }

    @Test
    void needsUpgradeReportsAWeakerProfile() {
        assertTrue(KdfProfile.pbkdf2(KdfProfile.MIN_ITERATIONS).needsUpgrade());
        assertFalse(KdfProfile.current().needsUpgrade());
    }
}
