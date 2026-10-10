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

import com.codename1.flutter.Offset;
import com.codename1.flutter.Widget;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;

/**
 * Paints a subtree shifted by an offset expressed as a FRACTION OF ITS OWN SIZE — Flutter's
 * {@code FractionalTranslation}, and the mechanism behind {@code SlideTransition}.
 *
 * <p>Like Flutter this is a paint effect: the child is laid out where it belongs and only
 * the painting moves, so a sliding page does not disturb the layout around it. The fraction
 * is resolved against the laid-out size, which is why it must happen here rather than in the
 * widget — the size is not known until layout has run.</p>
 *
 * <p>Both widgets previously reported the translation as unimplemented and drew the child in
 * place, which turned every slide in the app into a jump.</p>
 */
public class FractionalTranslationRenderElement extends EffectRenderElement {

    /** Resolves the current fractional offset — the widget's, or an animation's. */
    public interface FractionSource {
        Offset fraction();

        Widget child();

        /**
         * The animation to repaint with, or null for a static translation. Repainting is
         * enough: the fraction is read at paint time and the child's layout never moves,
         * so a tick must not cost a layout pass.
         */
        com.codename1.flutter.foundation.Listenable driver();
    }

    private com.codename1.flutter.foundation.Listenable listened;
    private final dart.runtime.Funcs.VoidFunc0 repaint = new dart.runtime.Funcs.VoidFunc0() {
        @Override
        public void call() {
            // The pane's box depends on the fraction, so a tick has to re-derive it
            // before the repaint -- otherwise the frame paints translated content
            // against the previous frame's clip. It is four setters on one component,
            // not a layout pass, which is why an animated slide stays cheap.
            refreshPaneBounds();
            markNeedsPaint();
        }
    };

    // The box Flutter laid this effect out at, BEFORE the pane was grown to make room
    // for the translation. position() is the only writer; refreshPaneBounds re-derives
    // the pane from it on every tick.
    private int laidX;
    private int laidY;
    private int laidW;
    private int laidH;

    public FractionalTranslationRenderElement(Widget widget) {
        super(widget);
    }

    /**
     * The CURRENT configuration. Read through {@code widget()} rather than captured at
     * construction: a rebuild swaps the widget, and a captured one would keep reporting the
     * offset and the animation of a configuration that is no longer on screen.
     */
    private FractionSource source() {
        Widget w = widget();
        return w instanceof FractionSource ? (FractionSource) w : null;
    }

    @Override
    public void mount(com.codename1.flutter.Element parent, int slot) {
        super.mount(parent, slot);
        subscribe();
    }

    @Override
    public void update(Widget newWidget) {
        unsubscribe();
        super.update(newWidget);
        subscribe();
    }

    @Override
    public void unmount() {
        unsubscribe();
        super.unmount();
    }

    private void subscribe() {
        FractionSource src = source();
        listened = src == null ? null : src.driver();
        if (listened != null) {
            listened.addListener(repaint);
        }
    }

    private void unsubscribe() {
        if (listened != null) {
            listened.removeListener(repaint);
            listened = null;
        }
    }

    @Override
    protected Widget effectChild() {
        FractionSource src = source();
        return src == null ? null : src.child();
    }

    /**
     * Grows the pane to cover BOTH the laid-out box and the translated one, and moves its
     * origin to the union's top-left.
     *
     * <p>This is what makes a negative translation visible at all. Codename One clips every
     * component to its own rectangle in {@code Component.paintInternalImpl} before calling
     * its {@code paint}, so painting the subtree shifted by {@code -w/2} inside a pane whose
     * box is still the laid-out one drew three quarters of it into the discarded region: the
     * gallery's feature-discovery circle, positioned at its centre and pulled back by half
     * its size, rendered as the bottom-right quadrant of a circle with two hard straight
     * edges meeting exactly at the centre point. No form of transform escapes that clip --
     * it is already installed in device space by the time this runs -- so the box has to
     * cover the pixels the effect intends to touch.</p>
     *
     * <p>The subtree sits at the pane's origin, so moving that origin moves the subtree with
     * it; {@link #paintOffsetX} compensates, leaving the net shift exactly the fraction.</p>
     */
    private void refreshPaneBounds() {
        Container pane = pane();
        if (pane == null) {
            return;
        }
        int[] box = translatedBox(laidX, laidY, laidW, laidH, deltaX(), deltaY());
        pane.setX(box[0]);
        pane.setY(box[1]);
        pane.setWidth(box[2]);
        pane.setHeight(box[3]);
    }

    /**
     * The union of a box and the same box shifted by {@code (dx, dy)}, plus the
     * translation still owed at paint time: {@code {x, y, w, h, paintDx, paintDy}}.
     *
     * <p>Separated out and package visible so the arithmetic can be asserted without a
     * display: the whole defect this fixes was a box that did not cover the pixels the
     * paint was going to touch.</p>
     */
    static int[] translatedBox(int x, int y, int w, int h, int dx, int dy) {
        return new int[] {
            x + Math.min(dx, 0),
            y + Math.min(dy, 0),
            w + Math.abs(dx),
            h + Math.abs(dy),
            Math.max(dx, 0),
            Math.max(dy, 0),
        };
    }

    private Container pane() {
        com.codename1.ui.Component c = component();
        return c instanceof Container ? (Container) c : null;
    }

    private Offset fraction() {
        FractionSource src = source();
        return src == null ? null : src.fraction();
    }

    private int deltaX() {
        Offset f = fraction();
        return f == null ? 0 : (int) Math.round(f.dx() * laidW);
    }

    private int deltaY() {
        Offset f = fraction();
        return f == null ? 0 : (int) Math.round(f.dy() * laidH);
    }

    /**
     * The translation to apply while painting: the fraction minus the part already
     * absorbed by moving the pane's origin. {@code min(d,0) + max(d,0) == d} for either
     * sign, so the subtree lands exactly {@code d} from where it was laid out.
     */
    private int paintOffsetX() {
        return Math.max(deltaX(), 0);
    }

    private int paintOffsetY() {
        return Math.max(deltaY(), 0);
    }

    @Override
    public void position(int x, int y) {
        super.position(x, y);
        Container pane = pane();
        if (pane == null) {
            return;
        }
        laidX = x;
        laidY = y;
        laidW = pane.getWidth();
        laidH = pane.getHeight();
        refreshPaneBounds();
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Subtree paintChildren) {
        int dx = paintOffsetX();
        int dy = paintOffsetY();
        if (dx == 0 && dy == 0) {
            paintChildren.paint(g);
            return;
        }
        // A plain integer translate, deliberately: this shifts a SUBTREE, and
        // Container.paint translates by its own x/y on top of it. The core's child
        // painting is written against the Graphics translation, not against an affine
        // transform, so installing a transform here would leave that bookkeeping
        // disagreeing with the clip. An affine belongs where a shape is rendered
        // directly (see FlutterShapeBorderPainter), not around a subtree walk.
        g.translate(dx, dy);
        try {
            paintChildren.paint(g);
        } finally {
            g.translate(-dx, -dy);
        }
    }
}
