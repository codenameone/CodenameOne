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
package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;

/// A button with a checked state: the base of CheckBox, RadioButton, Switch
/// and ToggleButton. The button drawable is drawn at the start, vertically
/// centered, and the text follows it, as in AOSP.
public abstract class CompoundButton extends Button implements Checkable {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(CompoundButton buttonView, boolean isChecked);
    }

    private boolean mChecked;
    private boolean mBroadcasting;
    private Drawable mButtonDrawable;
    private ColorStateList mButtonTint;
    private OnCheckedChangeListener mOnCheckedChangeListener;
    private OnCheckedChangeListener mOnCheckedChangeWidgetListener;

    public CompoundButton(Context context) {
        this(context, null);
    }

    public CompoundButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompoundButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CompoundButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.CompoundButton, defStyleAttr,
                defStyleRes);
        Drawable d = a.getDrawable(android.R.styleable.CompoundButton_button);
        if (a.hasValue(android.R.styleable.CompoundButton_buttonTint)) {
            mButtonTint = a.getColorStateList(android.R.styleable.CompoundButton_buttonTint);
        }
        boolean checked = a.getBoolean(android.R.styleable.CompoundButton_checked, false);
        boolean explicitButton = a.hasValueOrEmpty(android.R.styleable.CompoundButton_button);
        a.recycle();
        if (d == null && !explicitButton) {
            d = createDefaultButtonDrawable(context);
        }
        setButtonDrawable(d);
        setChecked(checked);
        setClickable(true);
        setFocusable(true);
    }

    /// The glyph used when no `android:button` is given; none for the base.
    Drawable createDefaultButtonDrawable(Context context) {
        return null;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    @Override
    public boolean performClick() {
        toggle();
        return super.performClick();
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (mChecked == checked) {
            return;
        }
        mChecked = checked;
        refreshDrawableState();
        invalidate();
        if (mBroadcasting) {
            return;
        }
        mBroadcasting = true;
        try {
            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged(this, mChecked);
            }
            if (mOnCheckedChangeWidgetListener != null) {
                mOnCheckedChangeWidgetListener.onCheckedChanged(this, mChecked);
            }
        } finally {
            mBroadcasting = false;
        }
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    /// The listener a containing RadioGroup installs.
    void setOnCheckedChangeWidgetListener(OnCheckedChangeListener listener) {
        mOnCheckedChangeWidgetListener = listener;
    }

    public void setButtonDrawable(int resId) {
        setButtonDrawable(resId == 0 ? null : getContext().getDrawable(resId));
    }

    public void setButtonDrawable(Drawable drawable) {
        if (mButtonDrawable == drawable) {
            return;
        }
        if (mButtonDrawable != null) {
            mButtonDrawable.setCallback(null);
        }
        mButtonDrawable = drawable;
        if (drawable != null) {
            drawable.setCallback(this);
            if (mButtonTint != null) {
                drawable.setTintList(mButtonTint);
            }
            if (drawable.isStateful()) {
                drawable.setState(getDrawableState());
            }
            drawable.setVisible(getVisibility() == VISIBLE, false);
        }
        textChanged();
    }

    public Drawable getButtonDrawable() {
        return mButtonDrawable;
    }

    public void setButtonTintList(ColorStateList tint) {
        mButtonTint = tint;
        if (mButtonDrawable != null) {
            mButtonDrawable.setTintList(tint);
        }
        invalidate();
    }

    public ColorStateList getButtonTintList() {
        return mButtonTint;
    }

    public void setButtonTintMode(PorterDuff.Mode mode) {
        if (mButtonDrawable != null) {
            mButtonDrawable.setTintMode(mode);
        }
    }

    private int buttonWidth() {
        return mButtonDrawable == null ? 0 : Math.max(0, mButtonDrawable.getIntrinsicWidth());
    }

    private int buttonHeight() {
        return mButtonDrawable == null ? 0 : Math.max(0, mButtonDrawable.getIntrinsicHeight());
    }

    @Override
    public int getCompoundPaddingLeft() {
        int p = super.getCompoundPaddingLeft();
        if (!isLayoutRtl()) {
            p += buttonWidth();
        }
        return p;
    }

    @Override
    public int getCompoundPaddingRight() {
        int p = super.getCompoundPaddingRight();
        if (isLayoutRtl()) {
            p += buttonWidth();
        }
        return p;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int need = buttonHeight();
        if (getMeasuredHeight() < need && MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            int h = need;
            if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
                h = Math.min(h, MeasureSpec.getSize(heightMeasureSpec));
            }
            setMeasuredDimension(getMeasuredWidthAndState(), h);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable d = mButtonDrawable;
        if (d != null) {
            int vg = getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
            int h = buttonHeight();
            int w = buttonWidth();
            int top;
            if (vg == Gravity.BOTTOM) {
                top = getHeight() - h;
            } else if (vg == Gravity.CENTER_VERTICAL) {
                top = (getHeight() - h) / 2;
            } else {
                top = 0;
            }
            int left = isLayoutRtl() ? getWidth() - w : 0;
            d.setBounds(left + mScrollX, top + mScrollY, left + w + mScrollX, top + h + mScrollY);
            d.draw(canvas);
        }
        super.onDraw(canvas);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] state = super.onCreateDrawableState(extraSpace + 1);
        if (mChecked) {
            mergeDrawableStates(state, new int[] {android.R.attr.state_checked});
        }
        return state;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        Drawable d = mButtonDrawable;
        if (d != null && d.isStateful() && d.setState(getDrawableState())) {
            invalidate();
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mButtonDrawable;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return CompoundButton.class.getName();
    }
}
