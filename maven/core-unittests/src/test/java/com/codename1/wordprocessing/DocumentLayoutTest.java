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
import com.codename1.pdf.PdfReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Pagination, line breaking, tables, notes, fields, hit testing and the PDF export.
class DocumentLayoutTest extends UITestBase {
    /// Every character is half the font size wide; ascent 0.8 and descent 0.2 of the size.
    static final Measurer FIXED = new Measurer() {
        @Override
        public float width(String text, TextStyle style) {
            return text.length() * style.size * 0.5f;
        }

        @Override
        public float ascent(TextStyle style) {
            return style.size * 0.8f;
        }

        @Override
        public float descent(TextStyle style) {
            return style.size * 0.2f;
        }
    };

    /// 100pt wide body, 100pt tall: ten 12pt lines of 16 characters (6pt each) per page.
    static final PageSpec SMALL = new PageSpec(120, 120, 10, 10, 10, 10);

    static List<Block> list(Block... b) {
        return new ArrayList<Block>(Arrays.asList(b));
    }

    static Paragraph para(String id, String text) {
        return new Paragraph(id, "Normal", Paragraph.LEFT, Paragraph.NO_LIST, 0, 0, 0, 0, 0, 1,
                false, false, RichText.of(text));
    }

    static DocumentLayout layout(Block... blocks) {
        return new DocumentLayout(new Document(list(blocks), SMALL, null, 1000), FIXED);
    }

    static List<LayoutItem> texts(LayoutPage p) {
        List<LayoutItem> out = new ArrayList<LayoutItem>();
        for (LayoutItem i : p.getItems()) {
            if (i.kind == LayoutItem.TEXT && i.text.length() > 0) {
                out.add(i);
            }
        }
        return out;
    }

    static String pageText(LayoutPage p) {
        StringBuilder b = new StringBuilder();
        for (LayoutItem i : texts(p)) {
            b.append(i.text);
        }
        return b.toString();
    }

    @Test
    void wrapsAtSpacesAndBreaksLongWords() {
        DocumentLayout l = layout(para("p", "aaaa bbbb cccc dddd eeeeeeeeeeeeeeeeeeee"));
        List<LayoutItem> t = texts(l.getPage(0));
        // "aaaa bbbb cccc " is 15 chars = 90pt; "dddd" would make 114 > 100
        assertEquals(10, t.get(0).x, 0.01);
        assertEquals("dddd ", t.get(3).text);
        assertEquals(t.get(0).y + 12, t.get(3).y, 0.01);
        assertEquals("eeeeeeeeeeeeeeee", t.get(4).text);
        assertEquals("eeee", t.get(5).text);
        assertEquals(1, l.getPageCount());
    }

    @Test
    void alignmentAndJustification() {
        Paragraph centered = para("c", "ab").withAlign(Paragraph.CENTER);
        Paragraph right = para("r", "ab").withAlign(Paragraph.RIGHT);
        Paragraph justified = para("j", "aa bb cc dd ee ff gg").withAlign(Paragraph.JUSTIFY);
        DocumentLayout l = layout(centered, right, justified);
        List<LayoutItem> t = texts(l.getPage(0));
        assertEquals(10 + 44, t.get(0).x, 0.01);
        assertEquals(10 + 88, t.get(1).x, 0.01);
        // the first justified line ends on the right margin, the last one is ragged
        LayoutItem lastOfLine = null;
        for (LayoutItem i : t) {
            if ("j".equals(i.paragraphId) && Math.abs(i.y - t.get(2).y) < 0.01) {
                lastOfLine = i;
            }
        }
        assertEquals(110, lastOfLine.x + 12, 0.01);
    }

    @Test
    void paginatesAndHonorsPageBreaks() {
        List<Block> blocks = new ArrayList<Block>();
        for (int i = 0; i < 25; i++) {
            blocks.add(para("p" + i, "line " + i));
        }
        blocks.add(para("brk", "fresh").withPageBreak(true));
        DocumentLayout l = new DocumentLayout(new Document(blocks, SMALL, null, 1000), FIXED);
        assertEquals(5, l.getPageCount());
        assertTrue(pageText(l.getPage(0)).startsWith("line 0"));
        assertTrue(pageText(l.getPage(1)).startsWith("line 8"), pageText(l.getPage(1)));
        assertEquals("line 24", pageText(l.getPage(3)));
        assertEquals("fresh", pageText(l.getPage(4)));
    }

    @Test
    void keepWithNextMovesTheHeading() {
        List<Block> blocks = new ArrayList<Block>();
        for (int i = 0; i < 7; i++) {
            blocks.add(para("p" + i, "x"));
        }
        blocks.add(para("h", "Heading").withKeepNext(true));
        blocks.add(para("b", "body"));
        DocumentLayout l = new DocumentLayout(new Document(blocks, SMALL, null, 1000), FIXED);
        assertEquals("Headingbody", pageText(l.getPage(1)));
    }

