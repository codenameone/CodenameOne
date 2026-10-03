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
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/// A vertically scrolling list of adapter views, with optional dividers,
/// header and footer views, and single or multiple choice.
public class ListView extends AbsListView {

    /// A fixed header or footer view and the data it represents.
    public class FixedViewInfo {
        public View view;
        public Object data;
        public boolean isSelectable;
    }

    private final ArrayList<FixedViewInfo> mHeaderViewInfos = new ArrayList<FixedViewInfo>();
    private final ArrayList<FixedViewInfo> mFooterViewInfos = new ArrayList<FixedViewInfo>();
    private Drawable mDivider;
    private int mDividerHeight;
    private boolean mHeaderDividersEnabled = true;
    private boolean mFooterDividersEnabled = true;

    public ListView(Context context) {
        this(context, null);
    }

    public ListView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.listViewStyle);
    }

    public ListView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.ListView, defStyleAttr, defStyleRes);
        CharSequence[] entries = a.getTextArray(android.R.styleable.ListView_entries);
        if (entries != null) {
            setAdapter(new ArrayAdapter<CharSequence>(context, android.R.layout.simple_list_item_1, entries));
        }
        Drawable d = a.getDrawable(android.R.styleable.ListView_divider);
        if (d != null) {
            setDivider(d);
        }
        if (a.hasValue(android.R.styleable.ListView_dividerHeight)) {
            int h = a.getDimensionPixelSize(android.R.styleable.ListView_dividerHeight, 0);
            if (h != 0) {
                setDividerHeight(h);
            }
        }
        mHeaderDividersEnabled = a.getBoolean(android.R.styleable.ListView_headerDividersEnabled, true);
        mFooterDividersEnabled = a.getBoolean(android.R.styleable.ListView_footerDividersEnabled, true);
        a.recycle();
    }

    // ------------------------------------------------------------ headers and footers

    public int getMaxScrollAmount() {
        return (int) (0.33f * getHeight());
    }

    public void addHeaderView(View v, Object data, boolean isSelectable) {
        FixedViewInfo info = new FixedViewInfo();
        info.view = v;
        info.data = data;
        info.isSelectable = isSelectable;
        mHeaderViewInfos.add(info);
        wrapAdapter();
    }

    public void addHeaderView(View v) {
        addHeaderView(v, null, true);
    }

    public int getHeaderViewsCount() {
        return mHeaderViewInfos.size();
    }

    public boolean removeHeaderView(View v) {
        if (mAdapter instanceof HeaderViewListAdapter && ((HeaderViewListAdapter) mAdapter).removeHeader(v)) {
            removeInfo(mHeaderViewInfos, v);
            dataChanged();
            return true;
        }
        return removeInfo(mHeaderViewInfos, v);
    }

    public void addFooterView(View v, Object data, boolean isSelectable) {
        FixedViewInfo info = new FixedViewInfo();
        info.view = v;
        info.data = data;
        info.isSelectable = isSelectable;
        mFooterViewInfos.add(info);
        wrapAdapter();
    }

    public void addFooterView(View v) {
        addFooterView(v, null, true);
    }

    public int getFooterViewsCount() {
        return mFooterViewInfos.size();
    }

    public boolean removeFooterView(View v) {
        if (mAdapter instanceof HeaderViewListAdapter && ((HeaderViewListAdapter) mAdapter).removeFooter(v)) {
            removeInfo(mFooterViewInfos, v);
            dataChanged();
            return true;
        }
        return removeInfo(mFooterViewInfos, v);
    }

    private static boolean removeInfo(ArrayList<FixedViewInfo> where, View v) {
        for (int i = 0; i < where.size(); i++) {
            if (where.get(i).view == v) {
                where.remove(i);
                return true;
            }
        }
        return false;
    }

    private void wrapAdapter() {
        if (mAdapter != null && !(mAdapter instanceof HeaderViewListAdapter)) {
            super.setAdapter(new HeaderViewListAdapter(mHeaderViewInfos, mFooterViewInfos, mAdapter));
        } else {
            dataChanged();
        }
    }

    private void dataChanged() {
        mItemCount = mAdapter == null ? 0 : mAdapter.getCount();
        mDataChanged = true;
        requestLayout();
        invalidate();
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (!mHeaderViewInfos.isEmpty() || !mFooterViewInfos.isEmpty()) {
            adapter = new HeaderViewListAdapter(mHeaderViewInfos, mFooterViewInfos, adapter);
        }
        super.setAdapter(adapter);
    }

    // ------------------------------------------------------------ dividers

    public Drawable getDivider() {
        return mDivider;
    }

    public void setDivider(Drawable divider) {
        mDivider = divider;
        mDividerHeight = divider == null ? 0 : Math.max(0, divider.getIntrinsicHeight());
        requestLayout();
        invalidate();
    }

    public int getDividerHeight() {
        return mDividerHeight;
    }

    public void setDividerHeight(int height) {
        mDividerHeight = height;
        requestLayout();
        invalidate();
    }

    public void setHeaderDividersEnabled(boolean headerDividersEnabled) {
        mHeaderDividersEnabled = headerDividersEnabled;
        invalidate();
    }

    public void setFooterDividersEnabled(boolean footerDividersEnabled) {
        mFooterDividersEnabled = footerDividersEnabled;
        invalidate();
    }

    private int divider() {
        return mDivider == null ? 0 : mDividerHeight;
    }

    // ------------------------------------------------------------ measure

    private int childWidthSpec(int widthMeasureSpec, View child) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        return getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight(),
                lp == null ? ViewGroup.LayoutParams.MATCH_PARENT : lp.width);
    }

    private static int childHeightSpec(View child) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        int h = lp == null ? ViewGroup.LayoutParams.WRAP_CONTENT : lp.height;
        return h > 0 ? MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
                : MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    }

    private void scrapMeasured(View child) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        if (lp instanceof AbsListView.LayoutParams && child.getParent() == null) {
            mRecycler.addScrap(child, ((AbsListView.LayoutParams) lp).viewType);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        mItemCount = mAdapter == null ? 0 : mAdapter.getCount();
        int childWidth = 0;
        int childHeight = 0;
        int childState = 0;
        if (mItemCount > 0 && (widthMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.UNSPECIFIED)) {
            View child = obtainView(0);
            child.measure(childWidthSpec(widthMeasureSpec, child), childHeightSpec(child));
            childWidth = child.getMeasuredWidth();
            childHeight = child.getMeasuredHeight();
            childState = combineMeasuredStates(childState, child.getMeasuredState());
            scrapMeasured(child);
        }
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            widthSize = getPaddingLeft() + getPaddingRight() + childWidth;
        } else {
            widthSize |= (childState & MEASURED_STATE_MASK);
        }
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            heightSize = getPaddingTop() + getPaddingBottom() + childHeight;
        }
        if (heightMode == MeasureSpec.AT_MOST) {
            heightSize = measureHeightOfChildren(widthMeasureSpec, 0, mItemCount - 1, heightSize);
        }
        setMeasuredDimension(widthSize, Math.max(heightSize, getSuggestedMinimumHeight()));
    }

    /// The height of items `start..end` stacked with dividers and padding,
    /// stopping at `maxHeight`.
    final int measureHeightOfChildren(int widthMeasureSpec, int start, int end, int maxHeight) {
        int returnedHeight = getPaddingTop() + getPaddingBottom();
        if (mAdapter == null) {
            return returnedHeight;
        }
        int dividerHeight = divider();
        int last = Math.min(end, mItemCount - 1);
        for (int i = start; i <= last; i++) {
            View child = i - mFirstPosition >= 0 && i - mFirstPosition < getChildCount() && !mDataChanged
                    ? getChildAt(i - mFirstPosition) : null;
            boolean fresh = child == null;
            if (fresh) {
                child = obtainView(i);
                child.measure(childWidthSpec(widthMeasureSpec, child), childHeightSpec(child));
            }
            if (i > 0) {
                returnedHeight += dividerHeight;
            }
            returnedHeight += child.getMeasuredHeight();
            if (fresh) {
                scrapMeasured(child);
            }
            if (returnedHeight >= maxHeight) {
                return maxHeight;
            }
        }
        return returnedHeight;
    }

    // ------------------------------------------------------------ layout

    @Override
    protected void layoutChildren() {
        if (mAdapter == null || mItemCount == 0) {
            recycleAllChildren();
            mFirstPosition = 0;
            invalidate();
            return;
        }
        int top = getChildCount() > 0 ? getChildAt(0).getTop() : listTop();
        if (mSyncPosition >= 0) {
            mFirstPosition = mSyncPosition;
            top = mSyncTop == Integer.MIN_VALUE ? listTop() : listTop() + mSyncTop;
        }
        boolean scrollToEnd = mSyncPosition >= 0 && mSyncTop == Integer.MIN_VALUE;
        mSyncPosition = INVALID_POSITION;
        if (mFirstPosition >= mItemCount) {
            mFirstPosition = mItemCount - 1;
        }
        if (mFirstPosition < 0) {
            mFirstPosition = 0;
        }
        recycleAllChildren();
        fillDown(mFirstPosition, top);
        if (scrollToEnd) {
            correctTooHigh();
        }
        correctTooLow();
        invalidate();
    }

    /// When the items end above the bottom edge, pulls them down, filling
    /// earlier positions above.
    private void correctTooLow() {
        int count = getChildCount();
        if (count == 0) {
            return;
        }
        int lastBottom = getChildAt(count - 1).getBottom();
        int gap = listBottom() - lastBottom;
        if (gap > 0 && mFirstPosition + count >= mItemCount) {
            int firstTop = getChildAt(0).getTop();
            if (mFirstPosition > 0 || firstTop < listTop()) {
                if (mFirstPosition == 0) {
                    gap = Math.min(gap, listTop() - firstTop);
                }
                offsetChildrenTopAndBottom(gap);
                if (mFirstPosition > 0) {
                    fillUp(mFirstPosition - 1, getChildAt(0).getTop() - divider());
                    int newTop = getChildAt(0).getTop();
                    if (mFirstPosition == 0 && newTop > listTop()) {
                        offsetChildrenTopAndBottom(listTop() - newTop);
                        fillDown(mFirstPosition + getChildCount(),
                                getChildAt(getChildCount() - 1).getBottom() + divider());
                    }
                }
            }
        }
    }

    /// Used to show the end of the list: lines the last item up with the bottom.
    private void correctTooHigh() {
        int count = getChildCount();
        if (count == 0) {
            return;
        }
        while (mFirstPosition + getChildCount() < mItemCount) {
            fillDown(mFirstPosition + getChildCount(), getChildAt(getChildCount() - 1).getBottom() + divider());
        }
        int lastBottom = getChildAt(getChildCount() - 1).getBottom();
        if (lastBottom > listBottom()) {
            offsetChildrenTopAndBottom(listBottom() - lastBottom);
        }
        removeOffscreen();
        fillGap(false);
    }

    @Override
    void fillGap(boolean down) {
        int count = getChildCount();
        if (count == 0) {
            fillDown(mFirstPosition, listTop());
            return;
        }
        if (down) {
            fillDown(mFirstPosition + count, getChildAt(count - 1).getBottom() + divider());
        } else {
            fillUp(mFirstPosition - 1, getChildAt(0).getTop() - divider());
        }
    }

    private void fillDown(int pos, int nextTop) {
        int end = listBottom();
        while (nextTop < end && pos < mItemCount) {
            View child = obtainView(pos);
            setupChild(child, pos, nextTop, true);
            nextTop = child.getBottom() + divider();
            pos++;
        }
    }

    private void fillUp(int pos, int nextBottom) {
        int end = listTop();
        while (nextBottom > end && pos >= 0) {
            View child = obtainView(pos);
            setupChild(child, pos, nextBottom, false);
            nextBottom = child.getTop() - divider();
            mFirstPosition = pos;
            pos--;
        }
    }

    private void setupChild(View child, int position, int y, boolean flowDown) {
        int widthSpec = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY);
        child.measure(childWidthSpec(widthSpec, child), childHeightSpec(child));
        int w = child.getMeasuredWidth();
        int h = child.getMeasuredHeight();
        int top = flowDown ? y : y - h;
        addViewInLayout(child, flowDown ? -1 : 0, child.getLayoutParams(), true);
        int left = getPaddingLeft();
        child.layout(left, top, left + w, top + h);
        applyCheckState(child, position);
    }

    // ------------------------------------------------------------ draw

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int dividerHeight = divider();
        if (dividerHeight > 0 && mDivider != null) {
            int count = getChildCount();
            int headers = getHeaderViewsCount();
            int footersStart = mItemCount - getFooterViewsCount();
            int left = getPaddingLeft();
            int right = getWidth() - getPaddingRight();
            for (int i = 0; i < count; i++) {
                int position = mFirstPosition + i;
                if (position >= mItemCount - 1) {
                    break;
                }
                boolean isHeader = position < headers;
                boolean isFooter = position >= footersStart;
                if ((isHeader && !mHeaderDividersEnabled) || (isFooter && !mFooterDividersEnabled)) {
                    continue;
                }
                View c = getChildAt(i);
                int top = c.getBottom();
                mDivider.setBounds(left, top, right, top + dividerHeight);
                mDivider.draw(canvas);
            }
        }
        super.dispatchDraw(canvas);
    }

    public boolean areHeaderDividersEnabled() {
        return mHeaderDividersEnabled;
    }

    public boolean areFooterDividersEnabled() {
        return mFooterDividersEnabled;
    }

    public void setItemsCanFocus(boolean itemsCanFocus) {
    }

    public boolean getItemsCanFocus() {
        return false;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return ListView.class.getName();
    }
}
