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
package com.codename1.nearby;

import com.codename1.nearby.ranging.RangingToken;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The token is the one value in this API that travels over a wire the
 * framework does not control -- an app writes it into a GATT characteristic
 * and reads whatever comes back. So it has to survive the round trip, and it
 * has to reject what is not one of ours rather than handing garbage to a
 * native call.
 */
class RangingTokenTest {

    @Test
    void aTokenSurvivesTheRoundTrip() {
        RangingToken original = RangingToken.forPayload(
                RangingToken.PLATFORM_APPLE_NI,
                new byte[] {1, 2, 3, (byte) 200, 0, -7});
        RangingToken back = RangingToken.fromByteArray(original.toByteArray());
        assertEquals(RangingToken.PLATFORM_APPLE_NI, back.getPlatform());
        assertArrayEquals(original.getPayload(), back.getPayload());
        assertEquals(original, back);
        assertEquals(original.hashCode(), back.hashCode());
    }

    @Test
    void anEmptyPayloadIsStillAValidToken() {
        RangingToken t = RangingToken.forPayload(
                RangingToken.PLATFORM_SIMULATED, new byte[0]);
        RangingToken back = RangingToken.fromByteArray(t.toByteArray());
        assertEquals(0, back.getPayload().length);
        assertEquals(RangingToken.PLATFORM_SIMULATED, back.getPlatform());
    }

    @Test
    void aUwbAddressTokenCarriesItsParameters() {
        byte[] address = {(byte) 0xAB, (byte) 0xCD};
        byte[] key = {9, 8, 7, 6, 5, 4, 3, 2};
        RangingToken t = RangingToken.forUwbAddress(address, 9, 11, 42, key);
        assertEquals(RangingToken.PLATFORM_ANDROID_UWB, t.getPlatform());
        RangingToken back = RangingToken.fromByteArray(t.toByteArray());
        assertEquals(RangingToken.PLATFORM_ANDROID_UWB, back.getPlatform());
        assertArrayEquals(t.getPayload(), back.getPayload());
    }

    @Test
    void aUwbAddressTokenAcceptsAnEightByteAddressAndNoKey() {
        RangingToken t = RangingToken.forUwbAddress(
                new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, 5, 9, 1, null);
        assertEquals(RangingToken.PLATFORM_ANDROID_UWB,
                RangingToken.fromByteArray(t.toByteArray()).getPlatform());
    }

    @Test
    void aUwbAddressOfTheWrongLengthIsRejectedAtTheCallSite() {
        // Better here, where the stack trace names the app's own code, than
        // three layers down in a native call that reads past the end.
        assertThrows(IllegalArgumentException.class,
                () -> RangingToken.forUwbAddress(new byte[] {1, 2, 3}, 9, 11,
                        1, null));
    }

    @Test
    void garbageIsRejectedRatherThanDecoded() {
        assertThrows(IllegalArgumentException.class,
                () -> RangingToken.fromByteArray(null));
        assertThrows(IllegalArgumentException.class,
                () -> RangingToken.fromByteArray(new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> RangingToken.fromByteArray("not a token at all".getBytes()));
    }

    @Test
    void aTruncatedTokenIsRejectedRatherThanReadPastItsEnd() {
        byte[] full = RangingToken.forPayload(RangingToken.PLATFORM_APPLE_NI,
                new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).toByteArray();
        byte[] cut = new byte[full.length - 3];
        System.arraycopy(full, 0, cut, 0, cut.length);
        assertThrows(IllegalArgumentException.class,
                () -> RangingToken.fromByteArray(cut));
    }

    @Test
    void aHugeDeclaredLengthIsRejectedRatherThanOverflowingIntoAnAllocation() {
        // 10 + Integer.MAX_VALUE wraps negative, so an additive bounds check
        // would accept this ten-byte input and then try to allocate 2GB.
        byte[] t = RangingToken.forPayload(RangingToken.PLATFORM_APPLE_NI,
                new byte[0]).toByteArray();
        t[6] = (byte) 0x7f;
        t[7] = (byte) 0xff;
        t[8] = (byte) 0xff;
        t[9] = (byte) 0xff;
        assertThrows(IllegalArgumentException.class,
                () -> RangingToken.fromByteArray(t));
    }

    @Test
    void trailingBytesAreRejectedBecauseTheEncodingHasNoRoomForThem() {
        byte[] full = RangingToken.forPayload(RangingToken.PLATFORM_APPLE_NI,
                new byte[] {1, 2, 3}).toByteArray();
        byte[] padded = new byte[full.length + 4];
        System.arraycopy(full, 0, padded, 0, full.length);
        assertThrows(IllegalArgumentException.class,
                () -> RangingToken.fromByteArray(padded));
    }

    @Test
    void anUnknownVersionIsRejectedRatherThanGuessedAt() {
        byte[] t = RangingToken.forPayload(RangingToken.PLATFORM_APPLE_NI,
                new byte[] {1}).toByteArray();
        t[4] = 99;
        assertThrows(IllegalArgumentException.class,
                () -> RangingToken.fromByteArray(t));
    }

    @Test
    void theEncodedFormIsCopiedSoACallerCannotMutateTheToken() {
        RangingToken t = RangingToken.forPayload(
                RangingToken.PLATFORM_SIMULATED, new byte[] {1, 2, 3});
        byte[] a = t.toByteArray();
        byte[] b = t.toByteArray();
        assertNotSame(a, b);
        a[10] = 99;
        assertArrayEquals(new byte[] {1, 2, 3}, t.getPayload());
        assertArrayEquals(b, t.toByteArray());
        byte[] payload = t.getPayload();
        payload[0] = 42;
        assertArrayEquals(new byte[] {1, 2, 3}, t.getPayload());
    }

    @Test
    void tokensFromDifferentPlatformsAreNotEqual() {
        RangingToken apple = RangingToken.forPayload(
                RangingToken.PLATFORM_APPLE_NI, new byte[] {1, 2});
        RangingToken android = RangingToken.forPayload(
                RangingToken.PLATFORM_ANDROID_UWB, new byte[] {1, 2});
        assertFalse(apple.equals(android));
        assertTrue(apple.equals(apple));
        assertFalse(apple.equals("not a token"));
    }
}
