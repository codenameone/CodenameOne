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
package androidx.cardview.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.FrameLayout;

import androidx.cardview.R;

import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

/// A `FrameLayout` on a rounded, elevated card. As in AndroidX, the
/// content padding is set through `setContentPadding`; `setPadding` is
/// ignored. The card's shadow is drawn outside its bounds, as Android draws
/// an elevated view's.
public class CardView extends FrameLayout {

    private final MaterialShapeDrawable mSurface;
    private final int[] mContentPadding = new int[4];
    private boolean mUseCompatPadding;
    private boolean mPreventCornerOverlap = true;
    private float mMaxElevation;

    public CardView(Context context) {
        this(context, null);
    }

    public CardView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.cardViewStyle);
    }

    public CardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CardView, defStyleAttr, R.style.CardView);
        try {
            ColorStateList bg = a.getColorStateList(R.styleable.CardView_cardBackgroundColor);
            if (bg == null) {
                bg = ColorStateList.valueOf(defaultBackground(context));
            }
            float radius = a.getDimension(R.styleable.CardView_cardCornerRadius, 0f);
            float elevation = a.getDimension(R.styleable.CardView_cardElevation, 0f);
            mMaxElevation = a.getDimension(R.styleable.CardView_cardMaxElevation, elevation);
            mUseCompatPadding = a.getBoolean(R.styleable.CardView_cardUseCompatPadding, false);
            mPreventCornerOverlap = a.getBoolean(R.styleable.CardView_cardPreventCornerOverlap, true);
            int all = a.getDimensionPixelSize(R.styleable.CardView_contentPadding, 0);
            mContentPadding[0] = a.getDimensionPixelSize(R.styleable.CardView_contentPaddingLeft, all);
            mContentPadding[1] = a.getDimensionPixelSize(R.styleable.CardView_contentPaddingTop, all);
            mContentPadding[2] = a.getDimensionPixelSize(R.styleable.CardView_contentPaddingRight, all);
            mContentPadding[3] = a.getDimensionPixelSize(R.styleable.CardView_contentPaddingBottom, all);
            if (a.hasValue(R.styleable.CardView_android_minWidth)) {
                setMinimumWidth(a.getDimensionPixelSize(R.styleable.CardView_android_minWidth, 0));
            }
            if (a.hasValue(R.styleable.CardView_android_minHeight)) {
                setMinimumHeight(a.getDimensionPixelSize(R.styleable.CardView_android_minHeight, 0));
            }
            mSurface = new MaterialShapeDrawable(ShapeAppearanceModel.builder().setAllCornerSizes(radius).build());
            setBackground(mSurface);
            mSurface.setFillColor(bg);
            mSurface.setElevation(elevation);
        } finally {
            a.recycle();
        }
        applyContentPadding();
    }

    private static int defaultBackground(Context context) {
        TypedValue tv = new TypedValue();
        int light = 0xffffffff;
        if (context.getTheme().resolveAttribute(android.R.attr.colorBackground, tv, true)
                && tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            int c = tv.data;
            float lum = (0.2126f * ((c >> 16) & 0xff) + 0.7152f * ((c >> 8) & 0xff) + 0.0722f * (c & 0xff)) / 255f;
            return lum > 0.5f ? light : 0xff424242;
        }
        return light;
    }

    /// The surface behind the card's content; [com.google.android.material.card.MaterialCardView]
    /// strokes and shapes it.
    protected final MaterialShapeDrawable getCardSurface() {
        return mSurface;
    }

    @Override
    protected boolean drawsOutsideBounds() {
        return mSurface != null && mSurface.getElevation() > 0;
    }

    private void applyContentPadding() {
        super.setPadding(mContentPadding[0], mContentPadding[1], mContentPadding[2], mContentPadding[3]);
    }

    /// Ignored, as AndroidX's CardView ignores it; use [#setContentPadding].
    @Override
    public void setPadding(int left, int top, int right, int bottom) {
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
    }

    public void setContentPadding(int left, int top, int right, int bottom) {
        mContentPadding[0] = left;
        mContentPadding[1] = top;
        mContentPadding[2] = right;
        mContentPadding[3] = bottom;
        applyContentPadding();
    }

    public int getContentPaddingLeft() {
        return mContentPadding[0];
    }

    public int getContentPaddingTop() {
        return mContentPadding[1];
    }

    public int getContentPaddingRight() {
        return mContentPadding[2];
    }

    public int getContentPaddingBottom() {
        return mContentPadding[3];
    }

    public void setCardBackgroundColor(int color) {
        setCardBackgroundColor(ColorStateList.valueOf(color));
    }

    public void setCardBackgroundColor(ColorStateList color) {
        mSurface.setFillColor(color);
        invalidate();
    }

    public ColorStateList getCardBackgroundColor() {
        return mSurface.getFillColor();
    }

    public void setRadius(float radius) {
        mSurface.setCornerSize(radius);
        invalidate();
    }

    public float getRadius() {
        return mSurface.getShapeAppearanceModel().getTopLeftCornerSize(
                new android.graphics.RectF(0, 0, Math.max(1, getWidth()), Math.max(1, getHeight())));
    }

    public void setCardElevation(float elevation) {
        mSurface.setElevation(elevation);
        invalidate();
    }

    public float getCardElevation() {
        return mSurface.getElevation();
    }

    public void setMaxCardElevation(float maxElevation) {
        mMaxElevation = maxElevation;
    }

    public float getMaxCardElevation() {
        return mMaxElevation;
    }

    public void setUseCompatPadding(boolean useCompatPadding) {
        mUseCompatPadding = useCompatPadding;
    }

    public boolean getUseCompatPadding() {
        return mUseCompatPadding;
    }

    public void setPreventCornerOverlap(boolean preventCornerOverlap) {
        mPreventCornerOverlap = preventCornerOverlap;
    }

    public boolean getPreventCornerOverlap() {
        return mPreventCornerOverlap;
    }
}
