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
package android.view;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import com.codename1.androidcompat.runtime.GroupPeer;
import com.codename1.ui.Component;

import java.util.ArrayList;

/// A view that contains other views. Children's peers are children of this
/// view's peer, so Codename One paints them in Android's order.
public abstract class ViewGroup extends View implements ViewParent, ViewManager {

    public static final int FOCUS_BEFORE_DESCENDANTS = 0x20000;
    public static final int FOCUS_AFTER_DESCENDANTS = 0x40000;
    public static final int FOCUS_BLOCK_DESCENDANTS = 0x60000;
    public static final int LAYOUT_MODE_CLIP_BOUNDS = 0;
    public static final int LAYOUT_MODE_OPTICAL_BOUNDS = 1;

    public interface OnHierarchyChangeListener {
        void onChildViewAdded(View parent, View child);

        void onChildViewRemoved(View parent, View child);
    }

    /// Width and height requests of a child: a size in pixels, or
    /// [#MATCH_PARENT] / [#WRAP_CONTENT].
    public static class LayoutParams {
        @Deprecated
        public static final int FILL_PARENT = -1;
        public static final int MATCH_PARENT = -1;
        public static final int WRAP_CONTENT = -2;

        public int width;
        public int height;
        /// Where the child sits in its group's layout animation.
        public LayoutAnimationController.AnimationParameters layoutAnimationParameters;

        public LayoutParams(Context c, AttributeSet attrs) {
            TypedArray a = c.obtainStyledAttributes(attrs, android.R.styleable.ViewGroup_Layout);
            setBaseAttributes(a, android.R.styleable.ViewGroup_Layout_layout_width,
                    android.R.styleable.ViewGroup_Layout_layout_height);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public LayoutParams(LayoutParams source) {
            this.width = source.width;
            this.height = source.height;
        }

        LayoutParams() {
        }

        protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr) {
            width = a.getLayoutDimension(widthAttr, "layout_width");
            height = a.getLayoutDimension(heightAttr, "layout_height");
        }

        public void resolveLayoutDirection(int layoutDirection) {
        }

        protected static String sizeToString(int size) {
            if (size == WRAP_CONTENT) {
                return "wrap-content";
            }
            if (size == MATCH_PARENT) {
                return "match-parent";
            }
            return String.valueOf(size);
        }

        @Override
        public String toString() {
            return "ViewGroup.LayoutParams={ width=" + sizeToString(width) + ", height=" + sizeToString(height) + " }";
        }
    }

    /// Layout params with margins. Start/end margins resolve to left/right
    /// against the layout direction.
    public static class MarginLayoutParams extends LayoutParams {
        public int leftMargin;
        public int topMargin;
        public int rightMargin;
        public int bottomMargin;
        private int startMargin = Integer.MIN_VALUE;
        private int endMargin = Integer.MIN_VALUE;
        private int layoutDirection;

        public MarginLayoutParams(Context c, AttributeSet attrs) {
            TypedArray a = c.obtainStyledAttributes(attrs, android.R.styleable.ViewGroup_MarginLayout);
            setBaseAttributes(a, android.R.styleable.ViewGroup_MarginLayout_layout_width,
                    android.R.styleable.ViewGroup_MarginLayout_layout_height);
            int margin = a.getDimensionPixelSize(android.R.styleable.ViewGroup_MarginLayout_layout_margin, -1);
            if (margin >= 0) {
                leftMargin = topMargin = rightMargin = bottomMargin = margin;
            } else {
                int h = a.getDimensionPixelSize(android.R.styleable.ViewGroup_MarginLayout_layout_marginHorizontal, -1);
                int v = a.getDimensionPixelSize(android.R.styleable.ViewGroup_MarginLayout_layout_marginVertical, -1);
                if (h >= 0) {
                    leftMargin = rightMargin = h;
                } else {
                    leftMargin = a.getDimensionPixelSize(android.R.styleable.ViewGroup_MarginLayout_layout_marginLeft, 0);
                    rightMargin = a.getDimensionPixelSize(android.R.styleable.ViewGroup_MarginLayout_layout_marginRight, 0);
                    startMargin = a.getDimensionPixelSize(android.R.styleable.ViewGroup_MarginLayout_layout_marginStart,
                            Integer.MIN_VALUE);
                    endMargin = a.getDimensionPixelSize(android.R.styleable.ViewGroup_MarginLayout_layout_marginEnd,
                            Integer.MIN_VALUE);
                }
                if (v >= 0) {
                    topMargin = bottomMargin = v;
                } else {
                    topMargin = a.getDimensionPixelSize(android.R.styleable.ViewGroup_MarginLayout_layout_marginTop, 0);
                    bottomMargin = a.getDimensionPixelSize(android.R.styleable.ViewGroup_MarginLayout_layout_marginBottom, 0);
                }
            }
            a.recycle();
            // Not the overridable resolveLayoutDirection: a subclass's fields
            // are not initialized yet while this constructor runs.
            resolveMargins(0);
        }

