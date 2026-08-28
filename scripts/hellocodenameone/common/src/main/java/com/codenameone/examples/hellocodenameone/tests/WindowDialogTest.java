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
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.TopLevelContainer;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/**
 * A modal {@code Dialog} shown from inside a desktop window.
 *
 * <p>A dialog used to resolve the current form, so one opened from a window landed on
 * the main window behind it and the window it was opened from sat there untouched. It
 * goes into the window's own layered pane now, and this captures that it does: the
 * golden shows the dialog centred on the window with the window's content dimmed
 * behind it, which is only possible if both are on the same surface.</p>
 *
 * <p>Three sizes, because this case is about reflow. The deliberately wide one catches a
 * dialog still measuring {@code Display.getDisplayWidth()} rather than its own host.</p>
 *
 * @author Shai Almog
 */
public class WindowDialogTest extends WindowHostTest {

    @Override
    protected String baseImageName() {
        return "Window-Dialog";
    }

    @Override
    protected Component createWindowContent(int width, int height) {
        Container root = new Container(BoxLayout.y()) {
            private boolean dialogShown;

            @Override
            protected void initComponent() {
                super.initComponent();
                if (dialogShown) {
                    return;
                }
                dialogShown = true;
                TopLevelContainer top = getTopLevelContainer();
                if (top == null) {
                    return;
                }
                Dialog d = new Dialog("Confirm");
                d.setLayout(new BorderLayout());
                d.add(BorderLayout.CENTER, new Label("Delete the document?"));
                d.setTopLevelHost(top);
                // Modeless: a modal dialog parks the caller, and the harness is the
                // caller. The rendering is identical either way.
                d.showModeless();
            }
        };
        root.add(new Label("Window content"));
        root.add(new Label("dimmed behind the dialog"));
        return root;
    }
}
