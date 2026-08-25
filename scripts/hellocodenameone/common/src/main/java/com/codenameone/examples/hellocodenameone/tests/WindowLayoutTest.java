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

import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.Slider;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;

/**
 * Ordinary widgets laid out inside a desktop window.
 *
 * <p>This is the baseline case of the windowed suite: it proves that layout, theming
 * and font metrics resolve against the <em>window's</em> size rather than the
 * application's main surface. The three capture sizes are what make that visible --
 * the same content at 400x300, 900x700 and a deliberately non-square 1000x400 has to
 * reflow, and a window that was still measuring itself against the main display would
 * produce three near-identical goldens.</p>
 *
 * @author Shai Almog
 */
public class WindowLayoutTest extends WindowHostTest {

    @Override
    protected String baseImageName() {
        return "Window-Layout";
    }

    @Override
    protected Component createWindowContent(int width, int height) {
        Container root = new Container(new BorderLayout());

        Label heading = new Label("Codename One window " + width + "x" + height);
        heading.setUIID("Title");
        root.add(BorderLayout.NORTH, heading);

        Container body = new Container(BoxLayout.y());
        body.add(new Label("Widgets in a native window"));
        body.add(new Button("Button"));
        body.add(new CheckBox("Check box"));
        TextField field = new TextField("Editable text");
        body.add(field);
        Slider slider = new Slider();
        slider.setProgress(40);
        body.add(slider);
        root.add(BorderLayout.CENTER, body);

        Container footer = new Container(new GridLayout(1, 3));
        footer.add(new Label("one"));
        footer.add(new Label("two"));
        footer.add(new Label("three"));
        root.add(BorderLayout.SOUTH, footer);
        return root;
    }
}
