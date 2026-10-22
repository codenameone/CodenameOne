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
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A group that can only match empty is tried once by `?` and `{0,1}`, as the
/// JDK does, and taken no round of by `*` and wider ranges. Expectations are
/// the JDK's answers, with `null` for a group left unset.
class ZeroWidthOptionalGroupTest {
    private static final String[][] CASES = {
        {"()?", "a", ""}, {"(^)?", "a", ""}, {"(\\b)?", "a", ""}, {"(){0,1}", "a", ""},
        {"(^){0,1}", "a", ""}, {"(){1,1}?", "a", ""}, {"()*", "a", null}, {"(^)??", "a", null},
        {"(^){0,3}", "a", null}, {"(){0,2}", "a", null}, {"(\\b)*", "a", null}, {"(){2,}", "a", ""},
        {"(?:(^)|x)?", "a", ""}, {"(^)?$", "a", null}, {"(?:a(^)?)+", "aa", null},
    };

    @Test
    void zeroWidthGroupsAreCapturedAsTheJdkDoes() {
        for (String[] c : CASES) {
            Matcher m = Pattern.compile(c[0]).matcher(c[1]);
            assertTrue(m.find(), c[0]);
            assertEquals(c[2], m.group(1), c[0]);
        }
    }

    @Test
    void aWordBoundaryTakesACountedQuantifier() {
        assertTrue(Pattern.compile("\\b{2}a").matcher("a").find());
        Matcher m = Pattern.compile("a\\b{0,2}").matcher("ab a");
        assertTrue(m.find());
        assertEquals(0, m.start());
    }
}
