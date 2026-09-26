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
package com.codename1.flutter.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// `Text.maxLines` and `TextOverflow.ellipsis`, pinned on the pure arithmetic.
///
/// Both were parsed and dropped, so a one-line preview rendered its whole body.
/// That reads as a layout choice rather than a defect, which is why it survived
/// so long: the Reply study's inbox showed every message in full.
///
/// A fixed-width measure stands in for a Font, so this needs no display.
class TextClampTest {

    /// Every character is 10 wide, so a line's width is its length times ten.
    private static final Funcs.Func1<String, Double> TEN_PER_CHAR =
            new Funcs.Func1<String, Double>() {
                @Override
                public Double call(String s) {
                    return Double.valueOf(s.length() * 10.0);
                }
            };

    private static List<String> lines(String... l) {
        return new ArrayList<String>(Arrays.asList(l));
    }

    @Test
    @DisplayName("no maxLines leaves the paragraph alone")
    void unclamped() {
        List<String> in = lines("one", "two", "three");
        assertEquals(in, TextRenderElement.clamp(in, null, true, TEN_PER_CHAR, 100));
    }

    @Test
    @DisplayName("fewer lines than the limit are left alone")
    void underTheLimit() {
        List<String> in = lines("one", "two");
        assertEquals(in, TextRenderElement.clamp(in, Long.valueOf(3), true, TEN_PER_CHAR, 100));
    }

    @Test
    @DisplayName("clip keeps the first maxLines lines verbatim")
    void clips() {
        List<String> out = TextRenderElement.clamp(
                lines("one", "two", "three"), Long.valueOf(2), false, TEN_PER_CHAR, 100);
        assertEquals(lines("one", "two"), out);
    }

    @Test
    @DisplayName("ellipsis marks the last kept line")
    void ellipsisMarksTheCut() {
        List<String> out = TextRenderElement.clamp(
                lines("one", "two", "three"), Long.valueOf(2), true, TEN_PER_CHAR, 100);
        assertEquals(2, out.size());
        assertEquals("one", out.get(0));
        assertTrue(out.get(1).endsWith("…"), "the cut is marked: " + out.get(1));
    }

    @Test
    @DisplayName("the ellipsis has to fit, so the line gives up characters for it")
    void ellipsisFitsWithinTheWidth() {
        // Width 50 = five characters. "abcdefgh" plus the marker must come back
        // no wider than that, which means dropping characters for it.
        List<String> out = TextRenderElement.clamp(
                lines("abcdefgh", "next"), Long.valueOf(1), true, TEN_PER_CHAR, 50);
        assertEquals(1, out.size());
        assertTrue(TEN_PER_CHAR.call(out.get(0)).doubleValue() <= 50,
                "the clamped line must fit: '" + out.get(0) + "'");
        assertTrue(out.get(0).endsWith("…"));
    }

    @Test
    @DisplayName("no trailing space is left before the ellipsis")
    void trailingSpaceIsTrimmed() {
        List<String> out = TextRenderElement.clamp(
                lines("hello ", "world"), Long.valueOf(1), true, TEN_PER_CHAR,
                Double.POSITIVE_INFINITY);
        assertEquals("hello…", out.get(0));
    }

    @Test
    @DisplayName("an unbounded width still ellipsises")
    void unboundedWidth() {
        List<String> out = TextRenderElement.clamp(
                lines("a", "b"), Long.valueOf(1), true, TEN_PER_CHAR,
                Double.POSITIVE_INFINITY);
        assertEquals(lines("a…"), out);
    }
}
