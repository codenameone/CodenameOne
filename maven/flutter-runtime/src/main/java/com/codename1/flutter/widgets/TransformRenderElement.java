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

import com.codename1.flutter.FlutterErrorReport;
import com.codename1.flutter.Offset;
import com.codename1.flutter.Widget;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;

/**
 * Paints {@link Transform}'s subtree through a scale, rotation and/or translation.
 *
 * <p>Like Flutter's Transform this is a PAINT effect: the child is laid out at its
 * natural size and only the painting is transformed, so a scaled card still occupies the
 * same slot in its parent.</p>
 *
 * <p>The transform is applied about the element's centre, which is Flutter's default
 * (Alignment.center) and what the gallery's carousel expects. An explicit
 * {@code origin}/{@code alignment} is not honoured yet and is reported rather than
 * silently ignored.</p>
 */
public class TransformRenderElement extends EffectRenderElement {

    public TransformRenderElement(Transform widget) {
        super(widget);
    }

    private Transform transform() {
        return (Transform) widget();
    }

    @Override
    protected Widget effectChild() {
        return transform().getChild();
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Subtree paintChildren) {
        double sx = transform().effectiveScaleX();
        double sy = transform().effectiveScaleY();
        Double angle = transform().effectiveAngle();
        Offset offset = transform().effectiveOffset();

        // A/B switch, flipped at runtime with
        // Display.setProperty("cn1.flutter.noTransform","true"). Transform.scale is the
        // main per-frame difference between the carousel (21-29fps on iOS) and a plain
        // list (60fps), and a layer per card per frame is only measurable on a device.
        // Same trick as cn1.flutter.noShapeClip.
        boolean suppressed = "true".equals(com.codename1.ui.Display.getInstance()
                .getProperty("cn1.flutter.noTransform", "false"));
        boolean scales = !suppressed && (sx != 1.0 || sy != 1.0);
        boolean rotates = !suppressed && angle != null && angle.doubleValue() != 0.0;
        boolean translates = offset != null && (offset.dx() != 0 || offset.dy() != 0);
        if (!scales && !rotates) {
            // A translation needs no matrix and no layer: shifting the origin is exact,
            // costs nothing, and works on every port.
            if (!translates) {
                paintChildren.paint(g);
                return;
            }
            int dx = (int) Math.round(com.codename1.flutter.rendering.Dp.px(offset.dx()));
            int dy = (int) Math.round(com.codename1.flutter.rendering.Dp.px(offset.dy()));
            g.translate(dx, dy);
            try {
                paintChildren.paint(g);
            } finally {
                g.translate(-dx, -dy);
            }
            return;
        }

        com.codename1.ui.Image rendered = layer(pane, paintChildren);
        if (rendered == null) {
            return;
        }

        int dx = 0;
        int dy = 0;
        if (translates) {
            dx = (int) Math.round(com.codename1.flutter.rendering.Dp.px(offset.dx()));
            dy = (int) Math.round(com.codename1.flutter.rendering.Dp.px(offset.dy()));
            g.translate(dx, dy);
        }
        try {
            if (rotates) {
                paintRotated(g, pane, rendered, sx, sy, angle.doubleValue());
            } else {
                paintScaled(g, pane, rendered, sx, sy);
            }
        } finally {
            if (translates) {
                g.translate(-dx, -dy);
            }
        }
    }

    /// Draws the layer scaled about the pane's centre - Flutter's default anchor, and
    /// what the gallery's carousel expects.
    ///
    /// A pure scale is a destination rectangle, so this needs no transform support and
    /// behaves identically on every port. Rounding the destination to whole pixels is
    /// what drawImage takes anyway.
    private static void paintScaled(Graphics g, Container pane, com.codename1.ui.Image layer,
            double sx, double sy) {
        int w = pane.getWidth();
        int h = pane.getHeight();
        int dw = (int) Math.round(w * sx);
        int dh = (int) Math.round(h * sy);
        if (dw <= 0 || dh <= 0) {
            // Scaled away to nothing: Flutter paints nothing here either.
            return;
        }
        g.drawImage(layer, pane.getX() + (w - dw) / 2, pane.getY() + (h - dh) / 2, dw, dh);
    }

    /// Draws the layer rotated (and possibly scaled) about the pane's centre.
    ///
    /// Rotation has no destination-rectangle form, so this is the one case that still
    /// needs a matrix. It is applied to a SINGLE drawImage rather than to a subtree
    /// walk, which is what makes it safe: Codename One's paint-time cull never runs
    /// under it, and the pivot is expressed in the pane's own parent coordinates -- the
    /// space the Graphics is in -- rather than in absolute screen coordinates.
    private static void paintRotated(Graphics g, Container pane, com.codename1.ui.Image layer,
            double sx, double sy, double angle) {
        if (!g.isTransformSupported()) {
            // Report rather than quietly dropping the visual: the layer still lands in
            // the right place, it simply is not turned.
            FlutterErrorReport.unimplemented("Transform",
                    "this platform has no transform support; rotation is ignored");
            paintScaled(g, pane, layer, sx, sy);
            return;
        }
        com.codename1.ui.Transform saved = g.getTransform();
        com.codename1.ui.Transform t = saved.copy();
        float cx = pane.getX() + pane.getWidth() / 2f;
        float cy = pane.getY() + pane.getHeight() / 2f;
        // Move the pivot to the centre, apply, move back - otherwise the subtree turns
        // about the screen origin instead of in place.
        t.translate(cx, cy);
        t.rotate((float) angle, 0, 0);
        if (sx != 1.0 || sy != 1.0) {
            t.scale((float) sx, (float) sy);
        }
        t.translate(-cx, -cy);
        g.setTransform(t);
        try {
            g.drawImage(layer, pane.getX(), pane.getY());
        } finally {
            g.setTransform(saved);
        }
    }
}