    @Test
    void listMarkersNumberAndRestart() {
        Paragraph a = para("a", "one").withList(Paragraph.NUMBERED, 0);
        Paragraph b = para("b", "two").withList(Paragraph.NUMBERED, 0);
        Paragraph c = para("c", "sub").withList(Paragraph.NUMBERED, 1);
        Paragraph d = para("d", "again").withList(Paragraph.NUMBERED, 0).withListStart(7);
        Paragraph e = para("e", "dot").withList(Paragraph.BULLET, 0);
        DocumentLayout l = layout(a, b, c, d, e);
        List<String> markers = new ArrayList<String>();
        for (LayoutItem i : l.getPage(0).getItems()) {
            if (i.kind == LayoutItem.TEXT && i.paragraphId == null) {
                markers.add(i.text);
            }
        }
        assertEquals(Arrays.asList("1.", "2.", "a.", "7.", "\u2022"), markers);
        assertEquals(10 + 18, texts(l.getPage(0)).get(1).x, 0.01);
    }

    static Table table(int rows, int cols, int headers) {
        List<List<List<Block>>> cells = new ArrayList<List<List<Block>>>();
        for (int r = 0; r < rows; r++) {
            List<List<Block>> row = new ArrayList<List<Block>>();
            for (int c = 0; c < cols; c++) {
                row.add(list(para("t" + r + "_" + c, "r" + r + "c" + c)));
            }
            cells.add(row);
        }
        return new Table("t", cells, new ArrayList<Table.Merge>(), headers);
    }

    @Test
    void tableRowsMoveWholeAndRepeatHeaders() {
        // each row is 12 + 8 padding = 20pt: five rows a page
        DocumentLayout l = layout(table(8, 2, 1));
        assertEquals(2, l.getPageCount());
        String second = pageText(l.getPage(1));
        assertTrue(second.startsWith("r0c0"), second);
        assertTrue(second.contains("r5c0"), second);
        int fills = 0;
        for (LayoutItem i : l.getPage(1).getItems()) {
            fills += i.kind == LayoutItem.FILL ? 1 : 0;
        }
        assertEquals(2, fills, "the repeated header row is shaded");
    }

    @Test
    void verticalMergeGrowsTheRow() {
        List<List<List<Block>>> cells = new ArrayList<List<List<Block>>>();
        cells.add(list2(list(para("a", "a"), para("b", "b"), para("c", "c"), para("d", "d")),
                list(para("x", "x"))));
        cells.add(list2(list(para("y0", "")), list(para("y", "y"))));
        List<Table.Merge> m = new ArrayList<Table.Merge>();
        m.add(new Table.Merge(0, 0, 2, 1));
        DocumentLayout l = layout(new Table("t", cells, m, 0));
        float yRow2 = 0;
        for (LayoutItem i : texts(l.getPage(0))) {
            if ("y".equals(i.paragraphId)) {
                yRow2 = i.y;
            }
        }
        // four 12pt lines plus padding is 56pt, shared by two rows: the second is 36pt high
        assertEquals(10 + 20 + 4, yRow2, 0.01);
    }

    @SafeVarargs
    static <T> List<T> list2(T... t) {
        return new ArrayList<T>(Arrays.asList(t));
    }

    @Test
    void aRowTallerThanAPageIsSplit() {
        List<Block> lines = new ArrayList<Block>();
        for (int i = 0; i < 20; i++) {
            lines.add(para("l" + i, "L" + i));
        }
        List<List<List<Block>>> cells = new ArrayList<List<List<Block>>>();
        cells.add(list2(lines));
        DocumentLayout l = layout(new Table("t", cells, new ArrayList<Table.Merge>(), 0));
        assertTrue(l.getPageCount() >= 3);
        StringBuilder all = new StringBuilder();
        for (int p = 0; p < l.getPageCount(); p++) {
            for (LayoutItem i : texts(l.getPage(p))) {
                assertTrue(i.y + i.height <= 110.01, "inside the page: " + i.text);
                all.append(i.text).append(',');
            }
        }
        assertTrue(all.toString().contains("L0,L1,") && all.toString().contains("L19,"));
    }

    @Test
    void footnotesSitAtTheBottomOfTheirPage() {
        Note n = new Note("n1", Note.FOOTNOTE, null, list(para("q", "note")));
        RichText t = new RichText.Builder().append("text", TextStyle.DEFAULT)
                .append(n, TextStyle.DEFAULT).build();
        DocumentLayout l = layout(new Paragraph("p", t));
        float noteY = -1;
        String mark = null;
        for (LayoutItem i : texts(l.getPage(0))) {
            if ("q".equals(i.paragraphId)) {
                noteY = i.y;
            }
            if ("p".equals(i.paragraphId) && TextStyle.SUPERSCRIPT.equals(i.style.script)) {
                mark = i.text;
            }
        }
        assertEquals("1", mark);
        assertTrue(noteY > 80, "near the bottom: " + noteY);
        assertTrue(pageText(l.getPage(0)).contains("1 note"));
    }

