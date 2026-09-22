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

import com.codename1.flutter.BorderRadius;
import com.codename1.flutter.Radius;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.Dp;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.GeneralPath;

/**
 * Clips its subtree to a ROUNDED rectangle — Flutter's {@code ClipRRect}.
 *
 * <p>It used to be a pass-through, so every corner the design rounds came out
 * square: the gallery frames each demo in a card with a 10dp top radius, the
 * mail study clips its avatars, and the study cards on the home screen are
 * rounded. None of it appeared.</p>
 *
 * <p>The nested pane from {@link EffectRenderElement} already confines the
 * subtree to a rectangle; the corners need a shaped clip on top, which is only
 * available where the port supports one. Where it is not, the rectangular pane
 * clip stands — square corners, but never content spilling out.</p>
 *
 * <p>The path is rebuilt only when the box or the radii change. Building a
 * GeneralPath per paint is how an earlier version of this runtime put the event
 * thread inside the garbage collector for the duration of every frame.</p>
 */
public class ClipRRectRenderElement extends ClipRectRenderElement {

    private GeneralPath path;
    private int pathX = Integer.MIN_VALUE;
    private int pathY = Integer.MIN_VALUE;
    private int pathW = -1;
    private int pathH = -1;
    private double pathTl;
    private double pathTr;
    private double pathBr;
    private double pathBl;

    public ClipRRectRenderElement(Widget widget) {
        super(widget);
    }

    private BorderRadius radius() {
        Widget w = widget();
        if (!(w instanceof ClipRRect)) {
            return null;
        }
        Object r = ((ClipRRect) w).getBorderRadius();
        return r instanceof BorderRadius ? (BorderRadius) r : null;
    }

    private static double px(Radius r) {
        return r == null ? 0 : Dp.px(r.x());
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Subtree paintChildren) {
        BorderRadius radius = radius();
        int w = pane.getWidth();
        int h = pane.getHeight();
        if (radius == null || w <= 0 || h <= 0) {
            paintChildren.paint(g);
            return;
        }
        double tl = px(radius.topLeft());
        double tr = px(radius.topRight());
        double br = px(radius.bottomRight());
        double bl = px(radius.bottomLeft());
        if (tl <= 0 && tr <= 0 && br <= 0 && bl <= 0) {
            paintChildren.paint(g);
            return;
        }
        // PARENT-RELATIVE, not absolute: a Graphics being painted through has
        // already accumulated its ancestors' translation, which is why the whole
        // of Codename One draws with getX(). Clipping with the absolute position
        // added that offset a second time, and the clip then landed somewhere
        // else entirely -- the subtree was still laid out, still had components,
        // and painted nothing.
        int ax = pane.getX();
        int ay = pane.getY();
        GeneralPath p = pathFor(ax, ay, w, h, tl, tr, br, bl);
        // The port's rounded-image path takes ONE radius, so it can draw this only when
        // the four agree. Where they do not, the shape clip is the only mechanism -- and
        // on iOS that does nothing at all, so say so rather than drawing square in
        // silence. See EffectRenderElement.paintRoundClipped.
        if (!shapeClipSupported(g)) {
            // No shaped clip at all on this port: paint the subtree plainly and
            // still finish the corners. The wedge fill needs fillShape, not a
            // shaped CLIP, so it works where setClip(Shape) does not.
            paintChildren.paint(g);
            paintCornerWedges(g, pane, ax, ay, w, h, tl, tr, br, bl);
            return;
        }
        if (paintShapeClipped(g, pane, paintChildren, p)) {
            // The clip holds for the outermost paint and degrades to a rectangle
            // once a child component paints (Component.paintComponent restores
            // it from four ints, deliberately). So the corners are finished HERE
            // instead: the four wedges between the box and its rounded outline
            // are painted over in the colour behind the box. That is one draw we
            // own completely -- clip set, drawn, restored -- and it needs neither
            // a shaped clip that survives the subtree nor a mutable-image detour.
            paintCornerWedges(g, pane, ax, ay, w, h, tl, tr, br, bl);
            return;
        }
        paintChildren.paint(g);
        paintCornerWedges(g, pane, ax, ay, w, h, tl, tr, br, bl);
    }

    /// The wedge path (box minus rounded box), cached against the geometry that
    /// produced it -- it only changes when the box or the radii do.
    private GeneralPath wedges;
    private String wedgeKey;

