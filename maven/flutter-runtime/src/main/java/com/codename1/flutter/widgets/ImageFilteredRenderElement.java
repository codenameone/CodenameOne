/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.widgets;

import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.Dp;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

/** Renders an {@link ImageFiltered} subtree offscreen and blurs it. */
public class ImageFilteredRenderElement extends EffectRenderElement {
    public ImageFilteredRenderElement(Widget widget) {
        super(widget);
    }

    private ImageFiltered w() {
        return (ImageFiltered) widget();
    }

    @Override
    protected Widget effectChild() {
        return w().getChild();
    }

    private static double dpr() {
        double d = Dp.scale();
        return d <= 0 ? 1 : d;
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Subtree paintChildren) {
        Object f = w().imageFilter;
        boolean on = w().enabled == null || w().enabled.booleanValue();
        if (!on || !(f instanceof com.codename1.flutter.ImageFilter)) {
            paintChildren.paint(g);
            return;
        }
        float r = ((com.codename1.flutter.ImageFilter) f).deviceRadius(dpr());
        Image layer = r > 0 && Display.getInstance().isGaussianBlurSupported()
                ? layer(pane, paintChildren) : null;
        if (layer == null) {
            paintChildren.paint(g);
            return;
        }
        g.drawImage(Display.getInstance().gaussianBlurImage(layer, r), pane.getX(), pane.getY());
    }
}
