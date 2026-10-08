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

/// A horizontally scrolling container with a single child: the
/// [ScrollView] model on the X axis. The child is measured with an
/// unspecified width (or stretched to the viewport when `fillViewport`).
public class HorizontalScrollView extends FrameLayout {

    static final int ANIMATED_SCROLL_GAP = 250;
    static final float MAX_SCROLL_FACTOR = 0.5f;
    private static final int SCROLLBAR_FADE_DELAY = 500;
    private static final int SCROLLBAR_FADE_DURATION = 250;

    private long mLastScroll;
    private OverScroller mScroller;
    private ScrollAnimator mAnimator;
    private boolean mIsBeingDragged;
    private VelocityTracker mVelocityTracker;
    private int mLastMotionX;
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

    public HorizontalScrollView(Context context) {
        this(context, null);
    }

    public HorizontalScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.horizontalScrollViewStyle);
    }

    public HorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public HorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.HorizontalScrollView, defStyleAttr,
                defStyleRes);
        setFillViewport(a.getBoolean(android.R.styleable.HorizontalScrollView_fillViewport, false));
        a.recycle();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public int getMaxScrollAmount() {
        return (int) (MAX_SCROLL_FACTOR * (mRight - mLeft));
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("HorizontalScrollView can host only one direct child");
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
    public void setHorizontalScrollBarEnabled(boolean enabled) {
        mScrollbarEnabled = enabled;
    }

    // ------------------------------------------------------------ measure & layout

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!mFillViewport) {
            return;
        }
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            return;
        }
        if (getChildCount() > 0) {
            final View child = getChildAt(0);
            final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
            final int widthPadding = mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin;
            final int heightPadding = mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin;
            final int desiredWidth = getMeasuredWidth() - widthPadding;
            if (child.getMeasuredWidth() < desiredWidth) {
                final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(desiredWidth, MeasureSpec.EXACTLY);
                final int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, heightPadding, lp.height);
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec, mPaddingTop + mPaddingBottom,
                lp.height);
        final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                Math.max(0, MeasureSpec.getSize(parentWidthMeasureSpec) - mPaddingLeft - mPaddingRight),
                MeasureSpec.UNSPECIFIED);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin + heightUsed, lp.height);
        final int usedTotal = mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin + widthUsed;
        final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                Math.max(0, MeasureSpec.getSize(parentWidthMeasureSpec) - usedTotal), MeasureSpec.UNSPECIFIED);
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
        int x = Math.max(0, Math.min(scrollRange, mScrollX));
        if (x != mScrollX) {
            super.scrollTo(x, mScrollY);
        }
    }

    int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            scrollRange = Math.max(0, ScrollView.childWidthWithMargins(child)
                    - (getWidth() - mPaddingLeft - mPaddingRight));
        }
        return scrollRange;
    }

    /// The child's right edge plus its right margin: `FrameLayout` lays the
    /// child out past its left margin, so the scroll range must reach both.
    private static int childRightWithMargin(View child) {
        int right = child.getRight();
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params instanceof MarginLayoutParams) {
            right += ((MarginLayoutParams) params).rightMargin;
        }
        return right;
    }

    @Override
    protected int computeHorizontalScrollRange() {
        final int count = getChildCount();
        final int contentWidth = getWidth() - mPaddingLeft - mPaddingRight;
        if (count == 0) {
            return contentWidth;
        }
        int scrollRange = getChildAt(0).getRight();
        final int scrollX = mScrollX;
        final int overscrollRight = Math.max(0, scrollRange - contentWidth);
        if (scrollX < 0) {
            scrollRange -= scrollX;
        } else if (scrollX > overscrollRight) {
            scrollRange += scrollX - overscrollRight;
        }
        return scrollRange;
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return Math.max(0, super.computeHorizontalScrollOffset());
    }

    // ------------------------------------------------------------ touch

    private boolean inChild(int x, int y) {
        if (getChildCount() > 0) {
            final int scrollX = mScrollX;
            final View child = getChildAt(0);
            return !(y < child.getTop() || y >= child.getBottom() || x < child.getLeft() - scrollX
                    || x >= child.getRight() - scrollX);
        }
        return false;
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
        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                final int x = (int) ev.getX();
                if (Math.abs(x - mLastMotionX) > mTouchSlop) {
                    mIsBeingDragged = true;
                    mLastMotionX = x;
                    if (mVelocityTracker == null) {
                        mVelocityTracker = VelocityTracker.obtain();
                    }
                    mVelocityTracker.addMovement(ev);
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                final int x = (int) ev.getX();
                if (!inChild(x, (int) ev.getY())) {
                    mIsBeingDragged = false;
                    recycleVelocityTracker();
                    break;
                }
                mLastMotionX = x;
                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                } else {
                    mVelocityTracker.clear();
                }
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
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
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
                mLastMotionX = (int) ev.getX();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int x = (int) ev.getX();
                int deltaX = mLastMotionX - x;
                if (!mIsBeingDragged && Math.abs(deltaX) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mIsBeingDragged = true;
                    deltaX += deltaX > 0 ? -mTouchSlop : mTouchSlop;
                }
                if (mIsBeingDragged) {
                    mLastMotionX = x;
                    doScrollX(deltaX);
                }
                break;
            }
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    mVelocityTracker.addMovement(ev);
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) mVelocityTracker.getXVelocity();
                    if (getChildCount() > 0 && Math.abs(initialVelocity) > mMinimumVelocity) {
                        fling(-initialVelocity);
                    }
                    mIsBeingDragged = false;
                    recycleVelocityTracker();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                recycleVelocityTracker();
                break;
            default:
                break;
        }
        if (mVelocityTracker != null && actionMasked != MotionEvent.ACTION_UP) {
            mVelocityTracker.addMovement(ev);
        }
        return true;
    }

    private void doScrollX(int delta) {
        if (delta != 0) {
            int newX = Math.max(0, Math.min(getScrollRange(), mScrollX + delta));
            scrollTo(newX, mScrollY);
        }
    }

    // ------------------------------------------------------------ programmatic scrolling

    public void fling(int velocityX) {
        if (getChildCount() > 0) {
            int width = getWidth() - mPaddingRight - mPaddingLeft;
            int right = ScrollView.childWidthWithMargins(getChildAt(0));
            mScroller.fling(mScrollX, mScrollY, velocityX, 0, 0, Math.max(0, right - width), 0, 0, width / 2, 0);
            mAnimator.start();
        }
    }

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
            final int width = getWidth() - mPaddingRight - mPaddingLeft;
            final int right = ScrollView.childWidthWithMargins(getChildAt(0));
            final int maxX = Math.max(0, right - width);
            final int scrollX = mScrollX;
            dx = Math.max(0, Math.min(scrollX + dx, maxX)) - scrollX;
            mScroller.startScroll(scrollX, mScrollY, dx, 0);
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
            x = clamp(x, getWidth() - mPaddingRight - mPaddingLeft, ScrollView.childWidthWithMargins(child));
            y = clamp(y, getHeight() - mPaddingBottom - mPaddingTop, ScrollView.childHeightWithMargins(child));
            if (x != mScrollX || y != mScrollY) {
                mLastScrollActivity = SystemClock.uptimeMillis();
                super.scrollTo(x, y);
                if (mScrollbarEnabled) {
                    postInvalidateDelayed(SCROLLBAR_FADE_DELAY + SCROLLBAR_FADE_DURATION + 16);
                }
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
        int target = 0;
        if (direction == View.FOCUS_RIGHT && getChildCount() > 0) {
            target = Math.max(0, childRightWithMargin(getChildAt(0)) + mPaddingRight - getWidth());
        }
        return scrollAndReport(target);
    }

    public boolean pageScroll(int direction) {
        int target = direction == View.FOCUS_RIGHT ? mScrollX + getWidth() : mScrollX - getWidth();
        return scrollAndReport(Math.max(0, Math.min(getScrollRange(), target)));
    }

    public boolean arrowScroll(int direction) {
        int amount = Math.max(1, getMaxScrollAmount() / 4);
        int target = direction == View.FOCUS_LEFT ? mScrollX - amount : mScrollX + amount;
        return scrollAndReport(Math.max(0, Math.min(getScrollRange(), target)));
    }

    private boolean scrollAndReport(int targetX) {
        if (targetX == mScrollX) {
            return false;
        }
        if (mSmoothScrollingEnabled) {
            smoothScrollTo(targetX, mScrollY);
        } else {
            scrollTo(targetX, mScrollY);
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
            scrollBy(scrollDelta, 0);
        }
    }

    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        if (getChildCount() == 0) {
            return 0;
        }
        int width = getWidth();
        int screenLeft = mScrollX;
        int screenRight = screenLeft + width;
        int scrollXDelta = 0;
        if (rect.right > screenRight && rect.left > screenLeft) {
            if (rect.width() > width) {
                scrollXDelta += (rect.left - screenLeft);
            } else {
                scrollXDelta += (rect.right - screenRight);
            }
            int right = childRightWithMargin(getChildAt(0));
            scrollXDelta = Math.min(scrollXDelta, right - screenRight);
        } else if (rect.left < screenLeft && rect.right < screenRight) {
            if (rect.width() > width) {
                scrollXDelta -= (screenRight - rect.right);
            } else {
                scrollXDelta -= (screenLeft - rect.left);
            }
            scrollXDelta = Math.max(scrollXDelta, -mScrollX);
        }
        return scrollXDelta;
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
                scrollBy(delta, 0);
            } else {
                smoothScrollBy(delta, 0);
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

    /// A thin thumb along the bottom edge while scrolling, fading out after.
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
        int range = computeHorizontalScrollRange();
        int extent = computeHorizontalScrollExtent();
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
        int length = Math.max(thickness * 4, Math.round(getWidth() * (extent / (float) range)));
        int offset = Math.round((getWidth() - length) * (computeHorizontalScrollOffset() / (float) (range - extent)));
        mScrollbarPaint.setColor((int) (0x66 * alpha) << 24);
        int top = mScrollY + getHeight() - thickness - Math.round(2 * density);
        int left = mScrollX + offset;
        canvas.drawRoundRect(left, top, left + length, top + thickness, thickness / 2f, thickness / 2f,
                mScrollbarPaint);
        if (!active) {
            postInvalidateDelayed(16);
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return HorizontalScrollView.class.getName();
    }
}
