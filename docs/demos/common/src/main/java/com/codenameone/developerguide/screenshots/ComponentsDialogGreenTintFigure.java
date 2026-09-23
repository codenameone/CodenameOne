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

import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;

/// The same dialog over a form that sets its own tint colour.
class ComponentsDialogGreenTintFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-dialog-green-tint";
    }

    @Override
    public Form build() {
        Form hi = new Form("Tint Dialog", new BoxLayout(BoxLayout.Y_AXIS));
        hi.setTintColor(0x7700ff00);
        hi.add(new Button("Tint"));
        hi.show();
        return hi;
    }

    /// See ComponentsDialogTintFigure: modeless, for the same reason.
    @Override
    public void afterShow(Form form) {
        ComponentsDialogTintFigure.tintedDialog("Tint").showModeless();
    }
}
