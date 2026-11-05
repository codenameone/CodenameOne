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

/** Paints a {@link ClipPath} subtree through the clipper's path. */
public class ClipPathRenderElement extends EffectRenderElement {
    public ClipPathRenderElement(Widget widget) {
        super(widget);
    }

    private ClipPath w() {
        return (ClipPath) widget();
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
        int pw = pane.getWidth();
        int ph = pane.getHeight();
        Object clipper = w().clipper;
        if (pw > 0 && ph > 0 && clipper instanceof com.codename1.flutter.rendering.CustomClipper) {
            double d = dpr();
            Object path = ((com.codename1.flutter.rendering.CustomClipper<?>) clipper)
                    .getClip(new com.codename1.flutter.rendering.Size(pw / d, ph / d));
            if (path instanceof com.codename1.flutter.Path && paintShapeClipped(g, pane, paintChildren,
                    com.codename1.flutter.material.NotchedSurfaceRenderElement.toGeneralPath(
                            (com.codename1.flutter.Path) path, pane.getX(), pane.getY(), d))) {
                return;
            }
        }
        g.pushClip();
        try {
            g.clipRect(pane.getX(), pane.getY(), pw, ph);
            paintChildren.paint(g);
        } finally {
            g.popClip();
        }
    }
}
