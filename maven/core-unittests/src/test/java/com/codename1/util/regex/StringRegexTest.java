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

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/// Holds [StringRegex], the stand-in for the regular expression methods of
/// `java.lang.String`, to the answers of the JDK's own `String`.
class StringRegexTest {

    private static final String[] INPUTS = {"", "a", "a,b,c", ",a,,b,", ",,,", "a, b ,c", "one  two   three",
        "key=value=more", "a.b.c", "a|b|c", "x1y22z333", "  lead", "trail  ", "a\tb\nc d", "abc", "aXbxc",
        "1+2-3*4", "a$b$c", "\\a\\b", "a^b", "boo:and:foo", "a::b", "(a)(b)", "a[1]b[2]"};

    private static final String[] REGEXES = {",", ", *", "\\s+", " ", "=", "\\.", "\\|", "[0-9]+", "\\d", "\\t",
        "", "b", "[xX]", "[+*-]", "\\$", "\\\\", "\\^", ":", "o", "::", "[()]", "\\[|\\]", "a|c", ",|=", "\\s*,\\s*",
        "$", "z"};

    @Test
    void splitMatchesTheJdkOverEveryInputAndPattern() {
        for (String input : INPUTS) {
            for (String regex : REGEXES) {
                String at = "\"" + input + "\".split(\"" + regex + "\")";
                assertEquals(Arrays.asList(input.split(regex)), Arrays.asList(StringRegex.split(input, regex)), at);
            }
        }
    }

    @Test
    void splitWithALimitMatchesTheJdk() {
        int[] limits = {-1, 0, 1, 2, 3, 5, 100};
        for (String input : INPUTS) {
            for (String regex : REGEXES) {
                for (int limit : limits) {
                    String at = "\"" + input + "\".split(\"" + regex + "\", " + limit + ")";
                    assertEquals(Arrays.asList(input.split(regex, limit)),
                            Arrays.asList(StringRegex.split(input, regex, limit)), at);
                }
            }
        }
    }

    @Test
    void theOtherMembersMatchTheJdk() {
        for (String input : INPUTS) {
            for (String regex : REGEXES) {
                String at = "\"" + input + "\" with \"" + regex + "\"";
                assertEquals(input.matches(regex), StringRegex.matches(input, regex), at);
                assertEquals(input.replaceAll(regex, "<$0>"), StringRegex.replaceAll(input, regex, "<$0>"), at);
                assertEquals(input.replaceFirst(regex, "[$0]"), StringRegex.replaceFirst(input, regex, "[$0]"), at);
            }
        }
    }

    @Test
    void splitRefusesNull() {
        try {
            StringRegex.split("a", null);
            fail();
        } catch (NullPointerException expected) {
            // As the JDK's.
        }
        try {
            StringRegex.split(null, ",");
            fail();
        } catch (NullPointerException expected) {
            // As a call on null would.
        }
    }
}
