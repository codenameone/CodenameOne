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
package com.google.android.material.chip;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatCheckBox;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.MaterialAttrs;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.shape.Shapeable;

/// A Material chip: a compact, shaped element with a label and optional
/// chip icon, check icon (when checkable and checked) and close icon. Chips
/// are 32dp tall inside a 48dp touch target.
public class Chip extends AppCompatCheckBox implements Shapeable {

    private final MaterialShapeDrawable mSurface;
    private ColorStateList mChipBackground;
    private float mStrokeWidth;
    private ColorStateList mCheckedBackground;
    private ColorStateList mTextColor;
    private ColorStateList mCheckedTextColor;
    private boolean mCheckable;
    private Drawable mChipIcon;
    private boolean mChipIconVisible;
    private ColorStateList mChipIconTint;
    private int mChipIconSize;
    private Drawable mCheckedIcon;
    private boolean mCheckedIconVisible;
    private ColorStateList mCheckedIconTint;
    private Drawable mCloseIcon;
    private boolean mCloseIconVisible;
    private ColorStateList mCloseIconTint;
    private int mCloseIconSize;
    private int mChipStartPadding;
    private int mChipEndPadding;
    private int mTextStartPadding;
    private int mTextEndPadding;
    private int mChipMinHeight;
    private View.OnClickListener mCloseIconClick;
    private boolean mCloseIconPressed;
    private CompoundButton2Listener mGroupListener;

    /// The listener a [ChipGroup] installs to follow its chips.
    interface CompoundButton2Listener {
        void onCheckedChanged(Chip chip, boolean isChecked);
    }

    public Chip(Context context) {
        this(context, null);
    }

    public Chip(Context context, AttributeSet attrs) {
        this(context, attrs, MaterialAttrs.defStyleAttr(context, R.attr.chipStyle));
    }

