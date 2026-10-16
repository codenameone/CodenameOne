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

/**
 * A thin horizontal rule with vertical breathing room. {@code height} is the
 * total vertical extent the divider occupies (default 16lp);
 * {@code thickness} is the painted line (default 1lp). Backed by a CN1
 * hairline strip component (UIID "FlutterDivider").
 */
public class Divider extends Widget {

    private Double height;
    private Double thickness;
    private Color color;
    private Double indent;
    private Double endIndent;

    public void height(double v) {
        this.height = v;
    }

    public void indent(double v) {
        this.indent = v;
    }

    public void endIndent(double v) {
        this.endIndent = v;
    }

    public Double getIndent() {
        return indent;
    }

    public Double getEndIndent() {
        return endIndent;
    }

    public void thickness(double v) {
        this.thickness = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public Double getHeight() {
        return height;
    }

    public Double getThickness() {
        return thickness;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public Element createElement() {
        return new DividerRenderElement(this);
    }
}
