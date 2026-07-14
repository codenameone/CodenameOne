/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.editor;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputState;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import static org.junit.jupiter.api.Assertions.*;

/// Verifies the editor shows its FIRST line at the top after loading content (i.e. it is not scrolled so
/// the first line hides under the title bar).
class EditorFirstLineTest extends UITestBase {

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

    @FormTest
    void codeViewShowsFirstLineAfterSetText() {
        Toolbar.setGlobalToolbar(true);
        CodeView v = new CodeView(new NoopHost());
        Form f = new Form("Code", new BorderLayout());
        f.setToolbar(new Toolbar());
        f.setTitle("Code");
        f.add(BorderLayout.CENTER, v);
        f.show();
        for (int i = 0; i < 6; i++) {
            flushSerialCalls();
        }
        v.setText("line1\nline2\nline3\nline4\nline5");
        for (int i = 0; i < 6; i++) {
            flushSerialCalls();
        }
        assertEquals(0, v.firstVisibleLine(),
                "the editor must show line 0 at the top (first line not scrolled under the title bar)");
        assertEquals(0, v.getVerticalScroll(), "vertical scroll must be 0 after loading content");
    }

    /// Reproduces the device/simulator ordering: the backend flushes the queued setText BEFORE the
    /// component is laid out (height still 0). Previously scrollCaretVisible() ran against a negative
    /// contentHeight() and pushed scrollY to ~one line, hiding the first line under the title bar.
    @FormTest
    void codeViewShowsFirstLineWhenTextSetBeforeLayout() {
        Toolbar.setGlobalToolbar(true);
        CodeView v = new CodeView(new NoopHost());
        // set text while the component is still unlaid-out (height == 0), as the backend does on device
        assertEquals(0, v.getHeight(), "precondition: component not laid out yet");
        v.setText("line1\nline2\nline3\nline4\nline5");
        Form f = new Form("Code", new BorderLayout());
        f.setToolbar(new Toolbar());
        f.setTitle("Code");
        f.add(BorderLayout.CENTER, v);
        f.show();
        for (int i = 0; i < 6; i++) {
            flushSerialCalls();
        }
        assertEquals(0, v.getVerticalScroll(),
                "vertical scroll must be 0 even when text is set before the first layout");
        assertEquals(0, v.firstVisibleLine(),
                "line 0 must be visible at the top even when text is set before the first layout");
    }
}
