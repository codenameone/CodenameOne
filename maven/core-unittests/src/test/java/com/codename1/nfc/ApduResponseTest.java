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
package com.codename1.nfc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic coverage for the {@link ApduResponse} ISO 7816 status-word
 * helpers: the canonical SW constants, status-word extraction, body slicing,
 * and the {@code withStatus} composer.
 */
class ApduResponseTest {

    @Test
    void statusWordConstantsHaveExpectedBytes() {
        assertArrayEquals(new byte[]{(byte) 0x90, (byte) 0x00}, ApduResponse.swSuccess());
        assertArrayEquals(new byte[]{(byte) 0x6A, (byte) 0x82}, ApduResponse.swFileNotFound());
        assertArrayEquals(new byte[]{(byte) 0x6D, (byte) 0x00}, ApduResponse.swInsNotSupported());
        assertArrayEquals(new byte[]{(byte) 0x6E, (byte) 0x00}, ApduResponse.swClaNotSupported());
        assertArrayEquals(new byte[]{(byte) 0x67, (byte) 0x00}, ApduResponse.swWrongLength());
        assertArrayEquals(new byte[]{(byte) 0x69, (byte) 0x82}, ApduResponse.swSecurityNotSatisfied());
        assertArrayEquals(new byte[]{(byte) 0x6F, (byte) 0x00}, ApduResponse.swUnknownError());
    }

    @Test
    void constantsAllocateFreshArraysEachCall() {
        // Mutating one returned array must not corrupt the next caller's copy.
        byte[] first = ApduResponse.swSuccess();
        first[0] = 0;
        assertArrayEquals(new byte[]{(byte) 0x90, (byte) 0x00}, ApduResponse.swSuccess());
    }

    @Test
    void isSuccessTrueOnlyForTrailing9000() {
        assertTrue(ApduResponse.isSuccess(new byte[]{'O', 'K', (byte) 0x90, (byte) 0x00}));
        assertTrue(ApduResponse.isSuccess(new byte[]{(byte) 0x90, (byte) 0x00}));
        assertFalse(ApduResponse.isSuccess(new byte[]{(byte) 0x6A, (byte) 0x82}));
    }

    @Test
    void bodyStripsTwoByteStatusTrailer() {
        byte[] apdu = {'h', 'i', (byte) 0x90, (byte) 0x00};
        assertArrayEquals(new byte[]{'h', 'i'}, ApduResponse.body(apdu));
    }

    @Test
    void bodyOfBareStatusWordIsEmpty() {
        assertEquals(0, ApduResponse.body(new byte[]{(byte) 0x90, (byte) 0x00}).length);
    }

    @Test
    void bodyOfNullOrShortInputIsEmpty() {
        assertEquals(0, ApduResponse.body(null).length);
        assertEquals(0, ApduResponse.body(new byte[]{0x01}).length);
    }

    @Test
    void statusWordReadsTrailingTwoBytesAsUnsigned16() {
        assertEquals(0x9000, ApduResponse.statusWord(new byte[]{'x', (byte) 0x90, (byte) 0x00}));
        assertEquals(0x6A82, ApduResponse.statusWord(new byte[]{(byte) 0x6A, (byte) 0x82}));
        // High bit set in SW1 must not sign-extend.
        assertEquals(0x9000, ApduResponse.statusWord(ApduResponse.swSuccess()));
    }

    @Test
    void statusWordOfNullOrShortInputIsZero() {
        assertEquals(0, ApduResponse.statusWord(null));
        assertEquals(0, ApduResponse.statusWord(new byte[]{0x01}));
    }

    @Test
    void swMasksToLowByteOfEachArgument() {
        assertArrayEquals(new byte[]{(byte) 0x90, (byte) 0x00}, ApduResponse.sw(0x90, 0x00));
        // Bits above the low byte are discarded.
        assertArrayEquals(new byte[]{(byte) 0x90, (byte) 0x00}, ApduResponse.sw(0x190, 0x100));
    }

    @Test
    void withStatusAppendsTrailerToBody() {
        byte[] combined = ApduResponse.withStatus(new byte[]{'O', 'K'}, ApduResponse.swSuccess());
        assertArrayEquals(new byte[]{'O', 'K', (byte) 0x90, (byte) 0x00}, combined);
        assertTrue(ApduResponse.isSuccess(combined));
    }

    @Test
    void withStatusTreatsNullBodyAsEmpty() {
        assertArrayEquals(new byte[]{(byte) 0x6F, (byte) 0x00},
                ApduResponse.withStatus(null, ApduResponse.swUnknownError()));
    }

    @Test
    void withStatusRejectsNonTwoByteStatusWord() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                ApduResponse.withStatus(new byte[]{'x'}, new byte[]{(byte) 0x90});
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                ApduResponse.withStatus(new byte[]{'x'}, null);
            }
        });
    }
}
