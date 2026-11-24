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
package com.codename1.wordprocessing;


import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Inputs found by review: memory and stack exhaustion from tiny files, and round trips
/// that drifted.
class HostileInputTest {
    private static Document pages() throws IOException {
        return Document.read(HostileInputTest.class.getResourceAsStream(
                "/wordprocessing/pages.docx"), Document.Format.DOCX);
    }

    private static byte[] write(Document d, Document.Format f) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        d.write(out, f);
        return out.toByteArray();
    }

    private static Document read(byte[] b, Document.Format f) throws IOException {
        return Document.read(new ByteArrayInputStream(b), f);
    }

    private static List<String> styles(Document d) {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < d.size(); i++) {
            if (d.get(i) instanceof Paragraph) {
                out.add(((Paragraph) d.get(i)).style);
            }
        }
        return out;
    }

    @Test
    void odtReusesParagraphStylesByTheirRealName() throws IOException {
        Document d = pages();
        Document back = read(write(d, Document.Format.ODT), Document.Format.ODT);
        for (String s : styles(back)) {
            assertFalse(s.indexOf(':') >= 0, s);
        }
        Document again = read(write(back, Document.Format.ODT), Document.Format.ODT);
        assertEquals(styles(back), styles(again));
    }

    @Test
    void repeatedOdtCellsAreBoundedInTotal() throws IOException {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < 330; i++) {
            rows.append("<table:table-row table:number-rows-repeated=\"100\"><table:table-cell "
                    + "table:number-columns-repeated=\"1024\"><text:p>x</text:p>"
                    + "</table:table-cell></table:table-row>");
        }
        String body = "<office:document-content xmlns:office=\"o\" xmlns:table=\"t\" "
                + "xmlns:text=\"x\"><office:body><office:text><table:table>" + rows
                + "</table:table></office:text></office:body></office:document-content>";
        byte[] odt = OdtFormatTest.zip(new String[] {"content.xml"}, new String[] {body});
        assertThrows(IOException.class, () -> read(odt, Document.Format.ODT));
    }

    private static String nest(String open, String close, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            b.append(open);
        }
        b.append("x");
        for (int i = 0; i < n; i++) {
            b.append(close);
        }
        return b.toString();
    }

    @Test
    void deeplyNestedTablesFailAsIoErrors() throws IOException {
        String odt = "<office:document-content xmlns:office=\"o\" xmlns:table=\"t\" "
                + "xmlns:text=\"x\"><office:body><office:text>"
                + nest("<table:table><table:table-row><table:table-cell>",
                        "</table:table-cell></table:table-row></table:table>", 5000)
                + "</office:text></office:body></office:document-content>";
        byte[] o = OdtFormatTest.zip(new String[] {"content.xml"}, new String[] {odt});
        assertThrows(IOException.class, () -> read(o, Document.Format.ODT));
        String docx = "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/"
                + "2006/main\"><w:body>" + nest("<w:tbl><w:tr><w:tc>", "</w:tc></w:tr></w:tbl>", 3000)
                + "</w:body></w:document>";
        byte[] x = DocxFormatTest.zip(new String[] {"word/document.xml"}, new String[] {docx});
        assertThrows(IOException.class, () -> read(x, Document.Format.DOCX));
    }

    @Test
    void deeplyNestedHtmlNeverThrowsAnError() throws IOException {
        String html = "<html><body>" + nest("<table><tr><td>", "</td></tr></table>", 600)
                + nest("<div><ul><li>", "</li></ul></div>", 1000) + "</body></html>";
        // core's HTMLParser recurses once per open tag, so a deep enough page overflows inside
        // it; the reader must answer that with an IOException, never let the Error escape
        try {
            Document d = read(html.getBytes("UTF-8"), Document.Format.HTML);
            assertTrue(d.size() >= 0);
        } catch (IOException expected) {
            assertTrue(expected.getMessage().indexOf("nested too deeply") >= 0);
        }
    }

    @Test
    void rtfListItemsGainNoSpaces() throws IOException {
        Document d = pages();
        Document once = read(write(d, Document.Format.RTF), Document.Format.RTF);
        Document twice = read(write(once, Document.Format.RTF), Document.Format.RTF);
        assertEquals(once.plainText(), twice.plainText());
        assertFalse(once.plainText().contains(" First bullet"));
    }

    @Test
    void rtfKeepsIndentsAndTabStops() throws IOException {
        List<TabStop> tabs = new ArrayList<TabStop>();
        tabs.add(new TabStop(144, TabStop.RIGHT, '.'));
        tabs.add(new TabStop(216, TabStop.LEFT, ' '));
        List<Block> b = new ArrayList<Block>();
        b.add(new Paragraph("p", RichText.of("a\tb")).withIndents(36, -18, 24).withTabs(tabs));
        Document back = read(write(new Document(b, null, "t", 10), Document.Format.RTF),
                Document.Format.RTF);
        Paragraph p = (Paragraph) back.get(0);
        assertEquals(36, p.indent, 0.01);
        assertEquals(-18, p.firstLine, 0.01);
        assertEquals(24, p.rightIndent, 0.01);
        assertEquals(tabs, p.tabs);
    }
}
