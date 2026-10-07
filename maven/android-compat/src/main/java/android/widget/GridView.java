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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

/// A vertically scrolling grid of adapter views: a fixed or `auto_fit`
/// number of columns, spaced and stretched as Android's GridView does.
public class GridView extends AbsListView {

    public static final int NO_STRETCH = 0;
    public static final int STRETCH_SPACING = 1;
    public static final int STRETCH_COLUMN_WIDTH = 2;
    public static final int STRETCH_SPACING_UNIFORM = 3;
    public static final int AUTO_FIT = -1;

    private int mNumColumns = AUTO_FIT;
    private int mRequestedNumColumns = AUTO_FIT;
    private int mHorizontalSpacing;
    private int mRequestedHorizontalSpacing;
    private int mVerticalSpacing;
    private int mStretchMode = STRETCH_COLUMN_WIDTH;
    private int mColumnWidth;
    private int mRequestedColumnWidth;
    private int mGravity = Gravity.START;

    public GridView(Context context) {
        this(context, null);
    }

    public GridView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.gridViewStyle);
    }

    public GridView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GridView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.GridView, defStyleAttr, defStyleRes);
        mRequestedHorizontalSpacing = a.getDimensionPixelOffset(android.R.styleable.GridView_horizontalSpacing, 0);
        mVerticalSpacing = a.getDimensionPixelOffset(android.R.styleable.GridView_verticalSpacing, 0);
        mStretchMode = a.getInt(android.R.styleable.GridView_stretchMode, STRETCH_COLUMN_WIDTH);
        mRequestedColumnWidth = a.getDimensionPixelOffset(android.R.styleable.GridView_columnWidth, -1);
        mRequestedNumColumns = a.getInt(android.R.styleable.GridView_numColumns, 1);
        mGravity = a.getInt(android.R.styleable.GridView_gravity, Gravity.START);
        a.recycle();
    }

    public void setNumColumns(int numColumns) {
        mRequestedNumColumns = numColumns;
        requestLayout();
    }

    public int getNumColumns() {
        return mNumColumns;
    }

    public int getRequestedNumColumns() {
        return mRequestedNumColumns;
    }

    public void setColumnWidth(int columnWidth) {
        mRequestedColumnWidth = columnWidth;
        requestLayout();
    }

    public int getColumnWidth() {
        return mColumnWidth;
    }

    public int getRequestedColumnWidth() {
        return mRequestedColumnWidth;
    }

    public void setHorizontalSpacing(int horizontalSpacing) {
        mRequestedHorizontalSpacing = horizontalSpacing;
        requestLayout();
    }

    public int getHorizontalSpacing() {
        return mHorizontalSpacing;
    }

    public int getRequestedHorizontalSpacing() {
        return mRequestedHorizontalSpacing;
    }

    public void setVerticalSpacing(int verticalSpacing) {
        mVerticalSpacing = verticalSpacing;
        requestLayout();
    }

    public int getVerticalSpacing() {
        return mVerticalSpacing;
    }

    public void setStretchMode(int stretchMode) {
        mStretchMode = stretchMode;
        requestLayout();
    }

    public int getStretchMode() {
        return mStretchMode;
    }

    public void setGravity(int gravity) {
        mGravity = gravity;
        requestLayout();
    }

    public int getGravity() {
        return mGravity;
    }

    @Override
    int rowSize() {
        return Math.max(1, mNumColumns);
    }

    /// AOSP's column computation for an available width.
    private boolean determineColumns(int availableSpace) {
        final int requestedHorizontalSpacing = mRequestedHorizontalSpacing;
        final int stretchMode = mStretchMode;
        final int requestedColumnWidth = mRequestedColumnWidth;
        boolean didNotInitiallyFit = false;
        if (mRequestedNumColumns == AUTO_FIT) {
            if (requestedColumnWidth > 0) {
                mNumColumns = (availableSpace + requestedHorizontalSpacing)
                        / (requestedColumnWidth + requestedHorizontalSpacing);
            } else {
                mNumColumns = 2;
            }
        } else {
            mNumColumns = mRequestedNumColumns;
        }
        if (mNumColumns <= 0) {
            mNumColumns = 1;
        }
        switch (stretchMode) {
            case NO_STRETCH:
                mColumnWidth = requestedColumnWidth > 0 ? requestedColumnWidth
                        : (availableSpace - (mNumColumns - 1) * requestedHorizontalSpacing) / mNumColumns;
                mHorizontalSpacing = requestedHorizontalSpacing;
                break;
            default:
                int spaceLeftOver = availableSpace - (mNumColumns * Math.max(0, requestedColumnWidth))
                        - ((mNumColumns - 1) * requestedHorizontalSpacing);
                if (spaceLeftOver < 0) {
                    didNotInitiallyFit = true;
                }
                switch (stretchMode) {
                    case STRETCH_COLUMN_WIDTH:
                        if (requestedColumnWidth > 0) {
                            mColumnWidth = requestedColumnWidth + spaceLeftOver / mNumColumns;
                        } else {
                            mColumnWidth = (availableSpace - (mNumColumns - 1) * requestedHorizontalSpacing)
                                    / mNumColumns;
                        }
                        mHorizontalSpacing = requestedHorizontalSpacing;
                        break;
                    case STRETCH_SPACING:
                        mColumnWidth = Math.max(0, requestedColumnWidth);
                        mHorizontalSpacing = mNumColumns > 1
                                ? requestedHorizontalSpacing + spaceLeftOver / (mNumColumns - 1)
                                : requestedHorizontalSpacing + spaceLeftOver;
                        break;
                    case STRETCH_SPACING_UNIFORM:
                        mColumnWidth = Math.max(0, requestedColumnWidth);
                        // The leftover is shared by the n + 1 gaps, the
                        // leading and trailing ones included, for one column
                        // too. AOSP gives a single column the whole leftover
                        // as its leading gap, which pushes it against the far
                        // edge; this keeps it centred like every other count.
                        mHorizontalSpacing = requestedHorizontalSpacing + spaceLeftOver / (mNumColumns + 1);
                        break;
                    default:
                        break;
                }
                break;
        }
        return didNotInitiallyFit;
    }

    private int childWidthSpec() {
        return MeasureSpec.makeMeasureSpec(Math.max(0, mColumnWidth), MeasureSpec.EXACTLY);
    }

    private static int childHeightSpec(View child) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        int h = lp == null ? ViewGroup.LayoutParams.WRAP_CONTENT : lp.height;
        return h > 0 ? MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
                : MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            widthSize = mColumnWidth > 0 ? mColumnWidth + getPaddingLeft() + getPaddingRight()
                    : getPaddingLeft() + getPaddingRight();
        }
        int childWidth = widthSize - getPaddingLeft() - getPaddingRight();
        determineColumns(childWidth);
        mItemCount = mAdapter == null ? 0 : mAdapter.getCount();
        int childHeight = 0;
        if (mItemCount > 0 && heightMode != MeasureSpec.EXACTLY) {
            View child = obtainView(0);
            child.measure(childWidthSpec(), childHeightSpec(child));
            childHeight = child.getMeasuredHeight();
            scrapMeasured(child);
        }
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            heightSize = getPaddingTop() + getPaddingBottom() + childHeight;
        }
        if (heightMode == MeasureSpec.AT_MOST) {
            int ourSize = getPaddingTop() + getPaddingBottom();
            int numColumns = mNumColumns;
            for (int i = 0; i < mItemCount; i += numColumns) {
                ourSize += childHeight;
                if (i + numColumns < mItemCount) {
                    ourSize += mVerticalSpacing;
                }
                if (ourSize >= heightSize) {
                    ourSize = heightSize;
                    break;
                }
            }
            heightSize = ourSize;
        }
        if (widthMode == MeasureSpec.AT_MOST && mRequestedNumColumns != AUTO_FIT) {
            int ourSize = (mRequestedNumColumns * Math.max(0, mColumnWidth))
                    + ((mRequestedNumColumns - 1) * mHorizontalSpacing) + getPaddingLeft() + getPaddingRight();
            if (ourSize > widthSize) {
                widthSize |= MEASURED_STATE_TOO_SMALL;
            }
        }
        setMeasuredDimension(widthSize, Math.max(heightSize, getSuggestedMinimumHeight()));
    }

    private void scrapMeasured(View child) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        if (lp instanceof AbsListView.LayoutParams && child.getParent() == null) {
            mRecycler.addScrap(child, ((AbsListView.LayoutParams) lp).viewType);
        }
    }

    // ------------------------------------------------------------ layout

    @Override
    protected void layoutChildren() {
        determineColumns(getWidth() - getPaddingLeft() - getPaddingRight());
        if (mAdapter == null || mItemCount == 0) {
            recycleAllChildren();
            mFirstPosition = 0;
            invalidate();
            return;
        }
        boolean bottomStart = getChildCount() == 0 && mSyncPosition < 0 && isStackFromBottom();
        int top = getChildCount() > 0 ? getChildAt(0).getTop() : listTop();
        if (mSyncPosition >= 0) {
            mFirstPosition = mSyncPosition;
            top = mSyncTop == Integer.MIN_VALUE ? listTop() : listTop() + mSyncTop;
            mSyncPosition = INVALID_POSITION;
        }
        if (mFirstPosition >= mItemCount) {
            mFirstPosition = mItemCount - 1;
        }
        mFirstPosition = Math.max(0, mFirstPosition - mFirstPosition % mNumColumns);
        recycleAllChildren();
        if (bottomStart) {
            int lastRow = (mItemCount - 1) / mNumColumns * mNumColumns;
            fillUp(lastRow, listBottom());
            invalidate();
            return;
        }
        fillDown(mFirstPosition, top);
        int count = getChildCount();
        if (count > 0 && mFirstPosition + count >= mItemCount) {
            int gap = listBottom() - maxBottom(0, count);
            int firstTop = getChildAt(0).getTop();
            if (gap > 0 && (mFirstPosition > 0 || firstTop < listTop())) {
                if (mFirstPosition == 0) {
                    gap = Math.min(gap, listTop() - firstTop);
                }
                offsetChildrenTopAndBottom(gap);
                fillGap(false);
            }
        }
        invalidate();
    }

    @Override
    void fillGap(boolean down) {
        int count = getChildCount();
        if (count == 0) {
            fillDown(mFirstPosition, listTop());
            return;
        }
        if (down) {
            int lastRowStart = ((count - 1) / mNumColumns) * mNumColumns;
            fillDown(mFirstPosition + count, maxBottom(lastRowStart, count) + mVerticalSpacing);
        } else {
            fillUp(mFirstPosition - mNumColumns, getChildAt(0).getTop() - mVerticalSpacing);
        }
    }

    private void fillDown(int rowStart, int nextTop) {
        int end = listBottom();
        while (nextTop < end && rowStart < mItemCount) {
            int bottom = makeRow(rowStart, nextTop, true);
            nextTop = bottom + mVerticalSpacing;
            rowStart += mNumColumns;
        }
    }

    private void fillUp(int rowStart, int nextBottom) {
        int end = listTop();
        while (nextBottom > end && rowStart >= 0) {
            int top = makeRow(rowStart, nextBottom, false);
            nextBottom = top - mVerticalSpacing;
            mFirstPosition = rowStart;
            rowStart -= mNumColumns;
        }
    }

    /// Lays out one row starting at `startPos` with its top (or bottom, when
    /// not `flow`) at `y`. Returns the row's bottom (or top).
    private int makeRow(int startPos, int y, boolean flow) {
        int last = Math.min(startPos + mNumColumns, mItemCount);
        View[] row = new View[last - startPos];
        int rowHeight = 0;
        for (int pos = startPos; pos < last; pos++) {
            View child = obtainView(pos);
            child.measure(childWidthSpec(), childHeightSpec(child));
            rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
            row[pos - startPos] = child;
        }
        int top = flow ? y : y - rowHeight;
        int used = mNumColumns * mColumnWidth + (mNumColumns - 1) * mHorizontalSpacing;
        int space = getWidth() - getPaddingLeft() - getPaddingRight();
        int childLeft = getPaddingLeft();
        int hg = Gravity.getAbsoluteGravity(mGravity, getLayoutDirection()) & Gravity.HORIZONTAL_GRAVITY_MASK;
        if (mStretchMode == STRETCH_SPACING_UNIFORM) {
            childLeft += mHorizontalSpacing;
        } else if (hg == Gravity.CENTER_HORIZONTAL) {
            childLeft += (space - used) / 2;
        } else if (hg == Gravity.RIGHT) {
            childLeft += space - used;
        }
        boolean rtl = isLayoutRtl();
        for (int i = 0; i < row.length; i++) {
            View child = row[i];
            int col = rtl ? mNumColumns - 1 - i : i;
            int left = childLeft + col * (mColumnWidth + mHorizontalSpacing);
            int insertAt = flow ? -1 : i;
            addViewInLayout(child, insertAt, child.getLayoutParams(), true);
            int h = child.getMeasuredHeight();
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (h < rowHeight && lp != null && lp.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                child.measure(childWidthSpec(), MeasureSpec.makeMeasureSpec(rowHeight, MeasureSpec.EXACTLY));
                h = rowHeight;
            }
            child.layout(left, top, left + child.getMeasuredWidth(), top + h);
            applyCheckState(child, startPos + i);
        }
        return flow ? top + rowHeight : top;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return GridView.class.getName();
    }
}
