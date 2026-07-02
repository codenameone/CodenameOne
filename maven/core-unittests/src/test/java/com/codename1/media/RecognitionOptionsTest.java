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
 * Pure-logic coverage for the {@link RecognitionOptions} fluent builder:
 * defaults, chaining identity, round-trips, and the {@code maxResults} floor.
 */
class RecognitionOptionsTest {

    @Test
    void defaultsAreSensible() {
        RecognitionOptions o = new RecognitionOptions();
        assertEquals("en-US", o.getLanguageTag());
        assertTrue(o.isPartialResults());
        assertFalse(o.isContinuous());
        assertEquals(1, o.getMaxResults());
    }

    @Test
    void settersChainAndRoundTrip() {
        RecognitionOptions o = new RecognitionOptions();
        assertSame(o, o.setLanguageTag("de-DE"));
        assertSame(o, o.setPartialResults(false));
        assertSame(o, o.setContinuous(true));
        assertSame(o, o.setMaxResults(5));

        assertEquals("de-DE", o.getLanguageTag());
        assertFalse(o.isPartialResults());
        assertTrue(o.isContinuous());
        assertEquals(5, o.getMaxResults());
    }

    @Test
    void maxResultsFloorsAtOne() {
        assertEquals(1, new RecognitionOptions().setMaxResults(0).getMaxResults());
        assertEquals(1, new RecognitionOptions().setMaxResults(-3).getMaxResults());
    }

    @Test
    void timestampCallbacksForwardToStringCallbacksByDefault() {
        final String[] transcript = new String[1];
        TimedRecognitionCallback callback = new TimedRecognitionCallback.Adapter() {
            @Override
            public void onResult(String t, float c, String[] a) {
                transcript[0] = t;
            }
        };
        callback.onResult(TranscriptionResult.textOnly("caption text"), -1, new String[0]);
        assertEquals("caption text", transcript[0]);
    }
}
