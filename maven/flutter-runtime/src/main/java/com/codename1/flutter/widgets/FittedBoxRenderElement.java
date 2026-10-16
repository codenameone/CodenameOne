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

import com.codename1.flutter.Alignment;
import com.codename1.flutter.AlignmentDirectional;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;

/**
 * Scales a subtree to fit the box it is given - Flutter's {@code FittedBox}.
 *
 * <p>The part that matters is the LAYOUT, not the scaling: Flutter's
 * {@code RenderFittedBox} lays its child out against fully unbounded
 * constraints, so the child takes the size it would like to be, and only then
 * is that size scaled into the available box. A {@code Text} inside one
 * therefore measures on a single line and is shrunk, and never wraps.</p>
 *
 * <p>This used to be a pass-through, which handed the child the box's own
 * constraints. The gallery's card titles are
 * {@code FittedBox(fit: BoxFit.scaleDown)} around a 24sp headline, so
 * "Top 10 Cities to Visit in Tamil Nadu" wrapped onto a second line at full
 * size instead of staying on one line at 80% of it -- a card that was visibly
 * the wrong shape, and the route's worst difference against the reference.</p>
 *
 * <p>The scaling itself reuses the layer mechanism {@code Transform} already
 * uses: the subtree renders once into an offscreen image and that image is
 * drawn scaled. A raster scale is slightly softer than scaling glyph outlines,
 * which is what Flutter does, but it is the mechanism this runtime has on every
 * port, and being softer by a fraction of a pixel is not in the same class of
 * error as laying the text out at the wrong size.</p>
 */
public class FittedBoxRenderElement extends EffectRenderElement {

    private Size natural = Size.ZERO;
    private double scaleX = 1;
    private double scaleY = 1;
    private double offsetX;
    private double offsetY;

    public FittedBoxRenderElement(Widget widget) {
        super(widget);
    }

    private FittedBox box() {
        Widget w = widget();
        return w instanceof FittedBox ? (FittedBox) w : null;
    }

    @Override
    protected Widget effectChild() {
        FittedBox b = box();
        return b == null ? null : b.getChild();
    }

    /**
     * A FittedBox's own configuration decides its size, unlike the paint-only
     * effects this shares a base class with, so a rebuild has to re-run layout.
     */
    @Override
    protected boolean updateAffectsLayout() {
        return true;
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RenderElement c = effectRenderChild();
        scaleX = 1;
        scaleY = 1;
        offsetX = 0;
        offsetY = 0;
        if (c == null) {
            natural = constraints.smallest();
            return natural;
        }
        natural = c.layout(new BoxConstraints());
        c.position(0, 0);
        if (natural.width() <= 0 || natural.height() <= 0) {
            return constraints.constrain(natural);
        }
        Size self = constrainPreservingAspect(constraints, natural);
        double[] dest = ImageRenderElement.fittedSize(
                fit(), self.width(), self.height(), natural.width(), natural.height());
        scaleX = dest[0] / natural.width();
        scaleY = dest[1] / natural.height();
        Alignment a = alignment();
        offsetX = Alignment.along(a.x(), self.width(), dest[0]);
        offsetY = Alignment.along(a.y(), self.height(), dest[1]);
        return self;
    }

    /**
     * Gives the pane the child's NATURAL box rather than the scaled one.
     *
     * <p>Codename One clips every component to its own rectangle, and the
     * subtree below this one was laid out at its natural size, so a pane sized
     * to the scaled result would crop the child before it could be scaled --
     * the layer would capture a fragment. The parent still lays out against the
     * scaled size, which is what {@link #performLayout} returned.</p>
     */
    @Override
    public void position(int x, int y) {
        super.position(x, y);
        Component pane = component();
        if (pane != null) {
            pane.setWidth(Math.max(0, (int) Math.round(natural.width())));
            pane.setHeight(Math.max(0, (int) Math.round(natural.height())));
        }
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Subtree paintChildren) {
        if (scaleX == 1 && scaleY == 1 && offsetX == 0 && offsetY == 0) {
            paintChildren.paint(g);
            return;
        }
        int w = pane.getWidth();
        int h = pane.getHeight();
        int dw = (int) Math.round(w * scaleX);
        int dh = (int) Math.round(h * scaleY);
        if (dw <= 0 || dh <= 0) {
            return;
        }
        com.codename1.ui.Image rendered = layer(pane, paintChildren);
        if (rendered == null) {
            // No layer available (a headless pane): drawing the subtree at its
            // natural size overflows, but showing it is closer than showing
            // nothing, and it is what the pass-through did for years.
            paintChildren.paint(g);
            return;
        }
        g.drawImage(rendered,
                pane.getX() + (int) Math.round(offsetX),
                pane.getY() + (int) Math.round(offsetY),
                dw, dh);
    }

    // ------------------------------------------------------------------

    private com.codename1.flutter.BoxFit fit() {
        FittedBox b = box();
        Object f = b == null ? null : b.getFit();
        return f instanceof com.codename1.flutter.BoxFit
                ? (com.codename1.flutter.BoxFit) f
                : com.codename1.flutter.BoxFit.contain;
    }

    private Alignment alignment() {
        FittedBox b = box();
        Object a = b == null ? null : b.getAlignment();
        if (a instanceof Alignment) {
            return (Alignment) a;
        }
        if (a instanceof AlignmentDirectional) {
            return ((AlignmentDirectional) a).resolve();
        }
        return Alignment.center;
    }

    /**
     * Flutter's {@code BoxConstraints.constrainSizeAndAttemptToPreserveAspectRatio}:
     * the size unchanged when it already fits, otherwise the largest one with the
     * same aspect ratio that does.
     */
    static Size constrainPreservingAspect(BoxConstraints c, Size size) {
        if (c.isTight()) {
            return new Size(c.minWidth(), c.minHeight());
        }
        double width = size.width();
        double height = size.height();
        if (width <= 0 || height <= 0) {
            return c.constrain(size);
        }
        double aspect = width / height;
        if (width > c.maxWidth()) {
            width = c.maxWidth();
            height = width / aspect;
        }
        if (height > c.maxHeight()) {
            height = c.maxHeight();
            width = height * aspect;
        }
        if (width < c.minWidth()) {
            width = c.minWidth();
            height = width / aspect;
        }
        if (height < c.minHeight()) {
            height = c.minHeight();
            width = height * aspect;
        }
        return new Size(c.constrainWidth(width), c.constrainHeight(height));
    }
}
