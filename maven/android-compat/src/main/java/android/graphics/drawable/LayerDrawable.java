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
import android.view.Gravity;

import java.util.ArrayList;

/// A `<layer-list>`: children drawn in order, each inset within the bounds.
/// Padding nests, as Android's default `PADDING_MODE_NEST`: every layer is
/// inset by the padding of the layers below it, and the drawable's padding is
/// the sum.
public class LayerDrawable extends Drawable implements Drawable.Callback {

    public static final int PADDING_MODE_NEST = 0;
    public static final int PADDING_MODE_STACK = 1;
    public static final int INSET_UNDEFINED = Integer.MIN_VALUE;

    static final class Layer {
        Drawable drawable;
        int id = -1;
        int insetL;
        int insetT;
        int insetR;
        int insetB;
        int width = -1;
        int height = -1;
        int gravity = Gravity.NO_GRAVITY;
    }

    final ArrayList<Layer> layers = new ArrayList<Layer>();
    private int paddingMode = PADDING_MODE_NEST;
    private int alpha = 255;
    private ColorFilter colorFilter;

    public LayerDrawable(Drawable[] layers) {
        if (layers != null) {
            for (Drawable d : layers) {
                addLayer(d);
            }
        }
    }

    public int addLayer(Drawable dr) {
        Layer l = new Layer();
        l.drawable = dr;
        if (dr != null) {
            dr.setCallback(this);
        }
        layers.add(l);
        onBoundsChange(getBounds());
        return layers.size() - 1;
    }

    public int getNumberOfLayers() {
        return layers.size();
    }

    public Drawable getDrawable(int index) {
        return layers.get(index).drawable;
    }

    public int getId(int index) {
        return layers.get(index).id;
    }

    public void setId(int index, int id) {
        layers.get(index).id = id;
    }

    /// Returns the highest-index layer with `id`, while `findIndexByLayerId`
    /// (and so `setDrawableByLayerId`) uses the lowest. That asymmetry is
    /// Android's documented behaviour and is kept deliberately; it only shows
    /// when two layers share an id.
    public Drawable findDrawableByLayerId(int id) {
        for (int i = layers.size() - 1; i >= 0; i--) {
            if (layers.get(i).id == id) {
                return layers.get(i).drawable;
            }
        }
        return null;
    }

