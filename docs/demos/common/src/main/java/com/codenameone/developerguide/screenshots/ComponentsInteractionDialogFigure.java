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

import com.codename1.components.InteractionDialog;
import com.codename1.ui.Button;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/// An InteractionDialog floating against the right edge of the form.
class ComponentsInteractionDialogFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-interaction-dialog";
    }

    @Override
    public Form build() {
        Form hi = new Form("Interaction Dialog", new BoxLayout(BoxLayout.Y_AXIS));
        hi.add(new Label("The form stays interactive."));
        hi.show();
        return hi;
    }

    /// An InteractionDialog is non-blocking by design, so unlike every other
    /// dialog figure here this runs the chapter listing as it stands. It still
    /// belongs in afterShow: it measures the display and positions itself
    /// against it.
    @Override
    public void afterShow(Form form) {
        // tag::the-components-of-codename-one-java-015[]
        InteractionDialog dlg = new InteractionDialog("Hello");
        dlg.setLayout(new BorderLayout());
        dlg.add(BorderLayout.CENTER, new Label("Hello Dialog"));
        Button close = new Button("Close");
        close.addActionListener((ee) -> dlg.dispose());
        dlg.addComponent(BorderLayout.SOUTH, close);
        Dimension pre = dlg.getContentPane().getPreferredSize();
        int displayWidth = Display.getInstance().getDisplayWidth();
        int dialogWidth = Math.max(pre.getWidth() + pre.getWidth() / 6, displayWidth * 2 / 3);
        dlg.show(0, 0, displayWidth - dialogWidth, 0);
        // end::the-components-of-codename-one-java-015[]
    }
}
