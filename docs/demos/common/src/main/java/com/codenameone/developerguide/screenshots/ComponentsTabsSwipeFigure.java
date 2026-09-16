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

import com.codename1.ui.ButtonGroup;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Tabs;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.DefaultLookAndFeel;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.components.SpanLabel;

/// Swipeable tabs with an iOS-style carousel of dots for the tab bar.
class ComponentsTabsSwipeFigure implements GuideFigure {

    private Tabs tabs;

    @Override
    public String id() {
        return "components-tabs-swipe1";
    }

    /// The tagged region is what the chapter includes, so the listing beside the
    /// picture is the code that drew it.
    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-081[]
        Form hi = new Form("Swipe Tabs", new LayeredLayout());
        Tabs t = new Tabs();
        t.hideTabs();

        Style s = UIManager.getInstance().getComponentStyle("Button");
        FontImage radioEmptyImage = FontImage.createMaterial(FontImage.MATERIAL_RADIO_BUTTON_UNCHECKED, s);
        FontImage radioFullImage = FontImage.createMaterial(FontImage.MATERIAL_RADIO_BUTTON_CHECKED, s);
        ((DefaultLookAndFeel) UIManager.getInstance().getLookAndFeel())
                .setRadioButtonImages(radioFullImage, radioEmptyImage, radioFullImage, radioEmptyImage);

        Container container1 = BoxLayout.encloseY(new Label("Swipe the tab to see more"),
                new Label("You can put anything here"));
        t.addTab("Tab1", container1);
        t.addTab("Tab2", new SpanLabel("Some text directly in the tab"));

        RadioButton firstTab = new RadioButton("");
        RadioButton secondTab = new RadioButton("");
        firstTab.setUIID("Container");
        secondTab.setUIID("Container");
        new ButtonGroup(firstTab, secondTab);
        firstTab.setSelected(true);
        Container tabsFlow = FlowLayout.encloseCenter(firstTab, secondTab);

        hi.add(t);
        hi.add(BorderLayout.south(tabsFlow));

        t.addSelectionListener((i1, i2) -> {
            switch (i2) {
                case 0:
                    if (!firstTab.isSelected()) {
                        firstTab.setSelected(true);
                    }
                    break;
                case 1:
                    if (!secondTab.isSelected()) {
                        secondTab.setSelected(true);
                    }
                    break;
                default:
                    break;
            }
        });
        // end::the-components-of-codename-one-java-081[]
        tabs = t;
        hi.show();
        return hi;
    }

    Tabs tabs() {
        return tabs;
    }

    @Override
    public boolean fillsViewport() {
        return true;
    }
}
