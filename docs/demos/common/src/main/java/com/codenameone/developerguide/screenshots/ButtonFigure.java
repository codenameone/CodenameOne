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

/// The buttons figure for the Components chapter.
///
/// Deliberately styled by nothing: every colour, border and font here comes
/// from the native theme the renderer installed, because the point of the
/// picture is what a button looks like on that platform. The chapter shows the
/// iOS and Android renders side by side, which is also what retires its old
/// claim that iOS draws no border on a button.
///
/// No `FontImage` icon appears here on purpose. A material glyph cannot be
/// byte-reproduced across platforms -- Java2D's antialiased edge coverage
/// differs even at an identical size and origin -- so a figure containing one
/// needs a tolerance sidecar, and this one does not have to.
public final class ButtonFigure implements GuideFigure {
    @Override
    public String id() {
        return "components-button";
    }

    @Override
    public Form build() {
        Form form = new Form("Buttons", BoxLayout.y());
        form.add(new Button("Press Me"));
        form.add(new Button("Another Button"));
        Button disabled = new Button("Disabled");
        disabled.setEnabled(false);
        form.add(disabled);
        return form;
    }
}
