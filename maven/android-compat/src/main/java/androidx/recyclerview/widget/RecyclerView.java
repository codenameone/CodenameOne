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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.VelocityTracker;
import android.view.animation.Interpolator;
import android.widget.EdgeEffect;
import android.widget.OverScroller;

import androidx.recyclerview.R;

import com.codename1.androidcompat.runtime.CompatReport;
import com.codename1.androidcompat.runtime.ScrollAnimator;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/// A scrolling list that keeps only the views on screen: as views leave the
/// screen their view holders are recycled and rebound for the items that
/// come into view. A [LayoutManager] positions the items, an [Adapter]
/// supplies them, [ItemDecoration]s draw around them and an [ItemAnimator]
/// animates adapter changes.
///
/// Adapter changes are applied to the view holders' positions as they are
/// notified and laid out on the next layout pass; there is no separate
/// pre-layout pass, so item animations are the simple kind (items that move
/// slide, new items fade in, removed items fade out) rather than predictive.
public class RecyclerView extends ViewGroup {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    public static final int NO_POSITION = -1;
    public static final long NO_ID = -1;
    public static final int INVALID_TYPE = -1;
    public static final int TOUCH_SLOP_DEFAULT = 0;
    public static final int TOUCH_SLOP_PAGING = 1;
    public static final int UNDEFINED_DURATION = Integer.MIN_VALUE;
    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_SETTLING = 2;

    static final int MAX_SCROLL_DURATION = 2000;
    static final int DEFAULT_ORIENTATION = VERTICAL;

    static final Interpolator QUINTIC_INTERPOLATOR = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    final Recycler mRecycler = new Recycler();
    final State mState = new State();
    Adapter mAdapter;
    LayoutManager mLayout;
    final ArrayList<ItemDecoration> mItemDecorations = new ArrayList<ItemDecoration>();
    private final ArrayList<OnItemTouchListener> mOnItemTouchListeners = new ArrayList<OnItemTouchListener>();
    private OnItemTouchListener mInterceptingOnItemTouchListener;
    private OnScrollListener mScrollListener;
    private List<OnScrollListener> mScrollListeners;
    private List<OnChildAttachStateChangeListener> mOnChildAttachStateListeners;
    RecyclerListener mRecyclerListener;
    final List<RecyclerListener> mRecyclerListeners = new ArrayList<RecyclerListener>();
    ItemAnimator mItemAnimator = new DefaultItemAnimator();
    private final ItemAnimatorRestoreListener mItemAnimatorListener = new ItemAnimatorRestoreListener();
    private final RecyclerViewDataObserver mObserver = new RecyclerViewDataObserver();
    private OnFlingListener mOnFlingListener;
    private EdgeEffectFactory mEdgeEffectFactory = new EdgeEffectFactory();
    private ChildDrawingOrderCallback mChildDrawingOrderCallback;

    boolean mHasFixedSize;
    boolean mIsAttached;
    boolean mLayoutSuppressed;
    private int mInterceptRequestLayoutDepth;
    boolean mLayoutWasDefered;
    boolean mInLayout;
    boolean mDataSetHasChangedAfterLayout;
    /// Set by item insertions, removals, moves and changes since the last
    /// layout: the next layout animates instead of jumping.
    boolean mItemsChangedForAnimation;
    private final ArrayList<int[]> mInsertedRanges = new ArrayList<int[]>();
    private boolean mClipToPaddingSet = true;
    boolean mPreserveFocusAfterLayout = true;
    private boolean mNestedScrollingEnabled = true;

    /// Views that left the adapter and fade out: still children of this
    /// view, never seen by the layout manager.
    final ArrayList<View> mHiddenViews = new ArrayList<View>();

    private int mScrollState = SCROLL_STATE_IDLE;
    private int mScrollPointerId = -1;
    private VelocityTracker mVelocityTracker;
    private int mInitialTouchX;
    private int mInitialTouchY;
    private int mLastTouchX;
    private int mLastTouchY;
    private int mTouchSlop;
    private final int mMinFlingVelocity;
    private final int mMaxFlingVelocity;
    final ViewFlinger mViewFlinger = new ViewFlinger();
    private final int[] mMinMaxLayoutPositions = new int[2];
    private final Rect mTempRect = new Rect();

    public RecyclerView(Context context) {
        this(context, null);
    }

