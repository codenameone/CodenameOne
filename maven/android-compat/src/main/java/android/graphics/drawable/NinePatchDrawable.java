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
package android.graphics.drawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import com.codename1.ui.Image;

/// A nine-patch: an image whose one-pixel black border marks the stretchable
/// region (top and left edges) and the content padding (bottom and right).
/// The marks are read from the `.9.png` at load time. One stretch span per
/// axis is honoured, from the first mark to the last, which is what nearly
/// every nine-patch uses.
public class NinePatchDrawable extends Drawable {

    private final Image image;
    private int stretchX0;
    private int stretchX1;
    private int stretchY0;
    private int stretchY1;
    private final Rect padding;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private ColorFilter colorFilter;
    private Image[] slices;

    /// `raw` is the decoded `.9.png` including its marker border.
    public NinePatchDrawable(Image raw) {
        this(raw, 1f);
    }

    /// As [#NinePatchDrawable(Image)], scaling the patch by `scale` after the
    /// markers are read: a nine-patch from a density bucket other than the
    /// device's is scaled like any bitmap, but its one-pixel marker border
    /// must be read before interpolation smears it.
    public NinePatchDrawable(Image raw, float scale) {
        int w = raw.getWidth();
        int h = raw.getHeight();
        int[] px = raw.getRGB();
        int x0 = -1;
        int x1 = -1;
        for (int x = 1; x < w - 1; x++) {
            if (isMark(px[x])) {
                if (x0 < 0) {
                    x0 = x - 1;
                }
                x1 = x;
            }
        }
        int y0 = -1;
        int y1 = -1;
        for (int y = 1; y < h - 1; y++) {
            if (isMark(px[y * w])) {
                if (y0 < 0) {
                    y0 = y - 1;
                }
                y1 = y;
            }
        }
        int iw = w - 2;
        int ih = h - 2;
        stretchX0 = x0 < 0 ? 0 : x0;
        stretchX1 = x1 < 0 ? iw : x1;
        stretchY0 = y0 < 0 ? 0 : y0;
        stretchY1 = y1 < 0 ? ih : y1;
        int pl = -1;
        int pr = -1;
        for (int x = 1; x < w - 1; x++) {
            if (isMark(px[(h - 1) * w + x])) {
                if (pl < 0) {
                    pl = x - 1;
                }
                pr = x;
            }
        }
        int pt = -1;
        int pb = -1;
        for (int y = 1; y < h - 1; y++) {
            if (isMark(px[y * w + w - 1])) {
                if (pt < 0) {
                    pt = y - 1;
                }
                pb = y;
            }
        }
        Rect pad = new Rect(pl < 0 ? x0 < 0 ? 0 : x0 : pl, pt < 0 ? y0 < 0 ? 0 : y0 : pt,
                pr < 0 ? iw - (x1 < 0 ? iw : x1) : iw - pr, pb < 0 ? ih - (y1 < 0 ? ih : y1) : ih - pb);
        Image inner = raw.subImage(1, 1, iw, ih, true);
        if (scale != 1f && scale > 0) {
            int sw = Math.max(1, Math.round(iw * scale));
            int sh = Math.max(1, Math.round(ih * scale));
            inner = inner.scaled(sw, sh);
            stretchX0 = Math.round(stretchX0 * scale);
            stretchX1 = Math.round(stretchX1 * scale);
            stretchY0 = Math.round(stretchY0 * scale);
            stretchY1 = Math.round(stretchY1 * scale);
            pad.set(Math.round(pad.left * scale), Math.round(pad.top * scale), Math.round(pad.right * scale),
                    Math.round(pad.bottom * scale));
        }
        padding = pad;
        image = inner;
    }

    private static boolean isMark(int argb) {
        return (argb >>> 24) == 0xff && (argb & 0xffffff) == 0;
    }

    public NinePatchDrawable(Bitmap bitmap) {
        this(bitmap.getImage());
    }

    @Override
    public boolean getPadding(Rect out) {
        out.set(padding);
        return true;
    }

    @Override
    public int getIntrinsicWidth() {
        return image.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return image.getHeight();
    }

    @Override
    public int getMinimumWidth() {
        return image.getWidth() - (stretchX1 - stretchX0);
    }

    @Override
    public int getMinimumHeight() {
        return image.getHeight() - (stretchY1 - stretchY0);
    }

    private Image[] slices() {
        if (slices == null) {
            int w = image.getWidth();
            int h = image.getHeight();
            int[] xs = {0, stretchX0, stretchX1, w};
            int[] ys = {0, stretchY0, stretchY1, h};
            slices = new Image[9];
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int sw = xs[c + 1] - xs[c];
                    int sh = ys[r + 1] - ys[r];
                    slices[r * 3 + c] = sw > 0 && sh > 0 ? image.subImage(xs[c], ys[r], sw, sh, true) : null;
                }
            }
        }
        return slices;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) {
            return;
        }
        Image[] s = slices();
        int w = image.getWidth();
        int h = image.getHeight();
        int left = stretchX0;
        int right = w - stretchX1;
        int top = stretchY0;
        int bottom = h - stretchY1;
        // Shrink the fixed edges proportionally when the bounds are smaller
        // than the unstretchable part, as Android does.
        float fx = b.width() < left + right ? b.width() / (float) (left + right) : 1;
        float fy = b.height() < top + bottom ? b.height() / (float) (top + bottom) : 1;
        int l = Math.round(left * fx);
        int r = Math.round(right * fx);
        int t = Math.round(top * fy);
        int bt = Math.round(bottom * fy);
        int[] xs = {b.left, b.left + l, b.right - r, b.right};
        int[] ys = {b.top, b.top + t, b.bottom - bt, b.bottom};
        ColorFilter cf = effectiveColorFilter(colorFilter);
        paint.setColorFilter(cf instanceof PorterDuffColorFilter ? cf : null);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Image slice = s[row * 3 + col];
                int dw = xs[col + 1] - xs[col];
                int dh = ys[row + 1] - ys[row];
                if (slice == null || dw <= 0 || dh <= 0) {
                    continue;
                }
                canvas.drawImage(slice, new RectF(xs[col], ys[row], xs[col + 1], ys[row + 1]), paint);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return paint.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        colorFilter = cf;
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
