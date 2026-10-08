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
package com.codename1.androidcompat.runtime;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/// The Material glyphs of the compound buttons -- checkbox and radio button --
/// drawn in code from the theme's control colors, so they need no bitmap
/// assets and follow the theme (and its tints) like the AOSP drawables do.
public final class ControlDrawables {

    private ControlDrawables() {
    }

    /// The theme's control colors: `[colorControlNormal, colorControlActivated,
    /// disabledAlpha * 255]`.
    public static int[] controlColors(Context c) {
        TypedArray a = c.obtainStyledAttributes(new int[] {android.R.attr.colorControlNormal,
            android.R.attr.colorControlActivated, android.R.attr.disabledAlpha});
        int normal = a.getColor(0, 0x8a000000);
        int activated = a.getColor(1, 0xff009688);
        float disabled = a.getFloat(2, 0.26f);
        a.recycle();
        return new int[] {normal, activated, Math.round(disabled * 255)};
    }

    static boolean has(int[] state, int attr) {
        if (state == null) {
            return false;
        }
        for (int s : state) {
            if (s == attr) {
                return true;
            }
        }
        return false;
    }

    /// A control glyph colored by checked and enabled state.
    abstract static class Glyph extends Drawable {
        final int normal;
        final int activated;
        final int disabledAlpha;
        final float density;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int alpha = 255;
        private ColorFilter filter;

        Glyph(Context c) {
            int[] colors = controlColors(c);
            normal = colors[0];
            activated = colors[1];
            disabledAlpha = colors[2];
            density = c.getResources().getDisplayMetrics().density;
        }

        boolean checked() {
            return has(getState(), android.R.attr.state_checked);
        }

        boolean enabled() {
            int[] s = getState();
            return s == null || s.length == 0 || has(s, android.R.attr.state_enabled);
        }

        /// The color for the current state, tint and alpha applied.
        int color() {
            int c = checked() ? activated : normal;
            if (tintList != null) {
                c = tintList.getColorForState(getState(), c);
            }
            int a = c >>> 24;
            if (!enabled()) {
                a = a * disabledAlpha / 255;
            }
            a = a * alpha / 255;
            return (c & 0xffffff) | (a << 24);
        }

        @Override
        public int getIntrinsicWidth() {
            return Math.round(32 * density);
        }

        @Override
        public int getIntrinsicHeight() {
            return Math.round(32 * density);
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            invalidateSelf();
            return true;
        }

        @Override
        public void setAlpha(int a) {
            alpha = a;
            invalidateSelf();
        }

        @Override
        public int getAlpha() {
            return alpha;
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            filter = cf;
            invalidateSelf();
        }

        @Override
        public ColorFilter getColorFilter() {
            return filter;
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /// Material checkbox: an 18dp rounded square, outlined when unchecked and
    /// filled with a white check mark when checked.
    public static final class CheckBox extends Glyph {
        public CheckBox(Context c) {
            super(c);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float size = 18 * density;
            float l = b.exactCenterX() - size / 2;
            float t = b.exactCenterY() - size / 2;
            RectF box = new RectF(l, t, l + size, t + size);
            float r = 2 * density;
            paint.setColor(color());
            if (checked()) {
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(box, r, r, paint);
                Path mark = new Path();
                mark.moveTo(l + size * 0.22f, t + size * 0.52f);
                mark.lineTo(l + size * 0.42f, t + size * 0.72f);
                mark.lineTo(l + size * 0.80f, t + size * 0.32f);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2 * density);
                p.setStrokeCap(Paint.Cap.SQUARE);
                p.setColor(0xffffffff);
                p.setAlpha(paint.getAlpha());
                canvas.drawPath(mark, p);
            } else {
                paint.setStyle(Paint.Style.STROKE);
                float sw = 2 * density;
                paint.setStrokeWidth(sw);
                box.inset(sw / 2, sw / 2);
                canvas.drawRoundRect(box, r, r, paint);
            }
        }
    }

    /// Material radio button: a 20dp ring, with a 10dp dot when checked.
    public static final class Radio extends Glyph {
        public Radio(Context c) {
            super(c);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float cx = b.exactCenterX();
            float cy = b.exactCenterY();
            float sw = 2 * density;
            paint.setColor(color());
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(sw);
            canvas.drawCircle(cx, cy, 10 * density - sw / 2, paint);
            if (checked()) {
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy, 5 * density, paint);
            }
        }
    }

    /// Material check mark for CheckedTextView: a check in the activated
    /// color when checked, nothing when not.
    public static final class CheckMark extends Glyph {
        public CheckMark(Context c) {
            super(c);
        }

        @Override
        public void draw(Canvas canvas) {
            if (!checked()) {
                return;
            }
            Rect b = getBounds();
            float size = 18 * density;
            float l = b.exactCenterX() - size / 2;
            float t = b.exactCenterY() - size / 2;
            Path mark = new Path();
            mark.moveTo(l + size * 0.15f, t + size * 0.52f);
            mark.lineTo(l + size * 0.40f, t + size * 0.77f);
            mark.lineTo(l + size * 0.88f, t + size * 0.27f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2 * density);
            paint.setColor(color());
            canvas.drawPath(mark, paint);
        }
    }
}
