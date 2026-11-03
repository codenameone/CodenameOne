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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        Element r = parse("<r a=\"x&amp;y&#65;\">&lt;a&gt; &amp; b&#x1F600;</r>", true, false);
        assertEquals("x&yA", r.getAttribute("a"));
        assertEquals("<a> & b\ud83d\ude00", r.getChildAt(0).getText());
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

    @Test
    void whitespaceAroundTheRootIsNotARoot() {
        Element r = parse("<?xml version=\"1.0\"?>\n<doc> <a/> </doc>\n", true, false);
        assertEquals("doc", r.getTagName());
        assertEquals(3, r.getNumChildren());
    }

    @Test
    void strictTagSearchesAreExact() {
        Element r = parse("<r><Name>bad</Name><name>good</name></r>", true, false);
        assertEquals(1, r.getChildrenByTagName("name").size());
        assertEquals("good", r.getFirstChildByTagName("name").getChildAt(0).getText());
        assertNull(r.getFirstChildByTagName("NAME"));
    }

    private static String failure(String xml) {
        return assertThrows(XMLParseException.class, () -> parse(xml, true, false)).getMessage();
    }

    @Test
    void malformedDocumentsAreRefused() {
        assertTrue(failure("<a><b></a>").contains("does not match"));
        assertTrue(failure("<a><b>").contains("end of document"));
        assertTrue(failure("<a></a></b>").contains("no open element"));
        assertTrue(failure("<a/><b/>").contains("second root"));
        assertTrue(failure("<a/>junk").contains("outside the root"));
        assertTrue(failure("junk<a/>").contains("outside the root"));
        assertTrue(failure("<a x=1/>").contains("not quoted"));
        assertTrue(failure("<a x='1' x='2'/>").contains("duplicate attribute"));
        assertTrue(failure("<a x='<'/>").contains("< in the value"));
        assertTrue(failure("<a><!-- open</a>").contains("unterminated comment"));
        assertTrue(failure("<a><![CDATA[x</a>").contains("unterminated CDATA"));
        assertTrue(failure("").contains("no root"));
        assertTrue(failure("<a x='1").contains("end of document"));
    }

    @Test
    void badEntitiesAreRefused() {
        assertTrue(failure("<a>&nbsp;</a>").contains("undefined entity &nbsp;"));
        assertTrue(failure("<a>&AMP;</a>").contains("undefined entity"));
        assertTrue(failure("<a>b&c</a>").contains("unterminated entity"));
        assertTrue(failure("<a x='&bogus;'/>").contains("undefined entity"));
        assertTrue(failure("<a>&#0;</a>").contains("undefined entity"));
        assertTrue(failure("<a>&#xD800;</a>").contains("undefined entity"));
        assertTrue(failure("<a>&#x110000;</a>").contains("undefined entity"));
        // the lenient mode keeps what it cannot decode, as it always has
        assertEquals("b&c", parse("<a>b&c</a>", false, false).getChildAt(0).getText());
    }

    @Test
    void anEntityTheApplicationAddsIsDefined() {
        XMLParser p = new XMLParser();
        p.setStrict(true);
        p.addCharEntity("nbsp", 160);
        assertEquals("\u00a0", p.parse(new StringReader("<a>&nbsp;</a>")).getChildAt(0).getText());
    }

    @Test
    void doctypeEntitiesAreNeverExpanded() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">"
                + "<!ENTITY a \"]>\">]><r>&xxe;</r>";
        assertTrue(failure(xxe).contains("undefined entity &xxe;"));
        String bomb = "<!DOCTYPE r [<!ENTITY a \"aaaa\"><!ENTITY b \"&a;&a;&a;\">]><r>&b;</r>";
        assertTrue(failure(bomb).contains("undefined entity &b;"));
        Element r = parse("<!DOCTYPE r [<!ENTITY a \"x\">]>\n<r>ok</r>", true, false);
        assertEquals("ok", r.getChildAt(0).getText());
    }

    @Test
    void nestingIsCapped() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            b.append("<a>");
        }
        assertTrue(failure(b.toString()).contains("nested deeper than 256"));
        XMLParser p = new XMLParser();
        p.setStrict(true);
        p.setMaxDepth(2);
        assertEquals(2, p.getMaxDepth());
        assertEquals("b", p.parse(new StringReader("<a><b/></a>")).getChildAt(0).getTagName());
        XMLParseException e = assertThrows(XMLParseException.class,
                () -> p.parse(new StringReader("<a><b><c/></b></a>")));
        assertTrue(e.getMessage().contains("nested deeper than 2"));
        assertThrows(IllegalArgumentException.class, () -> p.setMaxDepth(0));
    }

    @Test
    void aByteOrderMarkAndTheDeclarationAreSkipped() {
        Element r = parse("\ufeff<?xml version=\"1.0\"?>\n<r/>\n", true, false);
        assertEquals("r", r.getTagName());
    }

    @Test
    void eventModeIsStrictToo() {
        XMLParser p = new XMLParser();
        p.setStrict(true);
        assertThrows(XMLParseException.class, () -> p.eventParser(new StringReader("<a><b></a>")));
        assertThrows(XMLParseException.class, () -> p.eventParser(new StringReader("  ")));
    }

    @Test
    void elementHelpers() {
        Element r = parse("<r><i n='1'> x <![CDATA[y]]> </i><j/><i n='2'/></r>", true, false);
        assertSame(r.getFirstChildByTagName("i"), r.child("i"));
        assertEquals(2, r.children("i").size());
        assertEquals("2", r.children("i").get(1).getAttribute("n"));
        assertTrue(r.children("none").isEmpty());
        assertNull(r.child("none"));
        assertEquals("x y", r.childText("i"));
        assertEquals("", r.childText("j"));
        assertNull(r.childText("none"));
    }
}
