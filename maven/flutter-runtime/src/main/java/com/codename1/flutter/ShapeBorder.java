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
package com.codename1.flutter;

/**
 * The base class for shape outlines that can paint a border and clip a shape —
 * Flutter's {@code ShapeBorder}.
 */
public abstract class ShapeBorder {

    /**
     * Paints this outline -- Flutter's {@code ShapeBorder.paint}.
     *
     * <p>Declared HERE so an application's own border overrides it and can be
     * called back. It was absent, so a subclass that draws itself (Shrine's
     * CutCornersBorder chamfers its corners with a Path) declared a paint method
     * that overrode nothing and was never reached, and the runtime fell back to
     * approximating the outline with a rounded rectangle. Nothing about the
     * chamfer is beyond Codename One -- it is a polygon, and GraphicsCanvas
     * already turns a Flutter Path into one; the hook to ask for it was what was
     * missing.</p>
     *
     * <p>The default draws nothing, so a border with no opinion is unchanged.</p>
     */
    public void paint(Canvas canvas, Rect rect, Double gapStart, double gapExtent,
            double gapPercentage, TextDirection textDirection) {
    }

}
