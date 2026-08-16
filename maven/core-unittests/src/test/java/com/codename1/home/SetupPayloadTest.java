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
package com.codename1.home;

import com.codename1.home.commissioning.SetupPayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parsing the code printed on a Matter accessory.
 *
 * <p>The reason this is parsed in Java at all rather than handed to the
 * platform is that a wrong code otherwise fails inside an operating-system
 * sheet, in the OS's wording, which tells a user nothing about having scanned
 * the barcode on the box. These tests are mostly about the messages being
 * specific enough to act on.</p>
 */
class SetupPayloadTest {

    /**
     * The manual code the Matter specification uses as its own worked
     * example. Encodes discriminator 3840, passcode 20202021, no vendor or
     * product id.
     */
    private static final String SPEC_MANUAL_CODE = "34970112332";

    @Test
    void theSpecificationsOwnManualCodeParses() {
        SetupPayload p = SetupPayload.parse(SPEC_MANUAL_CODE);
        assertEquals(20202021, p.getPasscode());
        assertFalse(p.isFromQrCode());
        assertTrue(p.isShortDiscriminator(),
                "a typed code carries only four discriminator bits");
    }

    /**
     * People type these off a sticker, with whatever separators the sticker
     * uses.
     */
    @Test
    void separatorsPeopleTypeAreTolerated() {
        assertEquals(20202021,
                SetupPayload.parse("3497-011-2332").getPasscode());
        assertEquals(20202021,
                SetupPayload.parse("3497 011 2332").getPasscode());
        assertEquals(20202021,
                SetupPayload.parse("  34970112332  ").getPasscode());
    }

    /**
     * The check digit is the whole point of the manual code's last digit, and
     * a single mistyped digit is the failure it is there to catch.
     */
    @Test
    void aMistypedDigitIsCaughtByTheCheckDigit() {
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class,
                        () -> SetupPayload.parse("34970112333"));
        assertTrue(e.getMessage().indexOf("check digit") >= 0,
                "the message has to say what is wrong, because it is shown to"
                        + " somebody holding the accessory: " + e.getMessage());
    }

    @Test
    void aWrongLengthIsRejectedWithItsLength() {
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class,
                        () -> SetupPayload.parse("1234"));
        assertTrue(e.getMessage().indexOf("11 or 21") >= 0, e.getMessage());
    }

    @Test
    void nonDigitsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> SetupPayload.parse("3497011233A"));
    }

    @Test
    void nothingIsRejectedPlainly() {
        assertThrows(IllegalArgumentException.class,
                () -> SetupPayload.parse(null));
        assertThrows(IllegalArgumentException.class,
                () -> SetupPayload.parse("   "));
    }

    /**
     * A QR payload that is not the standard compact encoding -- a
     * vendor-extended one -- is refused rather than half-read, and the message
     * points at the way through: pass it to the platform unparsed.
     */
    @Test
    void aNonStandardQrPayloadIsRefusedWithAWayForward() {
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class,
                        () -> SetupPayload.parse("MT:TOO-SHORT"));
        assertTrue(e.getMessage().indexOf("unparsed") >= 0
                        || e.getMessage().indexOf("Vendor-extended") >= 0,
                e.getMessage());
    }

    @Test
    void aQrPayloadWithACharacterOutsideBase38IsRejected() {
        // Right length, wrong alphabet: '*' is not in the base-38 set.
        assertThrows(IllegalArgumentException.class,
                () -> SetupPayload.parse("MT:*******************"));
    }

    /**
     * A passcode the specification forbids is what a vendor ships when they
     * have not generated one, which is a security problem rather than a typo,
     * so it is worth naming as such.
     */
    @Test
    void aForbiddenPasscodeIsNamedAsOne() {
        // 11111111 with a valid Verhoeff digit: built by hand so the check
        // digit passes and the passcode rule is what rejects it.
        String code = manualCodeFor(0, 11111111);
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class,
                        () -> SetupPayload.parse(code));
        assertTrue(e.getMessage().indexOf("forbids") >= 0, e.getMessage());
    }

    @Test
    void isValidAnswersRatherThanThrowing() {
        assertTrue(SetupPayload.isValid(SPEC_MANUAL_CODE));
        assertFalse(SetupPayload.isValid("34970112333"));
        assertFalse(SetupPayload.isValid("not a code"));
        assertFalse(SetupPayload.isValid(null));
    }

    /**
     * The obvious toString would have put a pairing secret into the first log
     * line somebody added while debugging a scanner.
     */
    @Test
    void toStringDoesNotLeakThePasscode() {
        SetupPayload p = SetupPayload.parse(SPEC_MANUAL_CODE);
        assertFalse(p.toString().contains(Integer.toString(p.getPasscode())),
                "the passcode is a pairing secret and must not appear in a"
                        + " string that ends up in logs: " + p);
    }

    @Test
    void theRawCodeIsKeptForPassingThrough() {
        assertEquals(SPEC_MANUAL_CODE,
                SetupPayload.parse(SPEC_MANUAL_CODE).getRaw());
    }

    /**
     * Builds an 11-digit manual pairing code with a correct Verhoeff check
     * digit, so a test can exercise a rule other than the checksum.
     */
    private static String manualCodeFor(int discriminator, int passcode) {
        int first = (discriminator >> 10) & 0x03;
        int group2 = ((discriminator & 0x300) << 6) | (passcode & 0x3FFF);
        int group3 = (passcode >> 14) & 0x1FFF;
        StringBuilder b = new StringBuilder();
        b.append(first);
        appendPadded(b, group2, 5);
        appendPadded(b, group3, 4);
        b.append(verhoeffCheckDigit(b.toString()));
        return b.toString();
    }

    private static void appendPadded(StringBuilder b, int value, int width) {
        String s = Integer.toString(value);
        for (int i = s.length(); i < width; i++) {
            b.append('0');
        }
        b.append(s);
    }

    private static final int[][] D = {
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        {1, 2, 3, 4, 0, 6, 7, 8, 9, 5},
        {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
        {3, 4, 0, 1, 2, 8, 9, 5, 6, 7},
        {4, 0, 1, 2, 3, 9, 5, 6, 7, 8},
        {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
        {6, 5, 9, 8, 7, 1, 0, 4, 3, 2},
        {7, 6, 5, 9, 8, 2, 1, 0, 4, 3},
        {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
        {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
    };

    private static final int[][] P = {
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        {1, 5, 7, 6, 2, 8, 3, 0, 9, 4},
        {5, 8, 0, 3, 7, 9, 6, 1, 4, 2},
        {8, 9, 1, 6, 0, 4, 3, 5, 2, 7},
        {9, 4, 5, 3, 1, 2, 6, 8, 7, 0},
        {4, 2, 8, 6, 5, 7, 3, 9, 0, 1},
        {2, 7, 9, 3, 8, 0, 6, 4, 1, 5},
        {7, 0, 4, 6, 9, 1, 3, 2, 5, 8}
    };

    private static final int[] INV = {0, 4, 3, 2, 1, 5, 6, 7, 8, 9};

    private static int verhoeffCheckDigit(String digits) {
        int c = 0;
        int length = digits.length();
        for (int i = 0; i < length; i++) {
            int digit = digits.charAt(length - i - 1) - '0';
            c = D[c][P[(i + 1) % 8][digit]];
        }
        return INV[c];
    }
}
