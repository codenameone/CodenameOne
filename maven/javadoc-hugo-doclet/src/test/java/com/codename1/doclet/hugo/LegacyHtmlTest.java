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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Cases taken from the 13 core sources that still carry HTML in their comments. */
class LegacyHtmlTest {

    @Test
    void convertsAListSoItSurvives() {
        // com.codename1.gaming.physics.box2d.common.Settings. The whole list of
        // tuning values disappeared from the page.
        String out = LegacyHtml.convert("Good lerp precision\n<ul>\n<li>.0092</li>\n<li>.008201</li>\n</ul>");
        assertTrue(out.contains("- .0092"), out);
        assertTrue(out.contains("- .008201"), out);
        assertFalse(out.contains("<li>"), "the tag itself must not survive");
    }

    @Test
    void convertsParagraphsAndBreaks() {
        assertTrue(LegacyHtml.convert("<p>Euler angles are in degrees.").contains("Euler angles"));
        assertTrue(LegacyHtml.convert("one</br>two").contains("one\ntwo"));
    }

    @Test
    void convertsInlineEmphasis() {
        assertEquals("**bold** and *thin*", LegacyHtml.convert("<b>bold</b> and <i>thin</i>"));
    }

    @Test
    void levelsHeadingsDownSoTheyDoNotOutrankThePage() {
        String out = LegacyHtml.convert("<h2>Start at the level you need</h2>");
        assertTrue(out.contains("#### Start at the level you need"), out);
    }

    @Test
    void escapesATagCarryingAttributes() {
        // com.codename1.util.regex.RE documents a substitution string. Rendering
        // it would turn the example into a link; dropping it loses the example.
        String out = LegacyHtml.convert("the substitution String \"<a href=\\\"$0\\\">$0</a>\"");
        assertTrue(out.contains("&lt;a href="), out);
        assertFalse(out.contains("<a href="), "must never become real markup");
    }

    @Test
    void escapesTagsThatAreReallyPlaceholders() {
        // PushBuilder writes <metadata>;<body> to mean one value then another.
        String out = LegacyHtml.convert("Push callback will receive <metadata>;<body>.");
        assertTrue(out.contains("&lt;metadata&gt;;&lt;body&gt;")
                || out.contains("&lt;metadata>;&lt;body>"), out);
    }

    @Test
    void turnsAPreBlockIntoAFencedCodeBlock() {
        // com.codename1.payment.CommerceManager's usage example collapsed into a
        // paragraph when the tags were dropped: two-space indentation is not a
        // markdown code block, and the line breaks went with it.
        String out = LegacyHtml.convert(String.join("\n",
                "Typical use:",
                "<pre>",
                "  CommerceManager cm = CommerceManager.getInstance();",
                "  cm.subscribe(\"pro_monthly\");",
                "</pre>"));

        assertTrue(out.contains("```"), out);
        assertTrue(out.contains("  cm.subscribe(\"pro_monthly\");"),
                "the indentation inside the block survives");
        assertFalse(out.contains("<pre>"), "the tag itself does not");
    }

    @Test
    void doesNotWrapAlreadyFencedPreContent() {
        // <pre>{@code ...}</pre> arrives here already fenced, because a multi-line
        // {@code} is rendered as a code block. A fence inside a fence is not one.
        String out = LegacyHtml.convert("<pre>\n```\nString s = \"x\";\n```\n</pre>");

        assertEquals(1, countOccurrences(out, "```") / 2, "exactly one fenced block: " + out);
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int at = text.indexOf(needle);
        while (at >= 0) {
            count++;
            at = text.indexOf(needle, at + needle.length());
        }
        return count;
    }

    @Test
    void leavesAMarkdownAutolinkAlone() {
        // com.codename1.security.Otp documents the key URI format as an
        // autolink. It starts with a letter after the angle bracket, so a tag
        // check that looks only at that claimed it and escaped it, and the page
        // showed the URL as literal angle-bracketed text.
        String source = "The format is documented at\n<https://github.com/google/x/wiki/Key-Uri-Format>";
        assertEquals(source, LegacyHtml.convert(source));
        assertEquals("<mailto:a@b.example>", LegacyHtml.convert("<mailto:a@b.example>"));
    }

    @Test
    void stillEscapesSomethingThatIsReallyATag() {
        assertTrue(LegacyHtml.convert("see <metadata>;<body>").contains("&lt;metadata"),
                "a placeholder is still not markup");
    }

    @Test
    void leavesFencedCodeAlone() {
        String source = "before\n```java\nString s = \"<b>not markup</b>\";\n```\nafter";
        assertEquals(source, LegacyHtml.convert(source));
    }

    @Test
    void leavesCodeSpansAlone() {
        assertEquals("use `<p>` here", LegacyHtml.convert("use `<p>` here"));
    }

    @Test
    void passesThroughABodyWithNoHtml() {
        String source = "Just prose with a < b comparison.";
        assertEquals(source, LegacyHtml.convert(source));
    }
}
