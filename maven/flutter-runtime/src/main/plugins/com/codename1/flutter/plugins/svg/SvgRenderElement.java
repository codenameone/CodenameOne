/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.svg;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;

/** Hosts a Label showing an {@link SvgPicture}; none headless, an empty Label where SVG is unsupported. */
public class SvgRenderElement extends RenderElement {
    public SvgRenderElement(SvgPicture widget) {
        super(widget);
    }

    @Override
    protected Component createComponent() {
        if (!com.codename1.ui.Display.isInitialized()) {
            return null;
        }
        SvgPicture p = (SvgPicture) widget();
        com.codename1.ui.Label l = new com.codename1.ui.Label();
        l.setUIID("Container");
        com.codename1.ui.Image img = p.image();
        if (img != null) {
            l.setIcon(img.scaled((int) com.codename1.flutter.rendering.Dp.px(p.preferredLayoutSize().width()),
                    (int) com.codename1.flutter.rendering.Dp.px(p.preferredLayoutSize().height())));
        }
        if (p.semanticsLabel != null) {
            l.setAccessibilityText(p.semanticsLabel);
        }
        return l;
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        Size want = ((SvgPicture) widget()).preferredLayoutSize();
        double w = constraints.hasBoundedWidth() ? constraints.maxWidth() : Dp.px(want.width());
        double h = constraints.hasBoundedHeight() ? constraints.maxHeight() : Dp.px(want.height());
        return constraints.constrain(new Size(w, h));
    }
}
