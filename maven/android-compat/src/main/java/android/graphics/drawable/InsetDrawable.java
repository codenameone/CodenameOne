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
import android.graphics.PixelFormat;
import android.graphics.Rect;

/// Draws another drawable inset within its bounds; the insets count as padding.
public class InsetDrawable extends Drawable implements Drawable.Callback {

    private Drawable drawable;
    private int insetL;
    private int insetT;
    private int insetR;
    private int insetB;
    /// Fractional insets (of the bounds); negative when the inset is in px.
    private float fracL = -1;
    private float fracT = -1;
    private float fracR = -1;
    private float fracB = -1;

    public InsetDrawable(Drawable drawable, int inset) {
        this(drawable, inset, inset, inset, inset);
    }

    public InsetDrawable(Drawable drawable, int insetLeft, int insetTop, int insetRight, int insetBottom) {
        setDrawable(drawable);
        insetL = insetLeft;
        insetT = insetTop;
        insetR = insetRight;
        insetB = insetBottom;
    }

    public InsetDrawable(Drawable drawable, float inset) {
        this(drawable, inset, inset, inset, inset);
    }

    public InsetDrawable(Drawable drawable, float l, float t, float r, float b) {
        setDrawable(drawable);
        fracL = l;
        fracT = t;
        fracR = r;
        fracB = b;
    }

    /// Sets one side as a fraction of the bounds (from `insetLeft="10%"`).
    public void setFractionalInsets(float l, float t, float r, float b) {
        fracL = l;
        fracT = t;
        fracR = r;
        fracB = b;
    }

    public void setDrawable(Drawable d) {
        if (drawable != null) {
            drawable.setCallback(null);
        }
        drawable = d;
        if (d != null) {
            d.setCallback(this);
        }
    }

    public Drawable getDrawable() {
        return drawable;
    }

    private int l(int w) {
        return fracL >= 0 ? (int) (w * fracL) : insetL;
    }

    private int t(int h) {
        return fracT >= 0 ? (int) (h * fracT) : insetT;
    }

    private int r(int w) {
        return fracR >= 0 ? (int) (w * fracR) : insetR;
    }

    private int b(int h) {
        return fracB >= 0 ? (int) (h * fracB) : insetB;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (drawable != null) {
            int w = bounds.width();
            int h = bounds.height();
            drawable.setBounds(bounds.left + l(w), bounds.top + t(h), bounds.right - r(w), bounds.bottom - b(h));
        }
    }

    @Override
    public boolean getPadding(Rect padding) {
        boolean has = drawable != null && drawable.getPadding(padding);
        if (!has) {
            padding.set(0, 0, 0, 0);
        }
        Rect bounds = getBounds();
        padding.left += l(bounds.width());
        padding.top += t(bounds.height());
        padding.right += r(bounds.width());
        padding.bottom += b(bounds.height());
        return true;
    }

    @Override
    public int getIntrinsicWidth() {
        int w = drawable == null ? -1 : drawable.getIntrinsicWidth();
        return w < 0 || fracL >= 0 || fracR >= 0 ? w : w + insetL + insetR;
    }

    @Override
    public int getIntrinsicHeight() {
        int h = drawable == null ? -1 : drawable.getIntrinsicHeight();
        return h < 0 || fracT >= 0 || fracB >= 0 ? h : h + insetT + insetB;
    }

    @Override
    public void draw(Canvas canvas) {
        if (drawable != null) {
            drawable.draw(canvas);
        }
    }

    @Override
    public boolean isStateful() {
        return drawable != null && drawable.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean changed = drawable != null && drawable.setState(state);
        onBoundsChange(getBounds());
        return changed;
    }

    @Override
    protected boolean onLevelChange(int level) {
        return drawable != null && drawable.setLevel(level);
    }

    @Override
    public void setAlpha(int alpha) {
        if (drawable != null) {
            drawable.setAlpha(alpha);
        }
    }

    @Override
    public int getAlpha() {
        return drawable == null ? 255 : drawable.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (drawable != null) {
            drawable.setColorFilter(cf);
        }
    }

    @Override
    public void setTintList(android.content.res.ColorStateList tint) {
        if (drawable != null) {
            drawable.setTintList(tint);
        }
    }

    @Override
    public int getOpacity() {
        return drawable == null ? PixelFormat.TRANSPARENT : PixelFormat.TRANSLUCENT;
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }
}
