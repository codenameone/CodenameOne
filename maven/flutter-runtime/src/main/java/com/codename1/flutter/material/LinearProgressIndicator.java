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
 * A horizontal material progress bar — Flutter's {@code LinearProgressIndicator}.
 * This milestone renders a thin 4lp bar filled with the indicator {@code color}
 * (a determinate {@code value} is accepted but the fill fraction and the
 * indeterminate sweep animation are deferred).
 */
public class LinearProgressIndicator extends StatelessWidget {

    private Double value;
    private Color color;
    private Color backgroundColor;
    private Double minHeight;

    public void value(double v) {
        this.value = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void valueColor(Object v) {
    }

    public void minHeight(double v) {
        this.minHeight = v;
    }

    public void semanticsLabel(String v) {
    }

    public void semanticsValue(String v) {
    }

    public void borderRadius(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        SizedBox box = new SizedBox();
        box.height(minHeight != null ? minHeight : 4.0);
        Color fill = color != null ? color : backgroundColor;
        if (fill != null) {
            ColoredBox cb = new ColoredBox();
            cb.color(fill);
            box.child(cb);
        }
        return box;
    }
}
