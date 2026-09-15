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

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/// The tint a dialog paints over the form it covers, at the default colour.
class ComponentsDialogTintFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-dialog-tint";
    }

    @Override
    public Form build() {
        Form hi = new Form("Tint Dialog", new BoxLayout(BoxLayout.Y_AXIS));
        Button showDialog = new Button("Tint");
        hi.add(showDialog);
        hi.show();
        return hi;
    }

    /// The chapter shows the dialog from a button press, which no one is here to
    /// make, and through the static Dialog.show that does not return until it is
    /// dismissed. The same dialog is built and shown modelessly instead; what
    /// the figure is about -- the colour over the form behind it -- is identical.
    @Override
    public void afterShow(Form form) {
        tintedDialog("Tint").showModeless();
    }

    static Dialog tintedDialog(String title) {
        Dialog d = new Dialog(title);
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new SpanLabel("Is On", "DialogBody"));
        d.add(BorderLayout.SOUTH, new Button("OK"));
        return d;
    }
}
