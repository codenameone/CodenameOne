/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

import com.codename1.flutter.widgets.TextRenderElement;

import dart.runtime.Funcs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Word-wrapping math with stubbed font metrics (10 units per character).
 */
class TextWrapTest {

    private static final Funcs.Func1<String, Double> MEASURE = new Funcs.Func1<String, Double>() {
        @Override
        public Double call(String s) {
            return s.length() * 10.0;
        }
    };

    @Test
    void shortTextStaysOnOneLine() {
        List<String> lines = TextRenderElement.wrap("hello", MEASURE, 100);
        assertEquals(1, lines.size());
        assertEquals("hello", lines.get(0));
    }

    @Test
    void breaksOnWordBoundaries() {
        // 10 chars fit per line; "hello world" is 11.
        List<String> lines = TextRenderElement.wrap("hello world foo", MEASURE, 100);
        assertEquals(2, lines.size());
        assertEquals("hello", lines.get(0));
        assertEquals("world foo", lines.get(1));
    }

    @Test
    void fillsLinesGreedily() {
        List<String> lines = TextRenderElement.wrap("aa bb cc dd ee", MEASURE, 50);
        // 5 chars per line: "aa bb" fits exactly, then "cc dd", then "ee".
        assertEquals(3, lines.size());
        assertEquals("aa bb", lines.get(0));
        assertEquals("cc dd", lines.get(1));
        assertEquals("ee", lines.get(2));
    }

    @Test
    void hardBreaksAWordWiderThanTheLine() {
        List<String> lines = TextRenderElement.wrap("abcdefghijklmno", MEASURE, 100);
        assertEquals(2, lines.size());
        assertEquals("abcdefghij", lines.get(0));
        assertEquals("klmno", lines.get(1));
    }

    @Test
    void respectsEmbeddedNewlines() {
        List<String> lines = TextRenderElement.wrap("a\nb b\nc", MEASURE, 1000);
        assertEquals(3, lines.size());
        assertEquals("a", lines.get(0));
        assertEquals("b b", lines.get(1));
        assertEquals("c", lines.get(2));
    }

    @Test
    void unboundedWidthNeverWraps() {
        List<String> lines = TextRenderElement.wrap(
                "the quick brown fox", MEASURE, Double.POSITIVE_INFINITY);
        assertEquals(1, lines.size());
        assertEquals("the quick brown fox", lines.get(0));
    }

    @Test
    void everyLineFitsTheWidth() {
        List<String> lines = TextRenderElement.wrap(
                "one twotwo three fourfourfourfour x", MEASURE, 70);
        for (String line : lines) {
            assertTrue(MEASURE.call(line) <= 70, "line too wide: '" + line + "'");
        }
    }
}
