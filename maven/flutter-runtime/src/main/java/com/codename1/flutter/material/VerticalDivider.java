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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.ColoredBox;
import com.codename1.flutter.widgets.SizedBox;

/**
 * A thin vertical line, the vertical sibling of {@link Divider} — Flutter's
 * {@code VerticalDivider}. Occupies {@code width} horizontally and paints a
 * line {@code thickness} wide in {@code color}. This pass renders a full-height
 * box of the given width, filled when a color is supplied.
 */
public class VerticalDivider extends StatelessWidget {

    private Double width;
    private Double thickness;
    private Color color;

    public void width(double v) {
        this.width = v;
    }

    public void thickness(double v) {
        this.thickness = v;
    }

    public void indent(double v) {
    }

    public void endIndent(double v) {
    }

    public void color(Color v) {
        this.color = v;
    }

    @Override
    public Widget build(BuildContext context) {
        SizedBox box = new SizedBox();
        box.width(width != null ? width : 16.0);
        if (color != null) {
            ColoredBox cb = new ColoredBox();
            cb.color(color);
            box.child(cb);
        }
        return box;
    }
}
