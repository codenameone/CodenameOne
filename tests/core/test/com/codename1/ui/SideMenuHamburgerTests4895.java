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

import com.codename1.components.SpanLabel;
import com.codename1.testing.AbstractTest;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/// Regression coverage for https://github.com/codenameone/CodenameOne/issues/4895
///
/// The reporter saw the hamburger button vanish on the second Form in the JS
/// port after navigating from the side menu. Resizing the browser brought it
/// back, which pointed at a stale layout: openButton attached but measured at
/// width 0 on the destination Form's first paint. SideMenuBar.addOpenButton
/// now invalidates the openButton's preferred size and queues a revalidate
/// after attaching it; this test pins the invariants the fix relies on.
public class SideMenuHamburgerTests4895 extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        boolean priorGlobal = Toolbar.isGlobalToolbar();
        boolean priorOnTop = Toolbar.isOnTopSideMenu();
        try {
            Toolbar.setGlobalToolbar(true);
            Toolbar.setOnTopSideMenu(false);

            Form home = buildSideMenuForm("Home4895", "first");
            home.show();
            waitForFormName("Home4895");

            assertHamburgerAttached(home, "first form");

            Form about = buildSideMenuForm("About4895", "second");
            about.show();
            waitForFormName("About4895");

            assertHamburgerAttached(about, "second form (the one that regresses in JS)");

            return true;
        } finally {
            Toolbar.setGlobalToolbar(priorGlobal);
            Toolbar.setOnTopSideMenu(priorOnTop);
        }
    }

    private Form buildSideMenuForm(String name, String body) {
        Form f = new Form(name, new BorderLayout());
        f.setName(name);
        Toolbar tb = f.getToolbar();
        tb.addMaterialCommandToSideMenu("Home", FontImage.MATERIAL_HOME, e -> {});
        tb.addMaterialCommandToSideMenu("About", FontImage.MATERIAL_INFO, e -> {});
        f.add(BorderLayout.CENTER, BoxLayout.encloseY(new SpanLabel(body)));
        return f;
    }

    /// The Toolbar IS the title area (ToolbarSideMenu.getTitleAreaContainer
    /// returns Toolbar.this), and addCommandToSideMenu must leave the
    /// openButton sitting in the WEST slot with a non-zero measured width so
    /// the hamburger paints on the very first frame after show().
    private void assertHamburgerAttached(Form f, String label) {
        Toolbar tb = f.getToolbar();
        assertTrue(tb != null, label + ": form has no Toolbar");
        assertTrue(tb.getLayout() instanceof BorderLayout,
                label + ": Toolbar layout is not a BorderLayout (was "
                        + (tb.getLayout() == null ? "null" : tb.getLayout().getClass().getName()) + ")");

        Component west = ((BorderLayout) tb.getLayout()).getWest();
        assertTrue(west != null, label + ": Toolbar WEST is null (no hamburger attached)");
        assertTrue(west instanceof Button,
                label + ": Toolbar WEST is " + west.getClass().getName() + ", expected a Button");
        assertTrue("MenuButton".equals(west.getUIID()),
                label + ": Toolbar WEST UIID is '" + west.getUIID() + "', expected 'MenuButton'");

        int prefW = west.getPreferredW();
        assertTrue(prefW > 0,
                label + ": hamburger preferredW is " + prefW
                        + " (issue #4895: button measured at 0 width is invisible / unclickable)");
    }
}
