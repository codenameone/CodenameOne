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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.editor.CodeView;
import com.codename1.ui.editor.EditorView;
import com.codename1.ui.layouts.BorderLayout;
import static org.junit.jupiter.api.Assertions.*;

/// Verifies the platform key-command path (`onKeyCommand`) honors Alt (word) and Ctrl/Cmd (line /
/// document) modifiers and that shift-tab dedents while tab indents.
class EditorKeyCommandTest extends UITestBase {

    private EditorView show(String text) {
        implementation.setTextInputSupported(true);
        CodeEditor editor = new CodeEditor("java", text);
        Form f = new Form("k", new BorderLayout());
        f.add(BorderLayout.CENTER, editor);
        f.show();
        for (int i = 0; i < 8; i++) {
            flushSerialCalls();
        }
        return (EditorView) editor.getComponentAt(0);
    }

    @FormTest
    void altArrowJumpsByWord() {
        EditorView v = show("alpha beta gamma");
        v.setSelectionRange(16, 16); // end of "gamma"
        v.onKeyCommand(TextInputClient.KEY_LEFT, TextInputClient.MOD_ALT);
        assertEquals(11, v.getCaretOffset(), "alt+left to start of gamma");
        v.onKeyCommand(TextInputClient.KEY_LEFT, TextInputClient.MOD_ALT);
        assertEquals(6, v.getCaretOffset(), "alt+left to start of beta");
        v.onKeyCommand(TextInputClient.KEY_RIGHT, TextInputClient.MOD_ALT);
        assertEquals(10, v.getCaretOffset(), "alt+right to end of beta");
    }

    @FormTest
    void ctrlArrowJumpsToLineAndDocEdges() {
        EditorView v = show("first line\nsecond line\nthird line");
        int mid = 15; // inside "second line"
        v.setSelectionRange(mid, mid);
        v.onKeyCommand(TextInputClient.KEY_LEFT, TextInputClient.MOD_CTRL);
        assertEquals(11, v.getCaretOffset(), "ctrl/cmd+left to line home");
        v.onKeyCommand(TextInputClient.KEY_RIGHT, TextInputClient.MOD_CTRL);
        assertEquals(22, v.getCaretOffset(), "ctrl/cmd+right to line end");
        v.onKeyCommand(TextInputClient.KEY_UP, TextInputClient.MOD_CTRL);
        assertEquals(0, v.getCaretOffset(), "ctrl/cmd+up to document start");
        v.onKeyCommand(TextInputClient.KEY_DOWN, TextInputClient.MOD_CTRL);
        assertEquals(v.getText().length(), v.getCaretOffset(), "ctrl/cmd+down to document end");
    }

    @FormTest
    void tabIndentsAndShiftTabDedents() {
        EditorView v = show("foo\nbar\nbaz");
        // select all three lines
        v.setSelectionRange(0, v.getText().length());
        v.onKeyCommand(TextInputClient.KEY_TAB, 0);
        assertEquals("    foo\n    bar\n    baz", v.getText(), "tab indents every selected line");
        v.setSelectionRange(0, v.getText().length());
        v.onKeyCommand(TextInputClient.KEY_TAB, TextInputClient.MOD_SHIFT);
        assertEquals("foo\nbar\nbaz", v.getText(), "shift-tab dedents every selected line");
    }

    @FormTest
    void shiftTabDedentsCurrentLineWithoutSelection() {
        EditorView v = show("        indented");
        v.setSelectionRange(12, 12);
        v.onKeyCommand(TextInputClient.KEY_TAB, TextInputClient.MOD_SHIFT);
        assertEquals("    indented", v.getText(), "shift-tab removes one indent level from the line");
    }
}
