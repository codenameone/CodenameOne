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

/**
 * Material {@code IconThemeData}: a write-once icon styling bundle. Named Dart
 * constructor parameters map to setter methods; unset values stay null and
 * inherit. {@link #copyWith} merges non-null overrides onto a copy.
 */
public class IconThemeData {

    private Color color;
    private Double size;
    private Double opacity;
    private Double fill;
    private Double weight;
    private Double grade;
    private Double opticalSize;
    private Object shadows;
    private Boolean applyTextScaling;

    public void color(Color v) {
        this.color = v;
    }

    public void size(double v) {
        this.size = v;
    }

    public void opacity(double v) {
        this.opacity = v;
    }

    public void fill(double v) {
        this.fill = v;
    }

    public void weight(double v) {
        this.weight = v;
    }

    public void grade(double v) {
        this.grade = v;
    }

    public void opticalSize(double v) {
        this.opticalSize = v;
    }

    public void shadows(Object v) {
        this.shadows = v;
    }

    public void applyTextScaling(boolean v) {
        this.applyTextScaling = v;
    }

    public Color color() {
        return color;
    }

    public Double size() {
        return size;
    }

    public Double opacity() {
        return opacity;
    }

    public IconThemeData copyWith(Color color, Double size, Double opacity, Double fill,
                                  Double weight, Double grade, Double opticalSize, Object shadows,
                                  Boolean applyTextScaling) {
        IconThemeData c = new IconThemeData();
        c.color = color != null ? color : this.color;
        c.size = size != null ? size : this.size;
        c.opacity = opacity != null ? opacity : this.opacity;
        c.fill = fill != null ? fill : this.fill;
        c.weight = weight != null ? weight : this.weight;
        c.grade = grade != null ? grade : this.grade;
        c.opticalSize = opticalSize != null ? opticalSize : this.opticalSize;
        c.shadows = shadows != null ? shadows : this.shadows;
        c.applyTextScaling = applyTextScaling != null ? applyTextScaling : this.applyTextScaling;
        return c;
    }
}
