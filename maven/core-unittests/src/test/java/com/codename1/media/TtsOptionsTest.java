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
package com.codename1.media;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic coverage for the {@link TtsOptions} fluent builder: defaults,
 * chaining identity, and round-trips.
 */
class TtsOptionsTest {

    @Test
    void defaultsAreSensible() {
        TtsOptions o = new TtsOptions();
        assertNull(o.getLanguageTag());
        assertNull(o.getVoiceId());
        assertEquals(1.0f, o.getRate(), 1e-6f);
        assertEquals(1.0f, o.getPitch(), 1e-6f);
        assertEquals(1.0f, o.getVolume(), 1e-6f);
    }

    @Test
    void settersChainAndRoundTrip() {
        TtsOptions o = new TtsOptions();
        assertSame(o, o.setLanguageTag("ja-JP"));
        assertSame(o, o.setVoiceId("voice-7"));
        assertSame(o, o.setRate(0.5f));
        assertSame(o, o.setPitch(1.5f));
        assertSame(o, o.setVolume(0.25f));

        assertEquals("ja-JP", o.getLanguageTag());
        assertEquals("voice-7", o.getVoiceId());
        assertEquals(0.5f, o.getRate(), 1e-6f);
        assertEquals(1.5f, o.getPitch(), 1e-6f);
        assertEquals(0.25f, o.getVolume(), 1e-6f);
    }

    @Test
    void nullableStringsCanBeClearedBackToNull() {
        TtsOptions o = new TtsOptions().setLanguageTag("en-US").setVoiceId("v");
        o.setLanguageTag(null);
        o.setVoiceId(null);
        assertNull(o.getLanguageTag());
        assertNull(o.getVoiceId());
    }
}
