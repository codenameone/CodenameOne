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
package com.codename1.flutter.rendering;

import com.codename1.flutter.Canvas;
import com.codename1.flutter.Offset;
import com.codename1.flutter.Rect;

/**
 * The canvas + child-painting handle passed to {@code RenderObject.paint} —
 * Flutter's {@code PaintingContext}. The sliders demo's custom slider shapes
 * read {@link #canvas()} to draw the thumb / value indicator.
 */
public class PaintingContext {

    private final Canvas canvas;
    private final Rect estimatedBounds;

    public PaintingContext() {
        this(new Canvas(), Rect.zero);
    }

    public PaintingContext(Canvas canvas, Rect estimatedBounds) {
        this.canvas = canvas;
        this.estimatedBounds = estimatedBounds;
    }

    /** The canvas onto which painting should be done. */
    public Canvas canvas() {
        return canvas;
    }

    /** Paints a child render object at the given offset (no-op stub). */
    public void paintChild(RenderObject child, Offset offset) {
    }

    /** An estimate of the bounds within which painting will happen. */
    public Rect estimatedBounds() {
        return estimatedBounds;
    }
}
