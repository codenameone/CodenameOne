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

import com.codename1.wordprocessing.Picture;

/// A picture on a slide, optionally cropped and outlined. The image is a word processing
/// `Picture`, so it is decoded, cached and exported by the same code a document's pictures
/// use.
public final class PictureElement extends SlideElement {
    /// The image.
    public final Picture picture;

    /// The fraction of the image cut from the left, 0 to 1.
    public final float cropLeft;

    /// The fraction cut from the top.
    public final float cropTop;

    /// The fraction cut from the right.
    public final float cropRight;

    /// The fraction cut from the bottom.
    public final float cropBottom;

    /// The outline.
    public final LineStyle line;

    /// A picture filling a box.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `picture`: the image, not null
    ///
    /// - `x`: left edge in points
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width
    ///
    /// - `height`: height
    public PictureElement(String id, Picture picture, float x, float y, float width,
            float height) {
        this(id, null, picture, x, y, width, height, 0, false, false, 0, 0, 0, 0, null);
    }

    /// A picture with every property.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `name`: a name, or null
    ///
    /// - `picture`: the image
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
    /// - `cropLeft`: fraction cut from the left
    ///
    /// - `cropTop`: fraction cut from the top
    ///
    /// - `cropRight`: fraction cut from the right
    ///
    /// - `cropBottom`: fraction cut from the bottom
    ///
    /// - `line`: the outline, or null
    public PictureElement(String id, String name, Picture picture, float x, float y,
            float width, float height, float rotation, boolean flipH, boolean flipV,
            float cropLeft, float cropTop, float cropRight, float cropBottom, LineStyle line) {
        super(id, name, x, y, width, height, rotation, flipH, flipV);
        if (picture == null) {
            throw new IllegalArgumentException("A picture element needs a picture");
        }
        this.picture = picture;
        this.cropLeft = crop(cropLeft);
        this.cropTop = crop(cropTop);
        this.cropRight = crop(cropRight);
        this.cropBottom = crop(cropBottom);
        this.line = line == null ? LineStyle.NONE : line;
    }

    private static float crop(float f) {
        return Float.isNaN(f) || f < 0 ? 0 : f > 0.99f ? 0.99f : f;
    }

    /// True when part of the image is cut away.
    ///
    /// #### Returns
    ///
    /// true when cropped
    public boolean isCropped() {
        return cropLeft > 0 || cropTop > 0 || cropRight > 0 || cropBottom > 0;
    }

    @Override
    public SlideElement withTransform(float nx, float ny, float nw, float nh, float rot,
            boolean fh, boolean fv) {
        return new PictureElement(id, name, picture, nx, ny, nw, nh, rot, fh, fv, cropLeft,
                cropTop, cropRight, cropBottom, line);
    }

    @Override
    public SlideElement withId(String value) {
        return new PictureElement(value, name, picture, x, y, width, height, rotation, flipH,
                flipV, cropLeft, cropTop, cropRight, cropBottom, line);
    }

    /// This picture cropped.
    ///
    /// #### Parameters
    ///
    /// - `l`: fraction from the left
    ///
    /// - `t`: from the top
    ///
    /// - `r`: from the right
    ///
    /// - `b`: from the bottom
    ///
    /// #### Returns
    ///
    /// the picture element
    public PictureElement withCrop(float l, float t, float r, float b) {
        return new PictureElement(id, name, picture, x, y, width, height, rotation, flipH, flipV,
                l, t, r, b, line);
    }

    @Override
    public String plainText() {
        return picture.description == null ? "" : picture.description;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PictureElement)) {
            return false;
        }
        PictureElement p = (PictureElement) o;
        return sameFrame(p) && picture.equals(p.picture) && cropLeft == p.cropLeft
                && cropTop == p.cropTop && cropRight == p.cropRight && cropBottom == p.cropBottom
                && line.equals(p.line);
    }

    @Override
    public int hashCode() {
        return frameHash() * 31 + picture.hashCode();
    }
}
