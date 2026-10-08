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
package com.google.android.material.materialswitch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.MaterialAttrs;

/// The Material 3 switch: a 52x32dp track, filled with the primary color
/// when on and outlined when off, and a thumb that grows from 16dp to 24dp
/// as the switch turns on.
public class MaterialSwitch extends SwitchCompat {

    public MaterialSwitch(Context context) {
        this(context, null);
    }

    public MaterialSwitch(Context context, AttributeSet attrs) {
        this(context, attrs, MaterialAttrs.defStyleAttr(context, R.attr.materialSwitchStyle));
    }

    public MaterialSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setTrackDrawable(new Track(context));
        setThumbDrawable(new Thumb(context));
    }

    private static boolean has(int[] state, int attr) {
        for (int s : state) {
            if (s == attr) {
                return true;
            }
        }
        return false;
    }

    private static final class Track extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int primary;
        private final int surfaceHighest;
        private final int outline;
        private final int onSurface;
        private final float stroke;
        private final int width;
        private final int height;
        private int[] state = new int[0];

        Track(Context c) {
            primary = MaterialColors.getColor(c, R.attr.colorPrimary, 0xff6750a4);
            surfaceHighest = MaterialColors.getColor(c, R.attr.colorSurfaceContainerHighest, 0xffe6e0e9);
            outline = MaterialColors.getColor(c, R.attr.colorOutline, 0xff79747e);
            onSurface = MaterialAttrs.onSurface(c);
            stroke = MaterialAttrs.dp(c, 2);
            width = MaterialAttrs.dpi(c, 52);
            height = MaterialAttrs.dpi(c, 32);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            boolean checked = has(state, android.R.attr.state_checked);
            boolean enabled = has(state, android.R.attr.state_enabled);
            RectF r = new RectF(b);
            float radius = r.height() / 2f;
            int fill = checked ? primary : surfaceHighest;
            if (!enabled) {
                fill = checked ? MaterialColors.withAlpha(onSurface, 0.12f)
                        : MaterialColors.withAlpha(surfaceHighest, 0.12f);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(fill);
            canvas.drawRoundRect(r, radius, radius, paint);
            if (!checked) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(stroke);
                paint.setColor(enabled ? outline : MaterialColors.withAlpha(onSurface, 0.12f));
                RectF s = new RectF(r.left + stroke / 2, r.top + stroke / 2, r.right - stroke / 2, r.bottom - stroke / 2);
                canvas.drawRoundRect(s, s.height() / 2f, s.height() / 2f, paint);
            }
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(int[] s) {
            state = s == null ? new int[0] : s;
            return true;
        }

        @Override
        public int getIntrinsicWidth() {
            return width;
        }

        @Override
        public int getIntrinsicHeight() {
            return height;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private static final class Thumb extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int onPrimary;
        private final int outline;
        private final int onSurface;
        private final int surface;
        private final float offRadius;
        private final float onRadius;
        private final float pressedRadius;
        private final int size;
        private int[] state = new int[0];

        Thumb(Context c) {
            onPrimary = MaterialColors.getColor(c, R.attr.colorOnPrimary, 0xffffffff);
            outline = MaterialColors.getColor(c, R.attr.colorOutline, 0xff79747e);
            onSurface = MaterialAttrs.onSurface(c);
            surface = MaterialColors.getColor(c, R.attr.colorSurface, 0xfffef7ff);
            offRadius = MaterialAttrs.dp(c, 8);
            onRadius = MaterialAttrs.dp(c, 12);
            pressedRadius = MaterialAttrs.dp(c, 14);
            size = MaterialAttrs.dpi(c, 32);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            boolean checked = has(state, android.R.attr.state_checked);
            boolean enabled = has(state, android.R.attr.state_enabled);
            boolean pressed = has(state, android.R.attr.state_pressed);
            float radius = pressed ? pressedRadius : checked ? onRadius : offRadius;
            int color = checked ? onPrimary : outline;
            if (!enabled) {
                color = checked ? surface : MaterialColors.withAlpha(onSurface, 0.38f);
            }
            paint.setColor(color);
            canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), radius, paint);
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(int[] s) {
            state = s == null ? new int[0] : s;
            return true;
        }

        @Override
        public int getIntrinsicWidth() {
            return size;
        }

        @Override
        public int getIntrinsicHeight() {
            return size;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
