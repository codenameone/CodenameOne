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

package com.codename1.ui.editor;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputState;
import com.codename1.ui.layouts.BorderLayout;
import static org.junit.jupiter.api.Assertions.*;

/// Headless tests for the text-input path of the pure editor: IME/CJK marked-text composition (the
/// counterpart to the platform {@code setMarkedText}/{@code setComposingText}) and text commit.
class EditorViewInputTest extends UITestBase {

    // Japanese: NI HON (start with kana, convert to kanji); Chinese: ZHONG WEN
    private static final String KANA = "\u306B\u307B\u3093";
    private static final String KANJI = "\u65E5\u672C";

    private static final class NoopHost implements EditorHost {
        public boolean isTextInputSupported() {
            return false;
        }

        public Object startTextInput(TextInputClient client, TextInputConfig config) {
            return null;
        }

        public void updateTextInputState(Object handle, TextInputState state) {
        }

        public void stopTextInput(Object handle) {
        }

        public void editorChanged() {
        }

        public void fireEditorEvent(String type, String value) {
        }
    }

    private EditorView show(String text) {
        EditorView v = new EditorView(new NoopHost(), true);
        Form f = new Form("ev", new BorderLayout());
        f.add(BorderLayout.CENTER, v);
        f.show();
        for (int i = 0; i < 4; i++) {
            flushSerialCalls();
        }
        v.setText(text);
        v.setSelectionRange(text.length(), text.length());
        return v;
    }

    @FormTest
    void composingTextInsertsAtCaret() {
        EditorView v = show("ab");
        v.setComposingText(KANA, KANA.length());
        assertEquals("ab" + KANA, v.getText());
        // the marked (composing) range is reported to the platform for candidate placement
        TextInputState st = v.getEditingState();
        assertEquals(2, st.getComposingStart());
        assertEquals(2 + KANA.length(), st.getComposingEnd());
    }

    @FormTest
    void composingUpdatesInPlace() {
        EditorView v = show("ab");
        v.setComposingText(KANA, KANA.length());
        assertEquals("ab" + KANA, v.getText());
        // the IME converts the kana to kanji: the composing region is replaced, not appended
        v.setComposingText(KANJI, KANJI.length());
        assertEquals("ab" + KANJI, v.getText());
    }

    @FormTest
    void finishComposingKeepsTextAndClearsMarkedRange() {
        EditorView v = show("ab");
        v.setComposingText(KANJI, KANJI.length());
        v.finishComposing();
        assertEquals("ab" + KANJI, v.getText());
        TextInputState st = v.getEditingState();
        assertTrue(st.getComposingStart() < 0, "marked range cleared after finishComposing");
        // subsequent input is a normal insert at the caret, not part of a composition
        v.commitText("!");
        assertEquals("ab" + KANJI + "!", v.getText());
    }

    @FormTest
    void emptyComposingTextRemovesMarkedText() {
        EditorView v = show("ab");
        v.setComposingText(KANA, KANA.length());
        assertEquals("ab" + KANA, v.getText());
        // deleting during composition (empty marked text) removes the composed characters
        v.setComposingText("", 0);
        assertEquals("ab", v.getText());
    }

    @FormTest
    void commitTextReplacesActiveComposition() {
        EditorView v = show("ab");
        v.setComposingText(KANA, KANA.length());
        v.commitText(KANJI);
        assertEquals("ab" + KANJI, v.getText());
    }
}
