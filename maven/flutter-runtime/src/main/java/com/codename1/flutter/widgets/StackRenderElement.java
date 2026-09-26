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
import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Flutter's RenderStack:
 * <ol>
 *   <li>Non-positioned children are laid out against the constraints the
 *       stack's {@code StackFit} implies - loosened for {@code loose} (the
 *       default), tight to the stack for {@code expand}, unchanged for
 *       {@code passthrough}; the stack sizes to the biggest of them
 *       (constrained), or expands to the bounded axes when every child is
 *       positioned. Under tight constraints the stack fills them either way.</li>
 *   <li>Non-positioned children are placed by the stack's alignment
 *       (default topLeft).</li>
 *   <li>{@link Positioned} children resolve left/top/right/bottom/width/
 *       height (logical pixels) against the stack bounds: two opposing
 *       insets, or an inset plus an extent, tighten that axis; an
 *       unresolved axis stays loose and falls back to alignment.</li>
 * </ol>
 * Z-order is child order — later children paint on top, which the flat
 * container honors because components attach in element-tree order. Owns no
 * CN1 component.
 */
public class StackRenderElement extends RenderElement {

    private List<Element> children = new ArrayList<Element>();

    public StackRenderElement(Stack widget) {
        super(widget);
    }

    private Stack stack() {
        return (Stack) widget();
    }

    private Alignment alignment() {
        Alignment a = stack().getAlignment();
        return a == null ? Alignment.topLeft : a;
    }

    @Override
    protected void syncChildren() {
        List<Widget> newWidgets = new ArrayList<Widget>();
        if (stack().getChildren() != null) {
            for (Widget w : stack().getChildren()) {
                if (w != null) {
                    newWidgets.add(w);
                }
            }
        }
        children = updateChildren(children, newWidgets);
    }

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        for (Element c : children) {
            if (c != null) {
                visitor.call(c);
            }
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        List<RenderElement> kids = renderChildren();

        // Pass 1: size the stack from the non-positioned children.
        BoxConstraints nonPositioned = nonPositionedConstraints(constraints);
        double maxW = 0;
        double maxH = 0;
        boolean hasNonPositioned = false;
        for (RenderElement kid : kids) {
            if (kid instanceof PositionedRenderElement) {
                continue;
            }
            hasNonPositioned = true;
            Size cs = kid.layout(nonPositioned);
            maxW = Math.max(maxW, cs.width());
            maxH = Math.max(maxH, cs.height());
        }
        Size self;
        if (hasNonPositioned) {
            self = constraints.constrain(new Size(maxW, maxH));
        } else {
            self = constraints.constrain(new Size(
                    constraints.hasBoundedWidth() ? constraints.maxWidth() : 0,
                    constraints.hasBoundedHeight() ? constraints.maxHeight() : 0));
        }

        // Pass 2: place everything.
        Alignment a = alignment();
        for (RenderElement kid : kids) {
            if (kid instanceof PositionedRenderElement) {
                placePositioned((PositionedRenderElement) kid, self, a);
            } else {
                Size cs = kid.size();
                setChildOffset(kid,
                        Alignment.along(a.x(), self.width(), cs.width()),
                        Alignment.along(a.y(), self.height(), cs.height()));
            }
        }
        return self;
    }

    /**
     * The constraints a non-positioned child is laid out against — Flutter's
     * {@code RenderStack.performLayout} switch on {@code StackFit}.
     *
     * <p>{@code expand} is not a detail: it is how a Stack tells its children to FILL it,
     * and a child that also carries its own size gets that size overridden by the tight
     * constraints. The gallery's study card is exactly that shape - a
     * {@code Stack(fit: StackFit.expand)} over an image that separately declares
     * {@code height: 240} - so loosening unconditionally let the image keep its own height
     * and sit inside the card instead of covering it, framed by a band of the Material
     * surface behind it.</p>
     */
    private BoxConstraints nonPositionedConstraints(BoxConstraints constraints) {
        com.codename1.flutter.StackFit f = stack().getFit();
        if (f == com.codename1.flutter.StackFit.passthrough) {
            return constraints;
        }
        if (f == com.codename1.flutter.StackFit.expand) {
            // Tight to what we have room for. An unbounded axis has no biggest to be tight
            // to - Flutter asserts here; we loosen that axis instead so an unbounded parent
            // degrades to the loose behaviour rather than propagating an infinite size.
            return new BoxConstraints(
                    constraints.hasBoundedWidth() ? constraints.maxWidth() : 0,
                    constraints.maxWidth(),
                    constraints.hasBoundedHeight() ? constraints.maxHeight() : 0,
                    constraints.maxHeight());
        }
        return constraints.loosen();
    }

    private void placePositioned(PositionedRenderElement kid, Size self, Alignment a) {
        Positioned p = kid.positioned();
        Double left = px(p.getLeft());
        Double top = px(p.getTop());
        Double right = px(p.getRight());
        Double bottom = px(p.getBottom());
        Double width = px(p.getWidth());
        Double height = px(p.getHeight());

        BoxConstraints childConstraints = new BoxConstraints(
                resolvedExtent(left, right, width, self.width(), 0),
                resolvedExtent(left, right, width, self.width(), Double.POSITIVE_INFINITY),
                resolvedExtent(top, bottom, height, self.height(), 0),
                resolvedExtent(top, bottom, height, self.height(), Double.POSITIVE_INFINITY));
        Size cs = kid.layout(childConstraints);

        double x;
        if (left != null) {
            x = left;
        } else if (right != null) {
            x = self.width() - right - cs.width();
        } else {
            x = Alignment.along(a.x(), self.width(), cs.width());
        }
        double y;
        if (top != null) {
            y = top;
        } else if (bottom != null) {
            y = self.height() - bottom - cs.height();
        } else {
            y = Alignment.along(a.y(), self.height(), cs.height());
        }
        setChildOffset(kid, x, y);
    }

    /**
     * The tight extent implied by two opposing insets or an explicit extent,
     * or {@code fallback} (0 for the min bound, &#8734; for the max) when the
     * axis is unresolved and stays loose.
     */
    private static double resolvedExtent(Double lead, Double trail, Double extent,
                                         double stackExtent, double fallback) {
        if (lead != null && trail != null) {
            return Math.max(0, stackExtent - lead - trail);
        }
        if (extent != null) {
            return extent;
        }
        return fallback;
    }

    private static Double px(Double lp) {
        return lp == null ? null : Double.valueOf(Dp.px(lp));
    }
}
