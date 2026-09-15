/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
import com.codename1.ui.plaf.UIManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Whether a tint colour an application sets survives being shown.
class FormTintColorTest extends UITestBase {

    private static final int GREEN = 0x7700ff00;

    @FormTest
    void showKeepsAnExplicitTintColor() {
        Form hi = new Form("Tint");
        hi.setTintColor(GREEN);

        hi.show();

        assertEquals(GREEN, hi.getTintColor(),
                "show() replaced the tint colour the application asked for with the theme default");
    }

    @FormTest
    void aFormThatAsksForNothingStillGetsTheThemeTint() {
        Form untouched = new Form("Default");
        int fromTheme = untouched.getTintColor();

        untouched.show();

        assertEquals(fromTheme, untouched.getTintColor(), "the theme default should still apply");
    }

    private static void setThemeTint(int color) {
        UIManager.getInstance().getLookAndFeel().setDefaultFormTintColor(color);
    }

    /// ComboBox, the toolbar overflow, the floating action button submenu and
    /// GlassTutorial all save the form's tint, force their own, and put the old
    /// one back through the same setter an application uses. A form that went
    /// through that has not chosen anything and must still follow the theme.
    @FormTest
    void aFrameworkOverrideThatPutsTheTintBackIsNotAChoice() {
        int original = UIManager.getInstance().getLookAndFeel().getDefaultFormTintColor();
        try {
            setThemeTint(0x66112233);
            Form hi = new Form("Default");
            hi.show();

            int saved = hi.getTintColor();
            hi.setTintColor(0);
            hi.setTintColor(saved);

            setThemeTint(0x66445566);
            hi.show();

            assertEquals(0x66445566, hi.getTintColor(),
                    "a form nobody chose a tint for stopped following the theme");
        } finally {
            setThemeTint(original);
        }
    }

    @FormTest
    void anExplicitTintOutlastsAThemeChange() {
        int original = UIManager.getInstance().getLookAndFeel().getDefaultFormTintColor();
        try {
            setThemeTint(0x66112233);
            Form hi = new Form("Chosen");
            hi.setTintColor(GREEN);
            hi.show();

            setThemeTint(0x66445566);
            hi.show();

            assertEquals(GREEN, hi.getTintColor(), "the theme took back a tint the application chose");
        } finally {
            setThemeTint(original);
        }
    }
}
