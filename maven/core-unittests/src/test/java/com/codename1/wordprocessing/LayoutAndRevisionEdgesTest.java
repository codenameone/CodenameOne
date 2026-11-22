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
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Edge cases found by review: accepting a deletion that spans paragraphs, and text or
/// spacing taller than a page.
class LayoutAndRevisionEdgesTest {
    private static final Measurer FIXED = new Measurer() {
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

    private static DocumentModel threeParagraphs() {
        List<Block> b = new ArrayList<Block>();
        b.add(new Paragraph("p1", RichText.of("hello")));
        b.add(new Paragraph("p2", RichText.of("middle")));
        b.add(new Paragraph("p3", RichText.of("world")));
        return new DocumentModel(new Document(b, null, "t", 10));
    }

    private static String texts(Document d) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < d.size(); i++) {
            s.append('[').append(((Paragraph) d.get(i)).text.plainText()).append(']');
        }
        return s.toString();
    }

    @Test
    void acceptingADeletionAcrossParagraphsJoinsThem() {
        DocumentModel plain = threeParagraphs();
        plain.delete(new TextPosition("p1", 2), new TextPosition("p3", 2));
        DocumentModel tracked = threeParagraphs();
        tracked.setTrackChanges(true);
        tracked.delete(new TextPosition("p1", 2), new TextPosition("p3", 2));
        assertEquals("[hello][middle][world]", texts(tracked.getDocument()));
        tracked.acceptRevisions(null);
        assertEquals(texts(plain.getDocument()), texts(tracked.getDocument()));
        assertEquals("[herld]", texts(tracked.getDocument()));
    }

    @Test
    void rejectingADeletionAcrossParagraphsKeepsThem() {
        DocumentModel tracked = threeParagraphs();
        tracked.setTrackChanges(true);
        tracked.delete(new TextPosition("p1", 2), new TextPosition("p3", 2));
        tracked.rejectRevisions(null);
        assertEquals("[hello][middle][world]", texts(tracked.getDocument()));
    }

    @Test
    void separateDeletionsDoNotJoinParagraphs() {
        DocumentModel m = threeParagraphs();
        m.setTrackChanges(true);
        m.delete(new TextPosition("p1", 3), new TextPosition("p1", 5));
        m.delete(new TextPosition("p2", 0), new TextPosition("p2", 2));
        m.acceptRevisions(null);
        assertEquals("[hel][ddle][world]", texts(m.getDocument()));
    }

    @Test
    void aLineTallerThanThePageTakesOnePage() {
        List<Block> b = new ArrayList<Block>();
        b.add(new Paragraph("p", RichText.of("x", TextStyle.DEFAULT.withSize(500))));
        PageSpec tiny = new PageSpec(72, 72, 30, 30, 30, 30);
        DocumentLayout l = new DocumentLayout(new Document(b, tiny, "t", 10), FIXED);
        assertEquals(1, l.getPageCount());
    }

    @Test
    void hugeSpacingIsCappedAtAPage() {
        List<Block> b = new ArrayList<Block>();
        b.add(new Paragraph("p", "Normal", Paragraph.LEFT, Paragraph.NO_LIST, 0, 0, 5000, 5000, 0,
                1, false, false, RichText.of("x")));
        DocumentLayout l = new DocumentLayout(new Document(b, PageSpec.A4, "t", 10), FIXED);
        assertTrue(l.getPageCount() <= 3, "pages: " + l.getPageCount());
    }
}
