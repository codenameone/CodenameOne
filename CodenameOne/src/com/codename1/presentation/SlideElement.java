/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.presentation;

/// Something placed on a slide: a shape, a picture, a table, a chart, a diagram or a group of
/// them. Every element has an id unique in its presentation, a box in points from the slide's
/// top left corner, and a rotation about the box's centre. Elements are immutable; the
/// `with...` methods answer changed copies.
public abstract class SlideElement {
    /// The id, unique within the presentation.
    public final String id;

    /// A name for people and screen readers, or null.
    public final String name;

    /// The box's left edge in points.
    public final float x;

    /// The box's top edge in points.
    public final float y;

    /// The box's width in points, at least 0.
    public final float width;

    /// The box's height in points, at least 0.
    public final float height;

    /// Clockwise rotation in degrees about the box's centre.
    public final float rotation;

    /// True when mirrored left to right.
    public final boolean flipH;

    /// True when mirrored top to bottom.
    public final boolean flipV;

    /// Base constructor.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id, not empty
    ///
    /// - `name`: a name, or null
    ///
    /// - `x`: left edge in points
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width
    ///
    /// - `height`: height
    ///
    /// - `rotation`: degrees clockwise
    ///
    /// - `flipH`: mirrored horizontally
    ///
    /// - `flipV`: mirrored vertically
    protected SlideElement(String id, String name, float x, float y, float width, float height,
            float rotation, boolean flipH, boolean flipV) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("An element needs an id");
        }
        this.id = id;
        this.name = name;
        this.x = finite(x);
        this.y = finite(y);
        this.width = Math.max(0, finite(width));
        this.height = Math.max(0, finite(height));
        float r = finite(rotation) % 360;
        this.rotation = r < 0 ? r + 360 : r;
        this.flipH = flipH;
        this.flipV = flipV;
    }

    private static float finite(float f) {
        return Float.isNaN(f) || Float.isInfinite(f) ? 0 : f;
    }

    /// This element moved, resized, rotated or flipped.
    ///
    /// #### Parameters
    ///
    /// - `x`: left edge
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width
    ///
    /// - `height`: height
    ///
    /// - `rotation`: degrees clockwise
    ///
    /// - `flipH`: mirrored horizontally
    ///
    /// - `flipV`: mirrored vertically
    ///
    /// #### Returns
    ///
    /// the changed element
    public abstract SlideElement withTransform(float x, float y, float width, float height,
            float rotation, boolean flipH, boolean flipV);

    /// This element with another id, for copies.
    ///
    /// #### Parameters
    ///
    /// - `value`: the id
    ///
    /// #### Returns
    ///
    /// the element
    public abstract SlideElement withId(String value);

    /// This element moved and resized, keeping its rotation and flips.
    ///
    /// #### Parameters
    ///
    /// - `nx`: left edge
    ///
    /// - `ny`: top edge
    ///
    /// - `nw`: width
    ///
    /// - `nh`: height
    ///
    /// #### Returns
    ///
    /// the element
    public SlideElement withBounds(float nx, float ny, float nw, float nh) {
        return withTransform(nx, ny, nw, nh, rotation, flipH, flipV);
    }

    /// This element rotated to an angle.
    ///
    /// #### Parameters
    ///
    /// - `degrees`: clockwise
    ///
    /// #### Returns
    ///
    /// the element
    public SlideElement withRotation(float degrees) {
        return withTransform(x, y, width, height, degrees, flipH, flipV);
    }

    /// The text of the element for search, outlines and screen readers.
    ///
    /// #### Returns
    ///
    /// the text, empty when there is none
    public String plainText() {
        return "";
    }

    /// True when the point, in slide points, is inside the element's rotated box.
    ///
    /// #### Parameters
    ///
    /// - `px`: x
    ///
    /// - `py`: y
    ///
    /// #### Returns
    ///
    /// true when inside
    public boolean contains(float px, float py) {
        float cx = x + width / 2;
        float cy = y + height / 2;
        double a = -Math.toRadians(rotation);
        double dx = px - cx;
        double dy = py - cy;
        double rx = dx * Math.cos(a) - dy * Math.sin(a);
        double ry = dx * Math.sin(a) + dy * Math.cos(a);
        return Math.abs(rx) <= width / 2 + 0.5 && Math.abs(ry) <= height / 2 + 0.5;
    }

    /// True when the base fields equal another element's.
    ///
    /// #### Parameters
    ///
    /// - `e`: the other
    ///
    /// #### Returns
    ///
    /// true when equal
    protected final boolean sameFrame(SlideElement e) {
        return id.equals(e.id) && (name == null ? e.name == null : name.equals(e.name))
                && x == e.x && y == e.y && width == e.width && height == e.height
                && rotation == e.rotation && flipH == e.flipH && flipV == e.flipV;
    }

    /// A hash of the base fields.
    ///
    /// #### Returns
    ///
    /// the hash
    protected final int frameHash() {
        return ((id.hashCode() * 31 + Float.floatToIntBits(x)) * 31 + Float.floatToIntBits(y))
                * 31 + Float.floatToIntBits(width) * 7 + Float.floatToIntBits(height);
    }
}
