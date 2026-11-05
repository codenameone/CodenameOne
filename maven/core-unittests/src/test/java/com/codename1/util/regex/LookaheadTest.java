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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// `(?=...)` and `(?!...)` are zero-width: they test the text ahead without
/// consuming it. Expectations are the JDK's answers for the same patterns.
class LookaheadTest {

    @Test
    void positiveLookaheadMatchesWithoutConsuming() {
        Matcher m = Pattern.compile("foo(?=bar)").matcher("foobaz foobar");
        assertTrue(m.find());
        assertEquals(7, m.start());
        assertEquals(10, m.end());
        assertEquals("foo", m.group());
    }

    @Test
    void negativeLookaheadRejectsWhatFollows() {
        Matcher m = Pattern.compile("foo(?!bar)").matcher("foobar foobaz");
        assertTrue(m.find());
        assertEquals(7, m.start());
        assertFalse(Pattern.compile("^(?!abc)abc").matcher("abc").find());
        assertTrue(Pattern.compile("^(?!abc)ab").matcher("abd").find());
    }

    @Test
    void quantifiedGroupInsideLookahead() {
        Matcher m = Pattern.compile("^(?=(?:\\d{2})+$)\\d+").matcher("1234");
        assertTrue(m.find());
        assertEquals("1234", m.group());
        assertFalse(Pattern.compile("^(?=(?:\\d{2})+$)\\d+").matcher("123").find());
        assertTrue(Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{6,}$").matcher("abC1ef").find());
        assertFalse(Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{6,}$").matcher("abc1ef").find());
    }

    @Test
    void positiveLookaheadKeepsItsCaptures() {
        Matcher m = Pattern.compile("(?=(\\w+))\\w").matcher("hello");
        assertTrue(m.find());
        assertEquals("h", m.group());
        assertEquals("hello", m.group(1));
        Matcher n = Pattern.compile("a(?!(b))c").matcher("ac");
        assertTrue(n.find());
        assertNull(n.group(1));
    }

    @Test
    void lookaheadAtTheEndOfThePattern() {
        Matcher m = Pattern.compile("\\d+(?=%)").matcher("50 75%");
        assertTrue(m.find());
        assertEquals("75", m.group());
        Matcher n = Pattern.compile("\\w+(?!\\w)").matcher("ab cd");
        assertTrue(n.find());
        assertEquals("ab", n.group());
        assertFalse(Pattern.compile("x(?=y)").matcher("x").find());
    }

    @Test
    void lookaheadInsideARepeatedGroup() {
        Matcher m = Pattern.compile("(?:\\w(?=\\w))+").matcher("abc");
        assertTrue(m.find());
        assertEquals("ab", m.group());
    }
}
