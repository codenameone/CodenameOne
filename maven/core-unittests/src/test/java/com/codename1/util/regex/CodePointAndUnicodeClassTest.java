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
package com.codename1.util.regex;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodePointAndUnicodeClassTest {
    private static final String FACE = "\ud83d\ude00";

    private static boolean matches(String regex, String text) {
        return Pattern.compile(regex).matcher(text).matches();
    }

    private static String replaceAll(String regex, String text, String with) {
        return Pattern.compile(regex).matcher(text).replaceAll(with);
    }

    @Test
    void aDotTakesAWholeSurrogatePair() {
        assertTrue(matches(".", FACE));
        assertEquals("x", replaceAll(".", FACE, "x"));
        assertEquals("xx", replaceAll(".", "a" + FACE, "x"));
        assertTrue(matches(".{2}", FACE + FACE));
        assertTrue(matches(".*?" + FACE, "ab" + FACE));
    }

    @Test
    void aClassHoldsCodePointsAboveTheBmp() {
        assertTrue(matches("[" + FACE + "]", FACE));
        assertTrue(matches("[\\x{1F600}-\\x{1F64F}]+", FACE + FACE));
        assertFalse(matches("[^" + FACE + "]", FACE));
        assertTrue(matches("[^a]", FACE));
    }

    @Test
    void backtrackingStepsBackOverAWholePair() {
        assertTrue(matches(".*" + FACE, FACE + FACE));
        assertTrue(matches(".+a", FACE + "a"));
        Matcher m = Pattern.compile("(?<=" + FACE + ")b").matcher(FACE + "b");
        assertTrue(m.find());
        assertEquals(2, m.start());
        assertTrue(Pattern.compile("(?<=.)b").matcher(FACE + "b").find());
    }

    @Test
    void scriptsBlocksAndCategories() {
        assertTrue(matches("\\p{IsGreek}+", "\u03b1\u03b2"));
        assertFalse(matches("\\p{IsGreek}", "a"));
        assertTrue(matches("\\p{script=Latn}+", "abc"));
        assertTrue(matches("\\p{InGreek}", "\u03b1"));
        assertTrue(matches("\\p{block=Arrows}", "\u2190"));
        assertTrue(matches("\\p{Sc}", "$"));
        assertTrue(matches("\\p{IsPd}", "-"));
        assertTrue(matches("\\p{gc=Mn}", "\u0301"));
    }

    @Test
    void unicodeCharacterClassWidensTheShorthands() {
        assertTrue(matches("(?U)\\d+", "\u0661"));
        assertFalse(matches("\\d+", "\u0661"));
        assertTrue(matches("(?U)\\w+", "\u00e9\u0301"));
        assertTrue(matches("(?U)\\s", "\u2003"));
        assertTrue(Pattern.compile("\\d", Pattern.UNICODE_CHARACTER_CLASS).matcher("\u0661").matches());
        assertTrue(Pattern.compile("(?U)\\b\u00e9").matcher(" \u00e9").find());
        assertTrue(matches("(?U)\\p{Alpha}", "\u03b1"));
    }

    @Test
    void aMatchNeverStartsInsideAPair() {
        assertFalse(Pattern.compile("[^\\x{1F600}-\\x{1F64F}]").matcher(FACE).find());
        assertFalse(Pattern.compile("[^\\x{10400}]", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher("\ud801\udc28").find());
        assertFalse(Pattern.compile("\\uDE00").matcher(FACE).find());
    }

    @Test
    void escapedAndQuotedPairsAreOneCodePoint() {
        assertTrue(matches("\\uD83D\\uDE00", FACE));
        assertTrue(matches("\\uD83D\\uDE00+", FACE + FACE));
        assertTrue(matches("[\\uD83D\\uDE00]", FACE));
        assertTrue(matches("\\Q" + FACE + "\\E", FACE));
        assertTrue(matches("\\Q" + FACE + "\\E+", FACE + FACE));
        assertTrue(matches(Pattern.quote("a" + FACE), "a" + FACE));
        assertTrue(matches("[\\Q" + FACE + "\\E]", FACE));
        assertFalse(Pattern.compile("[^\\Q" + FACE + "\\E]").matcher(FACE).find());
    }

    @Test
    void anEmptyMatchBetweenTheHalvesUnlessThePatternNamesAPair() {
        assertEquals("-\ud83d-\ude00-", Pattern.compile("x*").matcher(FACE).replaceAll("-"));
    }

    @Test
    void binaryPropertiesAndCategoriesAnswerForAllOfUnicode() {
        assertTrue(matches("\\p{IsPunctuation}", "!"));
        assertTrue(matches("\\p{IsTITLECASE}", "\u01c5"));
        assertTrue(Pattern.compile("\\p{IsTitlecase}", Pattern.CASE_INSENSITIVE).matcher("a").matches());
        assertTrue(matches("\\p{IsJoin_Control}", "\u200d"));
        assertTrue(matches("\\p{IsNoncharacter_Code_Point}", "\ufffe"));
        assertFalse(matches("\\p{IsWhite_Space}", "\u001c"));
        assertTrue(matches("\\p{L}", "\u0250"));
        assertTrue(Pattern.compile("\\p{gc=Lu}", Pattern.CASE_INSENSITIVE).matcher("a").matches());
        assertFalse(matches("\\p{Lu}", "\u2160"));
    }

    private static final String DESERET = "\ud801\udc00\ud801\udc28";

    @Test
    void aNegatedPropertyNeverTakesHalfAPair() {
        assertFalse(Pattern.compile("[^\\p{L}]").matcher(DESERET).find());
        assertFalse(Pattern.compile("\\P{L}").matcher(DESERET).find());
        assertFalse(Pattern.compile("(?U)\\W").matcher(DESERET).find());
    }

    @Test
    void aCaselessBackReferenceComparesCodePoints() {
        assertTrue(matches("(?iu)(.)\\1", DESERET));
        assertTrue(matches("(?iu)(\\x{10400})\\1", DESERET));
    }

    @Test
    void aCaselessClassTakesTheLowerFormOfTheUpper() {
        assertTrue(matches("(?iu)[a-z]", "\u0131"));
        assertTrue(matches("(?iu)[i]", "\u0131"));
        assertTrue(matches("(?iu)[a-z]", "\u017f"));
    }

    @Test
    void aMarkBelongsToTheLetterBeforeIt() {
        assertEquals("|a\u0301b|", Pattern.compile("\\b").matcher("a\u0301b").replaceAll("|"));
        assertEquals("a|\u0301|b", Pattern.compile("\\B").matcher("a\u0301b").replaceAll("|"));
    }

    @Test
    void usePatternKeepsTheMatchButNotItsGroups() {
        Matcher m = Pattern.compile("(a)").matcher("aab");
        assertTrue(m.find());
        m.usePattern(Pattern.compile("b"));
        assertEquals(null, m.group(0));
        assertEquals(0, m.start());
    }

    @Test
    void aBoundaryInsideAPairReadsTheLoneHalves() {
        Matcher m = Pattern.compile("(?U)\\b.").matcher(DESERET);
        assertTrue(m.find());
        assertEquals(0, m.start());
        assertFalse(m.find());
        assertTrue(matches("\\p{all}+", FACE + "a"));
    }
}
