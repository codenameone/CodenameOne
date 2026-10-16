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

import com.codename1.flutter.Axis;
import com.codename1.flutter.Clip;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.WrapAlignment;
import com.codename1.flutter.WrapCrossAlignment;

import dart.core.DartList;

/**
 * Lays its children out in runs along a main axis, wrapping to a new run when
 * the current one is full — Flutter's {@code Wrap}.
 */
public class Wrap extends Widget {

    private Axis direction = Axis.horizontal;
    private WrapAlignment alignment = WrapAlignment.start;
    private double spacing;
    private WrapAlignment runAlignment = WrapAlignment.start;
    private double runSpacing;
    private WrapCrossAlignment crossAxisAlignment = WrapCrossAlignment.start;
    private Object textDirection;
    private Object verticalDirection;
    private Clip clipBehavior = Clip.none;
    private DartList<Widget> children;

    public void direction(Axis v) {
        this.direction = v == null ? Axis.horizontal : v;
    }

    public void alignment(WrapAlignment v) {
        this.alignment = v == null ? WrapAlignment.start : v;
    }

    public void spacing(double v) {
        this.spacing = v;
    }

    public void runAlignment(WrapAlignment v) {
        this.runAlignment = v == null ? WrapAlignment.start : v;
    }

    public void runSpacing(double v) {
        this.runSpacing = v;
    }

    public void crossAxisAlignment(WrapCrossAlignment v) {
        this.crossAxisAlignment = v == null ? WrapCrossAlignment.start : v;
    }

    public void textDirection(Object v) {
        this.textDirection = v;
    }

    public void verticalDirection(Object v) {
        this.verticalDirection = v;
    }

    public void clipBehavior(Clip v) {
        this.clipBehavior = v;
    }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public Axis getDirection() {
        return direction;
    }

    public WrapAlignment getAlignment() {
        return alignment;
    }

    public double getSpacing() {
        return spacing;
    }

    public double getRunSpacing() {
        return runSpacing;
    }

    public WrapCrossAlignment getCrossAxisAlignment() {
        return crossAxisAlignment;
    }

    public DartList<Widget> getChildren() {
        return children;
    }

    @Override
    public Element createElement() {
        return new WrapRenderElement(this);
    }
}
