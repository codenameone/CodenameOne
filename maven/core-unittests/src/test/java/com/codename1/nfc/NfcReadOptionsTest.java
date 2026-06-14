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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic coverage for the {@link NfcReadOptions} fluent builder: defaults,
 * chaining identity, defensive copying, and the unmodifiable views returned by
 * the getters.
 */
class NfcReadOptionsTest {

    @Test
    void defaultsAreSensible() {
        NfcReadOptions o = new NfcReadOptions();
        assertEquals("Hold your iPhone near the NFC tag", o.getAlertMessage());
        assertNull(o.getInvalidatedMessage());
        assertTrue(o.getTechFilter().isEmpty());
        assertFalse(o.isNdefOnly());
        assertEquals(0L, o.getTimeoutMs());
        assertTrue(o.getFelicaSystemCodes().isEmpty());
        assertTrue(o.getIsoSelectAids().isEmpty());
    }

    @Test
    void settersReturnSameInstanceForChaining() {
        NfcReadOptions o = new NfcReadOptions();
        assertSame(o, o.setAlertMessage("hi"));
        assertSame(o, o.setInvalidatedMessage("bye"));
        assertSame(o, o.setTechFilter(TagType.NDEF));
        assertSame(o, o.setNdefOnly(true));
        assertSame(o, o.setTimeoutMs(10));
        assertSame(o, o.setFelicaSystemCodes("0003"));
        assertSame(o, o.setIsoSelectAids(new byte[]{1, 2, 3, 4, 5}));
    }

    @Test
    void alertAndInvalidatedMessagesRoundTrip() {
        NfcReadOptions o = new NfcReadOptions()
                .setAlertMessage("Scan now")
                .setInvalidatedMessage("Failed");
        assertEquals("Scan now", o.getAlertMessage());
        assertEquals("Failed", o.getInvalidatedMessage());
    }

    @Test
    void timeoutRoundTrips() {
        assertEquals(5000L, new NfcReadOptions().setTimeoutMs(5000L).getTimeoutMs());
    }

    @Test
    void techFilterStoresTypesInOrder() {
        NfcReadOptions o = new NfcReadOptions().setTechFilter(TagType.ISO_DEP, TagType.NFC_F);
        List<TagType> filter = o.getTechFilter();
        assertEquals(2, filter.size());
        assertEquals(TagType.ISO_DEP, filter.get(0));
        assertEquals(TagType.NFC_F, filter.get(1));
    }

    @Test
    void techFilterIsUnmodifiable() {
        NfcReadOptions o = new NfcReadOptions().setTechFilter(TagType.NDEF);
        assertThrows(UnsupportedOperationException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                o.getTechFilter().add(TagType.NFC_A);
            }
        });
    }

    @Test
    void nullOrEmptyTechFilterResetsToEmpty() {
        NfcReadOptions o = new NfcReadOptions().setTechFilter(TagType.NDEF);
        o.setTechFilter((TagType[]) null);
        assertTrue(o.getTechFilter().isEmpty());
        o.setTechFilter(TagType.NDEF);
        o.setTechFilter();
        assertTrue(o.getTechFilter().isEmpty());
    }

    @Test
    void ndefOnlyImpliesNdefTechFilterWhenEmpty() {
        NfcReadOptions o = new NfcReadOptions().setNdefOnly(true);
        assertTrue(o.isNdefOnly());
        assertEquals(1, o.getTechFilter().size());
        assertEquals(TagType.NDEF, o.getTechFilter().get(0));
    }

    @Test
    void ndefOnlyDoesNotClobberExistingTechFilter() {
        NfcReadOptions o = new NfcReadOptions()
                .setTechFilter(TagType.ISO_DEP)
                .setNdefOnly(true);
        assertTrue(o.isNdefOnly());
        // The existing filter was non-empty, so it must be preserved.
        assertEquals(1, o.getTechFilter().size());
        assertEquals(TagType.ISO_DEP, o.getTechFilter().get(0));
    }

    @Test
    void ndefOnlyFalseLeavesTechFilterUntouched() {
        NfcReadOptions o = new NfcReadOptions().setNdefOnly(false);
        assertFalse(o.isNdefOnly());
        assertTrue(o.getTechFilter().isEmpty());
    }

    @Test
    void felicaSystemCodesRoundTripAndAreUnmodifiable() {
        NfcReadOptions o = new NfcReadOptions().setFelicaSystemCodes("0003", "8008");
        assertEquals(2, o.getFelicaSystemCodes().size());
        assertEquals("0003", o.getFelicaSystemCodes().get(0));
        assertThrows(UnsupportedOperationException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                o.getFelicaSystemCodes().add("x");
            }
        });
    }

    @Test
    void nullOrEmptyFelicaCodesResetToEmpty() {
        NfcReadOptions o = new NfcReadOptions().setFelicaSystemCodes("0003");
        o.setFelicaSystemCodes((String[]) null);
        assertTrue(o.getFelicaSystemCodes().isEmpty());
        o.setFelicaSystemCodes("0003");
        o.setFelicaSystemCodes();
        assertTrue(o.getFelicaSystemCodes().isEmpty());
    }

    @Test
    void isoSelectAidsAreDefensivelyCopied() {
        byte[] aid = {1, 2, 3, 4, 5};
        NfcReadOptions o = new NfcReadOptions().setIsoSelectAids(aid);
        // Mutating the caller's array after the call must not affect the stored copy.
        aid[0] = 99;
        byte[] stored = o.getIsoSelectAids().get(0);
        assertEquals(1, stored[0]);
        assertEquals(5, stored.length);
    }

    @Test
    void isoSelectAidsSkipNullEntries() {
        NfcReadOptions o = new NfcReadOptions()
                .setIsoSelectAids(new byte[]{1, 2, 3, 4, 5}, null, new byte[]{6, 7, 8, 9, 10});
        assertEquals(2, o.getIsoSelectAids().size());
        assertEquals(6, o.getIsoSelectAids().get(1)[0]);
    }

    @Test
    void nullOrEmptyIsoAidsResetToEmpty() {
        NfcReadOptions o = new NfcReadOptions().setIsoSelectAids(new byte[]{1, 2, 3, 4, 5});
        o.setIsoSelectAids((byte[][]) null);
        assertTrue(o.getIsoSelectAids().isEmpty());
        o.setIsoSelectAids(new byte[]{1, 2, 3, 4, 5});
        o.setIsoSelectAids();
        assertTrue(o.getIsoSelectAids().isEmpty());
    }

    @Test
    void isoSelectAidsViewIsUnmodifiable() {
        NfcReadOptions o = new NfcReadOptions().setIsoSelectAids(new byte[]{1, 2, 3, 4, 5});
        assertThrows(UnsupportedOperationException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                o.getIsoSelectAids().add(new byte[]{0});
            }
        });
    }
}
