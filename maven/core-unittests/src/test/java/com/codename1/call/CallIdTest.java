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
package com.codename1.call;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The identifier that names one call in this API, the system, and a push. */
public class CallIdTest {

    @Test
    public void aGeneratedIdIsCanonical() {
        String id = CallId.random();
        assertEquals(36, id.length());
        assertTrue(CallId.isValid(id));
        assertEquals(id, CallId.normalize(id), "generated ids are already canonical");
    }

    @Test
    public void aGeneratedIdIsUppercase() {
        // The identifier is compared as a string on every hop, including by
        // servers that never saw this API. Fixing the case is what makes
        // "compare case-insensitively everywhere" unnecessary.
        for (int i = 0; i < 200; i++) {
            String id = CallId.random();
            assertEquals(id.toUpperCase(), id);
        }
    }

    @Test
    public void aGeneratedIdCarriesTheVersionAndVariantBits() {
        // Without them the value is unique but is not a UUID, and CallKit
        // rejects it -- which would present as calls that never ring.
        for (int i = 0; i < 200; i++) {
            String id = CallId.random();
            assertEquals('4', id.charAt(14), "version nibble in " + id);
            char variant = id.charAt(19);
            assertTrue(variant == '8' || variant == '9' || variant == 'A'
                    || variant == 'B', "variant nibble in " + id);
        }
    }

    @Test
    public void generatedIdsDoNotRepeat() {
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < 2000; i++) {
            assertTrue(seen.add(CallId.random()), "a call id repeated");
        }
    }

    @Test
    public void aLowercaseIdFromAServerIsAccepted() {
        // Payloads come from servers that did not use this API, and plenty of
        // UUID libraries render lowercase. Rejecting those would look like
        // calls that mysteriously never connect.
        String lower = "6b29fc40-ca47-1067-b31d-00dd010662da";
        assertTrue(CallId.isValid(lower));
        assertEquals("6B29FC40-CA47-1067-B31D-00DD010662DA",
                CallId.normalize(lower));
    }

    @Test
    public void normalizeAnswersNullRatherThanThrowing() {
        // The value routinely comes from off the device, so a bad one is an
        // ordinary outcome that maps to INVALID_ID, not an exception.
        assertNull(CallId.normalize(null));
        assertNull(CallId.normalize(""));
        assertNull(CallId.normalize("not-a-uuid"));
        assertNull(CallId.normalize("6B29FC40CA471067B31D00DD010662DA"));
    }

    @Test
    public void hyphensMustBeInTheRightPlaces() {
        assertFalse(CallId.isValid("6B29FC40CA47-1067-B31D-00DD010662DA-"));
        assertFalse(CallId.isValid("6B29FC40-CA47-1067-B31D-00DD010662DAX"));
    }

    @Test
    public void nonHexDigitsAreRejected() {
        assertFalse(CallId.isValid("6B29FC40-CA47-1067-B31D-00DD010662DG"));
    }

    @Test
    public void formatRendersSixteenBytes() {
        byte[] b = new byte[16];
        for (int i = 0; i < 16; i++) {
            b[i] = (byte) i;
        }
        assertEquals("00010203-0405-0607-0809-0A0B0C0D0E0F", CallId.format(b));
    }

    @Test
    public void formatRefusesTheWrongLength() {
        try {
            CallId.format(new byte[15]);
            throw new AssertionError("a 15 byte id should not be formattable");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("16"));
        }
    }
}
