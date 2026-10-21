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
import com.codename1.ui.Form;

public final class DesktopStandardCommands {
    private DesktopStandardCommands() {
    }

    // tag::desktop-standard-commands[]
    public static void addStandardCommands(Form form) {
        // Named, placed and given a shortcut the way the running platform does it:
        // "Settings..." in the macOS application menu with Cmd+comma, "Options" under
        // Tools on Windows, "Preferences" at the foot of Edit on Linux.
        Command settings = Command.createDesktopRole(Command.DESKTOP_ROLE_SETTINGS,
                e -> System.out.println("Show settings"));
        // "Quit" on macOS and Linux, "Exit" at the foot of File on Windows.
        Command quit = Command.createDesktopRole(Command.DESKTOP_ROLE_QUIT,
                e -> System.out.println("Quit"));
        // Disabled until the document has unsaved changes.
        Command save = Command.createDesktopRole(Command.DESKTOP_ROLE_SAVE,
                e -> {
                    System.out.println("Save");
                    form.setDocumentModified(false);
                });
        // Only on the platforms named.
        Command services = new Command("Show Services");
        services.setDesktopMenu(Command.DESKTOP_MENU_HELP).setDesktopPlatforms("mac");

        form.getToolbar().addCommandToOverflowMenu(settings);
        form.getToolbar().addCommandToOverflowMenu(save);
        form.getToolbar().addCommandToOverflowMenu(quit);
        form.getToolbar().addCommandToOverflowMenu(services);

        // A dot in the close button on macOS, "*" before the title elsewhere.
        form.setDocumentModified(true);
    }
    // end::desktop-standard-commands[]
}
