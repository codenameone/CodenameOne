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
import com.codename1.ui.ClipboardContent;
import com.codename1.ui.Display;
import com.codename1.ui.TextInputClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// `DocumentView`'s clipboard: what a copy puts on the clipboard, and that a paste keeps
/// formatting and paragraphs.
class DocumentClipboardTest extends UITestBase {
    private static DocumentView view(Block... blocks) {
        return new DocumentView(DocumentEditingTest.doc(blocks));
    }

    @Test
    void copyOffersEveryTextFormat() {
        RichText t = new RichText.Builder().append("plain ", TextStyle.DEFAULT)
                .append("bold", TextStyle.DEFAULT.withBold(true)).build();
        DocumentView v = view(new Paragraph("a", t));
        v.setSelection(new TextPosition("a", 0), new TextPosition("a", 10));
        v.copySelection();
        ClipboardContent c = Display.getInstance().getClipboardContent();
        assertNotNull(c);
        assertEquals("plain bold", c.getText(ClipboardContent.MIME_TEXT).trim());
        assertTrue(c.getText(ClipboardContent.MIME_HTML).contains("<b>bold</b>"));
        assertTrue(c.getText(ClipboardContent.MIME_RTF).startsWith("{\\rtf1"));
        assertTrue(c.getText(ClipboardContent.MIME_MARKDOWN).contains("**bold**"));
    }

    @Test
    void pasteKeepsFormattingAndSplitsParagraphs() {
        DocumentView v = view(DocumentEditingTest.para("a", "startend"));
        Display.getInstance().copyToClipboard(new ClipboardContent()
                .setData(ClipboardContent.MIME_MARKDOWN, "**one**\n\n# Two\n\nthree")
                .setData(ClipboardContent.MIME_TEXT, "one two three"));
        v.setCaret(new TextPosition("a", 5));
        v.pasteClipboard();
        DocumentModel m = v.getModel();
        assertEquals(3, m.size());
        Paragraph first = (Paragraph) m.getBlock(0);
        assertEquals("startone", first.plainText());
        assertTrue(first.text.styleAt(6).bold);
        assertTrue(((Paragraph) m.getBlock(1)).isHeading());
        assertEquals("threeend", ((Paragraph) m.getBlock(2)).plainText());
        m.undo();
        assertEquals("startend", ((Paragraph) m.getBlock(0)).plainText());
        assertEquals(1, m.size());
    }

    @Test
    void cutAndSelectAllThroughKeyCommands() {
        DocumentView v = view(DocumentEditingTest.para("a", "first"),
                DocumentEditingTest.para("b", "second"));
        v.onKeyCommand(TextInputClient.KEY_SELECT_ALL, 0);
        TextPosition[] sel = v.getSelection();
        assertEquals("a", sel[0].paragraphId);
        assertEquals(6, sel[1].offset);
        v.onKeyCommand(TextInputClient.KEY_CUT, 0);
        assertEquals("", v.getModel().getDocument().plainText().trim());
        assertTrue(Display.getInstance().getClipboardContent()
                .getText(ClipboardContent.MIME_TEXT).contains("second"));
        v.onKeyCommand(TextInputClient.KEY_PASTE, 0);
        assertTrue(v.getModel().getDocument().plainText().contains("first"));
        assertTrue(v.getModel().getDocument().plainText().contains("second"));
    }
}
