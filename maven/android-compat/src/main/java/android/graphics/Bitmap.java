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
    /// The density [#setDensity] gave, or -1 for the device's, which is
    /// what a bitmap starts with on Android (decoded, created or wrapped).
    /// Read lazily, so a bitmap made before the resources exist still works.
    private int density = -1;

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
        Bitmap b = createBitmap(width, height, config);
        // As on Android, the bitmap takes the density of the metrics it was
        // made for, so scaled sizes and density-aware drawables honour it.
        if (display != null) {
            b.density = display.densityDpi;
        }
        return b;
    }

    public static Bitmap createBitmap(Bitmap src) {
        return new Bitmap(src.snapshot(), false, src.config);
    }

    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height) {
        if (x == 0 && y == 0 && width == source.width && height == source.height) {
            // As on Android, only an immutable source is handed back as is;
            // a mutable one is copied so later drawing does not reach it.
            return source.mutable ? new Bitmap(source.snapshot(), false, source.config) : source;
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
        float[] v = new float[9];
        m.getValues(v);
        if (m.isScaleTranslate() && v[0] > 0 && v[4] > 0) {
            // A plain enlargement or reduction: the port's own scaling.
            return new Bitmap(sub.getImage().scaled(w, h), false, source.config);
        }
        // Rotations, mirrors and skews move pixels, which scaling cannot do:
        // each output pixel is sampled from the source through the inverse
        // matrix, the output's origin being the mapped bounds' top left.
        int[] out = new int[w * h];
        Matrix inv = new Matrix();
        if (m.invert(inv)) {
            inv.getValues(v);
            int[] src = sub.getImage().getRGB();
            int sw = sub.width;
            int sh = sub.height;
            for (int dy = 0; dy < h; dy++) {
                float py = dy + 0.5f + r.top;
                for (int dx = 0; dx < w; dx++) {
                    float px = dx + 0.5f + r.left;
                    // Affine, like the rest of this Matrix.
                    float sx = v[0] * px + v[1] * py + v[2];
                    float sy = v[3] * px + v[4] * py + v[5];
                    out[dy * w + dx] = filter ? sampleBilinear(src, sw, sh, sx - 0.5f, sy - 0.5f)
                            : sampleNearest(src, sw, sh, sx, sy);
                }
            }
        }
        return new Bitmap(Image.createImage(out, w, h), false, source.config);
    }

    private static int sampleNearest(int[] src, int sw, int sh, float sx, float sy) {
        int x = (int) Math.floor(sx);
        int y = (int) Math.floor(sy);
        return x < 0 || y < 0 || x >= sw || y >= sh ? 0 : src[y * sw + x];
    }

    /// Bilinear sample at (`fx`, `fy`) in pixel-centre coordinates, outside
    /// the source counting as transparent, blended with premultiplied alpha
    /// so a transparent neighbour does not darken an edge.
    private static int sampleBilinear(int[] src, int sw, int sh, float fx, float fy) {
        int x0 = (int) Math.floor(fx);
        int y0 = (int) Math.floor(fy);
        float tx = fx - x0;
        float ty = fy - y0;
        float a = 0;
        float rr = 0;
        float gg = 0;
        float bb = 0;
        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < 2; i++) {
                int x = x0 + i;
                int y = y0 + j;
                if (x < 0 || y < 0 || x >= sw || y >= sh) {
                    continue;
                }
                float wgt = (i == 0 ? 1 - tx : tx) * (j == 0 ? 1 - ty : ty);
                int c = src[y * sw + x];
                float ca = ((c >>> 24) & 0xff) * wgt;
                a += ca;
                rr += ((c >> 16) & 0xff) * ca;
                gg += ((c >> 8) & 0xff) * ca;
                bb += (c & 0xff) * ca;
            }
        }
        if (a <= 0) {
            return 0;
        }
        int ia = Math.min(255, Math.round(a));
        return (ia << 24) | (Math.min(255, Math.round(rr / a)) << 16)
                | (Math.min(255, Math.round(gg / a)) << 8) | Math.min(255, Math.round(bb / a));
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
        return scale(width, getDensity(), targetDensity);
    }

    public int getScaledHeight(int targetDensity) {
        return scale(height, getDensity(), targetDensity);
    }

    private static int scale(int size, int density, int targetDensity) {
        return density == DENSITY_NONE || targetDensity == DENSITY_NONE || density == targetDensity
                ? size : (size * targetDensity + density / 2) / density;
    }

    public int getDensity() {
        if (density >= 0) {
            return density;
        }
        return android.content.res.Resources.getSystem().getDisplayMetrics().densityDpi;
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

    /// Refuses a pixel read on a recycled bitmap the way Android does. The
    /// image is still held (a `Canvas` may share it), so without this a
    /// recycled bitmap would quietly re-read the pixels it was told to drop.
    private void checkNotRecycled(String message) {
        if (recycled) {
            throw new IllegalStateException(message);
        }
    }

    /// Validates a single pixel coordinate the way Android does. The pixels
    /// are one flattened array, so without this `x == width` (or a negative
    /// `x`) quietly addressed a pixel of the neighbouring row.
    private void checkPixelAccess(int x, int y) {
        if (x < 0) {
            throw new IllegalArgumentException("x must be >= 0");
        }
        if (y < 0) {
            throw new IllegalArgumentException("y must be >= 0");
        }
        if (x >= width) {
            throw new IllegalArgumentException("x must be < bitmap.width()");
        }
        if (y >= height) {
            throw new IllegalArgumentException("y must be < bitmap.height()");
        }
    }

    /// Validates a pixel region the way Android does, for the same reason:
    /// a row that runs past `width` would spill into the next one.
    private void checkPixelsAccess(int x, int y, int w, int h) {
        if (x < 0) {
            throw new IllegalArgumentException("x must be >= 0");
        }
        if (y < 0) {
            throw new IllegalArgumentException("y must be >= 0");
        }
        if (w < 0) {
            throw new IllegalArgumentException("width must be >= 0");
        }
        if (h < 0) {
            throw new IllegalArgumentException("height must be >= 0");
        }
        if (x + w > width) {
            throw new IllegalArgumentException("x + width must be <= bitmap.width()");
        }
        if (y + h > height) {
            throw new IllegalArgumentException("y + height must be <= bitmap.height()");
        }
    }

    public int getPixel(int x, int y) {
        checkNotRecycled("Can't call getPixel() on a recycled bitmap");
        checkPixelAccess(x, y);
        return pixels()[y * width + x];
    }

    public void getPixels(int[] out, int offset, int stride, int x, int y, int w, int h) {
        checkNotRecycled("Can't call getPixels() on a recycled bitmap");
        checkPixelsAccess(x, y, w, h);
        int[] p = pixels();
        for (int row = 0; row < h; row++) {
            System.arraycopy(p, (y + row) * width + x, out, offset + row * stride, w);
        }
    }

    /// Refuses a pixel write the way Android does: a recycled bitmap has no
    /// pixels, and an immutable one may share its image with other bitmaps
    /// (`createBitmap(source)` hands an immutable source back as is, and a
    /// decoded resource is cached), so writing it would change them too.
    private void checkWritable(String op) {
        if (recycled) {
            throw new IllegalStateException("Can't call " + op + "() on a recycled bitmap");
        }
        if (!mutable) {
            throw new IllegalStateException();
        }
    }

    public void setPixel(int x, int y, int color) {
        checkWritable("setPixel");
        checkPixelAccess(x, y);
        pixels()[y * width + x] = color;
        pixelsDirty = true;
    }

    public void setPixels(int[] in, int offset, int stride, int x, int y, int w, int h) {
        checkWritable("setPixels");
        checkPixelsAccess(x, y, w, h);
        int[] p = pixels();
        for (int row = 0; row < h; row++) {
            System.arraycopy(in, offset + row * stride, p, (y + row) * width + x, w);
        }
        pixelsDirty = true;
    }

    public void eraseColor(int color) {
        checkWritable("eraseColor");
        int[] p = new int[width * height];
        for (int i = 0; i < p.length; i++) {
            p[i] = color;
        }
        pixels = p;
        pixelsDirty = true;
    }

    /// Called by `Canvas` on every draw into this bitmap, so a cached pixel
    /// array is re-read rather than going stale. The canvas has flushed
    /// pending pixel writes (through `getImage`) by then.
    void contentChanged() {
        if (!pixelsDirty) {
            pixels = null;
        }
    }

    void flushPixelsForDrawing() {
        getImage();
    }

    public Bitmap copy(Config config, boolean isMutable) {
        checkNotRecycled("Can't copy a recycled bitmap");
        Image src = getImage();
        if (isMutable) {
            Image copy = Image.createImage(width, height, 0);
            copy.getGraphics().drawImage(src, 0, 0);
            return new Bitmap(copy, true, config);
        }
        return new Bitmap(snapshot(), false, config);
    }

    /// The current image, copied when this bitmap is mutable: drawing into a
    /// mutable bitmap changes its image in place, so an immutable bitmap that
    /// shared it would change too.
    private Image snapshot() {
        Image src = getImage();
        return mutable ? src.subImage(0, 0, width, height, true) : src;
    }

    public Bitmap extractAlpha() {
        checkNotRecycled("Can't extractAlpha on a recycled bitmap");
        int[] p = pixels().clone();
        for (int i = 0; i < p.length; i++) {
            p[i] = p[i] & 0xff000000;
        }
        return new Bitmap(Image.createImage(p, width, height), false, Config.ALPHA_8);
    }

    public boolean compress(CompressFormat format, int quality, OutputStream stream) {
        checkNotRecycled("Can't compress a recycled bitmap");
        // Codename One encodes only PNG and JPEG. A WebP request fails the
        // way Android reports an encoder failure, rather than writing PNG
        // bytes the caller will label and serve as WebP.
        if (format != CompressFormat.JPEG && format != CompressFormat.PNG) {
            return false;
        }
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
