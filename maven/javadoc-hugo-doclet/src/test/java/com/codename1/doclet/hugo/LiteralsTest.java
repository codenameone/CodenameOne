/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.doclet.hugo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LiteralsTest {

    @Test
    void quotesStrings() {
        // com.codename1.l10n.DateFormatPatterns.RFC2822, which rendered as bare
        // text inside a code-formatted declaration.
        assertEquals("\"EEE, dd MMM yyyy HH:mm:ss Z\"",
                Literals.of("EEE, dd MMM yyyy HH:mm:ss Z"));
    }

    @Test
    void quotesCharacters() {
        assertEquals("'x'", Literals.of('x'));
    }

    @Test
    void escapesQuotesAndBackslashes() {
        assertEquals("\"a\\\"b\\\\c\"", Literals.of("a\"b\\c"));
        assertEquals("'\\''", Literals.of('\''));
    }

    @Test
    void escapesControlCharactersRatherThanEmittingThem() {
        // A raw control character in the front matter is rendered raw into the
        // page. The repository rejects one in its own sources outright.
        assertEquals("\"a\\tb\\nc\"", Literals.of("a\tb\nc"));
        assertEquals("\"\\u001f\"", Literals.of("\u001f"));
        assertEquals("'\\u0000'", Literals.of('\0'));
    }

    @Test
    void suffixesTheTypesThatNeedOne() {
        assertEquals("1L", Literals.of(1L));
        assertEquals("1.5f", Literals.of(1.5f));
        assertEquals("1", Literals.of(1));
        assertEquals("true", Literals.of(true));
    }

    @Test
    void writesAnArrayDefaultInJavaSyntax() {
        // "String[] options() default {}" is written {} in source; a list's
        // toString is not something Java would recognise.
        assertEquals("{}", Literals.of(java.util.List.of()));
        assertEquals("{\"a\", \"b\"}", Literals.of(java.util.List.of("a", "b")));
    }

    @Test
    void writesNonFiniteFloatingPointAsAnExpression() {
        // Infinity and NaN have no literal spelling, so toString produces text no
        // source could contain. Numeric.min() and Numeric.max() default to these.
        assertEquals("1.0/0.0", Literals.of(Double.POSITIVE_INFINITY));
        assertEquals("-1.0/0.0", Literals.of(Double.NEGATIVE_INFINITY));
        assertEquals("0.0/0.0", Literals.of(Double.NaN));
        assertEquals("1.0f/0.0f", Literals.of(Float.POSITIVE_INFINITY));
        assertEquals("0.0f/0.0f", Literals.of(Float.NaN));
        assertEquals("1.5", Literals.of(1.5d));
    }

    @Test
    void passesNullThroughSoTheTemplateCanTestIt() {
        assertNull(Literals.of(null));
    }
}
