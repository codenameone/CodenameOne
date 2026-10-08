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
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import com.codename1.androidcompat.runtime.ScrollAnimator;

/// A vertically scrolling container with a single child. AOSP's model: the
/// child is measured with an unspecified height (or stretched to the viewport
/// when `fillViewport`), drags past the touch slop are intercepted from the
/// children and scroll the view, and a release flings with Android's
/// deceleration. Overscroll is clamped rather than drawn.
public class ScrollView extends FrameLayout {

    static final int ANIMATED_SCROLL_GAP = 250;
    static final float MAX_SCROLL_FACTOR = 0.5f;
    private static final int SCROLLBAR_FADE_DELAY = 500;
    private static final int SCROLLBAR_FADE_DURATION = 250;

    private long mLastScroll;
    private OverScroller mScroller;
    private ScrollAnimator mAnimator;
    private boolean mIsBeingDragged;
    private VelocityTracker mVelocityTracker;
    private int mLastMotionY;
    private boolean mFillViewport;
    private boolean mSmoothScrollingEnabled = true;
    private boolean mIsLayoutDirty = true;
    private View mChildToScrollTo;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private boolean mScrollbarEnabled = true;
    private long mLastScrollActivity;
    private Paint mScrollbarPaint;

    public ScrollView(Context context) {
        this(context, null);
    }

