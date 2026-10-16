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
import com.codename1.ui.Component;
import com.codename1.ui.Display;

/**
 * Render element for {@link DecoratedBox}: a CN1 Container (UIID "FlutterBox")
 * styled from the decoration, covering the element bounds behind the child.
 * Sizes to the child, or fills the bounded incoming axes when childless.
 */
public class DecoratedBoxRenderElement extends SingleChildRenderElement {

    public DecoratedBoxRenderElement(DecoratedBox widget) {
        super(widget);
    }

    private DecoratedBox box() {
        return (DecoratedBox) widget();
    }

    @Override
    protected Widget childWidget() {
        return box().getChild();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            return null;
        }
        com.codename1.ui.Container face = new com.codename1.ui.Container();
        face.setUIID("FlutterBox");
        face.getAllStyles().setPadding(0, 0, 0, 0);
        face.getAllStyles().setMargin(0, 0, 0, 0);
        FlutterBoxStyle.apply(face, null, box().getDecoration());
        return face;
    }

    @Override
    protected void updateComponent(Component c) {
        FlutterBoxStyle.apply(c, null, box().getDecoration());
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RenderElement child = renderChild();
        if (child == null) {
            return constraints.constrain(new Size(
                    constraints.hasBoundedWidth() ? constraints.maxWidth() : 0,
                    constraints.hasBoundedHeight() ? constraints.maxHeight() : 0));
        }
        Size cs = child.layout(constraints);
        setChildOffset(child, 0, 0);
        return constraints.constrain(cs);
    }
}
