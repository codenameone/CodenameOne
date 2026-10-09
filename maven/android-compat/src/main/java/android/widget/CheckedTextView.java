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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import com.codename1.androidcompat.runtime.ControlDrawables;

/// A text view with a check mark at the end, used for list items in
/// single- and multiple-choice lists.
public class CheckedTextView extends TextView implements Checkable {

    private boolean mChecked;
    private Drawable mCheckMark;
    private ColorStateList mCheckMarkTint;

    public CheckedTextView(Context context) {
        this(context, null);
    }

    public CheckedTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.checkedTextViewStyle);
    }

    public CheckedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, android.R.style.Widget_Material_CheckedTextView);
    }

    public CheckedTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.CheckedTextView, defStyleAttr,
                defStyleRes);
        Drawable d = a.getDrawable(android.R.styleable.CheckedTextView_checkMark);
        boolean explicit = a.hasValueOrEmpty(android.R.styleable.CheckedTextView_checkMark);
        if (a.hasValue(android.R.styleable.CheckedTextView_checkMarkTint)) {
            mCheckMarkTint = a.getColorStateList(android.R.styleable.CheckedTextView_checkMarkTint);
        }
        boolean checked = a.getBoolean(android.R.styleable.CheckedTextView_checked, false);
        a.recycle();
        if (d == null && !explicit) {
            d = new ControlDrawables.CheckMark(context);
        }
        setCheckMarkDrawable(d);
        setChecked(checked);
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            refreshDrawableState();
            invalidate();
        }
    }

    public void setCheckMarkDrawable(int resId) {
        setCheckMarkDrawable(resId == 0 ? null : getContext().getDrawable(resId));
    }

    public void setCheckMarkDrawable(Drawable d) {
        if (mCheckMark != null) {
            mCheckMark.setCallback(null);
        }
        mCheckMark = d;
        if (d != null) {
            d.setCallback(this);
            if (mCheckMarkTint != null) {
                d.setTintList(mCheckMarkTint);
            }
            if (d.isStateful()) {
                d.setState(getDrawableState());
            }
        }
        textChanged();
    }

    public Drawable getCheckMarkDrawable() {
        return mCheckMark;
    }

    public void setCheckMarkTintList(ColorStateList tint) {
        mCheckMarkTint = tint;
        if (mCheckMark != null) {
            mCheckMark.setTintList(tint);
        }
    }

    private int markWidth() {
        return mCheckMark == null ? 0 : Math.max(0, mCheckMark.getIntrinsicWidth());
    }

    @Override
    public int getCompoundPaddingRight() {
        return super.getCompoundPaddingRight() + (isLayoutRtl() ? 0 : markWidth());
    }

    @Override
    public int getCompoundPaddingLeft() {
        return super.getCompoundPaddingLeft() + (isLayoutRtl() ? markWidth() : 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Drawable d = mCheckMark;
        if (d != null) {
            int w = markWidth();
            int h = Math.max(0, d.getIntrinsicHeight());
            int top = (getHeight() - h) / 2;
            int left = isLayoutRtl() ? getPaddingLeft() : getWidth() - getPaddingRight() - w;
            d.setBounds(left + mScrollX, top + mScrollY, left + w + mScrollX, top + h + mScrollY);
            d.draw(canvas);
        }
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
        Drawable d = mCheckMark;
        if (d != null && d.isStateful() && d.setState(getDrawableState())) {
            invalidate();
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mCheckMark;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return CheckedTextView.class.getName();
    }
}
