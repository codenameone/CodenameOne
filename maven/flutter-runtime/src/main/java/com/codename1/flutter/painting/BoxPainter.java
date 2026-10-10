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
package com.codename1.flutter.painting;

import com.codename1.flutter.Canvas;
import com.codename1.flutter.ImageConfiguration;
import com.codename1.flutter.Offset;

/**
 * The object a {@code Decoration} produces to paint itself — Flutter's
 * {@code BoxPainter}. A decoration returns one from {@code createBoxPainter};
 * the render layer calls {@link #paint} with the {@link Canvas}, the top-left
 * {@link Offset} of the box and an {@link ImageConfiguration} carrying the box
 * size. The tab-indicator and Rally pie-chart decorations subclass this to draw
 * custom borders. The optional repaint {@code onChanged} callback is captured by
 * the decoration; {@link #dispose()} releases any held resources.
 */
public abstract class BoxPainter {

    /**
     * Paints the decoration onto {@code canvas}. The box occupies the rectangle
     * whose top-left is {@code offset} and whose size is
     * {@code configuration.size}.
     */
    public abstract void paint(Canvas canvas, Offset offset, ImageConfiguration configuration);

    /** Releases resources held by this painter. */
    public void dispose() {
    }
}
