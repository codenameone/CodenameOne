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

/// A rectangle of non-premultiplied `0xAARRGGBB` pixels with a position.
///
/// The renderer paints every layer into a `Raster` and composites them with `Compositor`, so
/// a document looks the same on every port and in tests that have no display.
public final class Raster {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int[] pixels;

    /// The largest number of pixels a raster may hold: 64 megapixels.
    public static final int MAX_PIXELS = 64 * 1024 * 1024;

    /// Creates a transparent raster.
    ///
    /// #### Parameters
    ///
    /// - `x`: left edge in device pixels
    /// - `y`: top edge in device pixels
    /// - `width`: width, at least 0
    /// - `height`: height, at least 0
    public Raster(int x, int y, int width, int height) {
        this(x, y, width, height, null);
    }

    /// Wraps existing pixels.
    ///
    /// #### Parameters
    ///
    /// - `x`: left edge
    /// - `y`: top edge
    /// - `width`: width
    /// - `height`: height
    /// - `pixels`: `width * height` pixels, row major; null for transparent
    public Raster(int x, int y, int width, int height, int[] pixels) {
        if (width < 0 || height < 0 || (long) width * height > MAX_PIXELS) {
            throw new IllegalArgumentException("Raster too large: " + width + "x" + height);
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        if (pixels == null) {
            this.pixels = new int[width * height];
        } else {
            if (pixels.length < width * height) {
                throw new IllegalArgumentException("pixel array too short");
            }
            this.pixels = pixels;
        }
    }

    /// Left edge.
    public int getX() {
        return x;
    }

    /// Top edge.
    public int getY() {
        return y;
    }

    /// Width in pixels.
    public int getWidth() {
        return width;
    }

    /// Height in pixels.
    public int getHeight() {
        return height;
    }

    /// The pixel array, row major. Changes write through.
    public int[] getPixels() {
        return pixels;
    }

    /// The pixel at absolute coordinates, transparent outside.
    ///
    /// #### Parameters
    ///
    /// - `px`: absolute x
    /// - `py`: absolute y
    public int get(int px, int py) {
        int lx = px - x;
        int ly = py - y;
        if (lx < 0 || ly < 0 || lx >= width || ly >= height) {
            return 0;
        }
        return pixels[ly * width + lx];
    }

    /// A deep copy.
    public Raster copy() {
        int[] p = new int[pixels.length];
        System.arraycopy(pixels, 0, p, 0, pixels.length);
        return new Raster(x, y, width, height, p);
    }

    /// True when every pixel is fully transparent.
    public boolean isEmpty() {
        for (int i = 0; i < pixels.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            if ((pixels[i] >>> 24) != 0) {
                return false;
            }
        }
        return true;
    }

    /// Returns a raster covering `[nx, ny, nw, nh]` with this one's pixels copied in.
    ///
    /// #### Parameters
    ///
    /// - `nx`: new left edge
    /// - `ny`: new top edge
    /// - `nw`: new width
    /// - `nh`: new height
    public Raster resize(int nx, int ny, int nw, int nh) {
        Raster r = new Raster(nx, ny, nw, nh);
        r.drawRaw(this);
        return r;
    }

    /// Copies `src` into this raster where they overlap, replacing pixels.
    void drawRaw(Raster src) {
        int x0 = Math.max(x, src.x);
        int y0 = Math.max(y, src.y);
        int x1 = Math.min(x + width, src.x + src.width);
        int y1 = Math.min(y + height, src.y + src.height);
        for (int yy = y0; yy < y1; yy++) {
            if (x1 > x0) {
                System.arraycopy(src.pixels, (yy - src.y) * src.width + (x0 - src.x),
                        pixels, (yy - y) * width + (x0 - x), x1 - x0);
            }
        }
    }

    /// Composites `src` onto this raster where they overlap.
    ///
    /// #### Parameters
    ///
    /// - `src`: the layer
    /// - `mode`: how colors combine
    /// - `opacity`: the layer's opacity, 0 to 1
    public void composite(Raster src, BlendMode mode, float opacity) {
        int x0 = Math.max(x, src.x);
        int y0 = Math.max(y, src.y);
        int x1 = Math.min(x + width, src.x + src.width);
        int y1 = Math.min(y + height, src.y + src.height);
        boolean normal = mode == BlendMode.NORMAL;
        double[] scratch = normal ? null : new double[Compositor.SCRATCH];
        for (int yy = y0; yy < y1; yy++) {
            int si = (yy - src.y) * src.width + (x0 - src.x);
            int di = (yy - y) * width + (x0 - x);
            for (int xx = x0; xx < x1; xx++) {
                int s = src.pixels[si++];
                if ((s >>> 24) != 0) {
                    pixels[di] = normal ? over(s, pixels[di], opacity)
                            : Compositor.blendPixel(s, pixels[di], mode, opacity, scratch, xx * 73856093 ^ yy * 19349663);
                }
                di++;
            }
        }
    }

    /// Fast source-over for the normal mode.
    static int over(int s, int d, float opacity) {
        int sa = (int) ((s >>> 24) * opacity + 0.5f);
        if (sa <= 0) {
            return d;
        }
        int da = d >>> 24;
        if (sa >= 255 || da == 0) {
            return (sa << 24) | (s & 0xffffff);
        }
        int oa = sa * 255 + da * (255 - sa);
        int r = (((s >> 16) & 0xff) * sa * 255 + ((d >> 16) & 0xff) * da * (255 - sa)) / oa;
        int g = (((s >> 8) & 0xff) * sa * 255 + ((d >> 8) & 0xff) * da * (255 - sa)) / oa;
        int b = ((s & 0xff) * sa * 255 + (d & 0xff) * da * (255 - sa)) / oa;
        return (((oa + 127) / 255) << 24) | (r << 16) | (g << 8) | b;
    }

    /// Multiplies each pixel's alpha by a coverage mask of the same geometry (0..255).
    ///
    /// #### Parameters
    ///
    /// - `mask`: one byte of coverage per pixel
    public void applyMask(byte[] mask) {
        for (int i = 0; i < pixels.length; i++) {
            int m = mask[i] & 0xff;
            if (m != 255) {
                int p = pixels[i];
                int a = ((p >>> 24) * m + 127) / 255;
                pixels[i] = (a << 24) | (p & 0xffffff);
            }
        }
    }

    /// Returns this raster drawn through a transform, sampled bilinearly, clipped to the
    /// device rectangle `clip` (x, y, w, h) when it is not null.
    ///
    /// #### Parameters
    ///
    /// - `m`: maps this raster's absolute coordinates to device coordinates
    /// - `clip`: limits the result; may be null
    public Raster transform(Matrix m, int[] clip) {
        if (m.isIdentity() && clip == null) {
            return this;
        }
        double[] xs = {x, x + width, x, x + width};
        double[] ys = {y, y, y + height, y + height};
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            double tx = m.mapX(xs[i], ys[i]);
            double ty = m.mapY(xs[i], ys[i]);
            minX = Math.min(minX, tx);
            minY = Math.min(minY, ty);
            maxX = Math.max(maxX, tx);
            maxY = Math.max(maxY, ty);
        }
        int nx = (int) Math.floor(minX);
        int ny = (int) Math.floor(minY);
        int nx1 = (int) Math.ceil(maxX);
        int ny1 = (int) Math.ceil(maxY);
        if (clip != null) {
            nx = Math.max(nx, clip[0]);
            ny = Math.max(ny, clip[1]);
            nx1 = Math.min(nx1, clip[0] + clip[2]);
            ny1 = Math.min(ny1, clip[1] + clip[3]);
        }
        if (nx1 <= nx || ny1 <= ny) {
            return new Raster(nx, ny, 0, 0);
        }
        Raster out = new Raster(nx, ny, nx1 - nx, ny1 - ny);
        Matrix inv = m.invert();
        if (inv == null) {
            return out;
        }
        int[] op = out.pixels;
        int i = 0;
        for (int yy = ny; yy < ny1; yy++) {
            for (int xx = nx; xx < nx1; xx++) {
                double sx = inv.mapX(xx + 0.5, yy + 0.5) - 0.5;
                double sy = inv.mapY(xx + 0.5, yy + 0.5) - 0.5;
                op[i++] = sample(sx, sy);
            }
        }
        return out;
    }

