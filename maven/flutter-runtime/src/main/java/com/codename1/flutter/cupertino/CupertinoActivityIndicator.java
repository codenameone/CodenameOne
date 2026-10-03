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
package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

/**
 * The iOS spinner — Flutter's {@code CupertinoActivityIndicator}. The animated
 * ticks are not drawn this pass; it reserves the correct footprint (a
 * {@code 2 * radius} box, default radius 10) so surrounding layout matches.
 */
public class CupertinoActivityIndicator extends StatelessWidget {

    private Double radius;

    public void color(Color v) {
    }

    public void animating(boolean v) {
    }

    public void radius(double v) {
        this.radius = v;
    }

    @Override
    public Widget build(BuildContext context) {
        double diameter = (radius != null ? radius : 10.0) * 2.0;
        SizedBox b = new SizedBox();
        b.width(diameter);
        b.height(diameter);
        return b;
    }
}
