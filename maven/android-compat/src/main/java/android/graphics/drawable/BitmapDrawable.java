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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.Gravity;

/// Draws a bitmap, stretched to its bounds or placed by gravity.
public class BitmapDrawable extends Drawable {

    private Bitmap bitmap;
    private int gravity = Gravity.FILL;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private ColorFilter colorFilter;
    private Shader.TileMode tileX;
    private Shader.TileMode tileY;
    private final Rect dst = new Rect();

    public BitmapDrawable() {
    }

    public BitmapDrawable(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public BitmapDrawable(Resources res, Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public BitmapDrawable(Resources res, java.io.InputStream is) {
        this.bitmap = android.graphics.BitmapFactory.decodeStream(is);
    }

    public final Paint getPaint() {
        return paint;
    }

    public final Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        invalidateSelf();
    }

    public int getGravity() {
        return gravity;
    }

    public void setGravity(int gravity) {
        this.gravity = gravity;
        invalidateSelf();
    }

    public void setAntiAlias(boolean aa) {
        paint.setAntiAlias(aa);
    }

    public void setTileModeXY(Shader.TileMode x, Shader.TileMode y) {
        tileX = x;
        tileY = y;
        invalidateSelf();
    }

    public void setTargetDensity(int density) {
    }

    @Override
    public int getIntrinsicWidth() {
        return bitmap == null ? -1 : bitmap.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return bitmap == null ? -1 : bitmap.getHeight();
    }

    @Override
    public void draw(Canvas canvas) {
        if (bitmap == null) {
            return;
        }
        Rect b = getBounds();
        ColorFilter cf = effectiveColorFilter(colorFilter);
        paint.setColorFilter(cf instanceof PorterDuffColorFilter ? cf : null);
        if (tileX == Shader.TileMode.REPEAT || tileY == Shader.TileMode.REPEAT) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            canvas.save();
            canvas.clipRect(b);
            for (int y = b.top; y < b.bottom; y += h) {
                for (int x = b.left; x < b.right; x += w) {
                    canvas.drawBitmap(bitmap, x, y, paint);
                }
            }
            canvas.restore();
            return;
        }
        if (gravity == Gravity.FILL) {
            canvas.drawBitmap(bitmap, null, new RectF(b), paint);
        } else {
            Gravity.apply(Gravity.getAbsoluteGravity(gravity, getLayoutDirection()), bitmap.getWidth(),
                    bitmap.getHeight(), b, dst);
            canvas.drawBitmap(bitmap, null, new RectF(dst), paint);
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
    public ColorFilter getColorFilter() {
        return colorFilter;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public ConstantState getConstantState() {
        final Bitmap b = bitmap;
        final int g = gravity;
        return new ConstantState() {
            @Override
            public Drawable newDrawable() {
                BitmapDrawable d = new BitmapDrawable(b);
                d.setGravity(g);
                return d;
            }

            @Override
            public int getChangingConfigurations() {
                return 0;
            }
        };
    }
}
