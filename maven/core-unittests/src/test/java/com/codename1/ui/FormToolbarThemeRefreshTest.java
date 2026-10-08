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
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.plaf.UIManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// A theme refresh must leave a global-toolbar form wired to its toolbar.
class FormToolbarThemeRefreshTest extends UITestBase {

    /// Since Toolbar replaced the look and feel's menu bar, `Form#initLaf`
    /// swapped the toolbar's menu bar for a fresh look-and-feel one whenever a
    /// theme refresh ran it, and the back command went with the old one.
    @FormTest
    void theBackKeyStillRunsTheToolbarBackCommandAfterARefresh() {
        boolean wasGlobal = Toolbar.isGlobalToolbar();
        Toolbar.setGlobalToolbar(true);
        try {
            int[] fired = new int[1];
            Form f = showFormWithBack(fired);

            f.refreshTheme(false);

            assertStillWired(f, fired);
        } finally {
            Toolbar.setGlobalToolbar(wasGlobal);
        }
    }

    /// The same refresh, reached the way an ordinary application reaches it:
    /// the operating system switches to dark mode while a form is showing.
    @FormTest
    void aSystemDarkModeChangeKeepsTheBackCommand() {
        boolean wasGlobal = Toolbar.isGlobalToolbar();
        Boolean wasDark = Display.getInstance().isDarkMode();
        Toolbar.setGlobalToolbar(true);
        try {
            int[] fired = new int[1];
            Form f = showFormWithBack(fired);
            UIManager.getInstance().refreshNativeThemeSettings();

            Display.getInstance().setDarkMode(Boolean.valueOf(!Boolean.TRUE.equals(wasDark)));
            UIManager.getInstance().refreshNativeThemeSettings();

            assertStillWired(f, fired);
        } finally {
            Display.getInstance().setDarkMode(wasDark);
            UIManager.getInstance().refreshNativeThemeSettings();
            Toolbar.setGlobalToolbar(wasGlobal);
        }
    }

    private static Form showFormWithBack(final int[] fired) {
        Form f = new Form("Back");
        Command back = new Command("Back") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                fired[0]++;
            }
        };
        f.getToolbar().setBackCommand(back);
        f.show();
        // show() runs initLaf too when the theme sets no form transitions,
        // so the same replacement used to strike before any refresh.
        assertSame(back, f.getBackCommand(), "showing the form dropped the back command");
        return f;
    }

    private static void assertStillWired(Form f, int[] fired) {
        assertSame(f.getToolbar().getMenuBar(), f.getMenuBar(),
                "the refresh replaced the toolbar's menu bar");
        assertNotNull(f.getBackCommand(), "the refresh dropped the back command");
        f.keyPressed(MenuBar.backSK);
        f.keyReleased(MenuBar.backSK);
        assertEquals(1, fired[0], "the back key no longer runs the back command");
    }
}
