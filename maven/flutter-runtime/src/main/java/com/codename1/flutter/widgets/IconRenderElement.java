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
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.Style;

/**
 * Leaf render box for {@link Icon}: a CN1 Label carrying a material
 * FontImage sized in millimeters equivalent to the requested logical pixels.
 */
public class IconRenderElement extends RenderElement {

    /** Flutter's default icon size in logical pixels. */
    public static final double DEFAULT_SIZE_LP = 24;

    public IconRenderElement(Icon widget) {
        super(widget);
    }

    private Icon icon() {
        return (Icon) widget();
    }

    private double sizeLp() {
        return icon().getSize() != null ? icon().getSize() : DEFAULT_SIZE_LP;
    }

    @Override
    protected Component createComponent() {
        if (!com.codename1.ui.Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        Label l = new Label("", "FlutterIcon");
        l.getAllStyles().setPadding(0, 0, 0, 0);
        l.getAllStyles().setMargin(0, 0, 0, 0);
        applyIcon(l);
        return l;
    }

    @Override
    protected void updateComponent(Component c) {
        applyIcon((Label) c);
    }

    private void applyIcon(Label l) {
        if (icon().getIcon() == null) {
            l.setIcon(null);
            return;
        }
        Style s = new Style(l.getUnselectedStyle());
        if (icon().getColor() != null) {
            s.setFgColor(icon().getColor().rgb());
        }
        s.setBgTransparency(0);
        try {
            l.setIcon(FontImage.createMaterial(icon().getIcon().codePoint(), s, Dp.mm(sizeLp())));
        } catch (Exception err) {
            // headless or missing icon font: layout still reserves the box
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double px = Dp.px(sizeLp());
        return constraints.constrain(new Size(px, px));
    }
}
