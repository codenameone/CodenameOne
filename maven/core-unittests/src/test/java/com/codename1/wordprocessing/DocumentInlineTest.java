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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Inline objects, revisions and comments: the model and the formats that carry them.
class DocumentInlineTest extends UITestBase {
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13,
        'I', 'H', 'D', 'R', 0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0};

    static Document rich() {
        List<Block> noteBody = new ArrayList<Block>();
        noteBody.add(new Paragraph("p1", RichText.of("See the appendix.")));
        Revision ins = new Revision("r1", Revision.INSERT, "Ada", "2026-01-02T03:04:05Z");
        Revision del = new Revision("r2", Revision.DELETE, "Ada", "2026-01-02T03:04:05Z");
        RichText text = new RichText.Builder().append("Page ", TextStyle.DEFAULT)
                .append(new Field("PAGE", "3", false), TextStyle.DEFAULT)
                .append(" of the report", TextStyle.DEFAULT)
                .append(new Note("n1", Note.FOOTNOTE, null, noteBody),
                        TextStyle.DEFAULT.withScript(TextStyle.SUPERSCRIPT))
                .append(" new", TextStyle.DEFAULT.withRevision(ins))
                .append(" old", TextStyle.DEFAULT.withRevision(del))
                .append(new Picture("i7", PNG, "image/png", 12, 10, "logo"), TextStyle.DEFAULT)
                .append(new EmbeddedComponent("chart", "{\"v\":[1,2]}", 100, 50, PNG),
                        TextStyle.DEFAULT)
                .build();
        List<Block> blocks = new ArrayList<Block>();
        blocks.add(new Paragraph("p2", text));
        blocks.add(new Paragraph("p3", RichText.EMPTY));
        List<Comment> comments = new ArrayList<Comment>();
        comments.add(new Comment("c4", "Bob", "2026-01-03", "Check this\nplease", false, "p2", 0,
                "p2", 4));
        return new Document(blocks, null, "Rich", 10, comments, true);
    }

    @Test
    void objectsTakeOneCharacterAndNeverMerge() {
        Paragraph p = (Paragraph) rich().get(0);
        RichText t = p.text;
        assertEquals(RichText.OBJECT, t.plainText().charAt(5));
        assertTrue(t.objectAt(5) instanceof Field);
        assertEquals("Page 3 of the report new old", t.displayText().substring(0, 28));
        RichText cut = t.delete(5, 6);
        assertFalse(cut.objectAt(5) instanceof Field);
        assertEquals("Page  of", cut.plainText().substring(0, 8));
        RichText swapped = t.withObject(5, new Field("PAGE", "4", false));
        assertEquals("4", ((Field) swapped.objectAt(5)).result);
        assertEquals(' ', new RichText.Builder().append("a" + RichText.OBJECT, null).build()
                .plainText().charAt(1));
    }

    @Test
    void plainTextSkipsDeletionsAndShowsFieldResults() {
        String plain = rich().plainText();
        assertTrue(plain.startsWith("Page 3 of the report new"), plain);
        assertFalse(plain.contains("old"), plain);
    }

    @Test
    void nativeRoundTripKeepsEverything() throws IOException {
        Document d = rich();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        d.write(out, Document.Format.NATIVE);
        Document back = Document.read(new ByteArrayInputStream(out.toByteArray()),
                Document.Format.NATIVE);
        assertEquals(d.blocks, back.blocks);
        assertEquals(d.comments, back.comments);
        assertTrue(back.trackChanges);
        assertEquals(d, back);
    }

    @Test
    void htmlAndMarkdownRenderObjects() {
        Document d = rich();
        String html = d.toString(Document.Format.HTML);
        assertTrue(html.contains("<del title=\"Ada\"> old</del>"), html);
        assertTrue(html.contains("href=\"#note-1\""), html);
        assertTrue(html.contains("See the appendix."), html);
        assertFalse(html.indexOf(RichText.OBJECT) >= 0);
        assertTrue(html.contains("<p><br></p>"), html);
        Document back = HtmlFormat.read(html);
        boolean blank = false;
        for (Block b : back.blocks) {
            blank |= b instanceof Paragraph && ((Paragraph) b).text.isEmpty();
        }
        assertTrue(blank, "the blank paragraph survives HTML");
        String md = d.toString(Document.Format.MARKDOWN);
        assertTrue(md.contains("[^1]: See the appendix."), md);
        assertFalse(md.contains("old"), md);
        assertFalse(md.indexOf(RichText.OBJECT) >= 0);
    }

    @Test
    void modelKeepsComments() {
        DocumentModel m = new DocumentModel(rich());
        assertSame(m.getDocument().comment("c4"), m.getDocument().comments.get(0));
        List<Comment> none = new ArrayList<Comment>();
        m.setDocument(m.getDocument().withComments(none));
        assertTrue(m.getDocument().comments.isEmpty());
        m.undo();
        assertEquals(1, m.getDocument().comments.size());
    }
}
