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

/// `\R` takes a CR LF pair whole but gives the LF back when what follows
/// needs it, as Java 9 and later do. Expectations are JDK 21's answers, with
/// DOTALL on. One divergence is deliberate: the JDK does not split the pair
/// under a quantifier, so `\R{2}` fails on CR LF there, while here it is
/// `\R\R` and matches, as the documented equivalent of `\R` says it should.
class LineBreakBacktrackTest {
    private static final Object[][] CASES = {
        {"\\R\\R", "\r\n", true}, {"\\R", "\r\n", true}, {"\\R\\n", "\r\n", true},
        {"\\R.", "\r\n", true}, {"\\R$", "\r\n", true}, {"a\\Rb", "a\r\nb", true},
        {"\\R\\R", "\r\n\n", true},
    };

    @Test
    void eachCaseMatchesAsTheJdkDoes() {
        for (Object[] c : CASES) {
            Matcher m = Pattern.compile((String) c[0], Pattern.DOTALL).matcher((String) c[1]);
            assertEquals(c[2], m.matches(), (String) c[0]);
        }
    }

    @Test
    void findTakesThePairWhole() {
        Matcher m = Pattern.compile("\\R").matcher("x\r\ny");
        assertEquals(true, m.find());
        assertEquals(1, m.start());
        assertEquals(3, m.end());
    }
}
