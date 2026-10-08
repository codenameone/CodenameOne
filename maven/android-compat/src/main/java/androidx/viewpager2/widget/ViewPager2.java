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
package androidx.viewpager2.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.R;

import java.util.ArrayList;
import java.util.List;

/// Pages through an adapter's items one full-size page at a time, swiping
/// horizontally (or vertically), built on a [RecyclerView] with a pager
/// snap. Pages must fill the pager (`match_parent` both ways), as on
/// Android.
public final class ViewPager2 extends ViewGroup {

    public static final int ORIENTATION_HORIZONTAL = RecyclerView.HORIZONTAL;
    public static final int ORIENTATION_VERTICAL = RecyclerView.VERTICAL;
    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_SETTLING = 2;
    public static final int OFFSCREEN_PAGE_LIMIT_DEFAULT = -1;

    /// Told as pages scroll and settle.
    public abstract static class OnPageChangeCallback {
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        public void onPageSelected(int position) {
        }

        public void onPageScrollStateChanged(int state) {
        }
    }

    /// Transforms each page as it scrolls; `position` is the page's offset
    /// from the current one, in pages (0 centered, -1 one page before).
    public interface PageTransformer {
        void transformPage(View page, float position);
    }

    private final RecyclerView mRecyclerView;
    private final LinearLayoutManager mLayoutManager;
    private final PagerSnapHelper mSnapHelper = new PagerSnapHelper();
    private final List<OnPageChangeCallback> mCallbacks = new ArrayList<OnPageChangeCallback>();
    private PageTransformer mPageTransformer;
    private int mCurrentItem;
    private int mScrollState = SCROLL_STATE_IDLE;
    private boolean mUserInputEnabled = true;
    private int mOffscreenPageLimit = OFFSCREEN_PAGE_LIMIT_DEFAULT;
    private RecyclerView.Adapter mAdapter;

    public ViewPager2(Context context) {
        this(context, null);
    }

