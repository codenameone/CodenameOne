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

/**
 * A render element that lays out its single child with the incoming
 * constraints and reports the child's size — the structural behavior shared
 * by the wrapper widgets ({@link com.codename1.flutter.widgets.SafeArea},
 * Semantics, Clip*, MouseRegion, Scrollbar, Tooltip, ...). Semantic and visual
 * effects those widgets carry (a11y annotations, clipping, hover) are not yet
 * applied; the child renders unchanged. Owns no CN1 component.
 */
public class PassThroughRenderElement extends SingleChildRenderElement {

    public PassThroughRenderElement(Widget widget) {
        super(widget);
    }

    @Override
    protected Widget childWidget() {
        return ((HasChild) widget()).getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RenderElement child = renderChild();
        if (child == null) {
            return constraints.smallest();
        }
        Size cs = child.layout(constraints);
        setChildOffset(child, 0, 0);
        return constraints.constrain(cs);
    }
}
