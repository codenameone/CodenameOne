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

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Reading and writing every format, and round trips through each.
class DocumentFormatTest extends UITestBase {
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13,
        'I', 'H', 'D', 'R', 0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0};

    private static Document sample() {
        TextStyle bold = TextStyle.DEFAULT.withBold(true);
        List<Block> b = new ArrayList<Block>();
        b.add(new Paragraph("p1", RichText.of("Report", TextStyle.DEFAULT.withBold(true)
                .withSize(24))).withStyle("Heading1"));
        b.add(new Paragraph("p2", new RichText.Builder().append("  Plain ", TextStyle.DEFAULT)
                .append("bold", bold).append(" and ", TextStyle.DEFAULT)
                .append("linked", TextStyle.DEFAULT.withLink("https://example.com/a b")
                        .withUnderline(true).withColor(0x1a5fb4))
                .append(" <&> text", TextStyle.DEFAULT.withItalic(true)).build())
                .withAlign(Paragraph.CENTER));
        b.add(new Paragraph("p3", RichText.of("One")).withList(Paragraph.BULLET, 0));
        b.add(new Paragraph("p4", RichText.of("Nested")).withList(Paragraph.NUMBERED, 1));
        Table t = Table.ofText("t5", new String[][] {{"Item", "Price"}, {"Desk", "12"},
            {"Lamp", "3"}});
        List<Table.Merge> merges = new ArrayList<Table.Merge>();
        merges.add(new Table.Merge(2, 0, 1, 2));
        b.add(t.withMerges(merges).withHeaderRows(1));
        b.add(new Picture("i6", PNG, "image/png", 30, 20, "dot"));
        b.add(new Paragraph("p7", RichText.of("x", TextStyle.DEFAULT.withScript(
                TextStyle.SUPERSCRIPT).withBackground(0xffff00).withStrike(true))));
        return new Document(b, PageSpec.LETTER, "Sample", 8);
    }

    private static byte[] write(Document d, Document.Format f) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        d.write(out, f);
        return out.toByteArray();
    }

    private static Document read(byte[] b, Document.Format f) throws IOException {
        return Document.read(new ByteArrayInputStream(b), f);
    }

    private static Document read(String s, Document.Format f) throws IOException {
        return read(s.getBytes("UTF-8"), f);
    }

    @Test
    void nativeRoundTripIsExact() throws IOException {
        Document d = sample();
        Document back = read(write(d, Document.Format.NATIVE), Document.Format.NATIVE);
        assertEquals(d, back);
        assertEquals(d.nextId, back.nextId);
        assertArrayEquals(PNG, ((Picture) back.get(5)).data());
    }

    @Test
    void nativeReadsWordProcessorExtras() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><document version=\"1\" "
                + "title=\"T\" next=\"9\" trackChanges=\"false\"><properties protection=\"none\"/>"
                + "<page width=\"612\" height=\"792\" top=\"72\" right=\"72\" bottom=\"72\" "
                + "left=\"72\"/><noteOptions/><sections start=\"nextPage\" pageStart=\"0\"/>"
                + "<styles><style id=\"Normal\" name=\"Normal\"><p id=\"s\"/></style></styles>"
                + "<p id=\"p1\" style=\"Normal\" align=\"left\" list=\"\" listLevel=\"0\" "
                + "rtl=\"true\"><previousFormat><p id=\"old\"/></previousFormat>"
                + "<r font=\"Serif\" size=\"12\" color=\"202833\" background=\"ffffff\" "
                + "bold=\"false\" revision-id=\"r1\"> a </r><field instruction=\"PAGE\" "
                + "result=\"3\"><r font=\"Serif\" size=\"12\"/></field></p>"
                + "<comment id=\"c1\"><text>hi</text></comment></document>";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bytes);
        zip.putNextEntry(new ZipEntry("document.xml"));
        zip.write(xml.getBytes("UTF-8"));
        zip.closeEntry();
        zip.finish();
        Document d = read(bytes.toByteArray(), Document.Format.NATIVE);
        assertEquals(1, d.size());
        assertEquals(" a 3", ((Paragraph) d.get(0)).text.displayText());
        assertEquals(TextStyle.NO_BACKGROUND, ((Paragraph) d.get(0)).text.runStyle(0).background);
        assertEquals(PageSpec.LETTER, d.page);
        assertEquals("T", d.title);
    }

    @Test
    void nativeRejectsDamage() {
        assertThrows(IOException.class, () -> read(new byte[] {1, 2, 3}, Document.Format.NATIVE));
    }

    @Test
    void htmlRoundTripKeepsStructureAndStyles() throws IOException {
        Document d = sample();
        String html = d.toString(Document.Format.HTML);
        assertTrue(html.indexOf("&lt;&amp;&gt;") > 0, html);
        Document back = read(html, Document.Format.HTML);
        assertEquals("Sample", back.title);
        Paragraph h = (Paragraph) back.get(0);
        assertEquals("Heading1", h.style);
        Paragraph p = (Paragraph) back.get(1);
        assertEquals(Paragraph.CENTER, p.align);
        assertEquals(((Paragraph) d.get(1)).text.plainText().trim(), p.plainText().trim());
        assertTrue(hasStyle(p.text, "bold", TextStyle.DEFAULT.withBold(true)));
        assertEquals("https://example.com/a b", styleOf(p.text, "linked").link);
        assertEquals(Paragraph.BULLET, ((Paragraph) back.get(2)).list);
        assertEquals(Paragraph.NUMBERED, ((Paragraph) back.get(3)).list);
        assertEquals(1, ((Paragraph) back.get(3)).listLevel);
        Table t = (Table) back.get(4);
        assertEquals(3, t.rows());
        assertEquals(2, t.columns());
        assertEquals(1, t.headerRows);
        assertEquals(2, t.span(2, 0).columns);
        assertEquals("Lamp", t.cellText(2, 0));
        Picture pic = (Picture) back.get(5);
        assertArrayEquals(PNG, pic.data());
        assertEquals(30f, pic.width, 0.01f);
        assertEquals("dot", pic.description);
        TextStyle x = ((Paragraph) back.get(6)).text.runStyle(0);
        assertEquals(TextStyle.SUPERSCRIPT, x.script);
        assertEquals(0xffff00, x.background);
        assertTrue(x.strike);
    }

    @Test
    void htmlReadsForeignMarkup() throws IOException {
        Document d = read("<p style=\"color:#f00;font-size:16px\">A<strong>B</strong>"
                + "<span style=\"font-weight:700; background-color: rgb(0, 128, 0)\">C</span>"
                + "</p><TABLE><TR><TD ROWSPAN=2>x</TD><TD>y</TD></TR><TR><TD>z</TD></TR>"
                + "</TABLE><img src=\"http://example.com/a.png\" alt=\"remote\">",
                Document.Format.HTML);
        Paragraph p = (Paragraph) d.get(0);
        assertEquals(0xff0000, p.text.runStyle(0).color);
        assertEquals(12f, p.text.runStyle(0).size, 0.01f);
        assertTrue(styleOf(p.text, "B").bold);
        assertEquals(0x008000, styleOf(p.text, "C").background);
        Table t = (Table) d.get(1);
        assertEquals(2, t.span(0, 0).rows);
        assertEquals("z", t.cellText(1, 1));
        assertEquals("remote", ((Paragraph) d.get(2)).plainText());
    }

    @Test
    void markdownRoundTrip() throws IOException {
        Document d = sample();
        String md = d.toString(Document.Format.MARKDOWN);
        Document back = read(md, Document.Format.MARKDOWN);
        assertEquals("Heading1", ((Paragraph) back.get(0)).style);
        Paragraph p = (Paragraph) back.get(1);
        assertEquals("Plain bold and linked <&> text", p.plainText());
        assertTrue(styleOf(p.text, "bold").bold);
        assertEquals("https://example.com/a%20b", styleOf(p.text, "linked").link);
        assertTrue(styleOf(p.text, "<&> text").italic);
        assertEquals(Paragraph.NUMBERED, ((Paragraph) back.get(3)).list);
        assertEquals(1, ((Paragraph) back.get(3)).listLevel);
        Table t = (Table) back.get(4);
        assertEquals("Price", t.cellText(0, 1));
        assertEquals("12", t.cellText(1, 1));
        assertArrayEquals(PNG, ((Picture) back.get(5)).data());
        assertTrue(((Paragraph) back.get(6)).text.runStyle(0).strike);
    }

    @Test
    void markdownBlocks() throws IOException {
        Document d = read("# Title #\n\nline one\nline two  \nhard\n\n> quoted\n> more\n\n"
                + "```\ncode *raw*\n```\n\n3. three\n4. four\n   continued\n\n| a | b |\n"
                + "|:-:|--:|\n| 1 \\| 2 | x<br>y |\n\n\\# not a heading\n", Document.Format.MARKDOWN);
        assertEquals("Title", ((Paragraph) d.get(0)).plainText());
        assertEquals("line one line two\nhard", ((Paragraph) d.get(1)).plainText());
        assertEquals("Quote", ((Paragraph) d.get(2)).style);
        assertEquals("quoted more", ((Paragraph) d.get(2)).plainText());
        assertEquals("code *raw*", ((Paragraph) d.get(3)).plainText());
        assertEquals("Code", ((Paragraph) d.get(3)).style);
        Paragraph three = (Paragraph) d.get(4);
        assertEquals(3, three.listStart);
        assertEquals("four continued", ((Paragraph) d.get(5)).plainText());
        Table t = (Table) d.get(6);
        assertEquals("1 | 2", t.cellText(1, 0));
        assertEquals("x\ny", t.cellText(1, 1));
        assertEquals(Paragraph.CENTER, ((Paragraph) t.cell(1, 0).get(0)).align);
        assertEquals(Paragraph.RIGHT, ((Paragraph) t.cell(1, 1).get(0)).align);
        assertEquals("# not a heading", ((Paragraph) d.get(7)).plainText());
        assertEquals("Normal", ((Paragraph) d.get(7)).style);
    }

    @Test
    void textRoundTrip() throws IOException {
        Document d = read("one\r\ntwo\n\nfour", Document.Format.TEXT);
        assertEquals(4, d.size());
        assertEquals("", ((Paragraph) d.get(2)).plainText());
        assertEquals("one\ntwo\n\nfour\n", d.toString(Document.Format.TEXT));
        assertEquals("Item\tPrice\nDesk\t12\nLamp\t", plainTable());
    }

    private static String plainTable() {
        StringBuilder b = new StringBuilder();
        Document.appendPlainText(b, sample().get(4));
        return b.toString();
    }

    @Test
    void formatFromFileName() {
        assertEquals(Document.Format.NATIVE, Document.Format.forFileName("a/b.CN1DOC"));
        assertEquals(Document.Format.HTML, Document.Format.forFileName("x.htm"));
        assertEquals(Document.Format.MARKDOWN, Document.Format.forFileName("README.md"));
        assertNull(Document.Format.forFileName("x.docx"));
    }

    private static TextStyle styleOf(RichText t, String text) {
        for (int i = 0; i < t.runCount(); i++) {
            if (t.runText(i).equals(text)) {
                return t.runStyle(i);
            }
        }
        throw new AssertionError("No run '" + text + "' in " + runs(t));
    }

    private static boolean hasStyle(RichText t, String text, TextStyle s) {
        return styleOf(t, text).equals(s);
    }

    private static String runs(RichText t) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < t.runCount(); i++) {
            b.append('[').append(t.runText(i)).append(']');
        }
        return b.toString();
    }
}
