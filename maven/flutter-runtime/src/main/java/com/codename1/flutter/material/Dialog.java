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
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Flutter's {@code Dialog}: one child on the Material dialog surface. It is drawn by
 * {@link AlertDialog}'s renderer with the child as its whole content, so it gets the
 * same surface colour, corner radius and elevation.
 */
public class Dialog extends StatelessWidget {

    private Widget child;
    private com.codename1.flutter.Color backgroundColor;
    private com.codename1.flutter.ShapeBorder shape;

    public void child(Widget v) {
        this.child = v;
    }

    public void backgroundColor(com.codename1.flutter.Color v) {
        this.backgroundColor = v;
    }

    public void elevation(double v) {
    }

    public void insetPadding(com.codename1.flutter.EdgeInsets v) {
    }

    public void shape(Object v) {
        if (v instanceof com.codename1.flutter.ShapeBorder) {
            this.shape = (com.codename1.flutter.ShapeBorder) v;
        }
    }

    @Override
    public Widget build(BuildContext context) {
        AlertDialog d = new AlertDialog();
        d.content(child);
        if (backgroundColor != null) {
            d.backgroundColor(backgroundColor);
        }
        if (shape != null) {
            d.shape(shape);
        }
        return d;
    }
}
