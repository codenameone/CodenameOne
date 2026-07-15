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

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;

/**
 * Screenshot coverage for the pure Codename One text editors (no BrowserComponent). Renders a
 * {@link CodeEditor} whose Java source embeds a right-to-left (Hebrew) string literal and comment, plus a
 * {@link RichTextArea} with mixed left-to-right / right-to-left formatted content, so the golden exercises
 * syntax highlighting, the gutter, bidi reordering and rich styled runs in one capture.
 *
 * <p>Hebrew text is written with {@code \\u05Dx} escapes to keep the source ASCII only.</p>
 */
public class PureEditorScreenshotTest extends BaseTest {
    // "shalom" (Hebrew, strong right-to-left)
    private static final String SHALOM = "\u05E9\u05DC\u05D5\u05DD";

    @Override
    public boolean runTest() {
        Form form = createForm("Pure Editors", BoxLayout.y(), "PureEditors");

        ScreenshotPureEditors.Code code = new ScreenshotPureEditors.Code("java",
                "public class Greeting {\n"
                + "    // " + SHALOM + " means hello\n"
                + "    String msg = \"" + SHALOM + "\";\n"
                + "    int count = 42;\n"
                + "}\n");
        code.setShowLineNumbers(true);
        code.setPreferredH(Display.getInstance().getDisplayHeight() / 2);
        form.add(code);

        ScreenshotPureEditors.Rich rich = new ScreenshotPureEditors.Rich(
                "<p>Hello <b>world</b> and <i>" + SHALOM + "</i></p>"
                + "<ul><li>one</li><li>two</li></ul>");
        rich.setPreferredH(Display.getInstance().getDisplayHeight() / 3);
        form.add(rich);

        form.show();
        return true;
    }
}
