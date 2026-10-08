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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.R;
import com.google.android.material.internal.MaterialAttrs;

import java.util.ArrayList;
import java.util.List;

/// Lays [Chip]s out in rows that wrap (or one scrolling row with
/// `singleLine`), spaced by `chipSpacing`, and manages their checked state:
/// with `singleSelection` checking one chip unchecks the others.
public class ChipGroup extends ViewGroup {

    /// Called when the set of checked chips changes.
    public interface OnCheckedStateChangeListener {
        void onCheckedChanged(ChipGroup group, List<Integer> checkedIds);
    }

    /// Called when the single checked chip changes (single selection only).
    public interface OnCheckedChangeListener {
        void onCheckedChanged(ChipGroup group, int checkedId);
    }

    private int mSpacingHorizontal;
    private int mSpacingVertical;
    private boolean mSingleLine;
    private boolean mSingleSelection;
    private boolean mSelectionRequired;
    private int mDefaultCheckedId = View.NO_ID;
    private boolean mSkip;
    private OnCheckedStateChangeListener mStateListener;
    private OnCheckedChangeListener mListener;

    public ChipGroup(Context context) {
        this(context, null);
    }

    public ChipGroup(Context context, AttributeSet attrs) {
        this(context, attrs, MaterialAttrs.defStyleAttr(context, R.attr.chipGroupStyle));
    }

