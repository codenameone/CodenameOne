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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The bodies here are copied from real framework sources rather than invented,
 * because the whole parser exists to read one specific house convention. A test
 * written against an imagined shape would pass while the real comments were
 * being mangled.
 */
class MarkdownSectionsTest {

    @Test
    void liftsParametersAndReturnsOutOfProse() {
        // com.codename1.maps.Mercator.forwardMercator, verbatim.
        MarkdownSections.Result r = MarkdownSections.parse(String.join("\n",
                "Create a projected Mercator Coord from the given coordinate",
                "",
                "#### Parameters",
                "",
                "- `latitude`: to project",
                "",
                "- `longitude`: to project",
                "",
                "#### Returns",
                "",
                "a projected Mercator"));

        assertEquals("Create a projected Mercator Coord from the given coordinate", r.description());
        assertEquals(List.of(
                new MarkdownSections.NamedText("latitude", "to project"),
                new MarkdownSections.NamedText("longitude", "to project")), r.parameters());
        assertEquals("a projected Mercator", r.returns());
    }

    @Test
    void liftsThrows() {
        // com.codename1.io.ConnectionRequest.fetchJSON, verbatim.
        MarkdownSections.Result r = MarkdownSections.parse(String.join("\n",
                "Utility method that fetches JSON.",
                "",
                "#### Throws",
                "",
                "- `IOException`: in case of an error"));

        assertEquals(List.of(new MarkdownSections.NamedText("IOException", "in case of an error")),
                r.exceptions());
    }

    @Test
    void keepsSeeAlsoItemsWhole() {
        // com.codename1.ui.Component uses both a bare type and a member reference.
        MarkdownSections.Result r = MarkdownSections.parse(String.join("\n",
                "A component.",
                "",
                "#### See also",
                "",
                "- Container",
                "- #getBaselineResizeBehavior"));

        assertEquals(List.of("Container", "#getBaselineResizeBehavior"), r.seeAlso());
        assertEquals("A component.", r.description());
    }

    @Test
    void leavesProseHeadingsInPlace() {
        // "Threading", "Example", "Platform support" and a long tail of one-offs are
        // ordinary prose. Claiming one would move author copy somewhere unintended.
        String body = String.join("\n",
                "Relays state over REST.",
                "",
                "#### Threading",
                "",
                "Both methods are called from a background thread and block.",
                "",
                "#### Parameters",
                "",
                "- `url`: where to post");

        MarkdownSections.Result r = MarkdownSections.parse(body);

        assertTrue(r.description().contains("#### Threading"));
        assertTrue(r.description().contains("called from a background thread"));
        assertEquals(1, r.parameters().size());
        assertEquals("url", r.parameters().get(0).name());
    }

    @Test
    void ignoresHeadingsInsideFencedCode() {
        // #### Example sections contain java fences, and a fence can contain anything.
        String body = String.join("\n",
                "Shows a code editor.",
                "",
                "#### Example",
                "",
                "```java",
                "// #### Parameters",
                "String s = \"#### Returns\";",
                "```",
                "",
                "#### Returns",
                "",
                "the editor");

        MarkdownSections.Result r = MarkdownSections.parse(body);

        assertEquals("the editor", r.returns());
        assertTrue(r.parameters().isEmpty(), "a fenced line must not open a section");
        assertTrue(r.description().contains("// #### Parameters"));
    }

    @Test
    void foldsContinuationLinesIntoTheirBullet() {
        MarkdownSections.Result r = MarkdownSections.parse(String.join("\n",
                "Does a thing.",
                "",
                "#### Parameters",
                "",
                "- `cmp`: the component to add, which may be",
                "  null when the container is empty",
                "",
                "- `index`: the position"));

        assertEquals(2, r.parameters().size());
        assertTrue(r.parameters().get(0).text().contains("null when the container is empty"));
        assertEquals("index", r.parameters().get(1).name());
    }

    @Test
    void dropsSinceEntirely() {
        // Availability metadata is rejected in sources by scripts/check-since-tags.sh
        // and must not reappear in the rendered API. Ports/CLDC11 still carries some.
        MarkdownSections.Result r = MarkdownSections.parse(String.join("\n",
                "Returns the value.",
                "",
                "#### Since",
                "",
                "1.5"));

        assertEquals("Returns the value.", r.description());
        assertTrue(!r.description().contains("1.5"), "the version must not fall back into the prose");
    }

    @Test
    void recordsAnEmptyDeprecationSectionAsDeprecated() {
        MarkdownSections.Result plain = MarkdownSections.parse("Still fine.");
        assertNull(plain.deprecated());

        MarkdownSections.Result flagged = MarkdownSections.parse(String.join("\n",
                "Old way.",
                "",
                "#### Deprecated",
                "",
                "use the other one"));
        assertEquals("use the other one", flagged.deprecated());
    }

    @Test
    void keepsASentenceContainingAColonWhole() {
        // "Note: this blocks" must not become a parameter named "Note".
        MarkdownSections.Result r = MarkdownSections.parse(String.join("\n",
                "Does a thing.",
                "",
                "#### Parameters",
                "",
                "- the value to use, note: it blocks"));

        assertEquals(1, r.parameters().size());
        assertEquals("", r.parameters().get(0).name());
        assertEquals("the value to use, note: it blocks", r.parameters().get(0).text());
    }

    @Test
    void stripsATagTheConversionDuplicatedIntoTheBulletText() {
        // com.codename1.ui.animations.Transition.copy, verbatim. 1157 bullets
        // across 184 files name the parameter twice like this, and the standard
        // pages show the second one as prose.
        MarkdownSections.Result r = MarkdownSections.parse(String.join("\n",
                "Create a copy of the transition.",
                "",
                "#### Parameters",
                "",
                "- `reverse`: @param reverse creates a new transition instance"));

        assertEquals("creates a new transition instance", r.parameters().get(0).text());
    }

    @Test
    void stripsADuplicatedThrowsTagAndItsDash() {
        MarkdownSections.Result r = MarkdownSections.parse(String.join("\n",
                "Reads a value.",
                "",
                "#### Throws",
                "",
                "- `ArrayIndexOutOfBoundsException`: @throws ArrayIndexOutOfBoundsException - if the index is bad"));

        assertEquals("if the index is bad", r.exceptions().get(0).text());
    }

    @Test
    void leavesATagNamingSomethingElseAlone() {
        // Only a tag naming the same thing as the bullet is residue; anything
        // else is the author's text and must survive.
        MarkdownSections.Result r = MarkdownSections.parse(String.join("\n",
                "Does a thing.",
                "",
                "#### Parameters",
                "",
                "- `first`: @param second is documented elsewhere"));

        assertEquals("@param second is documented elsewhere", r.parameters().get(0).text());
    }

    @Test
    void passesThroughABodyWithNoStructure() {
        String body = "Just prose.\n\nWith a second paragraph.";
        MarkdownSections.Result r = MarkdownSections.parse(body);

        assertEquals(body, r.description());
        assertTrue(r.parameters().isEmpty());
        assertNull(r.returns());
    }
}
