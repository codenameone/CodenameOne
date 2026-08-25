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
import com.codename1.ui.layouts.BoxLayout;

/**
 * A long scrollable list inside a desktop window.
 *
 * <p>Scrolling is one of the behaviours that goes <em>silently</em> dead if a component
 * cannot resolve its top level: {@code isScrollableY} consults the enclosing top level
 * for the area hidden by a virtual keyboard, and the smooth-scroll motion registers
 * itself with that top level's internal animation registry. A window that failed to
 * resolve either would render this content unscrolled and clipped rather than throwing,
 * which is exactly why it earns a golden.
 *
 * @author Shai Almog
 */
public class WindowScrollTest extends WindowHostTest {

    @Override
    protected String baseImageName() {
        return "Window-Scroll";
    }

    @Override
    protected Component createWindowContent(int width, int height) {
        Container list = new Container(BoxLayout.y());
        list.setScrollableY(true);
        for (int iter = 0; iter < 40; iter++) {
            Label l = new Label("Row " + iter);
            l.setUIID(iter % 2 == 0 ? "Label" : "MultiLine1");
            list.add(l);
        }
        return list;
    }
}
