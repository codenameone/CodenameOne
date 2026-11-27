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

/// A bitmap layer. One image pixel is one unit of the node's space, placed at the raster's
/// own `x, y`.
public class RasterNode extends ArtNode {
    private Raster image;

    /// Creates a raster layer.
    ///
    /// #### Parameters
    ///
    /// - `image`: the pixels
    public RasterNode(Raster image) {
        this.image = image;
    }

    /// The pixels. Changes write through; call `ArtModel.changed` after editing them.
    public Raster getImage() {
        return image;
    }

    /// Replaces the pixels.
    ///
    /// #### Parameters
    ///
    /// - `image`: the new pixels
    public void setImage(Raster image) {
        this.image = image;
    }

    @Override
    public double[] getLocalBounds() {
        if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
            return null;
        }
        return new double[]{image.getX(), image.getY(), image.getWidth(), image.getHeight()};
    }

    @Override
    ArtNode snapshot() {
        return copyBase(new RasterNode(image));
    }

    @Override
    public ArtNode copy() {
        return copyBase(new RasterNode(image == null ? null : image.copy()));
    }
}
