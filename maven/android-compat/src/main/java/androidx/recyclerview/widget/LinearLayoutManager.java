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
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/// Lays items out in a single column or row and scrolls them.
///
/// The layout runs in "layout space": a coordinate that grows from the
/// start edge (the top, or the bottom when the layout is reversed) in the
/// direction of increasing adapter positions, so filling, scrolling and gap
/// correction are written once for every orientation and direction.
public class LinearLayoutManager extends RecyclerView.LayoutManager
        implements ItemTouchHelper.ViewDropHandler, RecyclerView.SmoothScroller.ScrollVectorProvider {

    public static final int HORIZONTAL = RecyclerView.HORIZONTAL;
    public static final int VERTICAL = RecyclerView.VERTICAL;
    public static final int INVALID_OFFSET = Integer.MIN_VALUE;

    int mOrientation = RecyclerView.DEFAULT_ORIENTATION;
    OrientationHelper mOrientationHelper;
    private boolean mReverseLayout;
    boolean mShouldReverseLayout;
    private boolean mStackFromEnd;
    private boolean mSmoothScrollbarEnabled = true;
    int mPendingScrollPosition = RecyclerView.NO_POSITION;
    int mPendingScrollPositionOffset = INVALID_OFFSET;
    private boolean mRecycleChildrenOnDetach;
    private int mInitialPrefetchItemCount = 2;
    SavedState mPendingSavedState;

    /// What the last laid-out chunk took: its size along the layout axis
    /// and the next position in the direction it was laid out.
    int mChunkConsumed;
    int mChunkNext;

    public LinearLayoutManager(Context context) {
        this(context, RecyclerView.DEFAULT_ORIENTATION, false);
    }

    public LinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        setOrientation(orientation);
        setReverseLayout(reverseLayout);
    }

    public LinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        Properties properties = getProperties(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(properties.orientation);
        setReverseLayout(properties.reverseLayout);
        setStackFromEnd(properties.stackFromEnd);
    }

    @Override
    public boolean isAutoMeasureEnabled() {
        return true;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public boolean getRecycleChildrenOnDetach() {
        return mRecycleChildrenOnDetach;
    }

    public void setRecycleChildrenOnDetach(boolean recycleChildrenOnDetach) {
        mRecycleChildrenOnDetach = recycleChildrenOnDetach;
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);
        if (mRecycleChildrenOnDetach) {
            removeAndRecycleAllViews(recycler);
            recycler.clear();
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        if (mPendingSavedState != null) {
            return new SavedState(mPendingSavedState);
        }
        SavedState state = new SavedState();
        if (getChildCount() > 0) {
            View ref = childClosestToStart();
            state.mAnchorPosition = getPosition(ref);
            state.mAnchorOffset = startL(ref);
        } else {
            state.invalidateAnchor();
        }
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            mPendingSavedState = (SavedState) state;
            if (mPendingScrollPosition != RecyclerView.NO_POSITION) {
                mPendingSavedState.invalidateAnchor();
            }
            requestLayout();
        }
    }

    @Override
    public boolean canScrollHorizontally() {
        return mOrientation == HORIZONTAL;
    }

    @Override
    public boolean canScrollVertically() {
        return mOrientation == VERTICAL;
    }

    public void setStackFromEnd(boolean stackFromEnd) {
        if (mStackFromEnd == stackFromEnd) {
            return;
        }
        mStackFromEnd = stackFromEnd;
        requestLayout();
    }

    public boolean getStackFromEnd() {
        return mStackFromEnd;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException("invalid orientation:" + orientation);
        }
        if (orientation != mOrientation || mOrientationHelper == null) {
            mOrientationHelper = OrientationHelper.createOrientationHelper(this, orientation);
            mOrientation = orientation;
            requestLayout();
        }
    }

    private void resolveShouldLayoutReverse() {
        if (mOrientation == VERTICAL || !isLayoutRTL()) {
            mShouldReverseLayout = mReverseLayout;
        } else {
            mShouldReverseLayout = !mReverseLayout;
        }
    }

    public boolean getReverseLayout() {
        return mReverseLayout;
    }

    public void setReverseLayout(boolean reverseLayout) {
        if (reverseLayout == mReverseLayout) {
            return;
        }
        mReverseLayout = reverseLayout;
        requestLayout();
    }

    protected boolean isLayoutRTL() {
        return getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    public void setSmoothScrollbarEnabled(boolean enabled) {
        mSmoothScrollbarEnabled = enabled;
    }

    public boolean isSmoothScrollbarEnabled() {
        return mSmoothScrollbarEnabled;
    }

    public void setInitialPrefetchItemCount(int itemCount) {
        mInitialPrefetchItemCount = itemCount;
    }

    public int getInitialPrefetchItemCount() {
        return mInitialPrefetchItemCount;
    }

    @Deprecated
    protected int getExtraLayoutSpace(RecyclerView.State state) {
        return 0;
    }

    protected void calculateExtraLayoutSpace(RecyclerView.State state, int[] extraLayoutSpace) {
        extraLayoutSpace[0] = 0;
        extraLayoutSpace[1] = 0;
    }

    @Override
    public View findViewByPosition(int position) {
        int childCount = getChildCount();
        if (childCount == 0) {
            return null;
        }
        int firstChild = getPosition(getChildAt(0));
        int viewPosition = position - firstChild;
        if (viewPosition >= 0 && viewPosition < childCount) {
            View child = getChildAt(viewPosition);
            if (getPosition(child) == position) {
                return child;
            }
        }
        return super.findViewByPosition(position);
    }

    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        if (getChildCount() == 0) {
            return null;
        }
        int firstChildPos = minPos();
        int direction = (targetPosition < firstChildPos) != mShouldReverseLayout ? -1 : 1;
        if (mOrientation == HORIZONTAL) {
            return new PointF(direction, 0);
        }
        return new PointF(0, direction);
    }

    @Override
    public void scrollToPosition(int position) {
        mPendingScrollPosition = position;
        mPendingScrollPositionOffset = INVALID_OFFSET;
        if (mPendingSavedState != null) {
            mPendingSavedState.invalidateAnchor();
        }
        requestLayout();
    }

    public void scrollToPositionWithOffset(int position, int offset) {
        mPendingScrollPosition = position;
        mPendingScrollPositionOffset = offset;
        if (mPendingSavedState != null) {
            mPendingSavedState.invalidateAnchor();
        }
        requestLayout();
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext());
        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        mPendingSavedState = null;
        mPendingScrollPosition = RecyclerView.NO_POSITION;
        mPendingScrollPositionOffset = INVALID_OFFSET;
        if (mOrientationHelper != null) {
            mOrientationHelper.onLayoutComplete();
        }
    }

    // ------------------------------------------------------------ layout space

    /// The space to fill; unbounded when the RecyclerView is measured with
    /// no limit along the layout axis (wrap_content inside a scroll view),
    /// where Android lays out every item.
    int avail() {
        if (mOrientationHelper.getMode() == android.view.View.MeasureSpec.UNSPECIFIED) {
            return Integer.MAX_VALUE / 4;
        }
        return mOrientationHelper.getTotalSpace();
    }

    int startL(View v) {
        if (mShouldReverseLayout) {
            return mOrientationHelper.getEndAfterPadding() - mOrientationHelper.getDecoratedEnd(v);
        }
        return mOrientationHelper.getDecoratedStart(v) - mOrientationHelper.getStartAfterPadding();
    }

    int endL(View v) {
        return startL(v) + mOrientationHelper.getDecoratedMeasurement(v);
    }

    void shiftL(int d) {
        if (d != 0) {
            mOrientationHelper.offsetChildren(mShouldReverseLayout ? -d : d);
        }
    }

    /// The real coordinate of the start of a chunk at layout-space `sL`.
    int realStart(int sL, int size) {
        if (mShouldReverseLayout) {
            return mOrientationHelper.getEndAfterPadding() - sL - size;
        }
        return mOrientationHelper.getStartAfterPadding() + sL;
    }

    /// Lays a measured child out at layout-space `sL` along the layout axis
    /// and `otherStart` (from the cross-axis padding) across it.
    void placeChild(View v, int sL, int size, int otherStart, int otherSize) {
        int start = realStart(sL, size);
        if (mOrientation == VERTICAL) {
            int left;
            if (isLayoutRTL()) {
                left = getWidth() - getPaddingRight() - otherStart - otherSize;
            } else {
                left = getPaddingLeft() + otherStart;
            }
            layoutDecoratedWithMargins(v, left, start, left + otherSize, start + size);
        } else {
            int top = getPaddingTop() + otherStart;
            layoutDecoratedWithMargins(v, start, top, start + size, top + otherSize);
        }
    }

    int minPos() {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < getChildCount(); i++) {
            int p = getPosition(getChildAt(i));
            if (p < min) {
                min = p;
            }
        }
        return min;
    }

    int maxPos() {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < getChildCount(); i++) {
            int p = getPosition(getChildAt(i));
            if (p > max) {
                max = p;
            }
        }
        return max;
    }

    int minStartL() {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < getChildCount(); i++) {
            int s = startL(getChildAt(i));
            if (s < min) {
                min = s;
            }
        }
        return min;
    }

    int maxEndL() {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < getChildCount(); i++) {
            int e = endL(getChildAt(i));
            if (e > max) {
                max = e;
            }
        }
        return max;
    }

    private View childClosestToStart() {
        View best = null;
        int bestL = Integer.MAX_VALUE;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            int s = startL(v);
            if (s < bestL) {
                bestL = s;
                best = v;
            }
        }
        return best;
    }

    // ------------------------------------------------------------ chunks

    /// Lays out the chunk that starts (`dir` 1) or ends (`dir` -1) at
    /// `position`, with that edge at layout-space `edge`; sets
    /// [#mChunkConsumed] and [#mChunkNext]. One item here; a row of a grid in
    /// [GridLayoutManager].
    void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state, int position, int edge, int dir) {
        View view = recycler.getViewForPosition(position);
        if (dir > 0) {
            addView(view);
        } else {
            addView(view, 0);
        }
        measureChildWithMargins(view, 0, 0);
        int size = mOrientationHelper.getDecoratedMeasurement(view);
        int other = mOrientationHelper.getDecoratedMeasurementInOther(view);
        placeChild(view, dir > 0 ? edge : edge - size, size, 0, other);
        mChunkConsumed = size;
        mChunkNext = position + dir;
    }

    /// The anchor of a layout is the start of a chunk (or its end when
    /// laying out from the end); a grid moves it to its row's edge.
    int alignAnchorPosition(int position, boolean atEnd, RecyclerView.State state) {
        return position;
    }

    /// Children that share a key are recycled together (a grid's row).
    int chunkKey(int position) {
        return position;
    }

    int fillForward(RecyclerView.Recycler recycler, RecyclerView.State state, int position, int edge, int limit) {
        int count = state.getItemCount();
        while (position < count && position >= 0 && edge < limit) {
            layoutChunk(recycler, state, position, edge, 1);
            edge += mChunkConsumed;
            position = mChunkNext;
        }
        return edge;
    }

    int fillBackward(RecyclerView.Recycler recycler, RecyclerView.State state, int position, int edge, int limit) {
        int count = state.getItemCount();
        while (position >= 0 && position < count && edge > limit) {
            layoutChunk(recycler, state, position, edge, -1);
            edge -= mChunkConsumed;
            position = mChunkNext;
        }
        return edge;
    }

    // ------------------------------------------------------------ layout

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (mPendingSavedState != null && mPendingScrollPosition == RecyclerView.NO_POSITION
                && mPendingSavedState.hasValidAnchor()) {
            mPendingScrollPosition = mPendingSavedState.mAnchorPosition;
            mPendingScrollPositionOffset = mPendingSavedState.mAnchorOffset;
        }
        int count = state.getItemCount();
        if (count == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }
        resolveShouldLayoutReverse();
        int avail = avail();

        int anchorPos;
        int anchorL;
        boolean atEnd;
        if (mPendingScrollPosition != RecyclerView.NO_POSITION && mPendingScrollPosition >= 0
                && mPendingScrollPosition < count) {
            anchorPos = mPendingScrollPosition;
            if (mPendingScrollPositionOffset != INVALID_OFFSET) {
                anchorL = mPendingScrollPositionOffset;
                atEnd = false;
            } else {
                View child = findViewByPosition(anchorPos);
                if (child != null) {
                    int sL = startL(child);
                    int eL = endL(child);
                    if (eL - sL > avail || sL < 0) {
                        anchorL = 0;
                        atEnd = false;
                    } else if (eL > avail) {
                        anchorL = avail;
                        atEnd = true;
                    } else {
                        anchorL = sL;
                        atEnd = false;
                    }
                } else if (getChildCount() > 0) {
                    atEnd = anchorPos > minPos();
                    anchorL = atEnd ? avail : 0;
                } else {
                    atEnd = mStackFromEnd;
                    anchorL = atEnd ? avail : 0;
                }
            }
        } else {
            View ref = null;
            int refL = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View v = getChildAt(i);
                RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) v.getLayoutParams();
                if (lp.isItemRemoved()) {
                    continue;
                }
                int p = lp.getViewLayoutPosition();
                if (p < 0 || p >= count) {
                    continue;
                }
                int l = mStackFromEnd ? endL(v) : startL(v);
                if (ref == null || (mStackFromEnd ? l > refL : l < refL)) {
                    ref = v;
                    refL = l;
                }
            }
            if (ref != null) {
                anchorPos = getPosition(ref);
                anchorL = refL;
                atEnd = mStackFromEnd;
            } else {
                atEnd = mStackFromEnd;
                anchorPos = atEnd ? count - 1 : 0;
                anchorL = atEnd ? avail : 0;
            }
        }
        anchorPos = Math.max(0, Math.min(count - 1, anchorPos));
        anchorPos = alignAnchorPosition(anchorPos, atEnd, state);

        detachAndScrapAttachedViews(recycler);
        if (!atEnd) {
            fillForward(recycler, state, anchorPos, anchorL, avail);
            if (getChildCount() > 0) {
                fillBackward(recycler, state, minPos() - 1, minStartL(), 0);
            } else {
                fillBackward(recycler, state, anchorPos - 1, anchorL, 0);
            }
        } else {
            fillBackward(recycler, state, anchorPos, anchorL, 0);
            if (getChildCount() > 0) {
                fillForward(recycler, state, maxPos() + 1, maxEndL(), avail);
            } else {
                fillForward(recycler, state, anchorPos + 1, anchorL, avail);
            }
        }
        fixGaps(recycler, state, avail);
        recycleOutOfBounds(recycler, avail);
    }

    /// Android never leaves a gap at an edge it could fill: content that
    /// starts below the start edge moves up (or, stacking from the end,
    /// content that ends short of the end edge moves down), filling what it
    /// uncovers.
    private void fixGaps(RecyclerView.Recycler recycler, RecyclerView.State state, int avail) {
        if (getChildCount() == 0) {
            return;
        }
        int count = state.getItemCount();
        int start = minStartL();
        int end = maxEndL();
        if (!mStackFromEnd) {
            if (start > 0 && minPos() == 0) {
                shiftL(-start);
                fillForward(recycler, state, maxPos() + 1, maxEndL(), avail);
            } else if (end < avail && maxPos() == count - 1) {
                shiftL(avail - end);
                fillBackward(recycler, state, minPos() - 1, minStartL(), 0);
                int s = minStartL();
                if (s > 0 && minPos() == 0) {
                    shiftL(-s);
                }
            }
        } else {
            if (end < avail && maxPos() == count - 1) {
                shiftL(avail - end);
                fillBackward(recycler, state, minPos() - 1, minStartL(), 0);
            } else if (start > 0 && minPos() == 0) {
                shiftL(-start);
                fillForward(recycler, state, maxPos() + 1, maxEndL(), avail);
                int e = maxEndL();
                if (e < avail && maxPos() == count - 1) {
                    shiftL(avail - e);
                }
            }
        }
    }

    /// Recycles the chunks wholly outside the visible area.
    void recycleOutOfBounds(RecyclerView.Recycler recycler, int avail) {
        int count = getChildCount();
        if (count == 0) {
            return;
        }
        java.util.HashMap<Integer, int[]> extents = new java.util.HashMap<Integer, int[]>();
        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            Integer key = Integer.valueOf(chunkKey(getPosition(v)));
            int[] e = extents.get(key);
            int s = startL(v);
            int en = endL(v);
            if (e == null) {
                extents.put(key, new int[] {s, en});
            } else {
                e[0] = Math.min(e[0], s);
                e[1] = Math.max(e[1], en);
            }
        }
        for (int i = count - 1; i >= 0; i--) {
            View v = getChildAt(i);
            int[] e = extents.get(Integer.valueOf(chunkKey(getPosition(v))));
            if (e[1] <= 0 || e[0] >= avail) {
                removeAndRecycleViewAt(i, recycler);
            }
        }
    }

    // ------------------------------------------------------------ scrolling

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (mOrientation == VERTICAL) {
            return 0;
        }
        return scrollBy(dx, recycler, state);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (mOrientation == HORIZONTAL) {
            return 0;
        }
        return scrollBy(dy, recycler, state);
    }

    int scrollBy(int delta, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || delta == 0) {
            return 0;
        }
        resolveShouldLayoutReverse();
        int avail = avail();
        int dL = mShouldReverseLayout ? -delta : delta;
        int consumed;
        if (dL > 0) {
            int edge = fillForward(recycler, state, maxPos() + 1, maxEndL(), avail + dL);
            edge = Math.max(edge, maxEndL());
            consumed = Math.min(dL, Math.max(0, edge - avail));
        } else {
            int edge = fillBackward(recycler, state, minPos() - 1, minStartL(), dL);
            edge = Math.min(edge, minStartL());
            consumed = Math.max(dL, Math.min(0, edge));
        }
        shiftL(-consumed);
        recycleOutOfBounds(recycler, avail);
        return mShouldReverseLayout ? -consumed : consumed;
    }

    // ------------------------------------------------------------ scroll metrics

    /// {offset, extent, range} in real coordinates (offset 0 at the top or
    /// left of the content), estimated from the laid-out items' average size
    /// but exact at both ends, so canScroll answers precisely.
    private int[] scrollMetrics(RecyclerView.State state) {
        int count = state.getItemCount();
        if (getChildCount() == 0 || count == 0 || mOrientationHelper == null) {
            return new int[] {0, 0, 0};
        }
        int avail = avail();
        int minP = minPos();
        int maxP = maxPos();
        int sL = minStartL();
        int eL = maxEndL();
        boolean atStart = minP == 0 && sL >= 0;
        boolean atEnd = maxP == count - 1 && eL <= avail;
        if (!mSmoothScrollbarEnabled) {
            int extent = maxP - minP + 1;
            int offset = mShouldReverseLayout ? Math.max(0, count - maxP - 1) : Math.max(0, minP);
            return new int[] {offset, extent, count};
        }
        float avg = (eL - sL) / (float) (maxP - minP + 1);
        int range = Math.max(avail, Math.round(avg * count));
        int extent = avail;
        int offsetL;
        if (atStart) {
            offsetL = 0;
        } else {
            offsetL = Math.max(1, Math.round(minP * avg - sL));
        }
        if (atEnd && !atStart) {
            offsetL = range - extent;
            if (offsetL < 1) {
                range = extent + 1;
                offsetL = 1;
            }
        } else if (!atEnd) {
            if (offsetL + extent + 2 > range) {
                range = offsetL + extent + 2;
            }
        }
        int offset = mShouldReverseLayout ? range - extent - offsetL : offsetL;
        return new int[] {offset, extent, range};
    }

    @Override
    public int computeHorizontalScrollOffset(RecyclerView.State state) {
        return mOrientation == HORIZONTAL ? scrollMetrics(state)[0] : 0;
    }

    @Override
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        return mOrientation == VERTICAL ? scrollMetrics(state)[0] : 0;
    }

    @Override
    public int computeHorizontalScrollExtent(RecyclerView.State state) {
        return mOrientation == HORIZONTAL ? scrollMetrics(state)[1] : 0;
    }

    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        return mOrientation == VERTICAL ? scrollMetrics(state)[1] : 0;
    }

    @Override
    public int computeHorizontalScrollRange(RecyclerView.State state) {
        return mOrientation == HORIZONTAL ? scrollMetrics(state)[2] : 0;
    }

    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        return mOrientation == VERTICAL ? scrollMetrics(state)[2] : 0;
    }

    // ------------------------------------------------------------ visible positions

    private int findVisible(boolean first, boolean completely) {
        if (getChildCount() == 0 || mOrientationHelper == null) {
            return RecyclerView.NO_POSITION;
        }
        int avail = avail();
        int best = RecyclerView.NO_POSITION;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            int s = startL(v);
            int e = endL(v);
            boolean visible = completely ? s >= 0 && e <= avail : s < avail && e > 0;
            if (!visible) {
                continue;
            }
            int p = getPosition(v);
            if (best == RecyclerView.NO_POSITION || (first ? p < best : p > best)) {
                best = p;
            }
        }
        return best;
    }

    public int findFirstVisibleItemPosition() {
        return findVisible(true, false);
    }

    public int findFirstCompletelyVisibleItemPosition() {
        return findVisible(true, true);
    }

    public int findLastVisibleItemPosition() {
        return findVisible(false, false);
    }

    public int findLastCompletelyVisibleItemPosition() {
        return findVisible(false, true);
    }

    // ------------------------------------------------------------ drag and drop

    @Override
    public void prepareForDrop(View view, View target, int x, int y) {
        resolveShouldLayoutReverse();
        int myPos = getPosition(view);
        int targetPos = getPosition(target);
        boolean towardEnd = myPos < targetPos;
        if (towardEnd) {
            scrollToPositionWithOffset(targetPos, endL(target) - mOrientationHelper.getDecoratedMeasurement(view));
        } else {
            scrollToPositionWithOffset(targetPos, startL(target));
        }
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }

    @Override
    public String toString() {
        return getClass().getName() + "{orientation=" + mOrientation + ", reverse=" + mReverseLayout
                + ", stackFromEnd=" + mStackFromEnd + "}";
    }

    /// The scroll position kept across recreation: the first item's
    /// position and its offset from the start edge.
    public static class SavedState implements Parcelable {
        int mAnchorPosition = RecyclerView.NO_POSITION;
        int mAnchorOffset;

        public SavedState() {
        }

        public SavedState(SavedState other) {
            mAnchorPosition = other.mAnchorPosition;
            mAnchorOffset = other.mAnchorOffset;
        }

        boolean hasValidAnchor() {
            return mAnchorPosition >= 0;
        }

        void invalidateAnchor() {
            mAnchorPosition = RecyclerView.NO_POSITION;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mAnchorPosition);
            dest.writeInt(mAnchorOffset);
        }
    }
}
