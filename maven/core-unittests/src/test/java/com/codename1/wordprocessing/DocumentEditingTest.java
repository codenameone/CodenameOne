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
import com.codename1.ui.Form;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Editing through `DocumentModel` and `DocumentView`: text edits, undo grouping, listeners,
/// edits inside table cells, and the view's lifecycle.
class DocumentEditingTest extends UITestBase {
    static Paragraph para(String id, String text) {
        return DocumentLayoutTest.para(id, text);
    }

    static Document doc(Block... b) {
        return new Document(new ArrayList<Block>(Arrays.asList(b)), null, null, 100);
    }

    static String text(DocumentModel m, String id) {
        return m.findParagraph(id).text.plainText();
    }

    @Test
    void typingGroupsIntoOneUndo() {
        DocumentModel m = new DocumentModel(doc(para("a", "")));
        TextPosition at = new TextPosition("a", 0);
        for (char c : "hello".toCharArray()) {
            at = m.insertText(at, String.valueOf(c), null);
        }
        at = m.insertText(at, " ", null);
        m.insertText(at, "x", null);
        assertEquals("hello x", text(m, "a"));
        m.undo();
        assertEquals("hello ", text(m, "a"));
        m.undo();
        assertEquals("hello", text(m, "a"));
        m.undo();
        assertEquals("", text(m, "a"));
        assertFalse(m.canUndo());
        m.redo();
        assertEquals("hello", text(m, "a"));
    }

    @Test
    void splitJoinAndDeleteAcrossParagraphs() {
        DocumentModel m = new DocumentModel(doc(para("a", "first"), para("b", "middle"),
                para("c", "last")));
        TextPosition p = m.splitParagraph(new TextPosition("a", 3));
        assertEquals("fir", text(m, "a"));
        assertEquals("st", text(m, p.paragraphId));
        assertEquals(101, m.getDocument().nextId);
        TextPosition j = m.joinWithPrevious(p.paragraphId);
        assertEquals(new TextPosition("a", 3), j);
        assertEquals("first", text(m, "a"));
        assertNull(m.joinWithPrevious("a"));
        TextPosition d = m.delete(new TextPosition("c", 2), new TextPosition("a", 2));
        assertEquals(new TextPosition("a", 2), d);
        assertEquals("fist", text(m, "a"));
        assertEquals(1, m.getDocument().size());
    }

    @Test
    void headingIsFollowedByNormal() {
        DocumentModel m = new DocumentModel(doc(para("h", "Title").withStyle("Heading1")));
        TextPosition p = m.splitParagraph(new TextPosition("h", 5));
        assertEquals("Normal", m.findParagraph(p.paragraphId).style);
    }

    @Test
    void editsReachTableCellsAndNotes() {
        Table t = DocumentLayoutTest.table(2, 2, 0);
        Note n = new Note("n", Note.FOOTNOTE, null,
                new ArrayList<Block>(Arrays.asList((Block) para("q", "note"))));
        RichText rt = new RichText.Builder().append("x", TextStyle.DEFAULT)
                .append(n, TextStyle.DEFAULT).build();
        DocumentModel m = new DocumentModel(doc(t, new Paragraph("p", rt)));
        m.insertText(new TextPosition("t1_1", 4), "!", null);
        assertEquals("r1c1!", text(m, "t1_1"));
        m.insertText(new TextPosition("q", 4), "s", null);
        assertEquals("notes", text(m, "q"));
        List<String> ids = new ArrayList<String>();
        DocumentModel.collectIds(m.getDocument().blocks, ids);
        assertEquals(Arrays.asList("t0_0", "t0_1", "t1_0", "t1_1", "p", "q"), ids);
    }

    @Test
    void restyleAndListeners() {
        DocumentModel m = new DocumentModel(doc(para("a", "bold me")));
        final int[] calls = new int[1];
        DocumentListener l = new DocumentListener() {
            @Override
            public void documentChanged(DocumentModel model, Document before, Document after) {
                calls[0]++;
            }
        };
        m.addDocumentListener(l);
        m.restyle(new TextPosition("a", 7), new TextPosition("a", 5), new RichText.StyleMapper() {
            @Override
            public TextStyle map(TextStyle s) {
                return s.withBold(true);
            }
        });
        assertTrue(m.findParagraph("a").text.styleAt(5).bold);
        assertFalse(m.findParagraph("a").text.styleAt(0).bold);
        assertEquals(1, calls[0]);
        m.removeDocumentListener(l);
        m.undo();
        assertEquals(1, calls[0]);
    }

    @Test
    void viewTypesThroughTheModel() {
        DocumentModel m = new DocumentModel(doc(para("a", "abc"), para("b", "def")));
        DocumentView v = new DocumentView(m);
        Form f = new Form();
        f.add(v);
        f.show();
        v.setCaret(new TextPosition("a", 3));
        v.typeText("X\nY");
        assertEquals("abcX", text(m, "a"));
        TextPosition c = v.getCaret();
        assertEquals("Y", text(m, c.paragraphId));
        v.deleteBackward();
        v.deleteBackward();
        assertEquals(new TextPosition("a", 4), v.getCaret());
        v.setSelection(new TextPosition("a", 1), new TextPosition("b", 1));
        v.typeText("-");
        assertEquals("a-ef", text(m, "a"));
        v.moveCaret(true, true);
        assertNotNull(v.getSelection());
        // the view follows outside edits while it is showing
        m.reset(doc(para("z", "fresh")));
        assertNull(v.getCaret());
        assertEquals(1, v.getLayout().getPageCount());
    }

    @Test
    void viewHitTestingAndReadOnly() {
        DocumentView v = new DocumentView(doc(para("a", "hello")));
        v.setWidth(800);
        v.setHeight(600);
        assertTrue(v.getScale() > 0);
        assertEquals(v.getLayout().getPageCount(), 1);
        v.setEditable(false);
        v.setCaret(new TextPosition("a", 0));
        v.typeText("x");
        assertEquals("hello", v.getDocument().blocks.get(0) instanceof Paragraph
                ? ((Paragraph) v.getDocument().blocks.get(0)).text.plainText() : null);
        v.setZoom(100);
        assertEquals(6f, v.getZoom(), 0.001);
    }
}
