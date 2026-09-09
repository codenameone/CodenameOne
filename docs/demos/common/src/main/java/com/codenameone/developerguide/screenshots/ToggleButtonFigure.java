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
import com.codename1.ui.CheckBox;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.RadioButton;
import com.codename1.ui.layouts.BoxLayout;

/// The toggle buttons the Components chapter shows beside its `createToggle`
/// sample.
///
/// Two of the seven are selected on purpose. `setToggle(true)` rewrites the
/// UIID to `ToggleButton`, and the whole point of the figure is the difference
/// between a selected and an unselected one, which the theme draws as a change
/// of fill rather than of label colour alone. A figure with nothing selected
/// would show the shape and hide the state.
class ToggleButtonFigure implements GuideFigure {
    @Override
    public String id() {
        return "components-toggle-buttons";
    }

    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-156[]
        Form hi = new Form("RadioButton", new BoxLayout(BoxLayout.Y_AXIS));
        Image icon = FontImage.createMaterial(FontImage.MATERIAL_INFO, "Label", 3.0f);
        CheckBox cb1 = CheckBox.createToggle("CheckBox No Icon");
        cb1.setSelected(true);
        CheckBox cb2 = CheckBox.createToggle("CheckBox With Icon", icon);
        CheckBox cb3 = CheckBox.createToggle("CheckBox Opposite True", icon);
        CheckBox cb4 = CheckBox.createToggle("CheckBox Opposite False", icon);
        cb3.setOppositeSide(true);
        cb4.setOppositeSide(false);
        ButtonGroup bg = new ButtonGroup();
        RadioButton rb1 = RadioButton.createToggle("Radio 1", bg);
        RadioButton rb2 = RadioButton.createToggle("Radio 2", bg);
        RadioButton rb3 = RadioButton.createToggle("Radio 3", icon, bg);
        rb2.setSelected(true);
        hi.add(cb1).add(cb2).add(cb3).add(cb4).add(rb1).add(rb2).add(rb3);
        hi.show();
        // end::the-components-of-codename-one-java-156[]
        return hi;
    }
}
