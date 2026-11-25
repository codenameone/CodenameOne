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
package com.codenameone.devruntime;

import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;

import java.util.Hashtable;

/**
 * The source of whatever is loaded, and the way to edit it.
 *
 * <p>This exists for a rule as much as for a person. The App Store permits an
 * app to run code it downloaded only where the source is "completely viewable
 * and editable by the user" -- which is also why the runtime refuses to load a
 * bundle whose sources it does not have. Viewing is this screen; editing is
 * "Edit", which saves the received program as a project in the on-device
 * editor, where it can be changed and run again. A text box that accepted
 * typing and went nowhere would be neither.</p>
 */
public class DeviceRuntimeSourceForm extends Form {
    public DeviceRuntimeSourceForm(final Form back) {
        super("Source", new BorderLayout());
        DeviceRuntimeService svc = DeviceRuntimeService.getInstance();
        String src = svc.getLoadedSource();
        final Hashtable<String, String> files = svc.getLoadedSources();
        final String main = svc.getLoadedName();
        TextArea source = new TextArea(src == null || src.length() == 0
                ? "Nothing is loaded. Send a program from your computer and its source appears here."
                : src, 40, 80);
        source.setEditable(false);
        source.setGrowByContent(true);
        source.getAllStyles().setFont(Font.createSystemFont(
                Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        add(BorderLayout.CENTER, source);
        getToolbar().setBackCommand("", e -> back.showBack());
        if (!files.isEmpty()) {
            getToolbar().addMaterialCommandToRightBar("Edit", FontImage.MATERIAL_EDIT,
                    e -> openInEditor(files, main));
        }
    }

    /// Stops the received program and opens a copy of its sources as a project.
    static void openInEditor(Hashtable<String, String> files, String main) {
        String simple = main == null || main.length() == 0 ? "Received"
                : main.substring(main.lastIndexOf('.') + 1);
        String name = simple;
        java.util.List<String> existing = IdeProject.listNames();
        for (int i = 2; existing.contains(name); i++) {
            name = simple + " " + i;
        }
        final IdeProject p = IdeProject.create(name, files);
        DeviceRuntimeService.getInstance().stopProgramThen(() -> IdeForm.showProject(p));
    }

    /** Shows the source screen, from any thread. */
    public static void showIt(final Form back) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                new DeviceRuntimeSourceForm(back).show();
            }
        });
    }
}
