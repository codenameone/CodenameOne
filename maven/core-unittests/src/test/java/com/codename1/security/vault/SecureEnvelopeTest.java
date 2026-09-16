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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// The wire format, and everything it has to refuse.
///
/// Most of these are rejection tests. A format that two platforms exchange is only as good as its
/// parser: a reader that clamps an out-of-range field, tolerates trailing bytes or falls back to
/// an older rule when it meets a version it does not know has handed the decision about how
/// strongly the data is protected to whoever can write the bytes.
class SecureEnvelopeTest extends UITestBase {

    private static byte[] key(int fill) {
        byte[] k = new byte[32];
        for (int iter = 0; iter < k.length; iter++) {
            k[iter] = (byte) (fill + iter);
        }
        return k;
    }

    private static AssociatedData binding() {
        return AssociatedData.of("com.example.app", "vault1", "note-7", "record");
    }

    @Test
    void roundTrip() {
        byte[] plaintext = "the magic words are squeamish ossifrage".getBytes();
        byte[] sealed = SecureEnvelope.seal(key(1), "dk", 3, binding(), plaintext);
        SecureEnvelope parsed = SecureEnvelope.parse(sealed);
        assertEquals(SecureEnvelope.VERSION_1, parsed.getVersion());
        assertEquals(SecureEnvelope.SUITE_AES_256_GCM, parsed.getSuite());
        assertEquals("dk", parsed.getKeyId());
        assertEquals(3, parsed.getKeyVersion());
        assertArrayEquals(plaintext, parsed.open(key(1), binding()));
    }

    @Test
    void everySealUsesAFreshNonce() {
        // Two seals of identical plaintext under an identical key must not produce identical
        // bytes. A repeated nonce under one AES-GCM key is catastrophic rather than merely weak,
        // and there is no API here that lets a caller supply one, so this is the check that the
        // generated one is actually generated.
        byte[] a = SecureEnvelope.seal(key(1), "dk", 1, binding(), "same".getBytes());
        byte[] b = SecureEnvelope.seal(key(1), "dk", 1, binding(), "same".getBytes());
        assertFalse(java.util.Arrays.equals(a, b));
    }

    @Test
    void wrongKeyFailsAuthentication() {
        byte[] sealed = SecureEnvelope.seal(key(1), "dk", 1, binding(), "secret".getBytes());
        try {
            SecureEnvelope.parse(sealed).open(key(9), binding());
            fail("expected an authentication failure");
        } catch (VaultException e) {
            assertEquals(VaultError.AUTHENTICATION_FAILED, e.getError());
        }
    }

    @Test
    void wrongBindingFailsAuthentication() {
        // The whole point of the binding: ciphertext written for one record must not open as
        // another, even with the right key.
        byte[] sealed = SecureEnvelope.seal(key(1), "dk", 1, binding(), "secret".getBytes());
        AssociatedData elsewhere = AssociatedData.of("com.example.app", "vault1", "note-8", "record");
        try {
            SecureEnvelope.parse(sealed).open(key(1), elsewhere);
            fail("expected an authentication failure");
        } catch (VaultException e) {
            assertEquals(VaultError.AUTHENTICATION_FAILED, e.getError());
        }
    }

    @Test
    void tamperedCiphertextFailsAuthentication() {
        byte[] sealed = SecureEnvelope.seal(key(1), "dk", 1, binding(), "secret".getBytes());
        sealed[sealed.length - 1] ^= 0x01;
        try {
            SecureEnvelope.parse(sealed).open(key(1), binding());
            fail("expected an authentication failure");
        } catch (VaultException e) {
            assertEquals(VaultError.AUTHENTICATION_FAILED, e.getError());
        }
    }

    @Test
    void tamperedHeaderFailsAuthentication() {
        // The header is authenticated, not merely present. An attacker who rewrites the key
        // version -- or, in a password envelope, the iteration count -- has to fail the tag
        // rather than steer the decryption.
        byte[] sealed = SecureEnvelope.seal(key(1), "dk", 1, binding(), "secret".getBytes());
        int keyVersionOffset = sealed.length - 8 - 16 - "secret".getBytes().length;
        sealed[keyVersionOffset] ^= 0x01;
        try {
            SecureEnvelope.parse(sealed).open(key(1), binding());
            fail("expected an authentication failure");
        } catch (VaultException e) {
            assertEquals(VaultError.AUTHENTICATION_FAILED, e.getError());
        }
    }

    @Test
    void badMagicIsCorrupt() {
        byte[] sealed = SecureEnvelope.seal(key(1), "dk", 1, binding(), "x".getBytes());
        sealed[0] = 'X';
        assertEquals(VaultError.CORRUPT, errorOfParse(sealed));
    }

    @Test
    void truncatedIsCorrupt() {
        byte[] sealed = SecureEnvelope.seal(key(1), "dk", 1, binding(), "x".getBytes());
        byte[] cut = new byte[sealed.length - 3];
        System.arraycopy(sealed, 0, cut, 0, cut.length);
        assertEquals(VaultError.CORRUPT, errorOfParse(cut));
    }

    @Test
    void trailingBytesAreRefused() {
        // Not "at least this long". Bytes past the declared ciphertext are not covered by the
        // tag, so accepting them would leave a channel past every check in the parser.
        byte[] sealed = SecureEnvelope.seal(key(1), "dk", 1, binding(), "x".getBytes());
        byte[] extended = new byte[sealed.length + 1];
        System.arraycopy(sealed, 0, extended, 0, sealed.length);
        assertEquals(VaultError.CORRUPT, errorOfParse(extended));
    }

