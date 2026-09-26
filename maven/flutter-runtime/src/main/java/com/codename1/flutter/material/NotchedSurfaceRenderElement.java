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
package com.codename1.flutter.material;

import com.codename1.flutter.Rect;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.widgets.EffectRenderElement;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;

/**
 * Paints {@link NotchedSurface}: the bar's outline with a notch cut for the docked
 * button, then the subtree on top.
 *
 * <p>Filled with {@code fillShape}, which every port honours -- unlike a shape CLIP,
 * which does not confine an image or a gradient. The geometry is read at PAINT time
 * rather than at build time: the bar is built before the scaffold lays its button out,
 * so at build time there is nothing to read.</p>
 */
public class NotchedSurfaceRenderElement extends EffectRenderElement {

    public NotchedSurfaceRenderElement(Widget widget) {
        super(widget);
    }

    private NotchedSurface surface() {
        return (NotchedSurface) widget();
    }

    @Override
    protected Widget effectChild() {
        return surface().getChild();
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Subtree paintChildren) {
        com.codename1.flutter.Color fill = surface().getColor();
        NotchedShape notch = surface().getShape();
        int w = pane.getWidth();
        int h = pane.getHeight();
        if (fill == null || notch == null || w <= 0 || h <= 0) {
            paintChildren.paint(g);
            return;
        }
        double dpr = Dp.scale();
        if (dpr <= 0) {
            dpr = 1;
        }
        Rect host = Rect.fromLTWH(0, 0, w / dpr, h / dpr);
        Rect guest = guestRect(new Size(w / dpr, h / dpr), logical(
                ScaffoldRenderElement.fabSizeOf(this), dpr),
                ScaffoldRenderElement.fabLocationOf(this), surface().getNotchMargin());
        com.codename1.flutter.Path outline = guest == null ? null
                : notch.getOuterPath(host, guest);
        int color = g.getColor();
        int alpha = g.getAlpha();
        boolean aa = g.isAntiAliased();
        try {
            g.setAntiAliased(true);
            g.setColor(fill.rgb());
            g.setAlpha(fill.alpha());
            if (outline == null || !g.isShapeSupported()) {
                g.fillRect(pane.getX(), pane.getY(), w, h);
            } else {
                g.fillShape(toGeneralPath(outline, pane.getX(), pane.getY(), dpr));
            }
        } finally {
            g.setAntiAliased(aa);
            g.setColor(color);
            g.setAlpha(alpha);
        }
        paintChildren.paint(g);
    }

    /// A Flutter path in Codename One geometry, offset to this pane and scaled to device
    /// pixels.
    ///
    /// Built here rather than through GraphicsCanvas: a canvas draw from this position
    /// produced nothing at all, while {@code fillShape} -- which is what a Material
    /// surface uses from the same kind of element -- paints. Only the verbs a notched
    /// outline uses are handled; anything else closes the subpath so a partial outline
    /// never leaks into the fill.
    private static com.codename1.ui.geom.GeneralPath toGeneralPath(
            com.codename1.flutter.Path path, int ox, int oy, double dpr) {
        com.codename1.ui.geom.GeneralPath out = new com.codename1.ui.geom.GeneralPath();
        double cx = 0;
        double cy = 0;
        for (com.codename1.flutter.Path.Segment seg : path.segments()) {
            double[] v = seg.coords;
            if ("moveTo".equals(seg.verb)) {
                out.moveTo(mx(v[0], ox, dpr), mx(v[1], oy, dpr));
                cx = v[0];
                cy = v[1];
            } else if ("lineTo".equals(seg.verb)) {
                out.lineTo(mx(v[0], ox, dpr), mx(v[1], oy, dpr));
                cx = v[0];
                cy = v[1];
            } else if ("quadraticBezierTo".equals(seg.verb) || "conicTo".equals(seg.verb)) {
                out.quadTo(mx(v[0], ox, dpr), mx(v[1], oy, dpr),
                        mx(v[2], ox, dpr), mx(v[3], oy, dpr));
                cx = v[2];
                cy = v[3];
            } else if ("cubicTo".equals(seg.verb)) {
                out.curveTo(mx(v[0], ox, dpr), mx(v[1], oy, dpr),
                        mx(v[2], ox, dpr), mx(v[3], oy, dpr),
                        mx(v[4], ox, dpr), mx(v[5], oy, dpr));
                cx = v[4];
                cy = v[5];
            } else if ("arcToPoint".equals(seg.verb)) {
                // The notch's floor. A chord here is what made the dimple curve down, cut
                // straight across and come back up -- a bump between two curves.
                double[] arc = com.codename1.flutter.rendering.GraphicsCanvas.arcToPoint(
                        cx, cy, v[0], v[1], v[2], v[5] != 0, v[6] != 0,
                        com.codename1.flutter.rendering.GraphicsCanvas.ARC_SEGMENTS);
                if (arc == null) {
                    out.lineTo(mx(v[0], ox, dpr), mx(v[1], oy, dpr));
                } else {
                    for (int i = 0; i < arc.length; i += 2) {
                        out.lineTo(mx(arc[i], ox, dpr), mx(arc[i + 1], oy, dpr));
                    }
                }
                cx = v[0];
                cy = v[1];
            } else if ("close".equals(seg.verb)) {
                out.closePath();
            }
        }
        return out;
    }

    private static float mx(double lp, int origin, double dpr) {
        return (float) (origin + lp * dpr);
    }

    private static Size logical(Size px, double dpr) {
        return px == null ? null : new Size(px.width() / dpr, px.height() / dpr);
    }

    /**
     * The docked button's box in the BAR's own coordinates, or null when nothing is
     * docked. A docked button straddles the bar's TOP edge, which is what puts the notch
     * there rather than inside the bar.
     */
    static Rect guestRect(Size bar, Size fab, FloatingActionButtonLocation where,
            double notchMargin) {
        if (bar == null || fab == null || fab.width() <= 0 || fab.height() <= 0) {
            return null;
        }
        double w = fab.width() + notchMargin * 2;
        double h = fab.height() + notchMargin * 2;
        double cx;
        if (where == FloatingActionButtonLocation.endDocked
                || where == FloatingActionButtonLocation.endFloat) {
            cx = bar.width() - 16 - fab.width() / 2;
        } else if (where == FloatingActionButtonLocation.startDocked
                || where == FloatingActionButtonLocation.startFloat) {
            cx = 16 + fab.width() / 2;
        } else {
            cx = bar.width() / 2;
        }
        return Rect.fromLTWH(cx - w / 2, -h / 2, w, h);
    }
}
