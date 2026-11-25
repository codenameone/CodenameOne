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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The immutable model and the editable model over it.
class DocumentModelTest {
    @Test
    void richTextMergesRunsAndSlices() {
        TextStyle bold = TextStyle.DEFAULT.withBold(true);
        RichText t = new RichText.Builder().append("Hel", TextStyle.DEFAULT)
                .append("lo", TextStyle.DEFAULT).append(" world", bold).build();
        assertEquals(2, t.runCount());
        assertEquals("Hello world", t.plainText());
        assertEquals(bold, t.styleAt(6));
        assertEquals(bold, t.styleAt(t.length()));
        assertEquals("lo w", t.substring(3, 7).plainText());
        assertEquals(2, t.substring(3, 7).runCount());
        assertEquals("Help world", t.delete(3, 5).insert(3, RichText.of("p")).plainText());
        RichText restyled = t.restyle(0, 5, new RichText.StyleMapper() {
            public TextStyle map(TextStyle s) {
                return s.withBold(true);
            }
        });
        assertEquals(1, restyled.runCount());
        assertEquals(RichText.of("Hello world", bold), restyled);
    }

    @Test
    void emptyDocumentHasOneParagraph() {
        Document d = new Document(new ArrayList<Block>(), null, null, 1);
        assertEquals(1, d.size());
        assertTrue(d.get(0) instanceof Paragraph);
        assertEquals(PageSpec.A4, d.page);
    }

    @Test
    void nextIdSkipsUsedIds() {
        List<Block> blocks = new ArrayList<Block>();
        blocks.add(new Paragraph("p41", RichText.of("x")));
        Document d = new Document(blocks, null, null, 3);
        assertEquals(42, d.nextId);
        assertEquals("t42", d.newId('t'));
    }

    @Test
    void headingLevels() {
        Paragraph p = new Paragraph("p1", RichText.of("x"));
        assertEquals(0, p.headingLevel());
        assertEquals(3, p.withStyle("Heading3").headingLevel());
        assertEquals(1, p.withStyle("Title").headingLevel());
        assertEquals(0, p.withStyle("Heading7").headingLevel());
    }

    @Test
    void tableRowAndColumnEditsMoveMerges() {
        Table t = Table.ofText("t1", new String[][] {{"a", "b", "c"}, {"d", "e", "f"}});
        List<Table.Merge> merges = new ArrayList<Table.Merge>();
        merges.add(new Table.Merge(0, 1, 2, 2));
        t = t.withMerges(merges);
        assertTrue(t.covered(1, 2));
        assertFalse(t.covered(0, 1));
        Table grown = t.insertRow(1);
        assertEquals(3, grown.rows());
        assertEquals(3, grown.span(0, 1).rows);
        Table shifted = t.insertColumn(0);
        assertEquals(2, shifted.span(0, 2).column);
        Table shrunk = t.deleteColumn(2);
        assertEquals(1, shrunk.span(0, 1).columns);
        Table gone = shrunk.deleteRow(1);
        assertTrue(gone.merges.isEmpty());
        assertEquals("a\tb\tc\nd\te\tf", plain(t.withMerges(null)));
        assertEquals("a\tb\t\nd\t\t", plain(t));
    }

    private static String plain(Block b) {
        StringBuilder sb = new StringBuilder();
        Document.appendPlainText(sb, b);
        return sb.toString();
    }

    @Test
    void overlappingMergesAreRejected() {
        final Table t = Table.ofText("t1", new String[][] {{"a", "b"}, {"c", "d"}});
        final List<Table.Merge> merges = new ArrayList<Table.Merge>();
        merges.add(new Table.Merge(0, 0, 2, 1));
        merges.add(new Table.Merge(1, 0, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> t.withMerges(merges));
    }

    @Test
    void modelUndoRedoAndListeners() {
        DocumentModel m = new DocumentModel();
        final int[] calls = new int[1];
        final Document[] last = new Document[1];
        m.addDocumentListener(new DocumentListener() {
            public void documentChanged(DocumentModel model, Document before, Document after) {
                calls[0]++;
                last[0] = after;
                assertNotEquals(before, after);
            }
        });
        Document start = m.getDocument();
        String id = m.newId('p');
        m.insertBlock(1, new Paragraph(id, RichText.of("Two")));
        assertEquals(1, calls[0]);
        assertSame(m.getDocument(), last[0]);
        assertEquals(2, m.size());
        assertTrue(m.undo());
        assertEquals(start.blocks, m.getDocument().blocks);
        assertTrue(m.canRedo());
        assertTrue(m.redo());
        assertEquals("Two", ((Paragraph) m.getBlock(1)).plainText());
        assertEquals(3, calls[0]);
        assertFalse(m.redo());
    }

    @Test
    void groupedEditsUndoTogether() {
        DocumentModel m = new DocumentModel();
        Paragraph p = (Paragraph) m.getBlock(0);
        m.beginGroup();
        for (int i = 0; i < 5; i++) {
            p = p.withText(p.text.append(RichText.of("x")));
            m.setBlock(0, p);
        }
        m.endGroup();
        assertEquals("xxxxx", ((Paragraph) m.getBlock(0)).plainText());
        assertTrue(m.undo());
        assertEquals("", ((Paragraph) m.getBlock(0)).plainText());
        assertFalse(m.canUndo());
    }

    @Test
    void newIdsNeverRepeatAcrossUndo() {
        DocumentModel m = new DocumentModel();
        String a = m.newId('p');
        m.insertBlock(1, new Paragraph(a, RichText.of("a")));
        m.undo();
        String b = m.newId('p');
        assertNotEquals(a, b);
    }
}
