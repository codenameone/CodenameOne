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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/// An adapter view that shows one selected item at a time.
public abstract class AbsSpinner extends AdapterView<SpinnerAdapter> {

    SpinnerAdapter mAdapter;
    private AdapterDataSetObserver mDataSetObserver;

    public AbsSpinner(Context context) {
        super(context);
    }

    public AbsSpinner(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbsSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AbsSpinner(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.AbsSpinner, defStyleAttr, defStyleRes);
        CharSequence[] entries = a.getTextArray(android.R.styleable.AbsSpinner_entries);
        a.recycle();
        if (entries != null) {
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(context,
                    android.R.layout.simple_spinner_item, entries);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            setAdapter(adapter);
        }
    }

    @Override
    public SpinnerAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }
        removeAllViewsInLayout();
        mAdapter = adapter;
        if (adapter != null) {
            mDataSetObserver = new AdapterDataSetObserver();
            adapter.registerDataSetObserver(mDataSetObserver);
            mItemCount = adapter.getCount();
        } else {
            mItemCount = 0;
        }
        int position = mItemCount > 0 ? 0 : INVALID_POSITION;
        setSelectedPositionInt(position);
        mDataChanged = true;
        updateEmptyStatus();
        requestLayout();
        checkSelectionChanged();
    }

    void setSelectedPositionInt(int position) {
        mSelectedPosition = position;
        mSelectedRowId = position >= 0 && mAdapter != null ? mAdapter.getItemId(position) : INVALID_ROW_ID;
    }

    @Override
    void handleDataChanged() {
        if (mSelectedPosition >= mItemCount) {
            setSelectedPositionInt(mItemCount > 0 ? mItemCount - 1 : INVALID_POSITION);
            checkSelectionChanged();
        } else if (mSelectedPosition < 0 && mItemCount > 0) {
            setSelectedPositionInt(0);
            checkSelectionChanged();
        } else if (mSelectedPosition >= 0) {
            // The position survived, but the row now there may be a different
            // one: keep getSelectedItemId() in step with getSelectedItem().
            // Android's stable-id search for the old row's new position is
            // not done; the selection stays at its position.
            setSelectedPositionInt(mSelectedPosition);
        }
    }

    @Override
    public void setSelection(int position) {
        setSelection(position, false);
    }

    public void setSelection(int position, boolean animate) {
        // INVALID_POSITION does not clear a populated spinner, and that is
        // Android's behaviour too: AbsSpinner only records it as the "next"
        // position, and Spinner/Gallery.layout() adopt the next position only
        // when it is >= 0, so the old selection stays and no
        // onNothingSelected() is delivered. A spinner is cleared by emptying
        // its adapter.
        if (position < 0 || position >= mItemCount || position == mSelectedPosition) {
            return;
        }
        setSelectedPositionInt(position);
        mDataChanged = true;
        requestLayout();
        invalidate();
        checkSelectionChanged();
    }

    @Override
    public View getSelectedView() {
        return getChildCount() > 0 ? getChildAt(0) : null;
    }

    public int pointToPosition(int x, int y) {
        View v = getSelectedView();
        if (v != null && x >= v.getLeft() && x < v.getRight() && y >= v.getTop() && y < v.getBottom()) {
            return mSelectedPosition;
        }
        return INVALID_POSITION;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return AbsSpinner.class.getName();
    }
}
