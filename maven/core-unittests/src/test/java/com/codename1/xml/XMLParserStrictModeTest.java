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
package com.codename1.xml;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Strict mode keeps OOXML exactly as written; the default mode is unchanged.
class XMLParserStrictModeTest {

    private static Element parse(String xml, boolean strict, boolean strip) {
        XMLParser p = new XMLParser();
        p.setStrict(strict);
        p.setStripNamespacePrefixes(strip);
        return p.parse(new StringReader(xml));
    }

    @Test
    void strictKeepsWhitespaceExactly() {
        Element r = parse("<r><t xml:space=\"preserve\">  a  b </t><t> </t>\n<t>x</t></r>", true, false);
        assertEquals(4, r.getNumChildren());
        assertEquals("  a  b ", r.getChildAt(0).getChildAt(0).getText());
        assertEquals(" ", r.getChildAt(1).getChildAt(0).getText());
        assertEquals("\n", r.getChildAt(2).getText());
        assertEquals("preserve", r.getChildAt(0).getAttribute("xml:space"));
    }

    @Test
    void defaultModeStillCollapsesLeadingWhitespace() {
        Element r = parse("<r><t>  a  b </t><t> </t></r>", false, false);
        assertEquals(" a  b ", r.getChildAt(0).getChildAt(0).getText());
        assertEquals(0, r.getChildAt(1).getNumChildren());
    }

    @Test
    void strictIsCaseExact() {
        Element r = parse("<Doc Val=\"1\" val=\"2\"><Child/></Doc>", true, false);
        assertEquals("Doc", r.getTagName());
        assertEquals("1", r.getAttribute("Val"));
        assertEquals("2", r.getAttribute("val"));
        assertNull(r.getAttribute("VAL"));
        assertEquals("Child", r.getChildAt(0).getTagName());
    }

    @Test
    void entitiesAreDecodedInTextAndAttributes() {
        Element r = parse("<r a=\"x&amp;y&#65;\">&lt;a&gt; &amp; b&c</r>", true, false);
        assertEquals("x&yA", r.getAttribute("a"));
        assertEquals("<a> & b&c", r.getChildAt(0).getText());
    }

    @Test
    void textAcrossTheReadBufferBoundary() {
        StringBuilder sb = new StringBuilder("<r>");
        for (int i = 0; i < 3000; i++) {
            sb.append("abc&amp;");
        }
        sb.append("</r>");
        String text = parse(sb.toString(), true, false).getChildAt(0).getText();
        assertEquals(3000 * 4, text.length());
        assertTrue(text.startsWith("abc&abc&"));
        assertTrue(text.endsWith("abc&"));
    }

    @Test
    void repeatedNamesAndValuesArePooled() {
        Element r = parse("<w:body><w:p w:val=\"left\"/><w:p w:val=\"left\"/></w:body>", true, false);
        Element a = r.getChildAt(0);
        Element b = r.getChildAt(1);
        assertSame(a.getTagName(), b.getTagName());
        assertSame(a.getAttribute("w:val"), b.getAttribute("w:val"));
    }

    @Test
    void namespacePrefixesCanBeStripped() {
        Element r = parse("<w:document xmlns:w=\"urn:w\"><w:p w:val=\"1\"/></w:document>", true, true);
        assertEquals("document", r.getTagName());
        assertEquals("urn:w", r.getAttribute("xmlns:w"));
        Element p = r.getChildAt(0);
        assertEquals("p", p.getTagName());
        assertEquals("1", p.getAttribute("val"));
    }
}
