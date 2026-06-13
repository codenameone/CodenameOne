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
package com.codename1.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic coverage for the {@link AuthenticationOptions} fluent builder:
 * defaults, chaining identity, round-trips, and the negative-button-text
 * null-coalescing rule.
 */
class AuthenticationOptionsTest {

    @Test
    void defaultsAreSensible() {
        AuthenticationOptions o = new AuthenticationOptions();
        assertNull(o.getReason());
        assertNull(o.getTitle());
        assertNull(o.getSubtitle());
        assertNull(o.getDescription());
        assertEquals("Cancel", o.getNegativeButtonText());
        assertFalse(o.isBiometricOnly());
        assertFalse(o.isSensitiveTransaction());
        assertFalse(o.isStickyAuth());
        assertTrue(o.isShowDialogOnAndroid());
    }

    @Test
    void settersChainAndRoundTrip() {
        AuthenticationOptions o = new AuthenticationOptions();
        assertSame(o, o.setReason("unlock"));
        assertSame(o, o.setTitle("Title"));
        assertSame(o, o.setSubtitle("Subtitle"));
        assertSame(o, o.setDescription("Description"));
        assertSame(o, o.setNegativeButtonText("Nope"));
        assertSame(o, o.setBiometricOnly(true));
        assertSame(o, o.setSensitiveTransaction(true));
        assertSame(o, o.setStickyAuth(true));
        assertSame(o, o.setShowDialogOnAndroid(false));

        assertEquals("unlock", o.getReason());
        assertEquals("Title", o.getTitle());
        assertEquals("Subtitle", o.getSubtitle());
        assertEquals("Description", o.getDescription());
        assertEquals("Nope", o.getNegativeButtonText());
        assertTrue(o.isBiometricOnly());
        assertTrue(o.isSensitiveTransaction());
        assertTrue(o.isStickyAuth());
        assertFalse(o.isShowDialogOnAndroid());
    }

    @Test
    void negativeButtonTextNullRestoresDefault() {
        AuthenticationOptions o = new AuthenticationOptions().setNegativeButtonText("X");
        assertEquals("X", o.getNegativeButtonText());
        o.setNegativeButtonText(null);
        assertEquals("Cancel", o.getNegativeButtonText());
    }
}
