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

package com.codename1.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for the Unicode Bidi Algorithm core used by the pure editor. Samples use unicode escapes
/// to keep the source ASCII only: Hebrew alef/bet/gimel = u05D0..u05D2, Arabic alef/beh/teh =
/// u0627/u0628/u062A, Arabic-Indic zero = u0660.
class BidiTest {

    private static final String HEB = "\u05D0\u05D1\u05D2";   // aleph bet gimel (strong R)
    private static final String ARABIC = "\u0627\u0628\u062A"; // alef beh teh (strong AL)

    @Test
    void classifiesCommonTypes() {
        assertEquals(Bidi.L, Bidi.typeOf('a'));
        assertEquals(Bidi.L, Bidi.typeOf('Z'));
        assertEquals(Bidi.EN, Bidi.typeOf('5'));
        assertEquals(Bidi.R, Bidi.typeOf('\u05D0'));
        assertEquals(Bidi.AL, Bidi.typeOf('\u0627'));
        assertEquals(Bidi.AN, Bidi.typeOf('\u0660'));
        assertEquals(Bidi.WS, Bidi.typeOf(' '));
        assertEquals(Bidi.ON, Bidi.typeOf('!'));
        assertEquals(Bidi.L, Bidi.typeOf('\u4E2D')); // CJK -> strong L
    }

    @Test
    void autoBaseDirectionFromFirstStrong() {
        assertFalse(Bidi.direction("hello") == Bidi.RTL);
        assertFalse(Bidi.direction("123 abc") == Bidi.RTL);      // digits are not strong; first strong is 'a'
        assertTrue(Bidi.direction(HEB) == Bidi.RTL);
        assertTrue(Bidi.direction("123 " + HEB) == Bidi.RTL);    // first strong is Hebrew
        assertFalse(Bidi.direction("abc " + HEB) == Bidi.RTL);   // first strong is Latin
        assertTrue(Bidi.direction(ARABIC + " abc") == Bidi.RTL); // first strong is Arabic
    }

    @Test
    void pureLtrIsIdentityOrder() {
        byte[] levels = Bidi.levels("hello", false);
        for (byte b : levels) {
            assertEquals(0, b);
        }
        int[] vis = Bidi.visualOrder(levels);
        assertArrayEquals(new int[]{0, 1, 2, 3, 4}, vis);
        assertTrue(Bidi.isLeftToRight("hello world 123", false));
    }

    @Test
    void pureRtlIsFullyReversed() {
        byte[] levels = Bidi.levels(HEB, true);
        for (byte b : levels) {
            assertEquals(1, b);
        }
        int[] vis = Bidi.visualOrder(levels);
        assertArrayEquals(new int[]{2, 1, 0}, vis);
        assertFalse(Bidi.isLeftToRight(HEB, true));
        assertFalse(Bidi.isLeftToRight(HEB, false)); // strong R makes even an LTR base non-trivial
    }

    @Test
    void embeddedRtlRunReversesWithinLtrParagraph() {
        // logical: a b c _ H0 H1 _ x y z   (H = Hebrew), LTR base
        String s = "abc \u05D0\u05D1 xyz";
        byte[] levels = Bidi.levels(s, false);
        int[] vis = Bidi.visualOrder(levels);
        // only the two Hebrew letters (indices 4,5) swap; everything else stays put
        assertArrayEquals(new int[]{0, 1, 2, 3, 5, 4, 6, 7, 8, 9}, vis);
    }

    @Test
    void numberRunStaysLtrInsideRtlParagraph() {
        // logical: H0 H1 _ '1' '2'  with RTL base. The number is an LTR island inside RTL text, so
        // visually (left to right) the number reads "12" on the left, then space, then the reversed Hebrew.
        String s = "\u05D0\u05D1 12";
        byte[] levels = Bidi.levels(s, true);
        assertEquals(1, levels[0]); // Hebrew
        assertEquals(1, levels[1]);
        assertEquals(2, levels[3]); // '1' European number -> level base+1 = 2 inside RTL
        assertEquals(2, levels[4]); // '2'
        int[] vis = Bidi.visualOrder(levels);
        assertArrayEquals(new int[]{3, 4, 2, 1, 0}, vis);
    }