        public MarginLayoutParams(int width, int height) {
            super(width, height);
        }

        public MarginLayoutParams(MarginLayoutParams source) {
            this.width = source.width;
            this.height = source.height;
            this.leftMargin = source.leftMargin;
            this.topMargin = source.topMargin;
            this.rightMargin = source.rightMargin;
            this.bottomMargin = source.bottomMargin;
            this.startMargin = source.startMargin;
            this.endMargin = source.endMargin;
        }

        public MarginLayoutParams(LayoutParams source) {
            super(source);
        }

        public void setMargins(int left, int top, int right, int bottom) {
            leftMargin = left;
            topMargin = top;
            rightMargin = right;
            bottomMargin = bottom;
            startMargin = Integer.MIN_VALUE;
            endMargin = Integer.MIN_VALUE;
        }

        public void setMarginsRelative(int start, int top, int end, int bottom) {
            startMargin = start;
            topMargin = top;
            endMargin = end;
            bottomMargin = bottom;
            resolveLayoutDirection(layoutDirection);
        }

        public void setMarginStart(int start) {
            startMargin = start;
            resolveLayoutDirection(layoutDirection);
        }

        public int getMarginStart() {
            if (startMargin != Integer.MIN_VALUE) {
                return startMargin;
            }
            return layoutDirection == View.LAYOUT_DIRECTION_RTL ? rightMargin : leftMargin;
        }

        public void setMarginEnd(int end) {
            endMargin = end;
            resolveLayoutDirection(layoutDirection);
        }

        public int getMarginEnd() {
            if (endMargin != Integer.MIN_VALUE) {
                return endMargin;
            }
            return layoutDirection == View.LAYOUT_DIRECTION_RTL ? leftMargin : rightMargin;
        }

        public boolean isMarginRelative() {
            return startMargin != Integer.MIN_VALUE || endMargin != Integer.MIN_VALUE;
        }

        public void setLayoutDirection(int layoutDirection) {
            this.layoutDirection = layoutDirection;
        }

        public int getLayoutDirection() {
            return layoutDirection;
        }

        @Override
        public void resolveLayoutDirection(int dir) {
            resolveMargins(dir);
        }

        private void resolveMargins(int dir) {
            layoutDirection = dir;
            boolean rtl = dir == View.LAYOUT_DIRECTION_RTL;
            if (startMargin != Integer.MIN_VALUE) {
                if (rtl) {
                    rightMargin = startMargin;
                } else {
                    leftMargin = startMargin;
                }
            }
            if (endMargin != Integer.MIN_VALUE) {
                if (rtl) {
                    leftMargin = endMargin;
                } else {
                    rightMargin = endMargin;
                }
            }
        }
    }

    final ArrayList<View> mChildren = new ArrayList<View>();
    private View mTouchTarget;
    private boolean mDisallowIntercept;
    private boolean mClipChildren = true;
    private boolean mClipToPadding = true;
    private boolean mAddStatesFromChildren;
    private int mDescendantFocusability = FOCUS_BEFORE_DESCENDANTS;
    private OnHierarchyChangeListener mOnHierarchyChangeListener;
    private LayoutTransition mLayoutTransition;
    private LayoutAnimationController mLayoutAnimationController;
    private Animation.AnimationListener mAnimationListener;
    private boolean mRunLayoutAnimation;
    private boolean mLayoutAnimationRunning;
    private boolean mAnimationCacheEnabled = true;
    private int mPersistentDrawingCache = PERSISTENT_SCROLLING_CACHE;

