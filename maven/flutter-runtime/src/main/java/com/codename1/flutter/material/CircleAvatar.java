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
import com.codename1.flutter.ImageProvider;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

/**
 * A circular avatar showing an image or a child (initials/icon) — Flutter's
 * {@code CircleAvatar}. This milestone renders the {@code child} when present,
 * otherwise a fixed-size box sized from {@code radius}; drawing the
 * {@code backgroundImage} clipped to a circle is deferred.
 */
public class CircleAvatar extends StatelessWidget {

    private Widget child;
    private Color backgroundColor;
    private Color foregroundColor;
    private ImageProvider backgroundImage;
    private ImageProvider foregroundImage;
    private Double radius;

    public void child(Widget v) {
        this.child = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void foregroundColor(Color v) {
        this.foregroundColor = v;
    }

    public void backgroundImage(ImageProvider v) {
        this.backgroundImage = v;
    }

    public void foregroundImage(ImageProvider v) {
        this.foregroundImage = v;
    }

    public void onBackgroundImageError(Object v) {
    }

    public void radius(double v) {
        this.radius = v;
    }

    public void minRadius(double v) {
    }

    public void maxRadius(double v) {
    }

    @Override
    public Widget build(BuildContext context) {
        if (child != null) {
            return child;
        }
        SizedBox box = new SizedBox();
        double r = radius != null ? radius : 20.0;
        box.width(r * 2);
        box.height(r * 2);
        return box;
    }
}