    public ViewPager2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewPager2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mRecyclerView = new RecyclerView(context) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent e) {
                return mUserInputEnabled && super.onInterceptTouchEvent(e);
            }

            @Override
            public boolean onTouchEvent(MotionEvent e) {
                return mUserInputEnabled && super.onTouchEvent(e);
            }
        };
        mRecyclerView.setId(View.generateViewId());
        mLayoutManager = new LinearLayoutManager(context);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
        mRecyclerView.addOnChildAttachStateChangeListener(new EnforceMatchParent());
        int orientation = ORIENTATION_HORIZONTAL;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewPager2, defStyleAttr, 0);
            orientation = a.getInt(R.styleable.ViewPager2_android_orientation, ORIENTATION_HORIZONTAL);
            a.recycle();
        }
        setOrientation(orientation);
        mSnapHelper.attachToRecyclerView(mRecyclerView);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                int state = newState == RecyclerView.SCROLL_STATE_DRAGGING ? SCROLL_STATE_DRAGGING
                        : newState == RecyclerView.SCROLL_STATE_SETTLING ? SCROLL_STATE_SETTLING : SCROLL_STATE_IDLE;
                if (state == SCROLL_STATE_IDLE) {
                    updateCurrentItemFromScroll();
                }
                if (state != mScrollState) {
                    mScrollState = state;
                    for (OnPageChangeCallback c : new ArrayList<OnPageChangeCallback>(mCallbacks)) {
                        c.onPageScrollStateChanged(state);
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                dispatchScrolled();
            }
        });
        attachViewToParent(mRecyclerView, 0, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    /// Pages must fill the pager, as Android enforces.
    private static final class EnforceMatchParent implements RecyclerView.OnChildAttachStateChangeListener {
        @Override
        public void onChildViewAttachedToWindow(View view) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp.width != LayoutParams.MATCH_PARENT || lp.height != LayoutParams.MATCH_PARENT) {
                throw new IllegalStateException("Pages must fill the whole ViewPager2 (use match_parent)");
            }
        }

        @Override
        public void onChildViewDetachedFromWindow(View view) {
        }
    }

    private int pageSize() {
        return getOrientation() == ORIENTATION_HORIZONTAL
                ? mRecyclerView.getWidth() - mRecyclerView.getPaddingLeft() - mRecyclerView.getPaddingRight()
                : mRecyclerView.getHeight() - mRecyclerView.getPaddingTop() - mRecyclerView.getPaddingBottom();
    }

    private void dispatchScrolled() {
        int size = pageSize();
        if (size <= 0 || mLayoutManager.getChildCount() == 0) {
            return;
        }
        int first = mLayoutManager.findFirstVisibleItemPosition();
        View firstView = mLayoutManager.findViewByPosition(first);
        if (firstView == null) {
            return;
        }
        int start = getOrientation() == ORIENTATION_HORIZONTAL
                ? firstView.getLeft() - mRecyclerView.getPaddingLeft()
                : firstView.getTop() - mRecyclerView.getPaddingTop();
        if (getOrientation() == ORIENTATION_HORIZONTAL && getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            start = mRecyclerView.getWidth() - mRecyclerView.getPaddingRight() - firstView.getRight();
        }
        int offsetPx = -start;
        float offset = offsetPx / (float) size;
        for (OnPageChangeCallback c : new ArrayList<OnPageChangeCallback>(mCallbacks)) {
            c.onPageScrolled(first, offset, offsetPx);
        }
        if (mScrollState == SCROLL_STATE_IDLE && offsetPx == 0 && first != mCurrentItem) {
            setCurrentInternal(first);
        }
        applyTransformer();
    }

    private void applyTransformer() {
        if (mPageTransformer == null) {
            return;
        }
        int size = pageSize();
        if (size <= 0) {
            return;
        }
        for (int i = 0; i < mLayoutManager.getChildCount(); i++) {
            View page = mLayoutManager.getChildAt(i);
            int start = getOrientation() == ORIENTATION_HORIZONTAL
                    ? page.getLeft() - mRecyclerView.getPaddingLeft() : page.getTop() - mRecyclerView.getPaddingTop();
            mPageTransformer.transformPage(page, start / (float) size);
        }
    }

    private void updateCurrentItemFromScroll() {
        View snap = mSnapHelper.findSnapView(mLayoutManager);
        if (snap != null) {
            setCurrentInternal(mLayoutManager.getPosition(snap));
        }
    }

    private void setCurrentInternal(int item) {
        if (item == mCurrentItem) {
            return;
        }
        mCurrentItem = item;
        for (OnPageChangeCallback c : new ArrayList<OnPageChangeCallback>(mCallbacks)) {
            c.onPageSelected(item);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChild(mRecyclerView, widthMeasureSpec, heightMeasureSpec);
        int width = mRecyclerView.getMeasuredWidth() + getPaddingLeft() + getPaddingRight();
        int height = mRecyclerView.getMeasuredHeight() + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSizeAndState(Math.max(width, getSuggestedMinimumWidth()), widthMeasureSpec, 0),
                resolveSizeAndState(Math.max(height, getSuggestedMinimumHeight()), heightMeasureSpec, 0));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = mRecyclerView.getMeasuredWidth();
        int height = mRecyclerView.getMeasuredHeight();
        mRecyclerView.layout(getPaddingLeft(), getPaddingTop(), getPaddingLeft() + width, getPaddingTop() + height);
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        mAdapter = adapter;
        mRecyclerView.setAdapter(adapter);
        mCurrentItem = 0;
        if (adapter != null && adapter.getItemCount() > 0) {
            for (OnPageChangeCallback c : new ArrayList<OnPageChangeCallback>(mCallbacks)) {
                c.onPageSelected(0);
            }
        }
    }

    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    public void setOrientation(int orientation) {
        mLayoutManager.setOrientation(orientation);
    }

    public int getOrientation() {
        return mLayoutManager.getOrientation();
    }

    public void setCurrentItem(int item) {
        setCurrentItem(item, true);
    }

    public void setCurrentItem(int item, boolean smoothScroll) {
        if (mAdapter == null || mAdapter.getItemCount() <= 0) {
            return;
        }
        item = Math.max(0, Math.min(item, mAdapter.getItemCount() - 1));
        if (item == mCurrentItem && mScrollState == SCROLL_STATE_IDLE) {
            return;
        }
        setCurrentInternal(item);
        if (smoothScroll && Math.abs(item - mLayoutManager.findFirstVisibleItemPosition()) <= 3) {
            mRecyclerView.smoothScrollToPosition(item);
        } else {
            mRecyclerView.scrollToPosition(item);
        }
    }

    public int getCurrentItem() {
        return mCurrentItem;
    }

    public int getScrollState() {
        return mScrollState;
    }

    public void setUserInputEnabled(boolean enabled) {
        mUserInputEnabled = enabled;
    }

    public boolean isUserInputEnabled() {
        return mUserInputEnabled;
    }

    /// Recorded; pages outside the viewport are laid out as they scroll in,
    /// with the RecyclerView's view cache sized to keep this many around.
    public void setOffscreenPageLimit(int limit) {
        if (limit < 1 && limit != OFFSCREEN_PAGE_LIMIT_DEFAULT) {
            throw new IllegalArgumentException("Offscreen page limit must be OFFSCREEN_PAGE_LIMIT_DEFAULT or a number > 0");
        }
        mOffscreenPageLimit = limit;
        mRecyclerView.setItemViewCacheSize(limit == OFFSCREEN_PAGE_LIMIT_DEFAULT ? 2 : 2 * limit);
    }

    public int getOffscreenPageLimit() {
        return mOffscreenPageLimit;
    }

    public void registerOnPageChangeCallback(OnPageChangeCallback callback) {
        mCallbacks.add(callback);
    }

    public void unregisterOnPageChangeCallback(OnPageChangeCallback callback) {
        mCallbacks.remove(callback);
    }

    public void setPageTransformer(PageTransformer transformer) {
        mPageTransformer = transformer;
        requestTransform();
    }

    public void requestTransform() {
        applyTransformer();
    }

    public void addItemDecoration(RecyclerView.ItemDecoration decor) {
        mRecyclerView.addItemDecoration(decor);
    }

    public void addItemDecoration(RecyclerView.ItemDecoration decor, int index) {
        mRecyclerView.addItemDecoration(decor, index);
    }

    public RecyclerView.ItemDecoration getItemDecorationAt(int index) {
        return mRecyclerView.getItemDecorationAt(index);
    }

    public int getItemDecorationCount() {
        return mRecyclerView.getItemDecorationCount();
    }

    public void invalidateItemDecorations() {
        mRecyclerView.invalidateItemDecorations();
    }

    public void removeItemDecorationAt(int index) {
        mRecyclerView.removeItemDecorationAt(index);
    }

    public void removeItemDecoration(RecyclerView.ItemDecoration decor) {
        mRecyclerView.removeItemDecoration(decor);
    }

    public boolean isFakeDragging() {
        return false;
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return mRecyclerView.canScrollHorizontally(direction);
    }

    @Override
    public boolean canScrollVertically(int direction) {
        return mRecyclerView.canScrollVertically(direction);
    }
}
