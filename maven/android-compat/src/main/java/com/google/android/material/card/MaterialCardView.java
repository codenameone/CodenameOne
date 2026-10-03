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
package com.google.android.material.card;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Checkable;

import androidx.cardview.widget.CardView;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.MaterialAttrs;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.shape.Shapeable;

/// A Material card: a [CardView] that can be stroked, shaped by a shape
/// appearance, show a pressed state layer, and be checkable with a check
/// icon at its top end corner.
public class MaterialCardView extends CardView implements Checkable, Shapeable {

    private static final int[] CHECKABLE_STATE = {android.R.attr.state_checkable};
    private static final int[] CHECKED_STATE = {android.R.attr.state_checked};

    /// Called when a checkable card is checked or unchecked.
    public interface OnCheckedChangeListener {
        void onCheckedChanged(MaterialCardView card, boolean isChecked);
    }

    private boolean mCheckable;
    private boolean mChecked;
    private Drawable mCheckedIcon;
    private ColorStateList mCheckedIconTint;
    private OnCheckedChangeListener mListener;

    public MaterialCardView(Context context) {
        this(context, null);
    }

    public MaterialCardView(Context context, AttributeSet attrs) {
        this(context, attrs, MaterialAttrs.defStyleAttr(context, R.attr.materialCardViewStyle));
    }

    public MaterialCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int defStyleRes = MaterialAttrs.isMaterial3(context) ? R.style.Widget_Material3_CardView_Elevated
                : R.style.Widget_MaterialComponents_CardView;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaterialCardView, defStyleAttr,
                defStyleAttr == 0 ? defStyleRes : 0);
        try {
            int shapeRes = a.getResourceId(R.styleable.MaterialCardView_shapeAppearance, 0);
            int overlayRes = a.getResourceId(R.styleable.MaterialCardView_shapeAppearanceOverlay, 0);
            if (shapeRes != 0 || overlayRes != 0) {
                getCardSurface().setShapeAppearanceModel(
                        ShapeAppearanceModel.builder(context, shapeRes, overlayRes).build());
            }
            ColorStateList stroke = a.getColorStateList(R.styleable.MaterialCardView_strokeColor);
            getCardSurface().setStrokeColor(stroke);
            getCardSurface().setStrokeWidth(a.getDimensionPixelSize(R.styleable.MaterialCardView_strokeWidth, 0));
            getCardSurface().setStateLayerColor(a.getColorStateList(R.styleable.MaterialCardView_rippleColor));
            mCheckable = a.getBoolean(R.styleable.MaterialCardView_android_checkable, false);
            mCheckedIcon = a.getDrawable(R.styleable.MaterialCardView_checkedIcon);
            if (mCheckedIcon == null) {
                mCheckedIcon = context.getDrawable(R.drawable.mtrl_ic_check_mark);
            }
            mCheckedIconTint = a.getColorStateList(R.styleable.MaterialCardView_checkedIconTint);
            if (mCheckedIconTint == null) {
                mCheckedIconTint = ColorStateList.valueOf(MaterialColors.getColor(context, R.attr.colorPrimary, 0xff6750a4));
            }
        } finally {
            a.recycle();
        }
        setWillNotDraw(false);
    }

    public void setStrokeColor(int strokeColor) {
        setStrokeColor(ColorStateList.valueOf(strokeColor));
    }

    public void setStrokeColor(ColorStateList strokeColor) {
        getCardSurface().setStrokeColor(strokeColor);
        invalidate();
    }

    public ColorStateList getStrokeColorStateList() {
        return getCardSurface().getStrokeColor();
    }

    public void setStrokeWidth(int strokeWidth) {
        getCardSurface().setStrokeWidth(strokeWidth);
        invalidate();
    }

    public int getStrokeWidth() {
        return Math.round(getCardSurface().getStrokeWidth());
    }

    public void setRippleColor(ColorStateList rippleColor) {
        getCardSurface().setStateLayerColor(rippleColor);
    }

    @Override
    public void setShapeAppearanceModel(ShapeAppearanceModel shapeAppearanceModel) {
        getCardSurface().setShapeAppearanceModel(shapeAppearanceModel);
        invalidate();
    }

    @Override
    public ShapeAppearanceModel getShapeAppearanceModel() {
        return getCardSurface().getShapeAppearanceModel();
    }

    // ------------------------------------------------------------ checkable

    public boolean isCheckable() {
        return mCheckable;
    }

    public void setCheckable(boolean checkable) {
        mCheckable = checkable;
        refreshDrawableState();
    }

    @Override
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            toggle();
        }
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        if (mCheckable && isEnabled()) {
            mChecked = !mChecked;
            refreshDrawableState();
            invalidate();
            if (mListener != null) {
                mListener.onCheckedChanged(this, mChecked);
            }
        }
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mListener = listener;
    }

    public void setCheckedIcon(Drawable checkedIcon) {
        mCheckedIcon = checkedIcon;
        invalidate();
    }

    public Drawable getCheckedIcon() {
        return mCheckedIcon;
    }

    public void setCheckedIconTint(ColorStateList checkedIconTint) {
        mCheckedIconTint = checkedIconTint;
        invalidate();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] state = super.onCreateDrawableState(extraSpace + 2);
        if (mCheckable) {
            mergeDrawableStates(state, CHECKABLE_STATE);
        }
        if (mChecked) {
            mergeDrawableStates(state, CHECKED_STATE);
        }
        return state;
    }

    /// The check icon of a checked card, 24dp at the top end corner with an
    /// 8dp margin, above the content.
    @Override
    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);
        if (!mChecked || mCheckedIcon == null) {
            return;
        }
        int size = MaterialAttrs.dpi(getContext(), 24);
        int margin = MaterialAttrs.dpi(getContext(), 8);
        Drawable icon = MaterialAttrs.tinted(mCheckedIcon, mCheckedIconTint);
        int left = isLayoutRtl() ? margin : getWidth() - margin - size;
        icon.setBounds(left, margin, left + size, margin + size);
        icon.draw(canvas);
    }
}
