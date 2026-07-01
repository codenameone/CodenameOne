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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TranscriptionResultTest {

    @Test
    void segmentsBuildPlainTextAndCaptions() {
        List<TranscriptionSegment> segments = new ArrayList<TranscriptionSegment>();
        segments.add(new TranscriptionSegment(0, 1250, "Hello "));
        segments.add(new TranscriptionSegment(61234, 65432, "world"));

        TranscriptionResult result = new TranscriptionResult(segments);

        assertEquals("Hello world", result.getText());
        assertEquals(2, result.getSegments().size());
        assertTrue(result.toSrt().contains("1\n00:00:00,000 --> 00:00:01,250\nHello"));
        assertTrue(result.toSrt().contains("00:01:01,234 --> 00:01:05,432"));
        assertTrue(result.toVtt().startsWith("WEBVTT\n\n"));
        assertTrue(result.toVtt().contains("00:01:01.234 --> 00:01:05.432"));
    }

    @Test
    void textOnlyKeepsCompatibilityTranscript() {
        TranscriptionResult result = TranscriptionResult.textOnly("plain text");
        assertEquals("plain text", result.getText());
        assertEquals(1, result.getSegments().size());
        assertEquals(0, result.getSegments().get(0).getStartTimeMs());
        assertEquals(0, result.getSegments().get(0).getEndTimeMs());
    }

    @Test
    void invalidSegmentTimesAreRejected() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                new TranscriptionSegment(-1, 10, "x");
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                new TranscriptionSegment(20, 10, "x");
            }
        });
    }
}
