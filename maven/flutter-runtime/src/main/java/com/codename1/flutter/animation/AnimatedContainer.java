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
package com.codename1.flutter.animation;

import com.codename1.flutter.Alignment;
import com.codename1.flutter.Color;
import com.codename1.flutter.EdgeInsets;

import dart.core.Duration;

/**
 * A Container whose properties animate to new values over a {@link Duration}
 * when the widget rebuilds — Flutter's {@code AnimatedContainer}. This pass
 * stores the properties and hosts the child; the tweened transitions are
 * deferred.
 */
public class AnimatedContainer extends AnimatedChildWidget {

    private Duration duration;
    private Curve curve;
    private Double width;
    private Double height;
    private Color color;
    private EdgeInsets padding;
    private EdgeInsets margin;
    private Alignment alignment;
    private Object decoration;

    public void duration(Duration v) {
        this.duration = v;
    }

    public void curve(Curve v) {
        this.curve = v;
    }

    public void width(double v) {
        this.width = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void margin(EdgeInsets v) {
        this.margin = v;
    }

    public void alignment(Alignment v) {
        this.alignment = v;
    }

    public void decoration(Object v) {
        this.decoration = v;
    }
}
