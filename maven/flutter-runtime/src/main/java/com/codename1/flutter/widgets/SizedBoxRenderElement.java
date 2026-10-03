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
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;

/**
 * Flutter's RenderConstrainedBox with tight additional constraints for the
 * specified dimensions, merged into the incoming constraints via
 * {@link BoxConstraints#tighten}. Owns no CN1 component.
 */
public class SizedBoxRenderElement extends SingleChildRenderElement {

    public SizedBoxRenderElement(SizedBox widget) {
        super(widget);
    }

    @Override
    protected Widget childWidget() {
        return ((SizedBox) widget()).getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        SizedBox w = (SizedBox) widget();
        Double widthPx = w.getWidth() == null ? null : Double.valueOf(Dp.px(w.getWidth()));
        Double heightPx = w.getHeight() == null ? null : Double.valueOf(Dp.px(w.getHeight()));
        BoxConstraints inner = constraints.tighten(widthPx, heightPx);
        RenderElement child = renderChild();
        if (child == null) {
            return inner.constrain(new Size(
                    widthPx == null ? 0 : widthPx,
                    heightPx == null ? 0 : heightPx));
        }
        Size cs = child.layout(inner);
        setChildOffset(child, 0, 0);
        return cs;
    }
}
