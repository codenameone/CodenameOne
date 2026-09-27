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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

/// A dialog is painted over the form it was opened from: the area around the dialog shows that
/// form, tinted.
///
/// Issue #5910 reported the area around a dialog as blank in a browser. The cause was in the
/// JavaScript port -- a theme refresh on show dropped the backdrop painter -- and
/// scripts/test-javascript-composited-rendering.mjs guards that. This guards the framework half
/// the port relies on, on the desktop path and under the legacy theme the report ran.
class DialogBackdropTest extends UITestBase {

    @FormTest
    void theFormShowsAroundADesktopDialog() {
        implementation.setDesktop(true);
        try {
            assertBackdropPainted();
        } finally {
            implementation.setDesktop(false);
        }
    }

    @FormTest
    void theFormShowsAroundADesktopDialogUnderTheLegacyTheme() throws Exception {
        java.io.File res = new java.io.File("../../Themes/iOS7Theme.res");
        com.codename1.ui.util.Resources r = com.codename1.ui.util.Resources.open(new java.io.FileInputStream(res));
        com.codename1.ui.plaf.UIManager.getInstance().setThemeProps(r.getTheme(r.getThemeResourceNames()[0]));
        implementation.setDesktop(true);
        try {
            assertBackdropPainted();
        } finally {
            implementation.setDesktop(false);
        }
    }

    @FormTest
    void theFormShowsAroundAMobileDialog() {
        assertBackdropPainted();
    }

    private void assertBackdropPainted() {
        Form f = new Form("Backdrop", BoxLayout.y());
        f.add(new Label("underneath"));
        f.show();
        DisplayTest.flushEdt();

        Dialog d = new Dialog("Hello", BoxLayout.y());
        d.add(new Label("Welcome"));
        d.showPacked(BorderLayout.CENTER, false);
        DisplayTest.flushEdt();

        Image img = Image.createImage(d.getWidth(), d.getHeight(), 0);
        d.paintComponent(img.getGraphics(), true);
        int[] rgb = img.getRGB();
        int corner = rgb[d.getWidth() * (d.getHeight() - 2) + 2];
        assertNotEquals(0, corner >>> 24, "the corner outside the dialog is transparent: the form "
                + "it was opened from was not painted under it");
        d.dispose();
    }
}
