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
package androidx.recyclerview.widget;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;

/// Lays items out in `spanCount` lanes of equal width (or height), each item
/// going to the lane that ends first, so items of different sizes pack like
/// a masonry wall. An item keeps its lane while it scrolls on and off
/// screen; when the first item comes back into view with the lanes
/// misaligned, the layout starts over from it, which closes the gaps the way
/// Android's `GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS` does.
public class StaggeredGridLayoutManager extends RecyclerView.LayoutManager
        implements RecyclerView.SmoothScroller.ScrollVectorProvider {

    public static final int HORIZONTAL = RecyclerView.HORIZONTAL;
    public static final int VERTICAL = RecyclerView.VERTICAL;
    public static final int GAP_HANDLING_NONE = 0;
    @Deprecated
    public static final int GAP_HANDLING_LAZY = 1;
    public static final int GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS = 2;

    private static final int INVALID_OFFSET = Integer.MIN_VALUE;

    private int mSpanCount = -1;
    private int mOrientation;
    private boolean mReverseLayout;
    private boolean mShouldReverseLayout;
    private int mGapStrategy = GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS;
    OrientationHelper mPrimaryOrientation;
    OrientationHelper mSecondaryOrientation;
    private int[] mLaneOfPosition = new int[0];
    private int mPendingScrollPosition = RecyclerView.NO_POSITION;
    private int mPendingScrollPositionOffset = INVALID_OFFSET;
    private boolean mRealignPending;
    private int[] mLaneStart = new int[0];
    private int[] mLaneEnd = new int[0];

    public StaggeredGridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        Properties properties = getProperties(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(properties.orientation);
        setSpanCount(properties.spanCount);
        setReverseLayout(properties.reverseLayout);
    }

    public StaggeredGridLayoutManager(int spanCount, int orientation) {
        setOrientation(orientation);
        setSpanCount(spanCount);
    }

    @Override
    public boolean isAutoMeasureEnabled() {
        return true;
    }

    public void setSpanCount(int spanCount) {
        if (spanCount < 1) {
            throw new IllegalArgumentException("Span count should be at least 1. Provided " + spanCount);
        }
        if (spanCount != mSpanCount) {
            mSpanCount = spanCount;
            invalidateSpanAssignments();
            requestLayout();
        }
    }

    public int getSpanCount() {
        return mSpanCount;
    }

    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException("invalid orientation.");
        }
        if (orientation == mOrientation && mPrimaryOrientation != null) {
            return;
        }
        mOrientation = orientation;
        mPrimaryOrientation = OrientationHelper.createOrientationHelper(this, mOrientation);
        mSecondaryOrientation = OrientationHelper.createOrientationHelper(this, 1 - mOrientation);
        requestLayout();
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setReverseLayout(boolean reverseLayout) {
        if (reverseLayout != mReverseLayout) {
            mReverseLayout = reverseLayout;
            requestLayout();
        }
    }

    public boolean getReverseLayout() {
        return mReverseLayout;
    }

    public int getGapStrategy() {
        return mGapStrategy;
    }

    public void setGapStrategy(int gapStrategy) {
        if (gapStrategy != GAP_HANDLING_NONE && gapStrategy != GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS) {
            throw new IllegalArgumentException("invalid gap strategy. Must be GAP_HANDLING_NONE "
                    + "or GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS");
        }
        mGapStrategy = gapStrategy;
        requestLayout();
    }

    public void invalidateSpanAssignments() {
        Arrays.fill(mLaneOfPosition, -1);
        requestLayout();
    }

    @Override
    public boolean canScrollVertically() {
        return mOrientation == VERTICAL;
    }

    @Override
    public boolean canScrollHorizontally() {
        return mOrientation == HORIZONTAL;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        if (mOrientation == HORIZONTAL) {
            return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        }
        return new LayoutParams(lp);
    }

    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        Arrays.fill(mLaneOfPosition, -1);
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        Arrays.fill(mLaneOfPosition, -1);
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        Arrays.fill(mLaneOfPosition, -1);
    }

    @Override
    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        Arrays.fill(mLaneOfPosition, -1);
    }

    @Override
    public void scrollToPosition(int position) {
        mPendingScrollPosition = position;
        mPendingScrollPositionOffset = INVALID_OFFSET;
        requestLayout();
    }

    public void scrollToPositionWithOffset(int position, int offset) {
        mPendingScrollPosition = position;
        mPendingScrollPositionOffset = offset;
        requestLayout();
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        LinearSmoothScroller scroller = new LinearSmoothScroller(recyclerView.getContext());
        scroller.setTargetPosition(position);
        startSmoothScroll(scroller);
    }

    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        if (getChildCount() == 0) {
            return null;
        }
        int direction = (targetPosition < minPos()) != mShouldReverseLayout ? -1 : 1;
        return mOrientation == HORIZONTAL ? new PointF(direction, 0) : new PointF(0, direction);
    }

    @Override
    public void onScrollStateChanged(int state) {
        if (state == RecyclerView.SCROLL_STATE_IDLE && mGapStrategy != GAP_HANDLING_NONE && hasGapAtStart()) {
            mRealignPending = true;
            requestLayout();
        }
    }

    // ------------------------------------------------------------ layout space

    private int avail() {
        if (mPrimaryOrientation.getMode() == View.MeasureSpec.UNSPECIFIED) {
            return Integer.MAX_VALUE / 4;
        }
        return mPrimaryOrientation.getTotalSpace();
    }

    private int startL(View v) {
        if (mShouldReverseLayout) {
            return mPrimaryOrientation.getEndAfterPadding() - mPrimaryOrientation.getDecoratedEnd(v);
        }
        return mPrimaryOrientation.getDecoratedStart(v) - mPrimaryOrientation.getStartAfterPadding();
    }

    private int endL(View v) {
        return startL(v) + mPrimaryOrientation.getDecoratedMeasurement(v);
    }

    private void shiftL(int d) {
        if (d != 0) {
            mPrimaryOrientation.offsetChildren(mShouldReverseLayout ? -d : d);
        }
    }

    private int minPos() {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < getChildCount(); i++) {
            min = Math.min(min, getPosition(getChildAt(i)));
        }
        return min;
    }

    private int maxPos() {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < getChildCount(); i++) {
            max = Math.max(max, getPosition(getChildAt(i)));
        }
        return max;
    }

    private void ensureLanes(int count) {
        if (mLaneStart.length != mSpanCount) {
            mLaneStart = new int[mSpanCount];
            mLaneEnd = new int[mSpanCount];
        }
        if (mLaneOfPosition.length < count) {
            int[] grown = new int[Math.max(count, mLaneOfPosition.length * 2)];
            Arrays.fill(grown, -1);
            System.arraycopy(mLaneOfPosition, 0, grown, 0, mLaneOfPosition.length);
            mLaneOfPosition = grown;
        }
    }

    private int laneOf(View v) {
        LayoutParams lp = (LayoutParams) v.getLayoutParams();
        return lp.mFullSpan ? -1 : lp.mSpanIndex;
    }

    /// The lane edges from the children on screen.
    private void lanesFromChildren() {
        Arrays.fill(mLaneStart, Integer.MAX_VALUE);
        Arrays.fill(mLaneEnd, Integer.MIN_VALUE);
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            int s = startL(v);
            int e = endL(v);
            int lane = laneOf(v);
            for (int l = 0; l < mSpanCount; l++) {
                if (lane == -1 || lane == l) {
                    mLaneStart[l] = Math.min(mLaneStart[l], s);
                    mLaneEnd[l] = Math.max(mLaneEnd[l], e);
                }
            }
        }
        int fallbackStart = Integer.MAX_VALUE;
        int fallbackEnd = Integer.MAX_VALUE;
        for (int l = 0; l < mSpanCount; l++) {
            if (mLaneEnd[l] != Integer.MIN_VALUE) {
                fallbackEnd = Math.min(fallbackEnd, mLaneEnd[l]);
                fallbackStart = Math.min(fallbackStart, mLaneStart[l]);
            }
        }
        for (int l = 0; l < mSpanCount; l++) {
            if (mLaneEnd[l] == Integer.MIN_VALUE) {
                mLaneEnd[l] = fallbackEnd == Integer.MAX_VALUE ? 0 : fallbackEnd;
                mLaneStart[l] = fallbackStart == Integer.MAX_VALUE ? 0 : fallbackStart;
            }
        }
    }

    private static int min(int[] a) {
        int m = Integer.MAX_VALUE;
        for (int v : a) {
            m = Math.min(m, v);
        }
        return m;
    }

    private static int max(int[] a) {
        int m = Integer.MIN_VALUE;
        for (int v : a) {
            m = Math.max(m, v);
        }
        return m;
    }

    private int[] laneBorders() {
        int total = mSecondaryOrientation.getTotalSpace();
        int[] borders = new int[mSpanCount + 1];
        for (int i = 0; i <= mSpanCount; i++) {
            borders[i] = (int) ((long) total * i / mSpanCount);
        }
        return borders;
    }

    private void layoutItem(RecyclerView.Recycler recycler, int position, boolean forward, int[] borders) {
        View view = recycler.getViewForPosition(position);
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        int lane;
        if (lp.mFullSpan) {
            lane = -1;
        } else {
            lane = mLaneOfPosition[position];
            if (lane < 0 || lane >= mSpanCount) {
                lane = 0;
                for (int l = 1; l < mSpanCount; l++) {
                    if (forward ? mLaneEnd[l] < mLaneEnd[lane] : mLaneStart[l] > mLaneStart[lane]) {
                        lane = l;
                    }
                }
            }
            mLaneOfPosition[position] = lane;
        }
        lp.mSpanIndex = lane == -1 ? 0 : lane;
        if (forward) {
            addView(view);
        } else {
            addView(view, 0);
        }
        int otherStart = lane == -1 ? 0 : borders[lane];
        int otherSize = lane == -1 ? borders[mSpanCount] : borders[lane + 1] - borders[lane];
        android.graphics.Rect insets = new android.graphics.Rect();
        calculateItemDecorationsForChild(view, insets);
        int otherInsets = mOrientation == VERTICAL
                ? insets.left + insets.right + lp.leftMargin + lp.rightMargin
                : insets.top + insets.bottom + lp.topMargin + lp.bottomMargin;
        int mainInsets = mOrientation == VERTICAL
                ? insets.top + insets.bottom + lp.topMargin + lp.bottomMargin
                : insets.left + insets.right + lp.leftMargin + lp.rightMargin;
        int otherSpec = View.MeasureSpec.makeMeasureSpec(Math.max(0, otherSize - otherInsets), View.MeasureSpec.EXACTLY);
        if (mOrientation == VERTICAL) {
            view.measure(otherSpec, getChildMeasureSpec(getHeight(), getHeightMode(), mainInsets, lp.height, true));
        } else {
            view.measure(getChildMeasureSpec(getWidth(), getWidthMode(), mainInsets, lp.width, true), otherSpec);
        }
        int size = mPrimaryOrientation.getDecoratedMeasurement(view);
        int sL;
        if (forward) {
            sL = lane == -1 ? max(mLaneEnd) : mLaneEnd[lane];
        } else {
            sL = (lane == -1 ? min(mLaneStart) : mLaneStart[lane]) - size;
        }
        int start = mShouldReverseLayout ? mPrimaryOrientation.getEndAfterPadding() - sL - size
                : mPrimaryOrientation.getStartAfterPadding() + sL;
        if (mOrientation == VERTICAL) {
            int left = getPaddingLeft() + otherStart;
            layoutDecoratedWithMargins(view, left, start, left + otherSize, start + size);
        } else {
            int top = getPaddingTop() + otherStart;
            layoutDecoratedWithMargins(view, start, top, start + size, top + otherSize);
        }
        for (int l = 0; l < mSpanCount; l++) {
            if (lane == -1 || lane == l) {
                if (forward) {
                    mLaneEnd[l] = sL + size;
                    mLaneStart[l] = Math.min(mLaneStart[l], sL);
                } else {
                    mLaneStart[l] = sL;
                    mLaneEnd[l] = Math.max(mLaneEnd[l], sL + size);
                }
            }
        }
    }

    private boolean hasGapAtStart() {
        if (getChildCount() == 0 || minPos() != 0) {
            return false;
        }
        ensureLanes(0);
        lanesFromChildren();
        int first = mLaneStart[0];
        for (int l = 1; l < mSpanCount; l++) {
            if (mLaneStart[l] != first) {
                return true;
            }
        }
        return first > 0;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int count = state.getItemCount();
        if (count == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }
        mShouldReverseLayout = mReverseLayout;
        ensureLanes(count);
        int avail = avail();
        int anchorPos = 0;
        int anchorL = 0;
        if (mPendingScrollPosition >= 0 && mPendingScrollPosition < count) {
            anchorPos = mPendingScrollPosition;
            anchorL = mPendingScrollPositionOffset == INVALID_OFFSET ? 0 : mPendingScrollPositionOffset;
        } else if (getChildCount() > 0 && !mRealignPending) {
            int best = Integer.MAX_VALUE;
            for (int i = 0; i < getChildCount(); i++) {
                View v = getChildAt(i);
                RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) v.getLayoutParams();
                int p = lp.getViewLayoutPosition();
                if (lp.isItemRemoved() || p < 0 || p >= count) {
                    continue;
                }
                if (p < best) {
                    best = p;
                    anchorPos = p;
                    anchorL = startL(v);
                }
            }
        }
        mRealignPending = false;
        if (anchorPos == 0) {
            anchorL = Math.min(anchorL, 0);
            if (anchorL == 0 || mGapStrategy != GAP_HANDLING_NONE) {
                Arrays.fill(mLaneOfPosition, -1);
            }
        }
        detachAndScrapAttachedViews(recycler);
        layoutFrom(recycler, state, anchorPos, anchorL, avail);
        if (getChildCount() > 0) {
            lanesFromChildren();
            if (maxPos() == count - 1 && max(mLaneEnd) < avail && minPos() > 0) {
                shiftL(avail - max(mLaneEnd));
                lanesFromChildren();
                int[] borders = laneBorders();
                int pos = minPos() - 1;
                while (pos >= 0 && max(mLaneStart) > 0) {
                    layoutItem(recycler, pos, false, borders);
                    pos--;
                }
            }
            if (minPos() == 0) {
                lanesFromChildren();
                if (max(mLaneStart) > 0 || min(mLaneStart) != max(mLaneStart)) {
                    detachAndScrapAttachedViews(recycler);
                    Arrays.fill(mLaneOfPosition, -1);
                    layoutFrom(recycler, state, 0, 0, avail);
                }
            }
        }
        recycleOutOfBounds(recycler, avail);
    }

    private void layoutFrom(RecyclerView.Recycler recycler, RecyclerView.State state, int anchorPos, int anchorL,
                            int avail) {
        int count = state.getItemCount();
        int[] borders = laneBorders();
        Arrays.fill(mLaneStart, anchorL);
        Arrays.fill(mLaneEnd, anchorL);
        int pos = anchorPos;
        while (pos < count && min(mLaneEnd) < avail) {
            layoutItem(recycler, pos, true, borders);
            pos++;
        }
        pos = anchorPos - 1;
        while (pos >= 0 && max(mLaneStart) > 0) {
            layoutItem(recycler, pos, false, borders);
            pos--;
        }
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        mPendingScrollPosition = RecyclerView.NO_POSITION;
        mPendingScrollPositionOffset = INVALID_OFFSET;
    }

    private void recycleOutOfBounds(RecyclerView.Recycler recycler, int avail) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View v = getChildAt(i);
            if (endL(v) <= 0 || startL(v) >= avail) {
                removeAndRecycleViewAt(i, recycler);
            }
        }
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return mOrientation == VERTICAL ? scrollBy(dy, recycler, state) : 0;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return mOrientation == HORIZONTAL ? scrollBy(dx, recycler, state) : 0;
    }

    private int scrollBy(int delta, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || delta == 0) {
            return 0;
        }
        int count = state.getItemCount();
        ensureLanes(count);
        lanesFromChildren();
        int avail = avail();
        int[] borders = laneBorders();
        int dL = mShouldReverseLayout ? -delta : delta;
        int consumed;
        if (dL > 0) {
            int pos = maxPos() + 1;
            while (pos < count && min(mLaneEnd) < avail + dL) {
                layoutItem(recycler, pos, true, borders);
                pos++;
            }
            consumed = Math.min(dL, Math.max(0, max(mLaneEnd) - avail));
        } else {
            int pos = minPos() - 1;
            while (pos >= 0 && max(mLaneStart) > dL) {
                layoutItem(recycler, pos, false, borders);
                pos--;
            }
            consumed = Math.max(dL, Math.min(0, min(mLaneStart)));
        }
        shiftL(-consumed);
        recycleOutOfBounds(recycler, avail);
        return mShouldReverseLayout ? -consumed : consumed;
    }

    // ------------------------------------------------------------ positions

    private int[] visiblePerLane(int[] into, boolean first, boolean completely) {
        if (into == null) {
            into = new int[mSpanCount];
        } else if (into.length < mSpanCount) {
            throw new IllegalArgumentException("Provided int[]'s size must be more than or equal"
                    + " to span count. Expected:" + mSpanCount + ", array size:" + into.length);
        }
        Arrays.fill(into, RecyclerView.NO_POSITION);
        int avail = avail();
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            int s = startL(v);
            int e = endL(v);
            boolean visible = completely ? s >= 0 && e <= avail : s < avail && e > 0;
            if (!visible) {
                continue;
            }
            int p = getPosition(v);
            int lane = laneOf(v);
            for (int l = 0; l < mSpanCount; l++) {
                if (lane == -1 || lane == l) {
                    if (into[l] == RecyclerView.NO_POSITION || (first ? p < into[l] : p > into[l])) {
                        into[l] = p;
                    }
                }
            }
        }
        return into;
    }

    public int[] findFirstVisibleItemPositions(int[] into) {
        return visiblePerLane(into, true, false);
    }

    public int[] findFirstCompletelyVisibleItemPositions(int[] into) {
        return visiblePerLane(into, true, true);
    }

    public int[] findLastVisibleItemPositions(int[] into) {
        return visiblePerLane(into, false, false);
    }

    public int[] findLastCompletelyVisibleItemPositions(int[] into) {
        return visiblePerLane(into, false, true);
    }

    private int[] scrollMetrics(RecyclerView.State state) {
        int count = state.getItemCount();
        if (getChildCount() == 0 || count == 0) {
            return new int[] {0, 0, 0};
        }
        int avail = avail();
        ensureLanes(count);
        lanesFromChildren();
        int minP = minPos();
        int maxP = maxPos();
        int sL = min(mLaneStart);
        int eL = max(mLaneEnd);
        boolean atStart = minP == 0 && sL >= 0;
        boolean atEnd = maxP == count - 1 && eL <= avail;
        float perItem = (eL - sL) / (float) Math.max(1, maxP - minP + 1);
        int range = Math.max(avail, Math.round(perItem * count));
        int offset = atStart ? 0 : Math.max(1, Math.round(minP * perItem - sL));
        if (atEnd && !atStart) {
            offset = range - avail;
            if (offset < 1) {
                range = avail + 1;
                offset = 1;
            }
        } else if (!atEnd && offset + avail + 2 > range) {
            range = offset + avail + 2;
        }
        if (mShouldReverseLayout) {
            offset = range - avail - offset;
        }
        return new int[] {offset, avail, range};
    }

    @Override
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        return mOrientation == VERTICAL ? scrollMetrics(state)[0] : 0;
    }

    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        return mOrientation == VERTICAL ? scrollMetrics(state)[1] : 0;
    }

    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        return mOrientation == VERTICAL ? scrollMetrics(state)[2] : 0;
    }

    @Override
    public int computeHorizontalScrollOffset(RecyclerView.State state) {
        return mOrientation == HORIZONTAL ? scrollMetrics(state)[0] : 0;
    }

    @Override
    public int computeHorizontalScrollExtent(RecyclerView.State state) {
        return mOrientation == HORIZONTAL ? scrollMetrics(state)[1] : 0;
    }

    @Override
    public int computeHorizontalScrollRange(RecyclerView.State state) {
        return mOrientation == HORIZONTAL ? scrollMetrics(state)[2] : 0;
    }

    /// The layout parameters of a staggered grid child: its lane, or
    /// whether it spans every lane.
    public static class LayoutParams extends RecyclerView.LayoutParams {
        public static final int INVALID_SPAN_ID = -1;
        int mSpanIndex = INVALID_SPAN_ID;
        boolean mFullSpan;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(RecyclerView.LayoutParams source) {
            super(source);
        }

        public void setFullSpan(boolean fullSpan) {
            mFullSpan = fullSpan;
        }

        public boolean isFullSpan() {
            return mFullSpan;
        }

        public final int getSpanIndex() {
            return mSpanIndex;
        }
    }
}