    public ChipGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int defStyleRes = MaterialAttrs.isMaterial3(context) ? R.style.Widget_Material3_ChipGroup
                : R.style.Widget_MaterialComponents_ChipGroup;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ChipGroup, defStyleAttr,
                defStyleAttr == 0 ? defStyleRes : 0);
        try {
            int spacing = a.getDimensionPixelOffset(R.styleable.ChipGroup_chipSpacing, MaterialAttrs.dpi(context, 8));
            mSpacingHorizontal = a.getDimensionPixelOffset(R.styleable.ChipGroup_chipSpacingHorizontal, spacing);
            mSpacingVertical = a.getDimensionPixelOffset(R.styleable.ChipGroup_chipSpacingVertical, spacing);
            mSingleLine = a.getBoolean(R.styleable.ChipGroup_singleLine, false);
            mSingleSelection = a.getBoolean(R.styleable.ChipGroup_singleSelection, false);
            mSelectionRequired = a.getBoolean(R.styleable.ChipGroup_selectionRequired, false);
            mDefaultCheckedId = a.getResourceId(R.styleable.ChipGroup_checkedChip, View.NO_ID);
        } finally {
            a.recycle();
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (child instanceof Chip) {
            final Chip chip = (Chip) child;
            if (chip.getId() == View.NO_ID) {
                chip.setId(View.generateViewId());
            }
            chip.setGroupListener(new Chip.CompoundButton2Listener() {
                @Override
                public void onCheckedChanged(Chip c, boolean isChecked) {
                    onChipChecked(c, isChecked);
                }
            });
        }
        super.addView(child, index, params);
        if (child instanceof Chip && child.getId() == mDefaultCheckedId) {
            ((Chip) child).setChecked(true);
        }
    }

    private void onChipChecked(Chip chip, boolean checked) {
        if (mSkip) {
            return;
        }
        if (!checked && mSelectionRequired && getCheckedChipIds().isEmpty()) {
            mSkip = true;
            chip.setChecked(true);
            mSkip = false;
            return;
        }
        if (checked && mSingleSelection) {
            mSkip = true;
            for (int i = 0; i < getChildCount(); i++) {
                View c = getChildAt(i);
                if (c != chip && c instanceof Chip && ((Chip) c).isChecked()) {
                    ((Chip) c).setChecked(false);
                }
            }
            mSkip = false;
        }
        dispatch();
    }

    private void dispatch() {
        if (mStateListener != null) {
            mStateListener.onCheckedChanged(this, getCheckedChipIds());
        }
        if (mListener != null && mSingleSelection) {
            mListener.onCheckedChanged(this, getCheckedChipId());
        }
    }

    // ------------------------------------------------------------ layout

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int maxWidth = widthMode == MeasureSpec.UNSPECIFIED || mSingleLine ? Integer.MAX_VALUE
                : MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int x = 0;
        int lineHeight = 0;
        int y = 0;
        int widest = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (c.getVisibility() == View.GONE) {
                continue;
            }
            measureChild(c, widthMeasureSpec, heightMeasureSpec);
            int w = c.getMeasuredWidth();
            int h = c.getMeasuredHeight();
            if (x > 0 && x + w > maxWidth) {
                y += lineHeight + mSpacingVertical;
                x = 0;
                lineHeight = 0;
            }
            x += w + mSpacingHorizontal;
            widest = Math.max(widest, x - mSpacingHorizontal);
            lineHeight = Math.max(lineHeight, h);
        }
        int width = widest + getPaddingLeft() + getPaddingRight();
        int height = y + lineHeight + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int maxWidth = mSingleLine ? Integer.MAX_VALUE : r - l - getPaddingLeft() - getPaddingRight();
        boolean rtl = isLayoutRtl();
        int x = 0;
        int y = getPaddingTop();
        int lineHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (c.getVisibility() == View.GONE) {
                continue;
            }
            int w = c.getMeasuredWidth();
            int h = c.getMeasuredHeight();
            if (x > 0 && x + w > maxWidth) {
                y += lineHeight + mSpacingVertical;
                x = 0;
                lineHeight = 0;
            }
            int left = rtl ? r - l - getPaddingRight() - x - w : getPaddingLeft() + x;
            c.layout(left, y, left + w, y + h);
            x += w + mSpacingHorizontal;
            lineHeight = Math.max(lineHeight, h);
        }
    }

    // ------------------------------------------------------------ checking

    public void check(int id) {
        View v = findViewById(id);
        if (v instanceof Chip) {
            ((Chip) v).setChecked(true);
        }
    }

    public void clearCheck() {
        mSkip = true;
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (c instanceof Chip) {
                ((Chip) c).setChecked(false);
            }
        }
        mSkip = false;
        dispatch();
    }

    public int getCheckedChipId() {
        if (!mSingleSelection) {
            return View.NO_ID;
        }
        List<Integer> ids = getCheckedChipIds();
        return ids.isEmpty() ? View.NO_ID : ids.get(0).intValue();
    }

    public List<Integer> getCheckedChipIds() {
        List<Integer> ids = new ArrayList<Integer>();
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (c instanceof Chip && ((Chip) c).isChecked()) {
                ids.add(Integer.valueOf(c.getId()));
            }
        }
        return ids;
    }

    public void setOnCheckedStateChangeListener(OnCheckedStateChangeListener listener) {
        mStateListener = listener;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mListener = listener;
    }

    public boolean isSingleSelection() {
        return mSingleSelection;
    }

    public void setSingleSelection(boolean singleSelection) {
        if (mSingleSelection != singleSelection) {
            mSingleSelection = singleSelection;
            clearCheck();
        }
    }

    public boolean isSelectionRequired() {
        return mSelectionRequired;
    }

    public void setSelectionRequired(boolean selectionRequired) {
        mSelectionRequired = selectionRequired;
    }

    public boolean isSingleLine() {
        return mSingleLine;
    }

    public void setSingleLine(boolean singleLine) {
        mSingleLine = singleLine;
        requestLayout();
    }

    public void setChipSpacing(int spacing) {
        mSpacingHorizontal = spacing;
        mSpacingVertical = spacing;
        requestLayout();
    }

    public void setChipSpacingHorizontal(int spacing) {
        mSpacingHorizontal = spacing;
        requestLayout();
    }

    public void setChipSpacingVertical(int spacing) {
        mSpacingVertical = spacing;
        requestLayout();
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new ViewGroup.MarginLayoutParams(getContext(), attrs);
    }
}
