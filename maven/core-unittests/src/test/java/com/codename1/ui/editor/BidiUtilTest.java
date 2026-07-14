/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.editor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for the Unicode Bidi Algorithm core used by the pure editor. Samples use unicode escapes
/// to keep the source ASCII only: Hebrew alef/bet/gimel = u05D0..u05D2, Arabic alef/beh/teh =
/// u0627/u0628/u062A, Arabic-Indic zero = u0660.
class BidiUtilTest {

    private static final String HEB = "\u05D0\u05D1\u05D2";   // aleph bet gimel (strong R)
    private static final String ARABIC = "\u0627\u0628\u062A"; // alef beh teh (strong AL)

    @Test
    void classifiesCommonTypes() {
        assertEquals(BidiUtil.L, BidiUtil.typeOf('a'));
        assertEquals(BidiUtil.L, BidiUtil.typeOf('Z'));
        assertEquals(BidiUtil.EN, BidiUtil.typeOf('5'));
        assertEquals(BidiUtil.R, BidiUtil.typeOf('\u05D0'));
        assertEquals(BidiUtil.AL, BidiUtil.typeOf('\u0627'));
        assertEquals(BidiUtil.AN, BidiUtil.typeOf('\u0660'));
        assertEquals(BidiUtil.WS, BidiUtil.typeOf(' '));
        assertEquals(BidiUtil.ON, BidiUtil.typeOf('!'));
        assertEquals(BidiUtil.L, BidiUtil.typeOf('\u4E2D')); // CJK -> strong L
    }

    @Test
    void autoBaseDirectionFromFirstStrong() {
        assertFalse(BidiUtil.autoBaseRtl("hello"));
        assertFalse(BidiUtil.autoBaseRtl("123 abc"));      // digits are not strong; first strong is 'a'
        assertTrue(BidiUtil.autoBaseRtl(HEB));
        assertTrue(BidiUtil.autoBaseRtl("123 " + HEB));    // first strong is Hebrew
        assertFalse(BidiUtil.autoBaseRtl("abc " + HEB));   // first strong is Latin
        assertTrue(BidiUtil.autoBaseRtl(ARABIC + " abc")); // first strong is Arabic
    }

    @Test
    void pureLtrIsIdentityOrder() {
        byte[] levels = BidiUtil.resolveLevels("hello", false);
        for (byte b : levels) {
            assertEquals(0, b);
        }
        int[] vis = BidiUtil.reorderVisual(levels);
        assertArrayEquals(new int[]{0, 1, 2, 3, 4}, vis);
        assertTrue(BidiUtil.isTrivialLtr("hello world 123", false));
    }

    @Test
    void pureRtlIsFullyReversed() {
        byte[] levels = BidiUtil.resolveLevels(HEB, true);
        for (byte b : levels) {
            assertEquals(1, b);
        }
        int[] vis = BidiUtil.reorderVisual(levels);
        assertArrayEquals(new int[]{2, 1, 0}, vis);
        assertFalse(BidiUtil.isTrivialLtr(HEB, true));
        assertFalse(BidiUtil.isTrivialLtr(HEB, false)); // strong R makes even an LTR base non-trivial
    }

    @Test
    void embeddedRtlRunReversesWithinLtrParagraph() {
        // logical: a b c _ H0 H1 _ x y z   (H = Hebrew), LTR base
        String s = "abc \u05D0\u05D1 xyz";
        byte[] levels = BidiUtil.resolveLevels(s, false);
        int[] vis = BidiUtil.reorderVisual(levels);
        // only the two Hebrew letters (indices 4,5) swap; everything else stays put
        assertArrayEquals(new int[]{0, 1, 2, 3, 5, 4, 6, 7, 8, 9}, vis);
    }

    @Test
    void numberRunStaysLtrInsideRtlParagraph() {
        // logical: H0 H1 _ '1' '2'  with RTL base. The number is an LTR island inside RTL text, so
        // visually (left to right) the number reads "12" on the left, then space, then the reversed Hebrew.
        String s = "\u05D0\u05D1 12";
        byte[] levels = BidiUtil.resolveLevels(s, true);
        assertEquals(1, levels[0]); // Hebrew
        assertEquals(1, levels[1]);
        assertEquals(2, levels[3]); // '1' European number -> level base+1 = 2 inside RTL
        assertEquals(2, levels[4]); // '2'
        int[] vis = BidiUtil.reorderVisual(levels);
        assertArrayEquals(new int[]{3, 4, 2, 1, 0}, vis);
    }

    @Test
    void arabicNumberFollowsArabicLetters() {
        // W2: European digits after an Arabic letter become Arabic numbers (still an LTR-ordered run).
        String s = ARABIC + "12";
        byte[] levels = BidiUtil.resolveLevels(s, true);
        // Arabic letters resolve to R (level 1); the digits are AN -> level 2
        assertEquals(1, levels[0]);
        assertEquals(2, levels[levels.length - 1]);
    }

    @Test
    void reorderIsAStablePermutation() {
        String s = "abc " + HEB + " 42 " + ARABIC;
        for (boolean rtl : new boolean[]{false, true}) {
            int[] vis = BidiUtil.reorderVisual(BidiUtil.resolveLevels(s, rtl));
            assertEquals(s.length(), vis.length);
            boolean[] seen = new boolean[s.length()];
            for (int v : vis) {
                assertTrue(v >= 0 && v < s.length());
                assertFalse(seen[v], "index " + v + " appeared twice");
                seen[v] = true;
            }
        }
    }

    @Test
    void emptyLineIsHandled() {
        assertEquals(0, BidiUtil.resolveLevels("", false).length);
        assertEquals(0, BidiUtil.reorderVisual(new byte[0]).length);
        assertTrue(BidiUtil.isTrivialLtr("", false));
    }
}
