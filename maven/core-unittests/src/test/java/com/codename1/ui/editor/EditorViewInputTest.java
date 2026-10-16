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
import com.codename1.ui.EditField;
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

    private NoopHost lastHost;

    private static final class NoopHost implements EditorHost {
        int changes;
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
            changes++;
        }

        public void fireEditorEvent(String type, String value) {
        }
    }

    private EditorView show(String text) {
        lastHost = new NoopHost();
        EditorView v = new EditorView(lastHost, true);
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

    @FormTest
    void aSecondCommitFollowsTheFirstRatherThanReplacingIt() {
        // A commit ends the composition, and the platform bridges rely on it: UIKit's
        // insertText: replaces marked text, and the iOS view clears its own marked range
        // and sends a commit with no finishComposing behind it. If the composing range
        // outlived the commit, the next commit would replace it instead of following it,
        // and an input method that delivers a value in fragments -- dictation,
        // handwriting -- could not build one up at all.
        //
        // It does not outlive it: the commit replaces the composed range through the
        // ordinary document-change path, which clears the composition. This test exists
        // because that is easy to doubt from reading commitText alone, where nothing
        // clears it in view.
        EditField f = new EditField();
        f.setComposingText("ab", 0);
        f.commitText("ab");
        f.commitText("cd");
        assertEquals("abcd", f.getText());
    }
    @FormTest
    void semanticDocumentCommandsPreserveSelectionDirection() {
        EditorView v = show("one\ntwo\nthree");
        v.onKeyCommand(TextInputClient.KEY_HOME, TextInputClient.MOD_CTRL | TextInputClient.MOD_SHIFT);
        TextInputState state = v.getEditingState();
        assertEquals(0, state.getSelectionStart());
        assertEquals(13, state.getSelectionEnd());
        assertEquals(13, state.getSelectionAnchor());
        assertEquals(0, state.getSelectionFocus());
        v.commitText("replacement");
        assertEquals("replacement", v.getText());
        v.onKeyCommand(TextInputClient.KEY_HOME, TextInputClient.MOD_CTRL);
        assertEquals(0, v.getEditingState().getSelectionEnd());
        v.onKeyCommand(TextInputClient.KEY_END, TextInputClient.MOD_CTRL);
        assertEquals(11, v.getEditingState().getSelectionEnd());
    }

    @FormTest
    void wordDeletionRespectsSelectionUndoAndReadOnly() {
        EditorView v = show("one two three");
        v.onKeyCommand(TextInputClient.KEY_BACKSPACE, TextInputClient.MOD_ALT);
        assertEquals("one two ", v.getText());
        v.onKeyCommand(TextInputClient.KEY_UNDO, 0);
        assertEquals("one two three", v.getText());
        v.setSelectionRange(0, 0);
        v.onKeyCommand(TextInputClient.KEY_DELETE, TextInputClient.MOD_ALT);
        assertEquals(" two three", v.getText());
        v.setSelectionRange(4, 1);
        v.onKeyCommand(TextInputClient.KEY_BACKSPACE, TextInputClient.MOD_ALT);
        assertEquals("  three", v.getText());
        v.setEditableState(false);
        v.onKeyCommand(TextInputClient.KEY_DELETE, TextInputClient.MOD_ALT);
        v.onKeyCommand(TextInputClient.KEY_BACKSPACE, TextInputClient.MOD_ALT);
        assertEquals("  three", v.getText());
    }

    @FormTest
    void paragraphCommandsKeepSelectionAndAdvanceAtBoundaries() {
        EditorView v = show("one\ntwo\nthree");
        v.setSelectionRange(5, 5);
        v.onKeyCommand(TextInputClient.KEY_HOME, TextInputClient.MOD_ALT);
        assertEquals(4, v.getEditingState().getSelectionEnd());
        v.onKeyCommand(TextInputClient.KEY_UP, TextInputClient.MOD_ALT | TextInputClient.MOD_SHIFT);
        assertEquals(0, v.getEditingState().getSelectionStart());
        assertEquals(4, v.getEditingState().getSelectionEnd());
        assertEquals(4, v.getEditingState().getSelectionAnchor());
        assertEquals(0, v.getEditingState().getSelectionFocus());
        v.setSelectionRange(5, 5);
        v.onKeyCommand(TextInputClient.KEY_END, TextInputClient.MOD_ALT);
        assertEquals(7, v.getEditingState().getSelectionEnd());
        v.onKeyCommand(TextInputClient.KEY_DOWN, TextInputClient.MOD_ALT | TextInputClient.MOD_SHIFT);
        assertEquals(7, v.getEditingState().getSelectionStart());
        assertEquals(13, v.getEditingState().getSelectionEnd());
        v.onKeyCommand(TextInputClient.KEY_DOWN, TextInputClient.MOD_ALT);
        assertEquals(13, v.getEditingState().getSelectionEnd());
    }


    @FormTest
    void wordDeletionUsesUnicodeLettersMarksAndCodePoints() {
        String[] words = {"ελληνικά", "العربية", "中文", "हिन्दी", "a\u0301", "\uD801\uDC00\uD801\uDC01"};
        for (String word : words) {
            EditorView v = show(word + " " + word);
            v.onKeyCommand(TextInputClient.KEY_BACKSPACE, TextInputClient.MOD_ALT);
            assertEquals(word + " ", v.getText());
            v.performUndo();
            v.setSelectionRange(0, 0);
            v.onKeyCommand(TextInputClient.KEY_DELETE, TextInputClient.MOD_ALT);
            assertEquals(" " + word, v.getText());
            v.performUndo();
            v.setSelectionRange(0, 0);
            v.onKeyCommand(TextInputClient.KEY_RIGHT, TextInputClient.MOD_ALT | TextInputClient.MOD_SHIFT);
            assertEquals(word.length(), v.getEditingState().getSelectionFocus());
        }
    }

    @FormTest
    void emptyWordDeletionPreservesRedoAndDoesNotNotifyHost() {
        EditorView v = show("one two");
        v.onKeyCommand(TextInputClient.KEY_BACKSPACE, TextInputClient.MOD_ALT);
        v.performUndo();
        v.setSelectionRange(0, 0);
        int changes = lastHost.changes;
        v.onKeyCommand(TextInputClient.KEY_BACKSPACE, TextInputClient.MOD_ALT);
        assertEquals(changes, lastHost.changes);
        v.performRedo();
        assertEquals("one ", v.getText());
        v.performUndo();
        v.setSelectionRange(v.getText().length(), v.getText().length());
        changes = lastHost.changes;
        v.onKeyCommand(TextInputClient.KEY_DELETE, TextInputClient.MOD_ALT);
        assertEquals(changes, lastHost.changes);
        v.performRedo();
        assertEquals("one ", v.getText());
    }

    @FormTest
    void scrollPageCommandsKeepCaretSelectionAndUndoHistory() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            text.append("line ").append(i).append('\n');
        }
        EditorView v = show(text.toString());
        v.setSelectionRange(2, 0);
        TextInputState before = v.getEditingState();
        int changes = lastHost.changes;
        assertEquals(0, v.getVerticalScroll());
        v.onKeyCommand(TextInputClient.KEY_SCROLL_PAGE_DOWN, TextInputClient.MOD_SHIFT);
        assertTrue(v.getVerticalScroll() > 0);
        assertEquals(before.getSelectionAnchor(), v.getEditingState().getSelectionAnchor());
        assertEquals(before.getSelectionFocus(), v.getEditingState().getSelectionFocus());
        assertEquals(text.toString(), v.getText());
        assertEquals(changes, lastHost.changes);
        v.onKeyCommand(TextInputClient.KEY_SCROLL_PAGE_UP, 0);
        assertEquals(0, v.getVerticalScroll());
        v.onKeyCommand(TextInputClient.KEY_PAGE_DOWN, 0);
        assertTrue(v.getEditingState().getSelectionFocus() > 0);
    }
}
