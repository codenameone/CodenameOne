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
package com.google.android.material.button;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Checkable;

import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.MaterialAttrs;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.shape.Shapeable;

/// A Material button: its background is a shaped surface filled with
/// `backgroundTint`, optionally stroked, with a pressed state layer in
/// `rippleColor`, inset by `insetTop`/`insetBottom` inside a 48dp touch
/// target, and with an optional icon. Under a Material theme a `<Button>`
/// inflates as this class.
public class MaterialButton extends AppCompatButton implements Checkable, Shapeable {

    public static final int ICON_GRAVITY_START = 0x1;
    public static final int ICON_GRAVITY_TEXT_START = 0x2;
    public static final int ICON_GRAVITY_END = 0x3;
    public static final int ICON_GRAVITY_TEXT_END = 0x4;
    public static final int ICON_GRAVITY_TOP = 0x10;
    public static final int ICON_GRAVITY_TEXT_TOP = 0x20;

    private static final int[] CHECKABLE_STATE = {android.R.attr.state_checkable};
    private static final int[] CHECKED_STATE = {android.R.attr.state_checked};

    /// Called when the checked state of a checkable button changes.
    public interface OnCheckedChangeListener {
        void onCheckedChanged(MaterialButton button, boolean isChecked);
    }

    private final MaterialShapeDrawable mSurface;
    private ColorStateList mBackgroundTint;
    private int mCornerRadius;
    private Drawable mIcon;
    private ColorStateList mIconTint;
    private int mIconSize;
    private int mIconPadding;
    private int mIconGravity = ICON_GRAVITY_TEXT_START;
    private boolean mCheckable;
    private boolean mChecked;
    private boolean mBroadcasting;
    private final java.util.List<OnCheckedChangeListener> mListeners = new java.util.ArrayList<OnCheckedChangeListener>();
    private OnCheckedChangeListener mGroupListener;

    public MaterialButton(Context context) {
        this(context, null);
    }

    public MaterialButton(Context context, AttributeSet attrs) {
        this(context, attrs, MaterialAttrs.defStyleAttr(context, R.attr.materialButtonStyle));
    }

