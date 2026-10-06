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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

/// Arranges children in a single row or column, distributing extra space by
/// `layout_weight`. Measurement is AOSP's algorithm, so weights, `weightSum`,
/// `measureWithLargestChild`, gravity and baseline alignment behave as they
/// do on Android.
public class LinearLayout extends ViewGroup {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    public static final int SHOW_DIVIDER_NONE = 0;
    public static final int SHOW_DIVIDER_BEGINNING = 1;
    public static final int SHOW_DIVIDER_MIDDLE = 2;
    public static final int SHOW_DIVIDER_END = 4;

    private boolean mBaselineAligned = true;
    private int mBaselineAlignedChildIndex = -1;
    private int mBaselineChildTop;
    private int mOrientation = HORIZONTAL;
    private int mGravity = Gravity.START | Gravity.TOP;
    private int mTotalLength;
    private float mWeightSum = -1;
    private boolean mUseLargestChild;
    private int[] mMaxAscent;
    private int[] mMaxDescent;
    private Drawable mDivider;
    private int mDividerWidth;
    private int mDividerHeight;
    private int mShowDividers;
    private int mDividerPadding;

    private static final int VERTICAL_GRAVITY_COUNT = 4;
    private static final int INDEX_CENTER_VERTICAL = 0;
    private static final int INDEX_TOP = 1;
    private static final int INDEX_BOTTOM = 2;
    private static final int INDEX_FILL = 3;

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public float weight;
        public int gravity = -1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, android.R.styleable.LinearLayout_Layout);
            weight = a.getFloat(android.R.styleable.LinearLayout_Layout_layout_weight, 0);
            gravity = a.getInt(android.R.styleable.LinearLayout_Layout_layout_gravity, -1);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            weight = 0;
        }

        public LayoutParams(int width, int height, float weight) {
            super(width, height);
            this.weight = weight;
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            this.weight = source.weight;
            this.gravity = source.gravity;
        }
    }

    public LinearLayout(Context context) {
        super(context);
    }

    public LinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public LinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.LinearLayout, defStyleAttr, defStyleRes);
        int index = a.getInt(android.R.styleable.LinearLayout_orientation, -1);
        if (index >= 0) {
            setOrientation(index);
        }
        index = a.getInt(android.R.styleable.LinearLayout_gravity, -1);
        if (index >= 0) {
            setGravity(index);
        }
        mBaselineAligned = a.getBoolean(android.R.styleable.LinearLayout_baselineAligned, true);
        mWeightSum = a.getFloat(android.R.styleable.LinearLayout_weightSum, -1.0f);
        mBaselineAlignedChildIndex = a.getInt(android.R.styleable.LinearLayout_baselineAlignedChildIndex, -1);
        mUseLargestChild = a.getBoolean(android.R.styleable.LinearLayout_measureWithLargestChild, false);
        mShowDividers = a.getInt(android.R.styleable.LinearLayout_showDividers, SHOW_DIVIDER_NONE);
        mDividerPadding = a.getDimensionPixelSize(android.R.styleable.LinearLayout_dividerPadding, 0);
        setDividerDrawable(a.getDrawable(android.R.styleable.LinearLayout_divider));
        a.recycle();
    }

    public void setShowDividers(int showDividers) {
        if (showDividers != mShowDividers) {
            requestLayout();
        }
        mShowDividers = showDividers;
        setWillNotDraw(mDivider == null);
    }

    public int getShowDividers() {
        return mShowDividers;
    }

    public Drawable getDividerDrawable() {
        return mDivider;
    }

    public void setDividerDrawable(Drawable divider) {
        if (divider == mDivider) {
            return;
        }
        mDivider = divider;
        if (divider != null) {
            mDividerWidth = Math.max(1, divider.getIntrinsicWidth());
            mDividerHeight = Math.max(1, divider.getIntrinsicHeight());
        } else {
            mDividerWidth = 0;
            mDividerHeight = 0;
        }
        setWillNotDraw(divider == null);
        requestLayout();
    }

    public void setDividerPadding(int padding) {
        mDividerPadding = padding;
    }

    public int getDividerPadding() {
        return mDividerPadding;
    }

    public int getDividerWidth() {
        return mDividerWidth;
    }

    public boolean isBaselineAligned() {
        return mBaselineAligned;
    }

    public void setBaselineAligned(boolean baselineAligned) {
        mBaselineAligned = baselineAligned;
    }

    public boolean isMeasureWithLargestChildEnabled() {
        return mUseLargestChild;
    }

    public void setMeasureWithLargestChildEnabled(boolean enabled) {
        mUseLargestChild = enabled;
    }

    public int getBaselineAlignedChildIndex() {
        return mBaselineAlignedChildIndex;
    }

    public void setBaselineAlignedChildIndex(int i) {
        mBaselineAlignedChildIndex = i;
    }

    public float getWeightSum() {
        return mWeightSum;
    }

    public void setWeightSum(float weightSum) {
        mWeightSum = Math.max(0.0f, weightSum);
    }

    public void setOrientation(int orientation) {
        if (mOrientation != orientation) {
            mOrientation = orientation;
            requestLayout();
        }
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
                gravity |= Gravity.START;
            }
            if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
                gravity |= Gravity.TOP;
            }
            mGravity = gravity;
            requestLayout();
        }
    }

    public int getGravity() {
        return mGravity;
    }

    public void setHorizontalGravity(int horizontalGravity) {
        final int gravity = horizontalGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        if ((mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) != gravity) {
            mGravity = (mGravity & ~Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) | gravity;
            requestLayout();
        }
    }

    public void setVerticalGravity(int verticalGravity) {
        final int gravity = verticalGravity & Gravity.VERTICAL_GRAVITY_MASK;
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != gravity) {
            mGravity = (mGravity & ~Gravity.VERTICAL_GRAVITY_MASK) | gravity;
            requestLayout();
        }
    }

    @Override
    public int getBaseline() {
        if (mBaselineAlignedChildIndex < 0) {
            return super.getBaseline();
        }
        if (getChildCount() <= mBaselineAlignedChildIndex) {
            throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout set to an index that is out of bounds.");
        }
        final View child = getChildAt(mBaselineAlignedChildIndex);
        final int childBaseline = child.getBaseline();
        if (childBaseline == -1) {
            if (mBaselineAlignedChildIndex == 0) {
                return -1;
            }
            throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout points to a View that doesn't know how to get its baseline.");
        }
        int childTop = mBaselineChildTop;
        if (mOrientation == VERTICAL) {
            final int majorGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
            if (majorGravity != Gravity.TOP) {
                switch (majorGravity) {
                    case Gravity.BOTTOM:
                        childTop = mBottom - mTop - mPaddingBottom - mTotalLength;
                        break;
                    case Gravity.CENTER_VERTICAL:
                        childTop += ((mBottom - mTop - mPaddingTop - mPaddingBottom) - mTotalLength) / 2;
                        break;
                    default:
                        break;
                }
            }
        }
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        return childTop + lp.topMargin + childBaseline;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        if (mOrientation == HORIZONTAL) {
            return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) lp);
        } else if (lp instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) lp);
        }
        return new LayoutParams(lp);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    View getVirtualChildAt(int index) {
        return getChildAt(index);
    }

    int getVirtualChildCount() {
        return getChildCount();
    }

    private boolean hasDividerBeforeChildAt(int childIndex) {
        if (mDivider == null) {
            return false;
        }
        if (childIndex == getVirtualChildCount()) {
            return (mShowDividers & SHOW_DIVIDER_END) != 0;
        }
        boolean allViewsAreGoneBefore = true;
        for (int i = childIndex - 1; i >= 0; i--) {
            if (getVirtualChildAt(i).getVisibility() != GONE) {
                allViewsAreGoneBefore = false;
                break;
            }
        }
        if (allViewsAreGoneBefore) {
            return (mShowDividers & SHOW_DIVIDER_BEGINNING) != 0;
        }
        return (mShowDividers & SHOW_DIVIDER_MIDDLE) != 0;
    }

    /// The space the dividers take along the orientation. The first measure
    /// pass counts them while it walks the children; every later pass that
    /// rebuilds `mTotalLength` from the children alone must add them back, or
    /// gravity reads the divider space as free and pushes the children over
    /// the edge. (Android's own weighted pass forgets them; we do not.)
    private int dividerLength(int count, int dividerSize) {
        if (mDivider == null) {
            return 0;
        }
        int total = 0;
        boolean anyVisible = false;
        for (int i = 0; i < count; i++) {
            final View child = getVirtualChildAt(i);
            if (child == null || child.getVisibility() == GONE) {
                continue;
            }
            anyVisible = true;
            if (hasDividerBeforeChildAt(i)) {
                total += dividerSize;
            }
        }
        if (anyVisible && hasDividerBeforeChildAt(count)) {
            total += dividerSize;
        }
        return total;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mOrientation == VERTICAL) {
            measureVertical(widthMeasureSpec, heightMeasureSpec);
        } else {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec);
        }
    }

    void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        mTotalLength = 0;
        int maxWidth = 0;
        int childState = 0;
        int alternativeMaxWidth = 0;
        int weightedMaxWidth = 0;
        boolean allFillParent = true;
        float totalWeight = 0;
        final int count = getVirtualChildCount();
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        boolean matchWidth = false;
        boolean skippedMeasure = false;
        final int baselineChildIndex = mBaselineAlignedChildIndex;
        final boolean useLargestChild = mUseLargestChild;
        int largestChildHeight = Integer.MIN_VALUE;
        int consumedExcessSpace = 0;
        int nonSkippedChildCount = 0;

        for (int i = 0; i < count; ++i) {
            final View child = getVirtualChildAt(i);
            if (child == null || child.getVisibility() == View.GONE) {
                continue;
            }
            nonSkippedChildCount++;
            if (hasDividerBeforeChildAt(i)) {
                mTotalLength += mDividerHeight;
            }
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            totalWeight += lp.weight;
            final boolean useExcessSpace = lp.height == 0 && lp.weight > 0;
            if (heightMode == MeasureSpec.EXACTLY && useExcessSpace) {
                final int totalLength = mTotalLength;
                mTotalLength = Math.max(totalLength, totalLength + lp.topMargin + lp.bottomMargin);
                skippedMeasure = true;
            } else {
                if (useExcessSpace) {
                    lp.height = LayoutParams.WRAP_CONTENT;
                }
                final int usedHeight = totalWeight == 0 ? mTotalLength : 0;
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, usedHeight);
                final int childHeight = child.getMeasuredHeight();
                if (useExcessSpace) {
                    lp.height = 0;
                    consumedExcessSpace += childHeight;
                }
                final int totalLength = mTotalLength;
                mTotalLength = Math.max(totalLength, totalLength + childHeight + lp.topMargin + lp.bottomMargin);
                if (useLargestChild) {
                    largestChildHeight = Math.max(childHeight, largestChildHeight);
                }
            }
            if ((baselineChildIndex >= 0) && (baselineChildIndex == i + 1)) {
                mBaselineChildTop = mTotalLength;
            }
            boolean matchWidthLocally = false;
            if (widthMode != MeasureSpec.EXACTLY && lp.width == LayoutParams.MATCH_PARENT) {
                matchWidth = true;
                matchWidthLocally = true;
            }
            final int margin = lp.leftMargin + lp.rightMargin;
            final int measuredWidth = child.getMeasuredWidth() + margin;
            maxWidth = Math.max(maxWidth, measuredWidth);
            childState = combineMeasuredStates(childState, child.getMeasuredState());
            allFillParent = allFillParent && lp.width == LayoutParams.MATCH_PARENT;
            if (lp.weight > 0) {
                weightedMaxWidth = Math.max(weightedMaxWidth, matchWidthLocally ? margin : measuredWidth);
            } else {
                alternativeMaxWidth = Math.max(alternativeMaxWidth, matchWidthLocally ? margin : measuredWidth);
            }
        }

        if (nonSkippedChildCount > 0 && hasDividerBeforeChildAt(count)) {
            mTotalLength += mDividerHeight;
        }

        if (useLargestChild && (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.UNSPECIFIED)) {
            mTotalLength = 0;
            for (int i = 0; i < count; ++i) {
                final View child = getVirtualChildAt(i);
                if (child == null || child.getVisibility() == GONE) {
                    continue;
                }
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                final int totalLength = mTotalLength;
                mTotalLength = Math.max(totalLength, totalLength + largestChildHeight + lp.topMargin + lp.bottomMargin);
            }
            mTotalLength += dividerLength(count, mDividerHeight);
        }

        mTotalLength += mPaddingTop + mPaddingBottom;
        int heightSize = mTotalLength;
        heightSize = Math.max(heightSize, getSuggestedMinimumHeight());
        int heightSizeAndState = resolveSizeAndState(heightSize, heightMeasureSpec, 0);
        heightSize = heightSizeAndState & MEASURED_SIZE_MASK;
        int remainingExcess = heightSize - mTotalLength + consumedExcessSpace;
        if (skippedMeasure || (remainingExcess != 0 && totalWeight > 0.0f)) {
            float remainingWeightSum = mWeightSum > 0.0f ? mWeightSum : totalWeight;
            mTotalLength = 0;
            for (int i = 0; i < count; ++i) {
                final View child = getVirtualChildAt(i);
                if (child == null || child.getVisibility() == View.GONE) {
                    continue;
                }
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                final float childWeight = lp.weight;
                if (childWeight > 0) {
                    final int share = (int) (childWeight * remainingExcess / remainingWeightSum);
                    remainingExcess -= share;
                    remainingWeightSum -= childWeight;
                    final int childHeight;
                    if (mUseLargestChild && heightMode != MeasureSpec.EXACTLY) {
                        childHeight = largestChildHeight;
                    } else if (lp.height == 0) {
                        childHeight = share;
                    } else {
                        childHeight = child.getMeasuredHeight() + share;
                    }
                    final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(Math.max(0, childHeight),
                            MeasureSpec.EXACTLY);
                    final int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                            mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin, lp.width);
                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                    childState = combineMeasuredStates(childState,
                            child.getMeasuredState() & (MEASURED_STATE_MASK >> MEASURED_HEIGHT_STATE_SHIFT));
                }
                final int margin = lp.leftMargin + lp.rightMargin;
                final int measuredWidth = child.getMeasuredWidth() + margin;
                maxWidth = Math.max(maxWidth, measuredWidth);
                boolean matchWidthLocally = widthMode != MeasureSpec.EXACTLY && lp.width == LayoutParams.MATCH_PARENT;
                alternativeMaxWidth = Math.max(alternativeMaxWidth, matchWidthLocally ? margin : measuredWidth);
                allFillParent = allFillParent && lp.width == LayoutParams.MATCH_PARENT;
                final int totalLength = mTotalLength;
                mTotalLength = Math.max(totalLength, totalLength + child.getMeasuredHeight() + lp.topMargin
                        + lp.bottomMargin);
            }
            mTotalLength += dividerLength(count, mDividerHeight);
            mTotalLength += mPaddingTop + mPaddingBottom;
        } else {
            alternativeMaxWidth = Math.max(alternativeMaxWidth, weightedMaxWidth);
            if (useLargestChild && heightMode != MeasureSpec.EXACTLY) {
                for (int i = 0; i < count; i++) {
                    final View child = getVirtualChildAt(i);
                    if (child == null || child.getVisibility() == View.GONE) {
                        continue;
                    }
                    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    if (lp.weight > 0) {
                        child.measure(MeasureSpec.makeMeasureSpec(child.getMeasuredWidth(), MeasureSpec.EXACTLY),
                                MeasureSpec.makeMeasureSpec(largestChildHeight, MeasureSpec.EXACTLY));
                    }
                }
            }
        }

        if (!allFillParent && widthMode != MeasureSpec.EXACTLY) {
            maxWidth = alternativeMaxWidth;
        }
        maxWidth += mPaddingLeft + mPaddingRight;
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState), heightSizeAndState);
        if (matchWidth) {
            forceUniformWidth(count, heightMeasureSpec);
        }
    }

    private void forceUniformWidth(int count, int heightMeasureSpec) {
        int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY);
        for (int i = 0; i < count; ++i) {
            final View child = getVirtualChildAt(i);
            if (child != null && child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.width == LayoutParams.MATCH_PARENT) {
                    int oldHeight = lp.height;
                    lp.height = child.getMeasuredHeight();
                    measureChildWithMargins(child, uniformMeasureSpec, 0, heightMeasureSpec, 0);
                    lp.height = oldHeight;
                }
            }
        }
    }

    void measureHorizontal(int widthMeasureSpec, int heightMeasureSpec) {
        mTotalLength = 0;
        int maxHeight = 0;
        int childState = 0;
        int alternativeMaxHeight = 0;
        int weightedMaxHeight = 0;
        boolean allFillParent = true;
        float totalWeight = 0;
        final int count = getVirtualChildCount();
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        boolean matchHeight = false;
        boolean skippedMeasure = false;
        if (mMaxAscent == null || mMaxDescent == null) {
            mMaxAscent = new int[VERTICAL_GRAVITY_COUNT];
            mMaxDescent = new int[VERTICAL_GRAVITY_COUNT];
        }
        final int[] maxAscent = mMaxAscent;
        final int[] maxDescent = mMaxDescent;
        maxAscent[0] = maxAscent[1] = maxAscent[2] = maxAscent[3] = -1;
        maxDescent[0] = maxDescent[1] = maxDescent[2] = maxDescent[3] = -1;
        final boolean baselineAligned = mBaselineAligned;
        final boolean useLargestChild = mUseLargestChild;
        final boolean isExactly = widthMode == MeasureSpec.EXACTLY;
        int largestChildWidth = Integer.MIN_VALUE;
        int usedExcessSpace = 0;
        int nonSkippedChildCount = 0;

        for (int i = 0; i < count; ++i) {
            final View child = getVirtualChildAt(i);
            if (child == null || child.getVisibility() == GONE) {
                continue;
            }
            nonSkippedChildCount++;
            if (hasDividerBeforeChildAt(i)) {
                mTotalLength += mDividerWidth;
            }
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            totalWeight += lp.weight;
            final boolean useExcessSpace = lp.width == 0 && lp.weight > 0;
            if (widthMode == MeasureSpec.EXACTLY && useExcessSpace) {
                if (isExactly) {
                    mTotalLength += lp.leftMargin + lp.rightMargin;
                } else {
                    final int totalLength = mTotalLength;
                    mTotalLength = Math.max(totalLength, totalLength + lp.leftMargin + lp.rightMargin);
                }
                if (baselineAligned) {
                    final int freeWidthSpec = MeasureSpec.makeSafeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec),
                            MeasureSpec.UNSPECIFIED);
                    final int freeHeightSpec = MeasureSpec.makeSafeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec),
                            MeasureSpec.UNSPECIFIED);
                    child.measure(freeWidthSpec, freeHeightSpec);
                } else {
                    skippedMeasure = true;
                }
            } else {
                if (useExcessSpace) {
                    lp.width = LayoutParams.WRAP_CONTENT;
                }
                final int usedWidth = totalWeight == 0 ? mTotalLength : 0;
                measureChildWithMargins(child, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);
                final int childWidth = child.getMeasuredWidth();
                if (useExcessSpace) {
                    lp.width = 0;
                    usedExcessSpace += childWidth;
                }
                if (isExactly) {
                    mTotalLength += childWidth + lp.leftMargin + lp.rightMargin;
                } else {
                    final int totalLength = mTotalLength;
                    mTotalLength = Math.max(totalLength, totalLength + childWidth + lp.leftMargin + lp.rightMargin);
                }
                if (useLargestChild) {
                    largestChildWidth = Math.max(childWidth, largestChildWidth);
                }
            }
            boolean matchHeightLocally = false;
            if (heightMode != MeasureSpec.EXACTLY && lp.height == LayoutParams.MATCH_PARENT) {
                matchHeight = true;
                matchHeightLocally = true;
            }
            final int margin = lp.topMargin + lp.bottomMargin;
            final int childHeight = child.getMeasuredHeight() + margin;
            childState = combineMeasuredStates(childState, child.getMeasuredState());
            if (baselineAligned) {
                final int childBaseline = child.getBaseline();
                if (childBaseline != -1) {
                    final int gravity = (lp.gravity < 0 ? mGravity : lp.gravity) & Gravity.VERTICAL_GRAVITY_MASK;
                    final int index = ((gravity >> Gravity.AXIS_Y_SHIFT) & ~Gravity.AXIS_SPECIFIED) >> 1;
                    maxAscent[index] = Math.max(maxAscent[index], childBaseline);
                    maxDescent[index] = Math.max(maxDescent[index], childHeight - childBaseline);
                }
            }
            maxHeight = Math.max(maxHeight, childHeight);
            allFillParent = allFillParent && lp.height == LayoutParams.MATCH_PARENT;
            if (lp.weight > 0) {
                weightedMaxHeight = Math.max(weightedMaxHeight, matchHeightLocally ? margin : childHeight);
            } else {
                alternativeMaxHeight = Math.max(alternativeMaxHeight, matchHeightLocally ? margin : childHeight);
            }
        }

        if (nonSkippedChildCount > 0 && hasDividerBeforeChildAt(count)) {
            mTotalLength += mDividerWidth;
        }
        if (maxAscent[INDEX_TOP] != -1 || maxAscent[INDEX_CENTER_VERTICAL] != -1 || maxAscent[INDEX_BOTTOM] != -1
                || maxAscent[INDEX_FILL] != -1) {
            final int ascent = Math.max(maxAscent[INDEX_FILL], Math.max(maxAscent[INDEX_CENTER_VERTICAL],
                    Math.max(maxAscent[INDEX_TOP], maxAscent[INDEX_BOTTOM])));
            final int descent = Math.max(maxDescent[INDEX_FILL], Math.max(maxDescent[INDEX_CENTER_VERTICAL],
                    Math.max(maxDescent[INDEX_TOP], maxDescent[INDEX_BOTTOM])));
            maxHeight = Math.max(maxHeight, ascent + descent);
        }
        if (useLargestChild && (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED)) {
            mTotalLength = 0;
            for (int i = 0; i < count; ++i) {
                final View child = getVirtualChildAt(i);
                if (child == null || child.getVisibility() == GONE) {
                    continue;
                }
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (isExactly) {
                    mTotalLength += largestChildWidth + lp.leftMargin + lp.rightMargin;
                } else {
                    final int totalLength = mTotalLength;
                    mTotalLength = Math.max(totalLength, totalLength + largestChildWidth + lp.leftMargin
                            + lp.rightMargin);
                }
            }
            mTotalLength += dividerLength(count, mDividerWidth);
        }
        mTotalLength += mPaddingLeft + mPaddingRight;
        int widthSize = mTotalLength;
        widthSize = Math.max(widthSize, getSuggestedMinimumWidth());
        int widthSizeAndState = resolveSizeAndState(widthSize, widthMeasureSpec, 0);
        widthSize = widthSizeAndState & MEASURED_SIZE_MASK;
        int remainingExcess = widthSize - mTotalLength + usedExcessSpace;
        if (skippedMeasure || (remainingExcess != 0 && totalWeight > 0.0f)) {
            float remainingWeightSum = mWeightSum > 0.0f ? mWeightSum : totalWeight;
            maxAscent[0] = maxAscent[1] = maxAscent[2] = maxAscent[3] = -1;
            maxDescent[0] = maxDescent[1] = maxDescent[2] = maxDescent[3] = -1;
            maxHeight = -1;
            mTotalLength = 0;
            for (int i = 0; i < count; ++i) {
                final View child = getVirtualChildAt(i);
                if (child == null || child.getVisibility() == View.GONE) {
                    continue;
                }
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                final float childWeight = lp.weight;
                if (childWeight > 0) {
                    final int share = (int) (childWeight * remainingExcess / remainingWeightSum);
                    remainingExcess -= share;
                    remainingWeightSum -= childWeight;
                    final int childWidth;
                    if (mUseLargestChild && widthMode != MeasureSpec.EXACTLY) {
                        childWidth = largestChildWidth;
                    } else if (lp.width == 0) {
                        childWidth = share;
                    } else {
                        childWidth = child.getMeasuredWidth() + share;
                    }
                    final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(Math.max(0, childWidth),
                            MeasureSpec.EXACTLY);
                    final int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                            mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin, lp.height);
                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                    childState = combineMeasuredStates(childState, child.getMeasuredState() & MEASURED_STATE_MASK);
                }
                if (isExactly) {
                    mTotalLength += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
                } else {
                    final int totalLength = mTotalLength;
                    mTotalLength = Math.max(totalLength, totalLength + child.getMeasuredWidth() + lp.leftMargin
                            + lp.rightMargin);
                }
                boolean matchHeightLocally = heightMode != MeasureSpec.EXACTLY && lp.height == LayoutParams.MATCH_PARENT;
                final int margin = lp.topMargin + lp.bottomMargin;
                int childHeight = child.getMeasuredHeight() + margin;
                maxHeight = Math.max(maxHeight, childHeight);
                alternativeMaxHeight = Math.max(alternativeMaxHeight, matchHeightLocally ? margin : childHeight);
                allFillParent = allFillParent && lp.height == LayoutParams.MATCH_PARENT;
                if (baselineAligned) {
                    final int childBaseline = child.getBaseline();
                    if (childBaseline != -1) {
                        final int gravity = (lp.gravity < 0 ? mGravity : lp.gravity) & Gravity.VERTICAL_GRAVITY_MASK;
                        final int index = ((gravity >> Gravity.AXIS_Y_SHIFT) & ~Gravity.AXIS_SPECIFIED) >> 1;
                        maxAscent[index] = Math.max(maxAscent[index], childBaseline);
                        maxDescent[index] = Math.max(maxDescent[index], childHeight - childBaseline);
                    }
                }
            }
            mTotalLength += dividerLength(count, mDividerWidth);
            mTotalLength += mPaddingLeft + mPaddingRight;
            if (maxAscent[INDEX_TOP] != -1 || maxAscent[INDEX_CENTER_VERTICAL] != -1
                    || maxAscent[INDEX_BOTTOM] != -1 || maxAscent[INDEX_FILL] != -1) {
                final int ascent = Math.max(maxAscent[INDEX_FILL], Math.max(maxAscent[INDEX_CENTER_VERTICAL],
                        Math.max(maxAscent[INDEX_TOP], maxAscent[INDEX_BOTTOM])));
                final int descent = Math.max(maxDescent[INDEX_FILL], Math.max(maxDescent[INDEX_CENTER_VERTICAL],
                        Math.max(maxDescent[INDEX_TOP], maxDescent[INDEX_BOTTOM])));
                maxHeight = Math.max(maxHeight, ascent + descent);
            }
        } else {
            alternativeMaxHeight = Math.max(alternativeMaxHeight, weightedMaxHeight);
            if (useLargestChild && widthMode != MeasureSpec.EXACTLY) {
                for (int i = 0; i < count; i++) {
                    final View child = getVirtualChildAt(i);
                    if (child == null || child.getVisibility() == View.GONE) {
                        continue;
                    }
                    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    if (lp.weight > 0) {
                        child.measure(MeasureSpec.makeMeasureSpec(largestChildWidth, MeasureSpec.EXACTLY),
                                MeasureSpec.makeMeasureSpec(child.getMeasuredHeight(), MeasureSpec.EXACTLY));
                    }
                }
            }
        }
        if (!allFillParent && heightMode != MeasureSpec.EXACTLY) {
            maxHeight = alternativeMaxHeight;
        }
        maxHeight += mPaddingTop + mPaddingBottom;
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        setMeasuredDimension(widthSizeAndState | (childState & MEASURED_STATE_MASK),
                resolveSizeAndState(maxHeight, heightMeasureSpec, (childState << MEASURED_HEIGHT_STATE_SHIFT)));
        if (matchHeight) {
            forceUniformHeight(count, widthMeasureSpec);
        }
    }

    private void forceUniformHeight(int count, int widthMeasureSpec) {
        int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY);
        for (int i = 0; i < count; ++i) {
            final View child = getVirtualChildAt(i);
            if (child != null && child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.height == LayoutParams.MATCH_PARENT) {
                    int oldWidth = lp.width;
                    lp.width = child.getMeasuredWidth();
                    measureChildWithMargins(child, widthMeasureSpec, 0, uniformMeasureSpec, 0);
                    lp.width = oldWidth;
                }
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mOrientation == VERTICAL) {
            layoutVertical(l, t, r, b);
        } else {
            layoutHorizontal(l, t, r, b);
        }
    }

    void layoutVertical(int left, int top, int right, int bottom) {
        final int paddingLeft = mPaddingLeft;
        int childTop;
        int childLeft;
        final int width = right - left;
        int childRight = width - mPaddingRight;
        int childSpace = width - paddingLeft - mPaddingRight;
        final int count = getVirtualChildCount();
        final int majorGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        final int minorGravity = mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        switch (majorGravity) {
            case Gravity.BOTTOM:
                childTop = mPaddingTop + bottom - top - mTotalLength;
                break;
            case Gravity.CENTER_VERTICAL:
                childTop = mPaddingTop + (bottom - top - mTotalLength) / 2;
                break;
            default:
                childTop = mPaddingTop;
                break;
        }
        for (int i = 0; i < count; i++) {
            final View child = getVirtualChildAt(i);
            if (child == null || child.getVisibility() == GONE) {
                continue;
            }
            final int childWidth = child.getMeasuredWidth();
            final int childHeight = child.getMeasuredHeight();
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int gravity = lp.gravity;
            if (gravity < 0) {
                gravity = minorGravity;
            }
            final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, getLayoutDirection());
            switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                case Gravity.CENTER_HORIZONTAL:
                    childLeft = paddingLeft + ((childSpace - childWidth) / 2) + lp.leftMargin - lp.rightMargin;
                    break;
                case Gravity.RIGHT:
                    childLeft = childRight - childWidth - lp.rightMargin;
                    break;
                default:
                    childLeft = paddingLeft + lp.leftMargin;
                    break;
            }
            if (hasDividerBeforeChildAt(i)) {
                childTop += mDividerHeight;
            }
            childTop += lp.topMargin;
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            childTop += childHeight + lp.bottomMargin;
        }
    }

    void layoutHorizontal(int left, int top, int right, int bottom) {
        final boolean isLayoutRtl = isLayoutRtl();
        final int paddingTop = mPaddingTop;
        int childTop;
        int childLeft;
        final int height = bottom - top;
        int childBottom = height - mPaddingBottom;
        int childSpace = height - paddingTop - mPaddingBottom;
        final int count = getVirtualChildCount();
        final int majorGravity = mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        final int minorGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        final boolean baselineAligned = mBaselineAligned;
        final int[] maxAscent = mMaxAscent;
        final int[] maxDescent = mMaxDescent;
        switch (Gravity.getAbsoluteGravity(majorGravity, getLayoutDirection())) {
            case Gravity.RIGHT:
                childLeft = mPaddingLeft + right - left - mTotalLength;
                break;
            case Gravity.CENTER_HORIZONTAL:
                childLeft = mPaddingLeft + (right - left - mTotalLength) / 2;
                break;
            default:
                childLeft = mPaddingLeft;
                break;
        }
        int start = 0;
        int dir = 1;
        if (isLayoutRtl) {
            start = count - 1;
            dir = -1;
        }
        for (int i = 0; i < count; i++) {
            final int childIndex = start + dir * i;
            final View child = getVirtualChildAt(childIndex);
            if (child == null || child.getVisibility() == GONE) {
                continue;
            }
            final int childWidth = child.getMeasuredWidth();
            final int childHeight = child.getMeasuredHeight();
            int childBaseline = -1;
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (baselineAligned && lp.height != LayoutParams.MATCH_PARENT) {
                childBaseline = child.getBaseline();
            }
            int gravity = lp.gravity;
            if (gravity < 0) {
                gravity = minorGravity;
            }
            switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
                case Gravity.TOP:
                    childTop = paddingTop + lp.topMargin;
                    if (childBaseline != -1 && maxAscent != null) {
                        childTop += maxAscent[INDEX_TOP] - childBaseline;
                    }
                    break;
                case Gravity.CENTER_VERTICAL:
                    childTop = paddingTop + ((childSpace - childHeight) / 2) + lp.topMargin - lp.bottomMargin;
                    break;
                case Gravity.BOTTOM:
                    childTop = childBottom - childHeight - lp.bottomMargin;
                    if (childBaseline != -1 && maxDescent != null) {
                        int descent = child.getMeasuredHeight() - childBaseline;
                        childTop -= (maxDescent[INDEX_BOTTOM] - descent);
                    }
                    break;
                default:
                    childTop = paddingTop;
                    break;
            }
            if (hasDividerBeforeChildAt(childIndex)) {
                childLeft += mDividerWidth;
            }
            childLeft += lp.leftMargin;
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            childLeft += childWidth + lp.rightMargin;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mDivider == null) {
            return;
        }
        if (mOrientation == VERTICAL) {
            drawDividersVertical(canvas);
        } else {
            drawDividersHorizontal(canvas);
        }
    }

    void drawDividersVertical(Canvas canvas) {
        final int count = getVirtualChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getVirtualChildAt(i);
            if (child != null && child.getVisibility() != GONE && hasDividerBeforeChildAt(i)) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                drawHorizontalDivider(canvas, child.getTop() - lp.topMargin - mDividerHeight);
            }
        }
        if (hasDividerBeforeChildAt(count)) {
            View last = null;
            for (int i = count - 1; i >= 0; i--) {
                View c = getVirtualChildAt(i);
                if (c != null && c.getVisibility() != GONE) {
                    last = c;
                    break;
                }
            }
            int bottom = last == null ? getHeight() - mPaddingBottom - mDividerHeight
                    : last.getBottom() + ((LayoutParams) last.getLayoutParams()).bottomMargin;
            drawHorizontalDivider(canvas, bottom);
        }
    }

    void drawDividersHorizontal(Canvas canvas) {
        final int count = getVirtualChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getVirtualChildAt(i);
            if (child != null && child.getVisibility() != GONE && hasDividerBeforeChildAt(i)) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                drawVerticalDivider(canvas, child.getLeft() - lp.leftMargin - mDividerWidth);
            }
        }
        if (hasDividerBeforeChildAt(count)) {
            // layoutHorizontal() reserves the ending divider's space after the
            // rightmost child: the last child, or the first one in RTL, where
            // the children are laid out in reverse.
            final boolean rtl = isLayoutRtl();
            View last = null;
            for (int i = 0; i < count; i++) {
                View c = getVirtualChildAt(rtl ? i : count - 1 - i);
                if (c != null && c.getVisibility() != GONE) {
                    last = c;
                    break;
                }
            }
            int left = last == null ? getWidth() - mPaddingRight - mDividerWidth
                    : last.getRight() + ((LayoutParams) last.getLayoutParams()).rightMargin;
            drawVerticalDivider(canvas, left);
        }
    }

    void drawHorizontalDivider(Canvas canvas, int top) {
        mDivider.setBounds(mPaddingLeft + mDividerPadding, top, getWidth() - mPaddingRight - mDividerPadding,
                top + mDividerHeight);
        mDivider.draw(canvas);
    }

    void drawVerticalDivider(Canvas canvas, int left) {
        mDivider.setBounds(left, mPaddingTop + mDividerPadding, left + mDividerWidth,
                getHeight() - mPaddingBottom - mDividerPadding);
        mDivider.draw(canvas);
    }

    /// A plain layout never scrolls, so a child's press and drag start at once.
    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public CharSequence getAccessibilityClassName() {
        return LinearLayout.class.getName();
    }
}
