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
import com.codename1.flutter.Element;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;

/**
 * A piece of material — Flutter's {@code Material}. Provides a surface color
 * (and, in Flutter, elevation shadow and ink effects) behind its child. This
 * milestone paints the {@code color} surface and sizes to the child; elevation
 * shadow, shape and ink are retained but not yet rendered.
 */
public class Material extends Widget {

    private Object type;
    private double elevation;
    private Color color;
    private Color shadowColor;
    private Color surfaceTintColor;
    private TextStyle textStyle;
    private Object borderRadius;
    private Object shape;
    private boolean borderOnForeground = true;
    private Clip clipBehavior = Clip.none;
    private Widget child;

    public void type(Object v) {
        this.type = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void shadowColor(Color v) {
        this.shadowColor = v;
    }

    public void surfaceTintColor(Color v) {
        this.surfaceTintColor = v;
    }

    public void textStyle(TextStyle v) {
        this.textStyle = v;
    }

    public void borderRadius(Object v) {
        this.borderRadius = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public void borderOnForeground(boolean v) {
        this.borderOnForeground = v;
    }

    public void clipBehavior(Clip v) {
        this.clipBehavior = v;
    }

    /** The duration of ink/elevation animations — Flutter's {@code animationDuration}. */
    public void animationDuration(dart.core.Duration v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    /// The shape given to this Material — a RoundedRectangleBorder carries the radius.
    /// Whether this surface clips its subtree to its shape.
    public Clip getClipBehavior() {
        return clipBehavior;
    }

    public Object getShape() {
        return shape;
    }

    /// The borderRadius given directly (Material accepts either form).
    public Object getBorderRadius() {
        return borderRadius;
    }

    public Color getColor() {
        return color;
    }

    public double getElevation() {
        return elevation;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new MaterialRenderElement(this);
    }
}
