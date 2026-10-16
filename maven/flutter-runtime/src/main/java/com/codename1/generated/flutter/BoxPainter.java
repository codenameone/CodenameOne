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
package com.codename1.generated.flutter;

import com.codename1.flutter.Canvas;
import com.codename1.flutter.ImageConfiguration;
import com.codename1.flutter.Offset;

/**
 * The object that paints a {@link com.codename1.flutter.Decoration}, created by
 * {@code Decoration.createBoxPainter} — Flutter's {@code BoxPainter}. The Rally
 * pie-chart outline and the Crane tab indicator subclass it to draw directly on
 * the canvas.
 *
 * <p>Lives in the transpiler's generated package because new_gallery's custom
 * {@code Decoration}s reference it unqualified (the Flutter SDK type carries no
 * {@code @JavaName} mapping); the concrete painters emitted next to it extend
 * this base.</p>
 */
public abstract class BoxPainter {

    /**
     * Paints the decoration onto {@code canvas} at {@code offset} for the box
     * described by {@code configuration} (notably its size).
     */
    public abstract void paint(Canvas canvas, Offset offset, ImageConfiguration configuration);

    /** Releases resources held by this painter (no-op in this milestone). */
    public void dispose() {
    }
}
