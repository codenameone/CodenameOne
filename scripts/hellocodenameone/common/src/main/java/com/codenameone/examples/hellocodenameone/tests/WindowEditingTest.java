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

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;

/**
 * Text input inside a desktop window.
 *
 * <p>Native editing is the case that used to attach the platform's editor to the main
 * window's canvas unconditionally, so a field inside a window put its caret on the wrong
 * window entirely. The port now resolves the owning window for both the editor and its
 * bounds; this golden is what would catch a regression, since a misplaced native editor
 * is invisible in the window's own capture.</p>
 *
 * @author Shai Almog
 */
public class WindowEditingTest extends WindowHostTest {

    @Override
    protected String baseImageName() {
        return "Window-Editing";
    }

    @Override
    protected Component createWindowContent(int width, int height) {
        Container root = new Container(BoxLayout.y());
        root.add(new Label("Text input"));

        TextField single = new TextField("Single line");
        single.setHint("Type here");
        root.add(single);

        TextField password = new TextField("", "Password", 20, TextField.PASSWORD);
        root.add(password);

        TextArea multi = new TextArea("Multi-line content\nsecond line\nthird line", 4, 30);
        root.add(multi);
        return root;
    }
}
