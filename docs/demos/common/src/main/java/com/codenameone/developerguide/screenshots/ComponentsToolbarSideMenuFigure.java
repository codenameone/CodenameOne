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

package com.codenameone.developerguide.screenshots;

import com.codename1.io.Log;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/// The toolbar of ComponentsToolbarFigure with its side menu open.
class ComponentsToolbarSideMenuFigure implements GuideFigure {

    private Toolbar toolbar;

    @Override
    public String id() {
        return "components-toolbar-sidemenu";
    }

    /// The same form the chapter builds a few lines earlier, which is what the
    /// side menu belongs to. The listing is included there rather than here.
    @Override
    public Form build() {
        Style s = UIManager.getInstance().getComponentStyle("TitleCommand");
        Image icon = FontImage.createMaterial(FontImage.MATERIAL_INFO, s);
        Toolbar.setGlobalToolbar(true);

        Form hi = new Form("Toolbar", new BoxLayout(BoxLayout.Y_AXIS));
        hi.getToolbar().addCommandToLeftBar("Left", icon, (e) -> Log.p("Clicked"));
        hi.getToolbar().addCommandToRightBar("Right", icon, (e) -> Log.p("Clicked"));
        hi.getToolbar().addCommandToOverflowMenu("Overflow", icon, (e) -> Log.p("Clicked"));
        hi.getToolbar().addCommandToSideMenu("Sidemenu", icon, (e) -> Log.p("Clicked"));
        toolbar = hi.getToolbar();
        hi.show();
        return hi;
    }

    /// A menu opens over a form that is already on screen, so this cannot happen
    /// in build().
    @Override
    public void afterShow(Form form) {
        toolbar.openSideMenu();
    }

    @Override
    public boolean fillsViewport() {
        return true;
    }
}
