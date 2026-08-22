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
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;

/**
 * The source of whatever is loaded, shown and editable.
 *
 * <p>This exists for a rule as much as for a person. The App Store permits an
 * app to run code it downloaded only in narrow circumstances, and one of the
 * stated conditions is that the source is "completely viewable and editable by
 * the user" -- which is also why the runtime refuses to load a bundle whose
 * sources it does not have. Removing this screen would make the app
 * unsubmittable, not merely less useful.</p>
 *
 * @author Shai Almog
 */
public class DeviceRuntimeSourceForm extends Form {
    public DeviceRuntimeSourceForm(final Form back) {
        super("Source", new BorderLayout());
        String src = DeviceRuntimeService.getInstance().getLoadedSource();
        TextArea source = new TextArea(src == null || src.length() == 0
                ? "Nothing is loaded. Push a program and its source appears here."
                : src, 40, 80);
        source.setEditable(true);
        source.setGrowByContent(true);
        source.getAllStyles().setFont(Font.createSystemFont(
                Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        add(BorderLayout.CENTER, source);
        getToolbar().setBackCommand("", e -> back.showBack());
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
