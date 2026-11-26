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
package com.codename1.art;

/// Fills with an image, stretched or tiled.
///
/// The image's pixel grid is placed in paint space by `getTransform`; one image pixel is one
/// unit before that transform.
public final class ImagePaint extends ArtPaint {
    private final Raster image;
    private Matrix transform = Matrix.IDENTITY;
    private boolean tiled;
    private boolean objectBoundingBox;

    /// Creates an image paint.
    ///
    /// #### Parameters
    ///
    /// - `image`: the pixels
    public ImagePaint(Raster image) {
        this.image = image;
    }

    @Override
    public ArtPaint copy() {
        ImagePaint p = new ImagePaint(image);
        p.transform = transform;
        p.tiled = tiled;
        p.objectBoundingBox = objectBoundingBox;
        return copyBase(p);
    }

    /// The image.
    public Raster getImage() {
        return image;
    }

    /// The image transform.
    public Matrix getTransform() {
        return transform;
    }

    /// Sets the image transform.
    ///
    /// #### Parameters
    ///
    /// - `transform`: places the image in paint space
    public void setTransform(Matrix transform) {
        this.transform = transform == null ? Matrix.IDENTITY : transform;
    }

    /// True when the image repeats.
    public boolean isTiled() {
        return tiled;
    }

    /// Sets whether the image repeats.
    ///
    /// #### Parameters
    ///
    /// - `tiled`: true to repeat
    public void setTiled(boolean tiled) {
        this.tiled = tiled;
    }

    /// True when the transform maps the unit square onto the bounding box first, so a
    /// transform of `scale(1/w, 1/h)` stretches the image over the shape.
    public boolean isObjectBoundingBox() {
        return objectBoundingBox;
    }

    /// Sets whether the transform is relative to the bounding box.
    ///
    /// #### Parameters
    ///
    /// - `objectBoundingBox`: true to stretch over the shape's bounds
    public void setObjectBoundingBox(boolean objectBoundingBox) {
        this.objectBoundingBox = objectBoundingBox;
    }

    /// Stretches the image over a rectangle.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    public void fit(double x, double y, double w, double h) {
        int iw = Math.max(1, image.getWidth());
        int ih = Math.max(1, image.getHeight());
        transform = new Matrix(w / iw, 0, 0, h / ih, x - image.getX() * w / iw, y - image.getY() * h / ih);
    }

    @Override
    public int colorAt(double x, double y, double[] bbox) {
        Matrix m = transform;
        if (objectBoundingBox && bbox != null) {
            m = new Matrix(bbox[2], 0, 0, bbox[3], bbox[0], bbox[1]).multiply(transform);
        }
        Matrix inv = m.invert();
        if (inv == null || image.getWidth() == 0 || image.getHeight() == 0) {
            return 0;
        }
        double ix = inv.mapX(x, y) - 0.5;
        double iy = inv.mapY(x, y) - 0.5;
        if (tiled) {
            double w = image.getWidth();
            double h = image.getHeight();
            ix = image.getX() + mod(ix - image.getX(), w);
            iy = image.getY() + mod(iy - image.getY(), h);
        }
        return image.sample(ix, iy);
    }

    private static double mod(double v, double m) {
        double r = v % m;
        return r < 0 ? r + m : r;
    }
}
