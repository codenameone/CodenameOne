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

/// What fills a shape, a stroke or text: a color, a gradient or an image.
public abstract class ArtPaint {
    private float opacity = 1;
    private BlendMode blendMode = BlendMode.NORMAL;
    private boolean enabled = true;

    /// The paint's own opacity, 0 to 1.
    public float getOpacity() {
        return opacity;
    }

    /// Sets the paint's opacity.
    ///
    /// #### Parameters
    ///
    /// - `opacity`: 0 to 1
    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    /// How this paint combines with the paints listed before it on the same shape.
    public BlendMode getBlendMode() {
        return blendMode;
    }

    /// Sets the blend mode.
    ///
    /// #### Parameters
    ///
    /// - `blendMode`: the mode
    public void setBlendMode(BlendMode blendMode) {
        this.blendMode = blendMode == null ? BlendMode.NORMAL : blendMode;
    }

    /// False when the paint is kept but not drawn.
    public boolean isEnabled() {
        return enabled;
    }

    /// Turns the paint on or off.
    ///
    /// #### Parameters
    ///
    /// - `enabled`: true to draw it
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /// A deep copy.
    public abstract ArtPaint copy();

    /// Copies the shared properties into `p` and returns it.
    protected ArtPaint copyBase(ArtPaint p) {
        p.opacity = opacity;
        p.blendMode = blendMode;
        p.enabled = enabled;
        return p;
    }

    /// The color at a point of paint space.
    ///
    /// #### Parameters
    ///
    /// - `x`: x in the space of the shape that is painted
    /// - `y`: y in that space
    /// - `bbox`: the shape's bounding box as `x, y, w, h`, for paints relative to it
    public abstract int colorAt(double x, double y, double[] bbox);

    /// Paints a coverage mask.
    Raster shade(Mask mask, Matrix deviceFromLocal, double[] bbox) {
        if (this instanceof SolidPaint) {
            int c = ((SolidPaint) this).getColor();
            int a = (int) ((c >>> 24) * opacity + 0.5f);
            return mask.toRaster((a << 24) | (c & 0xffffff));
        }
        Raster r = new Raster(mask.x, mask.y, mask.w, mask.h);
        Matrix inv = deviceFromLocal.invert();
        if (inv == null) {
            return r;
        }
        int[] p = r.getPixels();
        int i = 0;
        for (int yy = 0; yy < mask.h; yy++) {
            for (int xx = 0; xx < mask.w; xx++) {
                int cov = mask.a[i] & 0xff;
                if (cov != 0) {
                    double dx = mask.x + xx + 0.5;
                    double dy = mask.y + yy + 0.5;
                    int c = colorAt(inv.mapX(dx, dy), inv.mapY(dx, dy), bbox);
                    int a = (int) ((c >>> 24) * cov * opacity / 255 + 0.5f);
                    p[i] = (a << 24) | (c & 0xffffff);
                }
                i++;
            }
        }
        return r;
    }
}
