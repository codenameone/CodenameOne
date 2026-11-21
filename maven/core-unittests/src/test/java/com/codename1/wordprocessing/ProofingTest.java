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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Hunspell dictionaries, the spell and grammar checkers, and proofing in `DocumentView`.
class ProofingTest extends UITestBase {
    static final String AFF = "SET UTF-8\nTRY esianrtolcdugmphbyfvkwz\n"
            + "SFX S Y 1\nSFX S 0 s .\n"
            + "SFX D Y 2\nSFX D 0 ed [^e]\nSFX D 0 d e\n"
            + "PFX U Y 1\nPFX U 0 un .\n"
            + "REP 1\nREP f ph\nMAP 1\nMAP e\u00e9\nFORBIDDENWORD !\n";
    static final String DIC = "9\nhello/S\nworld/S\nwalk/DSU\nlike/DU\ncaf\u00e9\nParis\n"
            + "phone/S\nwrong/S\nwrongs/!\nsee\n";

    static HunspellDictionary dict() throws Exception {
        return HunspellDictionary.fromBytes(AFF.getBytes("UTF-8"), DIC.getBytes("UTF-8"));
    }

    @Test
    void checksRootsAffixesCaseAndForbiddenWords() throws Exception {
        HunspellDictionary d = dict();
        assertEquals(10, d.size());
        assertTrue(d.check("hello"));
        assertTrue(d.check("hellos"));
        assertTrue(d.check("walked"));
        assertTrue(d.check("liked"));
        assertTrue(d.check("unwalked"));
        assertTrue(d.check("Hello"));
        assertTrue(d.check("PARIS"));
        assertFalse(d.check("paris"));
        assertTrue(d.check("wrong"));
        assertFalse(d.check("wrongs"));
        assertFalse(d.check("helo"));
    }

    @Test
    void suggestsByTypoRepMapAndCase() throws Exception {
        HunspellDictionary d = dict();
        assertTrue(d.suggest("helo", 5).contains("hello"));
        assertEquals("World", d.suggest("Wrold", 5).get(0));
        assertTrue(d.suggest("fone", 5).contains("phone"));
        assertEquals("caf\u00e9", d.suggest("cafe", 5).get(0));
        assertEquals("hello world", d.suggest("helloworld", 5).get(0));
    }

    @Test
    void spellCheckerSkipsWhatIsNotAWord() throws Exception {
        SpellChecker s = SpellChecker.create(dict());
        assertTrue(s.isCorrect("NASA"));
        assertTrue(s.isCorrect("x"));
        assertTrue(s.isCorrect("abc123"));
        assertTrue(s.isCorrect("hello-world"));
        assertTrue(s.isCorrect("world's"));
        assertFalse(s.isCorrect("codenameone"));
        s.addWord("Codenameone");
        assertTrue(s.isCorrect("codenameone"));
        assertEquals("Codenameone", s.suggest("Codenameon", 3).get(0));
        s.ignore("zork");
        assertTrue(s.isCorrect("zork"));
        String text = "Hello wrold, see http://foo.bar and me@x.com";
        List<ProofIssue> issues = s.check(text);
        assertEquals(2, issues.size(), issues.toString());
        assertEquals("wrold", text.substring(issues.get(0).start, issues.get(0).end));
        assertEquals("and", text.substring(issues.get(1).start, issues.get(1).end));
        assertNull(SpellChecker.platform("en_US"));
    }

    @Test
    void grammarRules() {
        String text = "this is is an test.  ok , fine a apple";
        List<ProofIssue> issues = GrammarChecker.english().check(text);
        List<String> found = new ArrayList<String>();
        for (ProofIssue i : issues) {
            found.add(text.substring(i.start, i.end) + "->" + i.suggestions.get(0));
        }
        assertTrue(found.containsAll(Arrays.asList("is is->is", "an->a", "a->an", "this->This",
                "ok->Ok", "  -> ", " ,->,")), found.toString());
        assertEquals(7, issues.size(), found.toString());
        assertTrue(GrammarChecker.english().check("An hour and a unicorn. A user.").isEmpty());
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
