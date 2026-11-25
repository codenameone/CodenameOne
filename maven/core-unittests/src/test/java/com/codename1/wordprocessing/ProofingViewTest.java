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
import com.codename1.spellcheck.GrammarChecker;
import com.codename1.spellcheck.HunspellDictionary;
import com.codename1.spellcheck.ProofIssue;
import com.codename1.spellcheck.Proofreader;
import com.codename1.spellcheck.SpellChecker;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Proofing in `DocumentView`: underlines, the issue under a position and applying a fix.
class ProofingViewTest extends UITestBase {
    static HunspellDictionary dict() throws Exception {
        return HunspellDictionary.fromBytes(
                ("SET UTF-8\nSFX S Y 1\nSFX S 0 s .\n").getBytes("UTF-8"),
                ("3\nhello/S\nworld/S\nsee\n").getBytes("UTF-8"));
    }

    @Test
    void viewUnderlinesAndFixes() throws Exception {
        Paragraph p = DocumentLayoutTest.para("a", "Helo world");
        DocumentModel m = new DocumentModel(new Document(
                new ArrayList<Block>(Arrays.asList((Block) p)), null, null, 10));
        DocumentView v = new DocumentView(m);
        Proofreader pr = new Proofreader(SpellChecker.create(dict()), GrammarChecker.english());
        v.setProofreader(pr);
        Form f = new Form();
        f.add(v);
        f.show();
        flushSerialCalls();
        Image img = Image.createImage(400, 300);
        v.setWidth(400);
        v.setHeight(300);
        v.paintComponent(img.getGraphics());
        ProofIssue i = v.proofIssueAt(new TextPosition("a", 2), 3);
        assertNotNull(i);
        assertEquals("Hello", i.suggestions.get(0));
        v.applySuggestion("a", i, i.suggestions.get(0));
        assertEquals("Hello world", m.findParagraph("a").text.plainText());
        assertNull(v.proofIssueAt(new TextPosition("a", 2), 3));
        m.undo();
        assertEquals("Helo world", m.findParagraph("a").text.plainText());
    }
}
