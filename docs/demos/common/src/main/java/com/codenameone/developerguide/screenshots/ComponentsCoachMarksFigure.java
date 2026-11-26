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

import com.codename1.components.CoachMarks;
import com.codename1.components.MultiButton;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

/// The first step of a CoachMarks tour: the dimmed form, the lit Search
/// button and the bubble with its step counter and buttons.
class ComponentsCoachMarksFigure implements GuideFigure {

    private Button search;
    private Button filter;
    private Container list;

    @Override
    public String id() {
        return "components-coach-marks";
    }

    @Override
    public Form build() {
        Form hi = new Form("Orders", new BorderLayout());
        search = new Button("Search");
        FontImage.setMaterialIcon(search, FontImage.MATERIAL_SEARCH);
        filter = new Button("Filter");
        list = new Container(BoxLayout.y());
        for (int iter = 1; iter <= 5; iter++) {
            list.add(new MultiButton("Order #10" + iter));
        }
        hi.add(BorderLayout.NORTH, FlowLayout.encloseIn(search, filter));
        hi.add(BorderLayout.CENTER, list);
        hi.show();
        return hi;
    }

    /// The tour lights components by their on-screen bounds, so it starts
    /// once the form is laid out.
    @Override
    public void afterShow(Form form) {
        new CoachMarks(form)
                .add(search, "Search", "Find any order by number or name.")
                .add(filter, "Filters", "Narrow the list to what is still open.")
                .add(list, null, "Tap an order to see where it is.")
                .show();
        GuideFigure.settleAnimations(300);
    }

    @Override
    public boolean fillsViewport() {
        return true;
    }
}
