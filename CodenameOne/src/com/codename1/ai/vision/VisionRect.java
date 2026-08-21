/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.ai.vision;

import com.codename1.ui.Component;
import com.codename1.ui.geom.Rectangle;

/// Immutable normalized rectangle using a top-left origin. X/Y identify the
/// upper-left corner and width/height are fractions of the oriented input
/// dimensions. {@link #EMPTY} represents unavailable geometry.
///
/// {@link #toBounds(int, int, int, int)} converts one back to pixels for
/// drawing:
///
/// ```java
/// public void paint(Graphics g) {
///     super.paint(g);
///     g.setColor(0x34c759);
///     for (Barcode code : lastCodes) {
///         Rectangle r = code.getBounds().toBounds(this);
///         g.drawRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), 3);
///     }
/// }
/// ```
public final class VisionRect {
    public static final VisionRect EMPTY = new VisionRect(0, 0, 0, 0);

    private final float x;
    private final float y;
    private final float width;
    private final float height;

    /// Creates a rectangle in the oriented input image's top-left coordinate space.
    /// @param x left coordinate
    /// @param y top coordinate
    /// @param width non-negative rectangle width
    /// @param height non-negative rectangle height
    public VisionRect(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /// @return normalized left coordinate
    public float getX() {
        return x;
    }

    /// @return normalized top coordinate
    public float getY() {
        return y;
    }

    /// @return width as a fraction of oriented input width
    public float getWidth() {
        return width;
    }

    /// @return height as a fraction of oriented input height
    public float getHeight() {
        return height;
    }

    /// Whether this rectangle carries no geometry, which is what a backend
    /// that located a result without reporting where reports.
    ///
    /// @return {@code true} when the rectangle has no area
    public boolean isEmpty() {
        return width <= 0 || height <= 0;
    }

    /// Maps this normalized rectangle onto a pixel rectangle.
    ///
    /// The mapping stretches the whole 0..1 range across {@code width} and
    /// {@code height}. That is correct when the destination has the same
    /// aspect ratio as the analyzed image, which is the usual case for an
    /// image drawn to fit. A live preview scaled with
    /// {@link com.codename1.camera.ScaleType#CROP} does not: pass the
    /// rectangle the frame actually occupies rather than the component's own
    /// bounds.
    ///
    /// @param x left edge of the destination rectangle in pixels
    /// @param y top edge of the destination rectangle in pixels
    /// @param width destination width in pixels
    /// @param height destination height in pixels
    /// @return the corresponding pixel rectangle, rounded to whole pixels
    public Rectangle toBounds(int x, int y, int width, int height) {
        int left = Math.round(this.x * width);
        int top = Math.round(this.y * height);
        int right = Math.round((this.x + this.width) * width);
        int bottom = Math.round((this.y + this.height) * height);
        return new Rectangle(x + left, y + top, right - left, bottom - top);
    }

    /// Maps this normalized rectangle onto a component's absolute on-screen
    /// bounds, which is the coordinate space a {@code paint} method draws in.
    ///
    /// @param target component the analyzed image is displayed in
    /// @return the corresponding absolute pixel rectangle
    /// @throws NullPointerException if {@code target} is {@code null}
    public Rectangle toBounds(Component target) {
        if (target == null) {
            throw new NullPointerException("target");
        }
        return toBounds(target.getAbsoluteX(), target.getAbsoluteY(),
                target.getWidth(), target.getHeight());
    }
}
