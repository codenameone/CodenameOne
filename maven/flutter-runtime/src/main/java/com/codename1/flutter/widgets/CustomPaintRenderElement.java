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

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.CustomPainter;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.GraphicsCanvas;
import com.codename1.flutter.rendering.Size;
import com.codename1.io.Log;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;

/**
 * Runs a {@link CustomPainter} onto the screen — the render element behind
 * {@link CustomPaint}.
 *
 * <p>The element owns a CN1 component whose {@code paint} drives
 * {@code painter.paint(canvas, size)} through a {@link GraphicsCanvas}. The
 * painter is handed a size in LOGICAL pixels and a canvas pre-scaled by the
 * device pixel ratio, so a painter written against Flutter's coordinate system
 * draws at the right physical size on any density.</p>
 *
 * <p>Both painters are supported: {@code painter} draws behind the child,
 * {@code foregroundPainter} over it. The child's own components are ordinary
 * siblings attached after the backdrop, so they paint on top of it.</p>
 */
public class CustomPaintRenderElement extends SingleChildRenderElement {

    public CustomPaintRenderElement(CustomPaint widget) {
        super(widget);
    }

    private CustomPaint paintWidget() {
        return (CustomPaint) widget();
    }

    @Override
    protected Widget childWidget() {
        return paintWidget().getChild();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        return new PainterSurface();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RenderElement child = renderChild();
        if (child != null) {
            Size cs = child.layout(constraints);
            setChildOffset(child, 0, 0);
            return constraints.constrain(cs);
        }
        // No child: Flutter uses CustomPaint.size (logical pixels), falling
        // back to the largest the constraints allow.
        Size preferred = paintWidget().getSize();
        if (preferred != null) {
            return constraints.constrain(new Size(
                    Dp.px(preferred.width()), Dp.px(preferred.height())));
        }
        return constraints.constrain(new Size(
                constraints.hasBoundedWidth() ? constraints.maxWidth() : 0,
                constraints.hasBoundedHeight() ? constraints.maxHeight() : 0));
    }

    /** The component that hands its Graphics to the painters. */
    private final class PainterSurface extends Container {

        PainterSurface() {
            setUIID("FlutterCustomPaint");
            getAllStyles().setPadding(0, 0, 0, 0);
            getAllStyles().setMargin(0, 0, 0, 0);
            getAllStyles().setBgTransparency(0);
        }

        @Override
        public void paint(Graphics g) {
            run(g, paintWidget().getPainter());
            super.paint(g);
            run(g, paintWidget().getForegroundPainter());
        }

        /**
         * Gives the painter back the ground an ancestor Transform took away.
         *
         * <p>Flutter does not clip a CustomPainter to the box it was handed --
         * an ancestor ClipRect does that -- so a painter may draw well outside
         * its own size. Codename One clips every component to its bounds, and
         * when an ancestor Transform has shifted the origin those bounds move
         * with it, so the part of the drawing the shift brings into view is cut
         * off instead. The 2D-transformations demo centres a board wider than
         * the screen by translating it, and lost a strip down the right-hand
         * side exactly as wide as the shift.</p>
         *
         * <p>The shift is recoverable: a graphics being painted through has
         * accumulated its ancestors' offsets, so without a transform its
         * translation plus this component's parent-relative position is its
         * absolute position. Whatever that identity is out by IS the transform.
         * Clip to the box this component occupies ON SCREEN rather than to the
         * one the shift moved it to.</p>
         */
        private void unclipFromAncestorTransform(Graphics g) {
            int[] box = CustomPaintRenderElement.onScreenClip(g.getTranslateX(), g.getTranslateY(),
                    getX(), getY(), getAbsoluteX(), getAbsoluteY(),
                    getWidth(), getHeight());
            if (box != null) {
                g.setClip(box[0], box[1], box[2], box[3]);
            }
        }

        private void run(Graphics g, CustomPainter painter) {
            if (painter == null) {
                return;
            }
            double dpr = Dp.scale();
            if (dpr <= 0) {
                dpr = 1;
            }
            int clipX = g.getClipX();
            int clipY = g.getClipY();
            int clipW = g.getClipWidth();
            int clipH = g.getClipHeight();
            int color = g.getColor();
            int alpha = g.getAlpha();
            try {
                unclipFromAncestorTransform(g);
                // the painter's box, in the logical pixels it expects
                Size logical = new Size(getWidth() / dpr, getHeight() / dpr);
                // The origin is this component's PARENT-RELATIVE position, because a Graphics
                // being painted through has already accumulated its ancestors' translation
                // (Container.paintChildren translates by getX()/getY() on the way down) - which
                // is why the whole of Codename One draws with getX(), not getAbsoluteX(). Using
                // the absolute position here added the ancestors' offset a second time and
                // pushed the drawing outside the bounds this component clips to, so the painter
                // ran and nothing appeared.
                painter.paint(new GraphicsCanvas(g, getX(), getY(), dpr), logical);
            } catch (Throwable t) {
                // One misbehaving painter must not take the whole frame down —
                // but it must be REPORTED. A painter that throws leaves the
                // screen looking merely empty, and a log line is invisible to
                // the sweep: the 2D-transformations demo drew no board at all
                // and every check said the route was fine.
                com.codename1.flutter.FlutterErrorReport.unimplemented(
                        painter.getClass().getName(),
                        "its paint() threw " + t + "; nothing was drawn");
            } finally {
                g.setClip(clipX, clipY, clipW, clipH);
                g.setColor(color);
                g.setAlpha(alpha);
            }
        }
    }

    /**
     * The component's ON-SCREEN box in the coordinates the graphics is
     * currently painting in, or null when no ancestor transform has moved
     * it and the clip already in force is the right one.
     *
     * <p>Without a transform, a graphics being painted through has
     * accumulated exactly the ancestors' offsets, so its translation plus
     * this component's parent-relative position is its absolute position.
     * Whatever that identity is out by IS the transform's shift.</p>
     */
    static int[] onScreenClip(int translateX, int translateY, int x, int y,
            int absoluteX, int absoluteY, int width, int height) {
        int extraX = translateX + x - absoluteX;
        int extraY = translateY + y - absoluteY;
        if (extraX == 0 && extraY == 0) {
            return null;
        }
        return new int[] {x - extraX, y - extraY, width, height};
    }
}
