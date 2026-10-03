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

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.util.StateSet;

import java.io.InputStream;

/// Something that can be drawn into bounds: Android's drawable model, with
/// state, level, tint and an invalidation callback to the owning view.
public abstract class Drawable {

    /// The view (or container drawable) that redraws when this drawable changes.
    public interface Callback {
        void invalidateDrawable(Drawable who);

        void scheduleDrawable(Drawable who, Runnable what, long when);

        void unscheduleDrawable(Drawable who, Runnable what);
    }

    /// Shared state for creating copies of a drawable.
    public abstract static class ConstantState {
        public abstract Drawable newDrawable();

        public Drawable newDrawable(Resources res) {
            return newDrawable();
        }

        public Drawable newDrawable(Resources res, Resources.Theme theme) {
            return newDrawable(res);
        }

        public abstract int getChangingConfigurations();

        public boolean canApplyTheme() {
            return false;
        }
    }

    private final Rect bounds = new Rect();
    private int[] stateSet = StateSet.WILD_CARD;
    private int level;
    private boolean visible = true;
    private Callback callback;
    private int changingConfigurations;
    private int layoutDirection;
    private boolean autoMirrored;
    protected ColorStateList tintList;
    protected PorterDuff.Mode tintMode = PorterDuff.Mode.SRC_IN;
    private ColorFilter userColorFilter;

    public abstract void draw(Canvas canvas);

    public void setBounds(int left, int top, int right, int bottom) {
        if (bounds.left != left || bounds.top != top || bounds.right != right || bounds.bottom != bottom) {
            if (!bounds.isEmpty()) {
                invalidateSelf();
            }
            bounds.set(left, top, right, bottom);
            onBoundsChange(bounds);
        }
    }

    public void setBounds(Rect b) {
        setBounds(b.left, b.top, b.right, b.bottom);
    }

    public final void copyBounds(Rect out) {
        out.set(bounds);
    }

    public final Rect copyBounds() {
        return new Rect(bounds);
    }

    public final Rect getBounds() {
        return bounds;
    }

    public Rect getDirtyBounds() {
        return getBounds();
    }

    public void setChangingConfigurations(int configs) {
        changingConfigurations = configs;
    }

    public int getChangingConfigurations() {
        return changingConfigurations;
    }

    public void setDither(boolean dither) {
    }

    public void setFilterBitmap(boolean filter) {
    }

    public boolean isFilterBitmap() {
        return false;
    }

    public final void setCallback(Callback cb) {
        callback = cb;
    }

    public Callback getCallback() {
        return callback;
    }

    public void invalidateSelf() {
        Callback cb = callback;
        if (cb != null) {
            cb.invalidateDrawable(this);
        }
    }

    public void scheduleSelf(Runnable what, long when) {
        Callback cb = callback;
        if (cb != null) {
            cb.scheduleDrawable(this, what, when);
        }
    }

    public void unscheduleSelf(Runnable what) {
        Callback cb = callback;
        if (cb != null) {
            cb.unscheduleDrawable(this, what);
        }
    }

    public int getLayoutDirection() {
        return layoutDirection;
    }

    public final boolean setLayoutDirection(int layoutDirection) {
        if (this.layoutDirection != layoutDirection) {
            this.layoutDirection = layoutDirection;
            return onLayoutDirectionChanged(layoutDirection);
        }
        return false;
    }

    public boolean onLayoutDirectionChanged(int layoutDirection) {
        return false;
    }

    public abstract void setAlpha(int alpha);

    public int getAlpha() {
        return 0xff;
    }

    public abstract void setColorFilter(ColorFilter colorFilter);

    public void setColorFilter(int color, PorterDuff.Mode mode) {
        setColorFilter(new PorterDuffColorFilter(color, mode));
    }

    public void clearColorFilter() {
        setColorFilter(null);
    }

    public ColorFilter getColorFilter() {
        return null;
    }

    public void setTint(int tintColor) {
        setTintList(ColorStateList.valueOf(tintColor));
    }

    public void setTintList(ColorStateList tint) {
        tintList = tint;
        invalidateSelf();
    }

    public void setTintMode(PorterDuff.Mode mode) {
        tintMode = mode == null ? PorterDuff.Mode.SRC_IN : mode;
        invalidateSelf();
    }

