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
    void doesNotEndTheSummaryAtAnAbbreviation() {
        // AdError.CODE_INVALID_REQUEST was cut at the "g." of "e.g.", leaving the
        // summary ending mid-parenthesis in the package table and in search.
        assertEquals("Error code used when the request was rejected as invalid (e.g. a bad ad unit id).",
                TypeNames.summary(
                        "Error code used when the request was rejected as invalid (e.g. a bad ad unit id)."));
        assertEquals("Use the other one, i.e. the new API.",
                TypeNames.summary("Use the other one, i.e. the new API. More prose."));
    }

    @Test
    void reducesMarkdownToWordsForTheSearchIndex() {
        // The results list escapes what it is given, so a summary still carrying
        // markdown shows its own source: 622 of the 2096 types displayed
        // backticks, emphasis markers or a whole link destination.
        assertEquals("Default PII scrubber for CrashProtection uploads.",
                TypeNames.plainSummary("Default PII scrubber for `CrashProtection` uploads."));
        assertEquals("See the guide for details.",
                TypeNames.plainSummary("See the [guide](/developer-guide/) for details."));
        assertEquals("Important: read this.",
                TypeNames.plainSummary("**Important**: read this."));
    }

    @Test
    void keepsBalancedParenthesesInsideALinkDestination() {
        // A member URL ends in a signature, so its destination holds balanced
        // parentheses. Stopping at the first one left it behind: AdError arrived
        // in the search index as "AdListener.onFailedToLoad(AdError))".
        assertEquals("Raised by AdListener.onFailedToLoad(AdError).",
                TypeNames.plainSummary(
                        "Raised by [AdListener.onFailedToLoad(AdError)]"
                        + "(/javadoc/com/codename1/ads/AdListener/#onFailedToLoad(int))."));
    }

    @Test
    void leavesArithmeticAloneInAPlainSummary() {
        // WebMercator writes "tileSize * 2^zoom", which is multiplication rather
        // than emphasis and has to survive intact.
        assertEquals("The world spans tileSize * 2^zoom pixels.",
                TypeNames.plainSummary("The world spans tileSize * 2^zoom pixels."));
    }

    @Test
    void closesASpanTheLengthCapWouldLeaveOpen() {
        // BrowserNavigationCallback opens with a bold note that runs past the
        // cap. Backing out of it would leave nothing, so the cut closes it.
        String bold = "**" + "Important, and rather long. ".repeat(12) + "**";
        String summary = TypeNames.summary(bold);
        assertEquals(0, countOf(summary, "**") % 2, "the emphasis is balanced: " + summary);
    }

    private static int countOf(String text, String needle) {
        int n = 0;
        int at = text.indexOf(needle);
        while (at >= 0) {
            n++;
            at = text.indexOf(needle, at + needle.length());
        }
        return n;
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
