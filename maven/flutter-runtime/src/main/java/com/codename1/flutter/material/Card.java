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

import com.codename1.flutter.Clip;
import com.codename1.flutter.Color;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.ShapeBorder;
import com.codename1.flutter.Widget;

/**
 * A material card: a rounded, subtly elevated surface around its child.
 * Backed by a CN1 Container (UIID "FlutterCard") with a 12lp round-rect
 * border; default margin 4lp on every edge.
 */
public class Card extends Widget {

    private Color color;
    private Double elevation;
    private EdgeInsets margin;
    private Widget child;
    private ShapeBorder shape;
    private Clip clipBehavior;

    public void color(Color v) {
        this.color = v;
    }

    public void shape(ShapeBorder v) {
        this.shape = v;
    }

    public void clipBehavior(Clip v) {
        this.clipBehavior = v;
    }

    public ShapeBorder getShape() {
        return shape;
    }

    public Clip getClipBehavior() {
        return clipBehavior;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void margin(EdgeInsets v) {
        this.margin = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Color getColor() {
        return color;
    }

    public Double getElevation() {
        return elevation;
    }

    public EdgeInsets getMargin() {
        return margin;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new CardRenderElement(this);
    }
}
