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
import com.codename1.ui.Image;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tracked changes and review comments: recording, accepting, rejecting, and the view.
class ReviewTest extends UITestBase {
    static DocumentModel model(String... texts) {
        List<Block> b = new ArrayList<Block>();
        for (int i = 0; i < texts.length; i++) {
            b.add(DocumentLayoutTest.para("p" + i, texts[i]));
        }
        return new DocumentModel(new Document(b, null, null, 100));
    }

    static String text(DocumentModel m, String id) {
        return m.findParagraph(id).text.plainText();
    }

    @Test
    void trackedEditsAreKeptUntilAccepted() {
        DocumentModel m = model("Hello world");
        m.setAuthor("Ada");
        m.setTrackChanges(true);
        assertTrue(m.getDocument().trackChanges);
        m.insertText(new TextPosition("p0", 5), " big", null);
        m.delete(new TextPosition("p0", 0), new TextPosition("p0", 1));
        assertEquals("Hello big world", text(m, "p0"));
        assertTrue(m.findParagraph("p0").text.styleAt(0).revision.isDeletion());
        assertEquals("Ada", m.findParagraph("p0").text.styleAt(6).revision.author);
        assertEquals(2, m.getRevisions().size());
        // deleting text inserted under tracking removes it outright
        m.delete(new TextPosition("p0", 8), new TextPosition("p0", 9));
        assertEquals("Hello bi world", text(m, "p0"));
        assertEquals(4, m.acceptRevisions(null));
        assertEquals("ello bi world", text(m, "p0"));
        assertTrue(m.getRevisions().isEmpty());
        m.undo();
        assertEquals("Hello bi world", text(m, "p0"));
        assertEquals(4, m.rejectRevisions(null));
        assertEquals("Hello world", text(m, "p0"));
        assertNull(m.findParagraph("p0").text.styleAt(0).revision);
    }

    @Test
    void acceptOneRevisionAndTrackAcrossParagraphs() {
        DocumentModel m = model("abc", "def");
        m.setTrackChanges(true);
        m.insertText(new TextPosition("p1", 3), "!", null);
        m.delete(new TextPosition("p0", 2), new TextPosition("p1", 1));
        assertEquals("abc", text(m, "p0"));
        assertEquals("def!", text(m, "p1"));
        List<Revision> revs = m.getRevisions();
        assertEquals(2, revs.size());
        Revision del = revs.get(0).isDeletion() ? revs.get(0) : revs.get(1);
        assertEquals(2, m.acceptRevisions(del.id));
        assertEquals("ab", text(m, "p0"));
        assertEquals("ef!", text(m, "p1"));
        assertEquals(1, m.getRevisions().size());
    }

    @Test
    void trackedChangesSurviveDocx() throws Exception {
        DocumentModel m = model("Hello");
        m.setAuthor("Ada");
        m.setTrackChanges(true);
        m.insertText(new TextPosition("p0", 5), "!", null);
        m.delete(new TextPosition("p0", 0), new TextPosition("p0", 1));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.getDocument().write(out, Document.Format.DOCX);
        Document back = Document.read(new ByteArrayInputStream(out.toByteArray()),
                Document.Format.DOCX);
        RichText t = ((Paragraph) back.get(0)).text;
        assertEquals("Hello!", t.plainText());
        assertTrue(t.styleAt(0).revision.isDeletion());
        assertFalse(t.styleAt(5).revision.isDeletion());
        assertEquals("Ada", t.styleAt(5).revision.author);
    }

    @Test
    void commentsAddResolveRemove() {
        DocumentModel m = model("one two", "three");
        m.setAuthor("Bo");
        Comment c = m.addComment(new TextPosition("p1", 2), new TextPosition("p0", 4), "why?");
        assertEquals("p0", c.startId);
        assertEquals(4, c.start);
        assertEquals("Bo", c.author);
        assertEquals(1, m.commentsAt(new TextPosition("p0", 5)).size());
        assertEquals(1, m.commentsAt(new TextPosition("p1", 0)).size());
        assertTrue(m.commentsAt(new TextPosition("p0", 1)).isEmpty());
        assertTrue(m.commentsAt(new TextPosition("p1", 4)).isEmpty());
        assertTrue(m.setCommentResolved(c.id, true));
        assertTrue(m.getDocument().comments.get(0).resolved);
        assertTrue(m.removeComment(c.id));
        assertTrue(m.getDocument().comments.isEmpty());
        m.undo();
        assertEquals(1, m.getDocument().comments.size());
        assertFalse(m.removeComment("nope"));
    }

    @Test
    void viewPaintsReviewAndCommentsSelection() {
        DocumentModel m = model("Hello world");
        DocumentView v = new DocumentView(m);
        Form f = new Form();
        f.add(v);
        f.show();
        flushSerialCalls();
        v.setSelection(new TextPosition("p0", 0), new TextPosition("p0", 5));
        Comment c = v.commentSelection("note");
        assertNotNull(c);
        v.setCaret(new TextPosition("p0", 2));
        assertEquals(Arrays.asList(c), v.getCommentsAtCaret());
        m.setTrackChanges(true);
        v.setCaret(new TextPosition("p0", 0));
        v.deleteForward();
        assertEquals(new TextPosition("p0", 1), v.getCaret());
        assertEquals("Hello world", text(m, "p0"));
        v.setWidth(400);
        v.setHeight(300);
        Image img = Image.createImage(400, 300);
        v.paintComponent(img.getGraphics());
    }
}
