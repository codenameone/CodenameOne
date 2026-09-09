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
 * Flutter's RenderPositionedBox with a fixed center alignment: loosens the
 * incoming constraints for the child, expands itself to the bounded axes and
 * positions the child by alignment. Owns no CN1 component.
 */
public class CenterRenderElement extends SingleChildRenderElement {

    public CenterRenderElement(Center widget) {
        super(widget);
    }

    @Override
    protected Widget childWidget() {
        return ((Center) widget()).getChild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RenderElement child = renderChild();
        if (child == null) {
            return constraints.constrain(new Size(
                    constraints.hasBoundedWidth() ? constraints.maxWidth() : 0,
                    constraints.hasBoundedHeight() ? constraints.maxHeight() : 0));
        }
        Size cs = child.layout(constraints.loosen());
        double w = constraints.hasBoundedWidth() ? constraints.maxWidth() : cs.width();
        double h = constraints.hasBoundedHeight() ? constraints.maxHeight() : cs.height();
        Size self = constraints.constrain(new Size(w, h));
        setChildOffset(child,
                Alignment.along(0, self.width(), cs.width()),
                Alignment.along(0, self.height(), cs.height()));
        return self;
    }
}