    public MaterialButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, MaterialAttrs.isMaterial3(context) ? R.style.Widget_Material3_Button
                : R.style.Widget_MaterialComponents_Button);
    }

    /// `defStyleRes` styles the button when the theme does not define
    /// `defStyleAttr` (a subclass with a style of its own passes it).
    protected MaterialButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaterialButton, defStyleAttr,
                defStyleAttr == 0 ? defStyleRes : 0);
        ShapeAppearanceModel shape;
        try {
            int shapeRes = a.getResourceId(R.styleable.MaterialButton_shapeAppearance, 0);
            int overlayRes = a.getResourceId(R.styleable.MaterialButton_shapeAppearanceOverlay, 0);
            ShapeAppearanceModel.Builder sb = ShapeAppearanceModel.builder(context, shapeRes, overlayRes);
            if (a.hasValue(R.styleable.MaterialButton_cornerRadius)) {
                mCornerRadius = a.getDimensionPixelSize(R.styleable.MaterialButton_cornerRadius, 0);
                sb.setAllCornerSizes(mCornerRadius);
            }
            shape = sb.build();
            mSurface = new MaterialShapeDrawable(shape);
            int insetLeft = a.getDimensionPixelOffset(R.styleable.MaterialButton_android_insetLeft, 0);
            int insetRight = a.getDimensionPixelOffset(R.styleable.MaterialButton_android_insetRight, 0);
            int insetTop = a.getDimensionPixelOffset(R.styleable.MaterialButton_android_insetTop, 0);
            int insetBottom = a.getDimensionPixelOffset(R.styleable.MaterialButton_android_insetBottom, 0);
            mSurface.setInset(insetLeft, insetTop, insetRight, insetBottom);
            int onSurface = MaterialAttrs.onSurface(context);
            mBackgroundTint = MaterialColors.withDisabled(
                    MaterialAttrs.colorStateList(context, a, R.styleable.MaterialButton_backgroundTint, R.attr.colorPrimary),
                    onSurface, MaterialColors.ALPHA_DISABLED_LOW);
            mSurface.setFillColor(mBackgroundTint);
            ColorStateList stroke = a.getColorStateList(R.styleable.MaterialButton_strokeColor);
            mSurface.setStrokeColor(MaterialColors.withDisabled(stroke, onSurface, MaterialColors.ALPHA_DISABLED_LOW));
            mSurface.setStrokeWidth(a.getDimensionPixelSize(R.styleable.MaterialButton_strokeWidth, 0));
            mSurface.setStateLayerColor(a.getColorStateList(R.styleable.MaterialButton_rippleColor));
            float elevation = a.getDimension(R.styleable.MaterialButton_elevation, 0f);
            mIcon = a.getDrawable(R.styleable.MaterialButton_icon);
            mIconTint = MaterialColors.withDisabled(a.getColorStateList(R.styleable.MaterialButton_iconTint), onSurface,
                    MaterialColors.ALPHA_DISABLED);
            mIconSize = a.getDimensionPixelSize(R.styleable.MaterialButton_iconSize, 0);
            mIconPadding = a.getDimensionPixelSize(R.styleable.MaterialButton_iconPadding, 0);
            mIconGravity = a.getInt(R.styleable.MaterialButton_iconGravity, ICON_GRAVITY_TEXT_START);
            mCheckable = a.getBoolean(R.styleable.MaterialButton_android_checkable, false);
            setTextColor(MaterialColors.withDisabled(getTextColors(), onSurface, MaterialColors.ALPHA_DISABLED));
            setBackground(mSurface);
            // After setBackground, which hands the drawable the view's own
            // background tint: the fill keeps its disabled state.
            mSurface.setFillColor(mBackgroundTint);
            mSurface.setElevation(elevation);
        } finally {
            a.recycle();
        }
        updateIcon();
    }

    @Override
    protected boolean drawsOutsideBounds() {
        return mSurface != null && mSurface.getElevation() > 0 && isEnabled();
    }

    // ------------------------------------------------------------ surface

    @Override
    public void setBackgroundTintList(ColorStateList tint) {
        if (mSurface == null) {
            super.setBackgroundTintList(tint);
            return;
        }
        mBackgroundTint = MaterialColors.withDisabled(tint, MaterialAttrs.onSurface(getContext()),
                MaterialColors.ALPHA_DISABLED_LOW);
        mSurface.setFillColor(mBackgroundTint);
        invalidate();
    }

    @Override
    public ColorStateList getBackgroundTintList() {
        return mSurface == null ? super.getBackgroundTintList() : mBackgroundTint;
    }

    @Override
    public void setBackgroundColor(int color) {
        setBackgroundTintList(ColorStateList.valueOf(color));
    }

    public void setCornerRadius(int cornerRadius) {
        mCornerRadius = cornerRadius;
        mSurface.setCornerSize(cornerRadius);
        invalidate();
    }

    public void setCornerRadiusResource(int id) {
        setCornerRadius(getResources().getDimensionPixelSize(id));
    }

    /// The `cornerRadius` the button was given, or 0 when its corners come
    /// from a shape appearance alone -- as Material's own answers.
    public int getCornerRadius() {
        return mCornerRadius;
    }

    public void setStrokeColor(ColorStateList strokeColor) {
        mSurface.setStrokeColor(strokeColor);
        invalidate();
    }

    public ColorStateList getStrokeColor() {
        return mSurface.getStrokeColor();
    }

    public void setStrokeWidth(int strokeWidth) {
        mSurface.setStrokeWidth(strokeWidth);
        invalidate();
    }

    public int getStrokeWidth() {
        return Math.round(mSurface.getStrokeWidth());
    }

    public void setRippleColor(ColorStateList rippleColor) {
        mSurface.setStateLayerColor(rippleColor);
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

    @Override
    public void setElevation(float elevation) {
        super.setElevation(elevation);
        if (mSurface != null) {
            mSurface.setElevation(elevation);
        }
    }

    // ------------------------------------------------------------ icon

    public void setIcon(Drawable icon) {
        mIcon = icon;
        updateIcon();
    }

    public void setIconResource(int iconResourceId) {
        setIcon(iconResourceId == 0 ? null : getContext().getDrawable(iconResourceId));
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public void setIconTint(ColorStateList iconTint) {
        mIconTint = iconTint;
        updateIcon();
    }

    public ColorStateList getIconTint() {
        return mIconTint;
    }

    public void setIconSize(int iconSize) {
        mIconSize = iconSize;
        updateIcon();
    }

    public int getIconSize() {
        return mIconSize;
    }

    public void setIconPadding(int iconPadding) {
        mIconPadding = iconPadding;
        updateIcon();
    }

    public int getIconPadding() {
        return mIconPadding;
    }

    public void setIconGravity(int iconGravity) {
        mIconGravity = iconGravity;
        updateIcon();
    }

    public int getIconGravity() {
        return mIconGravity;
    }

    /// The icon is a compound drawable, as Material Components places it;
    /// the text-relative gravities sit at the same edges as the absolute
    /// ones.
    private void updateIcon() {
        Drawable icon = MaterialAttrs.tinted(mIcon, mIconTint);
        if (icon != null) {
            int w = mIconSize != 0 ? mIconSize : Math.max(0, icon.getIntrinsicWidth());
            int h = mIconSize != 0 ? mIconSize : Math.max(0, icon.getIntrinsicHeight());
            icon.setBounds(0, 0, w, h);
        }
        setCompoundDrawablePadding(mIconPadding);
        if (icon == null) {
            setCompoundDrawablesRelative(null, null, null, null);
        } else if (mIconGravity == ICON_GRAVITY_TOP || mIconGravity == ICON_GRAVITY_TEXT_TOP) {
            setCompoundDrawablesRelative(null, icon, null, null);
        } else if (mIconGravity == ICON_GRAVITY_END || mIconGravity == ICON_GRAVITY_TEXT_END) {
            setCompoundDrawablesRelative(null, null, icon, null);
        } else {
            setCompoundDrawablesRelative(icon, null, null, null);
        }
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
        if (!mCheckable || mChecked == checked) {
            return;
        }
        mChecked = checked;
        refreshDrawableState();
        if (mBroadcasting) {
            return;
        }
        mBroadcasting = true;
        for (OnCheckedChangeListener l : new java.util.ArrayList<OnCheckedChangeListener>(mListeners)) {
            l.onCheckedChanged(this, mChecked);
        }
        if (mGroupListener != null) {
            mGroupListener.onCheckedChanged(this, mChecked);
        }
        mBroadcasting = false;
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    @Override
    public boolean performClick() {
        if (isEnabled() && mCheckable) {
            toggle();
        }
        return super.performClick();
    }

    public void addOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mListeners.add(listener);
    }

    public void removeOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mListeners.remove(listener);
    }

    public void clearOnCheckedChangeListeners() {
        mListeners.clear();
    }

    void setGroupListener(OnCheckedChangeListener listener) {
        mGroupListener = listener;
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

    @Override
    public CharSequence getAccessibilityClassName() {
        return mCheckable ? "android.widget.CompoundButton" : "android.widget.Button";
    }
}
