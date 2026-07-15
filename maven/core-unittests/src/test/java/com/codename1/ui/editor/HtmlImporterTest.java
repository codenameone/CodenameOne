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

package com.codename1.ui.editor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/// Pure tests for the HTML importer, focused on entity decoding (named, numeric and hex).
class HtmlImporterTest {

    private static String text(String html) {
        return HtmlImporter.parse(html).getText();
    }

    @Test
    void decodesBasicEntities() {
        assertEquals("a & b < c > d \" e ' f", text("<p>a &amp; b &lt; c &gt; d &quot; e &apos; f</p>"));
    }

    @Test
    void decodesNamedTypographyEntities() {
        // mdash, ndash, hellip, curly quotes
        assertEquals("a \u2014 b", text("<p>a &mdash; b</p>"));
        assertEquals("a \u2013 b", text("<p>a &ndash; b</p>"));
        assertEquals("go\u2026", text("<p>go&hellip;</p>"));
        assertEquals("\u201Chi\u201D \u2018yo\u2019", text("<p>&ldquo;hi&rdquo; &lsquo;yo&rsquo;</p>"));
    }

    @Test
    void decodesSymbolEntities() {
        assertEquals("\u00A9 \u00AE \u2122", text("<p>&copy; &reg; &trade;</p>"));
        assertEquals("5 \u00D7 3 = 15", text("<p>5 &times; 3 = 15</p>"));
        assertEquals("\u20AC100 \u00A350", text("<p>&euro;100 &pound;50</p>"));
    }

    @Test
    void decodesNumericEntities() {
        assertEquals("A B", text("<p>&#65; &#66;</p>"));     // decimal
        assertEquals("A\u2014B", text("<p>&#65;&#x2014;&#66;</p>")); // hex mdash between letters
    }

    @Test
    void leavesUnknownEntitiesUntouched() {
        // an unknown entity is passed through verbatim rather than dropped
        assertTrue(text("<p>a &bogus; b</p>").contains("&bogus;"));
    }

    @Test
    void retainsModernDataAttributesInFragmentMode() {
        HtmlImporter.Result result = HtmlImporter.parse("<p data-indent=\"3\">indented</p>");
        assertEquals(3, result.getBlocks().get(0).indent);
    }
}
