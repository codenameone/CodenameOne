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

package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CodeEditor;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/**
 * Screenshot coverage for {@link CodeEditor}. Renders a read-only (caret-free, for a deterministic
 * capture) Java snippet with the line-number gutter and syntax highlighting.
 *
 * The test forces the pure Codename One text engine (see {@link ScreenshotPureEditors}), which paints
 * through the regular component pipeline on every port, so the capture simply waits for the editor's
 * ready event -- no web view, settle timers or retry machinery is involved.
 */
public class CodeEditorScreenshotTest extends BaseTest {
    private CodeEditor editor;

    @Override
    public boolean runTest() throws Exception {
        Form form = createForm("Code Editor", new BorderLayout(), "CodeEditor");
        editor = new ScreenshotPureEditors.Code();
        editor.setLanguage("java");
        editor.setShowLineNumbers(true);
        editor.setReadOnly(true);
        editor.setText("public class Main {\n"
                + "    public static void main(String[] args) {\n"
                + "        // greet the user\n"
                + "        int count = 3;\n"
                + "        for (int i = 0; i < count; i++) {\n"
                + "            System.out.println(\"hello \" + i);\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        form.add(BorderLayout.CENTER, editor);
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, final Runnable run) {
        // fires immediately when the backend already finished initializing
        editor.onReady(run);
    }
}