    public RecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.recyclerViewStyle);
    }

    public RecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        setFocusableInTouchMode(true);
        setWillNotDraw(true);
        mItemAnimator.setListener(mItemAnimatorListener);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecyclerView, defStyleAttr, 0);
            String layoutManagerName = a.getString(R.styleable.RecyclerView_layoutManager);
            if (a.hasValue(R.styleable.RecyclerView_android_clipToPadding)) {
                setClipToPadding(a.getBoolean(R.styleable.RecyclerView_android_clipToPadding, true));
            }
            if (a.getBoolean(R.styleable.RecyclerView_fastScrollEnabled, false)) {
                CompatReport.unsupported("RecyclerView", "fastScrollEnabled (the list scrolls without a fast-scroll thumb)");
            }
            a.recycle();
            createLayoutManager(context, layoutManagerName, attrs, defStyleAttr, 0);
        }
    }

    /// `app:layoutManager`: the standard layout managers by name, without
    /// reflection. A name without a dot is in this package; a leading dot
    /// is relative to the application package, as on Android.
    private void createLayoutManager(Context context, String className, AttributeSet attrs,
                                     int defStyleAttr, int defStyleRes) {
        if (className == null) {
            return;
        }
        className = className.trim();
        if (className.length() == 0) {
            return;
        }
        String simple = className;
        String prefix = RecyclerView.class.getName();
        prefix = prefix.substring(0, prefix.lastIndexOf('.') + 1);
        if (simple.startsWith(prefix)) {
            simple = simple.substring(prefix.length());
        } else if (simple.startsWith("androidx.recyclerview.widget.")) {
            simple = simple.substring("androidx.recyclerview.widget.".length());
        }
        LayoutManager lm;
        if ("LinearLayoutManager".equals(simple)) {
            lm = new LinearLayoutManager(context, attrs, defStyleAttr, defStyleRes);
        } else if ("GridLayoutManager".equals(simple)) {
            lm = new GridLayoutManager(context, attrs, defStyleAttr, defStyleRes);
        } else if ("StaggeredGridLayoutManager".equals(simple)) {
            lm = new StaggeredGridLayoutManager(context, attrs, defStyleAttr, defStyleRes);
        } else {
            CompatReport.unsupported("RecyclerView", "app:layoutManager=\"" + className
                    + "\" (only the standard layout managers can be named in XML; set it in code)");
            return;
        }
        setLayoutManager(lm);
    }

    String exceptionLabel() {
        return " " + super.toString() + ", adapter:" + mAdapter + ", layout:" + mLayout + ", context:" + getContext();
    }

    // ------------------------------------------------------------ configuration

    public void setHasFixedSize(boolean hasFixedSize) {
        mHasFixedSize = hasFixedSize;
    }

    public boolean hasFixedSize() {
        return mHasFixedSize;
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        if (clipToPadding != mClipToPaddingSet) {
            invalidateGlows();
        }
        mClipToPaddingSet = clipToPadding;
        super.setClipToPadding(clipToPadding);
        if (mIsAttached) {
            requestLayout();
        }
    }

    @Override
    public boolean getClipToPadding() {
        return mClipToPaddingSet;
    }

    private void invalidateGlows() {
    }

    public void setScrollingTouchSlop(int slopConstant) {
        ViewConfiguration vc = ViewConfiguration.get(getContext());
        mTouchSlop = slopConstant == TOUCH_SLOP_PAGING ? vc.getScaledPagingTouchSlop() : vc.getScaledTouchSlop();
    }

    public void setItemViewCacheSize(int size) {
        mRecycler.setViewCacheSize(size);
    }

    public RecycledViewPool getRecycledViewPool() {
        return mRecycler.getRecycledViewPool();
    }

    public void setRecycledViewPool(RecycledViewPool pool) {
        mRecycler.setRecycledViewPool(pool);
    }

    public void setViewCacheExtension(ViewCacheExtension extension) {
        mRecycler.mViewCacheExtension = extension;
    }

    public void setEdgeEffectFactory(EdgeEffectFactory edgeEffectFactory) {
        if (edgeEffectFactory == null) {
            throw new NullPointerException("edgeEffectFactory");
        }
        mEdgeEffectFactory = edgeEffectFactory;
    }

    public EdgeEffectFactory getEdgeEffectFactory() {
        return mEdgeEffectFactory;
    }

    /// Stored, not applied: like every view group here, a RecyclerView's
    /// children paint in child order. Their Codename One peers are painted
    /// by Container in component order, and this runtime honours no
    /// per-child paint order anywhere -- ViewGroup.getChildDrawingOrder,
    /// elevation and translationZ are ignored too -- so this callback stays
    /// consistent with the rest rather than reordering one widget's peers.
    /// The runtime's own ItemTouchHelper does not rely on it.
    public void setChildDrawingOrderCallback(ChildDrawingOrderCallback callback) {
        mChildDrawingOrderCallback = callback;
    }

    public void setPreserveFocusAfterLayout(boolean preserveFocusAfterLayout) {
        mPreserveFocusAfterLayout = preserveFocusAfterLayout;
    }

    public boolean getPreserveFocusAfterLayout() {
        return mPreserveFocusAfterLayout;
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingEnabled = enabled;
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingEnabled;
    }

    public void setRecyclerListener(RecyclerListener listener) {
        mRecyclerListener = listener;
    }

    public void addRecyclerListener(RecyclerListener listener) {
        mRecyclerListeners.add(listener);
    }

    public void removeRecyclerListener(RecyclerListener listener) {
        mRecyclerListeners.remove(listener);
    }

    public void addOnChildAttachStateChangeListener(OnChildAttachStateChangeListener listener) {
        if (mOnChildAttachStateListeners == null) {
            mOnChildAttachStateListeners = new ArrayList<OnChildAttachStateChangeListener>();
        }
        mOnChildAttachStateListeners.add(listener);
    }

    public void removeOnChildAttachStateChangeListener(OnChildAttachStateChangeListener listener) {
        if (mOnChildAttachStateListeners != null) {
            mOnChildAttachStateListeners.remove(listener);
        }
    }

    public void clearOnChildAttachStateChangeListeners() {
        if (mOnChildAttachStateListeners != null) {
            mOnChildAttachStateListeners.clear();
        }
    }

    public void setOnFlingListener(OnFlingListener onFlingListener) {
        mOnFlingListener = onFlingListener;
    }

    public OnFlingListener getOnFlingListener() {
        return mOnFlingListener;
    }

    public int getMinFlingVelocity() {
        return mMinFlingVelocity;
    }

    public int getMaxFlingVelocity() {
        return mMaxFlingVelocity;
    }

    // ------------------------------------------------------------ adapter

    public void setAdapter(Adapter adapter) {
        setLayoutFrozen(false);
        setAdapterInternal(adapter, false, true);
        processDataSetCompletelyChanged(false);
        requestLayout();
    }

    public void swapAdapter(Adapter adapter, boolean removeAndRecycleExistingViews) {
        setLayoutFrozen(false);
        setAdapterInternal(adapter, true, removeAndRecycleExistingViews);
        processDataSetCompletelyChanged(true);
        requestLayout();
    }

    private void setAdapterInternal(Adapter adapter, boolean compatibleWithPrevious,
                                    boolean removeAndRecycleViews) {
        if (mAdapter != null) {
            mAdapter.unregisterAdapterDataObserver(mObserver);
            mAdapter.onDetachedFromRecyclerView(this);
        }
        if (!compatibleWithPrevious || removeAndRecycleViews) {
            removeAndRecycleViews();
        }
        Adapter oldAdapter = mAdapter;
        mAdapter = adapter;
        if (adapter != null) {
            adapter.registerAdapterDataObserver(mObserver);
            adapter.onAttachedToRecyclerView(this);
        }
        if (mLayout != null) {
            mLayout.onAdapterChanged(oldAdapter, mAdapter);
        }
        mRecycler.onAdapterChanged(oldAdapter, mAdapter, compatibleWithPrevious);
        mState.mStructureChanged = true;
    }

    void removeAndRecycleViews() {
        if (mItemAnimator != null) {
            mItemAnimator.endAnimations();
        }
        if (mLayout != null) {
            mLayout.removeAndRecycleAllViews(mRecycler);
            mLayout.removeAndRecycleScrapInt(mRecycler);
        }
        mRecycler.clear();
    }

    public Adapter getAdapter() {
        return mAdapter;
    }

    // ------------------------------------------------------------ layout manager

    public void setLayoutManager(LayoutManager layout) {
        if (layout == mLayout) {
            return;
        }
        stopScroll();
        if (mLayout != null) {
            if (mItemAnimator != null) {
                mItemAnimator.endAnimations();
            }
            mLayout.removeAndRecycleAllViews(mRecycler);
            mLayout.removeAndRecycleScrapInt(mRecycler);
            mRecycler.clear();
            if (mIsAttached) {
                mLayout.dispatchDetachedFromWindow(this, mRecycler);
            }
            mLayout.setRecyclerView(null);
            mLayout = null;
        } else {
            mRecycler.clear();
        }
        removeHiddenViews();
        mLayout = layout;
        if (layout != null) {
            if (layout.mRecyclerView != null) {
                throw new IllegalArgumentException("LayoutManager " + layout
                        + " is already attached to a RecyclerView:" + layout.mRecyclerView.exceptionLabel());
            }
            mLayout.setRecyclerView(this);
            if (mIsAttached) {
                mLayout.dispatchAttachedToWindow(this);
            }
        }
        mRecycler.updateViewCacheSize();
        requestLayout();
    }

    public LayoutManager getLayoutManager() {
        return mLayout;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        if (mLayout == null) {
            return new LayoutParams(getContext(), attrs);
        }
        return mLayout.generateLayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (mLayout == null) {
            return p instanceof LayoutParams ? new LayoutParams((LayoutParams) p)
                    : p instanceof MarginLayoutParams ? new LayoutParams((MarginLayoutParams) p) : new LayoutParams(p);
        }
        return mLayout.generateLayoutParams(p);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        if (mLayout == null) {
            throw new IllegalStateException("RecyclerView has no LayoutManager" + exceptionLabel());
        }
        return mLayout.generateDefaultLayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && (mLayout == null || mLayout.checkLayoutParams((LayoutParams) p));
    }

    // ------------------------------------------------------------ decorations and listeners

    public void addItemDecoration(ItemDecoration decor, int index) {
        if (mItemDecorations.isEmpty()) {
            setWillNotDraw(false);
        }
        if (index < 0) {
            mItemDecorations.add(decor);
        } else {
            mItemDecorations.add(index, decor);
        }
        markItemDecorInsetsDirty();
        requestLayout();
    }

    public void addItemDecoration(ItemDecoration decor) {
        addItemDecoration(decor, -1);
    }

    public ItemDecoration getItemDecorationAt(int index) {
        int size = getItemDecorationCount();
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index + " is an invalid index for size " + size);
        }
        return mItemDecorations.get(index);
    }

    public int getItemDecorationCount() {
        return mItemDecorations.size();
    }

    public void removeItemDecorationAt(int index) {
        removeItemDecoration(getItemDecorationAt(index));
    }

    public void removeItemDecoration(ItemDecoration decor) {
        mItemDecorations.remove(decor);
        if (mItemDecorations.isEmpty()) {
            setWillNotDraw(true);
        }
        markItemDecorInsetsDirty();
        requestLayout();
    }

    public void invalidateItemDecorations() {
        if (mItemDecorations.isEmpty()) {
            return;
        }
        markItemDecorInsetsDirty();
        requestLayout();
    }

    void markItemDecorInsetsDirty() {
        for (int i = 0; i < getChildCount(); i++) {
            ViewGroup.LayoutParams lp = getChildAt(i).getLayoutParams();
            if (lp instanceof LayoutParams) {
                ((LayoutParams) lp).mInsetsDirty = true;
            }
        }
        mRecycler.markItemDecorInsetsDirty();
    }

    @Deprecated
    public void setOnScrollListener(OnScrollListener listener) {
        mScrollListener = listener;
    }

    public void addOnScrollListener(OnScrollListener listener) {
        if (mScrollListeners == null) {
            mScrollListeners = new ArrayList<OnScrollListener>();
        }
        mScrollListeners.add(listener);
    }

    public void removeOnScrollListener(OnScrollListener listener) {
        if (mScrollListeners != null) {
            mScrollListeners.remove(listener);
        }
    }

    public void clearOnScrollListeners() {
        if (mScrollListeners != null) {
            mScrollListeners.clear();
        }
    }

    public void addOnItemTouchListener(OnItemTouchListener listener) {
        mOnItemTouchListeners.add(listener);
    }

    public void removeOnItemTouchListener(OnItemTouchListener listener) {
        mOnItemTouchListeners.remove(listener);
        if (mInterceptingOnItemTouchListener == listener) {
            mInterceptingOnItemTouchListener = null;
        }
    }

    public void setItemAnimator(ItemAnimator animator) {
        if (mItemAnimator != null) {
            mItemAnimator.endAnimations();
            mItemAnimator.setListener(null);
        }
        mItemAnimator = animator;
        if (mItemAnimator != null) {
            mItemAnimator.setListener(mItemAnimatorListener);
        }
    }

    public ItemAnimator getItemAnimator() {
        return mItemAnimator;
    }

    public boolean isAnimating() {
        return mItemAnimator != null && mItemAnimator.isRunning();
    }

    // ------------------------------------------------------------ layout suppression

    public void suppressLayout(boolean suppress) {
        if (suppress != mLayoutSuppressed) {
            if (!suppress) {
                mLayoutSuppressed = false;
                if (mLayoutWasDefered && mLayout != null && mAdapter != null) {
                    requestLayout();
                }
                mLayoutWasDefered = false;
            } else {
                mLayoutSuppressed = true;
                stopScroll();
            }
        }
    }

    public boolean isLayoutSuppressed() {
        return mLayoutSuppressed;
    }

    @Deprecated
    public void setLayoutFrozen(boolean frozen) {
        suppressLayout(frozen);
    }

    @Deprecated
    public boolean isLayoutFrozen() {
        return isLayoutSuppressed();
    }

    public boolean isComputingLayout() {
        return mInLayout;
    }

    void startInterceptRequestLayout() {
        mInterceptRequestLayoutDepth++;
        if (mInterceptRequestLayoutDepth == 1 && !mLayoutSuppressed) {
            mLayoutWasDefered = false;
        }
    }

    void stopInterceptRequestLayout(boolean performLayoutChildren) {
        if (mInterceptRequestLayoutDepth < 1) {
            mInterceptRequestLayoutDepth = 1;
        }
        if (!performLayoutChildren && !mLayoutSuppressed) {
            mLayoutWasDefered = false;
        }
        if (mInterceptRequestLayoutDepth == 1) {
            if (performLayoutChildren && mLayoutWasDefered && !mLayoutSuppressed && mLayout != null && mAdapter != null) {
                dispatchLayout();
            }
            if (!mLayoutSuppressed) {
                mLayoutWasDefered = false;
            }
        }
        mInterceptRequestLayoutDepth--;
    }

    @Override
    public void requestLayout() {
        if (mInterceptRequestLayoutDepth == 0 && !mLayoutSuppressed) {
            super.requestLayout();
        } else {
            mLayoutWasDefered = true;
        }
    }

    // ------------------------------------------------------------ attach

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttached = true;
        if (mLayout != null) {
            mLayout.dispatchAttachedToWindow(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mItemAnimator != null) {
            mItemAnimator.endAnimations();
        }
        stopScroll();
        mIsAttached = false;
        if (mLayout != null) {
            mLayout.dispatchDetachedFromWindow(this, mRecycler);
        }
    }

    @Override
    public boolean isAttachedToWindow() {
        return mIsAttached || super.isAttachedToWindow();
    }

    // ------------------------------------------------------------ measure & layout

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        if (mLayout == null) {
            defaultOnMeasure(widthSpec, heightSpec);
            return;
        }
        if (mLayout.isAutoMeasureEnabled()) {
            int widthMode = MeasureSpec.getMode(widthSpec);
            int heightMode = MeasureSpec.getMode(heightSpec);
            mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
            boolean measureSpecModeIsExactly = widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY;
            if (measureSpecModeIsExactly || mAdapter == null) {
                return;
            }
            // Wrap content: lay the items out in the space offered and size
            // to them, as Android's auto-measure does.
            mLayout.setMeasureSpecs(widthSpec, heightSpec);
            mState.mIsMeasuring = true;
            dispatchLayoutInternal();
            mState.mIsMeasuring = false;
            mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec);
        } else {
            if (mHasFixedSize) {
                mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
                return;
            }
            mState.mItemCount = mAdapter != null ? mAdapter.getItemCount() : 0;
            mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
        }
    }

    void defaultOnMeasure(int widthSpec, int heightSpec) {
        int width = LayoutManager.chooseSize(widthSpec, getPaddingLeft() + getPaddingRight(), getSuggestedMinimumWidth());
        int height = LayoutManager.chooseSize(heightSpec, getPaddingTop() + getPaddingBottom(), getSuggestedMinimumHeight());
        setMeasuredDimension(width, height);
    }

    void setMeasuredDimensionInternal(int width, int height) {
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            invalidateGlows();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        dispatchLayout();
    }

    /// The layout pass: positions are already current (adapter changes were
    /// applied when they were notified), so the layout manager lays out
    /// against the adapter as it is, and the item animator runs on the
    /// difference between the views before and after.
    void dispatchLayout() {
        if (mAdapter == null || mLayout == null) {
            return;
        }
        mLayout.setExactMeasureSpecsFrom(this);
        dispatchLayoutInternal();
    }

    private void dispatchLayoutInternal() {
        if (mAdapter == null || mLayout == null) {
            return;
        }
        boolean measuring = mState.mIsMeasuring;
        boolean animate = !measuring && mItemAnimator != null
                && (mItemsChangedForAnimation || mLayout.mRequestedSimpleAnimations)
                && !mDataSetHasChangedAfterLayout && mIsAttached && mLayout.getChildCount() > 0;
        IdentityHashMap<ViewHolder, ItemAnimator.ItemHolderInfo> pre = null;
        if (animate) {
            pre = new IdentityHashMap<ViewHolder, ItemAnimator.ItemHolderInfo>();
            for (int i = 0; i < mLayout.getChildCount(); i++) {
                View child = mLayout.getChildAt(i);
                ViewHolder holder = getChildViewHolderInt(child);
                if (holder == null || holder.shouldIgnore()) {
                    continue;
                }
                mItemAnimator.endAnimation(holder);
                pre.put(holder, mItemAnimator.recordPreLayoutInformation(mState, holder,
                        ItemAnimator.buildAdapterChangeFlagsForAnimations(holder), holder.getUnmodifiedPayloads()));
            }
        }
        findMinMaxChildLayoutPositions(mMinMaxLayoutPositions);
        int minBefore = mMinMaxLayoutPositions[0];
        int maxBefore = mMinMaxLayoutPositions[1];
        startInterceptRequestLayout();
        mInLayout = true;
        mState.mItemCount = mAdapter.getItemCount();
        mState.mInPreLayout = false;
        mState.mRunSimpleAnimations = animate;
        mLayout.onLayoutChildren(mRecycler, mState);
        mState.mStructureChanged = false;
        if (!measuring) {
            mLayout.onLayoutCompleted(mState);
        }
        if (animate) {
            runLayoutAnimations(pre);
        }
        mLayout.removeAndRecycleScrapInt(mRecycler);
        mRecycler.updateViewCacheSize();
        if (!measuring) {
            mLayout.mRequestedSimpleAnimations = false;
            mItemsChangedForAnimation = false;
            mDataSetHasChangedAfterLayout = false;
            mInsertedRanges.clear();
            clearOldPositions();
            mState.mTargetPosition = NO_POSITION;
        }
        mInLayout = false;
        stopInterceptRequestLayout(false);
        if (!measuring) {
            findMinMaxChildLayoutPositions(mMinMaxLayoutPositions);
            if (mMinMaxLayoutPositions[0] != minBefore || mMinMaxLayoutPositions[1] != maxBefore) {
                dispatchOnScrolled(0, 0);
            }
            invalidate();
        }
    }

    private void runLayoutAnimations(IdentityHashMap<ViewHolder, ItemAnimator.ItemHolderInfo> pre) {
        boolean any = false;
        for (int i = 0; i < mLayout.getChildCount(); i++) {
            View child = mLayout.getChildAt(i);
            ViewHolder holder = getChildViewHolderInt(child);
            if (holder == null || holder.shouldIgnore()) {
                continue;
            }
            ItemAnimator.ItemHolderInfo post = mItemAnimator.recordPostLayoutInformation(mState, holder);
            ItemAnimator.ItemHolderInfo before = pre.remove(holder);
            if (before != null) {
                if (holder.mAnimationChanged) {
                    holder.mAnimationChanged = false;
                    if (mItemAnimator.animateChange(holder, holder, before, post)) {
                        any = true;
                    }
                } else if (mItemAnimator.animatePersistence(holder, before, post)) {
                    any = true;
                }
            } else if (wasInserted(holder.mPosition) && mItemAnimator.animateAppearance(holder, null, post)) {
                any = true;
            }
        }
        // What is left was on screen before and is not now: an item removed
        // from the adapter fades out where it was; one pushed off screen
        // simply goes.
        for (Map.Entry<ViewHolder, ItemAnimator.ItemHolderInfo> e : pre.entrySet()) {
            ViewHolder holder = e.getKey();
            if (!holder.isRemoved() || !holder.isScrap()) {
                continue;
            }
            mRecycler.unscrapView(holder);
            View view = holder.itemView;
            ItemAnimator.ItemHolderInfo info = e.getValue();
            addHiddenView(view);
            view.layout(info.left, info.top, info.right, info.bottom);
            holder.setIsRecyclable(false);
            if (mItemAnimator.animateDisappearance(holder, info, null)) {
                any = true;
            } else {
                holder.setIsRecyclable(true);
                removeHiddenView(view);
                mRecycler.recycleViewHolderInternal(holder);
            }
        }
        if (any) {
            mItemAnimator.runPendingAnimations();
        }
    }

    private boolean wasInserted(int position) {
        for (int[] r : mInsertedRanges) {
            if (position >= r[0] && position < r[0] + r[1]) {
                return true;
            }
        }
        return false;
    }

    private void clearOldPositions() {
        for (int i = 0; i < getChildCount(); i++) {
            ViewHolder holder = getChildViewHolderInt(getChildAt(i));
            if (holder != null) {
                holder.clearOldPosition();
            }
        }
        mRecycler.clearOldPositions();
    }

    private void findMinMaxChildLayoutPositions(int[] into) {
        int count = mLayout == null ? 0 : mLayout.getChildCount();
        if (count == 0) {
            into[0] = NO_POSITION;
            into[1] = NO_POSITION;
            return;
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            ViewHolder holder = getChildViewHolderInt(mLayout.getChildAt(i));
            if (holder == null || holder.shouldIgnore()) {
                continue;
            }
            int pos = holder.getLayoutPosition();
            if (pos < min) {
                min = pos;
            }
            if (pos > max) {
                max = pos;
            }
        }
        into[0] = min;
        into[1] = max;
    }

    // ------------------------------------------------------------ hidden (disappearing) children

    void addHiddenView(View view) {
        ViewHolder holder = getChildViewHolderInt(view);
        if (view.getParent() == this) {
            if (!mHiddenViews.contains(view)) {
                mHiddenViews.add(view);
            }
            return;
        }
        if (holder != null && holder.isTmpDetached()) {
            holder.clearTmpDetachFlag();
            attachViewToParent(view, 0, view.getLayoutParams());
        } else {
            addViewInLayout(view, 0, view.getLayoutParams(), true);
        }
        mHiddenViews.add(view);
    }

    void removeHiddenView(View view) {
        mHiddenViews.remove(view);
        if (view.getParent() == this) {
            removeViewInLayout(view);
        }
    }

    private void removeHiddenViews() {
        for (int i = mHiddenViews.size() - 1; i >= 0; i--) {
            View v = mHiddenViews.get(i);
            ViewHolder holder = getChildViewHolderInt(v);
            removeHiddenView(v);
            if (holder != null) {
                holder.setIsRecyclable(true);
            }
        }
    }

    boolean isHidden(View view) {
        return mHiddenViews.contains(view);
    }

    int managedChildCount() {
        return getChildCount() - mHiddenViews.size();
    }

    View managedChildAt(int index) {
        if (mHiddenViews.isEmpty()) {
            return getChildAt(index);
        }
        int seen = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (mHiddenViews.contains(v)) {
                continue;
            }
            if (seen == index) {
                return v;
            }
            seen++;
        }
        return null;
    }

    int managedIndexOf(View view) {
        if (mHiddenViews.contains(view)) {
            return -1;
        }
        int seen = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v == view) {
                return seen;
            }
            if (!mHiddenViews.contains(v)) {
                seen++;
            }
        }
        return -1;
    }

    private int rawIndexForManaged(int index) {
        int count = managedChildCount();
        if (index < 0 || index >= count) {
            return getChildCount();
        }
        return indexOfChild(managedChildAt(index));
    }

    void attachManaged(View child, int index, ViewGroup.LayoutParams lp) {
        ViewHolder holder = getChildViewHolderInt(child);
        if (holder != null) {
            holder.clearTmpDetachFlag();
        }
        attachViewToParent(child, rawIndexForManaged(index), lp);
    }

    void addManaged(View child, int index) {
        addViewInLayout(child, rawIndexForManaged(index), child.getLayoutParams(), true);
        dispatchChildAttached(child);
    }

    void moveManaged(View child, int toIndex) {
        int from = indexOfChild(child);
        if (from < 0) {
            return;
        }
        detachViewFromParent(from);
        attachViewToParent(child, rawIndexForManaged(toIndex), child.getLayoutParams());
    }

    void detachManaged(int index) {
        View child = managedChildAt(index);
        if (child == null) {
            return;
        }
        ViewHolder holder = getChildViewHolderInt(child);
        if (holder != null) {
            holder.addFlags(ViewHolder.FLAG_TMP_DETACHED);
        }
        detachViewFromParent(indexOfChild(child));
    }

    void removeManaged(View child) {
        if (child.getParent() == this) {
            removeViewInLayout(child);
            dispatchChildDetached(child);
        }
    }

    void dispatchChildAttached(View child) {
        ViewHolder viewHolder = getChildViewHolderInt(child);
        onChildAttachedToWindow(child);
        if (mAdapter != null && viewHolder != null) {
            mAdapter.onViewAttachedToWindow(viewHolder);
        }
        if (mOnChildAttachStateListeners != null) {
            for (int i = mOnChildAttachStateListeners.size() - 1; i >= 0; i--) {
                mOnChildAttachStateListeners.get(i).onChildViewAttachedToWindow(child);
            }
        }
    }

    void dispatchChildDetached(View child) {
        ViewHolder viewHolder = getChildViewHolderInt(child);
        onChildDetachedFromWindow(child);
        if (mAdapter != null && viewHolder != null) {
            mAdapter.onViewDetachedFromWindow(viewHolder);
        }
        if (mOnChildAttachStateListeners != null) {
            for (int i = mOnChildAttachStateListeners.size() - 1; i >= 0; i--) {
                mOnChildAttachStateListeners.get(i).onChildViewDetachedFromWindow(child);
            }
        }
    }

    public void onChildAttachedToWindow(View child) {
    }

    public void onChildDetachedFromWindow(View child) {
    }

    // ------------------------------------------------------------ adapter updates

    /// The view holders whose positions follow the adapter: on screen, in
    /// the scrap, cached and fading out.
    private List<ViewHolder> knownViewHolders() {
        ArrayList<ViewHolder> out = new ArrayList<ViewHolder>();
        for (int i = 0; i < getChildCount(); i++) {
            ViewHolder h = getChildViewHolderInt(getChildAt(i));
            if (h != null && !h.shouldIgnore()) {
                out.add(h);
            }
        }
        for (ViewHolder h : mRecycler.mAttachedScrap) {
            if (!out.contains(h)) {
                out.add(h);
            }
        }
        out.addAll(mRecycler.mCachedViews);
        return out;
    }

    void offsetPositionRecordsForInsert(int positionStart, int itemCount) {
        for (ViewHolder holder : knownViewHolders()) {
            if (holder.mPosition >= positionStart && !holder.isRemoved()) {
                holder.offsetPosition(itemCount);
            }
        }
        for (int[] r : mInsertedRanges) {
            if (r[0] >= positionStart) {
                r[0] += itemCount;
            }
        }
        mInsertedRanges.add(new int[] {positionStart, itemCount});
        mState.mStructureChanged = true;
        markItemDecorInsetsDirty();
    }

    void offsetPositionRecordsForRemove(int positionStart, int itemCount) {
        int end = positionStart + itemCount;
        for (ViewHolder holder : knownViewHolders()) {
            if (holder.isRemoved()) {
                continue;
            }
            if (holder.mPosition >= end) {
                holder.offsetPosition(-itemCount);
            } else if (holder.mPosition >= positionStart) {
                holder.saveOldPosition();
                holder.addFlags(ViewHolder.FLAG_REMOVED);
            }
        }
        mRecycler.recycleRemovedCachedViews();
        for (int i = mInsertedRanges.size() - 1; i >= 0; i--) {
            int[] r = mInsertedRanges.get(i);
            if (r[0] >= end) {
                r[0] -= itemCount;
            } else if (r[0] + r[1] > positionStart) {
                mInsertedRanges.remove(i);
            }
        }
        mState.mStructureChanged = true;
        markItemDecorInsetsDirty();
    }

    void offsetPositionRecordsForMove(int from, int to) {
        int start;
        int end;
        int inBetweenOffset;
        if (from < to) {
            start = from;
            end = to;
            inBetweenOffset = -1;
        } else {
            start = to;
            end = from;
            inBetweenOffset = 1;
        }
        for (ViewHolder holder : knownViewHolders()) {
            if (holder.isRemoved() || holder.mPosition < start || holder.mPosition > end) {
                continue;
            }
            if (holder.mPosition == from) {
                holder.offsetPosition(to - from);
            } else {
                holder.offsetPosition(inBetweenOffset);
            }
        }
        mState.mStructureChanged = true;
        markItemDecorInsetsDirty();
    }

    void viewRangeUpdate(int positionStart, int itemCount, Object payload) {
        int end = positionStart + itemCount;
        for (ViewHolder holder : knownViewHolders()) {
            if (holder.isRemoved()) {
                continue;
            }
            if (holder.mPosition >= positionStart && holder.mPosition < end) {
                holder.addFlags(ViewHolder.FLAG_UPDATE);
                holder.addChangePayload(payload);
                holder.mAnimationChanged = true;
                ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
                if (lp instanceof LayoutParams) {
                    ((LayoutParams) lp).mInsetsDirty = true;
                }
            }
        }
        mRecycler.viewRangeUpdate(positionStart, itemCount);
    }

    void processDataSetCompletelyChanged(boolean dispatchItemsChanged) {
        mDataSetHasChangedAfterLayout = true;
        mItemsChangedForAnimation = false;
        mInsertedRanges.clear();
        for (int i = 0; i < getChildCount(); i++) {
            ViewHolder holder = getChildViewHolderInt(getChildAt(i));
            if (holder != null && !holder.shouldIgnore()) {
                holder.addFlags(ViewHolder.FLAG_UPDATE | ViewHolder.FLAG_INVALID);
            }
        }
        markItemDecorInsetsDirty();
        mRecycler.markKnownViewsInvalid();
        if (dispatchItemsChanged && mLayout != null) {
            mLayout.onItemsChanged(this);
        }
    }

    /// With a fixed size the adapter's changes cannot change this view's
    /// size, so only this view lays out again rather than its whole window.
    void triggerUpdateProcessor() {
        if (mHasFixedSize && mIsAttached && !mLayoutSuppressed) {
            if (!mUpdatePosted) {
                mUpdatePosted = true;
                post(mUpdateChildViewsRunnable);
            }
        } else {
            requestLayout();
        }
    }

    private boolean mUpdatePosted;
    private final Runnable mUpdateChildViewsRunnable = new Runnable() {
        @Override
        public void run() {
            mUpdatePosted = false;
            if (!mIsAttached || mLayoutSuppressed) {
                if (mLayoutSuppressed) {
                    mLayoutWasDefered = true;
                }
                return;
            }
            dispatchLayout();
        }
    };

    private final class RecyclerViewDataObserver extends AdapterDataObserver {
        @Override
        public void onChanged() {
            mState.mStructureChanged = true;
            processDataSetCompletelyChanged(true);
            requestLayout();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            if (itemCount < 1) {
                return;
            }
            viewRangeUpdate(positionStart, itemCount, payload);
            mItemsChangedForAnimation = true;
            if (mLayout != null) {
                mLayout.onItemsUpdated(RecyclerView.this, positionStart, itemCount, payload);
            }
            triggerUpdateProcessor();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (itemCount < 1) {
                return;
            }
            offsetPositionRecordsForInsert(positionStart, itemCount);
            mItemsChangedForAnimation = true;
            if (mLayout != null) {
                mLayout.onItemsAdded(RecyclerView.this, positionStart, itemCount);
            }
            triggerUpdateProcessor();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            if (itemCount < 1) {
                return;
            }
            offsetPositionRecordsForRemove(positionStart, itemCount);
            mItemsChangedForAnimation = true;
            if (mLayout != null) {
                mLayout.onItemsRemoved(RecyclerView.this, positionStart, itemCount);
            }
            triggerUpdateProcessor();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            if (itemCount != 1) {
                throw new IllegalArgumentException("Moving more than 1 item is not supported yet");
            }
            if (fromPosition == toPosition) {
                return;
            }
            offsetPositionRecordsForMove(fromPosition, toPosition);
            mItemsChangedForAnimation = true;
            if (mLayout != null) {
                mLayout.onItemsMoved(RecyclerView.this, fromPosition, toPosition, itemCount);
            }
            triggerUpdateProcessor();
        }
    }

    // ------------------------------------------------------------ children and holders

    public ViewHolder getChildViewHolder(View child) {
        if (child.getParent() != this) {
            throw new IllegalArgumentException("View " + child + " is not a direct child of " + this);
        }
        return getChildViewHolderInt(child);
    }

    static ViewHolder getChildViewHolderInt(View child) {
        if (child == null) {
            return null;
        }
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        return lp instanceof LayoutParams ? ((LayoutParams) lp).mViewHolder : null;
    }

    public View findContainingItemView(View view) {
        Object parent = view.getParent();
        while (parent != null && parent != this && parent instanceof View) {
            view = (View) parent;
            parent = view.getParent();
        }
        return parent == this ? view : null;
    }

    public ViewHolder findContainingViewHolder(View view) {
        View itemView = findContainingItemView(view);
        return itemView == null ? null : getChildViewHolder(itemView);
    }

    @Deprecated
    public int getChildPosition(View child) {
        return getChildAdapterPosition(child);
    }

    public int getChildAdapterPosition(View child) {
        ViewHolder holder = getChildViewHolderInt(child);
        return holder != null ? holder.getAbsoluteAdapterPosition() : NO_POSITION;
    }

    public int getChildLayoutPosition(View child) {
        ViewHolder holder = getChildViewHolderInt(child);
        return holder != null ? holder.getLayoutPosition() : NO_POSITION;
    }

    public long getChildItemId(View child) {
        if (mAdapter == null || !mAdapter.hasStableIds()) {
            return NO_ID;
        }
        ViewHolder holder = getChildViewHolderInt(child);
        return holder != null ? holder.getItemId() : NO_ID;
    }

    @Deprecated
    public ViewHolder findViewHolderForPosition(int position) {
        return findViewHolderForPosition(position, false);
    }

    public ViewHolder findViewHolderForLayoutPosition(int position) {
        return findViewHolderForPosition(position, false);
    }

    public ViewHolder findViewHolderForAdapterPosition(int position) {
        if (mDataSetHasChangedAfterLayout) {
            return null;
        }
        ViewHolder hidden = null;
        for (int i = 0; i < getChildCount(); i++) {
            ViewHolder holder = getChildViewHolderInt(getChildAt(i));
            if (holder != null && !holder.isRemoved() && holder.getAbsoluteAdapterPosition() == position) {
                if (isHidden(holder.itemView)) {
                    hidden = holder;
                } else {
                    return holder;
                }
            }
        }
        return hidden;
    }

    ViewHolder findViewHolderForPosition(int position, boolean checkNewPosition) {
        ViewHolder hidden = null;
        for (int i = 0; i < getChildCount(); i++) {
            ViewHolder holder = getChildViewHolderInt(getChildAt(i));
            if (holder != null && !holder.isRemoved()) {
                int p = checkNewPosition ? holder.mPosition : holder.getLayoutPosition();
                if (p == position) {
                    if (isHidden(holder.itemView)) {
                        hidden = holder;
                    } else {
                        return holder;
                    }
                }
            }
        }
        return hidden;
    }

    public ViewHolder findViewHolderForItemId(long id) {
        if (mAdapter == null || !mAdapter.hasStableIds()) {
            return null;
        }
        for (int i = 0; i < getChildCount(); i++) {
            ViewHolder holder = getChildViewHolderInt(getChildAt(i));
            if (holder != null && !holder.isRemoved() && holder.getItemId() == id) {
                return holder;
            }
        }
        return null;
    }

    public View findChildViewUnder(float x, float y) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            float translationX = child.getTranslationX();
            float translationY = child.getTranslationY();
            if (x >= child.getLeft() + translationX && x <= child.getRight() + translationX
                    && y >= child.getTop() + translationY && y <= child.getBottom() + translationY) {
                return child;
            }
        }
        return null;
    }

    public void offsetChildrenVertical(int dy) {
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).offsetTopAndBottom(dy);
        }
    }

    public void offsetChildrenHorizontal(int dx) {
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).offsetLeftAndRight(dx);
        }
    }

    public void getDecoratedBoundsWithMargins(View view, Rect outBounds) {
        getDecoratedBoundsWithMarginsInt(view, outBounds);
    }

    static void getDecoratedBoundsWithMarginsInt(View view, Rect outBounds) {
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        Rect insets = lp.mDecorInsets;
        outBounds.set(view.getLeft() - insets.left - lp.leftMargin,
                view.getTop() - insets.top - lp.topMargin,
                view.getRight() + insets.right + lp.rightMargin,
                view.getBottom() + insets.bottom + lp.bottomMargin);
    }

    Rect getItemDecorInsetsForChild(View child) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (!lp.mInsetsDirty) {
            return lp.mDecorInsets;
        }
        Rect insets = lp.mDecorInsets;
        insets.set(0, 0, 0, 0);
        for (int i = 0; i < mItemDecorations.size(); i++) {
            mTempRect.set(0, 0, 0, 0);
            mItemDecorations.get(i).getItemOffsets(mTempRect, child, this, mState);
            insets.left += mTempRect.left;
            insets.top += mTempRect.top;
            insets.right += mTempRect.right;
            insets.bottom += mTempRect.bottom;
        }
        lp.mInsetsDirty = false;
        return insets;
    }

    // ------------------------------------------------------------ drawing

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        for (int i = 0; i < mItemDecorations.size(); i++) {
            mItemDecorations.get(i).onDraw(c, this, mState);
        }
    }

    /// Runs after the children paint: the decorations that draw over them.
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        for (int i = 0; i < mItemDecorations.size(); i++) {
            mItemDecorations.get(i).onDrawOver(canvas, this, mState);
        }
    }

    // ------------------------------------------------------------ scrolling

    public int getScrollState() {
        return mScrollState;
    }

    void setScrollState(int state) {
        if (state == mScrollState) {
            return;
        }
        mScrollState = state;
        if (state != SCROLL_STATE_SETTLING) {
            mViewFlinger.stop();
            if (mLayout != null) {
                mLayout.stopSmoothScroller();
            }
        }
        dispatchOnScrollStateChanged(state);
    }

    void dispatchOnScrollStateChanged(int state) {
        if (mLayout != null) {
            mLayout.onScrollStateChanged(state);
        }
        onScrollStateChanged(state);
        if (mScrollListener != null) {
            mScrollListener.onScrollStateChanged(this, state);
        }
        if (mScrollListeners != null) {
            for (int i = mScrollListeners.size() - 1; i >= 0; i--) {
                mScrollListeners.get(i).onScrollStateChanged(this, state);
            }
        }
    }

    public void onScrollStateChanged(int state) {
    }

    void dispatchOnScrolled(int hresult, int vresult) {
        onScrolled(hresult, vresult);
        if (mScrollListener != null) {
            mScrollListener.onScrolled(this, hresult, vresult);
        }
        if (mScrollListeners != null) {
            for (int i = mScrollListeners.size() - 1; i >= 0; i--) {
                mScrollListeners.get(i).onScrolled(this, hresult, vresult);
            }
        }
    }

    public void onScrolled(int dx, int dy) {
    }

    @Override
    public void scrollTo(int x, int y) {
        // RecyclerView does not support scrolling to an absolute position.
    }

    @Override
    public void scrollBy(int x, int y) {
        if (mLayout == null || mLayoutSuppressed) {
            return;
        }
        boolean canScrollHorizontal = mLayout.canScrollHorizontally();
        boolean canScrollVertical = mLayout.canScrollVertically();
        if (canScrollHorizontal || canScrollVertical) {
            scrollByInternal(canScrollHorizontal ? x : 0, canScrollVertical ? y : 0);
        }
    }

    /// Scrolls by up to (`x`, `y`); answers what was consumed.
    int[] scrollStep(int dx, int dy) {
        int consumedX = 0;
        int consumedY = 0;
        startInterceptRequestLayout();
        if (dx != 0) {
            consumedX = mLayout.scrollHorizontallyBy(dx, mRecycler, mState);
        }
        if (dy != 0) {
            consumedY = mLayout.scrollVerticallyBy(dy, mRecycler, mState);
        }
        mLayout.removeAndRecycleScrapInt(mRecycler);
        stopInterceptRequestLayout(false);
        return new int[] {consumedX, consumedY};
    }

    boolean scrollByInternal(int x, int y) {
        if (mAdapter == null || mLayout == null) {
            return false;
        }
        int[] consumed = scrollStep(x, y);
        if (consumed[0] != 0 || consumed[1] != 0) {
            dispatchOnScrolled(consumed[0], consumed[1]);
            invalidate();
        }
        return consumed[0] != 0 || consumed[1] != 0;
    }

    public void scrollToPosition(int position) {
        if (mLayoutSuppressed) {
            return;
        }
        stopScroll();
        if (mLayout == null) {
            return;
        }
        mLayout.scrollToPosition(position);
    }

    void jumpToPositionForSmoothScroller(int position) {
        if (mLayout == null) {
            return;
        }
        setScrollState(SCROLL_STATE_SETTLING);
        mLayout.scrollToPosition(position);
    }

    public void smoothScrollToPosition(int position) {
        if (mLayoutSuppressed || mLayout == null) {
            return;
        }
        mLayout.smoothScrollToPosition(this, mState, position);
    }

    public void smoothScrollBy(int dx, int dy) {
        smoothScrollBy(dx, dy, null);
    }

    public void smoothScrollBy(int dx, int dy, Interpolator interpolator) {
        smoothScrollBy(dx, dy, interpolator, UNDEFINED_DURATION);
    }

    public void smoothScrollBy(int dx, int dy, Interpolator interpolator, int duration) {
        if (mLayout == null || mLayoutSuppressed) {
            return;
        }
        if (!mLayout.canScrollHorizontally()) {
            dx = 0;
        }
        if (!mLayout.canScrollVertically()) {
            dy = 0;
        }
        if (dx != 0 || dy != 0) {
            if (duration == UNDEFINED_DURATION || duration > 0) {
                mViewFlinger.smoothScrollBy(dx, dy, duration, interpolator);
            } else {
                scrollBy(dx, dy);
            }
        }
    }

    public boolean fling(int velocityX, int velocityY) {
        if (mLayout == null || mLayoutSuppressed) {
            return false;
        }
        boolean canScrollHorizontal = mLayout.canScrollHorizontally();
        boolean canScrollVertical = mLayout.canScrollVertically();
        if (!canScrollHorizontal || Math.abs(velocityX) < mMinFlingVelocity) {
            velocityX = 0;
        }
        if (!canScrollVertical || Math.abs(velocityY) < mMinFlingVelocity) {
            velocityY = 0;
        }
        if (velocityX == 0 && velocityY == 0) {
            return false;
        }
        if (mOnFlingListener != null && mOnFlingListener.onFling(velocityX, velocityY)) {
            return true;
        }
        velocityX = Math.max(-mMaxFlingVelocity, Math.min(velocityX, mMaxFlingVelocity));
        velocityY = Math.max(-mMaxFlingVelocity, Math.min(velocityY, mMaxFlingVelocity));
        mViewFlinger.fling(velocityX, velocityY);
        return true;
    }

    public void stopScroll() {
        setScrollState(SCROLL_STATE_IDLE);
        stopScrollersInternal();
    }

    private void stopScrollersInternal() {
        mViewFlinger.stop();
        if (mLayout != null) {
            mLayout.stopSmoothScroller();
        }
    }

    @Override
    public boolean canScrollVertically(int direction) {
        if (mLayout == null || !mLayout.canScrollVertically()) {
            return false;
        }
        return mLayout.canScrollInDirection(direction, mState);
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        if (mLayout == null || !mLayout.canScrollHorizontally()) {
            return false;
        }
        return mLayout.canScrollInDirection(direction, mState);
    }

    @Override
    public int computeVerticalScrollOffset() {
        return mLayout != null && mLayout.canScrollVertically() ? mLayout.computeVerticalScrollOffset(mState) : 0;
    }

    @Override
    public int computeVerticalScrollExtent() {
        return mLayout != null && mLayout.canScrollVertically() ? mLayout.computeVerticalScrollExtent(mState) : 0;
    }

    @Override
    public int computeVerticalScrollRange() {
        return mLayout != null && mLayout.canScrollVertically() ? mLayout.computeVerticalScrollRange(mState) : 0;
    }

    @Override
    public int computeHorizontalScrollOffset() {
        return mLayout != null && mLayout.canScrollHorizontally() ? mLayout.computeHorizontalScrollOffset(mState) : 0;
    }

    @Override
    public int computeHorizontalScrollExtent() {
        return mLayout != null && mLayout.canScrollHorizontally() ? mLayout.computeHorizontalScrollExtent(mState) : 0;
    }

    @Override
    public int computeHorizontalScrollRange() {
        return mLayout != null && mLayout.canScrollHorizontally() ? mLayout.computeHorizontalScrollRange(mState) : 0;
    }

    // ------------------------------------------------------------ touch

    private boolean dispatchToOnItemTouchListeners(MotionEvent e) {
        if (mInterceptingOnItemTouchListener == null) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                return false;
            }
            return findInterceptingOnItemTouchListener(e);
        }
        mInterceptingOnItemTouchListener.onTouchEvent(this, e);
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mInterceptingOnItemTouchListener = null;
        }
        return true;
    }

    private boolean findInterceptingOnItemTouchListener(MotionEvent e) {
        int action = e.getActionMasked();
        for (int i = 0; i < mOnItemTouchListeners.size(); i++) {
            OnItemTouchListener listener = mOnItemTouchListeners.get(i);
            if (listener.onInterceptTouchEvent(this, e) && action != MotionEvent.ACTION_CANCEL) {
                mInterceptingOnItemTouchListener = listener;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (mLayoutSuppressed) {
            return false;
        }
        mInterceptingOnItemTouchListener = null;
        if (findInterceptingOnItemTouchListener(e)) {
            cancelScroll();
            return true;
        }
        if (mLayout == null) {
            return false;
        }
        boolean canScrollHorizontally = mLayout.canScrollHorizontally();
        boolean canScrollVertically = mLayout.canScrollVertically();
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(e);
        int action = e.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mScrollPointerId = e.getPointerId(0);
                mInitialTouchX = mLastTouchX = (int) (e.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);
                if (mScrollState == SCROLL_STATE_SETTLING) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    setScrollState(SCROLL_STATE_DRAGGING);
                    stopScrollersInternal();
                }
                break;
            case MotionEvent.ACTION_MOVE: {
                int x = (int) (e.getX() + 0.5f);
                int y = (int) (e.getY() + 0.5f);
                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    int dx = x - mInitialTouchX;
                    int dy = y - mInitialTouchY;
                    boolean startScroll = false;
                    if (canScrollHorizontally && Math.abs(dx) > mTouchSlop) {
                        mLastTouchX = x;
                        startScroll = true;
                    }
                    if (canScrollVertically && Math.abs(dy) > mTouchSlop) {
                        mLastTouchY = y;
                        startScroll = true;
                    }
                    if (startScroll) {
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
                mVelocityTracker.clear();
                break;
            case MotionEvent.ACTION_CANCEL:
                cancelScroll();
                break;
            default:
                break;
        }
        return mScrollState == SCROLL_STATE_DRAGGING;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        for (int i = 0; i < mOnItemTouchListeners.size(); i++) {
            mOnItemTouchListeners.get(i).onRequestDisallowInterceptTouchEvent(disallowIntercept);
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (mLayoutSuppressed) {
            return false;
        }
        if (dispatchToOnItemTouchListeners(e)) {
            cancelScroll();
            return true;
        }
        if (mLayout == null) {
            return false;
        }
        boolean canScrollHorizontally = mLayout.canScrollHorizontally();
        boolean canScrollVertically = mLayout.canScrollVertically();
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        boolean eventAddedToVelocityTracker = false;
        int action = e.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mScrollPointerId = e.getPointerId(0);
                mInitialTouchX = mLastTouchX = (int) (e.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);
                break;
            case MotionEvent.ACTION_MOVE: {
                int x = (int) (e.getX() + 0.5f);
                int y = (int) (e.getY() + 0.5f);
                int dx = mLastTouchX - x;
                int dy = mLastTouchY - y;
                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    boolean startScroll = false;
                    if (canScrollHorizontally) {
                        if (dx > 0) {
                            dx = Math.max(0, dx - mTouchSlop);
                        } else {
                            dx = Math.min(0, dx + mTouchSlop);
                        }
                        if (dx != 0) {
                            startScroll = true;
                        }
                    }
                    if (canScrollVertically) {
                        if (dy > 0) {
                            dy = Math.max(0, dy - mTouchSlop);
                        } else {
                            dy = Math.min(0, dy + mTouchSlop);
                        }
                        if (dy != 0) {
                            startScroll = true;
                        }
                    }
                    if (startScroll) {
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }
                if (mScrollState == SCROLL_STATE_DRAGGING) {
                    mLastTouchX = x;
                    mLastTouchY = y;
                    if (scrollByInternal(canScrollHorizontally ? dx : 0, canScrollVertically ? dy : 0)) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                mVelocityTracker.addMovement(e);
                eventAddedToVelocityTracker = true;
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                float xvel = canScrollHorizontally ? -mVelocityTracker.getXVelocity(mScrollPointerId) : 0;
                float yvel = canScrollVertically ? -mVelocityTracker.getYVelocity(mScrollPointerId) : 0;
                if (!((xvel != 0 || yvel != 0) && fling((int) xvel, (int) yvel))) {
                    setScrollState(SCROLL_STATE_IDLE);
                }
                resetScroll();
                break;
            }
            case MotionEvent.ACTION_CANCEL:
                cancelScroll();
                break;
            default:
                break;
        }
        if (!eventAddedToVelocityTracker) {
            mVelocityTracker.addMovement(e);
        }
        return true;
    }

    private void resetScroll() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
    }

    private void cancelScroll() {
        resetScroll();
        setScrollState(SCROLL_STATE_IDLE);
    }

    // ------------------------------------------------------------ saved state

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        if (mLayout != null) {
            state.mLayoutState = mLayout.onSaveInstanceState();
        }
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState saved = (SavedState) state;
        super.onRestoreInstanceState(saved.mSuperState);
        if (mLayout != null && saved.mLayoutState != null) {
            mLayout.onRestoreInstanceState(saved.mLayoutState);
        }
    }

    static final class SavedState implements Parcelable {
        final Parcelable mSuperState;
        Parcelable mLayoutState;

        SavedState(Parcelable superState) {
            mSuperState = superState;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
        }
    }

    // ------------------------------------------------------------ the scroller

    /// Runs flings and smooth scrolls one frame at a time, driving the
    /// layout manager's smooth scroller along the way.
    final class ViewFlinger implements ScrollAnimator.Step {
        private OverScroller mOverScroller;
        private Interpolator mInterpolator = QUINTIC_INTERPOLATOR;
        private int mLastFlingX;
        private int mLastFlingY;
        private ScrollAnimator mAnimator;
        private boolean mFling;

        ViewFlinger() {
        }

        private OverScroller scroller(Interpolator interpolator) {
            if (mOverScroller == null || mInterpolator != interpolator) {
                mInterpolator = interpolator;
                mOverScroller = new OverScroller(getContext(), interpolator);
            }
            return mOverScroller;
        }

        private ScrollAnimator animator() {
            if (mAnimator == null) {
                mAnimator = new ScrollAnimator(RecyclerView.this, this);
            }
            return mAnimator;
        }

        void fling(int velocityX, int velocityY) {
            setScrollState(SCROLL_STATE_SETTLING);
            mLastFlingX = 0;
            mLastFlingY = 0;
            mFling = true;
            OverScroller s = scroller(QUINTIC_INTERPOLATOR);
            s.fling(0, 0, velocityX, velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE,
                    Integer.MIN_VALUE, Integer.MAX_VALUE);
            animator().start();
        }

        void smoothScrollBy(int dx, int dy, int duration, Interpolator interpolator) {
            if (duration == UNDEFINED_DURATION) {
                duration = computeScrollDuration(dx, dy);
            }
            if (interpolator == null) {
                interpolator = QUINTIC_INTERPOLATOR;
            }
            OverScroller s = scroller(interpolator);
            setScrollState(SCROLL_STATE_SETTLING);
            mLastFlingX = 0;
            mLastFlingY = 0;
            mFling = false;
            s.startScroll(0, 0, dx, dy, duration);
            animator().start();
        }

        /// Keeps the frame loop going while only the smooth scroller has work.
        void postOnAnimation() {
            animator().start();
        }

        private int computeScrollDuration(int dx, int dy) {
            int absDx = Math.abs(dx);
            int absDy = Math.abs(dy);
            boolean horizontal = absDx > absDy;
            int containerSize = horizontal ? getWidth() : getHeight();
            float absDelta = horizontal ? absDx : absDy;
            int duration = (int) (((absDelta / Math.max(1, containerSize)) + 1) * 300);
            return Math.min(duration, MAX_SCROLL_DURATION);
        }

        void stop() {
            if (mAnimator != null) {
                mAnimator.stop();
            }
            if (mOverScroller != null) {
                mOverScroller.abortAnimation();
            }
        }

        @Override
        public boolean step() {
            if (mLayout == null) {
                return false;
            }
            OverScroller scroller = mOverScroller;
            int consumedX = 0;
            int consumedY = 0;
            boolean scrollerRunning = scroller != null && scroller.computeScrollOffset();
            if (scrollerRunning) {
                int x = scroller.getCurrX();
                int y = scroller.getCurrY();
                mState.mRemainingScrollHorizontal = scroller.getFinalX() - x;
                mState.mRemainingScrollVertical = scroller.getFinalY() - y;
                int unconsumedX = x - mLastFlingX;
                int unconsumedY = y - mLastFlingY;
                mLastFlingX = x;
                mLastFlingY = y;
                if (mAdapter != null && (unconsumedX != 0 || unconsumedY != 0)) {
                    int[] c = scrollStep(unconsumedX, unconsumedY);
                    consumedX = c[0];
                    consumedY = c[1];
                    if (consumedX != 0 || consumedY != 0) {
                        dispatchOnScrolled(consumedX, consumedY);
                        invalidate();
                    }
                    boolean hitEdge = (unconsumedX != 0 && consumedX == 0) || (unconsumedY != 0 && consumedY == 0);
                    if (hitEdge && mFling) {
                        scroller.abortAnimation();
                        scrollerRunning = false;
                    }
                }
            }
            SmoothScroller smoothScroller = mLayout.mSmoothScroller;
            if (smoothScroller != null && smoothScroller.isRunning()) {
                smoothScroller.onAnimation(consumedX, consumedY);
            }
            boolean smoothRunning = smoothScroller != null && smoothScroller.isRunning();
            boolean more = (scroller != null && !scroller.isFinished()) || smoothRunning;
            if (!more) {
                setScrollState(SCROLL_STATE_IDLE);
            }
            return more;
        }
    }

    // ------------------------------------------------------------ Adapter

    /// Supplies the views for the items a [RecyclerView] shows.
    public abstract static class Adapter<VH extends ViewHolder> {

        /// Whether a [RecyclerView] restores its scroll position when the
        /// adapter has no data yet.
        public enum StateRestorationPolicy {
            ALLOW, PREVENT_WHEN_EMPTY, PREVENT
        }

        private final AdapterDataObservable mObservable = new AdapterDataObservable();
        private boolean mHasStableIds;
        private StateRestorationPolicy mStateRestorationPolicy = StateRestorationPolicy.ALLOW;

        public abstract VH onCreateViewHolder(ViewGroup parent, int viewType);

        public abstract void onBindViewHolder(VH holder, int position);

        public void onBindViewHolder(VH holder, int position, List<Object> payloads) {
            onBindViewHolder(holder, position);
        }

        public final VH createViewHolder(ViewGroup parent, int viewType) {
            VH holder = onCreateViewHolder(parent, viewType);
            if (holder.itemView.getParent() != null) {
                throw new IllegalStateException("ViewHolder views must not be attached when created. "
                        + "Ensure that you are not passing 'true' to the attachToRoot parameter of "
                        + "LayoutInflater.inflate(..., boolean attachToRoot)");
            }
            holder.mItemViewType = viewType;
            return holder;
        }

        @SuppressWarnings("unchecked")
        public final void bindViewHolder(VH holder, int position) {
            boolean rootBind = holder.mBindingAdapter == null;
            if (rootBind) {
                holder.mPosition = position;
                if (hasStableIds()) {
                    holder.mItemId = getItemId(position);
                }
                holder.setFlags(ViewHolder.FLAG_BOUND,
                        ViewHolder.FLAG_BOUND | ViewHolder.FLAG_UPDATE | ViewHolder.FLAG_INVALID
                                | ViewHolder.FLAG_ADAPTER_POSITION_UNKNOWN);
            }
            holder.mBindingAdapter = this;
            onBindViewHolder(holder, position, holder.getUnmodifiedPayloads());
            holder.clearPayload();
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            if (lp instanceof LayoutParams) {
                ((LayoutParams) lp).mInsetsDirty = true;
            }
        }

        public int getItemViewType(int position) {
            return 0;
        }

        public void setHasStableIds(boolean hasStableIds) {
            if (hasObservers()) {
                throw new IllegalStateException("Cannot change whether this adapter has "
                        + "stable IDs while the adapter has registered observers.");
            }
            mHasStableIds = hasStableIds;
        }

        public long getItemId(int position) {
            return NO_ID;
        }

        public abstract int getItemCount();

        public final boolean hasStableIds() {
            return mHasStableIds;
        }

        public void onViewRecycled(VH holder) {
        }

        public boolean onFailedToRecycleView(VH holder) {
            return false;
        }

        public void onViewAttachedToWindow(VH holder) {
        }

        public void onViewDetachedFromWindow(VH holder) {
        }

        public final boolean hasObservers() {
            return mObservable.hasObservers();
        }

        public void registerAdapterDataObserver(AdapterDataObserver observer) {
            mObservable.registerObserver(observer);
        }

        public void unregisterAdapterDataObserver(AdapterDataObserver observer) {
            mObservable.unregisterObserver(observer);
        }

        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        }

        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        }

        public final void notifyDataSetChanged() {
            mObservable.notifyChanged();
        }

        public final void notifyItemChanged(int position) {
            mObservable.notifyItemRangeChanged(position, 1, null);
        }

        public final void notifyItemChanged(int position, Object payload) {
            mObservable.notifyItemRangeChanged(position, 1, payload);
        }

        public final void notifyItemRangeChanged(int positionStart, int itemCount) {
            mObservable.notifyItemRangeChanged(positionStart, itemCount, null);
        }

        public final void notifyItemRangeChanged(int positionStart, int itemCount, Object payload) {
            mObservable.notifyItemRangeChanged(positionStart, itemCount, payload);
        }

        public final void notifyItemInserted(int position) {
            mObservable.notifyItemRangeInserted(position, 1);
        }

        public final void notifyItemMoved(int fromPosition, int toPosition) {
            mObservable.notifyItemMoved(fromPosition, toPosition);
        }

        public final void notifyItemRangeInserted(int positionStart, int itemCount) {
            mObservable.notifyItemRangeInserted(positionStart, itemCount);
        }

        public final void notifyItemRemoved(int position) {
            mObservable.notifyItemRangeRemoved(position, 1);
        }

        public final void notifyItemRangeRemoved(int positionStart, int itemCount) {
            mObservable.notifyItemRangeRemoved(positionStart, itemCount);
        }

        public void setStateRestorationPolicy(StateRestorationPolicy strategy) {
            mStateRestorationPolicy = strategy;
            mObservable.notifyStateRestorationPolicyChanged();
        }

        public final StateRestorationPolicy getStateRestorationPolicy() {
            return mStateRestorationPolicy;
        }

        public int findRelativeAdapterPositionIn(Adapter<? extends ViewHolder> adapter, ViewHolder viewHolder,
                                                 int localPosition) {
            return adapter == this ? localPosition : NO_POSITION;
        }
    }

    static final class AdapterDataObservable {
        private final ArrayList<AdapterDataObserver> mObservers = new ArrayList<AdapterDataObserver>();

        void registerObserver(AdapterDataObserver observer) {
            if (observer == null) {
                throw new IllegalArgumentException("The observer is null.");
            }
            if (mObservers.contains(observer)) {
                throw new IllegalStateException("Observer " + observer + " is already registered.");
            }
            mObservers.add(observer);
        }

        void unregisterObserver(AdapterDataObserver observer) {
            if (observer == null) {
                throw new IllegalArgumentException("The observer is null.");
            }
            int index = mObservers.indexOf(observer);
            if (index == -1) {
                throw new IllegalStateException("Observer " + observer + " was not registered.");
            }
            mObservers.remove(index);
        }

        boolean hasObservers() {
            return !mObservers.isEmpty();
        }

        void notifyChanged() {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChanged();
            }
        }

        void notifyStateRestorationPolicyChanged() {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onStateRestorationPolicyChanged();
            }
        }

        void notifyItemRangeChanged(int positionStart, int itemCount, Object payload) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onItemRangeChanged(positionStart, itemCount, payload);
            }
        }

        void notifyItemRangeInserted(int positionStart, int itemCount) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onItemRangeInserted(positionStart, itemCount);
            }
        }

        void notifyItemRangeRemoved(int positionStart, int itemCount) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onItemRangeRemoved(positionStart, itemCount);
            }
        }

        void notifyItemMoved(int fromPosition, int toPosition) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onItemRangeMoved(fromPosition, toPosition, 1);
            }
        }
    }

    /// Observes an adapter's data changes.
    public abstract static class AdapterDataObserver {
        public void onChanged() {
        }

        public void onItemRangeChanged(int positionStart, int itemCount) {
        }

        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            onItemRangeChanged(positionStart, itemCount);
        }

        public void onItemRangeInserted(int positionStart, int itemCount) {
        }

        public void onItemRangeRemoved(int positionStart, int itemCount) {
        }

        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
        }

        public void onStateRestorationPolicyChanged() {
        }
    }

    // ------------------------------------------------------------ ViewHolder

    /// An item view and what [RecyclerView] knows about its place in the
    /// adapter.
    public abstract static class ViewHolder {
        static final int FLAG_BOUND = 1 << 0;
        static final int FLAG_UPDATE = 1 << 1;
        static final int FLAG_INVALID = 1 << 2;
        static final int FLAG_REMOVED = 1 << 3;
        static final int FLAG_NOT_RECYCLABLE = 1 << 4;
        static final int FLAG_RETURNED_FROM_SCRAP = 1 << 5;
        static final int FLAG_IGNORE = 1 << 7;
        static final int FLAG_TMP_DETACHED = 1 << 8;
        static final int FLAG_ADAPTER_POSITION_UNKNOWN = 1 << 9;
        static final int FLAG_ADAPTER_FULLUPDATE = 1 << 10;
        static final int FLAG_MOVED = 1 << 11;

        private static final List<Object> FULLUPDATE_PAYLOADS = new ArrayList<Object>(0);

        public final View itemView;
        int mPosition = NO_POSITION;
        int mOldPosition = NO_POSITION;
        long mItemId = NO_ID;
        int mItemViewType = INVALID_TYPE;
        int mPreLayoutPosition = NO_POSITION;
        int mFlags;
        List<Object> mPayloads;
        private int mIsRecyclableCount;
        Recycler mScrapContainer;
        RecyclerView mOwnerRecyclerView;
        Adapter<? extends ViewHolder> mBindingAdapter;
        boolean mAnimationChanged;

        public ViewHolder(View itemView) {
            if (itemView == null) {
                throw new IllegalArgumentException("itemView may not be null");
            }
            this.itemView = itemView;
        }

        void offsetPosition(int offset) {
            saveOldPosition();
            mPosition += offset;
            ViewGroup.LayoutParams lp = itemView.getLayoutParams();
            if (lp instanceof LayoutParams) {
                ((LayoutParams) lp).mInsetsDirty = true;
            }
        }

        void saveOldPosition() {
            if (mOldPosition == NO_POSITION) {
                mOldPosition = mPosition;
            }
        }

        void clearOldPosition() {
            mOldPosition = NO_POSITION;
            mPreLayoutPosition = NO_POSITION;
        }

        boolean shouldIgnore() {
            return (mFlags & FLAG_IGNORE) != 0;
        }

        @Deprecated
        public final int getPosition() {
            return mPreLayoutPosition == NO_POSITION ? mPosition : mPreLayoutPosition;
        }

        public final int getLayoutPosition() {
            return mPreLayoutPosition == NO_POSITION ? mPosition : mPreLayoutPosition;
        }

        @Deprecated
        public final int getAdapterPosition() {
            return getBindingAdapterPosition();
        }

        public final int getBindingAdapterPosition() {
            if (mBindingAdapter == null || mOwnerRecyclerView == null) {
                return NO_POSITION;
            }
            return getAbsoluteAdapterPosition();
        }

        public final int getAbsoluteAdapterPosition() {
            if (mOwnerRecyclerView == null
                    || (mFlags & (FLAG_INVALID | FLAG_REMOVED | FLAG_ADAPTER_POSITION_UNKNOWN)) != 0) {
                return NO_POSITION;
            }
            return mPosition;
        }

        public final Adapter<? extends ViewHolder> getBindingAdapter() {
            return mBindingAdapter;
        }

        public final int getOldPosition() {
            return mOldPosition;
        }

        public final long getItemId() {
            return mItemId;
        }

        public final int getItemViewType() {
            return mItemViewType;
        }

        boolean isScrap() {
            return mScrapContainer != null;
        }

        void unScrap() {
            if (mScrapContainer != null) {
                mScrapContainer.unscrapView(this);
            }
        }

        boolean wasReturnedFromScrap() {
            return (mFlags & FLAG_RETURNED_FROM_SCRAP) != 0;
        }

        void clearReturnedFromScrapFlag() {
            mFlags = mFlags & ~FLAG_RETURNED_FROM_SCRAP;
        }

        void clearTmpDetachFlag() {
            mFlags = mFlags & ~FLAG_TMP_DETACHED;
        }

        void setScrapContainer(Recycler recycler) {
            mScrapContainer = recycler;
        }

        boolean isInvalid() {
            return (mFlags & FLAG_INVALID) != 0;
        }

        boolean needsUpdate() {
            return (mFlags & FLAG_UPDATE) != 0;
        }

        boolean isBound() {
            return (mFlags & FLAG_BOUND) != 0;
        }

        boolean isRemoved() {
            return (mFlags & FLAG_REMOVED) != 0;
        }

        boolean hasAnyOfTheFlags(int flags) {
            return (mFlags & flags) != 0;
        }

        boolean isTmpDetached() {
            return (mFlags & FLAG_TMP_DETACHED) != 0;
        }

        boolean isAttachedToTransitionOverlay() {
            return false;
        }

        boolean isAdapterPositionUnknown() {
            return (mFlags & FLAG_ADAPTER_POSITION_UNKNOWN) != 0 || isInvalid();
        }

        void setFlags(int flags, int mask) {
            mFlags = (mFlags & ~mask) | (flags & mask);
        }

        void addFlags(int flags) {
            mFlags |= flags;
        }

        void addChangePayload(Object payload) {
            if (payload == null) {
                addFlags(FLAG_ADAPTER_FULLUPDATE);
            } else if ((mFlags & FLAG_ADAPTER_FULLUPDATE) == 0) {
                if (mPayloads == null) {
                    mPayloads = new ArrayList<Object>();
                }
                mPayloads.add(payload);
            }
        }

        void clearPayload() {
            if (mPayloads != null) {
                mPayloads.clear();
            }
            mFlags = mFlags & ~FLAG_ADAPTER_FULLUPDATE;
        }

        List<Object> getUnmodifiedPayloads() {
            if ((mFlags & FLAG_ADAPTER_FULLUPDATE) == 0) {
                if (mPayloads == null || mPayloads.isEmpty()) {
                    return FULLUPDATE_PAYLOADS;
                }
                return new UnmodifiableList<Object>(mPayloads);
            }
            return FULLUPDATE_PAYLOADS;
        }

        void resetInternal() {
            mFlags = 0;
            mPosition = NO_POSITION;
            mOldPosition = NO_POSITION;
            mItemId = NO_ID;
            mPreLayoutPosition = NO_POSITION;
            mIsRecyclableCount = 0;
            mAnimationChanged = false;
            clearPayload();
        }

        public final void setIsRecyclable(boolean recyclable) {
            mIsRecyclableCount = recyclable ? mIsRecyclableCount - 1 : mIsRecyclableCount + 1;
            if (mIsRecyclableCount < 0) {
                mIsRecyclableCount = 0;
            } else if (!recyclable && mIsRecyclableCount == 1) {
                mFlags |= FLAG_NOT_RECYCLABLE;
            } else if (recyclable && mIsRecyclableCount == 0) {
                mFlags &= ~FLAG_NOT_RECYCLABLE;
            }
        }

        public final boolean isRecyclable() {
            return (mFlags & FLAG_NOT_RECYCLABLE) == 0;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("ViewHolder{" + Integer.toHexString(hashCode())
                    + " position=" + mPosition + " id=" + mItemId + ", oldPos=" + mOldPosition
                    + ", pLpos:" + mPreLayoutPosition);
            if (isScrap()) {
                sb.append(" scrap");
            }
            if (isInvalid()) {
                sb.append(" invalid");
            }
            if (!isBound()) {
                sb.append(" unbound");
            }
            if (needsUpdate()) {
                sb.append(" update");
            }
            if (isRemoved()) {
                sb.append(" removed");
            }
            if (shouldIgnore()) {
                sb.append(" ignored");
            }
            if (!isRecyclable()) {
                sb.append(" not recyclable(").append(mIsRecyclableCount).append(")");
            }
            sb.append("}");
            return sb.toString();
        }
    }

    /// A read-only view of a list; Codename One's runtime has no
    /// `Collections.unmodifiableList`.
    static final class UnmodifiableList<T> extends AbstractList<T> {
        private final List<T> mList;

        UnmodifiableList(List<T> list) {
            mList = list;
        }

        @Override
        public T get(int index) {
            return mList.get(index);
        }

        @Override
        public int size() {
            return mList.size();
        }
    }

    // ------------------------------------------------------------ LayoutParams

    /// The layout parameters of a [RecyclerView]'s children: margins, plus
    /// the item's view holder and decoration insets.
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        ViewHolder mViewHolder;
        final Rect mDecorInsets = new Rect();
        boolean mInsetsDirty = true;
        boolean mPendingInvalidate;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super((MarginLayoutParams) source);
        }

        public boolean viewNeedsUpdate() {
            return mViewHolder.needsUpdate();
        }

        public boolean isViewInvalid() {
            return mViewHolder.isInvalid();
        }

        public boolean isItemRemoved() {
            return mViewHolder.isRemoved();
        }

        public boolean isItemChanged() {
            return mViewHolder.needsUpdate() && !mViewHolder.isInvalid();
        }

        @Deprecated
        public int getViewPosition() {
            return mViewHolder.getPosition();
        }

        public int getViewLayoutPosition() {
            return mViewHolder.getLayoutPosition();
        }

        @Deprecated
        public int getViewAdapterPosition() {
            return mViewHolder.getBindingAdapterPosition();
        }

        public int getAbsoluteAdapterPosition() {
            return mViewHolder.getAbsoluteAdapterPosition();
        }

        public int getBindingAdapterPosition() {
            return mViewHolder.getBindingAdapterPosition();
        }
    }

    // ------------------------------------------------------------ Recycler

    /// Hands out views for adapter positions, reusing the ones it can: the
    /// views detached for a layout pass first, then a small cache of views
    /// that keep their binding, then the shared pool, which rebinds.
    public final class Recycler {
        final ArrayList<ViewHolder> mAttachedScrap = new ArrayList<ViewHolder>();
        final ArrayList<ViewHolder> mCachedViews = new ArrayList<ViewHolder>();
        private int mRequestedCacheMax = DEFAULT_CACHE_SIZE;
        int mViewCacheMax = DEFAULT_CACHE_SIZE;
        RecycledViewPool mRecyclerPool;
        ViewCacheExtension mViewCacheExtension;
        static final int DEFAULT_CACHE_SIZE = 2;

        public void clear() {
            mAttachedScrap.clear();
            recycleAndClearCachedViews();
        }

        public void setViewCacheSize(int viewCount) {
            mRequestedCacheMax = viewCount;
            updateViewCacheSize();
        }

        void updateViewCacheSize() {
            mViewCacheMax = mRequestedCacheMax;
            for (int i = mCachedViews.size() - 1; i >= 0 && mCachedViews.size() > mViewCacheMax; i--) {
                recycleCachedViewAt(i);
            }
        }

        public List<ViewHolder> getScrapList() {
            return new UnmodifiableList<ViewHolder>(mAttachedScrap);
        }

        public int convertPreLayoutPositionToPostLayout(int position) {
            if (position < 0 || position >= mState.getItemCount()) {
                throw new IndexOutOfBoundsException("invalid position " + position + ". State "
                        + "item count is " + mState.getItemCount() + exceptionLabel());
            }
            return position;
        }

        public View getViewForPosition(int position) {
            return getViewForPosition(position, false);
        }

        View getViewForPosition(int position, boolean dryRun) {
            return tryGetViewHolderForPosition(position).itemView;
        }

        @SuppressWarnings("unchecked")
        ViewHolder tryGetViewHolderForPosition(int position) {
            int itemCount = mState.getItemCount();
            if (position < 0 || position >= itemCount) {
                throw new IndexOutOfBoundsException("Invalid item position " + position + "(" + position
                        + "). Item count:" + itemCount + exceptionLabel());
            }
            ViewHolder holder = null;
            boolean fromScrapOrCache = false;
            for (int i = 0; i < mAttachedScrap.size(); i++) {
                ViewHolder h = mAttachedScrap.get(i);
                if (!h.wasReturnedFromScrap() && h.getLayoutPosition() == position && !h.isInvalid() && !h.isRemoved()) {
                    h.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP);
                    holder = h;
                    fromScrapOrCache = true;
                    break;
                }
            }
            if (holder == null) {
                for (int i = 0; i < mCachedViews.size(); i++) {
                    ViewHolder h = mCachedViews.get(i);
                    if (!h.isInvalid() && h.getLayoutPosition() == position) {
                        mCachedViews.remove(i);
                        holder = h;
                        fromScrapOrCache = true;
                        break;
                    }
                }
            }
            int type = mAdapter.getItemViewType(position);
            if (holder != null && holder.getItemViewType() != type) {
                // The item's type changed under the cached view: it cannot be reused for it.
                if (holder.isScrap()) {
                    holder.clearReturnedFromScrapFlag();
                } else {
                    recycleViewHolderInternal(holder);
                }
                holder = null;
                fromScrapOrCache = false;
            }
            if (holder == null && mAdapter.hasStableIds()) {
                holder = getScrapOrCachedViewForId(mAdapter.getItemId(position), type);
                if (holder != null) {
                    holder.mPosition = position;
                    fromScrapOrCache = true;
                }
            }
            if (holder == null && mViewCacheExtension != null) {
                View view = mViewCacheExtension.getViewForPositionAndType(this, position, type);
                if (view != null) {
                    holder = getChildViewHolder(view);
                    if (holder == null) {
                        throw new IllegalArgumentException("getViewForPositionAndType returned"
                                + " a view which does not have a ViewHolder" + exceptionLabel());
                    }
                }
            }
            if (holder == null) {
                holder = getRecycledViewPool().getRecycledView(type);
                if (holder != null) {
                    holder.resetInternal();
                }
            }
            if (holder == null) {
                holder = mAdapter.createViewHolder(RecyclerView.this, type);
            }
            boolean bound = false;
            if (!holder.isBound() || holder.needsUpdate() || holder.isInvalid()) {
                holder.mOwnerRecyclerView = RecyclerView.this;
                holder.mBindingAdapter = null;
                mAdapter.bindViewHolder(holder, position);
                bound = true;
            }
            holder.mOwnerRecyclerView = RecyclerView.this;
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            LayoutParams rvLayoutParams;
            if (lp == null) {
                rvLayoutParams = (LayoutParams) generateDefaultLayoutParams();
                holder.itemView.setLayoutParams(rvLayoutParams);
            } else if (!checkLayoutParams(lp)) {
                rvLayoutParams = (LayoutParams) generateLayoutParams(lp);
                holder.itemView.setLayoutParams(rvLayoutParams);
            } else {
                rvLayoutParams = (LayoutParams) lp;
            }
            rvLayoutParams.mViewHolder = holder;
            rvLayoutParams.mPendingInvalidate = fromScrapOrCache && bound;
            return holder;
        }

        private ViewHolder getScrapOrCachedViewForId(long id, int type) {
            for (int i = mAttachedScrap.size() - 1; i >= 0; i--) {
                ViewHolder h = mAttachedScrap.get(i);
                if (h.getItemId() == id && !h.wasReturnedFromScrap() && h.getItemViewType() == type && !h.isRemoved()) {
                    h.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP);
                    return h;
                }
            }
            for (int i = mCachedViews.size() - 1; i >= 0; i--) {
                ViewHolder h = mCachedViews.get(i);
                if (h.getItemId() == id && h.getItemViewType() == type) {
                    mCachedViews.remove(i);
                    return h;
                }
            }
            return null;
        }

        public void bindViewToPosition(View view, int position) {
            ViewHolder holder = getChildViewHolderInt(view);
            if (holder == null) {
                throw new IllegalArgumentException("The view does not have a ViewHolder. You cannot"
                        + " pass arbitrary views to this method, they should be created by the "
                        + "Adapter" + exceptionLabel());
            }
            holder.mOwnerRecyclerView = RecyclerView.this;
            holder.mBindingAdapter = null;
            mAdapter.bindViewHolder(holder, position);
        }

        public void recycleView(View view) {
            ViewHolder holder = getChildViewHolderInt(view);
            if (holder == null) {
                return;
            }
            if (holder.isTmpDetached()) {
                removeDetachedView(view, false);
                holder.clearTmpDetachFlag();
            }
            if (holder.isScrap()) {
                holder.unScrap();
            } else if (holder.wasReturnedFromScrap()) {
                holder.clearReturnedFromScrapFlag();
            }
            recycleViewHolderInternal(holder);
        }

        void recycleViewHolderInternal(ViewHolder holder) {
            if (holder.isScrap() || holder.itemView.getParent() != null) {
                throw new IllegalArgumentException("Scrapped or attached views may not be recycled. isScrap:"
                        + holder.isScrap() + " isAttached:" + (holder.itemView.getParent() != null) + exceptionLabel());
            }
            if (holder.shouldIgnore()) {
                return;
            }
            @SuppressWarnings("unchecked")
            boolean forceRecycle = !holder.isRecyclable() && mAdapter != null && mAdapter.onFailedToRecycleView(holder);
            if (forceRecycle || holder.isRecyclable()) {
                boolean cached = false;
                if (mViewCacheMax > 0 && !holder.hasAnyOfTheFlags(ViewHolder.FLAG_INVALID | ViewHolder.FLAG_REMOVED
                        | ViewHolder.FLAG_UPDATE | ViewHolder.FLAG_ADAPTER_POSITION_UNKNOWN)) {
                    if (mCachedViews.size() >= mViewCacheMax && !mCachedViews.isEmpty()) {
                        recycleCachedViewAt(0);
                    }
                    mCachedViews.add(holder);
                    cached = true;
                }
                if (!cached) {
                    addViewHolderToRecycledViewPool(holder, true);
                }
            }
        }

        @SuppressWarnings("unchecked")
        void addViewHolderToRecycledViewPool(ViewHolder holder, boolean dispatchRecycled) {
            if (dispatchRecycled) {
                if (mRecyclerListener != null) {
                    mRecyclerListener.onViewRecycled(holder);
                }
                for (int i = 0; i < mRecyclerListeners.size(); i++) {
                    mRecyclerListeners.get(i).onViewRecycled(holder);
                }
                if (mAdapter != null) {
                    mAdapter.onViewRecycled(holder);
                }
            }
            holder.mBindingAdapter = null;
            holder.mOwnerRecyclerView = null;
            getRecycledViewPool().putRecycledView(holder);
        }

        void recycleCachedViewAt(int cachedViewIndex) {
            ViewHolder holder = mCachedViews.remove(cachedViewIndex);
            addViewHolderToRecycledViewPool(holder, true);
        }

        void recycleAndClearCachedViews() {
            for (int i = mCachedViews.size() - 1; i >= 0; i--) {
                recycleCachedViewAt(i);
            }
            mCachedViews.clear();
        }

        void recycleRemovedCachedViews() {
            for (int i = mCachedViews.size() - 1; i >= 0; i--) {
                if (mCachedViews.get(i).isRemoved()) {
                    recycleCachedViewAt(i);
                }
            }
        }

        void viewRangeUpdate(int positionStart, int itemCount) {
            int end = positionStart + itemCount;
            for (int i = mCachedViews.size() - 1; i >= 0; i--) {
                ViewHolder holder = mCachedViews.get(i);
                int pos = holder.mPosition;
                if (pos >= positionStart && pos < end) {
                    holder.addFlags(ViewHolder.FLAG_UPDATE);
                    recycleCachedViewAt(i);
                }
            }
        }

        void markKnownViewsInvalid() {
            for (int i = 0; i < mCachedViews.size(); i++) {
                ViewHolder holder = mCachedViews.get(i);
                holder.addFlags(ViewHolder.FLAG_UPDATE | ViewHolder.FLAG_INVALID);
                holder.addChangePayload(null);
            }
            for (int i = 0; i < mAttachedScrap.size(); i++) {
                mAttachedScrap.get(i).addFlags(ViewHolder.FLAG_UPDATE | ViewHolder.FLAG_INVALID);
            }
            if (mAdapter == null || !mAdapter.hasStableIds()) {
                recycleAndClearCachedViews();
            }
        }

        void markItemDecorInsetsDirty() {
            for (int i = 0; i < mCachedViews.size(); i++) {
                ViewGroup.LayoutParams lp = mCachedViews.get(i).itemView.getLayoutParams();
                if (lp instanceof LayoutParams) {
                    ((LayoutParams) lp).mInsetsDirty = true;
                }
            }
        }

        void clearOldPositions() {
            for (int i = 0; i < mCachedViews.size(); i++) {
                mCachedViews.get(i).clearOldPosition();
            }
            for (int i = 0; i < mAttachedScrap.size(); i++) {
                mAttachedScrap.get(i).clearOldPosition();
            }
        }

        void scrapView(View view) {
            ViewHolder holder = getChildViewHolderInt(view);
            holder.setScrapContainer(this);
            mAttachedScrap.add(holder);
        }

        void unscrapView(ViewHolder holder) {
            mAttachedScrap.remove(holder);
            holder.mScrapContainer = null;
            holder.clearReturnedFromScrapFlag();
        }

        int getScrapCount() {
            return mAttachedScrap.size();
        }

        View getScrapViewAt(int index) {
            return mAttachedScrap.get(index).itemView;
        }

        void clearScrap() {
            mAttachedScrap.clear();
        }

        void quickRecycleScrapView(View view) {
            ViewHolder holder = getChildViewHolderInt(view);
            holder.mScrapContainer = null;
            holder.clearReturnedFromScrapFlag();
            recycleViewHolderInternal(holder);
        }

        RecycledViewPool getRecycledViewPool() {
            if (mRecyclerPool == null) {
                mRecyclerPool = new RecycledViewPool();
            }
            return mRecyclerPool;
        }

        void setRecycledViewPool(RecycledViewPool pool) {
            if (mRecyclerPool != null) {
                mRecyclerPool.detach();
            }
            mRecyclerPool = pool;
            if (mRecyclerPool != null && getAdapter() != null) {
                mRecyclerPool.attach();
            }
        }

        void onAdapterChanged(Adapter oldAdapter, Adapter newAdapter, boolean compatibleWithPrevious) {
            clear();
            getRecycledViewPool().onAdapterChanged(oldAdapter, newAdapter, compatibleWithPrevious);
        }
    }

    // ------------------------------------------------------------ RecycledViewPool

    /// Views put aside for reuse, by view type; can be shared between
    /// RecyclerViews whose adapters use the same view types.
    public static class RecycledViewPool {
        private static final int DEFAULT_MAX_SCRAP = 5;

        static class ScrapData {
            final ArrayList<ViewHolder> mScrapHeap = new ArrayList<ViewHolder>();
            int mMaxScrap = DEFAULT_MAX_SCRAP;
        }

        SparseArray<ScrapData> mScrap = new SparseArray<ScrapData>();
        private int mAttachCount;

        public void clear() {
            for (int i = 0; i < mScrap.size(); i++) {
                mScrap.valueAt(i).mScrapHeap.clear();
            }
        }

        public void setMaxRecycledViews(int viewType, int max) {
            ScrapData scrapData = getScrapDataForType(viewType);
            scrapData.mMaxScrap = max;
            ArrayList<ViewHolder> scrapHeap = scrapData.mScrapHeap;
            while (scrapHeap.size() > max) {
                scrapHeap.remove(scrapHeap.size() - 1);
            }
        }

        public int getRecycledViewCount(int viewType) {
            return getScrapDataForType(viewType).mScrapHeap.size();
        }

        public ViewHolder getRecycledView(int viewType) {
            ScrapData scrapData = mScrap.get(viewType);
            if (scrapData != null && !scrapData.mScrapHeap.isEmpty()) {
                ArrayList<ViewHolder> scrapHeap = scrapData.mScrapHeap;
                for (int i = scrapHeap.size() - 1; i >= 0; i--) {
                    if (!scrapHeap.get(i).isAttachedToTransitionOverlay()) {
                        return scrapHeap.remove(i);
                    }
                }
            }
            return null;
        }

        int size() {
            int count = 0;
            for (int i = 0; i < mScrap.size(); i++) {
                count += mScrap.valueAt(i).mScrapHeap.size();
            }
            return count;
        }

        public void putRecycledView(ViewHolder scrap) {
            int viewType = scrap.getItemViewType();
            ArrayList<ViewHolder> scrapHeap = getScrapDataForType(viewType).mScrapHeap;
            if (mScrap.get(viewType).mMaxScrap <= scrapHeap.size()) {
                return;
            }
            scrap.resetInternal();
            scrapHeap.add(scrap);
        }

        void attach() {
            mAttachCount++;
        }

        void detach() {
            mAttachCount--;
        }

        void onAdapterChanged(Adapter oldAdapter, Adapter newAdapter, boolean compatibleWithPrevious) {
            if (oldAdapter != null) {
                detach();
            }
            if (!compatibleWithPrevious && mAttachCount == 0) {
                clear();
            }
            if (newAdapter != null) {
                attach();
            }
        }

        private ScrapData getScrapDataForType(int viewType) {
            ScrapData scrapData = mScrap.get(viewType);
            if (scrapData == null) {
                scrapData = new ScrapData();
                mScrap.put(viewType, scrapData);
            }
            return scrapData;
        }
    }

    /// A last-chance source of views, consulted before the shared pool.
    public abstract static class ViewCacheExtension {
        public abstract View getViewForPositionAndType(Recycler recycler, int position, int type);
    }

    // ------------------------------------------------------------ State

    /// What a layout pass needs to know about the adapter and the scroll.
    public static class State {
        static final int STEP_START = 1;
        static final int STEP_LAYOUT = 2;
        static final int STEP_ANIMATIONS = 4;

        int mTargetPosition = NO_POSITION;
        private SparseArray<Object> mData;
        int mItemCount;
        boolean mStructureChanged;
        boolean mInPreLayout;
        boolean mRunSimpleAnimations;
        boolean mIsMeasuring;
        int mRemainingScrollHorizontal;
        int mRemainingScrollVertical;

        public boolean isMeasuring() {
            return mIsMeasuring;
        }

        public boolean isPreLayout() {
            return mInPreLayout;
        }

        public boolean willRunPredictiveAnimations() {
            return false;
        }

        public boolean willRunSimpleAnimations() {
            return mRunSimpleAnimations;
        }

        public void remove(int resourceId) {
            if (mData != null) {
                mData.remove(resourceId);
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T get(int resourceId) {
            return mData == null ? null : (T) mData.get(resourceId);
        }

        public void put(int resourceId, Object data) {
            if (mData == null) {
                mData = new SparseArray<Object>();
            }
            mData.put(resourceId, data);
        }

        public int getTargetScrollPosition() {
            return mTargetPosition;
        }

        public boolean hasTargetScrollPosition() {
            return mTargetPosition != NO_POSITION;
        }

        public boolean didStructureChange() {
            return mStructureChanged;
        }

        public int getItemCount() {
            return mItemCount;
        }

        public int getRemainingScrollHorizontal() {
            return mRemainingScrollHorizontal;
        }

        public int getRemainingScrollVertical() {
            return mRemainingScrollVertical;
        }

        @Override
        public String toString() {
            return "State{mTargetPosition=" + mTargetPosition + ", mData=" + mData + ", mItemCount=" + mItemCount
                    + ", mStructureChanged=" + mStructureChanged + ", mInPreLayout=" + mInPreLayout
                    + ", mRunSimpleAnimations=" + mRunSimpleAnimations + '}';
        }
    }

    // ------------------------------------------------------------ listeners and callbacks

    /// Draws around items and asks for space next to them.
    public abstract static class ItemDecoration {
        public void onDraw(Canvas c, RecyclerView parent, State state) {
            onDraw(c, parent);
        }

        @Deprecated
        public void onDraw(Canvas c, RecyclerView parent) {
        }

        public void onDrawOver(Canvas c, RecyclerView parent, State state) {
            onDrawOver(c, parent);
        }

        @Deprecated
        public void onDrawOver(Canvas c, RecyclerView parent) {
        }

        @Deprecated
        public void getItemOffsets(Rect outRect, int itemPosition, RecyclerView parent) {
            outRect.set(0, 0, 0, 0);
        }

        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            getItemOffsets(outRect, ((LayoutParams) view.getLayoutParams()).getViewLayoutPosition(), parent);
        }
    }

    /// Sees the touches a RecyclerView receives before it scrolls with them.
    public interface OnItemTouchListener {
        boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e);

        void onTouchEvent(RecyclerView rv, MotionEvent e);

        void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept);
    }

    public static class SimpleOnItemTouchListener implements OnItemTouchListener {
        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }
    }

    public abstract static class OnScrollListener {
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        }

        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        }
    }

    public interface RecyclerListener {
        void onViewRecycled(ViewHolder holder);
    }

    public interface OnChildAttachStateChangeListener {
        void onChildViewAttachedToWindow(View view);

        void onChildViewDetachedFromWindow(View view);
    }

    public abstract static class OnFlingListener {
        public abstract boolean onFling(int velocityX, int velocityY);
    }

    public interface ChildDrawingOrderCallback {
        int onGetChildDrawingOrder(int childCount, int i);
    }

    /// Creates the edge effects; kept for API compatibility -- the list
    /// stops at its ends without an overscroll glow.
    public static class EdgeEffectFactory {
        public static final int DIRECTION_LEFT = 0;
        public static final int DIRECTION_TOP = 1;
        public static final int DIRECTION_RIGHT = 2;
        public static final int DIRECTION_BOTTOM = 3;

        protected EdgeEffect createEdgeEffect(RecyclerView view, int direction) {
            return new EdgeEffect(view.getContext());
        }
    }

    private final class ItemAnimatorRestoreListener implements ItemAnimator.ItemAnimatorListener {
        @Override
        public void onAnimationFinished(ViewHolder item) {
            item.setIsRecyclable(true);
            if (isHidden(item.itemView)) {
                removeHiddenView(item.itemView);
                if (item.isRemoved() || item.isRecyclable()) {
                    mRecycler.recycleViewHolderInternal(item);
                }
            }
        }
    }

    // ------------------------------------------------------------ ItemAnimator

    /// Animates the items when the adapter changes.
    public abstract static class ItemAnimator {
        public static final int FLAG_CHANGED = ViewHolder.FLAG_UPDATE;
        public static final int FLAG_REMOVED = ViewHolder.FLAG_REMOVED;
        public static final int FLAG_INVALIDATED = ViewHolder.FLAG_INVALID;
        public static final int FLAG_MOVED = ViewHolder.FLAG_MOVED;
        public static final int FLAG_APPEARED_IN_PRE_LAYOUT = 1 << 12;

        interface ItemAnimatorListener {
            void onAnimationFinished(ViewHolder item);
        }

        public interface ItemAnimatorFinishedListener {
            void onAnimationsFinished();
        }

        /// Where an item was (or is) and how its adapter changed.
        public static class ItemHolderInfo {
            public int left;
            public int top;
            public int right;
            public int bottom;
            public int changeFlags;

            public ItemHolderInfo setFrom(ViewHolder holder) {
                return setFrom(holder, 0);
            }

            public ItemHolderInfo setFrom(ViewHolder holder, int flags) {
                View view = holder.itemView;
                this.left = view.getLeft();
                this.top = view.getTop();
                this.right = view.getRight();
                this.bottom = view.getBottom();
                return this;
            }
        }

        private ItemAnimatorListener mListener;
        private final ArrayList<ItemAnimatorFinishedListener> mFinishedListeners =
                new ArrayList<ItemAnimatorFinishedListener>();
        private long mAddDuration = 120;
        private long mRemoveDuration = 120;
        private long mMoveDuration = 250;
        private long mChangeDuration = 250;

        public long getMoveDuration() {
            return mMoveDuration;
        }

        public void setMoveDuration(long moveDuration) {
            mMoveDuration = moveDuration;
        }

        public long getAddDuration() {
            return mAddDuration;
        }

        public void setAddDuration(long addDuration) {
            mAddDuration = addDuration;
        }

        public long getRemoveDuration() {
            return mRemoveDuration;
        }

        public void setRemoveDuration(long removeDuration) {
            mRemoveDuration = removeDuration;
        }

        public long getChangeDuration() {
            return mChangeDuration;
        }

        public void setChangeDuration(long changeDuration) {
            mChangeDuration = changeDuration;
        }

        void setListener(ItemAnimatorListener listener) {
            mListener = listener;
        }

        public ItemHolderInfo recordPreLayoutInformation(State state, ViewHolder viewHolder, int changeFlags,
                                                         List<Object> payloads) {
            ItemHolderInfo info = obtainHolderInfo().setFrom(viewHolder);
            info.changeFlags = changeFlags;
            return info;
        }

        public ItemHolderInfo recordPostLayoutInformation(State state, ViewHolder viewHolder) {
            return obtainHolderInfo().setFrom(viewHolder);
        }

        public abstract boolean animateDisappearance(ViewHolder viewHolder, ItemHolderInfo preLayoutInfo,
                                                     ItemHolderInfo postLayoutInfo);

        public abstract boolean animateAppearance(ViewHolder viewHolder, ItemHolderInfo preLayoutInfo,
                                                  ItemHolderInfo postLayoutInfo);

        public abstract boolean animatePersistence(ViewHolder viewHolder, ItemHolderInfo preLayoutInfo,
                                                   ItemHolderInfo postLayoutInfo);

        public abstract boolean animateChange(ViewHolder oldHolder, ViewHolder newHolder,
                                              ItemHolderInfo preLayoutInfo, ItemHolderInfo postLayoutInfo);

        static int buildAdapterChangeFlagsForAnimations(ViewHolder viewHolder) {
            int flags = viewHolder.mFlags & (FLAG_INVALIDATED | FLAG_REMOVED | FLAG_CHANGED);
            if (viewHolder.isInvalid()) {
                return FLAG_INVALIDATED;
            }
            if ((flags & FLAG_INVALIDATED) == 0) {
                int oldPos = viewHolder.getOldPosition();
                int pos = viewHolder.getAbsoluteAdapterPosition();
                if (oldPos != NO_POSITION && pos != NO_POSITION && oldPos != pos) {
                    flags |= FLAG_MOVED;
                }
            }
            return flags;
        }

        public abstract void runPendingAnimations();

        public abstract void endAnimation(ViewHolder item);

        public abstract void endAnimations();

        public abstract boolean isRunning();

        public final void dispatchAnimationFinished(ViewHolder viewHolder) {
            onAnimationFinished(viewHolder);
            if (mListener != null) {
                mListener.onAnimationFinished(viewHolder);
            }
        }

        public void onAnimationFinished(ViewHolder viewHolder) {
        }

        public final void dispatchAnimationStarted(ViewHolder viewHolder) {
            onAnimationStarted(viewHolder);
        }

        public void onAnimationStarted(ViewHolder viewHolder) {
        }

        public final boolean isRunning(ItemAnimatorFinishedListener listener) {
            boolean running = isRunning();
            if (listener != null) {
                if (!running) {
                    listener.onAnimationsFinished();
                } else {
                    mFinishedListeners.add(listener);
                }
            }
            return running;
        }

        public boolean canReuseUpdatedViewHolder(ViewHolder viewHolder) {
            return true;
        }

        public boolean canReuseUpdatedViewHolder(ViewHolder viewHolder, List<Object> payloads) {
            return canReuseUpdatedViewHolder(viewHolder);
        }

        public final void dispatchAnimationsFinished() {
            int count = mFinishedListeners.size();
            for (int i = 0; i < count; ++i) {
                mFinishedListeners.get(i).onAnimationsFinished();
            }
            mFinishedListeners.clear();
        }

        public ItemHolderInfo obtainHolderInfo() {
            return new ItemHolderInfo();
        }
    }

    // ------------------------------------------------------------ SmoothScroller

    /// Scrolls until a target position is on screen, then onto its final
    /// place; [LinearSmoothScroller] is the usual one.
    public abstract static class SmoothScroller {
        private int mTargetPosition = NO_POSITION;
        private RecyclerView mRecyclerView;
        private LayoutManager mLayoutManager;
        private boolean mPendingInitialRun;
        private boolean mRunning;
        private View mTargetView;
        private final Action mRecyclingAction;
        private boolean mStarted;

        public SmoothScroller() {
            mRecyclingAction = new Action(0, 0);
        }

        void start(RecyclerView recyclerView, LayoutManager layoutManager) {
            recyclerView.mViewFlinger.stop();
            if (mStarted) {
                CompatReport.unsupported("RecyclerView", "restarting a SmoothScroller (use a new instance)");
            }
            mRecyclerView = recyclerView;
            mLayoutManager = layoutManager;
            if (mTargetPosition == NO_POSITION) {
                throw new IllegalArgumentException("Invalid target position");
            }
            mRecyclerView.mState.mTargetPosition = mTargetPosition;
            mRunning = true;
            mPendingInitialRun = true;
            mTargetView = findViewByPosition(getTargetPosition());
            onStart();
            mRecyclerView.setScrollState(SCROLL_STATE_SETTLING);
            mRecyclerView.mViewFlinger.postOnAnimation();
            mStarted = true;
        }

        public void setTargetPosition(int targetPosition) {
            mTargetPosition = targetPosition;
        }

        public PointF computeScrollVectorForPosition(int targetPosition) {
            LayoutManager layoutManager = getLayoutManager();
            if (layoutManager instanceof ScrollVectorProvider) {
                return ((ScrollVectorProvider) layoutManager).computeScrollVectorForPosition(targetPosition);
            }
            return null;
        }

        public LayoutManager getLayoutManager() {
            return mLayoutManager;
        }

        protected final void stop() {
            if (!mRunning) {
                return;
            }
            mRunning = false;
            onStop();
            mRecyclerView.mState.mTargetPosition = NO_POSITION;
            mTargetView = null;
            mTargetPosition = NO_POSITION;
            mPendingInitialRun = false;
            if (mLayoutManager != null) {
                mLayoutManager.onSmoothScrollerStopped(this);
            }
            mLayoutManager = null;
            mRecyclerView = null;
        }

        public boolean isPendingInitialRun() {
            return mPendingInitialRun;
        }

        public boolean isRunning() {
            return mRunning;
        }

        public int getTargetPosition() {
            return mTargetPosition;
        }

        void onAnimation(int dx, int dy) {
            RecyclerView recyclerView = mRecyclerView;
            if (mTargetPosition == NO_POSITION || recyclerView == null) {
                stop();
                return;
            }
            if (mPendingInitialRun && mTargetView == null && mLayoutManager != null) {
                PointF pointF = computeScrollVectorForPosition(mTargetPosition);
                if (pointF != null && (pointF.x != 0 || pointF.y != 0)) {
                    recyclerView.scrollStep(sign(pointF.x), sign(pointF.y));
                }
            }
            mPendingInitialRun = false;
            if (mTargetView != null) {
                if (getChildPosition(mTargetView) == mTargetPosition) {
                    onTargetFound(mTargetView, recyclerView.mState, mRecyclingAction);
                    mRecyclingAction.runIfNecessary(recyclerView);
                    stop();
                    return;
                } else {
                    mTargetView = null;
                }
            }
            if (mRunning) {
                onSeekTargetStep(dx, dy, recyclerView.mState, mRecyclingAction);
                boolean hadJumpTarget = mRecyclingAction.hasJumpTarget();
                mRecyclingAction.runIfNecessary(recyclerView);
                if (hadJumpTarget && mRunning) {
                    mPendingInitialRun = true;
                    recyclerView.mViewFlinger.postOnAnimation();
                }
            }
        }

        private static int sign(float f) {
            return f > 0 ? 1 : f < 0 ? -1 : 0;
        }

        public int getChildPosition(View view) {
            return mRecyclerView.getChildLayoutPosition(view);
        }

        public int getChildCount() {
            return mRecyclerView.mLayout.getChildCount();
        }

        public View findViewByPosition(int position) {
            return mRecyclerView.mLayout.findViewByPosition(position);
        }

        @Deprecated
        public void instantScrollToPosition(int position) {
            mRecyclerView.scrollToPosition(position);
        }

        protected void onChildAttachedToWindow(View child) {
            if (getChildPosition(child) == getTargetPosition()) {
                mTargetView = child;
            }
        }

        protected void normalize(PointF scrollVector) {
            float magnitude = (float) Math.sqrt(scrollVector.x * scrollVector.x + scrollVector.y * scrollVector.y);
            scrollVector.x /= magnitude;
            scrollVector.y /= magnitude;
        }

        protected abstract void onStart();

        protected abstract void onStop();

        protected abstract void onSeekTargetStep(int dx, int dy, State state, Action action);

        protected abstract void onTargetFound(View targetView, State state, Action action);

        /// What the next frames of a smooth scroll do.
        public static class Action {
            public static final int UNDEFINED_DURATION = RecyclerView.UNDEFINED_DURATION;
            private int mDx;
            private int mDy;
            private int mDuration;
            private int mJumpToPosition = NO_POSITION;
            private Interpolator mInterpolator;
            private boolean mChanged;

            public Action(int dx, int dy) {
                this(dx, dy, UNDEFINED_DURATION, null);
            }

            public Action(int dx, int dy, int duration) {
                this(dx, dy, duration, null);
            }

            public Action(int dx, int dy, int duration, Interpolator interpolator) {
                mDx = dx;
                mDy = dy;
                mDuration = duration;
                mInterpolator = interpolator;
            }

            public void jumpTo(int targetPosition) {
                mJumpToPosition = targetPosition;
            }

            boolean hasJumpTarget() {
                return mJumpToPosition >= 0;
            }

            void runIfNecessary(RecyclerView recyclerView) {
                if (mJumpToPosition >= 0) {
                    int position = mJumpToPosition;
                    mJumpToPosition = NO_POSITION;
                    recyclerView.jumpToPositionForSmoothScroller(position);
                    mChanged = false;
                    return;
                }
                if (mChanged) {
                    validate();
                    recyclerView.mViewFlinger.smoothScrollBy(mDx, mDy, mDuration, mInterpolator);
                    mChanged = false;
                }
            }

            private void validate() {
                if (mInterpolator != null && mDuration < 1) {
                    throw new IllegalStateException("If you provide an interpolator, you must set a positive duration");
                } else if (mDuration < 1) {
                    throw new IllegalStateException("Scroll duration must be a positive number");
                }
            }

            public int getDx() {
                return mDx;
            }

            public void setDx(int dx) {
                mChanged = true;
                mDx = dx;
            }

            public int getDy() {
                return mDy;
            }

            public void setDy(int dy) {
                mChanged = true;
                mDy = dy;
            }

            public int getDuration() {
                return mDuration;
            }

            public void setDuration(int duration) {
                mChanged = true;
                mDuration = duration;
            }

            public Interpolator getInterpolator() {
                return mInterpolator;
            }

            public void setInterpolator(Interpolator interpolator) {
                mChanged = true;
                mInterpolator = interpolator;
            }

            public void update(int dx, int dy, int duration, Interpolator interpolator) {
                mDx = dx;
                mDy = dy;
                mDuration = duration;
                mInterpolator = interpolator;
                mChanged = true;
            }
        }

        /// A layout manager that knows which way to scroll to reach a
        /// position.
        public interface ScrollVectorProvider {
            PointF computeScrollVectorForPosition(int targetPosition);
        }
    }

    // ------------------------------------------------------------ LayoutManager

    /// Measures and positions the items, and scrolls them.
    public abstract static class LayoutManager {
        RecyclerView mRecyclerView;
        SmoothScroller mSmoothScroller;
        boolean mRequestedSimpleAnimations;
        boolean mIsAttachedToWindow;
        private boolean mAutoMeasure;
        private boolean mItemPrefetchEnabled = true;
        private boolean mMeasurementCacheEnabled = true;
        private int mWidthMode;
        private int mHeightMode;
        private int mWidth;
        private int mHeight;

        /// The layout attributes a layout manager reads from its
        /// RecyclerView's XML.
        public static class Properties {
            public int orientation;
            public int spanCount;
            public boolean reverseLayout;
            public boolean stackFromEnd;
        }

        public static Properties getProperties(Context context, AttributeSet attrs, int defStyleAttr,
                                               int defStyleRes) {
            Properties properties = new Properties();
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecyclerView, defStyleAttr, defStyleRes);
            properties.orientation = a.getInt(R.styleable.RecyclerView_android_orientation, DEFAULT_ORIENTATION);
            properties.spanCount = a.getInt(R.styleable.RecyclerView_spanCount, 1);
            properties.reverseLayout = a.getBoolean(R.styleable.RecyclerView_reverseLayout, false);
            properties.stackFromEnd = a.getBoolean(R.styleable.RecyclerView_stackFromEnd, false);
            a.recycle();
            return properties;
        }

        void setRecyclerView(RecyclerView recyclerView) {
            if (recyclerView == null) {
                mRecyclerView = null;
                mWidth = 0;
                mHeight = 0;
            } else {
                mRecyclerView = recyclerView;
                mWidth = recyclerView.getWidth();
                mHeight = recyclerView.getHeight();
            }
            mWidthMode = MeasureSpec.EXACTLY;
            mHeightMode = MeasureSpec.EXACTLY;
        }

        void setMeasureSpecs(int wSpec, int hSpec) {
            mWidth = MeasureSpec.getSize(wSpec);
            mWidthMode = MeasureSpec.getMode(wSpec);
            if (mWidthMode == MeasureSpec.UNSPECIFIED) {
                mWidth = 0;
            }
            mHeight = MeasureSpec.getSize(hSpec);
            mHeightMode = MeasureSpec.getMode(hSpec);
            if (mHeightMode == MeasureSpec.UNSPECIFIED) {
                mHeight = 0;
            }
        }

        void setExactMeasureSpecsFrom(RecyclerView recyclerView) {
            setMeasureSpecs(MeasureSpec.makeMeasureSpec(recyclerView.getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(recyclerView.getHeight(), MeasureSpec.EXACTLY));
        }

        void setMeasuredDimensionFromChildren(int widthSpec, int heightSpec) {
            int count = getChildCount();
            if (count == 0) {
                mRecyclerView.defaultOnMeasure(widthSpec, heightSpec);
                return;
            }
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                Rect bounds = mRecyclerView.mTempRect;
                getDecoratedBoundsWithMargins(child, bounds);
                if (bounds.left < minX) {
                    minX = bounds.left;
                }
                if (bounds.right > maxX) {
                    maxX = bounds.right;
                }
                if (bounds.top < minY) {
                    minY = bounds.top;
                }
                if (bounds.bottom > maxY) {
                    maxY = bounds.bottom;
                }
            }
            mRecyclerView.mTempRect.set(minX, minY, maxX, maxY);
            setMeasuredDimension(mRecyclerView.mTempRect, widthSpec, heightSpec);
        }

        public void setMeasuredDimension(Rect childrenBounds, int wSpec, int hSpec) {
            int usedWidth = childrenBounds.width() + getPaddingLeft() + getPaddingRight();
            int usedHeight = childrenBounds.height() + getPaddingTop() + getPaddingBottom();
            int width = chooseSize(wSpec, usedWidth, getMinimumWidth());
            int height = chooseSize(hSpec, usedHeight, getMinimumHeight());
            setMeasuredDimension(width, height);
        }

        public void requestLayout() {
            if (mRecyclerView != null) {
                mRecyclerView.requestLayout();
            }
        }

        public void assertInLayoutOrScroll(String message) {
        }

        public static int chooseSize(int spec, int desired, int min) {
            int mode = MeasureSpec.getMode(spec);
            int size = MeasureSpec.getSize(spec);
            switch (mode) {
                case MeasureSpec.EXACTLY:
                    return size;
                case MeasureSpec.AT_MOST:
                    return Math.min(size, Math.max(desired, min));
                default:
                    return Math.max(desired, min);
            }
        }

        public void assertNotInLayoutOrScroll(String message) {
        }

        @Deprecated
        public void setAutoMeasureEnabled(boolean enabled) {
            mAutoMeasure = enabled;
        }

        public boolean isAutoMeasureEnabled() {
            return mAutoMeasure;
        }

        public boolean supportsPredictiveItemAnimations() {
            return false;
        }

        public final void setItemPrefetchEnabled(boolean enabled) {
            mItemPrefetchEnabled = enabled;
        }

        public final boolean isItemPrefetchEnabled() {
            return mItemPrefetchEnabled;
        }

        public void collectAdjacentPrefetchPositions(int dx, int dy, State state,
                                                     LayoutPrefetchRegistry layoutPrefetchRegistry) {
        }

        public void collectInitialPrefetchPositions(int adapterItemCount,
                                                    LayoutPrefetchRegistry layoutPrefetchRegistry) {
        }

        void dispatchAttachedToWindow(RecyclerView view) {
            mIsAttachedToWindow = true;
            onAttachedToWindow(view);
        }

        void dispatchDetachedFromWindow(RecyclerView view, Recycler recycler) {
            mIsAttachedToWindow = false;
            onDetachedFromWindow(view, recycler);
        }

        public boolean isAttachedToWindow() {
            return mIsAttachedToWindow;
        }

        public void postOnAnimation(Runnable action) {
            if (mRecyclerView != null) {
                mRecyclerView.postOnAnimation(action);
            }
        }

        public boolean removeCallbacks(Runnable action) {
            return mRecyclerView != null && mRecyclerView.removeCallbacks(action);
        }

        public void onAttachedToWindow(RecyclerView view) {
        }

        @Deprecated
        public void onDetachedFromWindow(RecyclerView view) {
        }

        public void onDetachedFromWindow(RecyclerView view, Recycler recycler) {
            onDetachedFromWindow(view);
        }

        public boolean getClipToPadding() {
            return mRecyclerView != null && mRecyclerView.getClipToPadding();
        }

        public void onLayoutChildren(Recycler recycler, State state) {
            android.util.Log.e("RecyclerView", "You must override onLayoutChildren(Recycler recycler, State state) ");
        }

        public void onLayoutCompleted(State state) {
        }

        public abstract LayoutParams generateDefaultLayoutParams();

        public boolean checkLayoutParams(LayoutParams lp) {
            return lp != null;
        }

        public LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
            if (lp instanceof LayoutParams) {
                return new LayoutParams((LayoutParams) lp);
            } else if (lp instanceof MarginLayoutParams) {
                return new LayoutParams((MarginLayoutParams) lp);
            } else {
                return new LayoutParams(lp);
            }
        }

        public LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
            return new LayoutParams(c, attrs);
        }

        public int scrollHorizontallyBy(int dx, Recycler recycler, State state) {
            return 0;
        }

        public int scrollVerticallyBy(int dy, Recycler recycler, State state) {
            return 0;
        }

        public boolean canScrollHorizontally() {
            return false;
        }

        public boolean canScrollVertically() {
            return false;
        }

        /// Whether the content can scroll further toward the end (positive
        /// `direction`) or start: the computed scroll offset and range.
        boolean canScrollInDirection(int direction, State state) {
            boolean vertical = canScrollVertically();
            int offset = vertical ? computeVerticalScrollOffset(state) : computeHorizontalScrollOffset(state);
            int range = (vertical ? computeVerticalScrollRange(state) : computeHorizontalScrollRange(state))
                    - (vertical ? computeVerticalScrollExtent(state) : computeHorizontalScrollExtent(state));
            if (range == 0) {
                return false;
            }
            if (direction < 0) {
                return offset > 0;
            }
            return offset < range - 1;
        }

        public void scrollToPosition(int position) {
        }

        public void smoothScrollToPosition(RecyclerView recyclerView, State state, int position) {
            android.util.Log.e("RecyclerView", "You must override smoothScrollToPosition to support smooth scrolling");
        }

        public void startSmoothScroll(SmoothScroller smoothScroller) {
            if (mSmoothScroller != null && smoothScroller != mSmoothScroller && mSmoothScroller.isRunning()) {
                mSmoothScroller.stop();
            }
            mSmoothScroller = smoothScroller;
            mSmoothScroller.start(mRecyclerView, this);
        }

        public boolean isSmoothScrolling() {
            return mSmoothScroller != null && mSmoothScroller.isRunning();
        }

        void stopSmoothScroller() {
            if (mSmoothScroller != null) {
                mSmoothScroller.stop();
            }
        }

        void onSmoothScrollerStopped(SmoothScroller smoothScroller) {
            if (mSmoothScroller == smoothScroller) {
                mSmoothScroller = null;
            }
        }

        public int getLayoutDirection() {
            return mRecyclerView == null ? View.LAYOUT_DIRECTION_LTR : mRecyclerView.getLayoutDirection();
        }

        public void endAnimation(View view) {
            if (mRecyclerView.mItemAnimator != null) {
                mRecyclerView.mItemAnimator.endAnimation(getChildViewHolderInt(view));
            }
        }

        public void addDisappearingView(View child) {
            addDisappearingView(child, -1);
        }

        public void addDisappearingView(View child, int index) {
            addView(child, index);
        }

        public void addView(View child) {
            addView(child, -1);
        }

        public void addView(View child, int index) {
            ViewHolder holder = getChildViewHolderInt(child);
            ViewGroup.LayoutParams raw = child.getLayoutParams();
            if (holder == null) {
                throw new IllegalArgumentException("View is not a RecyclerView item: " + child);
            }
            if (holder.wasReturnedFromScrap() || holder.isScrap()) {
                if (holder.isScrap()) {
                    holder.unScrap();
                } else {
                    holder.clearReturnedFromScrapFlag();
                }
                if (child.getParent() == mRecyclerView) {
                    if (holder.isTmpDetached()) {
                        mRecyclerView.attachManaged(child, index, raw);
                    }
                } else {
                    mRecyclerView.attachManaged(child, index, raw);
                }
            } else if (child.getParent() == mRecyclerView) {
                if (mRecyclerView.isHidden(child)) {
                    mRecyclerView.mHiddenViews.remove(child);
                }
                int currentIndex = mRecyclerView.managedIndexOf(child);
                if (index == -1) {
                    index = getChildCount() - 1;
                }
                if (currentIndex != index) {
                    mRecyclerView.moveManaged(child, index);
                }
            } else {
                if (holder.isTmpDetached()) {
                    mRecyclerView.attachManaged(child, index, raw);
                    mRecyclerView.dispatchChildAttached(child);
                } else {
                    mRecyclerView.addManaged(child, index);
                }
                if (raw instanceof LayoutParams) {
                    ((LayoutParams) raw).mInsetsDirty = true;
                }
                if (mSmoothScroller != null && mSmoothScroller.isRunning()) {
                    mSmoothScroller.onChildAttachedToWindow(child);
                }
            }
            if (raw instanceof LayoutParams && ((LayoutParams) raw).mPendingInvalidate) {
                child.invalidate();
                ((LayoutParams) raw).mPendingInvalidate = false;
            }
        }

        public void removeView(View child) {
            mRecyclerView.removeManaged(child);
        }

        public void removeViewAt(int index) {
            View child = getChildAt(index);
            if (child != null) {
                mRecyclerView.removeManaged(child);
            }
        }

        public void removeAllViews() {
            for (int i = getChildCount() - 1; i >= 0; i--) {
                removeViewAt(i);
            }
        }

        public int getBaseline() {
            return -1;
        }

        public int getPosition(View view) {
            return ((LayoutParams) view.getLayoutParams()).getViewLayoutPosition();
        }

        public int getItemViewType(View view) {
            return getChildViewHolderInt(view).getItemViewType();
        }

        public View findContainingItemView(View view) {
            if (mRecyclerView == null) {
                return null;
            }
            View found = mRecyclerView.findContainingItemView(view);
            if (found == null || mRecyclerView.isHidden(found)) {
                return null;
            }
            return found;
        }

        public View findViewByPosition(int position) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                ViewHolder vh = getChildViewHolderInt(child);
                if (vh == null || vh.shouldIgnore()) {
                    continue;
                }
                if (vh.getLayoutPosition() == position && !vh.isRemoved()) {
                    return child;
                }
            }
            return null;
        }

        public void detachView(View child) {
            int ind = mRecyclerView.managedIndexOf(child);
            if (ind >= 0) {
                detachViewInternal(ind);
            }
        }

        public void detachViewAt(int index) {
            detachViewInternal(index);
        }

        private void detachViewInternal(int index) {
            mRecyclerView.detachManaged(index);
        }

        public void attachView(View child, int index, LayoutParams lp) {
            ViewHolder vh = getChildViewHolderInt(child);
            mRecyclerView.attachManaged(child, index, lp);
            if (vh != null) {
                vh.clearTmpDetachFlag();
            }
        }

        public void attachView(View child, int index) {
            attachView(child, index, (LayoutParams) child.getLayoutParams());
        }

        public void attachView(View child) {
            attachView(child, -1);
        }

        public void removeDetachedView(View child) {
            mRecyclerView.removeDetachedView(child, false);
        }

        public void moveView(int fromIndex, int toIndex) {
            View view = getChildAt(fromIndex);
            if (view == null) {
                throw new IllegalArgumentException("Cannot move a child from non-existing index:" + fromIndex
                        + mRecyclerView.toString());
            }
            mRecyclerView.moveManaged(view, toIndex);
        }

        public void detachAndScrapView(View child, Recycler recycler) {
            int index = mRecyclerView.managedIndexOf(child);
            scrapOrRecycleView(recycler, index, child);
        }

        public void detachAndScrapViewAt(int index, Recycler recycler) {
            scrapOrRecycleView(recycler, index, getChildAt(index));
        }

        public void removeAndRecycleView(View child, Recycler recycler) {
            removeView(child);
            recycler.recycleView(child);
        }

        public void removeAndRecycleViewAt(int index, Recycler recycler) {
            View view = getChildAt(index);
            removeViewAt(index);
            recycler.recycleView(view);
        }

        public int getChildCount() {
            return mRecyclerView != null ? mRecyclerView.managedChildCount() : 0;
        }

        public View getChildAt(int index) {
            return mRecyclerView != null ? mRecyclerView.managedChildAt(index) : null;
        }

        public int getWidthMode() {
            return mWidthMode;
        }

        public int getHeightMode() {
            return mHeightMode;
        }

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }

        public int getPaddingLeft() {
            return mRecyclerView != null ? mRecyclerView.getPaddingLeft() : 0;
        }

        public int getPaddingTop() {
            return mRecyclerView != null ? mRecyclerView.getPaddingTop() : 0;
        }

        public int getPaddingRight() {
            return mRecyclerView != null ? mRecyclerView.getPaddingRight() : 0;
        }

        public int getPaddingBottom() {
            return mRecyclerView != null ? mRecyclerView.getPaddingBottom() : 0;
        }

        public int getPaddingStart() {
            return mRecyclerView != null ? mRecyclerView.getPaddingStart() : 0;
        }

        public int getPaddingEnd() {
            return mRecyclerView != null ? mRecyclerView.getPaddingEnd() : 0;
        }

        public boolean isFocused() {
            return mRecyclerView != null && mRecyclerView.isFocused();
        }

        public boolean hasFocus() {
            return mRecyclerView != null && mRecyclerView.hasFocus();
        }

        public View getFocusedChild() {
            if (mRecyclerView == null) {
                return null;
            }
            View focused = mRecyclerView.getFocusedChild();
            if (focused == null || mRecyclerView.isHidden(focused)) {
                return null;
            }
            return focused;
        }

        public int getItemCount() {
            Adapter a = mRecyclerView != null ? mRecyclerView.getAdapter() : null;
            return a != null ? a.getItemCount() : 0;
        }

        public void offsetChildrenHorizontal(int dx) {
            if (mRecyclerView != null) {
                mRecyclerView.offsetChildrenHorizontal(dx);
            }
        }

        public void offsetChildrenVertical(int dy) {
            if (mRecyclerView != null) {
                mRecyclerView.offsetChildrenVertical(dy);
            }
        }

        public void ignoreView(View view) {
            ViewHolder vh = getChildViewHolderInt(view);
            vh.addFlags(ViewHolder.FLAG_IGNORE);
        }

        public void stopIgnoringView(View view) {
            ViewHolder vh = getChildViewHolderInt(view);
            vh.mFlags &= ~ViewHolder.FLAG_IGNORE;
            vh.resetInternal();
            vh.addFlags(ViewHolder.FLAG_INVALID);
        }

        public void detachAndScrapAttachedViews(Recycler recycler) {
            int childCount = getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                View v = getChildAt(i);
                scrapOrRecycleView(recycler, i, v);
            }
        }

        private void scrapOrRecycleView(Recycler recycler, int index, View view) {
            ViewHolder viewHolder = getChildViewHolderInt(view);
            if (viewHolder == null || viewHolder.shouldIgnore()) {
                return;
            }
            if (viewHolder.isInvalid() && !viewHolder.isRemoved() && !mRecyclerView.mAdapter.hasStableIds()) {
                removeViewAt(index);
                recycler.recycleViewHolderInternal(viewHolder);
            } else {
                detachViewAt(index);
                recycler.scrapView(view);
            }
        }

        void removeAndRecycleScrapInt(Recycler recycler) {
            int scrapCount = recycler.getScrapCount();
            for (int i = scrapCount - 1; i >= 0; i--) {
                View scrap = recycler.getScrapViewAt(i);
                ViewHolder vh = getChildViewHolderInt(scrap);
                if (vh.shouldIgnore()) {
                    continue;
                }
                vh.setIsRecyclable(false);
                if (vh.isTmpDetached()) {
                    mRecyclerView.removeDetachedView(scrap, false);
                    mRecyclerView.dispatchChildDetached(scrap);
                    vh.clearTmpDetachFlag();
                }
                if (mRecyclerView.mItemAnimator != null) {
                    mRecyclerView.mItemAnimator.endAnimation(vh);
                }
                vh.setIsRecyclable(true);
                recycler.quickRecycleScrapView(scrap);
            }
            recycler.clearScrap();
        }

        public void measureChild(View child, int widthUsed, int heightUsed) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            Rect insets = mRecyclerView.getItemDecorInsetsForChild(child);
            widthUsed += insets.left + insets.right;
            heightUsed += insets.top + insets.bottom;
            int widthSpec = getChildMeasureSpec(getWidth(), getWidthMode(),
                    getPaddingLeft() + getPaddingRight() + widthUsed, lp.width, canScrollHorizontally());
            int heightSpec = getChildMeasureSpec(getHeight(), getHeightMode(),
                    getPaddingTop() + getPaddingBottom() + heightUsed, lp.height, canScrollVertically());
            child.measure(widthSpec, heightSpec);
        }

        public boolean isMeasurementCacheEnabled() {
            return mMeasurementCacheEnabled;
        }

        public void setMeasurementCacheEnabled(boolean measurementCacheEnabled) {
            mMeasurementCacheEnabled = measurementCacheEnabled;
        }

        public void measureChildWithMargins(View child, int widthUsed, int heightUsed) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            Rect insets = mRecyclerView.getItemDecorInsetsForChild(child);
            widthUsed += insets.left + insets.right;
            heightUsed += insets.top + insets.bottom;
            int widthSpec = getChildMeasureSpec(getWidth(), getWidthMode(),
                    getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin + widthUsed, lp.width,
                    canScrollHorizontally());
            int heightSpec = getChildMeasureSpec(getHeight(), getHeightMode(),
                    getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin + heightUsed, lp.height,
                    canScrollVertically());
            child.measure(widthSpec, heightSpec);
        }

        public static int getChildMeasureSpec(int parentSize, int parentMode, int padding, int childDimension,
                                              boolean canScroll) {
            int size = Math.max(0, parentSize - padding);
            int resultSize = 0;
            int resultMode = 0;
            if (canScroll) {
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == ViewGroup.LayoutParams.MATCH_PARENT) {
                    switch (parentMode) {
                        case MeasureSpec.AT_MOST:
                        case MeasureSpec.EXACTLY:
                            resultSize = size;
                            resultMode = parentMode;
                            break;
                        default:
                            resultSize = 0;
                            resultMode = MeasureSpec.UNSPECIFIED;
                            break;
                    }
                } else if (childDimension == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    resultSize = 0;
                    resultMode = MeasureSpec.UNSPECIFIED;
                }
            } else {
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == ViewGroup.LayoutParams.MATCH_PARENT) {
                    resultSize = size;
                    resultMode = parentMode;
                } else if (childDimension == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    resultSize = size;
                    if (parentMode == MeasureSpec.AT_MOST || parentMode == MeasureSpec.EXACTLY) {
                        resultMode = MeasureSpec.AT_MOST;
                    } else {
                        resultMode = MeasureSpec.UNSPECIFIED;
                    }
                }
            }
            return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
        }

        public int getDecoratedMeasuredWidth(View child) {
            Rect insets = ((LayoutParams) child.getLayoutParams()).mDecorInsets;
            return child.getMeasuredWidth() + insets.left + insets.right;
        }

        public int getDecoratedMeasuredHeight(View child) {
            Rect insets = ((LayoutParams) child.getLayoutParams()).mDecorInsets;
            return child.getMeasuredHeight() + insets.top + insets.bottom;
        }

        public void layoutDecorated(View child, int left, int top, int right, int bottom) {
            Rect insets = ((LayoutParams) child.getLayoutParams()).mDecorInsets;
            child.layout(left + insets.left, top + insets.top, right - insets.right, bottom - insets.bottom);
        }

        public void layoutDecoratedWithMargins(View child, int left, int top, int right, int bottom) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            Rect insets = lp.mDecorInsets;
            child.layout(left + insets.left + lp.leftMargin, top + insets.top + lp.topMargin,
                    right - insets.right - lp.rightMargin, bottom - insets.bottom - lp.bottomMargin);
        }

        public void getTransformedBoundingBox(View child, boolean includeDecorInsets, Rect out) {
            if (includeDecorInsets) {
                Rect insets = ((LayoutParams) child.getLayoutParams()).mDecorInsets;
                out.set(-insets.left, -insets.top, child.getWidth() + insets.right, child.getHeight() + insets.bottom);
            } else {
                out.set(0, 0, child.getWidth(), child.getHeight());
            }
            out.offset(child.getLeft() + Math.round(child.getTranslationX()),
                    child.getTop() + Math.round(child.getTranslationY()));
        }

        public void getDecoratedBoundsWithMargins(View view, Rect outBounds) {
            RecyclerView.getDecoratedBoundsWithMarginsInt(view, outBounds);
        }

        public int getDecoratedLeft(View child) {
            return child.getLeft() - getLeftDecorationWidth(child);
        }

        public int getDecoratedTop(View child) {
            return child.getTop() - getTopDecorationHeight(child);
        }

        public int getDecoratedRight(View child) {
            return child.getRight() + getRightDecorationWidth(child);
        }

        public int getDecoratedBottom(View child) {
            return child.getBottom() + getBottomDecorationHeight(child);
        }

        public void calculateItemDecorationsForChild(View child, Rect outRect) {
            if (mRecyclerView == null) {
                outRect.set(0, 0, 0, 0);
                return;
            }
            outRect.set(mRecyclerView.getItemDecorInsetsForChild(child));
        }

        public int getTopDecorationHeight(View child) {
            return ((LayoutParams) child.getLayoutParams()).mDecorInsets.top;
        }

        public int getBottomDecorationHeight(View child) {
            return ((LayoutParams) child.getLayoutParams()).mDecorInsets.bottom;
        }

        public int getLeftDecorationWidth(View child) {
            return ((LayoutParams) child.getLayoutParams()).mDecorInsets.left;
        }

        public int getRightDecorationWidth(View child) {
            return ((LayoutParams) child.getLayoutParams()).mDecorInsets.right;
        }

        public View onFocusSearchFailed(View focused, int direction, Recycler recycler, State state) {
            return null;
        }

        public View onInterceptFocusSearch(View focused, int direction) {
            return null;
        }

        public boolean requestChildRectangleOnScreen(RecyclerView parent, View child, Rect rect, boolean immediate) {
            return requestChildRectangleOnScreen(parent, child, rect, immediate, false);
        }

        public boolean requestChildRectangleOnScreen(RecyclerView parent, View child, Rect rect, boolean immediate,
                                                     boolean focusedChildVisible) {
            int parentLeft = getPaddingLeft();
            int parentTop = getPaddingTop();
            int parentRight = getWidth() - getPaddingRight();
            int parentBottom = getHeight() - getPaddingBottom();
            int childLeft = child.getLeft() + rect.left - child.getScrollX();
            int childTop = child.getTop() + rect.top - child.getScrollY();
            int childRight = childLeft + rect.width();
            int childBottom = childTop + rect.height();
            int offScreenLeft = Math.min(0, childLeft - parentLeft);
            int offScreenTop = Math.min(0, childTop - parentTop);
            int offScreenRight = Math.max(0, childRight - parentRight);
            int offScreenBottom = Math.max(0, childBottom - parentBottom);
            int dx = offScreenLeft != 0 ? offScreenLeft : Math.min(childLeft - parentLeft, offScreenRight);
            int dy = offScreenTop != 0 ? offScreenTop : Math.min(childTop - parentTop, offScreenBottom);
            if (dx != 0 || dy != 0) {
                if (immediate) {
                    parent.scrollBy(dx, dy);
                } else {
                    parent.smoothScrollBy(dx, dy);
                }
                return true;
            }
            return false;
        }

        @Deprecated
        public boolean onRequestChildFocus(RecyclerView parent, View child, View focused) {
            return isSmoothScrolling() || parent.isComputingLayout();
        }

        public boolean onRequestChildFocus(RecyclerView parent, State state, View child, View focused) {
            return onRequestChildFocus(parent, child, focused);
        }

        public void onAdapterChanged(Adapter oldAdapter, Adapter newAdapter) {
        }

        public boolean onAddFocusables(RecyclerView recyclerView, ArrayList<View> views, int direction,
                                       int focusableMode) {
            return false;
        }

        public void onItemsChanged(RecyclerView recyclerView) {
        }

        public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        }

        public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        }

        public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount) {
        }

        public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount, Object payload) {
            onItemsUpdated(recyclerView, positionStart, itemCount);
        }

        public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        }

        public int computeHorizontalScrollExtent(State state) {
            return 0;
        }

        public int computeHorizontalScrollOffset(State state) {
            return 0;
        }

        public int computeHorizontalScrollRange(State state) {
            return 0;
        }

        public int computeVerticalScrollExtent(State state) {
            return 0;
        }

        public int computeVerticalScrollOffset(State state) {
            return 0;
        }

        public int computeVerticalScrollRange(State state) {
            return 0;
        }

        public void onMeasure(Recycler recycler, State state, int widthSpec, int heightSpec) {
            mRecyclerView.defaultOnMeasure(widthSpec, heightSpec);
        }

        public void setMeasuredDimension(int widthSize, int heightSize) {
            mRecyclerView.setMeasuredDimensionInternal(widthSize, heightSize);
        }

        public int getMinimumWidth() {
            return mRecyclerView == null ? 0 : mRecyclerView.getMinimumWidth();
        }

        public int getMinimumHeight() {
            return mRecyclerView == null ? 0 : mRecyclerView.getMinimumHeight();
        }

        public Parcelable onSaveInstanceState() {
            return null;
        }

        public void onRestoreInstanceState(Parcelable state) {
        }

        public void onScrollStateChanged(int state) {
        }

        public void removeAndRecycleAllViews(Recycler recycler) {
            for (int i = getChildCount() - 1; i >= 0; i--) {
                View view = getChildAt(i);
                if (!getChildViewHolderInt(view).shouldIgnore()) {
                    removeAndRecycleViewAt(i, recycler);
                }
            }
        }

        public void requestSimpleAnimationsInNextLayout() {
            mRequestedSimpleAnimations = true;
        }

        public int getRowCountForAccessibility(Recycler recycler, State state) {
            return canScrollVertically() ? getItemCount() : 1;
        }

        public int getColumnCountForAccessibility(Recycler recycler, State state) {
            return canScrollHorizontally() ? getItemCount() : 1;
        }

        public boolean isLayoutHierarchical(Recycler recycler, State state) {
            return false;
        }

        public boolean isViewPartiallyVisible(View child, boolean completelyVisible, boolean acceptEndPointInclusion) {
            int parentLeft = getPaddingLeft();
            int parentTop = getPaddingTop();
            int parentRight = getWidth() - getPaddingRight();
            int parentBottom = getHeight() - getPaddingBottom();
            int left = getDecoratedLeft(child);
            int top = getDecoratedTop(child);
            int right = getDecoratedRight(child);
            int bottom = getDecoratedBottom(child);
            boolean isViewFullyVisible = left >= parentLeft && right <= parentRight && top >= parentTop
                    && bottom <= parentBottom;
            boolean isViewPartiallyVisible = right > parentLeft && left < parentRight && bottom > parentTop
                    && top < parentBottom;
            return completelyVisible ? isViewFullyVisible : isViewPartiallyVisible || isViewFullyVisible;
        }

        /// Receives the positions a layout manager would like prefetched.
        public interface LayoutPrefetchRegistry {
            void addPosition(int layoutPosition, int pixelDistance);
        }
    }
}
