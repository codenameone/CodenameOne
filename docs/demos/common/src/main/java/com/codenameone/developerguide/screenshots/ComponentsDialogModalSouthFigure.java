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

/// A packed dialog pinned to the bottom of the screen, over the form it covers.
class ComponentsDialogModalSouthFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-dialog-modal-south";
    }

    @Override
    public Form build() {
        Form hi = new Form("Dialog", BoxLayout.y());
        hi.add(new SpanLabel("The form underneath, tinted by the dialog.", "DialogBody"));
        hi.show();
        return hi;
    }

    /// Showing the dialog belongs here rather than in build(): a dialog is a
    /// form of its own and only becomes the current one once it is shown, which
    /// is what puts it and the tinted parent into the same picture.
    ///
    /// This repeats the listing above instead of including it, which every other
    /// figure avoids, and the reason is the boolean. A modal show does not
    /// return until the dialog is dismissed and nothing here is going to dismiss
    /// it, so running the listing verbatim parks the thread that still has to
    /// take the photograph. The listing has to keep saying true because the
    /// chapter's next paragraph tells the reader to flip that exact argument to
    /// get a modeless dialog. The two calls put the same dialog in the same
    /// place; only the waiting differs.
    @Override
    public void afterShow(Form form) {
        Dialog d = new Dialog("Title");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new SpanLabel("Dialog Body", "DialogBody"));
        d.showPacked(BorderLayout.SOUTH, false);
    }
}