    public Chip(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setButtonDrawable((Drawable) null);
        boolean m3 = MaterialAttrs.isMaterial3(context);
        int defStyleRes = m3 ? R.style.Widget_Material3_Chip_Assist : R.style.Widget_MaterialComponents_Chip_Action;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Chip, defStyleAttr,
                defStyleAttr == 0 ? defStyleRes : 0);
        try {
            int shapeRes = a.getResourceId(R.styleable.Chip_shapeAppearance, 0);
            int overlayRes = a.getResourceId(R.styleable.Chip_shapeAppearanceOverlay, 0);
            ShapeAppearanceModel.Builder sb = ShapeAppearanceModel.builder(context, shapeRes, overlayRes);
            if (a.hasValue(R.styleable.Chip_chipCornerRadius) || (shapeRes == 0 && overlayRes == 0)) {
                sb.setAllCornerSizes(a.getDimension(R.styleable.Chip_chipCornerRadius, MaterialAttrs.dp(context, 8)));
            }
            mSurface = new MaterialShapeDrawable(sb.build());
            mChipBackground = a.getColorStateList(R.styleable.Chip_chipBackgroundColor);
            if (mChipBackground == null) {
                mChipBackground = ColorStateList.valueOf(0);
            }
            mSurface.setStrokeColor(a.getColorStateList(R.styleable.Chip_chipStrokeColor));
            mStrokeWidth = a.getDimension(R.styleable.Chip_chipStrokeWidth, 0f);
            mSurface.setStrokeWidth(mStrokeWidth);
            mSurface.setStateLayerColor(a.getColorStateList(R.styleable.Chip_rippleColor));
            mChipMinHeight = a.getDimensionPixelSize(R.styleable.Chip_chipMinHeight, MaterialAttrs.dpi(context, 32));
            mCheckable = a.getBoolean(R.styleable.Chip_android_checkable, false);
            mChipIcon = a.getDrawable(R.styleable.Chip_chipIcon);
            mChipIconVisible = a.getBoolean(R.styleable.Chip_chipIconVisible,
                    a.getBoolean(R.styleable.Chip_chipIconEnabled, mChipIcon != null));
            mChipIconTint = a.getColorStateList(R.styleable.Chip_chipIconTint);
            mChipIconSize = a.getDimensionPixelSize(R.styleable.Chip_chipIconSize, MaterialAttrs.dpi(context, 18));
            mCheckedIcon = a.getDrawable(R.styleable.Chip_checkedIcon);
            if (mCheckedIcon == null) {
                mCheckedIcon = context.getDrawable(R.drawable.mtrl_ic_check_mark);
            }
            mCheckedIconVisible = a.getBoolean(R.styleable.Chip_checkedIconVisible,
                    a.getBoolean(R.styleable.Chip_checkedIconEnabled, false));
            mCheckedIconTint = a.getColorStateList(R.styleable.Chip_checkedIconTint);
            mCloseIcon = a.getDrawable(R.styleable.Chip_closeIcon);
            if (mCloseIcon == null) {
                mCloseIcon = context.getDrawable(m3 ? R.drawable.mtrl_ic_close : R.drawable.mtrl_ic_cancel);
            }
            mCloseIconVisible = a.getBoolean(R.styleable.Chip_closeIconVisible,
                    a.getBoolean(R.styleable.Chip_closeIconEnabled, false));
            mCloseIconTint = a.getColorStateList(R.styleable.Chip_closeIconTint);
            mCloseIconSize = a.getDimensionPixelSize(R.styleable.Chip_closeIconSize, MaterialAttrs.dpi(context, 18));
            mChipStartPadding = a.getDimensionPixelSize(R.styleable.Chip_chipStartPadding, MaterialAttrs.dpi(context, 8));
            mChipEndPadding = a.getDimensionPixelSize(R.styleable.Chip_chipEndPadding, MaterialAttrs.dpi(context, 8));
            mTextStartPadding = a.getDimensionPixelSize(R.styleable.Chip_textStartPadding, MaterialAttrs.dpi(context, 8));
            mTextEndPadding = a.getDimensionPixelSize(R.styleable.Chip_textEndPadding, MaterialAttrs.dpi(context, 8));
        } finally {
            a.recycle();
        }
        mTextColor = getTextColors();
        int onSurface = MaterialAttrs.onSurface(context);
        if (m3) {
            mCheckedBackground = ColorStateList.valueOf(
                    MaterialColors.getColor(context, R.attr.colorSecondaryContainer, 0xffe8def8));
            mCheckedTextColor = ColorStateList.valueOf(
                    MaterialColors.getColor(context, R.attr.colorOnSecondaryContainer, 0xff1d192b));
        } else {
            mCheckedBackground = ColorStateList.valueOf(MaterialColors.layer(mChipBackground.getDefaultColor(),
                    MaterialColors.getColor(context, R.attr.colorPrimary, 0xff6200ee), 0.24f));
            mCheckedTextColor = ColorStateList.valueOf(MaterialColors.getColor(context, R.attr.colorPrimary, 0xff6200ee));
        }
        if (mCheckedIconTint == null) {
            mCheckedIconTint = mCheckedTextColor;
        }
        mTextColor = MaterialColors.withDisabled(mTextColor, onSurface, MaterialColors.ALPHA_DISABLED);
        setBackground(mSurface);
        setGravity(Gravity.CENTER_VERTICAL);
        setMaxLines(1);
        int touch = MaterialAttrs.dpi(context, 48);
        int inset = Math.max(0, (touch - mChipMinHeight) / 2);
        mSurface.setInset(0, inset, 0, inset);
        setMinimumHeight(touch);
        applyState();
    }

    @Override
    public void toggle() {
        if (mCheckable) {
            super.toggle();
        }
    }

    @Override
    public void setChecked(boolean checked) {
        boolean was = isChecked();
        super.setChecked(checked);
        if (was != isChecked()) {
            applyState();
            if (mGroupListener != null) {
                mGroupListener.onCheckedChanged(this, isChecked());
            }
        }
    }

    void setGroupListener(CompoundButton2Listener listener) {
        mGroupListener = listener;
    }

    /// The colors and icons of the checked or unchecked state, and the
    /// paddings that place them.
    private void applyState() {
        if (mSurface == null) {
            return;
        }
        boolean checked = isChecked();
        mSurface.setFillColor(checked && mCheckable ? mCheckedBackground : mChipBackground);
        // A checked Material 3 filter chip is filled and has no outline; an
        // unchecked one gets its outline back.
        mSurface.setStrokeWidth(checked && mCheckable && MaterialAttrs.isMaterial3(getContext()) ? 0f : mStrokeWidth);
        setTextColor(checked && mCheckable ? mCheckedTextColor : mTextColor);
        Drawable start = null;
        if (checked && mCheckable && mCheckedIconVisible && mCheckedIcon != null) {
            start = MaterialAttrs.tinted(mCheckedIcon, mCheckedIconTint);
        } else if (mChipIconVisible && mChipIcon != null) {
            start = MaterialAttrs.tinted(mChipIcon, mChipIconTint);
        }
        if (start != null) {
            start.setBounds(0, 0, mChipIconSize, mChipIconSize);
        }
        Drawable end = null;
        if (mCloseIconVisible && mCloseIcon != null) {
            end = MaterialAttrs.tinted(mCloseIcon, mCloseIconTint);
            end.setBounds(0, 0, mCloseIconSize, mCloseIconSize);
        }
        setCompoundDrawablePadding(start != null || end != null ? mTextStartPadding / 2 : 0);
        setCompoundDrawablesRelative(start, null, end, null);
        int startPad = start != null ? mChipStartPadding : mChipStartPadding + mTextStartPadding;
        int endPad = end != null ? mChipEndPadding : mChipEndPadding + mTextEndPadding;
        if (start != null) {
            startPad = Math.max(0, mChipStartPadding - MaterialAttrs.dpi(getContext(), 4));
        }
        setPaddingRelative(startPad, 0, endPad, 0);
    }

    // ------------------------------------------------------------ attributes

    public void setCheckable(boolean checkable) {
        mCheckable = checkable;
        if (!checkable && isChecked()) {
            super.setChecked(false);
        }
        applyState();
    }

    public boolean isCheckable() {
        return mCheckable;
    }

    public void setChipBackgroundColor(ColorStateList color) {
        mChipBackground = color == null ? ColorStateList.valueOf(0) : color;
        applyState();
    }

    public void setChipBackgroundColorResource(int id) {
        setChipBackgroundColor(getContext().getColorStateList(id));
    }

    public ColorStateList getChipBackgroundColor() {
        return mChipBackground;
    }

    public void setChipStrokeColor(ColorStateList color) {
        mSurface.setStrokeColor(color);
        invalidate();
    }

    public void setChipStrokeWidth(float width) {
        mStrokeWidth = width;
        applyState();
        invalidate();
    }

    public float getChipStrokeWidth() {
        return mStrokeWidth;
    }

    public void setChipCornerRadius(float radius) {
        mSurface.setCornerSize(radius);
        invalidate();
    }

    public void setChipMinHeight(float minHeight) {
        mChipMinHeight = Math.round(minHeight);
        requestLayout();
    }

    public void setRippleColor(ColorStateList color) {
        mSurface.setStateLayerColor(color);
    }

    public void setChipIcon(Drawable icon) {
        mChipIcon = icon;
        mChipIconVisible = icon != null || mChipIconVisible;
        applyState();
    }

    public void setChipIconResource(int id) {
        setChipIcon(id == 0 ? null : getContext().getDrawable(id));
    }

    public Drawable getChipIcon() {
        return mChipIcon;
    }

    public void setChipIconVisible(boolean visible) {
        mChipIconVisible = visible;
        applyState();
    }

    public boolean isChipIconVisible() {
        return mChipIconVisible;
    }

    public void setChipIconTint(ColorStateList tint) {
        mChipIconTint = tint;
        applyState();
    }

    public void setChipIconSize(float size) {
        mChipIconSize = Math.round(size);
        applyState();
    }

    public void setCheckedIcon(Drawable icon) {
        mCheckedIcon = icon;
        applyState();
    }

    public void setCheckedIconVisible(boolean visible) {
        mCheckedIconVisible = visible;
        applyState();
    }

    public boolean isCheckedIconVisible() {
        return mCheckedIconVisible;
    }

    public void setCloseIcon(Drawable icon) {
        mCloseIcon = icon;
        applyState();
    }

    public void setCloseIconResource(int id) {
        setCloseIcon(id == 0 ? null : getContext().getDrawable(id));
    }

    public Drawable getCloseIcon() {
        return mCloseIcon;
    }

    public void setCloseIconVisible(boolean visible) {
        mCloseIconVisible = visible;
        applyState();
    }

    public boolean isCloseIconVisible() {
        return mCloseIconVisible;
    }

    public void setCloseIconTint(ColorStateList tint) {
        mCloseIconTint = tint;
        applyState();
    }

    public void setOnCloseIconClickListener(View.OnClickListener listener) {
        mCloseIconClick = listener;
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

    // ------------------------------------------------------------ close icon touch

    private boolean inCloseIcon(float x) {
        if (!mCloseIconVisible || mCloseIcon == null) {
            return false;
        }
        int area = mCloseIconSize + mChipEndPadding * 2;
        return isLayoutRtl() ? x < area : x > getWidth() - area;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN && inCloseIcon(event.getX())) {
            mCloseIconPressed = true;
            return true;
        }
        if (mCloseIconPressed) {
            if (action == MotionEvent.ACTION_UP) {
                mCloseIconPressed = false;
                if (inCloseIcon(event.getX()) && mCloseIconClick != null) {
                    mCloseIconClick.onClick(this);
                }
            } else if (action == MotionEvent.ACTION_CANCEL) {
                mCloseIconPressed = false;
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    public boolean performCloseIconClick() {
        if (mCloseIconClick == null) {
            return false;
        }
        mCloseIconClick.onClick(this);
        return true;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return mCheckable ? "android.widget.CompoundButton" : "android.widget.Button";
    }
}