    @Test
    void arabicNumberFollowsArabicLetters() {
        // W2: European digits after an Arabic letter become Arabic numbers (still an LTR-ordered run).
        String s = ARABIC + "12";
        byte[] levels = Bidi.levels(s, true);
        // Arabic letters resolve to R (level 1); the digits are AN -> level 2
        assertEquals(1, levels[0]);
        assertEquals(2, levels[levels.length - 1]);
    }

    @Test
    void reorderIsAStablePermutation() {
        String s = "abc " + HEB + " 42 " + ARABIC;
        for (boolean rtl : new boolean[]{false, true}) {
            int[] vis = Bidi.visualOrder(Bidi.levels(s, rtl));
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
        assertEquals(0, Bidi.levels("", false).length);
        assertEquals(0, Bidi.visualOrder(new byte[0]).length);
        assertTrue(Bidi.isLeftToRight("", false));
    }

    private static String levelString(String text, boolean rtl) {
        byte[] lv = Bidi.levels(text, rtl);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < lv.length; i++) {
            char c = text.charAt(i);
            boolean removed = c >= 0x202A && c <= 0x202E;
            b.append(removed ? 'x' : (char) ('0' + lv[i]));
        }
        return b.toString();
    }

    /// UAX #9 BidiCharacterTest-style known answers covering explicit embeddings, overrides and isolates
    /// (X1-X10), bracket pairs (N0), number separators (W4-W6) and segment separators (L1). An `x`
    /// marks a character removed by rule X9.
    @Test
    void uax9KnownAnswers() {
        String[][] cases = {
            {"a (b) c", "rtl", "2222222"},
            {"\u05D0 (b) \u05D1", "rtl", "1112111"},
            {"a (\u05D0) b", "ltr", "0001000"},
            {"\u05D0 (\u05D1) c", "ltr", "1111100"},
            {"a (b", "rtl", "2222"},
            {"\u05D0 [1]", "rtl", "11121"},
            {"a\u202Bb c\u202Cd", "ltr", "0x222x0"},
            {"a\u202Ebc\u202Cd", "ltr", "0x11x0"},
            {"\u202D\u05D0\u05D1\u202C", "rtl", "x22x"},
            {"a \u2067b c\u2069 d", "ltr", "000222000"},
            {"a\u2068\u05D0 b\u2069c", "ltr", "0011200"},
            {"\u05D0\u2068b\u2069", "rtl", "1121"},
            {"\u05D0 \u2066b\u2069 \u05D1", "ltr", "1112111"},
            {"a\u2069b", "ltr", "000"},
            {"\u05D0 1.5", "ltr", "11222"},
            {"\u05D0 1,000.50", "ltr", "1122222222"},
            {"\u05D0 1+2", "ltr", "11222"},
            {"\u05D0 $12", "ltr", "11222"},
            {"\u0627 12", "ltr", "1122"},
            {"1-2 \u05D0", "rtl", "22211"},
            {"a\u0301 b", "rtl", "2222"},
            {"\u05D0\t\u05D1", "ltr", "101"},
            {"\u05D0\u200Ea", "ltr", "100"},
        };
        for (String[] c : cases) {
            assertEquals(c[2], levelString(c[0], "rtl".equals(c[1])), c[0]);
        }
    }

    @Test
    void explicitControlsAreNotTrivial() {
        assertFalse(Bidi.isLeftToRight("a\u2067b\u2069", false));
        assertTrue(Bidi.isLeftToRight("a (b) c", false));
    }

    @Test
    void otherScriptsAreLeftToRightAndBracketsMirror() {
        // Greek and Cyrillic are strong left to right, not neutrals.
        assertEquals(Bidi.LTR, Bidi.strongDirection('\u03b1'));
        assertEquals(Bidi.LTR, Bidi.strongDirection('\u0436'));
        assertEquals(Bidi.LTR, Bidi.direction("123 \u0436 " + HEB));
        assertEquals(Bidi.NEUTRAL, Bidi.direction("123 !"));
        assertEquals(')', Bidi.mirror('('));
        assertTrue(Bidi.isMark('\u05b0'));
        // The bracket pair is resolved as a pair (N0) and mirrored in the right-to-left run.
        assertEquals("(" + new StringBuilder(HEB).reverse() + ")", Bidi.reorder("(" + HEB + ")", true));
    }
}
