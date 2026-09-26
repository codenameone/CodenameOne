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
 * Material {@code DividerThemeData}: write-once divider styling.
 */
public class DividerThemeData {

    private Double thickness;
    private Color color;
    private Double space;
    private Double indent;
    private Double endIndent;

    public void thickness(double v) {
        this.thickness = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void space(double v) {
        this.space = v;
    }

    public void indent(double v) {
        this.indent = v;
    }

    public void endIndent(double v) {
        this.endIndent = v;
    }

    public Double thickness() {
        return thickness;
    }

    public Color color() {
        return color;
    }

    public Double space() {
        return space;
    }

    public Double indent() {
        return indent;
    }

    public Double endIndent() {
        return endIndent;
    }
}
