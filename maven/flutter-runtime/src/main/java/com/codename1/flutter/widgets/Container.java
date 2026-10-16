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

import com.codename1.flutter.Clip;
import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;

/**
 * A convenience widget that combines painting, positioning and sizing —
 * Flutter's {@code Container}. Applies (in order) margin, decoration/color,
 * additional constraints + explicit width/height, padding and alignment around
 * an optional child.
 *
 * <p>Loosely-typed properties ({@code alignment}, {@code padding},
 * {@code margin}, {@code decoration}) accept their several Flutter value types;
 * the render element interprets the ones it supports
 * ({@link com.codename1.flutter.Alignment}/{@link com.codename1.flutter.AlignmentDirectional},
 * {@link com.codename1.flutter.EdgeInsets}, {@link com.codename1.flutter.BoxDecoration}).</p>
 */
/// Implements {@link HasChild} so the widget walkers can see THROUGH it.
/// A button consumes its content rather than mounting it, and the walk that finds that
/// content stops at any wrapper it cannot open: Shrine's login buttons wrap their label
/// in a Padding, and both rendered with no label at all -- the row collapsed to a blob
/// where the reference reads CANCEL and NEXT.
public class Container extends Widget implements HasChild {

    private Object alignment;
    private Object padding;
    private Color color;
    private Object decoration;
    private Object foregroundDecoration;
    private Double width;
    private Double height;
    private BoxConstraints constraints;
    private Object margin;
    private Object transform;
    private Object transformAlignment;
    private Clip clipBehavior = Clip.none;
    private Widget child;

    public void alignment(Object v) {
        this.alignment = v;
    }

    public void padding(Object v) {
        this.padding = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void decoration(Object v) {
        this.decoration = v;
    }

    public void foregroundDecoration(Object v) {
        this.foregroundDecoration = v;
    }

    public void width(double v) {
        this.width = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void constraints(BoxConstraints v) {
        this.constraints = v;
    }

    public void margin(Object v) {
        this.margin = v;
    }

    public void transform(Object v) {
        this.transform = v;
    }

    public void transformAlignment(Object v) {
        this.transformAlignment = v;
    }

    public void clipBehavior(Clip v) {
        this.clipBehavior = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Object getAlignment() {
        return alignment;
    }

    public Object getPadding() {
        return padding;
    }

    public Color getColor() {
        return color;
    }

    public Object getDecoration() {
        return decoration;
    }

    public Double getWidth() {
        return width;
    }

    public Double getHeight() {
        return height;
    }

    public BoxConstraints getConstraints() {
        return constraints;
    }

    public Object getMargin() {
        return margin;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new ContainerRenderElement(this);
    }
}
