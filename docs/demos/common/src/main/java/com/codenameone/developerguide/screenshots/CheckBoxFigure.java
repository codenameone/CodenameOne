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

/// The check and radio figure for the Components chapter.
///
/// The tagged region below is what the chapter includes, so the listing beside
/// the picture is the code that drew it. That is the point of moving these out
/// of the compile-only snippet fixtures: a fixture and a screenshot are two
/// sources for one example and drift apart silently.
public final class CheckBoxFigure implements GuideFigure {
    @Override
    public String id() {
        return "components-radiobutton-checkbox";
    }

    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-155[]
        Form hi = new Form("CheckBox", new BoxLayout(BoxLayout.Y_AXIS));
        Image icon = FontImage.createMaterial(FontImage.MATERIAL_INFO, "Label", 3.0f);
        CheckBox cb1 = new CheckBox("CheckBox No Icon");
        cb1.setSelected(true);
        CheckBox cb2 = new CheckBox("CheckBox With Icon", icon);
        CheckBox cb3 = new CheckBox("CheckBox Opposite True", icon);
        CheckBox cb4 = new CheckBox("CheckBox Opposite False", icon);
        cb3.setOppositeSide(true);
        cb4.setOppositeSide(false);
        RadioButton rb1 = new RadioButton("Radio 1");
        RadioButton rb2 = new RadioButton("Radio 2");
        RadioButton rb3 = new RadioButton("Radio 3", icon);
        new ButtonGroup(rb1, rb2, rb3);
        rb2.setSelected(true);
        hi.add(cb1).add(cb2).add(cb3).add(cb4).add(rb1).add(rb2).add(rb3);
        hi.show();
        // end::the-components-of-codename-one-java-155[]
        return hi;
    }
}