    /// Bilinear sample at absolute pixel-center coordinates.
    int sample(double sx, double sy) {
        int x0 = (int) Math.floor(sx);
        int y0 = (int) Math.floor(sy);
        double fx = sx - x0;
        double fy = sy - y0;
        int p00 = get(x0, y0);
        int p10 = get(x0 + 1, y0);
        int p01 = get(x0, y0 + 1);
        int p11 = get(x0 + 1, y0 + 1);
        if (p00 == p10 && p00 == p01 && p00 == p11) {
            return p00;
        }
        double w00 = (1 - fx) * (1 - fy);
        double w10 = fx * (1 - fy);
        double w01 = (1 - fx) * fy;
        double w11 = fx * fy;
        double a00 = (p00 >>> 24) * w00;
        double a10 = (p10 >>> 24) * w10;
        double a01 = (p01 >>> 24) * w01;
        double a11 = (p11 >>> 24) * w11;
        double a = a00 + a10 + a01 + a11;
        if (a <= 0.001) {
            return 0;
        }
        int out = clamp(a) << 24;
        for (int sh = 16; sh >= 0; sh -= 8) {
            double v = ((p00 >> sh) & 0xff) * a00 + ((p10 >> sh) & 0xff) * a10
                    + ((p01 >> sh) & 0xff) * a01 + ((p11 >> sh) & 0xff) * a11;
            out |= clamp(v / a) << sh;
        }
        return out;
    }

    static int clamp(double v) {
        int i = (int) (v + 0.5);
        if (i < 0) {
            return 0;
        }
        return i > 255 ? 255 : i;
    }
}
