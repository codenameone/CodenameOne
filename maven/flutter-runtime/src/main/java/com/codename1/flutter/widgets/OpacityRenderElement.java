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
import com.codename1.ui.Graphics;

/**
 * Paints {@link Opacity}'s subtree at its opacity, as one layer: overlapping children fade
 * together, the way Flutter's Opacity behaves. See {@link #paintWithEffect}.
 */
public class OpacityRenderElement extends EffectRenderElement {

    public OpacityRenderElement(Opacity widget) {
        super(widget);
    }

    private Opacity opacity() {
        return (Opacity) widget();
    }

    @Override
    protected Widget effectChild() {
        return opacity().getChild();
    }

    /// How many times this effect rendered its subtree offscreen. For tests.
    private int layersRendered;
    private com.codename1.ui.Image lastLayer;

    int layersRendered() {
        return layersRendered;
    }

    com.codename1.ui.Image lastLayer() {
        return lastLayer;
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Subtree paintChildren) {
        double o = opacity().getOpacity();
        if (o >= 1.0) {
            paintChildren.paint(g);
            return;
        }
        if (o <= 0.0) {
            return;   // fully transparent: painting anything would be wrong
        }
        int previous = g.getAlpha();
        // Compose with the alpha already in effect, so nested Opacity multiplies.
        int alpha = (int) Math.round(previous * o);
        if (drawsOnceThroughAlpha(pane)) {
            g.setAlpha(alpha);
            try {
                paintChildren.paint(g);
            } finally {
                g.setAlpha(previous);
            }
            return;
        }
        // Flutter's saveLayer: render the subtree opaque, then blend the result ONCE.
        // Setting the alpha and letting each component draw through it blends wherever
        // two draws overlap twice (a 0.5 fade over two stacked boxes came out 0.75),
        // and a background fill ignores it entirely -- Codename One paints a style's
        // background with its OWN alpha (fillRect(.., bgTransparency) sets it, it does
        // not multiply), so a faded ColoredBox did not fade at all.
        //
        // Only the part of the pane inside the clip is rendered. The buffer is kept
        // across frames while that size holds, so a FadeTransition repaints into the
        // same image every frame instead of allocating one.
        int px = pane.getX();
        int py = pane.getY();
        int vx = Math.max(px, g.getClipX());
        int vy = Math.max(py, g.getClipY());
        int vr = Math.min(px + pane.getWidth(), g.getClipX() + g.getClipWidth());
        int vb = Math.min(py + pane.getHeight(), g.getClipY() + g.getClipHeight());
        com.codename1.ui.Image rendered = layer(pane, paintChildren, vx - px, vy - py, vr - vx, vb - vy);
        if (rendered == null) {
            return;   // nothing of the pane is visible
        }
        layersRendered++;
        lastLayer = rendered;
        g.setAlpha(alpha);
        try {
            g.drawImage(rendered, vx, vy);
        } finally {
            g.setAlpha(previous);
        }
    }

    /// True when the subtree is a single component that is known to paint ONE draw,
    /// or several that never overlap, all through the current alpha -- so applying the
    /// alpha directly is exactly what a layer would produce, without the offscreen.
    ///
    /// That is a lone Label (Text, Icon, Image all render as one) with nothing behind
    /// its content: glyphs of one run do not overlap each other, and an icon is one
    /// image. Anything else takes the layer, including a lone box: its fill is a style
    /// background, which Codename One paints at its own alpha rather than through the
    /// current one. A border, an elevation shadow, a text decoration or a style opacity
    /// each add a draw (or reset the alpha), so any of them disqualifies the shortcut.
    static boolean drawsOnceThroughAlpha(Container pane) {
        if (pane.getComponentCount() != 1) {
            return false;
        }
        com.codename1.ui.Component c = pane.getComponentAt(0);
        if (!(c instanceof com.codename1.ui.Label)) {
            return false;
        }
        com.codename1.ui.plaf.Style s = c.getStyle();
        return s.getBgTransparency() == 0
                && s.getBgImage() == null
                && (s.getBorder() == null || s.getBorder().isEmptyBorder())
                && s.getElevation() == 0
                && s.getTextDecoration() == 0
                && (s.getOpacity() & 0xff) == 0xff;
    }
}
