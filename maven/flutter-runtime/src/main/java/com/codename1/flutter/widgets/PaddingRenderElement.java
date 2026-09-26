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

import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;

/**
 * Deflates the incoming constraints by the padding (converted from logical
 * pixels to device pixels), lays out the child, and reports the child size
 * plus the insets. Owns no CN1 component.
 */
public class PaddingRenderElement extends SingleChildRenderElement {

    public PaddingRenderElement(Padding widget) {
        super(widget);
    }

    @Override
    protected Widget childWidget() {
        return ((Padding) widget()).getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        EdgeInsets lp = ((Padding) widget()).getPadding();
        EdgeInsets px = lp == null
                ? EdgeInsets.all(0)
                : EdgeInsets.only(Dp.px(lp.left()), Dp.px(lp.top()), Dp.px(lp.right()), Dp.px(lp.bottom()));
        RenderElement child = renderChild();
        if (child == null) {
            return constraints.constrain(new Size(px.horizontal(), px.vertical()));
        }
        Size cs = child.layout(constraints.deflate(px));
        setChildOffset(child, px.left(), px.top());
        return constraints.constrain(new Size(cs.width() + px.horizontal(), cs.height() + px.vertical()));
    }
}