    public static final int PERSISTENT_NO_CACHE = 0x0;
    public static final int PERSISTENT_ANIMATION_CACHE = 0x1;
    public static final int PERSISTENT_SCROLLING_CACHE = 0x2;
    public static final int PERSISTENT_ALL_CACHES = 0x3;
    private boolean mMotionEventSplitting = true;

    public ViewGroup(Context context) {
        super(context);
        setWillNotDraw(true);
    }

    public ViewGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ViewGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        // As on Android, a view group does not call onDraw until it has a
        // background or setWillNotDraw(false) says it draws.
        setWillNotDraw(true);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.ViewGroup, defStyleAttr, defStyleRes);
        mClipChildren = a.getBoolean(android.R.styleable.ViewGroup_clipChildren, true);
        mClipToPadding = a.getBoolean(android.R.styleable.ViewGroup_clipToPadding, true);
        a.recycle();
        a = context.obtainStyledAttributes(attrs, android.R.styleable.ViewGroupAnimation, defStyleAttr, defStyleRes);
        int layoutAnimation = a.getResourceId(android.R.styleable.ViewGroupAnimation_layoutAnimation, 0);
        boolean animateLayoutChanges = a.getBoolean(android.R.styleable.ViewGroupAnimation_animateLayoutChanges, false);
        mAnimationCacheEnabled = a.getBoolean(android.R.styleable.ViewGroupAnimation_animationCache, true);
        mPersistentDrawingCache = a.getInt(android.R.styleable.ViewGroupAnimation_persistentDrawingCache,
                PERSISTENT_SCROLLING_CACHE);
        a.recycle();
        if (layoutAnimation > 0) {
            setLayoutAnimation(AnimationUtils.loadLayoutAnimation(context, layoutAnimation));
        }
        if (animateLayoutChanges) {
            setLayoutTransition(new LayoutTransition());
        }
    }

    @Override
    protected Component createPeer() {
        return new GroupPeer(this);
    }

    /// The peer as a container.
    protected final GroupPeer groupPeer() {
        return (GroupPeer) getPeer();
    }

    // ------------------------------------------------------------ children

    public int getChildCount() {
        return mChildren.size();
    }

    public View getChildAt(int index) {
        if (index < 0 || index >= mChildren.size()) {
            return null;
        }
        return mChildren.get(index);
    }

    public int indexOfChild(View child) {
        return mChildren.indexOf(child);
    }

    public void addView(View child) {
        addView(child, -1);
    }

    public void addView(View child, int index) {
        LayoutParams params = child.getLayoutParams();
        if (params == null) {
            params = generateDefaultLayoutParams();
            if (params == null) {
                throw new IllegalArgumentException("generateDefaultLayoutParams() cannot return null");
            }
        }
        addView(child, index, params);
    }

    public void addView(View child, int width, int height) {
        LayoutParams params = generateDefaultLayoutParams();
        params.width = width;
        params.height = height;
        addView(child, -1, params);
    }

    @Override
    public void addView(View child, LayoutParams params) {
        addView(child, -1, params);
    }

    public void addView(View child, int index, LayoutParams params) {
        requestLayout();
        invalidate();
        addViewInner(child, index, params, false);
    }

    protected boolean addViewInLayout(View child, int index, LayoutParams params) {
        return addViewInLayout(child, index, params, false);
    }

    protected boolean addViewInLayout(View child, int index, LayoutParams params, boolean preventRequestLayout) {
        addViewInner(child, index, params, preventRequestLayout);
        return true;
    }

    private void addViewInner(View child, int index, LayoutParams params, boolean preventRequestLayout) {
        if (child.getParent() != null) {
            throw new IllegalStateException("The specified child already has a parent. "
                    + "You must call removeView() on the child's parent first.");
        }
        if (!checkLayoutParams(params)) {
            params = generateLayoutParams(params);
        }
        child.mLayoutParams = params;
        if (params instanceof MarginLayoutParams) {
            ((MarginLayoutParams) params).resolveLayoutDirection(getLayoutDirection());
        }
        if (index < 0 || index > mChildren.size()) {
            index = mChildren.size();
        }
        mChildren.add(index, child);
        child.assignParent(this);
        groupPeer().addChildPeer(child.getPeer(), index);
        if (!preventRequestLayout) {
            child.requestLayout();
        }
        if (isAttachedToWindow()) {
            child.dispatchAttachedToWindow(true);
        }
        onViewAdded(child);
        if (mOnHierarchyChangeListener != null) {
            mOnHierarchyChangeListener.onChildViewAdded(this, child);
        }
    }

    @Override
    public void updateViewLayout(View view, LayoutParams params) {
        view.setLayoutParams(params);
    }

    void onSetLayoutParams(View child, LayoutParams params) {
        requestLayout();
    }

    public void onViewAdded(View child) {
    }

    public void onViewRemoved(View child) {
    }

    @Override
    public void removeView(View view) {
        int index = mChildren.indexOf(view);
        if (index >= 0) {
            removeViewAt(index);
        }
    }

    public void removeViewInLayout(View view) {
        int index = mChildren.indexOf(view);
        if (index >= 0) {
            removeViewInternal(index, false);
        }
    }

    public void removeViewsInLayout(int start, int count) {
        for (int i = start + count - 1; i >= start; i--) {
            removeViewInternal(i, false);
        }
    }

    public void removeViewAt(int index) {
        removeViewInternal(index, true);
        requestLayout();
        invalidate();
    }

    public void removeViews(int start, int count) {
        for (int i = start + count - 1; i >= start; i--) {
            removeViewInternal(i, true);
        }
        requestLayout();
        invalidate();
    }

    public void removeAllViews() {
        removeAllViewsInLayout();
        requestLayout();
        invalidate();
    }

    public void removeAllViewsInLayout() {
        for (int i = mChildren.size() - 1; i >= 0; i--) {
            removeViewInternal(i, true);
        }
    }

    private void removeViewInternal(int index, boolean notify) {
        View child = mChildren.remove(index);
        if (child == mTouchTarget) {
            mTouchTarget = null;
        }
        if (child.isFocused()) {
            child.clearFocus();
        }
        if (child.hasPeer()) {
            groupPeer().removeChildPeer(child.getPeer());
        }
        if (child.isAttachedToWindow()) {
            child.dispatchAttachedToWindow(false);
        }
        child.assignParent(null);
        onViewRemoved(child);
        if (notify && mOnHierarchyChangeListener != null) {
            mOnHierarchyChangeListener.onChildViewRemoved(this, child);
        }
    }

    /// Detaches without the usual bookkeeping; RecyclerView-style containers
    /// pair this with [#attachViewToParent(View,int,LayoutParams)].
    protected void detachViewFromParent(View child) {
        int index = mChildren.indexOf(child);
        if (index >= 0) {
            detachViewFromParent(index);
        }
    }

    protected void detachViewFromParent(int index) {
        View child = mChildren.remove(index);
        if (child.hasPeer()) {
            groupPeer().removeChildPeer(child.getPeer());
        }
        child.assignParent(null);
    }

    protected void attachViewToParent(View child, int index, LayoutParams params) {
        child.mLayoutParams = params;
        if (index < 0 || index > mChildren.size()) {
            index = mChildren.size();
        }
        mChildren.add(index, child);
        child.assignParent(this);
        groupPeer().addChildPeer(child.getPeer(), index);
    }

    protected void detachAllViewsFromParent() {
        for (int i = mChildren.size() - 1; i >= 0; i--) {
            detachViewFromParent(i);
        }
    }

    protected void removeDetachedView(View child, boolean animate) {
        if (child.isAttachedToWindow()) {
            child.dispatchAttachedToWindow(false);
        }
    }

    @Override
    public void bringChildToFront(View child) {
        int index = mChildren.indexOf(child);
        if (index >= 0) {
            mChildren.remove(index);
            mChildren.add(child);
            groupPeer().removeChildPeer(child.getPeer());
            groupPeer().addChildPeer(child.getPeer(), mChildren.size() - 1);
            requestLayout();
            invalidate();
        }
    }

    public void setOnHierarchyChangeListener(OnHierarchyChangeListener listener) {
        mOnHierarchyChangeListener = listener;
    }

    public void setLayoutTransition(LayoutTransition transition) {
        mLayoutTransition = transition;
    }

    public LayoutTransition getLayoutTransition() {
        return mLayoutTransition;
    }

    // ------------------------------------------------------------ layout animation

    public void setLayoutAnimation(LayoutAnimationController controller) {
        mLayoutAnimationController = controller;
        if (controller != null) {
            mRunLayoutAnimation = true;
        }
    }

    public LayoutAnimationController getLayoutAnimation() {
        return mLayoutAnimationController;
    }

    public void startLayoutAnimation() {
        if (mLayoutAnimationController != null) {
            mRunLayoutAnimation = true;
            requestLayout();
            invalidate();
        }
    }

    public void scheduleLayoutAnimation() {
        mRunLayoutAnimation = true;
    }

    public void setLayoutAnimationListener(Animation.AnimationListener animationListener) {
        mAnimationListener = animationListener;
    }

    public Animation.AnimationListener getLayoutAnimationListener() {
        return mAnimationListener;
    }

    public boolean isAnimationCacheEnabled() {
        return mAnimationCacheEnabled;
    }

    public void setAnimationCacheEnabled(boolean enabled) {
        mAnimationCacheEnabled = enabled;
    }

    public int getPersistentDrawingCache() {
        return mPersistentDrawingCache;
    }

    public void setPersistentDrawingCache(int drawingCacheToKeep) {
        mPersistentDrawingCache = drawingCacheToKeep & PERSISTENT_ALL_CACHES;
    }

    protected boolean canAnimate() {
        return mLayoutAnimationController != null;
    }

    protected void attachLayoutAnimationParameters(View child, LayoutParams params, int index, int count) {
        LayoutAnimationController.AnimationParameters animationParams = params.layoutAnimationParameters;
        if (animationParams == null) {
            animationParams = new LayoutAnimationController.AnimationParameters();
            params.layoutAnimationParameters = animationParams;
        }
        animationParams.count = count;
        animationParams.index = index;
    }

    /// As AOSP's dispatchDraw: on the first paint after a layout animation
    /// is scheduled, each visible child gets its staggered copy of it.
    @Override
    void onGroupPaint() {
        if (mRunLayoutAnimation && canAnimate()) {
            mRunLayoutAnimation = false;
            LayoutAnimationController controller = mLayoutAnimationController;
            int count = mChildren.size();
            for (int i = 0; i < count; i++) {
                View child = mChildren.get(i);
                if (child.getVisibility() == VISIBLE && child.getLayoutParams() != null) {
                    attachLayoutAnimationParameters(child, child.getLayoutParams(), i, count);
                    child.setAnimation(controller.getAnimationForView(child));
                }
            }
            controller.start();
            mLayoutAnimationRunning = true;
            if (mAnimationListener != null) {
                mAnimationListener.onAnimationStart(controller.getAnimation());
            }
        } else if (mLayoutAnimationRunning && mLayoutAnimationController != null
                && mLayoutAnimationController.isDone()) {
            mLayoutAnimationRunning = false;
            if (mAnimationListener != null) {
                mAnimationListener.onAnimationEnd(mLayoutAnimationController.getAnimation());
            }
        }
    }

    // ------------------------------------------------------------ layout params

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return p;
    }

    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    protected boolean checkLayoutParams(LayoutParams p) {
        return p != null;
    }

    // ------------------------------------------------------------ measure

    protected void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
        for (View child : mChildren) {
            if (child.getVisibility() != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
            }
        }
    }

    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        final LayoutParams lp = child.getLayoutParams();
        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                mPaddingLeft + mPaddingRight, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                mPaddingTop + mPaddingBottom, lp.height);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin + widthUsed, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin + heightUsed, lp.height);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    /// AOSP's rule for turning a parent spec, the padding used, and a child's
    /// layout dimension into the child's spec.
    public static int getChildMeasureSpec(int spec, int padding, int childDimension) {
        int specMode = MeasureSpec.getMode(spec);
        int specSize = MeasureSpec.getSize(spec);
        int size = Math.max(0, specSize - padding);
        int resultSize = 0;
        int resultMode = 0;
        switch (specMode) {
            case MeasureSpec.EXACTLY:
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    resultSize = size;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                }
                break;
            case MeasureSpec.AT_MOST:
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                }
                break;
            default:
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    resultSize = size;
                    resultMode = MeasureSpec.UNSPECIFIED;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    resultSize = size;
                    resultMode = MeasureSpec.UNSPECIFIED;
                }
                break;
        }
        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }

    @Override
    protected abstract void onLayout(boolean changed, int l, int t, int r, int b);

    @Override
    public void requestLayout() {
        super.requestLayout();
    }

    public boolean shouldDelayChildPressedState() {
        return true;
    }

    // ------------------------------------------------------------ traversal

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends View> T findViewTraversal(int id) {
        if (id == mID) {
            return (T) this;
        }
        for (View v : mChildren) {
            View found = v.findViewTraversal(id);
            if (found != null) {
                return (T) found;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends View> T findViewWithTagTraversal(Object tag) {
        if (tag != null && tag.equals(mTag)) {
            return (T) this;
        }
        for (View v : mChildren) {
            View found = v.findViewWithTagTraversal(tag);
            if (found != null) {
                return (T) found;
            }
        }
        return null;
    }

    @Override
    public void findViewsWithText(ArrayList<View> outViews, CharSequence text, int flags) {
        super.findViewsWithText(outViews, text, flags);
        for (View v : mChildren) {
            v.findViewsWithText(outViews, text, flags);
        }
    }

    @Override
    public View findFocus() {
        if (isFocused()) {
            return this;
        }
        for (View v : mChildren) {
            View f = v.findFocus();
            if (f != null) {
                return f;
            }
        }
        return null;
    }

    @Override
    public boolean hasFocus() {
        return findFocus() != null;
    }

    public View getFocusedChild() {
        for (View v : mChildren) {
            if (v.findFocus() != null) {
                return v;
            }
        }
        return null;
    }

    public int getDescendantFocusability() {
        return mDescendantFocusability;
    }

    public void setDescendantFocusability(int focusability) {
        mDescendantFocusability = focusability;
    }

    @Override
    public void dispatchAttachedToWindow(boolean attached) {
        super.dispatchAttachedToWindow(attached);
        for (View v : new ArrayList<View>(mChildren)) {
            v.dispatchAttachedToWindow(attached);
        }
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        for (View v : mChildren) {
            if (!pressed || (!v.isClickable() && !v.isLongClickable())) {
                if (v.isDuplicateParentStateEnabled()) {
                    v.setPressed(pressed);
                }
            }
        }
    }

    @Override
    protected void dispatchSetSelected(boolean selected) {
        for (View v : mChildren) {
            v.setSelected(selected);
        }
    }

    @Override
    protected void dispatchSetActivated(boolean activated) {
        for (View v : mChildren) {
            v.setActivated(activated);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    public void setAddStatesFromChildren(boolean addsStates) {
        mAddStatesFromChildren = addsStates;
        refreshDrawableState();
    }

    public boolean addStatesFromChildren() {
        return mAddStatesFromChildren;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        if (!mAddStatesFromChildren) {
            return super.onCreateDrawableState(extraSpace);
        }
        int need = 0;
        for (View v : mChildren) {
            need += v.getDrawableState().length;
        }
        int[] state = super.onCreateDrawableState(extraSpace + need);
        for (View v : mChildren) {
            state = mergeDrawableStates(state, v.getDrawableState());
        }
        return state;
    }

    public void setClipChildren(boolean clipChildren) {
        mClipChildren = clipChildren;
    }

    public boolean getClipChildren() {
        return mClipChildren;
    }

    public void setClipToPadding(boolean clipToPadding) {
        mClipToPadding = clipToPadding;
    }

    public boolean getClipToPadding() {
        return mClipToPadding;
    }

    public void setMotionEventSplittingEnabled(boolean split) {
        mMotionEventSplitting = split;
    }

    public boolean isMotionEventSplittingEnabled() {
        return mMotionEventSplitting;
    }

    public void setChildrenDrawingOrderEnabled(boolean enabled) {
    }

    public void setDrawingCacheEnabled(boolean enabled) {
    }

    public void setLayoutMode(int layoutMode) {
    }

    public void setTransitionGroup(boolean isTransitionGroup) {
    }

    public void startViewTransition(View view) {
    }

    public void endViewTransition(View view) {
    }

    /// Resolves every child's start/end margins and relative rules against
    /// this group's layout direction before it measures, so parameters an
    /// application changed after adding the child are seen, as on Android.
    @Override
    void resolveChildLayoutParams() {
        int dir = getLayoutDirection();
        for (int i = 0, n = getChildCount(); i < n; i++) {
            ViewGroup.LayoutParams lp = getChildAt(i).getLayoutParams();
            if (lp instanceof MarginLayoutParams) {
                ((MarginLayoutParams) lp).resolveLayoutDirection(dir);
            }
        }
    }

    public void suppressLayout(boolean suppress) {
    }

    // ------------------------------------------------------------ drawing

    /// Children draw through their own peers; nothing to do here.
    @Override
    protected void dispatchDraw(Canvas canvas) {
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        return false;
    }

    // ------------------------------------------------------------ touch

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            mTouchTarget = null;
            mDisallowIntercept = false;
        }
        boolean intercepted;
        if (action == MotionEvent.ACTION_DOWN || mTouchTarget != null) {
            intercepted = !mDisallowIntercept && onInterceptTouchEvent(ev);
        } else {
            intercepted = true;
        }
        boolean handled = false;
        boolean dispatchedToNewTarget = false;
        if (action == MotionEvent.ACTION_DOWN && !intercepted) {
            final float x = ev.getX() + mScrollX;
            final float y = ev.getY() + mScrollY;
            for (int i = mChildren.size() - 1; i >= 0; i--) {
                View child = mChildren.get(i);
                if (child.getVisibility() != VISIBLE) {
                    continue;
                }
                float cx = x - child.getLeft() - child.getTranslationX();
                float cy = y - child.getTop() - child.getTranslationY();
                if (cx < 0 || cy < 0 || cx >= child.getWidth() || cy >= child.getHeight()) {
                    continue;
                }
                if (dispatchTransformed(child, ev)) {
                    mTouchTarget = child;
                    handled = true;
                    dispatchedToNewTarget = true;
                    break;
                }
            }
        }
        if (mTouchTarget == null) {
            if (!handled) {
                handled = super.dispatchTouchEvent(ev);
            }
        } else if (!dispatchedToNewTarget) {
            if (intercepted) {
                // The event that starts the interception only cancels the
                // child; it is not also handed to this group's onTouchEvent.
                // That is Android's contract: per onInterceptTouchEvent's
                // documentation the target receives this same event as
                // ACTION_CANCEL and only the events that follow reach the
                // group's onTouchEvent. Parents such as ScrollView rely on it
                // and start their drag inside onInterceptTouchEvent.
                MotionEvent cancel = MotionEvent.obtain(ev);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                dispatchTransformed(mTouchTarget, cancel);
                mTouchTarget = null;
                handled = true;
            } else {
                handled = dispatchTransformed(mTouchTarget, ev);
            }
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mTouchTarget = null;
            mDisallowIntercept = false;
        }
        return handled;
    }

    private boolean dispatchTransformed(View child, MotionEvent ev) {
        float dx = mScrollX - child.getLeft() - child.getTranslationX();
        float dy = mScrollY - child.getTop() - child.getTranslationY();
        ev.offsetLocation(dx, dy);
        boolean r = child.dispatchTouchEvent(ev);
        ev.offsetLocation(-dx, -dy);
        return r;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        mDisallowIntercept = disallowIntercept;
        if (mParent != null) {
            mParent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        View f = getFocusedChild();
        if (f != null && f.dispatchKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    // ------------------------------------------------------------ ViewParent

    @Override
    public void requestChildFocus(View child, View focused) {
        if (mParent != null) {
            mParent.requestChildFocus(this, focused);
        }
    }

    @Override
    public void clearChildFocus(View child) {
    }

    @Override
    public void focusableViewAvailable(View v) {
    }

    @Override
    public void childDrawableStateChanged(View child) {
        if (mAddStatesFromChildren) {
            refreshDrawableState();
        }
    }

    @Override
    public boolean getChildVisibleRect(View child, Rect r, Point offset) {
        r.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
        return r.intersect(0, 0, getWidth(), getHeight());
    }

    @Override
    public void invalidateChild(View child, Rect r) {
        invalidate();
    }

    @Override
    public void recomputeViewAttributes(View child) {
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        return mParent != null && mParent.showContextMenuForChild(originalView);
    }

    @Override
    public void requestTransparentRegion(View child) {
    }

    @Override
    public void childHasTransientStateChanged(View child, boolean hasTransientState) {
    }

    @Override
    public void requestFitSystemWindows() {
    }

    @Override
    public ViewParent getParentForAccessibility() {
        return mParent;
    }

    @Override
    public void notifySubtreeAccessibilityStateChanged(View child, View source, int changeType) {
    }

    @Override
    public boolean canResolveLayoutDirection() {
        return true;
    }

    @Override
    public boolean isLayoutDirectionResolved() {
        return true;
    }
}
