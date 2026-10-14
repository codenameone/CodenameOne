/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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

package com.codenameone.developerguide.snippets;

import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;

public final class DesktopTitleContent {
    private DesktopTitleContent() {
    }

    // tag::desktop-title-content[]
    public static Form createEditor(Command save, Command search) {
        Display.getInstance().setProperty("desktop.titleBar", "custom");
        Toolbar.setGlobalToolbar(true);
        Form editor = new Form("Quarterly report", new BorderLayout());
        Container title = new Container(new BorderLayout());
        title.add(BorderLayout.WEST, new Button(save));
        title.add(BorderLayout.CENTER, new Label("Quarterly report"));
        title.add(BorderLayout.EAST, new Button(search));
        editor.getToolbar().setTitleComponent(title);
        return editor;
    }
    // end::desktop-title-content[]
}
