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
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

/**
 * Flutter's RenderPositionedBox: loosens the incoming constraints for the
 * child, expands itself to the bounded axes and positions the child by the
 * configured alignment. Owns no CN1 component.
 */
public class AlignRenderElement extends SingleChildRenderElement {

    public AlignRenderElement(Align widget) {
        super(widget);
    }

    private Alignment alignment() {
        Alignment a = ((Align) widget()).getAlignment();
        return a == null ? Alignment.center : a;
    }

    @Override
    protected Widget childWidget() {
        return ((Align) widget()).getChild();
    }

    /**
     * Flutter shrink-wraps an axis when a factor is given for it, or when that axis is
     * unbounded; otherwise the box expands to fill.
     */
    private static boolean shrinkWraps(Double factor, boolean bounded) {
        return factor != null || !bounded;
    }

    private static double factored(Double factor, double childExtent) {
        return childExtent * (factor == null ? 1.0 : factor.doubleValue());
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        Align self0 = (Align) widget();
        Double wf = self0.getWidthFactor();
        Double hf = self0.getHeightFactor();
        boolean shrinkW = shrinkWraps(wf, constraints.hasBoundedWidth());
        boolean shrinkH = shrinkWraps(hf, constraints.hasBoundedHeight());
        RenderElement child = renderChild();
        if (child == null) {
            return constraints.constrain(new Size(
                    shrinkW ? 0 : constraints.maxWidth(),
                    shrinkH ? 0 : constraints.maxHeight()));
        }
        Size cs = child.layout(constraints.loosen());
        // A factor scales the box to a FRACTION of the child, which is how an expand/collapse
        // animates: heightFactor runs 0 -> 1 while the child keeps its full size, and the
        // enclosing ClipRect hides the part that does not fit yet.
        double w = shrinkW ? factored(wf, cs.width()) : constraints.maxWidth();
        double h = shrinkH ? factored(hf, cs.height()) : constraints.maxHeight();
        Size self = constraints.constrain(new Size(w, h));
        Alignment a = alignment();
        setChildOffset(child,
                Alignment.along(a.x(), self.width(), cs.width()),
                Alignment.along(a.y(), self.height(), cs.height()));
        return self;
    }
}
