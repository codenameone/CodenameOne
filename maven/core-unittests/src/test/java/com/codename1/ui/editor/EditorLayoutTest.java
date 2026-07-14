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
import com.codename1.ui.CodeEditor;
import com.codename1.ui.Form;
import com.codename1.ui.RichTextArea;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import static org.junit.jupiter.api.Assertions.*;

/// Verifies the editors lay out below the form title bar (Toolbar) rather than under it.
class EditorLayoutTest extends UITestBase {

    private void pump() {
        for (int i = 0; i < 6; i++) {
            flushSerialCalls();
        }
    }

    @FormTest
    void codeEditorSitsBelowToolbar() {
        Toolbar.setGlobalToolbar(true);
        Form f = new Form("Code", new BorderLayout());
        f.setToolbar(new Toolbar());
        f.setTitle("Code");
        CodeEditor ed = new CodeEditor("java", "line1\nline2\nline3");
        f.add(BorderLayout.CENTER, ed);
        f.show();
        pump();
        Toolbar tb = f.getToolbar();
        assertNotNull(tb);
        int toolbarBottom = tb.getAbsoluteY() + tb.getHeight();
        assertTrue(ed.getAbsoluteY() >= toolbarBottom,
                "code editor top (" + ed.getAbsoluteY() + ") must be at or below the toolbar bottom ("
                        + toolbarBottom + ")");
    }

    @FormTest
    void richTextAreaSitsBelowToolbar() {
        Toolbar.setGlobalToolbar(true);
        Form f = new Form("Rich", new BorderLayout());
        f.setToolbar(new Toolbar());
        f.setTitle("Rich");
        RichTextArea ed = new RichTextArea("<p>hello</p>");
        f.add(BorderLayout.CENTER, ed);
        f.show();
        pump();
        Toolbar tb = f.getToolbar();
        int toolbarBottom = tb.getAbsoluteY() + tb.getHeight();
        assertTrue(ed.getAbsoluteY() >= toolbarBottom,
                "rich text area top (" + ed.getAbsoluteY() + ") must be at or below the toolbar bottom ("
                        + toolbarBottom + ")");
    }
}
