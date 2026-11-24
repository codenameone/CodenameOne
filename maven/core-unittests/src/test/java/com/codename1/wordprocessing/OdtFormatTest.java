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

import com.codename1.io.zip.ZipArchiveOutputStream;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// ODT reading and writing.
class OdtFormatTest extends UITestBase {
    static byte[] odt(Document d) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        d.write(out, Document.Format.ODT);
        return out.toByteArray();
    }

    static Document read(byte[] b) throws IOException {
        return Document.read(new ByteArrayInputStream(b), Document.Format.ODT);
    }

    static Document roundTrip(Document d) throws IOException {
        return read(odt(d));
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
        assertEquals("alt", q.description);
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
    void readsAFileAnotherProgramWrote() throws IOException {
        // written by macOS textutil from a small HTML page
        Document d = Document.read(OdtFormatTest.class.getResourceAsStream(
                "/wordprocessing/textutil.odt"), Document.Format.ODT);
        String plain = d.plainText();
        assertTrue(plain.startsWith("Big Title"), plain);
        assertTrue(plain.contains("Plain bold and italic link."), plain);
        assertTrue(plain.contains("wide"), plain);
        boolean bold = false;
        for (TextStyle s : ((Paragraph) d.get(0)).text.styles()) {
            bold |= s.bold;
        }
        assertTrue(bold);
    }

    static byte[] zip(String[] names, String[] contents) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipArchiveOutputStream zip = new ZipArchiveOutputStream(out);
        for (int i = 0; i < names.length; i++) {
            zip.putNextEntry(new ZipEntry(names[i]));
            byte[] b = contents[i].getBytes("UTF-8");
            zip.write(b, 0, b.length);
            zip.closeEntry();
        }
        zip.finish();
        return out.toByteArray();
    }

    @Test
    void hostileFilesFailCleanly() throws IOException {
        assertThrows(IOException.class, () -> read("not a zip at all".getBytes("UTF-8")));
        byte[] good = odt(DocumentInlineTest.rich());
        assertThrows(IOException.class, () -> read(Arrays.copyOf(good, good.length / 2)));
        assertThrows(IOException.class, () -> read(zip(new String[] {"mimetype", "content.xml"},
                new String[] {"application/vnd.oasis.opendocument.spreadsheet", "<x/>"})));
        assertThrows(IOException.class, () -> read(zip(new String[] {"a.txt"},
                new String[] {"x"})));
        // repeated rows and columns and runaway spans are clamped
        String body = "<office:document-content xmlns:office=\"o\" xmlns:table=\"t\" "
                + "xmlns:text=\"x\"><office:body><office:text><table:table><table:table-row "
                + "table:number-rows-repeated=\"99999999\"><table:table-cell "
                + "table:number-columns-repeated=\"99999999\" table:number-columns-spanned="
                + "\"999999\"><text:p>a</text:p></table:table-cell></table:table-row>"
                + "</table:table></office:text></office:body></office:document-content>";
        Document d = read(zip(new String[] {"content.xml"}, new String[] {body}));
        Table t = (Table) d.get(0);
        assertTrue(t.columns() <= 1024 && t.rows() <= 100);
    }
}
