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
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/// A dialog positioned by its distance from the four edges of the screen.
class ComponentsDialogBottomHalfFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-dialog-modal-bottom-half";
    }

    @Override
    public Form build() {
        Form hi = new Form("Dialog", BoxLayout.y());
        hi.add(new SpanLabel("The form underneath, tinted by the dialog.", "DialogBody"));
        hi.show();
        return hi;
    }

    /// Shown here rather than in build() for the reason given in
    /// ComponentsDialogModalSouthFigure, which also explains why the listing in
    /// the chapter is repeated rather than included: this show has to be
    /// modeless or it never returns to the thread taking the photograph.
    @Override
    public void afterShow(Form form) {
        Dialog d = new Dialog("Title");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new SpanLabel("Dialog Body", "DialogBody"));
        d.show(form.getHeight() / 2, 0, 0, 0, true, false);
    }
}
