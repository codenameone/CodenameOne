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

/** Blurs the form's pixels under a {@link BackdropFilter}, then paints its child. */
public class BackdropFilterRenderElement extends EffectRenderElement {
    public BackdropFilterRenderElement(Widget widget) {
        super(widget);
    }

    private BackdropFilter w() {
        return (BackdropFilter) widget();
    }

    @Override
    protected Widget effectChild() {
        return w().getChild();
    }

    private static double dpr() {
        double d = Dp.scale();
        return d <= 0 ? 1 : d;
    }

    /// Set while this element repaints the form to capture its backdrop: the capture
    /// must not contain this element's own subtree.
    private boolean capturing;

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Subtree paintChildren) {
        if (capturing) {
            return;
        }
        Object f = w().filter;
        boolean on = w().enabled == null || w().enabled.booleanValue();
        com.codename1.ui.Form form = pane.getComponentForm();
        int pw = pane.getWidth();
        int ph = pane.getHeight();
        if (on && f instanceof com.codename1.flutter.ImageFilter && form != null && pw > 0 && ph > 0
                && Display.getInstance().isGaussianBlurSupported()) {
            float r = ((com.codename1.flutter.ImageFilter) f).deviceRadius(dpr());
            if (r > 0) {
                Image backdrop = Image.createImage(pw, ph, 0);
                Graphics bg = backdrop.getGraphics();
                bg.translate(-pane.getAbsoluteX(), -pane.getAbsoluteY());
                capturing = true;
                try {
                    form.paintComponent(bg, true);
                } finally {
                    capturing = false;
                }
                g.drawImage(Display.getInstance().gaussianBlurImage(backdrop, r), pane.getX(), pane.getY());
            }
        }
        paintChildren.paint(g);
    }
}