    @Test
    void endnotesFollowTheText() {
        Note n = new Note("n1", Note.ENDNOTE, "*", list(para("q", "end")));
        RichText t = new RichText.Builder().append("a", TextStyle.DEFAULT)
                .append(n, TextStyle.DEFAULT).build();
        DocumentLayout l = layout(new Paragraph("p", t), para("z", "last"));
        String s = pageText(l.getPage(0));
        assertTrue(s.indexOf("last") < s.indexOf("end"), s);
    }

    @Test
    void pageFieldsShowRealNumbers() {
        List<Block> blocks = new ArrayList<Block>();
        for (int p = 0; p < 3; p++) {
            RichText t = new RichText.Builder().append("p", TextStyle.DEFAULT)
                    .append(new Field("PAGE", "1", false), TextStyle.DEFAULT)
                    .append("/", TextStyle.DEFAULT)
                    .append(new Field("NUMPAGES", "1", false), TextStyle.DEFAULT).build();
            blocks.add(new Paragraph("f" + p, t).withPageBreak(p > 0));
        }
        DocumentLayout l = new DocumentLayout(new Document(blocks, SMALL, null, 1000), FIXED);
        assertEquals("p1/3", pageText(l.getPage(0)));
        assertEquals("p3/3", pageText(l.getPage(2)));
    }

    @Test
    void picturesAndComponentsReserveTheirBoxes() {
        Picture big = new Picture("i", DocumentInlineTest.PNG, "image/png", 400, 200, "");
        EmbeddedComponent c = new EmbeddedComponent("button", "{}", 30, 20, null);
        RichText t = new RichText.Builder().append("x", TextStyle.DEFAULT)
                .append(c, TextStyle.DEFAULT).build();
        DocumentLayout l = layout(big, new Paragraph("p", t));
        LayoutItem pic = null;
        LayoutItem comp = null;
        for (LayoutItem i : l.getPage(0).getItems()) {
            if (i.kind == LayoutItem.PICTURE) {
                pic = i;
            } else if (i.kind == LayoutItem.COMPONENT) {
                comp = i;
            }
        }
        assertEquals(100, pic.width, 0.01);
        assertEquals(50, pic.height, 0.01);
        assertEquals(30, comp.width, 0.01);
        assertEquals(16, comp.x, 0.01);
        assertEquals(comp.baseline, comp.y + comp.height, 0.01);
    }

    @Test
    void hitTestingRoundTrips() {
        DocumentLayout l = layout(para("p", "hello world"), para("q", "second"));
        TextPosition pos = l.positionAt(0, 10 + 6 * 3 + 1, 15);
        assertEquals(new TextPosition("p", 3), pos);
        float[] r = l.caretRect(new TextPosition("p", 7));
        assertEquals(0, r[0], 0.01);
        assertEquals(10 + 42, r[1], 0.01);
        assertEquals(new TextPosition("q", 6), l.positionAt(0, 200, 24));
        assertEquals(new TextPosition("q", 6), l.positionAt(0, 200, 300));
        assertNotNull(l.caretRect(new TextPosition("q", 6)));
        assertNull(l.caretRect(new TextPosition("missing", 0)));
        assertEquals(new TextPosition("p", 0), l.positionAt(0, 0, 0));
    }

    @Test
    void emptyDocumentHasOnePage() {
        DocumentLayout l = new DocumentLayout(new Document(), FIXED);
        assertEquals(1, l.getPageCount());
        assertEquals(PageSpec.A4, l.getPageSpec());
    }

    @Test
    void exportsAReadablePdf() throws IOException {
        Document d = DocumentInlineTest.rich();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        d.write(out, Document.Format.PDF);
        PdfReader r = PdfReader.open(out.toByteArray());
        assertTrue(r.getPageCount() >= 1);
        String text = r.extractText();
        String first = d.plainText().trim().split(" ")[0];
        assertTrue(text.contains(first), text);
        assertThrows(IOException.class, () -> Document.read(
                new java.io.ByteArrayInputStream(out.toByteArray()), Document.Format.PDF));
    }

    @Test
    void pdfMeasurerMapsFamilies() {
        PdfMeasurer m = new PdfMeasurer();
        TextStyle s = TextStyle.DEFAULT;
        assertEquals(com.codename1.pdf.PdfFont.TIMES_ROMAN, m.font(s));
        assertEquals(com.codename1.pdf.PdfFont.HELVETICA_BOLD,
                m.font(s.withFont("SansSerif").withBold(true)));
        assertEquals(com.codename1.pdf.PdfFont.COURIER, m.font(s.withFont("Monospace")));
        assertEquals(com.codename1.pdf.PdfFont.HELVETICA, m.font(s.withFont("Calibri")));
        m.register("Calibri", false, false, com.codename1.pdf.PdfFont.COURIER);
        assertEquals(com.codename1.pdf.PdfFont.COURIER, m.font(s.withFont("Calibri").withBold(true)));
        assertTrue(m.width("abc", s) > 0);
    }
}
