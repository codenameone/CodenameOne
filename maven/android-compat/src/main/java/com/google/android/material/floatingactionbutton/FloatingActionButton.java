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
package com.google.android.material.floatingactionbutton;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.MaterialAttrs;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.shape.Shapeable;

/// A floating action button: an elevated, shaped button holding one icon,
/// 56dp (40dp mini), with animated [#show()] and [#hide()].
public class FloatingActionButton extends ImageButton implements Shapeable {

    public static final int SIZE_MINI = 1;
    public static final int SIZE_NORMAL = 0;
    public static final int SIZE_AUTO = -1;
    public static final int NO_CUSTOM_SIZE = 0;

    /// Told when an animated show or hide finishes.
    public abstract static class OnVisibilityChangedListener {
        public void onShown(FloatingActionButton fab) {
        }

        public void onHidden(FloatingActionButton fab) {
        }
    }

    private final MaterialShapeDrawable mSurface;
    private int mSize;
    private int mCustomSize;
    private int mMaxImageSize;
    private boolean mUseCompatPadding;
    private boolean mHiding;

    public FloatingActionButton(Context context) {
        this(context, null);
    }

    public FloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, MaterialAttrs.defStyleAttr(context, R.attr.floatingActionButtonStyle));
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int defStyleRes = MaterialAttrs.isMaterial3(context) ? R.style.Widget_Material3_FloatingActionButton_Primary
                : R.style.Widget_MaterialComponents_FloatingActionButton;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FloatingActionButton, defStyleAttr,
                defStyleAttr == 0 ? defStyleRes : 0);
        try {
            mSize = a.getInt(R.styleable.FloatingActionButton_fabSize, SIZE_AUTO);
            mCustomSize = a.getDimensionPixelSize(R.styleable.FloatingActionButton_fabCustomSize, NO_CUSTOM_SIZE);
            mMaxImageSize = a.getDimensionPixelSize(R.styleable.FloatingActionButton_maxImageSize,
                    MaterialAttrs.dpi(context, 24));
            mUseCompatPadding = a.getBoolean(R.styleable.FloatingActionButton_useCompatPadding, false);
            int shapeRes = a.getResourceId(R.styleable.FloatingActionButton_shapeAppearance, 0);
            int overlayRes = a.getResourceId(R.styleable.FloatingActionButton_shapeAppearanceOverlay, 0);
            ShapeAppearanceModel shape = shapeRes != 0 || overlayRes != 0
                    ? ShapeAppearanceModel.builder(context, shapeRes, overlayRes).build()
                    : ShapeAppearanceModel.builder().setAllCornerSizesRelative(0.5f).build();
            mSurface = new MaterialShapeDrawable(shape);
            ColorStateList bg = MaterialAttrs.colorStateList(context, a, R.styleable.FloatingActionButton_backgroundTint,
                    R.attr.colorSecondary);
            mSurface.setStateLayerColor(a.getColorStateList(R.styleable.FloatingActionButton_rippleColor));
            float elevation = a.getDimension(R.styleable.FloatingActionButton_elevation, MaterialAttrs.dp(context, 6));
            Drawable src = a.getDrawable(R.styleable.FloatingActionButton_srcCompat);
            if (src != null) {
                setImageDrawable(src);
            }
            ColorStateList tint = a.getColorStateList(R.styleable.FloatingActionButton_tint);
            if (tint != null) {
                setImageTintList(tint);
            }
            setBackground(mSurface);
            mSurface.setFillColor(MaterialColors.withDisabled(bg, MaterialAttrs.onSurface(context),
                    MaterialColors.ALPHA_DISABLED_LOW));
            mSurface.setElevation(elevation);
        } finally {
            a.recycle();
        }
        setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        setClickable(true);
    }

    @Override
    protected boolean drawsOutsideBounds() {
        return mSurface != null && mSurface.getElevation() > 0;
    }

    /// The diameter for the size setting: mini is 40dp, normal 56dp, auto
    /// is mini only on very small screens, as Material Components decides.
    public int getSizeDimension() {
        if (mCustomSize != NO_CUSTOM_SIZE) {
            return mCustomSize;
        }
        int size = mSize;
        if (size == SIZE_AUTO) {
            android.content.res.Configuration c = getResources().getConfiguration();
            size = Math.max(c.screenWidthDp, c.screenHeightDp) < 470 ? SIZE_MINI : SIZE_NORMAL;
        }
        return MaterialAttrs.dpi(getContext(), size == SIZE_MINI ? 40 : 56);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int d = getSizeDimension();
        int image = Math.min(mMaxImageSize, d);
        int pad = (d - image) / 2;
        super.setPadding(pad, pad, pad, pad);
        int w = resolveSize(d, widthMeasureSpec);
        int h = resolveSize(d, heightMeasureSpec);
        int s = Math.min(w, h);
        setMeasuredDimension(s, s);
    }

    public void setSize(int size) {
        mCustomSize = NO_CUSTOM_SIZE;
        if (size != mSize) {
            mSize = size;
            requestLayout();
        }
    }

    public int getSize() {
        return mSize;
    }

    public void setCustomSize(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Custom size must be non-negative");
        }
        mCustomSize = size;
        requestLayout();
    }

    public int getCustomSize() {
        return mCustomSize;
    }

    public void clearCustomSize() {
        setCustomSize(NO_CUSTOM_SIZE);
    }

    public void setMaxImageSize(int imageSize) {
        mMaxImageSize = imageSize;
        requestLayout();
    }

    public void setUseCompatPadding(boolean useCompatPadding) {
        mUseCompatPadding = useCompatPadding;
    }

    public boolean getUseCompatPadding() {
        return mUseCompatPadding;
    }

    @Override
    public void setBackgroundTintList(ColorStateList tint) {
        if (mSurface == null) {
            super.setBackgroundTintList(tint);
            return;
        }
        mSurface.setFillColor(tint);
        invalidate();
    }

    @Override
    public ColorStateList getBackgroundTintList() {
        return mSurface == null ? super.getBackgroundTintList() : mSurface.getFillColor();
    }

    @Override
    public void setBackgroundColor(int color) {
        setBackgroundTintList(ColorStateList.valueOf(color));
    }

    public void setRippleColor(int color) {
        setRippleColor(ColorStateList.valueOf(color));
    }

    public void setRippleColor(ColorStateList color) {
        mSurface.setStateLayerColor(color);
    }

    @Override
    public void setElevation(float elevation) {
        super.setElevation(elevation);
        if (mSurface != null) {
            mSurface.setElevation(elevation);
        }
    }

    public void setCompatElevation(float elevation) {
        setElevation(elevation);
    }

    public float getCompatElevation() {
        return mSurface.getElevation();
    }

    @Override
    public void setShapeAppearanceModel(ShapeAppearanceModel shapeAppearanceModel) {
        mSurface.setShapeAppearanceModel(shapeAppearanceModel);
        invalidate();
    }

    @Override
    public ShapeAppearanceModel getShapeAppearanceModel() {
        return mSurface.getShapeAppearanceModel();
    }

    // ------------------------------------------------------------ show and hide

    public void show() {
        show(null);
    }

    /// Scales and fades the button in.
    public void show(final OnVisibilityChangedListener listener) {
        if (getVisibility() == View.VISIBLE && !mHiding) {
            if (listener != null) {
                listener.onShown(this);
            }
            return;
        }
        mHiding = false;
        animate().cancel();
        if (getVisibility() != View.VISIBLE) {
            setScaleX(0f);
            setScaleY(0f);
            setAlpha(0f);
        }
        setVisibility(View.VISIBLE);
        animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(150).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (listener != null) {
                    listener.onShown(FloatingActionButton.this);
                }
            }
        }).start();
    }

    public void hide() {
        hide(null);
    }

    /// Scales and fades the button out, then makes it invisible.
    public void hide(final OnVisibilityChangedListener listener) {
        if (getVisibility() != View.VISIBLE || mHiding) {
            if (listener != null && getVisibility() != View.VISIBLE) {
                listener.onHidden(this);
            }
            return;
        }
        mHiding = true;
        animate().cancel();
        animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(100).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mHiding) {
                    mHiding = false;
                    setVisibility(View.INVISIBLE);
                    setScaleX(1f);
                    setScaleY(1f);
                    setAlpha(1f);
                    if (listener != null) {
                        listener.onHidden(FloatingActionButton.this);
                    }
                }
            }
        }).start();
    }

    public boolean isOrWillBeShown() {
        return getVisibility() == View.VISIBLE && !mHiding;
    }

    public boolean isOrWillBeHidden() {
        return getVisibility() != View.VISIBLE || mHiding;
    }
}
