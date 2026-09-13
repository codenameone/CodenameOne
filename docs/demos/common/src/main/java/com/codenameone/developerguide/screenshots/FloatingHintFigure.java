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

import com.codename1.components.FloatingHint;
import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;

/// The FloatingHint component beside the sample that builds it.
///
/// The sample creates both fields empty, and the hint animates up on focus
/// gained, so the field the renderer focuses shows the floated state and the
/// other shows the hint still inside it. That is both states of the component
/// in one still, which is why focus is left where the renderer puts it rather
/// than moved or cleared.
class FloatingHintFigure implements GuideFigure {
    @Override
    public String id() {
        return "components-floatinghint";
    }

    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-137[]
        Form hi = new Form("Floating Hint", BoxLayout.y());
        TextField first = new TextField("", "First Field");
        TextField second = new TextField("", "Second Field");
        hi.add(new FloatingHint(first)).
                add(new FloatingHint(second)).
                add(new Button("Go"));
        hi.show();
        // end::the-components-of-codename-one-java-137[]
        hi.setFocused(null);
        return hi;
    }
}
