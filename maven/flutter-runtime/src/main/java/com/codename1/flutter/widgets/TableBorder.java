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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Kit;
import dart.core.DartList;
import dart.runtime.Funcs;

/** Flutter's {@code TableBorder}: the lines a {@link Table} draws round and between cells. */
public class TableBorder {
    private com.codename1.flutter.Color color = new com.codename1.flutter.Color(0xFF000000L);
    private double width = 1;
    public static TableBorder all(com.codename1.flutter.Color color, Double width, Object style, Object borderRadius) {
        TableBorder b = new TableBorder();
        if (color != null) {
            b.color = color;
        }
        if (width != null) {
            b.width = width.doubleValue();
        }
        return b;
    }
    /** {@code TableBorder.symmetric(inside:, outside:)}: drawn here as one grid of the inside side. */
    public static TableBorder symmetric(Object inside, Object outside) {
        TableBorder b = new TableBorder();
        Object side = inside != null ? inside : outside;
        if (side instanceof com.codename1.flutter.BorderSide) {
            com.codename1.flutter.BorderSide s = (com.codename1.flutter.BorderSide) side;
            b.color = s.color();
            b.width = s.width();
        }
        return b;
    }
    public com.codename1.flutter.Color getColor() { return color; }
    public double getWidth() { return width; }
}
