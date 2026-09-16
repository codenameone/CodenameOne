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

import com.codename1.ui.Component;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/// A search field standing in for the toolbar title.
class ComponentsToolbarSearchFigure implements GuideFigure {

    private TextField searchField;

    @Override
    public String id() {
        return "components-toolbar-search";
    }

    /// The tagged region is what the chapter includes, so the listing beside the
    /// picture is the code that drew it.
    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-090[]
        Toolbar.setGlobalToolbar(true);
        Style s = UIManager.getInstance().getComponentStyle("Title");

        Form hi = new Form("Toolbar", new BoxLayout(BoxLayout.Y_AXIS));
        TextField searchField = new TextField("", "Toolbar Search"); // <1>
        searchField.getHintLabel().setUIID("Title");
        searchField.setUIID("Title");
        searchField.getAllStyles().setAlignment(Component.LEFT);
        hi.getToolbar().setTitleComponent(searchField);
        FontImage searchIcon = FontImage.createMaterial(FontImage.MATERIAL_SEARCH, s);
        searchField.addDataChangeListener((i1, i2) -> { // <2>
            String t = searchField.getText();
            if (t.length() < 1) {
                for (Component cmp : hi.getContentPane()) {
                    cmp.setHidden(false);
                    cmp.setVisible(true);
                }
            } else {
                String needle = t.toLowerCase();
                for (Component cmp : hi.getContentPane()) {
                    String val;
                    if (cmp instanceof Label) {
                        val = ((Label) cmp).getText();
                    } else {
                        if (cmp instanceof TextArea) {
                            val = ((TextArea) cmp).getText();
                        } else {
                            val = (String) cmp.getPropertyValue("text");
                        }
                    }
                    boolean show = val != null && val.toLowerCase().indexOf(needle) > -1;
                    cmp.setHidden(!show);
                    cmp.setVisible(show); // <3>
                }
            }
            hi.getContentPane().animateLayout(250);
        });
        hi.getToolbar().addCommandToRightBar("", searchIcon, (e) -> { // <4>
            searchField.startEditingAsync();
        });

        hi.add("A Game of Thrones").
                add("A Clash Of Kings").
                add("A Storm Of Swords").
                add("A Feast For Crows").
                add("A Dance With Dragons").
                add("The Winds of Winter").
                add("A Dream of Spring");
        hi.show();
        // end::the-components-of-codename-one-java-090[]
        this.searchField = searchField;
        return hi;
    }

    TextField searchField() {
        return searchField;
    }
}
