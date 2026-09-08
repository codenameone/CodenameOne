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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TypeNamesTest {

    @Test
    void doesNotEndTheSummaryInsideACodeSpan() {
        // com.codename1.io.JSONWriter.ArrayBuilder opens with a code span that
        // contains full stops. Cutting there left the span unterminated, and the
        // summary is rendered as markdown on the package page and in search.
        String summary = TypeNames.summary("Fluent builder for `[ ..., ..., ... ]`. More prose here.");

        assertEquals("Fluent builder for `[ ..., ..., ... ]`.", summary);
        assertEquals(0, summary.chars().filter(c -> c == '`').count() % 2,
                "the backticks must balance");
    }

    @Test
    void stillEndsAtAnOrdinarySentence() {
        assertEquals("Does a thing.", TypeNames.summary("Does a thing. And then another."));
    }

    @Test
    void stopsAtABlankLineSoAListDoesNotBecomeATableCell() {
        assertEquals("Intro", TypeNames.summary("Intro\n\n- one\n- two"));
    }

    @Test
    void neverLeavesACodeSpanOpenAtTheLengthCap() {
        String long_ = "x".repeat(200) + " and `an unclosed span starts here and runs well past the cap"
                + " with plenty more text to push it over".repeat(3);
        String summary = TypeNames.summary(long_);
        assertTrue(summary.chars().filter(c -> c == '`').count() % 2 == 0,
                "the cap must not cut a span open: " + summary);
    }
}
