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

import com.codename1.ui.Command;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;

public final class DesktopMenuStructure {
    private DesktopMenuStructure() {
    }

    // tag::desktop-menu-structure[]
    public static void addViewMenu(Form form) {
        // A check item: the state flips before the action runs.
        Command sidebar = Command.create("Show Sidebar", null, e -> {
            boolean visible = ((Command) e.getSource()).isDesktopChecked();
            System.out.println("Sidebar visible: " + visible);
        });
        sidebar.setDesktopMenu(Command.DESKTOP_MENU_VIEW).setDesktopChecked(true);

        // A radio group in a submenu, a separator group below the check item.
        String[] zooms = {"50%", "100%", "200%"};
        for (String zoom : zooms) {
            Command c = Command.create(zoom, null, e -> System.out.println("Zoom " + zoom));
            c.setDesktopMenu("View/Zoom").setDesktopMenuGroup(1).setDesktopRadioGroup("zoom");
            c.setDesktopChecked("100%".equals(zoom));
            form.getToolbar().addCommandToOverflowMenu(c);
        }

        // An icon and a separator group of its own.
        Command refresh = Command.createMaterial("Refresh", FontImage.MATERIAL_REFRESH,
                e -> System.out.println("Refresh"));
        refresh.setDesktopMenu(Command.DESKTOP_MENU_VIEW).setDesktopMenuGroup(2);
        refresh.setDesktopShortcut('R');

        form.getToolbar().addCommandToOverflowMenu(sidebar);
        form.getToolbar().addCommandToOverflowMenu(refresh);
    }
    // end::desktop-menu-structure[]
}
