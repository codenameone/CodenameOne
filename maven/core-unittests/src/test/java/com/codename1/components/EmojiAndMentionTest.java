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
package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.TextField;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmojiAndMentionTest extends UITestBase {

    @FormTest
    void everyCategoryHasEmojiAndNames() {
        assertEquals(8, EmojiPicker.getCategoryCount());
        for (int i = 0; i < EmojiPicker.getCategoryCount(); i++) {
            assertTrue(EmojiPicker.getEmoji(i).length > 5, "category " + i);
        }
    }

    @FormTest
    void searchMatchesNamesIgnoringCase() {
        List<String> hearts = EmojiPicker.search("HEART");
        assertFalse(hearts.isEmpty());
        assertTrue(EmojiPicker.search("   ").isEmpty());
        assertTrue(EmojiPicker.search("zzzqqq").isEmpty());
    }

    @FormTest
    void pickInsertsAtCursorAndRecordsRecent() {
        EmojiPicker p = new EmojiPicker();
        TextField f = new TextField("ab");
        p.bindTo(f);
        String e = EmojiPicker.getEmoji(0)[0];
        p.pick(e);
        assertEquals(e, p.getSelectedEmoji());
        assertTrue(f.getText().contains(e));
        assertEquals(e, EmojiPicker.getRecents().get(0));
        com.codename1.io.Preferences.delete("cn1.emojiPicker.recent");
    }

    @FormTest
    void mentionMatchesWordStarts() {
        assertTrue(MentionField.matches("Grace Hopper", "hop"));
        assertTrue(MentionField.matches("Grace Hopper", "GR"));
        assertFalse(MentionField.matches("Grace Hopper", "race"));
        assertTrue(MentionField.matches("anything", ""));
    }

    @FormTest
    void mentionSuggestsAndReplacesOnlyTheToken() {
        MentionField m = new MentionField();
        m.setCandidates("Ada Lovelace", "Alan Turing", "Grace Hopper");
        m.setText("hi @al");
        assertTrue(m.isShowingSuggestions());
        m.pick("Alan Turing");
        assertEquals("hi @Alan Turing ", m.getText());
        assertFalse(m.isShowingSuggestions());
        assertEquals(Arrays.asList("Alan Turing"), m.getMentions());
    }

    @FormTest
    void triggerInsideAWordIsNotAMention() {
        MentionField m = new MentionField();
        m.setCandidates("Ada Lovelace");
        m.setText("mail me at x@a");
        assertFalse(m.isShowingSuggestions());
    }

    @FormTest
    void mentionsNeedAWordBoundary() {
        MentionField m = new MentionField();
        m.setCandidates("Ann");
        m.setText("@an");
        m.pick("Ann");
        assertEquals(Arrays.asList("Ann"), m.getMentions());
        m.setText("hi @Anna");
        assertTrue(m.getMentions().isEmpty());
        m.setText("hi @Ann.");
        assertEquals(Arrays.asList("Ann"), m.getMentions(), "a name typed back is found again");
    }
}
