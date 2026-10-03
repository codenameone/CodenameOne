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
package android.graphics;

import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;

import java.io.IOException;
import java.io.OutputStream;

/// A bitmap, backed by a Codename One `Image`. Pixel access goes through an
/// ARGB array that is written back to a fresh image on the next draw, since
/// Codename One images are not pixel-addressable in place.
public final class Bitmap {

    public enum Config { ALPHA_8, RGB_565, ARGB_4444, ARGB_8888, RGBA_F16, HARDWARE }

    public enum CompressFormat { JPEG, PNG, WEBP, WEBP_LOSSY, WEBP_LOSSLESS }

    public static final int DENSITY_NONE = 0;

    private Image image;
    private final int width;
    private final int height;
    private final boolean mutable;
    private final Config config;
    private int[] pixels;
    private boolean pixelsDirty;
    private boolean recycled;
    private int density = android.util.DisplayMetrics.DENSITY_DEFAULT;

    Bitmap(Image image, boolean mutable, Config config) {
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.mutable = mutable;
        this.config = config == null ? Config.ARGB_8888 : config;
    }

    /// Wraps an existing Codename One image.
    public static Bitmap wrap(Image image) {
        return new Bitmap(image, false, Config.ARGB_8888);
    }

    /// The Codename One image, with any pending pixel writes applied.
    public Image getImage() {
        if (pixelsDirty) {
            image = Image.createImage(pixels, width, height);
            pixelsDirty = false;
        }
        return image;
    }

    public static Bitmap createBitmap(int width, int height, Config config) {
        return new Bitmap(Image.createImage(width, height, 0), true, config);
    }

    public static Bitmap createBitmap(int width, int height, Config config, boolean hasAlpha) {
        return createBitmap(width, height, config);
    }

    public static Bitmap createBitmap(android.util.DisplayMetrics display, int width, int height, Config config) {
        return createBitmap(width, height, config);
    }

    public static Bitmap createBitmap(Bitmap src) {
        return new Bitmap(src.getImage(), false, src.config);
    }

    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height) {
        if (x == 0 && y == 0 && width == source.width && height == source.height) {
            return source;
        }
        return new Bitmap(source.getImage().subImage(x, y, width, height, true), false, source.config);
    }

    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height, Matrix m, boolean filter) {
        Bitmap sub = createBitmap(source, x, y, width, height);
        if (m == null || m.isIdentity()) {
            return sub;
        }
        RectF r = new RectF(0, 0, width, height);
        m.mapRect(r);
        int w = Math.max(1, Math.round(r.width()));
        int h = Math.max(1, Math.round(r.height()));
        return new Bitmap(sub.getImage().scaled(w, h), false, source.config);
    }

    public static Bitmap createBitmap(int[] colors, int width, int height, Config config) {
        return new Bitmap(Image.createImage(colors.clone(), width, height), false, config);
    }

    public static Bitmap createBitmap(int[] colors, int offset, int stride, int width, int height, Config config) {
        int[] px = new int[width * height];
        for (int row = 0; row < height; row++) {
            System.arraycopy(colors, offset + row * stride, px, row * width, width);
        }
        return new Bitmap(Image.createImage(px, width, height), false, config);
    }

    public static Bitmap createScaledBitmap(Bitmap src, int dstWidth, int dstHeight, boolean filter) {
        if (dstWidth == src.width && dstHeight == src.height) {
            return src;
        }
        return new Bitmap(src.getImage().scaled(dstWidth, dstHeight), false, src.config);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getScaledWidth(int targetDensity) {
        return density == DENSITY_NONE || targetDensity == DENSITY_NONE ? width : (width * targetDensity + density / 2) / density;
    }

    public int getScaledHeight(int targetDensity) {
        return density == DENSITY_NONE || targetDensity == DENSITY_NONE ? height : (height * targetDensity + density / 2) / density;
    }

    public int getDensity() {
        return density;
    }

    public void setDensity(int density) {
        this.density = density;
    }

    public boolean isMutable() {
        return mutable;
    }

    public Config getConfig() {
        return config;
    }

    public boolean hasAlpha() {
        return config != Config.RGB_565;
    }

    public void setHasAlpha(boolean hasAlpha) {
    }

    public int getByteCount() {
        return width * height * 4;
    }

    public int getAllocationByteCount() {
        return getByteCount();
    }

    public int getRowBytes() {
        return width * 4;
    }

    public void recycle() {
        recycled = true;
        pixels = null;
    }

    public boolean isRecycled() {
        return recycled;
    }

    private int[] pixels() {
        if (pixels == null) {
            pixels = image.getRGB();
        }
        return pixels;
    }

    public int getPixel(int x, int y) {
        return pixels()[y * width + x];
    }

    public void getPixels(int[] out, int offset, int stride, int x, int y, int w, int h) {
        int[] p = pixels();
        for (int row = 0; row < h; row++) {
            System.arraycopy(p, (y + row) * width + x, out, offset + row * stride, w);
        }
    }

    public void setPixel(int x, int y, int color) {
        pixels()[y * width + x] = color;
        pixelsDirty = true;
    }

    public void setPixels(int[] in, int offset, int stride, int x, int y, int w, int h) {
        int[] p = pixels();
        for (int row = 0; row < h; row++) {
            System.arraycopy(in, offset + row * stride, p, (y + row) * width + x, w);
        }
        pixelsDirty = true;
    }

    public void eraseColor(int color) {
        int[] p = new int[width * height];
        for (int i = 0; i < p.length; i++) {
            p[i] = color;
        }
        pixels = p;
        pixelsDirty = true;
    }

    /// Called by `Canvas` after drawing into a mutable bitmap, so a cached
    /// pixel array does not go stale.
    void contentChanged() {
        pixels = null;
    }

    void flushPixelsForDrawing() {
        getImage();
    }

    public Bitmap copy(Config config, boolean isMutable) {
        Image src = getImage();
        if (isMutable) {
            Image copy = Image.createImage(width, height, 0);
            copy.getGraphics().drawImage(src, 0, 0);
            return new Bitmap(copy, true, config);
        }
        return new Bitmap(src, false, config);
    }

    public Bitmap extractAlpha() {
        int[] p = pixels().clone();
        for (int i = 0; i < p.length; i++) {
            p[i] = p[i] & 0xff000000;
        }
        return new Bitmap(Image.createImage(p, width, height), false, Config.ALPHA_8);
    }

    public boolean compress(CompressFormat format, int quality, OutputStream stream) {
        ImageIO io = ImageIO.getImageIO();
        if (io == null) {
            return false;
        }
        try {
            io.save(getImage(), stream, format == CompressFormat.JPEG ? ImageIO.FORMAT_JPEG : ImageIO.FORMAT_PNG,
                    quality / 100f);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean sameAs(Bitmap other) {
        if (other == null || other.width != width || other.height != height) {
            return false;
        }
        int[] a = pixels();
        int[] b = other.pixels();
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }
}
