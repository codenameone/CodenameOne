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
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/// Lays items out in a grid: rows of `spanCount` spans (columns when
/// horizontal), each item taking as many spans as its [SpanSizeLookup] says.
/// Rows are laid out and recycled as a unit.
public class GridLayoutManager extends LinearLayoutManager {

    public static final int DEFAULT_SPAN_COUNT = -1;

    int mSpanCount = DEFAULT_SPAN_COUNT;
    SpanSizeLookup mSpanSizeLookup = new DefaultSpanSizeLookup();
    private boolean mUsingSpansToEstimateScrollBarDimensions;
    private final ArrayList<View> mRow = new ArrayList<View>();

    public GridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        Properties properties = getProperties(context, attrs, defStyleAttr, defStyleRes);
        setSpanCount(properties.spanCount);
    }

    public GridLayoutManager(Context context, int spanCount) {
        super(context);
        setSpanCount(spanCount);
    }

    public GridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
        setSpanCount(spanCount);
    }

    @Override
    public void setStackFromEnd(boolean stackFromEnd) {
        if (stackFromEnd) {
            throw new UnsupportedOperationException(
                    "GridLayoutManager does not support stack from end. Consider using reverse layout");
        }
        super.setStackFromEnd(false);
    }

    public int getSpanCount() {
        return mSpanCount;
    }

    public void setSpanCount(int spanCount) {
        if (spanCount == mSpanCount) {
            return;
        }
        if (spanCount < 1) {
            throw new IllegalArgumentException("Span count should be at least 1. Provided " + spanCount);
        }
        mSpanCount = spanCount;
        mSpanSizeLookup.invalidateSpanIndexCache();
        requestLayout();
    }

    public void setSpanSizeLookup(SpanSizeLookup spanSizeLookup) {
        mSpanSizeLookup = spanSizeLookup;
    }

    public SpanSizeLookup getSpanSizeLookup() {
        return mSpanSizeLookup;
    }

    public void setUsingSpansToEstimateScrollbarDimensions(boolean useSpansToEstimateScrollBarDimensions) {
        mUsingSpansToEstimateScrollBarDimensions = useSpansToEstimateScrollBarDimensions;
    }

    public boolean isUsingSpansToEstimateScrollbarDimensions() {
        return mUsingSpansToEstimateScrollBarDimensions;
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
        mSpanSizeLookup.invalidateSpanIndexCache();
        mSpanSizeLookup.invalidateSpanGroupIndexCache();
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        onItemsChanged(recyclerView);
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        onItemsChanged(recyclerView);
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount, Object payload) {
        onItemsChanged(recyclerView);
    }

    @Override
    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        onItemsChanged(recyclerView);
    }

    private int spanGroup(int position) {
        return mSpanSizeLookup.getCachedSpanGroupIndex(position, mSpanCount);
    }

    private int spanIndex(int position) {
        return mSpanSizeLookup.getCachedSpanIndex(position, mSpanCount);
    }

    private int spanSize(int position) {
        return Math.min(mSpanCount, mSpanSizeLookup.getSpanSize(position));
    }

    @Override
    int chunkKey(int position) {
        return spanGroup(position);
    }

    @Override
    int alignAnchorPosition(int position, boolean atEnd, RecyclerView.State state) {
        int group = spanGroup(position);
        if (!atEnd) {
            while (position > 0 && spanGroup(position - 1) == group) {
                position--;
            }
        } else {
            int count = state.getItemCount();
            while (position + 1 < count && spanGroup(position + 1) == group) {
                position++;
            }
        }
        return position;
    }

    /// The span borders across the cross axis: span `i` runs from
    /// `borders[i]` to `borders[i + 1]`, the remainder spread like Android.
    private int[] calculateItemBorders(int totalSpace) {
        int[] cachedBorders = new int[mSpanCount + 1];
        int consumedPixels = 0;
        int sizePerSpan = totalSpace / mSpanCount;
        int sizePerSpanRemainder = totalSpace % mSpanCount;
        int additionalSize = 0;
        for (int i = 1; i <= mSpanCount; i++) {
            int itemSize = sizePerSpan;
            additionalSize += sizePerSpanRemainder;
            if (additionalSize > 0 && (mSpanCount - additionalSize) < sizePerSpanRemainder) {
                itemSize += 1;
                additionalSize -= mSpanCount;
            }
            consumedPixels += itemSize;
            cachedBorders[i] = consumedPixels;
        }
        return cachedBorders;
    }

    @Override
    void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state, int position, int edge, int dir) {
        int count = state.getItemCount();
        int group = spanGroup(position);
        int first = position;
        int last = position;
        if (dir > 0) {
            while (last + 1 < count && spanGroup(last + 1) == group) {
                last++;
            }
        } else {
            while (first > 0 && spanGroup(first - 1) == group) {
                first--;
            }
        }
        int otherSpace = mOrientation == VERTICAL
                ? getWidth() - getPaddingLeft() - getPaddingRight()
                : getHeight() - getPaddingTop() - getPaddingBottom();
        int[] borders = calculateItemBorders(otherSpace);
        mRow.clear();
        int insertAt = 0;
        for (int p = first; p <= last; p++) {
            View view = recycler.getViewForPosition(p);
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            lp.mSpanIndex = spanIndex(p);
            lp.mSpanSize = spanSize(p);
            if (dir > 0) {
                addView(view);
            } else {
                addView(view, insertAt++);
            }
            mRow.add(view);
        }
        int maxSize = 0;
        for (int i = 0; i < mRow.size(); i++) {
            View view = mRow.get(i);
            measureInSpans(view, borders, false, 0);
            int size = mOrientationHelper.getDecoratedMeasurement(view);
            if (size > maxSize) {
                maxSize = size;
            }
        }
        for (int i = 0; i < mRow.size(); i++) {
            View view = mRow.get(i);
            if (mOrientationHelper.getDecoratedMeasurement(view) != maxSize) {
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                int dim = mOrientation == VERTICAL ? lp.height : lp.width;
                if (dim == ViewGroup.LayoutParams.MATCH_PARENT) {
                    measureInSpans(view, borders, true, maxSize);
                }
            }
        }
        int sL = dir > 0 ? edge : edge - maxSize;
        for (int i = 0; i < mRow.size(); i++) {
            View view = mRow.get(i);
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            int otherStart = borders[lp.mSpanIndex];
            int otherSize = mOrientationHelper.getDecoratedMeasurementInOther(view);
            placeChild(view, sL, mOrientationHelper.getDecoratedMeasurement(view), otherStart, otherSize);
        }
        mRow.clear();
        mChunkConsumed = maxSize;
        mChunkNext = dir > 0 ? last + 1 : first - 1;
    }

    private void measureInSpans(View view, int[] borders, boolean exactMain, int mainSize) {
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        android.graphics.Rect decorInsets = new android.graphics.Rect();
        calculateItemDecorationsForChild(view, decorInsets);
        lp.mDecorInsets.set(decorInsets);
        int verticalInsets = decorInsets.top + decorInsets.bottom + lp.topMargin + lp.bottomMargin;
        int horizontalInsets = decorInsets.left + decorInsets.right + lp.leftMargin + lp.rightMargin;
        int availableSpaceInOther = borders[lp.mSpanIndex + lp.mSpanSize] - borders[lp.mSpanIndex];
        int wSpec;
        int hSpec;
        if (mOrientation == VERTICAL) {
            wSpec = View.MeasureSpec.makeMeasureSpec(Math.max(0, availableSpaceInOther - horizontalInsets),
                    View.MeasureSpec.EXACTLY);
            hSpec = exactMain
                    ? View.MeasureSpec.makeMeasureSpec(Math.max(0, mainSize - verticalInsets), View.MeasureSpec.EXACTLY)
                    : getChildMeasureSpec(getHeight(), getHeightMode(), verticalInsets, lp.height, true);
        } else {
            hSpec = View.MeasureSpec.makeMeasureSpec(Math.max(0, availableSpaceInOther - verticalInsets),
                    View.MeasureSpec.EXACTLY);
            wSpec = exactMain
                    ? View.MeasureSpec.makeMeasureSpec(Math.max(0, mainSize - horizontalInsets), View.MeasureSpec.EXACTLY)
                    : getChildMeasureSpec(getWidth(), getWidthMode(), horizontalInsets, lp.width, true);
        }
        view.measure(wSpec, hSpec);
    }

    // ------------------------------------------------------------ span lookups

    /// How many spans each item takes, and where its span starts.
    public abstract static class SpanSizeLookup {
        final SparseIntArray mSpanIndexCache = new SparseIntArray();
        final SparseIntArray mSpanGroupIndexCache = new SparseIntArray();
        private boolean mCacheSpanIndices;
        private boolean mCacheSpanGroupIndices;

        public abstract int getSpanSize(int position);

        public void setSpanIndexCacheEnabled(boolean cacheSpanIndices) {
            if (!cacheSpanIndices) {
                mSpanGroupIndexCache.clear();
            }
            mCacheSpanIndices = cacheSpanIndices;
        }

        public void setSpanGroupIndexCacheEnabled(boolean cacheSpanGroupIndices) {
            if (!cacheSpanGroupIndices) {
                mSpanGroupIndexCache.clear();
            }
            mCacheSpanGroupIndices = cacheSpanGroupIndices;
        }

        public void invalidateSpanIndexCache() {
            mSpanIndexCache.clear();
        }

        public void invalidateSpanGroupIndexCache() {
            mSpanGroupIndexCache.clear();
        }

        public boolean isSpanIndexCacheEnabled() {
            return mCacheSpanIndices;
        }

        public boolean isSpanGroupIndexCacheEnabled() {
            return mCacheSpanGroupIndices;
        }

        int getCachedSpanIndex(int position, int spanCount) {
            if (!mCacheSpanIndices) {
                return getSpanIndex(position, spanCount);
            }
            int existing = mSpanIndexCache.get(position, -1);
            if (existing != -1) {
                return existing;
            }
            int value = getSpanIndex(position, spanCount);
            mSpanIndexCache.put(position, value);
            return value;
        }

        int getCachedSpanGroupIndex(int position, int spanCount) {
            if (!mCacheSpanGroupIndices) {
                return getSpanGroupIndex(position, spanCount);
            }
            int existing = mSpanGroupIndexCache.get(position, -1);
            if (existing != -1) {
                return existing;
            }
            int value = getSpanGroupIndex(position, spanCount);
            mSpanGroupIndexCache.put(position, value);
            return value;
        }

        public int getSpanIndex(int position, int spanCount) {
            int positionSpanSize = getSpanSize(position);
            if (positionSpanSize == spanCount) {
                return 0;
            }
            int span = 0;
            for (int i = 0; i < position; i++) {
                int size = getSpanSize(i);
                span += size;
                if (span == spanCount) {
                    span = 0;
                } else if (span > spanCount) {
                    span = size;
                }
            }
            if (span + positionSpanSize <= spanCount) {
                return span;
            }
            return 0;
        }

        public int getSpanGroupIndex(int adapterPosition, int spanCount) {
            int span = 0;
            int group = 0;
            int positionSpanSize = getSpanSize(adapterPosition);
            for (int i = 0; i < adapterPosition; i++) {
                int size = getSpanSize(i);
                span += size;
                if (span == spanCount) {
                    span = 0;
                    group++;
                } else if (span > spanCount) {
                    span = size;
                    group++;
                }
            }
            if (span + positionSpanSize > spanCount) {
                group++;
            }
            return group;
        }
    }

    /// Every item takes one span.
    public static final class DefaultSpanSizeLookup extends SpanSizeLookup {
        @Override
        public int getSpanSize(int position) {
            return 1;
        }

        @Override
        public int getSpanIndex(int position, int spanCount) {
            return position % spanCount;
        }

        @Override
        public int getSpanGroupIndex(int adapterPosition, int spanCount) {
            return adapterPosition / spanCount;
        }
    }

    /// The layout parameters of a grid child: where its span starts and how
    /// many it takes.
    public static class LayoutParams extends RecyclerView.LayoutParams {
        public static final int INVALID_SPAN_ID = -1;
        int mSpanIndex = INVALID_SPAN_ID;
        int mSpanSize;

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

        public int getSpanIndex() {
            return mSpanIndex;
        }

        public int getSpanSize() {
            return mSpanSize;
        }
    }
}
