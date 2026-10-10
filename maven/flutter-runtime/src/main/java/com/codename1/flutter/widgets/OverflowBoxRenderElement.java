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
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;

/**
 * Lays its child out against DIFFERENT constraints from the ones it was given, and lets
 * the result overflow -- Flutter's {@code OverflowBox}.
 *
 * <p>The box itself still takes the size its own constraints allow; only the child is
 * freed. Overriding just {@code maxHeight} and leaving the child centred is how a layout
 * lifts something above its slot: the child grows past the box at both ends and the
 * alignment splits the difference.</p>
 *
 * <p>This was a pass-through, so the overrides did nothing and the child was laid out in
 * the box. Crane's three front layers are positioned exactly that way -- the middle one
 * rides 60dp higher than its slot because the TabBarView is allowed to overflow by 120
 * and centred, and the outer two pad themselves back down by 60. With the overflow
 * ignored the padding had nothing to cancel, so every layer sat 60dp too low and took
 * the whole page below it along.</p>
 */
public class OverflowBoxRenderElement extends SingleChildRenderElement {

    public OverflowBoxRenderElement(Widget widget) {
        super(widget);
    }

    @Override
    protected Widget childWidget() {
        return box().getChild();
    }

    private OverflowBox box() {
        return (OverflowBox) widget();
    }

    /** The child's constraints: each edge overridden where given, inherited where not. */
    static BoxConstraints innerConstraints(BoxConstraints outer, Double minW, Double maxW,
            Double minH, Double maxH) {
        return new BoxConstraints(
                minW != null ? minW.doubleValue() : outer.minWidth(),
                maxW != null ? maxW.doubleValue() : outer.maxWidth(),
                minH != null ? minH.doubleValue() : outer.minHeight(),
                maxH != null ? maxH.doubleValue() : outer.maxHeight());
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        Size self = constraints.constrain(new Size(
                constraints.hasBoundedWidth() ? constraints.maxWidth() : 0,
                constraints.hasBoundedHeight() ? constraints.maxHeight() : 0));
        RenderElement child = renderChild();
        if (child == null) {
            return self;
        }
        OverflowBox w = box();
        Size cs = child.layout(innerConstraints(constraints,
                px(w.getMinWidth()), px(w.getMaxWidth()),
                px(w.getMinHeight()), px(w.getMaxHeight())));
        Alignment a = alignment();
        setChildOffset(child,
                Alignment.along(a.x(), self.width(), cs.width()),
                Alignment.along(a.y(), self.height(), cs.height()));
        return self;
    }

    /** A logical-pixel override in device pixels, or null when it was not given. */
    private static Double px(Double lp) {
        return lp == null ? null
                : Double.valueOf(com.codename1.flutter.rendering.Dp.px(lp.doubleValue()));
    }

    /** Flutter's default is {@code Alignment.center}. */
    private Alignment alignment() {
        Object a = box().getAlignment();
        return a instanceof Alignment ? (Alignment) a : Alignment.center;
    }
}
