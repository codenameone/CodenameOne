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

import com.codename1.flutter.Brightness;
import com.codename1.flutter.Color;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.TextStyle;

/**
 * Material {@code ChipThemeData}: write-once chip styling. Named Dart
 * constructor parameters map to setter methods; unset values stay null.
 */
public class ChipThemeData {

    private Color backgroundColor;
    private Color disabledColor;
    private Color selectedColor;
    private Color secondarySelectedColor;
    private Color deleteIconColor;
    private Color shadowColor;
    private EdgeInsets padding;
    private EdgeInsets labelPadding;
    private Object shape;
    private TextStyle labelStyle;
    private TextStyle secondaryLabelStyle;
    private Brightness brightness;
    private Double elevation;
    private Double pressElevation;

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void disabledColor(Color v) {
        this.disabledColor = v;
    }

    public void selectedColor(Color v) {
        this.selectedColor = v;
    }

    public void secondarySelectedColor(Color v) {
        this.secondarySelectedColor = v;
    }

    public void deleteIconColor(Color v) {
        this.deleteIconColor = v;
    }

    public void shadowColor(Color v) {
        this.shadowColor = v;
    }

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void labelPadding(EdgeInsets v) {
        this.labelPadding = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public void labelStyle(TextStyle v) {
        this.labelStyle = v;
    }

    public void secondaryLabelStyle(TextStyle v) {
        this.secondaryLabelStyle = v;
    }

    public void brightness(Brightness v) {
        this.brightness = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void pressElevation(double v) {
        this.pressElevation = v;
    }

    public Color backgroundColor() {
        return backgroundColor;
    }

    public EdgeInsets labelPadding() {
        return labelPadding;
    }

    public Color secondarySelectedColor() {
        return secondarySelectedColor;
    }

    public Brightness brightness() {
        return brightness;
    }
}