    public int findIndexByLayerId(int id) {
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    public boolean setDrawableByLayerId(int id, Drawable drawable) {
        int i = findIndexByLayerId(id);
        if (i < 0) {
            return false;
        }
        setDrawable(i, drawable);
        return true;
    }

    public void setDrawable(int index, Drawable drawable) {
        Layer l = layers.get(index);
        if (l.drawable != null) {
            l.drawable.setCallback(null);
        }
        l.drawable = drawable;
        if (drawable != null) {
            drawable.setCallback(this);
            drawable.setState(getState());
            drawable.setLevel(getLevel());
            drawable.setVisible(isVisible(), true);
            drawable.setAlpha(getAlpha());
            drawable.setLayoutDirection(getLayoutDirection());
            drawable.setAutoMirrored(isAutoMirrored());
            if (colorFilter != null) {
                drawable.setColorFilter(colorFilter);
            }
            if (tintList != null) {
                drawable.setTintList(tintList);
                drawable.setTintMode(tintMode);
            }
        }
        onBoundsChange(getBounds());
        invalidateSelf();
    }

    public void setLayerInset(int index, int l, int t, int r, int b) {
        Layer layer = layers.get(index);
        layer.insetL = l;
        layer.insetT = t;
        layer.insetR = r;
        layer.insetB = b;
        onBoundsChange(getBounds());
    }

    public void setLayerSize(int index, int w, int h) {
        Layer layer = layers.get(index);
        layer.width = w;
        layer.height = h;
        onBoundsChange(getBounds());
    }

    public void setLayerGravity(int index, int gravity) {
        layers.get(index).gravity = gravity;
        onBoundsChange(getBounds());
    }

    public void setPaddingMode(int mode) {
        paddingMode = mode;
        onBoundsChange(getBounds());
    }

    public int getPaddingMode() {
        return paddingMode;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        int padL = 0;
        int padT = 0;
        int padR = 0;
        int padB = 0;
        Rect pad = new Rect();
        Rect container = new Rect();
        Rect out = new Rect();
        for (Layer l : layers) {
            if (l.drawable == null) {
                continue;
            }
            container.set(bounds.left + l.insetL + padL, bounds.top + l.insetT + padT,
                    bounds.right - l.insetR - padR, bounds.bottom - l.insetB - padB);
            int gravity = l.gravity;
            int w = l.width >= 0 ? l.width : l.drawable.getIntrinsicWidth();
            int h = l.height >= 0 ? l.height : l.drawable.getIntrinsicHeight();
            if (gravity == Gravity.NO_GRAVITY && l.width < 0 && l.height < 0) {
                out.set(container);
            } else {
                if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == 0) {
                    gravity |= w < 0 ? Gravity.FILL_HORIZONTAL : Gravity.LEFT;
                }
                if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
                    gravity |= h < 0 ? Gravity.FILL_VERTICAL : Gravity.TOP;
                }
                Gravity.apply(Gravity.getAbsoluteGravity(gravity, getLayoutDirection()),
                        Math.max(0, w), Math.max(0, h), container, out);
            }
            l.drawable.setBounds(out);
            if (paddingMode == PADDING_MODE_NEST && l.drawable.getPadding(pad)) {
                padL += pad.left;
                padT += pad.top;
                padR += pad.right;
                padB += pad.bottom;
            }
        }
    }

    @Override
    public boolean getPadding(Rect padding) {
        padding.set(0, 0, 0, 0);
        Rect r = new Rect();
        boolean any = false;
        for (Layer l : layers) {
            if (l.drawable != null && l.drawable.getPadding(r)) {
                any = true;
                if (paddingMode == PADDING_MODE_NEST) {
                    padding.left += r.left;
                    padding.top += r.top;
                    padding.right += r.right;
                    padding.bottom += r.bottom;
                } else {
                    padding.left = Math.max(padding.left, r.left);
                    padding.top = Math.max(padding.top, r.top);
                    padding.right = Math.max(padding.right, r.right);
                    padding.bottom = Math.max(padding.bottom, r.bottom);
                }
            }
        }
        return any;
    }

    @Override
    public int getIntrinsicWidth() {
        int w = -1;
        int pad = 0;
        Rect r = new Rect();
        for (Layer l : layers) {
            if (l.drawable == null) {
                continue;
            }
            int lw = l.width >= 0 ? l.width : l.drawable.getIntrinsicWidth();
            if (lw >= 0) {
                w = Math.max(w, lw + l.insetL + l.insetR + pad);
            }
            if (paddingMode == PADDING_MODE_NEST && l.drawable.getPadding(r)) {
                pad += r.left + r.right;
            }
        }
        return w;
    }

    @Override
    public int getIntrinsicHeight() {
        int h = -1;
        int pad = 0;
        Rect r = new Rect();
        for (Layer l : layers) {
            if (l.drawable == null) {
                continue;
            }
            int lh = l.height >= 0 ? l.height : l.drawable.getIntrinsicHeight();
            if (lh >= 0) {
                h = Math.max(h, lh + l.insetT + l.insetB + pad);
            }
            if (paddingMode == PADDING_MODE_NEST && l.drawable.getPadding(r)) {
                pad += r.top + r.bottom;
            }
        }
        return h;
    }

    @Override
    public void draw(Canvas canvas) {
        for (Layer l : layers) {
            if (l.drawable != null) {
                l.drawable.draw(canvas);
            }
        }
    }

    @Override
    public boolean isStateful() {
        for (Layer l : layers) {
            if (l.drawable != null && l.drawable.isStateful()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean changed = false;
        for (Layer l : layers) {
            if (l.drawable != null && l.drawable.setState(state)) {
                changed = true;
            }
        }
        if (changed) {
            onBoundsChange(getBounds());
        }
        return changed;
    }

    @Override
    protected boolean onLevelChange(int level) {
        boolean changed = false;
        for (Layer l : layers) {
            if (l.drawable != null && l.drawable.setLevel(level)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        for (Layer l : layers) {
            if (l.drawable != null) {
                l.drawable.setAlpha(alpha);
            }
        }
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        colorFilter = cf;
        for (Layer l : layers) {
            if (l.drawable != null) {
                l.drawable.setColorFilter(cf);
            }
        }
    }

    @Override
    public ColorFilter getColorFilter() {
        return colorFilter;
    }

    @Override
    public void setTintList(android.content.res.ColorStateList tint) {
        super.setTintList(tint);
        for (Layer l : layers) {
            if (l.drawable != null) {
                l.drawable.setTintList(tint);
            }
        }
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        for (Layer l : layers) {
            if (l.drawable != null) {
                l.drawable.setVisible(visible, restart);
            }
        }
        return super.setVisible(visible, restart);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
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