    public ScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.scrollViewStyle);
    }

    public ScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initScrollView();
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.ScrollView, defStyleAttr, defStyleRes);
        setFillViewport(a.getBoolean(android.R.styleable.ScrollView_fillViewport, false));
        a.recycle();
    }

    private void initScrollView() {
        mScroller = new OverScroller(getContext());
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mAnimator = new ScrollAnimator(this, new ScrollAnimator.Step() {
            @Override
            public boolean step() {
                return computeScrollStep();
            }
        });
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public int getMaxScrollAmount() {
        return (int) (MAX_SCROLL_FACTOR * (mBottom - mTop));
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child, index, params);
    }

    public boolean isFillViewport() {
        return mFillViewport;
    }

    public void setFillViewport(boolean fillViewport) {
        if (fillViewport != mFillViewport) {
            mFillViewport = fillViewport;
            requestLayout();
        }
    }

    public boolean isSmoothScrollingEnabled() {
        return mSmoothScrollingEnabled;
    }

    public void setSmoothScrollingEnabled(boolean smoothScrollingEnabled) {
        mSmoothScrollingEnabled = smoothScrollingEnabled;
    }

    @Override
    public void setVerticalScrollBarEnabled(boolean enabled) {
        mScrollbarEnabled = enabled;
    }

    @Override
    public boolean isVerticalScrollBarEnabled() {
        return mScrollbarEnabled;
    }

    // ------------------------------------------------------------ measure & layout

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!mFillViewport) {
            return;
        }
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            return;
        }
        if (getChildCount() > 0) {
            final View child = getChildAt(0);
            final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
            final int widthPadding = mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin;
            final int heightPadding = mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin;
            final int desiredHeight = getMeasuredHeight() - heightPadding;
            if (child.getMeasuredHeight() < desiredHeight) {
                final int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, widthPadding, lp.width);
                final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(desiredHeight, MeasureSpec.EXACTLY);
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        final int horizontalPadding = mPaddingLeft + mPaddingRight;
        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, horizontalPadding, lp.width);
        final int verticalPadding = mPaddingTop + mPaddingBottom;
        final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                Math.max(0, MeasureSpec.getSize(parentHeightMeasureSpec) - verticalPadding), MeasureSpec.UNSPECIFIED);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin + widthUsed, lp.width);
        final int usedTotal = mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin + heightUsed;
        final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                Math.max(0, MeasureSpec.getSize(parentHeightMeasureSpec) - usedTotal), MeasureSpec.UNSPECIFIED);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mIsLayoutDirty = false;
        if (mChildToScrollTo != null && isViewDescendantOf(mChildToScrollTo, this)) {
            scrollToChild(mChildToScrollTo);
        }
        mChildToScrollTo = null;
        int scrollRange = getScrollRange();
        int y = mScrollY;
        if (y > scrollRange) {
            y = scrollRange;
        } else if (y < 0) {
            y = 0;
        }
        if (y != mScrollY) {
            super.scrollTo(mScrollX, y);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        View currentFocused = findFocus();
        if (currentFocused == null || this == currentFocused || oldh <= h) {
            return;
        }
        // The viewport shrank (the keyboard came up): keep the focused field
        // in view, as Android does.
        Rect r = new Rect();
        offsetDescendantRect(currentFocused, r);
        int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(r);
        if (scrollDelta != 0) {
            doScrollY(scrollDelta);
        }
    }

    int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            scrollRange = Math.max(0, childHeightWithMargins(child) - (getHeight() - mPaddingBottom - mPaddingTop));
        }
        return scrollRange;
    }

    /// The child's height plus its vertical margins. `FrameLayout` lays the
    /// child out below its top margin, so every scroll range and clamp has to
    /// reach the bottom margin too, or the end of the content is unreachable.
    static int childHeightWithMargins(View child) {
        int height = child.getHeight();
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params instanceof MarginLayoutParams) {
            MarginLayoutParams lp = (MarginLayoutParams) params;
            height += lp.topMargin + lp.bottomMargin;
        }
        return height;
    }

    /// The child's width plus its horizontal margins; see [#childHeightWithMargins(View)].
    static int childWidthWithMargins(View child) {
        int width = child.getWidth();
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params instanceof MarginLayoutParams) {
            MarginLayoutParams lp = (MarginLayoutParams) params;
            width += lp.leftMargin + lp.rightMargin;
        }
        return width;
    }

    /// The child's bottom edge plus its bottom margin.
    static int childBottomWithMargin(View child) {
        int bottom = child.getBottom();
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params instanceof MarginLayoutParams) {
            bottom += ((MarginLayoutParams) params).bottomMargin;
        }
        return bottom;
    }

    @Override
    protected int computeVerticalScrollRange() {
        final int count = getChildCount();
        final int contentHeight = getHeight() - mPaddingBottom - mPaddingTop;
        if (count == 0) {
            return contentHeight;
        }
        int scrollRange = getChildAt(0).getBottom();
        final int scrollY = mScrollY;
        final int overscrollBottom = Math.max(0, scrollRange - contentHeight);
        if (scrollY < 0) {
            scrollRange -= scrollY;
        } else if (scrollY > overscrollBottom) {
            scrollRange += scrollY - overscrollBottom;
        }
        return scrollRange;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    // ------------------------------------------------------------ touch

    private boolean inChild(int x, int y) {
        if (getChildCount() > 0) {
            final int scrollY = mScrollY;
            final View child = getChildAt(0);
            return !(y < child.getTop() - scrollY || y >= child.getBottom() - scrollY
                    || x < child.getLeft() || x >= child.getRight());
        }
        return false;
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_MOVE && mIsBeingDragged) {
            return true;
        }
        if (super.onInterceptTouchEvent(ev)) {
            return true;
        }
        if (mScrollY == 0 && !canScrollVertically(1)) {
            return false;
        }
        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                final int y = (int) ev.getY();
                final int yDiff = Math.abs(y - mLastMotionY);
                if (yDiff > mTouchSlop) {
                    mIsBeingDragged = true;
                    mLastMotionY = y;
                    initVelocityTrackerIfNotExists();
                    mVelocityTracker.addMovement(ev);
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                final int y = (int) ev.getY();
                if (!inChild((int) ev.getX(), y)) {
                    mIsBeingDragged = false;
                    recycleVelocityTracker();
                    break;
                }
                mLastMotionY = y;
                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);
                mScroller.computeScrollOffset();
                mIsBeingDragged = !mScroller.isFinished();
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mIsBeingDragged = false;
                recycleVelocityTracker();
                break;
            default:
                break;
        }
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        initVelocityTrackerIfNotExists();
        final int actionMasked = ev.getActionMasked();
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                if (getChildCount() == 0) {
                    return false;
                }
                if ((mIsBeingDragged = !mScroller.isFinished())) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                    mAnimator.stop();
                }
                mLastMotionY = (int) ev.getY();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int y = (int) ev.getY();
                int deltaY = mLastMotionY - y;
                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mIsBeingDragged = true;
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
                }
                if (mIsBeingDragged) {
                    mLastMotionY = y;
                    doScrollY(deltaY);
                }
                break;
            }
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    mVelocityTracker.addMovement(ev);
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) mVelocityTracker.getYVelocity();
                    if (Math.abs(initialVelocity) > mMinimumVelocity) {
                        fling(-initialVelocity);
                    }
                    endDrag();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged && getChildCount() > 0) {
                    endDrag();
                }
                break;
            default:
                break;
        }
        if (mVelocityTracker != null && actionMasked != MotionEvent.ACTION_UP) {
            mVelocityTracker.addMovement(ev);
        }
        return true;
    }

    private void endDrag() {
        mIsBeingDragged = false;
        recycleVelocityTracker();
    }

    /// Scrolls by `delta`, clamped to the content.
    private void doScrollY(int delta) {
        if (delta != 0) {
            int range = getScrollRange();
            int newY = Math.max(0, Math.min(range, mScrollY + delta));
            scrollTo(mScrollX, newY);
        }
    }

    // ------------------------------------------------------------ programmatic scrolling

    public void fling(int velocityY) {
        if (getChildCount() > 0) {
            int height = getHeight() - mPaddingBottom - mPaddingTop;
            int bottom = childHeightWithMargins(getChildAt(0));
            mScroller.fling(mScrollX, mScrollY, 0, velocityY, 0, 0, 0, Math.max(0, bottom - height), 0, height / 2);
            mAnimator.start();
        }
    }

    /// One frame of a fling or smooth scroll; false when it has ended.
    boolean computeScrollStep() {
        if (mScroller.computeScrollOffset()) {
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            if (x != mScrollX || y != mScrollY) {
                scrollTo(x, y);
            }
            return !mScroller.isFinished();
        }
        return false;
    }

    @Override
    public void computeScroll() {
        computeScrollStep();
    }

    public final void smoothScrollBy(int dx, int dy) {
        if (getChildCount() == 0) {
            return;
        }
        long duration = SystemClock.uptimeMillis() - mLastScroll;
        if (duration > ANIMATED_SCROLL_GAP) {
            final int height = getHeight() - mPaddingBottom - mPaddingTop;
            final int bottom = childHeightWithMargins(getChildAt(0));
            final int maxY = Math.max(0, bottom - height);
            final int scrollY = mScrollY;
            dy = Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY;
            mScroller.startScroll(mScrollX, scrollY, 0, dy);
            mAnimator.start();
        } else {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
                mAnimator.stop();
            }
            scrollBy(dx, dy);
        }
        mLastScroll = SystemClock.uptimeMillis();
    }

    public final void smoothScrollTo(int x, int y) {
        smoothScrollBy(x - mScrollX, y - mScrollY);
    }

    @Override
    public void scrollTo(int x, int y) {
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            x = clamp(x, getWidth() - mPaddingRight - mPaddingLeft, childWidthWithMargins(child));
            y = clamp(y, getHeight() - mPaddingBottom - mPaddingTop, childHeightWithMargins(child));
            if (x != mScrollX || y != mScrollY) {
                mLastScrollActivity = SystemClock.uptimeMillis();
                super.scrollTo(x, y);
                scheduleScrollbarFade();
            }
        }
    }

    private static int clamp(int n, int my, int child) {
        if (my >= child || n < 0) {
            return 0;
        }
        if ((my + n) > child) {
            return child - my;
        }
        return n;
    }

    public boolean fullScroll(int direction) {
        boolean down = direction == View.FOCUS_DOWN;
        int height = getHeight();
        int target = 0;
        if (down && getChildCount() > 0) {
            target = Math.max(0, childBottomWithMargin(getChildAt(0)) + mPaddingBottom - height);
        }
        return scrollAndReport(target);
    }

    public boolean pageScroll(int direction) {
        boolean down = direction == View.FOCUS_DOWN;
        int height = getHeight();
        int target = down ? mScrollY + height : mScrollY - height;
        return scrollAndReport(Math.max(0, Math.min(getScrollRange(), target)));
    }

    public boolean arrowScroll(int direction) {
        int amount = Math.max(1, getMaxScrollAmount() / 4);
        int target = direction == View.FOCUS_UP ? mScrollY - amount : mScrollY + amount;
        return scrollAndReport(Math.max(0, Math.min(getScrollRange(), target)));
    }

    private boolean scrollAndReport(int targetY) {
        if (targetY == mScrollY) {
            return false;
        }
        if (mSmoothScrollingEnabled) {
            smoothScrollTo(mScrollX, targetY);
        } else {
            scrollTo(mScrollX, targetY);
        }
        return true;
    }

    // ------------------------------------------------------------ descendants

    private static boolean isViewDescendantOf(View child, View parent) {
        if (child == parent) {
            return true;
        }
        final ViewParent theParent = child.getParent();
        return (theParent instanceof View) && isViewDescendantOf((View) theParent, parent);
    }

    /// The bounds of `descendant`, in this view's content coordinates.
    void offsetDescendantRect(View descendant, Rect r) {
        r.set(0, 0, descendant.getWidth(), descendant.getHeight());
        View v = descendant;
        while (v != null && v != this) {
            r.offset(v.getLeft(), v.getTop());
            ViewParent p = v.getParent();
            if (!(p instanceof View)) {
                break;
            }
            v = (View) p;
            if (v != this) {
                r.offset(-v.getScrollX(), -v.getScrollY());
            }
        }
    }

    private void scrollToChild(View child) {
        Rect r = new Rect();
        offsetDescendantRect(child, r);
        int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(r);
        if (scrollDelta != 0) {
            scrollBy(0, scrollDelta);
        }
    }

    public void scrollToDescendant(View child) {
        if (!mIsLayoutDirty) {
            Rect r = new Rect();
            offsetDescendantRect(child, r);
            int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(r);
            if (scrollDelta != 0) {
                smoothScrollBy(0, scrollDelta);
            }
        } else {
            mChildToScrollTo = child;
        }
    }

    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        if (getChildCount() == 0) {
            return 0;
        }
        int height = getHeight();
        int screenTop = mScrollY;
        int screenBottom = screenTop + height;
        int fadingEdge = 0;
        if (rect.top > 0) {
            screenTop += fadingEdge;
        }
        if (rect.bottom < getChildAt(0).getHeight()) {
            screenBottom -= fadingEdge;
        }
        int scrollYDelta = 0;
        if (rect.bottom > screenBottom && rect.top > screenTop) {
            if (rect.height() > height) {
                scrollYDelta += (rect.top - screenTop);
            } else {
                scrollYDelta += (rect.bottom - screenBottom);
            }
            int bottom = childBottomWithMargin(getChildAt(0));
            int distanceToBottom = bottom - screenBottom;
            scrollYDelta = Math.min(scrollYDelta, distanceToBottom);
        } else if (rect.top < screenTop && rect.bottom < screenBottom) {
            if (rect.height() > height) {
                scrollYDelta -= (screenBottom - rect.bottom);
            } else {
                scrollYDelta -= (screenTop - rect.top);
            }
            scrollYDelta = Math.max(scrollYDelta, -mScrollY);
        }
        return scrollYDelta;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        if (focused != null) {
            if (!mIsLayoutDirty) {
                scrollToChild(focused);
            } else {
                mChildToScrollTo = focused;
            }
        }
        super.requestChildFocus(child, focused);
    }

    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        Rect r = new Rect();
        offsetDescendantRect(child, r);
        r.set(r.left + rectangle.left, r.top + rectangle.top, r.left + rectangle.right, r.top + rectangle.bottom);
        int delta = computeScrollDeltaToGetChildRectOnScreen(r);
        if (delta != 0) {
            if (immediate) {
                scrollBy(0, delta);
            } else {
                smoothScrollBy(0, delta);
            }
        }
        return delta != 0;
    }

    @Override
    public void requestLayout() {
        mIsLayoutDirty = true;
        super.requestLayout();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAnimator.stop();
        mScroller.abortAnimation();
    }

    // ------------------------------------------------------------ scrollbar

    private void scheduleScrollbarFade() {
        if (mScrollbarEnabled) {
            postInvalidateDelayed(SCROLLBAR_FADE_DELAY + SCROLLBAR_FADE_DURATION + 16);
        }
    }

    /// A thin thumb along the right edge while scrolling, fading out after.
    @Override
    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);
        if (!mScrollbarEnabled || getChildCount() == 0) {
            return;
        }
        long idle = SystemClock.uptimeMillis() - mLastScrollActivity;
        boolean active = mIsBeingDragged || !mScroller.isFinished();
        if (!active && idle > SCROLLBAR_FADE_DELAY + SCROLLBAR_FADE_DURATION) {
            return;
        }
        int range = computeVerticalScrollRange();
        int extent = computeVerticalScrollExtent();
        if (range <= extent) {
            return;
        }
        float alpha = active || idle <= SCROLLBAR_FADE_DELAY ? 1f
                : 1f - (idle - SCROLLBAR_FADE_DELAY) / (float) SCROLLBAR_FADE_DURATION;
        if (mScrollbarPaint == null) {
            mScrollbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }
        float density = getResources().getDisplayMetrics().density;
        int thickness = Math.max(2, Math.round(4 * density));
        int length = Math.max(thickness * 4, Math.round(getHeight() * (extent / (float) range)));
        int offset = Math.round((getHeight() - length) * (computeVerticalScrollOffset() / (float) (range - extent)));
        mScrollbarPaint.setColor((int) (0x66 * alpha) << 24);
        int left = mScrollX + getWidth() - thickness - Math.round(2 * density);
        int top = mScrollY + offset;
        canvas.drawRoundRect(left, top, left + thickness, top + length, thickness / 2f, thickness / 2f,
                mScrollbarPaint);
        if (!active) {
            postInvalidateDelayed(16);
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return ScrollView.class.getName();
    }
}
