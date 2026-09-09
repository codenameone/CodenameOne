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
package com.codename1.flutter.semantics;

import com.codename1.flutter.Key;
import com.codename1.flutter.Rect;

/**
 * A single accessibility node emitted by a {@code CustomPainter} —  Flutter's
 * {@code CustomPainterSemantics}. It maps a {@link Rect} region of the painted
 * surface to a set of semantic {@code properties} (a
 * {@code SemanticsProperties}). The Rally line chart emits one per data group so
 * screen readers can announce each day's balance. {@code transform} and
 * {@code tags} are captured for API shape.
 */
public class CustomPainterSemantics {

    private Rect rect;
    private Object properties;
    private Object transform;
    private Object tags;
    private Key key;

    public CustomPainterSemantics() {
    }

    public void rect(Rect v) {
        this.rect = v;
    }

    public void properties(Object v) {
        this.properties = v;
    }

    public void transform(Object v) {
        this.transform = v;
    }

    public void tags(Object v) {
        this.tags = v;
    }

    public void key(Key v) {
        this.key = v;
    }

    public Rect getRect() {
        return rect;
    }

    public Object getProperties() {
        return properties;
    }

    public Key getKey() {
        return key;
    }
}
