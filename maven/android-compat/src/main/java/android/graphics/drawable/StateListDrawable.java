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
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.StateSet;

import java.util.ArrayList;

/// A `<selector>`: shows the first child whose state spec matches the
/// current state.
public class StateListDrawable extends Drawable implements Drawable.Callback {

    private final ArrayList<int[]> specs = new ArrayList<int[]>();
    private final ArrayList<Drawable> drawables = new ArrayList<Drawable>();
    private int current = -1;
    private boolean variablePadding;
    private boolean constantSize;
    private int alpha = 255;
    private boolean hasAlpha;
    private ColorFilter colorFilter;
    private boolean hasColorFilter;
    // The container never paints itself, so whatever is set on it is kept here
    // and handed to every child, including states added afterwards -- as
    // Android's DrawableContainer does when it selects a child.
    private android.content.res.ColorStateList childTintList;
    private boolean hasTintList;
    private PorterDuff.Mode childTintMode;
    private boolean hasTintMode;

    public StateListDrawable() {
    }

    public void addState(int[] stateSet, Drawable drawable) {
        if (drawable == null) {
            return;
        }
        specs.add(stateSet);
        drawables.add(drawable);
        drawable.setCallback(this);
        if (hasAlpha) {
            drawable.setAlpha(alpha);
        }
        if (hasColorFilter) {
            drawable.setColorFilter(colorFilter);
        }
        if (hasTintList) {
            drawable.setTintList(childTintList);
        }
        if (hasTintMode) {
            drawable.setTintMode(childTintMode);
        }
        drawable.setBounds(getBounds());
        drawable.setVisible(false, true);
        onStateChange(getState());
    }

    public void setVariablePadding(boolean variable) {
        variablePadding = variable;
    }

    public void setConstantSize(boolean constant) {
        constantSize = constant;
    }

    public int getStateCount() {
        return specs.size();
    }

    public int[] getStateSet(int index) {
        return specs.get(index);
    }

    public Drawable getStateDrawable(int index) {
        return drawables.get(index);
    }

    public int findStateDrawableIndex(int[] stateSet) {
        for (int i = 0; i < specs.size(); i++) {
            if (StateSet.stateSetMatches(specs.get(i), stateSet)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected boolean onStateChange(int[] state) {
        int idx = findStateDrawableIndex(state);
        if (idx < 0) {
            idx = findStateDrawableIndex(StateSet.WILD_CARD);
        }
        boolean changed = idx != current;
        Drawable previous = getCurrent();
        if (changed && previous != null) {
            previous.setVisible(false, false);
        }
        current = idx;
        Drawable d = getCurrent();
        if (d != null && d != this) {
            d.setVisible(isVisible(), changed);
            d.setBounds(getBounds());
            if (d.setState(state)) {
                changed = true;
            }
        }
        if (changed) {
            invalidateSelf();
        }
        return changed;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public Drawable getCurrent() {
        return current >= 0 && current < drawables.size() ? drawables.get(current) : null;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        for (Drawable d : drawables) {
            d.setBounds(bounds);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Drawable d = getCurrent();
        if (d != null) {
            d.draw(canvas);
        }
    }

    @Override
    public boolean getPadding(Rect padding) {
        if (variablePadding) {
            Drawable d = getCurrent();
            return d != null ? d.getPadding(padding) : super.getPadding(padding);
        }
        // Constant padding: the largest of every state's, so the view does not
        // jump when its state changes.
        Rect r = new Rect();
        Rect max = new Rect();
        boolean any = false;
        for (Drawable d : drawables) {
            if (d.getPadding(r)) {
                any = true;
                max.left = Math.max(max.left, r.left);
                max.top = Math.max(max.top, r.top);
                max.right = Math.max(max.right, r.right);
                max.bottom = Math.max(max.bottom, r.bottom);
            }
        }
        padding.set(max);
        return any;
    }

    @Override
    public int getIntrinsicWidth() {
        if (constantSize) {
            int w = -1;
            for (Drawable d : drawables) {
                w = Math.max(w, d.getIntrinsicWidth());
            }
            return w;
        }
        Drawable d = getCurrent();
        return d == null ? -1 : d.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        if (constantSize) {
            int h = -1;
            for (Drawable d : drawables) {
                h = Math.max(h, d.getIntrinsicHeight());
            }
            return h;
        }
        Drawable d = getCurrent();
        return d == null ? -1 : d.getIntrinsicHeight();
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        hasAlpha = true;
        for (Drawable d : drawables) {
            d.setAlpha(alpha);
        }
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        colorFilter = cf;
        hasColorFilter = true;
        for (Drawable d : drawables) {
            d.setColorFilter(cf);
        }
    }

    @Override
    public ColorFilter getColorFilter() {
        return colorFilter;
    }

    @Override
    public void setTintList(android.content.res.ColorStateList tint) {
        childTintList = tint;
        hasTintList = true;
        for (Drawable d : drawables) {
            d.setTintList(tint);
        }
    }

    @Override
    public void setTintMode(PorterDuff.Mode mode) {
        childTintMode = mode;
        hasTintMode = true;
        for (Drawable d : drawables) {
            d.setTintMode(mode);
        }
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        Drawable d = getCurrent();
        if (d != null) {
            d.setVisible(visible, restart);
        }
        return super.setVisible(visible, restart);
    }

    @Override
    public int getOpacity() {
        Drawable d = getCurrent();
        return d == null ? PixelFormat.TRANSPARENT : d.getOpacity();
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        if (who == getCurrent()) {
            invalidateSelf();
        }
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        if (who == getCurrent() && isVisible()) {
            scheduleSelf(what, when);
        }
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }
}
