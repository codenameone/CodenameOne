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
import com.codename1.flutter.ShapeBorder;
import com.codename1.flutter.TextDirection;
import com.codename1.flutter.rendering.GraphicsCanvas;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Border;

/**
 * A Codename One border that asks a Flutter {@link ShapeBorder} to draw itself.
 *
 * <p>Flutter borders are arbitrary outlines: Shrine's input fields chamfer their
 * corners, and approximating that with a rounded rectangle is what made them
 * read as square. Codename One draws arbitrary shapes perfectly well -- a
 * chamfer is a polygon -- and {@link GraphicsCanvas} already turns a Flutter
 * Path into one. This is the adapter that lets the border's OWN code run, so
 * any shape an application defines paints exactly as written rather than being
 * matched to the nearest thing the runtime knows how to draw.</p>
 */
public final class FlutterShapeBorderPainter extends Border {

    private final ShapeBorder border;

    public FlutterShapeBorderPainter(ShapeBorder border) {
        this.border = border;
    }

    @Override
    public void paintBorderBackground(Graphics g, Component c) {
        // The fill, if any, is the component's own style background; this
        // border only draws the outline.
    }

    @Override
    public void paint(Graphics g, Component c) {
        if (border == null) {
            return;
        }
        boolean priorAa = g.isAntiAliased();
        g.setAntiAliased(true);
        try {
            // Inset by half the stroke so the outline sits INSIDE the box, the
            // way Flutter centres a border side on the edge of its rect.
            // The Graphics carries the offset and the canvas carries only the
            // scale. GraphicsCanvas applies its origin BEFORE its scale, so an
            // origin in device pixels lands at a third of where it belongs at
            // 3x -- the outline was being drawn, correctly shaped, near the top
            // left of the screen instead of around the field.
            double dpr = com.codename1.flutter.rendering.Dp.scale();
            // An AFFINE transform, not g.translate: translate() shifts the
            // integer draw origin, while the shape this border draws goes
            // through the transform pipeline. Mixing the two put the outline a
            // component's offset away from the box it belongs to on any port
            // that honours the matrix. The matrix below is scale dpr with the
            // component's origin in the translation column -- the same shape as
            // the AffineScale sample in scripts/hellocodenameone.
            com.codename1.ui.Transform prior = null;
            boolean affine = g.isTransformSupported();
            if (affine) {
                prior = g.getTransform();
                com.codename1.ui.geom.AffineTransform at = new com.codename1.ui.geom.AffineTransform(
                        (float) dpr, 0f,
                        0f, (float) dpr,
                        (float) c.getX(), (float) c.getY());
                g.setTransform(at.toTransform());
            }
            try {
                // The canvas adds no scale of its own now: the matrix carries it.
                GraphicsCanvas canvas = new GraphicsCanvas(g, 0, 0, affine ? 1 : dpr);
                double w = c.getWidth() / dpr;
                double h = c.getHeight() / dpr;
                if (!affine) {
                    // No transform support: fall back to drawing at absolute
                    // coordinates, which is what the canvas does unaided.
                    canvas = new GraphicsCanvas(g, (int) (c.getX() * dpr),
                            (int) (c.getY() * dpr), dpr);
                }
                border.paint(canvas, Rect.fromLTRB(0, 0, w, h), null, 0, 0, TextDirection.ltr);
            } finally {
                if (affine) {
                    g.setTransform(prior);
                }
            }
        } catch (Throwable cannotPaint) {
            com.codename1.flutter.FlutterErrorReport.record(cannotPaint);
        } finally {
            g.setAntiAliased(priorAa);
        }
    }
}
