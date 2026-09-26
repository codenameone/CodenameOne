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

import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.HasChild;

/**
 * Fills its box with a {@link NotchedShape}'s outline and paints its child on top.
 *
 * <p>Not a Flutter widget: it is the piece a {@code BottomAppBar} needs and Flutter gets
 * from its own render object. A Container fills its box, and the whole point of a notched
 * shape is that the box is not what should be filled -- the docked button sits in a
 * cut-out of the bar's top edge, and behind that cut-out the page shows through.</p>
 */
public class NotchedSurface extends Widget implements HasChild {

    private NotchedShape shape;
    private Color color;
    private double notchMargin;
    private Widget child;

    public void shape(NotchedShape v) {
        this.shape = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void notchMargin(double v) {
        this.notchMargin = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    public NotchedShape getShape() {
        return shape;
    }

    public Color getColor() {
        return color;
    }

    public double getNotchMargin() {
        return notchMargin;
    }

    @Override
    public Element createElement() {
        return new NotchedSurfaceRenderElement(this);
    }
}
