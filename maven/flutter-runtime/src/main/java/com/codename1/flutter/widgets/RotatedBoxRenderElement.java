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
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;

/**
 * Lays out and paints a {@link RotatedBox}: quarter turns that affect LAYOUT, not just
 * painting.
 *
 * <p>That is the whole difference from {@code Transform.rotate} — an odd number of quarter
 * turns swaps the box's width and height, so the parent reserves the rotated footprint. The
 * child is measured against constraints with the axes swapped, and the painting is rotated
 * about the centre to match.</p>
 */
public class RotatedBoxRenderElement extends EffectRenderElement {

    public RotatedBoxRenderElement(RotatedBox widget) {
        super(widget);
    }

    private RotatedBox box() {
        return (RotatedBox) widget();
    }

    /** Quarter turns normalised to 0..3; only the parity affects the axes. */
    private int turns() {
        long q = box().getQuarterTurns() % 4;
        return (int) (q < 0 ? q + 4 : q);
    }

    private boolean swapsAxes() {
        return (turns() & 1) == 1;
    }

    @Override
    protected Widget effectChild() {
        return box().getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        if (!swapsAxes()) {
            return super.performLayout(constraints);
        }
        // Measure the child in the ROTATED frame, then report its footprint swapped back.
        BoxConstraints swapped = new BoxConstraints(constraints.minHeight(),
                constraints.maxHeight(), constraints.minWidth(), constraints.maxWidth());
        Size child = super.performLayout(swapped);
        return constraints.constrain(new Size(child.height(), child.width()));
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Subtree paintChildren) {
        int t = turns();
        if (t == 0) {
            paintChildren.paint(g);
            return;
        }
        if (!g.isTransformSupported()) {
            com.codename1.flutter.FlutterErrorReport.unimplemented("RotatedBox",
                    "this platform has no transform support; the rotation is not painted");
            paintChildren.paint(g);
            return;
        }
        // The subtree is rendered to a layer and the LAYER is turned, rather than the
        // subtree being walked through a rotated Graphics. Under a matrix, Codename
        // One's paint-time cull compares device-pixel component bounds against a clip
        // reported in the matrix's own coordinates, and drops children that are in
        // fact on screen -- see the note on EffectRenderElement.layer.
        //
        // The layer takes the CHILD's box, not the pane's: performLayout above reports
        // the child's footprint with its axes swapped, so for a quarter turn the two
        // differ and a pane-sized layer would clip the child before turning it.
        int lw = swapsAxes() ? pane.getHeight() : pane.getWidth();
        int lh = swapsAxes() ? pane.getWidth() : pane.getHeight();
        com.codename1.ui.Image rendered = layer(pane, paintChildren, lw, lh);
        if (rendered == null) {
            return;
        }
        com.codename1.ui.Transform saved = g.getTransform();
        com.codename1.ui.Transform r = saved.copy();
        // The pivot is in the pane's own parent coordinates -- the space this Graphics
        // is in. Absolute screen coordinates are a different space once any ancestor
        // has translated, which is every ancestor.
        float cx = pane.getX() + pane.getWidth() / 2f;
        float cy = pane.getY() + pane.getHeight() / 2f;
        r.translate(cx, cy);
        r.rotate((float) (t * Math.PI / 2), 0, 0);
        r.translate(-cx, -cy);
        g.setTransform(r);
        try {
            g.drawImage(rendered, Math.round(cx - lw / 2f), Math.round(cy - lh / 2f));
        } finally {
            g.setTransform(saved);
        }
    }
}
