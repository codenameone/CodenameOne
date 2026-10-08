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
package com.google.android.material.shape;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.google.android.material.R;

/// The shape of a Material surface: a size and a family (rounded or cut)
/// for each corner. A size is absolute (pixels) or relative to the shorter
/// side, as `50%` in a shape appearance style makes a pill.
public class ShapeAppearanceModel {

    private static final int TL = 0;
    private static final int TR = 1;
    private static final int BR = 2;
    private static final int BL = 3;

    private final float[] sizes;
    private final boolean[] relative;
    private final int family;

    ShapeAppearanceModel(Builder b) {
        sizes = new float[] {b.sizes[0], b.sizes[1], b.sizes[2], b.sizes[3]};
        relative = new boolean[] {b.relative[0], b.relative[1], b.relative[2], b.relative[3]};
        family = b.family;
    }

    public ShapeAppearanceModel() {
        this(new Builder());
    }

    public static Builder builder() {
        return new Builder();
    }

    /// Reads a shape appearance style and an optional overlay on top of it.
    public static Builder builder(Context context, int shapeAppearanceResId, int overlayResId) {
        Builder b = new Builder();
        if (shapeAppearanceResId != 0) {
            b.read(context, shapeAppearanceResId);
        }
        if (overlayResId != 0) {
            b.read(context, overlayResId);
        }
        return b;
    }

    /// Reads `shapeAppearance` and `shapeAppearanceOverlay` from a view's
    /// attributes and style.
    public static Builder builder(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaterialShape, defStyleAttr, defStyleRes);
        int shape = a.getResourceId(R.styleable.MaterialShape_shapeAppearance, 0);
        int overlay = a.getResourceId(R.styleable.MaterialShape_shapeAppearanceOverlay, 0);
        a.recycle();
        return builder(context, shape, overlay);
    }

    public Builder toBuilder() {
        Builder b = new Builder();
        for (int i = 0; i < 4; i++) {
            b.sizes[i] = sizes[i];
            b.relative[i] = relative[i];
        }
        b.family = family;
        return b;
    }

    public ShapeAppearanceModel withCornerSize(float cornerSize) {
        return toBuilder().setAllCornerSizes(cornerSize).build();
    }

    public int getCornerFamily() {
        return family;
    }

    /// The size of the top-left corner in pixels, for a shape of `bounds`.
    public float getTopLeftCornerSize(RectF bounds) {
        return resolve(TL, bounds);
    }

    public float getTopRightCornerSize(RectF bounds) {
        return resolve(TR, bounds);
    }

    public float getBottomRightCornerSize(RectF bounds) {
        return resolve(BR, bounds);
    }

    public float getBottomLeftCornerSize(RectF bounds) {
        return resolve(BL, bounds);
    }

    private float resolve(int corner, RectF bounds) {
        float min = Math.min(bounds.width(), bounds.height());
        float v = relative[corner] ? sizes[corner] * min : sizes[corner];
        return Math.max(0f, Math.min(v, min / 2f));
    }

    /// The radii `Path.addRoundRect` takes, clamped to the bounds.
    public float[] radii(RectF bounds) {
        float tl = resolve(TL, bounds);
        float tr = resolve(TR, bounds);
        float br = resolve(BR, bounds);
        float bl = resolve(BL, bounds);
        return new float[] {tl, tl, tr, tr, br, br, bl, bl};
    }

    public boolean isRoundRect(RectF bounds) {
        float tl = resolve(TL, bounds);
        return family == CornerFamily.ROUNDED && tl == resolve(TR, bounds) && tl == resolve(BR, bounds)
                && tl == resolve(BL, bounds);
    }

    /// Builds a [ShapeAppearanceModel].
    public static final class Builder {
        final float[] sizes = new float[4];
        final boolean[] relative = new boolean[4];
        int family = CornerFamily.ROUNDED;

        public Builder setAllCorners(int cornerFamily, float cornerSize) {
            family = cornerFamily;
            return setAllCornerSizes(cornerSize);
        }

        public Builder setAllCornerSizes(float cornerSize) {
            for (int i = 0; i < 4; i++) {
                sizes[i] = cornerSize;
                relative[i] = false;
            }
            return this;
        }

        /// Every corner sized relative to the shorter side (0.5 is a pill).
        public Builder setAllCornerSizesRelative(float fraction) {
            for (int i = 0; i < 4; i++) {
                sizes[i] = fraction;
                relative[i] = true;
            }
            return this;
        }

        public Builder setTopLeftCornerSize(float size) {
            return set(TL, size);
        }

        public Builder setTopRightCornerSize(float size) {
            return set(TR, size);
        }

        public Builder setBottomRightCornerSize(float size) {
            return set(BR, size);
        }

        public Builder setBottomLeftCornerSize(float size) {
            return set(BL, size);
        }

        private Builder set(int corner, float size) {
            sizes[corner] = size;
            relative[corner] = false;
            return this;
        }

        public Builder setCornerFamily(int cornerFamily) {
            family = cornerFamily;
            return this;
        }

        void read(Context context, int styleResId) {
            TypedArray a = context.obtainStyledAttributes(styleResId, R.styleable.ShapeAppearance);
            try {
                family = a.getInt(R.styleable.ShapeAppearance_cornerFamily, family);
                if (a.hasValue(R.styleable.ShapeAppearance_cornerSize)) {
                    readCorner(a, R.styleable.ShapeAppearance_cornerSize, -1);
                }
                readCorner(a, R.styleable.ShapeAppearance_cornerSizeTopLeft, TL);
                readCorner(a, R.styleable.ShapeAppearance_cornerSizeTopRight, TR);
                readCorner(a, R.styleable.ShapeAppearance_cornerSizeBottomRight, BR);
                readCorner(a, R.styleable.ShapeAppearance_cornerSizeBottomLeft, BL);
            } finally {
                a.recycle();
            }
        }

        private void readCorner(TypedArray a, int index, int corner) {
            TypedValue v = a.peekValue(index);
            if (v == null || v.type == TypedValue.TYPE_NULL) {
                return;
            }
            boolean rel = v.type == TypedValue.TYPE_FRACTION;
            float size = rel ? a.getFraction(index, 1, 1, 0f) : a.getDimension(index, 0f);
            for (int i = 0; i < 4; i++) {
                if (corner < 0 || corner == i) {
                    sizes[i] = size;
                    relative[i] = rel;
                }
            }
        }

        public ShapeAppearanceModel build() {
            return new ShapeAppearanceModel(this);
        }
    }
}
