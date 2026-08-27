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

import com.codename1.call.CallHandleType;
import com.codename1.impl.call.CallWire;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The codec the SPI speaks, which several ports implement by hand. */
public class CallWireTest {

    @Test
    public void trailingEmptyFieldsSurviveTheSplit() {
        // String.split drops them, which would shift every index for a record
        // that ends in an absent display name -- so a call would ring showing
        // the video flag as its caller name.
        String[] f = CallWire.split("a\tb\t\t");
        assertEquals(4, f.length);
        assertEquals("a", f[0]);
        assertEquals("", f[3]);
    }

    @Test
    public void aFieldPastTheEndReadsAsEmptyRatherThanThrowing() {
        // Records cross from native code and an older port sends shorter ones.
        // Reading past the end has to be ordinary, not exceptional.
        String[] f = CallWire.split("only");
        assertEquals("", CallWire.field(f, 7));
        assertFalse(CallWire.flag(f, 7));
        assertEquals(-1, CallWire.integer(f, 7, -1));
    }

    @Test
    public void separatorsInsideAFieldCannotForgeANewField() {
        // A display name is attacker-influenced on any app that shows a remote
        // party's chosen name, so a tab in it must not shift the record.
        String record = CallWire.join(new String[]{"a\tb", "c"});
        assertEquals(2, CallWire.split(record).length);
        assertEquals("a b", CallWire.split(record)[0]);
    }

    @Test
    public void newlinesAreSanitizedBecauseTheQueueIsLineDelimited() {
        // The iOS side appends pending calls to a line-per-record file. A
        // newline in a display name would split one call into two.
        assertEquals("a b", CallWire.sanitize("a\nb"));
        assertEquals("a b", CallWire.sanitize("a\rb"));
    }

    @Test
    public void aHandleRoundTrips() {
        CallHandle h = CallHandle.phone("+14155551212");
        CallHandle back = CallWire.decodeHandle(CallWire.encodeHandle(h));
        assertNotNull(back);
        assertSame(CallHandleType.PHONE_NUMBER, back.getType());
        assertEquals("+14155551212", back.getValue());
    }

    @Test
    public void aHandleWithNoValueDecodesToNull() {
        assertNull(CallWire.decodeHandle(""));
        assertNull(CallWire.decodeHandle("1\t"));
    }

    @Test
    public void anUnknownHandleTypeKeepsTheAddressInsteadOfLosingTheCall() {
        // A newer port sending a type this build has not heard of must not
        // turn into a call that rings with no caller at all.
        CallHandle h = CallWire.decodeHandle("99\t+14155551212");
        assertNotNull(h);
        assertSame(CallHandleType.GENERIC, h.getType());
        assertEquals("+14155551212", h.getValue());
    }

    @Test
    public void unknownOrdinalsFallBackRatherThanThrowing() {
        assertSame(CallEndReason.FAILED, CallWire.endReason(99));
        assertSame(CallError.UNKNOWN, CallWire.error(99));
        assertSame(CallEndReason.FAILED, CallWire.endReason(-1));
    }

    @Test
    public void decodeErrorAlwaysProducesSomething() {
        // Called on a path that has already failed. Answering null would leave
        // the caller's AsyncResource unsettled, which the bridge contract
        // calls worse than an outright error.
        assertNotNull(CallWire.decodeError(0, null));
        assertNotNull(CallWire.decodeError(99, ""));
        assertSame(CallError.NOT_SUPPORTED,
                CallWire.decodeError(CallError.NOT_SUPPORTED.ordinal(), "x")
                        .getError());
    }

    @Test
    public void aMalformedNumberFallsBackInsteadOfThrowing() {
        String[] f = CallWire.split("notanumber");
        assertEquals(7, CallWire.integer(f, 0, 7));
        assertEquals(7L, CallWire.integer64(f, 0, 7L));
    }

    @Test
    public void splitOfNullIsEmptyRatherThanAFailure() {
        assertEquals(0, CallWire.split(null).length);
        assertEquals("", CallWire.sanitize(null));
        assertEquals("", CallWire.join(null));
    }

    @Test
    public void flagsRoundTripThroughTheirOwnRendering() {
        String[] f = CallWire.split(CallWire.join(
                new String[]{CallWire.flagOf(true), CallWire.flagOf(false)}));
        assertTrue(CallWire.flag(f, 0));
        assertFalse(CallWire.flag(f, 1));
    }

    @Test
    public void handleTypesDecodeFromOneField() {
        // The Android account registers its Telecom schemes from these, so a
        // record it cannot read means an app that said "phone numbers only"
        // is registered for SIP as well -- and Telecom then routes it a call
        // it said it could not take.
        String[] f = CallWire.split("a\t1\tb\tc\td\t1,0\te");
        List<CallHandleType> types = CallWire.handleTypes(f, 5);
        assertEquals(2, types.size());
        assertTrue(types.contains(CallHandleType.PHONE_NUMBER));
        assertTrue(types.contains(CallHandleType.GENERIC));

        // An absent or blank field is "the app named none", which the caller
        // reads against its own defaults -- not "the app wants none".
        assertTrue(CallWire.handleTypes(f, 99).isEmpty());
        assertTrue(CallWire.handleTypes(CallWire.split("a\tb"), 1).isEmpty());

        // A record this class did not write does not cost the rest of it.
        List<CallHandleType> messy =
                CallWire.handleTypes(CallWire.split("x\t1,zz,2"), 1);
        assertEquals(2, messy.size());
    }
}
