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
import com.codename1.ui.Sheet;
import com.codename1.ui.TopLevelContainer;
import com.codename1.ui.layouts.BoxLayout;

/**
 * A {@code Sheet} shown on a desktop window.
 *
 * <p>A sheet attached to whatever form was current, so one raised from a window
 * appeared on the main window instead -- and could not then be dismissed at all,
 * because its teardown was gated on a form lookup that is null inside a window.</p>
 *
 * @author Shai Almog
 */
public class WindowSheetTest extends WindowHostTest {

    /** One size: this proves attachment, not reflow. */
    @Override
    protected int[][] sizes() {
        return new int[][]{{900, 700}};
    }

    @Override
    protected String baseImageName() {
        return "Window-Sheet";
    }

    @Override
    protected Component createWindowContent(int width, int height) {
        Container root = new Container(BoxLayout.y()) {
            private boolean sheetShown;

            @Override
            protected void initComponent() {
                super.initComponent();
                if (sheetShown) {
                    return;
                }
                sheetShown = true;
                TopLevelContainer top = getTopLevelContainer();
                if (top == null) {
                    return;
                }
                Sheet sheet = new Sheet(null, "Options");
                sheet.getContentPane().add(new Label("Rename"));
                sheet.getContentPane().add(new Label("Duplicate"));
                sheet.setTopLevelHost(top);
                sheet.show(0);
            }
        };
        root.add(new Label("Window content"));
        root.add(new Label("with a sheet over it"));
        return root;
    }
}
