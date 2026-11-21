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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// RTF reading and writing.
class RtfFormatTest extends UITestBase {
    static byte[] rtf(Document d) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        d.write(out, Document.Format.RTF);
        return out.toByteArray();
    }

    static Document read(byte[] b) throws IOException {
        return Document.read(new ByteArrayInputStream(b), Document.Format.RTF);
    }

    static Document roundTrip(Document d) throws IOException {
        return read(rtf(d));
    }

    static List<Block> list(Block... b) {
        return new ArrayList<Block>(Arrays.asList(b));
    }

    @Test
    void paragraphFormattingSurvives() throws IOException {
        TextStyle bold = TextStyle.DEFAULT.withBold(true).withColor(0xff0000).withSize(14);
        RichText text = new RichText.Builder().append("Hello ", TextStyle.DEFAULT)
                .append("bold", bold).append(" and ", TextStyle.DEFAULT)
                .append("link", TextStyle.DEFAULT.withLink("https://example.com/a?b=1&c"))
                .append("\tx\ny", TextStyle.DEFAULT.withFont("Monospace").withItalic(true)
                        .withBackground(0x00ff00).withUnderline(true).withStrike(true))
                .append("2", TextStyle.DEFAULT.withScript(TextStyle.SUBSCRIPT)).build();
        Paragraph p = new Paragraph("p1", "Heading2", Paragraph.CENTER, Paragraph.NO_LIST, 0, 0,
                6, 12, 18, 1.5f, true, true, text);
        Paragraph custom = new Paragraph("p2", RichText.of("x")).withStyle("My Fancy Style");
        Document d = new Document(list(p, custom), PageSpec.LETTER, "Title & more", 3,
                new ArrayList<Comment>(), false);
        Document back = roundTrip(d);
        assertEquals("Title & more", back.title);
        assertEquals(PageSpec.LETTER, back.page);
        Paragraph q = (Paragraph) back.get(0);
        assertEquals("Heading2", q.style);
        assertEquals(Paragraph.CENTER, q.align);
        assertEquals(6, q.before, 0.01);
        assertEquals(12, q.after, 0.01);
        assertEquals(18, q.indent, 0.01);
        assertEquals(1.5f, q.lineSpacing, 0.001);
        assertTrue(q.pageBreak);
        assertTrue(q.keepNext);
        assertEquals(text.plainText(), q.text.plainText());
        assertEquals(text.styles(), q.text.styles());
        assertEquals("My Fancy Style", ((Paragraph) back.get(1)).style);
    }

    @Test
    void listsAndRestartsSurvive() throws IOException {
        Paragraph a = new Paragraph("p1", RichText.of("a")).withList(Paragraph.BULLET, 0);
        Paragraph b = new Paragraph("p2", RichText.of("b")).withList(Paragraph.BULLET, 1);
        Paragraph c = new Paragraph("p3", RichText.of("c")).withList(Paragraph.NUMBERED, 0);
        Paragraph e = new Paragraph("p4", RichText.of("d")).withList(Paragraph.NUMBERED, 0);
        Paragraph f = new Paragraph("p5", RichText.of("e")).withList(Paragraph.NUMBERED, 0)
                .withListStart(5);
        Paragraph g = new Paragraph("p6", RichText.of("f")).withList(Paragraph.NUMBERED, 0);
        Document back = roundTrip(new Document(list(a, b, c, e, f, g), null, null, 100));
        String[] kinds = {Paragraph.BULLET, Paragraph.BULLET, Paragraph.NUMBERED,
            Paragraph.NUMBERED, Paragraph.NUMBERED, Paragraph.NUMBERED};
        int[] starts = {0, 0, 0, 0, 5, 0};
        for (int i = 0; i < kinds.length; i++) {
            Paragraph p = (Paragraph) back.get(i);
            assertEquals(kinds[i], p.list, "paragraph " + i);
            assertEquals(starts[i], p.listStart, "paragraph " + i);
        }
        assertEquals(1, ((Paragraph) back.get(1)).listLevel);
    }

    @Test
    void tablesWithMergesSurvive() throws IOException {
        List<List<List<Block>>> cells = new ArrayList<List<List<Block>>>();
        for (int r = 0; r < 3; r++) {
            List<List<Block>> row = new ArrayList<List<Block>>();
            for (int c = 0; c < 3; c++) {
                row.add(list(new Paragraph("p" + r + c, RichText.of("r" + r + "c" + c))));
            }
            cells.add(row);
        }
        List<Table.Merge> merges = new ArrayList<Table.Merge>();
        merges.add(new Table.Merge(0, 0, 1, 2));
        merges.add(new Table.Merge(1, 2, 2, 1));
        Table t = new Table("t1", cells, merges, 1);
        Document back = roundTrip(new Document(list(t), null, null, 100));
        Table u = (Table) back.get(0);
        assertEquals(3, u.rows());
        assertEquals(3, u.columns());
        assertEquals(1, u.headerRows);
        assertEquals(t.merges, u.merges);
        assertEquals("r0c0", u.cellText(0, 0));
        assertEquals("r1c2", u.cellText(1, 2));
        assertEquals("r2c1", u.cellText(2, 1));
    }

    @Test
    void inlineObjectsRevisionsAndCommentsSurvive() throws IOException {
        Document d = DocumentInlineTest.rich();
        Document back = roundTrip(d);
        assertTrue(back.trackChanges);
        Paragraph p = (Paragraph) back.get(0);
        RichText t = p.text;
        Field field = (Field) t.objectAt(5);
        assertEquals("PAGE", field.instruction);
        assertEquals("3", field.result);
        Note note = null;
        int pictures = 0;
        boolean sawDeleted = false;
        for (int i = 0; i < t.length(); i++) {
            Inline o = t.objectAt(i);
            if (o instanceof Note) {
                note = (Note) o;
            } else if (o instanceof Picture) {
                pictures++;
            }
            Revision r = t.styleAt(i).revision;
            sawDeleted |= r != null && r.isDeletion() && "Ada".equals(r.author);
        }
        assertNotNull(note);
        assertEquals("See the appendix.", note.plainText());
        assertEquals(2, pictures, "the picture and the component's snapshot");
        assertTrue(sawDeleted);
        assertEquals(d.plainText().trim(), back.plainText().trim());
        assertEquals(1, back.comments.size());
        Comment c = back.comments.get(0);
        assertEquals("Bob", c.author);
        assertEquals("Check this\nplease", c.text);
        assertEquals(p.id, c.startId);
        assertEquals(0, c.start);
        assertEquals(4, c.end);
    }

    @Test
    void pictureBlockSurvives() throws IOException {
        Picture pic = new Picture("i1", DocumentInlineTest.PNG, "image/png", 30, 20, "alt");
        Document back = roundTrip(new Document(list(pic), null, null, 100));
        Picture q = (Picture) back.get(0);
        assertEquals(30, q.width, 0.01);
        assertEquals(20, q.height, 0.01);
        assertEquals(DocumentInlineTest.PNG.length, q.dataLength());
    }

    @Test
    void customNoteMarkAndLockedField() throws IOException {
        RichText text = new RichText.Builder().append("x", TextStyle.DEFAULT)
                .append(new Note("n1", Note.ENDNOTE, "*", list(new Paragraph("q",
                        RichText.of("end")))), TextStyle.DEFAULT)
                .append(new Field("DATE \\@ \"yyyy\"", "2026", true), TextStyle.DEFAULT)
                .build();
        Document back = roundTrip(new Document(list(new Paragraph("p1", text)), null, null, 100));
        RichText t = ((Paragraph) back.get(0)).text;
        Note n = (Note) t.objectAt(1);
        assertEquals(Note.ENDNOTE, n.kind);
        assertEquals("*", n.mark);
        Field f = (Field) t.objectAt(2);
        assertTrue(f.locked);
        assertEquals("DATE \\@ \"yyyy\"", f.instruction);
        assertEquals("x*2026", back.plainText().trim().replace(" ", ""));
    }

    @Test
    void readsAWordPadStyleFile() throws IOException {
        String src = "{\\rtf1\\ansi\\deff0{\\fonttbl{\\f0\\fswiss Arial;}{\\f1 Courier New;}}"
                + "{\\colortbl;\\red255\\green0\\blue0;}"
                + "{\\stylesheet{\\s1 heading 1;}}"
                + "{\\info{\\title My Doc}}\\paperw11906\\paperh16838"
                + "\\pard\\s1 Big {\\b Title}\\par"
                + "\\pard\\qc Caf\\'e9 {\\cf1 red} \\u8364? {\\field{\\*\\fldinst HYPERLINK "
                + "\"http://x.y\"}{\\fldrslt link}}\\par"
                + "\\pard{\\pntext\\'b7\\tab}{\\*\\pn\\pnlvlblt}item\\par"
                + "\\trowd\\cellx1000\\cellx2000\\pard\\intbl a\\cell b\\cell\\row"
                + "\\pard after}";
        Document d = read(src.getBytes("ISO-8859-1"));
        assertEquals("My Doc", d.title);
        assertEquals(595.3f, d.page.width, 0.1);
        Paragraph h = (Paragraph) d.get(0);
        assertEquals("Heading1", h.style);
        assertEquals("Big Title", h.text.plainText());
        assertTrue(h.text.styleAt(4).bold);
        assertEquals("SansSerif", h.text.styleAt(0).font);
        Paragraph p = (Paragraph) d.get(1);
        assertEquals(Paragraph.CENTER, p.align);
        assertEquals("Caf\u00e9 red \u20ac link", p.text.plainText());
        assertEquals(0xff0000, p.text.styleAt(5).color);
        assertEquals("http://x.y", p.text.styleAt(13).link);
        Paragraph item = (Paragraph) d.get(2);
        assertEquals(Paragraph.BULLET, item.list);
        assertEquals("item", item.text.plainText());
        Table t = (Table) d.get(3);
        assertEquals("b", t.cellText(0, 1));
        assertEquals("after", ((Paragraph) d.get(4)).text.plainText());
    }

    @Test
    void hostileFilesFailCleanly() throws IOException {
        assertThrows(IOException.class, () -> read("not rtf at all".getBytes("UTF-8")));
        byte[] good = rtf(DocumentInlineTest.rich());
        // a truncated file reads what it holds
        assertNotNull(read(Arrays.copyOf(good, good.length / 2)));
        StringBuilder deep = new StringBuilder("{\\rtf1 ");
        for (int i = 0; i < 100000; i++) {
            deep.append('{');
        }
        assertThrows(IOException.class, () -> read(deep.toString().getBytes("UTF-8")));
        // a runaway table is clamped
        StringBuilder wide = new StringBuilder("{\\rtf1 \\trowd");
        for (int i = 0; i < 500; i++) {
            wide.append("\\cellx").append(i * 10 + 10);
        }
        wide.append("\\pard\\intbl ");
        for (int i = 0; i < 500; i++) {
            wide.append("x\\cell ");
        }
        wide.append("\\row}");
        Table t = (Table) read(wide.toString().getBytes("UTF-8")).get(0);
        assertTrue(t.columns() <= 64);
        // hex escapes at the end, huge parameters and a bad picture do not throw
        assertNotNull(read("{\\rtf1 \\fs999999999999 a\\'".getBytes("UTF-8")));
        assertNotNull(read("{\\rtf1 {\\pict\\pngblip zz}}".getBytes("UTF-8")));
        assertNotNull(read("{\\rtf1 {\\*\\atrfstart 1}x}".getBytes("UTF-8")));
    }

    @Test
    void fieldResultOverParagraphsKeepsEveryLine() throws IOException {
        Document d = read(("{\\rtf1 {\\field{\\*\\fldinst TOC}{\\fldrslt one\\par two}}\\par}")
                .getBytes("UTF-8"));
        Field f = (Field) ((Paragraph) d.get(0)).text.objectAt(0);
        assertEquals("one\ntwo", f.result);
    }
}
