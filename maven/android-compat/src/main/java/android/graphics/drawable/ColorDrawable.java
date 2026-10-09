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

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuffColorFilter;

/// Fills its bounds with a color.
public class ColorDrawable extends Drawable {

    private int baseColor;
    private int alpha = 255;
    private ColorFilter colorFilter;
    private final Paint paint = new Paint();

    public ColorDrawable() {
        this(0);
    }

    public ColorDrawable(int color) {
        baseColor = color;
        alpha = color >>> 24;
    }

    public int getColor() {
        return baseColor;
    }

    public void setColor(int color) {
        if (baseColor != color) {
            baseColor = color;
            alpha = color >>> 24;
            invalidateSelf();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        int color = baseColor;
        ColorFilter cf = effectiveColorFilter(colorFilter);
        if (cf instanceof PorterDuffColorFilter) {
            int c = ((PorterDuffColorFilter) cf).getColor();
            color = (c & 0xffffff) | ((((c >>> 24) * (color >>> 24)) / 255) << 24);
        }
        if ((color >>> 24) == 0) {
            return;
        }
        paint.setColor(color);
        canvas.drawRect(getBounds(), paint);
    }

    @Override
    public void setAlpha(int a) {
        baseColor = (baseColor & 0xffffff) | ((alpha * a / 255) << 24);
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return baseColor >>> 24;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.colorFilter = colorFilter;
        invalidateSelf();
    }

    @Override
    public ColorFilter getColorFilter() {
        return colorFilter;
    }

    @Override
    public int getOpacity() {
        int a = baseColor >>> 24;
        return a == 255 ? PixelFormat.OPAQUE : a == 0 ? PixelFormat.TRANSPARENT : PixelFormat.TRANSLUCENT;
    }

    @Override
    public ConstantState getConstantState() {
        final int c = baseColor;
        return new ConstantState() {
            @Override
            public Drawable newDrawable() {
                return new ColorDrawable(c);
            }

            @Override
            public int getChangingConfigurations() {
                return 0;
            }
        };
    }
}
