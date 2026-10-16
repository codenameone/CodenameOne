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

import com.codename1.flutter.TextDirection;

/**
 * Adapts a {@code ShapeBorder} to the {@code CustomClipper} protocol consumed
 * by {@code PhysicalShape} — Flutter's {@code ShapeBorderClipper}. crane's
 * backdrop uses it to round the front layer's top corners. API-shape only for
 * this milestone; the shape is captured for the clipper to apply.
 */
public class ShapeBorderClipper {

    private Object shape;
    private TextDirection textDirection;

    public void shape(Object v) {
        this.shape = v;
    }

    public void textDirection(TextDirection v) {
        this.textDirection = v;
    }

    public Object getShape() {
        return shape;
    }

    public TextDirection getTextDirection() {
        return textDirection;
    }
}