    /**
     * Fills the four corner wedges with the colour behind this box.
     *
     * <p>Exact when what sits behind the box is a flat colour, which is the
     * case this exists for -- a Material card on a page. When the backdrop is
     * not a flat opaque colour there is nothing safe to paint, so the corners
     * are left as the subtree drew them rather than covered with a guess.</p>
     */
    private void paintCornerWedges(Graphics g, Container pane, int x, int y, int w, int h,
            double tl, double tr, double br, double bl) {
        int backdrop = backdropRgb(pane);
        if (backdrop < 0) {
            return;
        }
        String key = x + "," + y + "," + w + "," + h + ","
                + (int) tl + "," + (int) tr + "," + (int) br + "," + (int) bl;
        if (wedges == null || !key.equals(wedgeKey)) {
            GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
            path.moveTo(x, y);
            path.lineTo(x + w, y);
            path.lineTo(x + w, y + h);
            path.lineTo(x, y + h);
            path.closePath();
            // The rounded outline as a second subpath: under the even-odd rule
            // it becomes a hole, so a fill paints only the corners.
            path.append(pathFor(x, y, w, h, tl, tr, br, bl), false);
            wedges = path;
            wedgeKey = key;
        }
        int priorColor = g.getColor();
        boolean priorAa = g.isAntiAliased();
        g.setAntiAliased(true);
        g.setColor(backdrop);
        g.pushClip();
        try {
            g.clipRect(x, y, w, h);
            g.fillShape(wedges);
        } finally {
            g.popClip();
            g.setColor(priorColor);
            g.setAntiAliased(priorAa);
        }
    }

    /**
     * The opaque colour painted behind {@code c}, or -1 when the nearest
     * ancestor that paints anything is not a flat opaque fill.
     */
    private static int backdropRgb(Container c) {
        com.codename1.ui.Component cur = c == null ? null : c.getParent();
        while (cur != null) {
            com.codename1.ui.plaf.Style st = cur.getStyle();
            if (st != null && st.getBgTransparency() == (byte) 0xff
                    && st.getBgImage() == null) {
                return st.getBgColor();
            }
            cur = cur.getParent();
        }
        return -1;
    }

    private static boolean shapeClipSupported(Graphics g) {
        try {
            return Display.isInitialized() && g.isShapeClipSupported();
        } catch (Throwable t) {
            return false;
        }
    }

    private GeneralPath pathFor(int x, int y, int w, int h,
            double tl, double tr, double br, double bl) {
        // REUSED, not rebuilt. The path is geometry, and the geometry only
        // changes when the box does; rebuilding it on every paint puts a fresh
        // GeneralPath in front of the collector on every frame of every
        // animation that crosses a rounded card, which is how an earlier
        // version of this runtime parked the event thread inside the GC.
        // The origin participates because the path is in parent-relative
        // coordinates, which move when an ancestor scrolls.
        if (path != null && pathX == x && pathY == y && pathW == w && pathH == h
                && pathTl == tl && pathTr == tr && pathBr == br && pathBl == bl) {
            return path;
        }
        path = new GeneralPath();
        pathX = x;
        pathY = y;
        pathW = w;
        pathH = h;
        pathTl = tl;
        pathTr = tr;
        pathBr = br;
        pathBl = bl;
        // A radius can never exceed half the box, or opposite corners overlap and
        // the outline crosses itself.
        double max = Math.min(w, h) / 2.0;
        tl = Math.min(tl, max);
        tr = Math.min(tr, max);
        br = Math.min(br, max);
        bl = Math.min(bl, max);
        float left = x;
        float top = y;
        float right = x + w;
        float bottom = y + h;
        path.moveTo(left + tl, top);
        path.lineTo(right - tr, top);
        if (tr > 0) {
            path.quadTo(right, top, right, top + tr);
        }
        path.lineTo(right, bottom - br);
        if (br > 0) {
            path.quadTo(right, bottom, right - br, bottom);
        }
        path.lineTo(left + bl, bottom);
        if (bl > 0) {
            path.quadTo(left, bottom, left, bottom - bl);
        }
        path.lineTo(left, top + tl);
        if (tl > 0) {
            path.quadTo(left, top, left + tl, top);
        }
        path.closePath();
        return path;
    }
}
