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
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/**
 * A layered overlay inside a desktop window.
 *
 * <p>Sheet, InteractionDialog and ToastBar all attach themselves to their host's layered
 * pane, and a window has to provide one that spans it exactly as a form's does. This case
 * puts content into that pane directly, which is the same attachment point those
 * components use, so a window whose layered pane was mis-sized or mis-stacked shows up as
 * a pixel difference here.</p>
 *
 * @author Shai Almog
 */
public class WindowOverlayTest extends WindowHostTest {

    /** One size is enough: this proves stacking, not reflow. */
    @Override
    protected int[][] sizes() {
        return new int[][]{{600, 450}};
    }

    @Override
    protected String baseImageName() {
        return "Window-Overlay";
    }

    @Override
    protected Component createWindowContent(int width, int height) {
        Container root = new Container(BoxLayout.y()) {
            private boolean overlayInstalled;

            @Override
            protected void initComponent() {
                super.initComponent();
                if (overlayInstalled) {
                    return;
                }
                overlayInstalled = true;
                com.codename1.ui.TopLevelContainer top = getTopLevelContainer();
                if (top == null) {
                    return;
                }
                Container layer = top.getFormLayeredPane(WindowOverlayTest.class, true);
                Label banner = new Label("Overlay layer");
                banner.setUIID("Title");
                layer.setLayout(new BorderLayout());
                layer.add(BorderLayout.SOUTH, banner);
            }
        };
        root.add(new Label("Base content"));
        root.add(new Label("sits under the overlay"));
        return root;
    }
}
