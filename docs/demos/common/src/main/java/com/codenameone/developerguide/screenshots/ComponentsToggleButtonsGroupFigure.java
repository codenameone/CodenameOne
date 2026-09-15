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
import com.codename1.ui.ComponentGroup;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Form;
import com.codename1.ui.RadioButton;
import com.codename1.ui.layouts.BoxLayout;

/// Toggle buttons welded into one group by ComponentGroup.
class ComponentsToggleButtonsGroupFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-toggle-buttons-component-group";
    }

    /// The tagged region is what the chapter includes, so the listing beside the
    /// picture is the code that drew it.
    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-157[]
        Form hi = new Form("ComponentGroup", new BoxLayout(BoxLayout.Y_AXIS));
        CheckBox cb1 = CheckBox.createToggle("CheckBox 1");
        CheckBox cb2 = CheckBox.createToggle("CheckBox 2");
        CheckBox cb3 = CheckBox.createToggle("CheckBox 3");
        CheckBox cb4 = CheckBox.createToggle("CheckBox 4");
        ButtonGroup bg = new ButtonGroup();
        RadioButton rb1 = RadioButton.createToggle("Radio 1", bg);
        RadioButton rb2 = RadioButton.createToggle("Radio 2", bg);
        RadioButton rb3 = RadioButton.createToggle("Radio 3", bg);
        hi.add(ComponentGroup.enclose(cb1, cb2, cb3, cb4)).
                add(ComponentGroup.encloseHorizontal(rb1, rb2, rb3));
        hi.show();
        // end::the-components-of-codename-one-java-157[]
        return hi;
    }
}
