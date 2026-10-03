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
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;

/// A view whose children come from an [Adapter]. Children are managed by the
/// view itself, so `addView` and `removeView` are unsupported, as on Android.
public abstract class AdapterView<T extends Adapter> extends ViewGroup {

    public static final int ITEM_VIEW_TYPE_IGNORE = -1;
    public static final int ITEM_VIEW_TYPE_HEADER_OR_FOOTER = -2;
    public static final int INVALID_POSITION = -1;
    public static final long INVALID_ROW_ID = Long.MIN_VALUE;

    public interface OnItemClickListener {
        void onItemClick(AdapterView<?> parent, View view, int position, long id);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id);
    }

    public interface OnItemSelectedListener {
        void onItemSelected(AdapterView<?> parent, View view, int position, long id);

        void onNothingSelected(AdapterView<?> parent);
    }

    /// The menu info a context menu on an adapter view's item receives.
    public static class AdapterContextMenuInfo implements ContextMenu.ContextMenuInfo {
        public View targetView;
        public int position;
        public long id;

        public AdapterContextMenuInfo(View targetView, int position, long id) {
            this.targetView = targetView;
            this.position = position;
            this.id = id;
        }
    }

    /// Position of the first child in the adapter.
    int mFirstPosition;
    int mItemCount;
    int mSelectedPosition = INVALID_POSITION;
    long mSelectedRowId = INVALID_ROW_ID;
    boolean mDataChanged;
    private int mOldSelectedPosition = INVALID_POSITION;
    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;
    private OnItemSelectedListener mOnItemSelectedListener;
    private View mEmptyView;
    private boolean mSelectionNotifyPending;

    public AdapterView(Context context) {
        super(context);
    }

    public AdapterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AdapterView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AdapterView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public abstract T getAdapter();

    public abstract void setAdapter(T adapter);

    public abstract View getSelectedView();

    public abstract void setSelection(int position);

    // ------------------------------------------------------------ listeners

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public final OnItemClickListener getOnItemClickListener() {
        return mOnItemClickListener;
    }

    public boolean performItemClick(View view, int position, long id) {
        if (mOnItemClickListener != null) {
            playSoundEffect(SoundEffectConstants.CLICK);
            mOnItemClickListener.onItemClick(this, view, position, id);
            return true;
        }
        return false;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        if (!isLongClickable()) {
            setLongClickable(true);
        }
        mOnItemLongClickListener = listener;
    }

    public final OnItemLongClickListener getOnItemLongClickListener() {
        return mOnItemLongClickListener;
    }

    /// Runs the long-click listener, or opens a context menu for the item.
    boolean performItemLongClick(View child, int position, long id) {
        boolean handled = false;
        if (mOnItemLongClickListener != null) {
            handled = mOnItemLongClickListener.onItemLongClick(this, child, position, id);
        }
        if (!handled) {
            handled = showContextMenuForChild(child);
        }
        return handled;
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    public final OnItemSelectedListener getOnItemSelectedListener() {
        return mOnItemSelectedListener;
    }

    @Override
    public void setOnClickListener(View.OnClickListener l) {
        throw new RuntimeException("Don't call setOnClickListener for an AdapterView. "
                + "You probably want setOnItemClickListener instead");
    }

    // ------------------------------------------------------------ children are ours

    @Override
    public void addView(View child) {
        throw new UnsupportedOperationException("addView(View) is not supported in AdapterView");
    }

    @Override
    public void addView(View child, int index) {
        throw new UnsupportedOperationException("addView(View, int) is not supported in AdapterView");
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        throw new UnsupportedOperationException("addView(View, LayoutParams) is not supported in AdapterView");
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        throw new UnsupportedOperationException("addView(View, int, LayoutParams) is not supported in AdapterView");
    }

    @Override
    public void removeView(View child) {
        throw new UnsupportedOperationException("removeView(View) is not supported in AdapterView");
    }

    @Override
    public void removeViewAt(int index) {
        throw new UnsupportedOperationException("removeViewAt(int) is not supported in AdapterView");
    }

    @Override
    public void removeAllViews() {
        throw new UnsupportedOperationException("removeAllViews() is not supported in AdapterView");
    }

    // ------------------------------------------------------------ queries

    public int getCount() {
        return mItemCount;
    }

    public int getFirstVisiblePosition() {
        return mFirstPosition;
    }

    public int getLastVisiblePosition() {
        return mFirstPosition + getChildCount() - 1;
    }

    public int getSelectedItemPosition() {
        return mSelectedPosition;
    }

    public long getSelectedItemId() {
        return mSelectedRowId;
    }

    public Object getSelectedItem() {
        T adapter = getAdapter();
        int selection = getSelectedItemPosition();
        if (adapter != null && adapter.getCount() > 0 && selection >= 0) {
            return adapter.getItem(selection);
        }
        return null;
    }

    public Object getItemAtPosition(int position) {
        T adapter = getAdapter();
        return (adapter == null || position < 0) ? null : adapter.getItem(position);
    }

    public long getItemIdAtPosition(int position) {
        T adapter = getAdapter();
        return (adapter == null || position < 0) ? INVALID_ROW_ID : adapter.getItemId(position);
    }

    /// The adapter position of `view`, which may be any descendant of a child.
    public int getPositionForView(View view) {
        View listItem = view;
        while (true) {
            Object parent = listItem.getParent();
            if (parent == this) {
                break;
            }
            if (!(parent instanceof View)) {
                return INVALID_POSITION;
            }
            listItem = (View) parent;
        }
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            if (getChildAt(i) == listItem) {
                return mFirstPosition + i;
            }
        }
        return INVALID_POSITION;
    }

    // ------------------------------------------------------------ empty view

    public void setEmptyView(View emptyView) {
        mEmptyView = emptyView;
        updateEmptyStatus();
    }

    public View getEmptyView() {
        return mEmptyView;
    }

    void updateEmptyStatus() {
        T adapter = getAdapter();
        boolean empty = adapter == null || adapter.isEmpty();
        if (mEmptyView != null) {
            if (empty) {
                mEmptyView.setVisibility(View.VISIBLE);
                setVisibility(View.GONE);
            } else {
                mEmptyView.setVisibility(View.GONE);
                setVisibility(View.VISIBLE);
            }
        }
    }

    // ------------------------------------------------------------ selection notification

    /// Fires onItemSelected / onNothingSelected after the selection changed,
    /// posted like Android so the listener sees the laid-out view.
    void checkSelectionChanged() {
        if (mSelectedPosition != mOldSelectedPosition && !mSelectionNotifyPending) {
            mSelectionNotifyPending = true;
            post(new Runnable() {
                @Override
                public void run() {
                    mSelectionNotifyPending = false;
                    fireOnSelected();
                }
            });
        }
    }

    private void fireOnSelected() {
        if (mSelectedPosition == mOldSelectedPosition) {
            return;
        }
        mOldSelectedPosition = mSelectedPosition;
        if (mOnItemSelectedListener == null) {
            return;
        }
        int selection = getSelectedItemPosition();
        if (selection >= 0) {
            View v = getSelectedView();
            mOnItemSelectedListener.onItemSelected(this, v, selection, getAdapter().getItemId(selection));
        } else {
            mOnItemSelectedListener.onNothingSelected(this);
        }
    }

    /// Observes the adapter and re-lays out the view on changes.
    class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            mDataChanged = true;
            T adapter = getAdapter();
            mItemCount = adapter == null ? 0 : adapter.getCount();
            handleDataChanged();
            updateEmptyStatus();
            requestLayout();
            invalidate();
        }

        @Override
        public void onInvalidated() {
            mDataChanged = true;
            mItemCount = 0;
            handleDataChanged();
            updateEmptyStatus();
            requestLayout();
            invalidate();
        }
    }

    /// Subclass hook when the data set changes, before relayout.
    void handleDataChanged() {
    }

    public CharSequence getAccessibilityClassName() {
        return AdapterView.class.getName();
    }
}
