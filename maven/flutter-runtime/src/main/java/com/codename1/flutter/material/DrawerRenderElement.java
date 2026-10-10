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

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;

/**
 * Render element for {@link Drawer}: sizes to the standard Material drawer
 * width (304lp) when unconstrained (the side-menu preferred-size dry pass)
 * and fills whatever the side menu hands it otherwise; the child subtree
 * fills the panel.
 */
public class DrawerRenderElement extends SingleChildRenderElement {

    /** Standard Material drawer width in logical pixels. */
    public static final double WIDTH_LP = 304;

    public DrawerRenderElement(Drawer widget) {
        super(widget);
    }

    @Override
    protected Widget childWidget() {
        return ((Drawer) widget()).getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double width = constraints.hasBoundedWidth()
                ? constraints.maxWidth()
                : Dp.px(WIDTH_LP);
        double height = constraints.hasBoundedHeight() ? constraints.maxHeight() : 0;
        RenderElement child = renderChild();
        if (child != null) {
            Size cs = child.layout(new BoxConstraints(
                    width, width, 0,
                    constraints.hasBoundedHeight() ? height : Double.POSITIVE_INFINITY));
            setChildOffset(child, 0, 0);
            height = Math.max(height, cs.height());
        }
        return constraints.constrain(new Size(width, height));
    }
}