    @Test
    void futureVersionIsRefusedNotDowngraded() {
        byte[] sealed = SecureEnvelope.seal(key(1), "dk", 1, binding(), "x".getBytes());
        sealed[4] = 2;
        assertEquals(VaultError.UNSUPPORTED_FORMAT, errorOfParse(sealed));
    }

    @Test
    void unknownSuiteIsRefused() {
        byte[] sealed = SecureEnvelope.seal(key(1), "dk", 1, binding(), "x".getBytes());
        sealed[5] = 7;
        assertEquals(VaultError.UNSUPPORTED_FORMAT, errorOfParse(sealed));
    }

    @Test
    void implausibleCiphertextLengthIsRefusedBeforeAllocating() {
        byte[] sealed = SecureEnvelope.seal(key(1), "dk", 1, binding(), "x".getBytes());
        // Overwrite the declared ciphertext length with 2GB. A parser that allocated first would
        // die here rather than report.
        int at = sealed.length - 16 - "x".getBytes().length - 4;
        sealed[at] = 0x7f;
        sealed[at + 1] = (byte) 0xff;
        sealed[at + 2] = (byte) 0xff;
        sealed[at + 3] = (byte) 0xff;
        assertEquals(VaultError.CORRUPT, errorOfParse(sealed));
    }

    @Test
    void passwordRoundTripAndWrongPassword() {
        KdfProfile fast = KdfProfile.pbkdf2(KdfProfile.MIN_ITERATIONS);
        byte[] sealed = SecureEnvelope.sealWithPassword("correct horse".toCharArray(), fast,
                "dk", 1, binding(), "diary".getBytes());
        assertArrayEquals("diary".getBytes(),
                SecureEnvelope.parse(sealed).openWithPassword("correct horse".toCharArray(), binding()));
        try {
            SecureEnvelope.parse(sealed).openWithPassword("wrong horse".toCharArray(), binding());
            fail("expected an authentication failure");
        } catch (VaultException e) {
            assertEquals(VaultError.AUTHENTICATION_FAILED, e.getError());
        }
    }

    @Test
    void loweredIterationCountIsRefused() {
        // The attack this exists to stop: an attacker who can edit stored bytes rewrites the
        // iteration count to something a laptop can brute force. Two defences, and this checks
        // both -- the count is out of range, so the parse refuses before any key is derived.
        KdfProfile fast = KdfProfile.pbkdf2(KdfProfile.MIN_ITERATIONS);
        byte[] sealed = SecureEnvelope.sealWithPassword("pw".toCharArray(), fast, "dk", 1,
                binding(), "x".getBytes());
        sealed[7] = 0;
        sealed[8] = 0;
        sealed[9] = 0;
        sealed[10] = 10;
        assertEquals(VaultError.UNSUPPORTED_FORMAT, errorOfParse(sealed));
    }

    @Test
    void raisedIterationCountFailsTheTag() {
        // The second defence, for a count that stays inside the legal range: the header is
        // authenticated, so changing it fails the tag rather than being honoured.
        KdfProfile fast = KdfProfile.pbkdf2(KdfProfile.MIN_ITERATIONS);
        byte[] sealed = SecureEnvelope.sealWithPassword("pw".toCharArray(), fast, "dk", 1,
                binding(), "x".getBytes());
        sealed[10] = (byte) (sealed[10] + 1);
        try {
            SecureEnvelope.parse(sealed).openWithPassword("pw".toCharArray(), binding());
            fail("expected an authentication failure");
        } catch (VaultException e) {
            assertEquals(VaultError.AUTHENTICATION_FAILED, e.getError());
        }
    }

    @Test
    void directEnvelopeRefusesAPassword() {
        byte[] sealed = SecureEnvelope.seal(key(1), "dk", 1, binding(), "x".getBytes());
        try {
            SecureEnvelope.parse(sealed).openWithPassword("pw".toCharArray(), binding());
            fail("expected a format refusal");
        } catch (VaultException e) {
            assertEquals(VaultError.UNSUPPORTED_FORMAT, e.getError());
        }
    }

    @Test
    void shortKeyIsRefused() {
        try {
            SecureEnvelope.seal(new byte[16], "dk", 1, binding(), "x".getBytes());
            fail("expected a refusal of a 16 byte key for a 256 bit suite");
        } catch (VaultException e) {
            assertEquals(VaultError.UNSUPPORTED_FORMAT, e.getError());
        }
    }

    @Test
    void sealNeverEmitsAnEnvelopeParseWouldRefuse() {
        // The parser bounds the declared ciphertext length, and the sealer has to respect the same
        // bound or this class can write bytes it then refuses to read -- a failure that would
        // surface at the open rather than at the write that caused it.
        //
        // A byte over the limit is enough to prove it; allocating the full 64MiB to check would
        // cost more than the property is worth.
        byte[] tooLarge = new byte[SecureEnvelope.MAX_CIPHERTEXT - SecureEnvelope.TAG_LENGTH + 1];
        try {
            SecureEnvelope.seal(key(1), "dk", 1, binding(), tooLarge);
            fail("expected an oversized plaintext to be refused at seal time");
        } catch (VaultException e) {
            assertEquals(VaultError.UNSUPPORTED_FORMAT, e.getError());
        }
    }

    @Test
    void emptyPlaintextRoundTrips() {
        byte[] sealed = SecureEnvelope.seal(key(1), "dk", 1, binding(), new byte[0]);
        assertEquals(0, SecureEnvelope.parse(sealed).open(key(1), binding()).length);
    }

    private static VaultError errorOfParse(byte[] sealed) {
        try {
            SecureEnvelope.parse(sealed);
            return null;
        } catch (VaultException e) {
            return e.getError();
        }
    }
}
