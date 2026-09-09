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
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.GeneralPath;

/**
 * Clips its subtree to an ellipse inscribed in its box — Flutter's
 * {@code ClipOval}, and what makes an avatar round.
 *
 * <p>It was a pass-through, so every circular portrait in the app rendered as
 * a square photograph sitting on top of a round background: the mail study's
 * sender avatars, the contact rows, the profile chips.</p>
 */
public class ClipOvalRenderElement extends ClipRectRenderElement {

    /// The circle-through-Béziers constant: the control-point offset, as a
    /// fraction of the radius, that makes a cubic segment match a quarter arc.
    private static final double KAPPA = 0.5522847498307933;

    private GeneralPath path;
    private int pathX = Integer.MIN_VALUE;
    private int pathY = Integer.MIN_VALUE;
    private int pathW = -1;
    private int pathH = -1;

    public ClipOvalRenderElement(Widget widget) {
        super(widget);
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Subtree paintChildren) {
        int w = pane.getWidth();
        int h = pane.getHeight();
        if (w <= 0 || h <= 0) {
            paintChildren.paint(g);
            return;
        }
        // An oval in a SQUARE box is a circle, and a circle is a rounded rectangle of
        // radius w/2 -- so the port's own rounded-image path draws it exactly. That
        // matters because on iOS the shape clip does nothing at all; see
        // EffectRenderElement.paintRoundClipped. Every ClipOval in the gallery is
        // square: an avatar is a photograph in a box as wide as it is tall.
        //
        // In an oblong box it is a true ellipse, which the rounded-image path cannot
        // express, so the shape clip is the only mechanism there. Passing radius 0 says
        // exactly that: use the clip or answer false.
        boolean circle = Math.abs(w - h) <= 1;
        float radius = circle ? Math.min(w, h) / 2f : 0f;
        if (paintRoundClipped(g, pane, paintChildren,
                pathFor(pane.getX(), pane.getY(), w, h), radius)) {
            return;
        }
        com.codename1.flutter.FlutterErrorReport.unimplemented("ClipOval",
                "this platform cannot clip to an ellipse, so the subtree paints square");
        paintChildren.paint(g);
    }

    /** The inscribed ellipse, rebuilt only when the box changes. */
    private GeneralPath pathFor(int x, int y, int w, int h) {
        // Reused rather than rebuilt while the box is unchanged; see
        // ClipRRectRenderElement for why that matters. The origin is part of
        // the key because the path is in parent-relative coordinates.
        if (path != null && pathX == x && pathY == y && pathW == w && pathH == h) {
            return path;
        }
        path = new GeneralPath();
        pathX = x;
        pathY = y;
        pathW = w;
        pathH = h;
        float rx = w / 2f;
        float ry = h / 2f;
        float cx = x + rx;
        float cy = y + ry;
        float ox = (float) (rx * KAPPA);
        float oy = (float) (ry * KAPPA);
        path.moveTo(cx - rx, cy);
        path.curveTo(cx - rx, cy - oy, cx - ox, cy - ry, cx, cy - ry);
        path.curveTo(cx + ox, cy - ry, cx + rx, cy - oy, cx + rx, cy);
        path.curveTo(cx + rx, cy + oy, cx + ox, cy + ry, cx, cy + ry);
        path.curveTo(cx - ox, cy + ry, cx - rx, cy + oy, cx - rx, cy);
        path.closePath();
        return path;
    }
}
