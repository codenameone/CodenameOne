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
package com.codename1.backend.image;

/// A mutable image held in memory as 32-bit ARGB pixels.
///
/// Pixels are **not** premultiplied: `0x80FF0000` is half transparent pure
/// red. They are stored row by row with no padding, so pixel `(x, y)` is
/// `getPixels()[y * getWidth() + x]`.
///
/// Unlike `com.codename1.ui.Image` this class needs no display and no
/// platform: it runs on a device, in the simulator and on a Codename One
/// server alike. Draw into it with [RasterGraphics], read and write files with
/// [PngCodec] and [JpegCodec], and hand the pixels to the UI with
/// `Image.createImage(raster.getPixels(), w, h)` when you need one.
public final class RasterImage {
    private final int width;
    private final int height;
    private final int[] pixels;

    /// Creates a fully transparent image.
    ///
    /// #### Parameters
    ///
    /// - `width`: width in pixels, at least 1
    /// - `height`: height in pixels, at least 1
    public RasterImage(int width, int height) {
        this(width, height, new int[checkSize(width, height)]);
    }

    /// Wraps an existing ARGB array without copying it.
    ///
    /// #### Parameters
    ///
    /// - `width`: width in pixels
    /// - `height`: height in pixels
    /// - `argb`: at least `width * height` non-premultiplied ARGB pixels
    public RasterImage(int width, int height, int[] argb) {
        if (argb.length < checkSize(width, height)) {
            throw new IllegalArgumentException("pixel array too small");
        }
        this.width = width;
        this.height = height;
        this.pixels = argb;
    }

    private static int checkSize(int w, int h) {
        if (w <= 0 || h <= 0 || (long) w * h > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("bad image size " + w + "x" + h);
        }
        return w * h;
    }

    /// Returns the width in pixels.
    public int getWidth() {
        return width;
    }

    /// Returns the height in pixels.
    public int getHeight() {
        return height;
    }

    /// Returns the live pixel array; writes to it change the image.
    public int[] getPixels() {
        return pixels;
    }

    /// Returns the ARGB value at `(x, y)`.
    ///
    /// #### Parameters
    ///
    /// - `x`: column
    /// - `y`: row
    ///
    /// #### Returns
    ///
    /// the pixel; an `ArrayIndexOutOfBoundsException` or
    /// `IllegalArgumentException` for a point outside the image
    public int getRGB(int x, int y) {
        return pixels[index(x, y)];
    }

    /// Sets the ARGB value at `(x, y)`, replacing what was there.
    ///
    /// #### Parameters
    ///
    /// - `x`: column
    /// - `y`: row
    /// - `argb`: the new pixel
    public void setRGB(int x, int y, int argb) {
        pixels[index(x, y)] = argb;
    }

    private int index(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new IllegalArgumentException("pixel (" + x + ", " + y + ") outside " + width + "x" + height);
        }
        return y * width + x;
    }

    /// Fills the whole image with one ARGB value.
    ///
    /// #### Parameters
    ///
    /// - `argb`: the pixel value
    public void fill(int argb) {
        for (int i = 0, n = width * height; i < n; i++) {
            pixels[i] = argb;
        }
    }

    /// Returns a copy of this image.
    public RasterImage copy() {
        return subImage(0, 0, width, height);
    }

    /// Returns a copy of a rectangle of this image.
    ///
    /// #### Parameters
    ///
    /// - `x`: left edge
    /// - `y`: top edge
    /// - `w`: width
    /// - `h`: height; the rectangle must lie inside the image
    ///
    /// #### Returns
    ///
    /// a new image
    public RasterImage subImage(int x, int y, int w, int h) {
        if (x < 0 || y < 0 || w <= 0 || h <= 0 || x + w > width || y + h > height) {
            throw new IllegalArgumentException("rectangle outside the image");
        }
        int[] out = new int[w * h];
        for (int r = 0; r < h; r++) {
            System.arraycopy(pixels, (y + r) * width + x, out, r * w, w);
        }
        return new RasterImage(w, h, out);
    }

    /// Returns this image resized with one of the [Resampler] filters.
    ///
    /// Color is filtered premultiplied, so transparent pixels do not darken
    /// the edges of what is next to them. The convolution filters give the
    /// same bytes as Pillow's `resize`.
    ///
    /// #### Parameters
    ///
    /// - `w`: new width
    /// - `h`: new height
    /// - `filter`: `Resampler.NEAREST`, `BILINEAR`, `BICUBIC`, `LANCZOS`,
    ///   `BOX` or `HAMMING`
    ///
    /// #### Returns
    ///
    /// a new image
    public RasterImage scale(int w, int h, int filter) {
        checkSize(w, h);
        byte[] rgba = Resampler.premultiply(toRGBA(), 4);
        byte[] out = Resampler.resample(rgba, width, height, 4, w, h, new double[] {0, 0, width, height}, filter);
        return fromRGBA(w, h, Resampler.unpremultiply(out, 4));
    }

    /// Returns the pixels as `R, G, B, A` bytes, row by row.
    public byte[] toRGBA() {
        int n = width * height;
        byte[] d = new byte[n * 4];
        for (int p = 0; p < n; p++) {
            int c = pixels[p];
            d[p * 4] = (byte) (c >> 16);
            d[p * 4 + 1] = (byte) (c >> 8);
            d[p * 4 + 2] = (byte) c;
            d[p * 4 + 3] = (byte) (c >>> 24);
        }
        return d;
    }

    /// Builds an image from `R, G, B, A` bytes, row by row.
    ///
    /// #### Parameters
    ///
    /// - `w`: width
    /// - `h`: height
    /// - `rgba`: `w * h * 4` bytes, not premultiplied
    ///
    /// #### Returns
    ///
    /// a new image
    public static RasterImage fromRGBA(int w, int h, byte[] rgba) {
        int n = checkSize(w, h);
        int[] px = new int[n];
        for (int p = 0; p < n; p++) {
            px[p] = (rgba[p * 4 + 3] & 0xff) << 24 | (rgba[p * 4] & 0xff) << 16
                    | (rgba[p * 4 + 1] & 0xff) << 8 | (rgba[p * 4 + 2] & 0xff);
        }
        return new RasterImage(w, h, px);
    }

    /// Returns this image turned 90 degrees clockwise.
    public RasterImage rotate90() {
        int[] out = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out[x * height + (height - 1 - y)] = pixels[y * width + x];
            }
        }
        return new RasterImage(height, width, out);
    }

    /// Returns this image turned 180 degrees.
    public RasterImage rotate180() {
        int n = width * height;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[n - 1 - i] = pixels[i];
        }
        return new RasterImage(width, height, out);
    }

    /// Returns this image turned 90 degrees counterclockwise.
    public RasterImage rotate270() {
        int[] out = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out[(width - 1 - x) * height + y] = pixels[y * width + x];
            }
        }
        return new RasterImage(height, width, out);
    }

    /// Returns this image mirrored left to right.
    public RasterImage flipHorizontal() {
        int[] out = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out[y * width + width - 1 - x] = pixels[y * width + x];
            }
        }
        return new RasterImage(width, height, out);
    }

    /// Returns this image mirrored top to bottom.
    public RasterImage flipVertical() {
        int[] out = new int[width * height];
        for (int y = 0; y < height; y++) {
            System.arraycopy(pixels, y * width, out, (height - 1 - y) * width, width);
        }
        return new RasterImage(width, height, out);
    }

    /// Returns a [RasterGraphics] that draws into this image.
    public RasterGraphics createGraphics() {
        return new RasterGraphics(this);
    }
}