    /// The filter the tint resolves to in the current state, or null.
    protected PorterDuffColorFilter tintFilter() {
        if (tintList == null) {
            return null;
        }
        return new PorterDuffColorFilter(tintList.getColorForState(getState(), tintList.getDefaultColor()), tintMode);
    }

    public void setHotspot(float x, float y) {
    }

    public void setHotspotBounds(int left, int top, int right, int bottom) {
    }

    public boolean isStateful() {
        return tintList != null && tintList.isStateful();
    }

    public boolean hasFocusStateSpecified() {
        return false;
    }

    public boolean setState(int[] stateSet) {
        if (!java.util.Arrays.equals(this.stateSet, stateSet)) {
            this.stateSet = stateSet;
            boolean changed = onStateChange(stateSet);
            if (tintList != null && tintList.isStateful()) {
                invalidateSelf();
                changed = true;
            }
            return changed;
        }
        return false;
    }

    public int[] getState() {
        return stateSet;
    }

    public void jumpToCurrentState() {
    }

    public Drawable getCurrent() {
        return this;
    }

    public final boolean setLevel(int level) {
        if (this.level != level) {
            this.level = level;
            return onLevelChange(level);
        }
        return false;
    }

    public final int getLevel() {
        return level;
    }

    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = this.visible != visible;
        if (changed) {
            this.visible = visible;
            invalidateSelf();
        }
        return changed;
    }

    public final boolean isVisible() {
        return visible;
    }

    public void setAutoMirrored(boolean mirrored) {
        autoMirrored = mirrored;
    }

    public boolean isAutoMirrored() {
        return autoMirrored;
    }

    public void applyTheme(Resources.Theme t) {
    }

    public boolean canApplyTheme() {
        return false;
    }

    /// One of `PixelFormat.TRANSLUCENT`, `TRANSPARENT` or `OPAQUE`.
    public abstract int getOpacity();

    public static int resolveOpacity(int op1, int op2) {
        if (op1 == op2) {
            return op1;
        }
        if (op1 == PixelFormat.UNKNOWN || op2 == PixelFormat.UNKNOWN) {
            return PixelFormat.UNKNOWN;
        }
        if (op1 == PixelFormat.TRANSLUCENT || op2 == PixelFormat.TRANSLUCENT) {
            return PixelFormat.TRANSLUCENT;
        }
        if (op1 == PixelFormat.TRANSPARENT || op2 == PixelFormat.TRANSPARENT) {
            return PixelFormat.TRANSPARENT;
        }
        return PixelFormat.OPAQUE;
    }

    protected boolean onStateChange(int[] state) {
        return false;
    }

    protected boolean onLevelChange(int level) {
        return false;
    }

    protected void onBoundsChange(Rect bounds) {
    }

    public int getIntrinsicWidth() {
        return -1;
    }

    public int getIntrinsicHeight() {
        return -1;
    }

    public int getMinimumWidth() {
        int w = getIntrinsicWidth();
        return w > 0 ? w : 0;
    }

    public int getMinimumHeight() {
        int h = getIntrinsicHeight();
        return h > 0 ? h : 0;
    }

    public boolean getPadding(Rect padding) {
        padding.set(0, 0, 0, 0);
        return false;
    }

    public Insets getOpticalInsets() {
        return Insets.NONE;
    }

    public Drawable mutate() {
        return this;
    }

    public ConstantState getConstantState() {
        return null;
    }

    public static Drawable createFromStream(InputStream is, String srcName) {
        android.graphics.Bitmap b = android.graphics.BitmapFactory.decodeStream(is);
        return b == null ? null : new BitmapDrawable(Resources.getSystem(), b);
    }

    public static Drawable createFromPath(String pathName) {
        android.graphics.Bitmap b = android.graphics.BitmapFactory.decodeFile(pathName);
        return b == null ? null : new BitmapDrawable(Resources.getSystem(), b);
    }

    /// The user color filter, else the tint, for subclasses that recolor.
    protected ColorFilter effectiveColorFilter(ColorFilter own) {
        if (own != null) {
            return own;
        }
        return tintFilter();
    }

    void setUserColorFilter(ColorFilter cf) {
        userColorFilter = cf;
    }

    ColorFilter userColorFilter() {
        return userColorFilter;
    }

    /// Optical insets, used for layout bounds; always empty here.
    public static final class Insets {
        public static final Insets NONE = new Insets(0, 0, 0, 0);
        public final int left;
        public final int top;
        public final int right;
        public final int bottom;

        Insets(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
