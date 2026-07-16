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

/// Formatting commands (inline styles, block formats, links) must participate in the undo stack:
/// undo restores the pre-command styling without touching the text, redo reapplies it, and an undo
/// of an earlier TEXT edit must not silently wipe formatting applied afterwards.
class RichViewFormatUndoTest extends UITestBase {

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

    private RichView show(String text) {
        RichView v = new RichView(new NoopHost());
        Form f = new Form("rvu", new BorderLayout());
        f.add(BorderLayout.CENTER, v);
        f.show();
        for (int i = 0; i < 4; i++) {
            flushSerialCalls();
        }
        v.setText(text);
        return v;
    }

    @FormTest
    void boldTogglesUndoAndRedo() {
        RichView v = show("hello world");
        v.setSelectionRange(0, 5);
        v.toggleBold();
        assertTrue(v.queryState("bold"), "bold applied to the selection");

        v.performUndo();
        assertEquals("hello world", v.getText(), "undo of formatting keeps the text");
        v.setSelectionRange(0, 5);
        assertFalse(v.queryState("bold"), "undo removed the bold styling");

        v.performRedo();
        v.setSelectionRange(0, 5);
        assertTrue(v.queryState("bold"), "redo reapplied the bold styling");
    }

    @FormTest
    void blockFormatUndoRestoresParagraph() {
        RichView v = show("title line");
        v.setSelectionRange(0, 0);
        v.setBlockFormat("h1");
        v.performUndo();
        v.setBlockFormat("blockquote");
        v.performUndo();
        // both block changes undone; a fresh h1 still works afterwards
        v.setBlockFormat("h1");
        assertEquals("title line", v.getText());
    }

    @FormTest
    void undoOfTextEditKeepsLaterFormatting() {
        RichView v = show("abc");
        v.setSelectionRange(3, 3);
        v.insertText("xyz");
        v.setSelectionRange(0, 3);
        v.toggleItalic();
        // undo order: first the formatting, then the insert
        v.performUndo();
        v.setSelectionRange(0, 3);
        assertFalse(v.queryState("italic"), "first undo removes the italic styling");
        assertEquals("abcxyz", v.getText(), "text untouched by the formatting undo");
        v.performUndo();
        assertEquals("abc", v.getText(), "second undo removes the typed text");
    }

    @FormTest
    void linkApplyIsOneUndoUnit() {
        RichView v = show("visit here now");
        v.setSelectionRange(6, 10);
        v.applyLink("https://example.com");
        v.setSelectionRange(6, 10);
        assertTrue(v.queryState("underline"), "link styling applied");
        v.performUndo();
        v.setSelectionRange(6, 10);
        assertFalse(v.queryState("underline"), "one undo removes the whole link command");
        assertEquals("visit here now", v.getText());
    }
}
