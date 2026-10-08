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
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/// Positions children relative to each other and to the parent. AOSP's
/// algorithm: children are sorted by their dependencies twice -- once over
/// the horizontal rules, once over the vertical ones -- and measured and
/// positioned in that order, so a view's anchors are always placed first.
public class RelativeLayout extends ViewGroup {

    public static final int TRUE = -1;
    public static final int LEFT_OF = 0;
    public static final int RIGHT_OF = 1;
    public static final int ABOVE = 2;
    public static final int BELOW = 3;
    public static final int ALIGN_BASELINE = 4;
    public static final int ALIGN_LEFT = 5;
    public static final int ALIGN_TOP = 6;
    public static final int ALIGN_RIGHT = 7;
    public static final int ALIGN_BOTTOM = 8;
    public static final int ALIGN_PARENT_LEFT = 9;
    public static final int ALIGN_PARENT_TOP = 10;
    public static final int ALIGN_PARENT_RIGHT = 11;
    public static final int ALIGN_PARENT_BOTTOM = 12;
    public static final int CENTER_IN_PARENT = 13;
    public static final int CENTER_HORIZONTAL = 14;
    public static final int CENTER_VERTICAL = 15;
    public static final int START_OF = 16;
    public static final int END_OF = 17;
    public static final int ALIGN_START = 18;
    public static final int ALIGN_END = 19;
    public static final int ALIGN_PARENT_START = 20;
    public static final int ALIGN_PARENT_END = 21;

    private static final int VERB_COUNT = 22;

    private static final int[] RULES_VERTICAL = {ABOVE, BELOW, ALIGN_BASELINE, ALIGN_TOP, ALIGN_BOTTOM};
    private static final int[] RULES_HORIZONTAL = {LEFT_OF, RIGHT_OF, ALIGN_LEFT, ALIGN_RIGHT, START_OF, END_OF,
        ALIGN_START, ALIGN_END};

    private static final int VALUE_NOT_SET = Integer.MIN_VALUE;

    private View mBaselineView;
    private int mGravity = Gravity.START | Gravity.TOP;
    private final Rect mContentBounds = new Rect();
    private final Rect mSelfBounds = new Rect();
    private int mIgnoreGravity = View.NO_ID;
    private View[] mSortedHorizontalChildren = new View[0];
    private View[] mSortedVerticalChildren = new View[0];
    private final Map<Integer, View> mKeyViews = new HashMap<Integer, View>();

    /// Per-child rules, resolved against the layout direction, plus the
    /// frame computed during measurement.
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        private final int[] mRules = new int[VERB_COUNT];
        private final int[] mInitialRules = new int[VERB_COUNT];
        int mLeft;
        int mTop;
        int mRight;
        int mBottom;
        public boolean alignWithParent;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, android.R.styleable.RelativeLayout_Layout);
            for (int i = 0, n = a.getIndexCount(); i < n; i++) {
                int attr = a.getIndex(i);
                int verb = verbFor(attr);
                if (attr == android.R.styleable.RelativeLayout_Layout_layout_alignWithParentIfMissing) {
                    alignWithParent = a.getBoolean(attr, false);
                } else if (verb >= 0) {
                    if (isAnchorVerb(verb)) {
                        mInitialRules[verb] = a.getResourceId(attr, 0);
                    } else {
                        mInitialRules[verb] = a.getBoolean(attr, false) ? TRUE : 0;
                    }
                }
            }
            a.recycle();
            System.arraycopy(mInitialRules, 0, mRules, 0, VERB_COUNT);
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            alignWithParent = source.alignWithParent;
            System.arraycopy(source.mRules, 0, mRules, 0, VERB_COUNT);
            System.arraycopy(source.mInitialRules, 0, mInitialRules, 0, VERB_COUNT);
        }

        private static boolean isAnchorVerb(int verb) {
            return verb <= ALIGN_BOTTOM || (verb >= START_OF && verb <= ALIGN_END);
        }

        private static int verbFor(int attr) {
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_toLeftOf) {
                return LEFT_OF;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_toRightOf) {
                return RIGHT_OF;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_above) {
                return ABOVE;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_below) {
                return BELOW;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_alignBaseline) {
                return ALIGN_BASELINE;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_alignLeft) {
                return ALIGN_LEFT;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_alignTop) {
                return ALIGN_TOP;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_alignRight) {
                return ALIGN_RIGHT;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_alignBottom) {
                return ALIGN_BOTTOM;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_alignParentLeft) {
                return ALIGN_PARENT_LEFT;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_alignParentTop) {
                return ALIGN_PARENT_TOP;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_alignParentRight) {
                return ALIGN_PARENT_RIGHT;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_alignParentBottom) {
                return ALIGN_PARENT_BOTTOM;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_centerInParent) {
                return CENTER_IN_PARENT;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_centerHorizontal) {
                return CENTER_HORIZONTAL;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_centerVertical) {
                return CENTER_VERTICAL;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_toStartOf) {
                return START_OF;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_toEndOf) {
                return END_OF;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_alignStart) {
                return ALIGN_START;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_alignEnd) {
                return ALIGN_END;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_alignParentStart) {
                return ALIGN_PARENT_START;
            }
            if (attr == android.R.styleable.RelativeLayout_Layout_layout_alignParentEnd) {
                return ALIGN_PARENT_END;
            }
            return -1;
        }

        public void addRule(int verb) {
            addRule(verb, TRUE);
        }

        public void addRule(int verb, int subject) {
            mRules[verb] = subject;
            mInitialRules[verb] = subject;
        }

        public void removeRule(int verb) {
            addRule(verb, 0);
        }

        public int getRule(int verb) {
            return mRules[verb];
        }

        public int[] getRules() {
            return mRules;
        }

        /// The rules with start/end resolved to left/right for `layoutDirection`.
        public int[] getRules(int layoutDirection) {
            resolveRules(layoutDirection);
            return mRules;
        }

        private void resolveRules(int layoutDirection) {
            boolean rtl = layoutDirection == View.LAYOUT_DIRECTION_RTL;
            System.arraycopy(mInitialRules, 0, mRules, 0, VERB_COUNT);
            if ((mRules[ALIGN_START] != 0 || mRules[ALIGN_END] != 0)
                    && (mRules[ALIGN_LEFT] != 0 || mRules[ALIGN_RIGHT] != 0)) {
                mRules[ALIGN_LEFT] = 0;
                mRules[ALIGN_RIGHT] = 0;
            }
            if (mRules[ALIGN_START] != 0) {
                mRules[rtl ? ALIGN_RIGHT : ALIGN_LEFT] = mRules[ALIGN_START];
                mRules[ALIGN_START] = 0;
            }
            if (mRules[ALIGN_END] != 0) {
                mRules[rtl ? ALIGN_LEFT : ALIGN_RIGHT] = mRules[ALIGN_END];
                mRules[ALIGN_END] = 0;
            }
            if ((mRules[START_OF] != 0 || mRules[END_OF] != 0) && (mRules[LEFT_OF] != 0 || mRules[RIGHT_OF] != 0)) {
                mRules[LEFT_OF] = 0;
                mRules[RIGHT_OF] = 0;
            }
            if (mRules[START_OF] != 0) {
                mRules[rtl ? RIGHT_OF : LEFT_OF] = mRules[START_OF];
                mRules[START_OF] = 0;
            }
            if (mRules[END_OF] != 0) {
                mRules[rtl ? LEFT_OF : RIGHT_OF] = mRules[END_OF];
                mRules[END_OF] = 0;
            }
            if ((mRules[ALIGN_PARENT_START] != 0 || mRules[ALIGN_PARENT_END] != 0)
                    && (mRules[ALIGN_PARENT_LEFT] != 0 || mRules[ALIGN_PARENT_RIGHT] != 0)) {
                mRules[ALIGN_PARENT_LEFT] = 0;
                mRules[ALIGN_PARENT_RIGHT] = 0;
            }
            if (mRules[ALIGN_PARENT_START] != 0) {
                mRules[rtl ? ALIGN_PARENT_RIGHT : ALIGN_PARENT_LEFT] = mRules[ALIGN_PARENT_START];
                mRules[ALIGN_PARENT_START] = 0;
            }
            if (mRules[ALIGN_PARENT_END] != 0) {
                mRules[rtl ? ALIGN_PARENT_LEFT : ALIGN_PARENT_RIGHT] = mRules[ALIGN_PARENT_END];
                mRules[ALIGN_PARENT_END] = 0;
            }
        }

        @Override
        public void resolveLayoutDirection(int layoutDirection) {
            super.resolveLayoutDirection(layoutDirection);
            if (mInitialRules != null) {
                resolveRules(layoutDirection);
            }
        }
    }

    public RelativeLayout(Context context) {
        super(context);
    }

    public RelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.RelativeLayout, defStyleAttr,
                defStyleRes);
        mIgnoreGravity = a.getResourceId(android.R.styleable.RelativeLayout_ignoreGravity, View.NO_ID);
        mGravity = a.getInt(android.R.styleable.RelativeLayout_gravity, mGravity);
        a.recycle();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public void setIgnoreGravity(int viewId) {
        mIgnoreGravity = viewId;
    }

    public int getIgnoreGravity() {
        return mIgnoreGravity;
    }

    public int getGravity() {
        return mGravity;
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
        return mBaselineView != null ? mBaselineView.getBaseline() + ((LayoutParams) mBaselineView.getLayoutParams()).mTop
                : super.getBaseline();
    }

    // ------------------------------------------------------------ dependency sort

    /// Kahn's algorithm over the anchor rules in `filter`: a child comes after
    /// every sibling it is anchored to.
    private View[] sort(int[] filter) {
        int count = getChildCount();
        View[] out = new View[count];
        int[] pending = new int[count];
        ArrayList<ArrayList<Integer>> dependents = new ArrayList<ArrayList<Integer>>(count);
        Map<View, Integer> index = new HashMap<View, Integer>();
        for (int i = 0; i < count; i++) {
            dependents.add(new ArrayList<Integer>());
            index.put(getChildAt(i), Integer.valueOf(i));
        }
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int[] rules = ((LayoutParams) child.getLayoutParams()).getRules();
            for (int verb : filter) {
                int id = rules[verb];
                if (id > 0) {
                    View anchor = mKeyViews.get(Integer.valueOf(id));
                    if (anchor == null || anchor == child) {
                        continue;
                    }
                    Integer ai = index.get(anchor);
                    if (ai != null && !dependents.get(ai.intValue()).contains(Integer.valueOf(i))) {
                        dependents.get(ai.intValue()).add(Integer.valueOf(i));
                        pending[i]++;
                    }
                }
            }
        }
        ArrayList<Integer> queue = new ArrayList<Integer>();
        for (int i = 0; i < count; i++) {
            if (pending[i] == 0) {
                queue.add(Integer.valueOf(i));
            }
        }
        int n = 0;
        while (!queue.isEmpty()) {
            int i = queue.remove(0).intValue();
            out[n++] = getChildAt(i);
            for (Integer d : dependents.get(i)) {
                if (--pending[d.intValue()] == 0) {
                    queue.add(d);
                }
            }
        }
        if (n < count) {
            throw new IllegalStateException("Circular dependencies cannot exist in RelativeLayout");
        }
        return out;
    }

    private void sortChildren() {
        mKeyViews.clear();
        int dir = getLayoutDirection();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int id = child.getId();
            if (id != View.NO_ID) {
                mKeyViews.put(Integer.valueOf(id), child);
            }
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp instanceof LayoutParams) {
                ((LayoutParams) lp).getRules(dir);
            }
        }
        mSortedVerticalChildren = sort(RULES_VERTICAL);
        mSortedHorizontalChildren = sort(RULES_HORIZONTAL);
    }

    // ------------------------------------------------------------ measure

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        sortChildren();
        int myWidth = -1;
        int myHeight = -1;
        int width = 0;
        int height = 0;
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode != MeasureSpec.UNSPECIFIED) {
            myWidth = widthSize;
        }
        if (heightMode != MeasureSpec.UNSPECIFIED) {
            myHeight = heightSize;
        }
        if (widthMode == MeasureSpec.EXACTLY) {
            width = myWidth;
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            height = myHeight;
        }
        View ignore = null;
        int gravity = mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        final boolean horizontalGravity = gravity != Gravity.START && gravity != 0;
        gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        final boolean verticalGravity = gravity != Gravity.TOP && gravity != 0;
        int left = Integer.MAX_VALUE;
        int top = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int bottom = Integer.MIN_VALUE;
        boolean offsetHorizontalAxis = false;
        boolean offsetVerticalAxis = false;
        if ((horizontalGravity || verticalGravity) && mIgnoreGravity != View.NO_ID) {
            ignore = findViewById(mIgnoreGravity);
        }
        final boolean isWrapContentWidth = widthMode != MeasureSpec.EXACTLY;
        final boolean isWrapContentHeight = heightMode != MeasureSpec.EXACTLY;
        final int layoutDirection = getLayoutDirection();
        final boolean rtl = isLayoutRtl();

        for (View child : mSortedHorizontalChildren) {
            if (child.getVisibility() != GONE) {
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                int[] rules = params.getRules(layoutDirection);
                applyHorizontalSizeRules(params, myWidth, rules);
                measureChildHorizontal(child, params, myWidth, myHeight);
                if (positionChildHorizontal(child, params, myWidth, isWrapContentWidth)) {
                    offsetHorizontalAxis = true;
                }
            }
        }
        for (View child : mSortedVerticalChildren) {
            if (child.getVisibility() != GONE) {
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                applyVerticalSizeRules(params, myHeight, child.getBaseline());
                measureChild(child, params, myWidth, myHeight);
                if (positionChildVertical(child, params, myHeight, isWrapContentHeight)) {
                    offsetVerticalAxis = true;
                }
                if (isWrapContentWidth) {
                    if (rtl) {
                        width = Math.max(width, myWidth - params.mLeft + params.leftMargin);
                    } else {
                        width = Math.max(width, params.mRight + params.rightMargin);
                    }
                }
                if (isWrapContentHeight) {
                    height = Math.max(height, params.mBottom + params.bottomMargin);
                }
                if (child != ignore || verticalGravity) {
                    left = Math.min(left, params.mLeft - params.leftMargin);
                    top = Math.min(top, params.mTop - params.topMargin);
                }
                if (child != ignore || horizontalGravity) {
                    right = Math.max(right, params.mRight + params.rightMargin);
                    bottom = Math.max(bottom, params.mBottom + params.bottomMargin);
                }
            }
        }

        View baselineView = null;
        LayoutParams baselineParams = null;
        for (View child : mSortedVerticalChildren) {
            if (child.getVisibility() != GONE && child.getBaseline() != -1) {
                LayoutParams p = (LayoutParams) child.getLayoutParams();
                if (baselineView == null || p.mTop < baselineParams.mTop
                        || (p.mTop == baselineParams.mTop && p.mLeft < baselineParams.mLeft)) {
                    baselineView = child;
                    baselineParams = p;
                }
            }
        }
        mBaselineView = baselineView;

        if (isWrapContentWidth) {
            width += mPaddingRight;
            if (mLayoutParams != null && mLayoutParams.width >= 0) {
                width = Math.max(width, mLayoutParams.width);
            }
            width = Math.max(width, getSuggestedMinimumWidth());
            width = resolveSize(width, widthMeasureSpec);
            if (offsetHorizontalAxis) {
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    if (child.getVisibility() != GONE) {
                        LayoutParams params = (LayoutParams) child.getLayoutParams();
                        int[] rules = params.getRules(layoutDirection);
                        if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_HORIZONTAL] != 0) {
                            centerHorizontal(child, params, width);
                        } else if (rules[ALIGN_PARENT_RIGHT] != 0) {
                            final int childWidth = child.getMeasuredWidth();
                            params.mLeft = width - mPaddingRight - childWidth;
                            params.mRight = params.mLeft + childWidth;
                        }
                    }
                }
            }
        }
        if (isWrapContentHeight) {
            height += mPaddingBottom;
            if (mLayoutParams != null && mLayoutParams.height >= 0) {
                height = Math.max(height, mLayoutParams.height);
            }
            height = Math.max(height, getSuggestedMinimumHeight());
            height = resolveSize(height, heightMeasureSpec);
            if (offsetVerticalAxis) {
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    if (child.getVisibility() != GONE) {
                        LayoutParams params = (LayoutParams) child.getLayoutParams();
                        int[] rules = params.getRules(layoutDirection);
                        if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_VERTICAL] != 0) {
                            centerVertical(child, params, height);
                        } else if (rules[ALIGN_PARENT_BOTTOM] != 0) {
                            final int childHeight = child.getMeasuredHeight();
                            params.mTop = height - mPaddingBottom - childHeight;
                            params.mBottom = params.mTop + childHeight;
                        }
                    }
                }
            }
        }

        if ((horizontalGravity || verticalGravity) && left != Integer.MAX_VALUE) {
            final Rect selfBounds = mSelfBounds;
            selfBounds.set(mPaddingLeft, mPaddingTop, width - mPaddingRight, height - mPaddingBottom);
            final Rect contentBounds = mContentBounds;
            Gravity.apply(mGravity, right - left, bottom - top, selfBounds, contentBounds, layoutDirection);
            final int horizontalOffset = contentBounds.left - left;
            final int verticalOffset = contentBounds.top - top;
            if (horizontalOffset != 0 || verticalOffset != 0) {
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    if (child.getVisibility() != GONE && child != ignore) {
                        LayoutParams params = (LayoutParams) child.getLayoutParams();
                        if (horizontalGravity) {
                            params.mLeft += horizontalOffset;
                            params.mRight += horizontalOffset;
                        }
                        if (verticalGravity) {
                            params.mTop += verticalOffset;
                            params.mBottom += verticalOffset;
                        }
                    }
                }
            }
        }

        if (rtl) {
            final int offsetWidth = myWidth - width;
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getVisibility() != GONE) {
                    LayoutParams params = (LayoutParams) child.getLayoutParams();
                    params.mLeft -= offsetWidth;
                    params.mRight -= offsetWidth;
                }
            }
        }
        setMeasuredDimension(width, height);
    }

    private void measureChild(View child, LayoutParams params, int myWidth, int myHeight) {
        int childWidthMeasureSpec = getChildMeasureSpec(params.mLeft, params.mRight, params.width, params.leftMargin,
                params.rightMargin, mPaddingLeft, mPaddingRight, myWidth);
        int childHeightMeasureSpec = getChildMeasureSpec(params.mTop, params.mBottom, params.height,
                params.topMargin, params.bottomMargin, mPaddingTop, mPaddingBottom, myHeight);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    private void measureChildHorizontal(View child, LayoutParams params, int myWidth, int myHeight) {
        final int childWidthMeasureSpec = getChildMeasureSpec(params.mLeft, params.mRight, params.width,
                params.leftMargin, params.rightMargin, mPaddingLeft, mPaddingRight, myWidth);
        final int childHeightMeasureSpec;
        if (myHeight < 0) {
            if (params.height >= 0) {
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY);
            } else {
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            }
        } else {
            final int maxHeight = Math.max(0, myHeight - mPaddingTop - mPaddingBottom - params.topMargin
                    - params.bottomMargin);
            final int heightMode = params.height == LayoutParams.MATCH_PARENT ? MeasureSpec.EXACTLY
                    : MeasureSpec.AT_MOST;
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, heightMode);
        }
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    private int getChildMeasureSpec(int childStart, int childEnd, int childSize, int startMargin, int endMargin,
                                    int startPadding, int endPadding, int mySize) {
        int childSpecMode = 0;
        int childSpecSize = 0;
        final boolean isUnspecified = mySize < 0;
        if (isUnspecified) {
            if (childStart != VALUE_NOT_SET && childEnd != VALUE_NOT_SET) {
                childSpecSize = Math.max(0, childEnd - childStart);
                childSpecMode = MeasureSpec.EXACTLY;
            } else if (childSize >= 0) {
                childSpecSize = childSize;
                childSpecMode = MeasureSpec.EXACTLY;
            } else {
                childSpecSize = 0;
                childSpecMode = MeasureSpec.UNSPECIFIED;
            }
            return MeasureSpec.makeMeasureSpec(childSpecSize, childSpecMode);
        }
        int tempStart = childStart;
        int tempEnd = childEnd;
        if (tempStart == VALUE_NOT_SET) {
            tempStart = startPadding + startMargin;
        }
        if (tempEnd == VALUE_NOT_SET) {
            tempEnd = mySize - endPadding - endMargin;
        }
        final int maxAvailable = tempEnd - tempStart;
        if (childStart != VALUE_NOT_SET && childEnd != VALUE_NOT_SET) {
            childSpecMode = MeasureSpec.EXACTLY;
            childSpecSize = Math.max(0, maxAvailable);
        } else {
            if (childSize >= 0) {
                childSpecMode = MeasureSpec.EXACTLY;
                childSpecSize = maxAvailable >= 0 ? Math.min(maxAvailable, childSize) : childSize;
            } else if (childSize == LayoutParams.MATCH_PARENT) {
                childSpecMode = MeasureSpec.EXACTLY;
                childSpecSize = Math.max(0, maxAvailable);
            } else if (childSize == LayoutParams.WRAP_CONTENT) {
                if (maxAvailable >= 0) {
                    childSpecMode = MeasureSpec.AT_MOST;
                    childSpecSize = maxAvailable;
                } else {
                    childSpecMode = MeasureSpec.UNSPECIFIED;
                    childSpecSize = 0;
                }
            }
        }
        return MeasureSpec.makeMeasureSpec(childSpecSize, childSpecMode);
    }

    private boolean positionChildHorizontal(View child, LayoutParams params, int myWidth, boolean wrapContent) {
        final int[] rules = params.getRules(getLayoutDirection());
        if (params.mLeft == VALUE_NOT_SET && params.mRight != VALUE_NOT_SET) {
            params.mLeft = params.mRight - child.getMeasuredWidth();
        } else if (params.mLeft != VALUE_NOT_SET && params.mRight == VALUE_NOT_SET) {
            params.mRight = params.mLeft + child.getMeasuredWidth();
        } else if (params.mLeft == VALUE_NOT_SET && params.mRight == VALUE_NOT_SET) {
            if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_HORIZONTAL] != 0) {
                if (!wrapContent) {
                    centerHorizontal(child, params, myWidth);
                } else {
                    positionAtEdge(child, params, myWidth);
                }
                return true;
            }
            positionAtEdge(child, params, myWidth);
        }
        return rules[ALIGN_PARENT_RIGHT] != 0;
    }

    private void positionAtEdge(View child, LayoutParams params, int myWidth) {
        if (isLayoutRtl()) {
            params.mRight = myWidth - mPaddingRight - params.rightMargin;
            params.mLeft = params.mRight - child.getMeasuredWidth();
        } else {
            params.mLeft = mPaddingLeft + params.leftMargin;
            params.mRight = params.mLeft + child.getMeasuredWidth();
        }
    }

    private boolean positionChildVertical(View child, LayoutParams params, int myHeight, boolean wrapContent) {
        int[] rules = params.getRules();
        if (params.mTop == VALUE_NOT_SET && params.mBottom != VALUE_NOT_SET) {
            params.mTop = params.mBottom - child.getMeasuredHeight();
        } else if (params.mTop != VALUE_NOT_SET && params.mBottom == VALUE_NOT_SET) {
            params.mBottom = params.mTop + child.getMeasuredHeight();
        } else if (params.mTop == VALUE_NOT_SET && params.mBottom == VALUE_NOT_SET) {
            if (rules[CENTER_IN_PARENT] != 0 || rules[CENTER_VERTICAL] != 0) {
                if (!wrapContent) {
                    centerVertical(child, params, myHeight);
                } else {
                    params.mTop = mPaddingTop + params.topMargin;
                    params.mBottom = params.mTop + child.getMeasuredHeight();
                }
                return true;
            }
            params.mTop = mPaddingTop + params.topMargin;
            params.mBottom = params.mTop + child.getMeasuredHeight();
        }
        return rules[ALIGN_PARENT_BOTTOM] != 0;
    }

    private void applyHorizontalSizeRules(LayoutParams childParams, int myWidth, int[] rules) {
        LayoutParams anchorParams;
        childParams.mLeft = VALUE_NOT_SET;
        childParams.mRight = VALUE_NOT_SET;
        anchorParams = getRelatedViewParams(rules, LEFT_OF);
        if (anchorParams != null) {
            childParams.mRight = anchorParams.mLeft - (anchorParams.leftMargin + childParams.rightMargin);
        } else if (childParams.alignWithParent && rules[LEFT_OF] != 0) {
            if (myWidth >= 0) {
                childParams.mRight = myWidth - mPaddingRight - childParams.rightMargin;
            }
        }
        anchorParams = getRelatedViewParams(rules, RIGHT_OF);
        if (anchorParams != null) {
            childParams.mLeft = anchorParams.mRight + (anchorParams.rightMargin + childParams.leftMargin);
        } else if (childParams.alignWithParent && rules[RIGHT_OF] != 0) {
            childParams.mLeft = mPaddingLeft + childParams.leftMargin;
        }
        anchorParams = getRelatedViewParams(rules, ALIGN_LEFT);
        if (anchorParams != null) {
            childParams.mLeft = anchorParams.mLeft + childParams.leftMargin;
        } else if (childParams.alignWithParent && rules[ALIGN_LEFT] != 0) {
            childParams.mLeft = mPaddingLeft + childParams.leftMargin;
        }
        anchorParams = getRelatedViewParams(rules, ALIGN_RIGHT);
        if (anchorParams != null) {
            childParams.mRight = anchorParams.mRight - childParams.rightMargin;
        } else if (childParams.alignWithParent && rules[ALIGN_RIGHT] != 0) {
            if (myWidth >= 0) {
                childParams.mRight = myWidth - mPaddingRight - childParams.rightMargin;
            }
        }
        if (rules[ALIGN_PARENT_LEFT] != 0) {
            childParams.mLeft = mPaddingLeft + childParams.leftMargin;
        }
        if (rules[ALIGN_PARENT_RIGHT] != 0) {
            if (myWidth >= 0) {
                childParams.mRight = myWidth - mPaddingRight - childParams.rightMargin;
            }
        }
    }

    private void applyVerticalSizeRules(LayoutParams childParams, int myHeight, int myBaseline) {
        final int[] rules = childParams.getRules();
        int baselineOffset = getRelatedViewBaselineOffset(rules);
        if (baselineOffset != -1) {
            if (myBaseline != -1) {
                baselineOffset -= myBaseline;
            }
            childParams.mTop = baselineOffset;
            childParams.mBottom = VALUE_NOT_SET;
            return;
        }
        LayoutParams anchorParams;
        childParams.mTop = VALUE_NOT_SET;
        childParams.mBottom = VALUE_NOT_SET;
        anchorParams = getRelatedViewParams(rules, ABOVE);
        if (anchorParams != null) {
            childParams.mBottom = anchorParams.mTop - (anchorParams.topMargin + childParams.bottomMargin);
        } else if (childParams.alignWithParent && rules[ABOVE] != 0) {
            if (myHeight >= 0) {
                childParams.mBottom = myHeight - mPaddingBottom - childParams.bottomMargin;
            }
        }
        anchorParams = getRelatedViewParams(rules, BELOW);
        if (anchorParams != null) {
            childParams.mTop = anchorParams.mBottom + (anchorParams.bottomMargin + childParams.topMargin);
        } else if (childParams.alignWithParent && rules[BELOW] != 0) {
            childParams.mTop = mPaddingTop + childParams.topMargin;
        }
        anchorParams = getRelatedViewParams(rules, ALIGN_TOP);
        if (anchorParams != null) {
            childParams.mTop = anchorParams.mTop + childParams.topMargin;
        } else if (childParams.alignWithParent && rules[ALIGN_TOP] != 0) {
            childParams.mTop = mPaddingTop + childParams.topMargin;
        }
        anchorParams = getRelatedViewParams(rules, ALIGN_BOTTOM);
        if (anchorParams != null) {
            childParams.mBottom = anchorParams.mBottom - childParams.bottomMargin;
        } else if (childParams.alignWithParent && rules[ALIGN_BOTTOM] != 0) {
            if (myHeight >= 0) {
                childParams.mBottom = myHeight - mPaddingBottom - childParams.bottomMargin;
            }
        }
        if (rules[ALIGN_PARENT_TOP] != 0) {
            childParams.mTop = mPaddingTop + childParams.topMargin;
        }
        if (rules[ALIGN_PARENT_BOTTOM] != 0) {
            if (myHeight >= 0) {
                childParams.mBottom = myHeight - mPaddingBottom - childParams.bottomMargin;
            }
        }
    }

    /// The sibling `rules[relation]` names; when it is GONE, the view it is
    /// in turn anchored to by the same relation, as Android does.
    private View getRelatedView(int[] rules, int relation) {
        int id = rules[relation];
        if (id == 0) {
            return null;
        }
        View v = mKeyViews.get(Integer.valueOf(id));
        int guard = 0;
        while (v != null && v.getVisibility() == View.GONE) {
            if (++guard > getChildCount() || !(v.getLayoutParams() instanceof LayoutParams)) {
                return null;
            }
            int[] r = ((LayoutParams) v.getLayoutParams()).getRules(v.getLayoutDirection());
            View next = mKeyViews.get(Integer.valueOf(r[relation]));
            if (next == null || next == v) {
                return null;
            }
            v = next;
        }
        return v;
    }

    private LayoutParams getRelatedViewParams(int[] rules, int relation) {
        View v = getRelatedView(rules, relation);
        if (v != null && v.getLayoutParams() instanceof LayoutParams) {
            return (LayoutParams) v.getLayoutParams();
        }
        return null;
    }

    private int getRelatedViewBaselineOffset(int[] rules) {
        final View v = getRelatedView(rules, ALIGN_BASELINE);
        if (v != null) {
            final int baseline = v.getBaseline();
            if (baseline != -1 && v.getLayoutParams() instanceof LayoutParams) {
                return ((LayoutParams) v.getLayoutParams()).mTop + baseline;
            }
        }
        return -1;
    }

    private static void centerHorizontal(View child, LayoutParams params, int myWidth) {
        int childWidth = child.getMeasuredWidth();
        int left = (myWidth - childWidth) / 2;
        params.mLeft = left;
        params.mRight = left + childWidth;
    }

    private static void centerVertical(View child, LayoutParams params, int myHeight) {
        int childHeight = child.getMeasuredHeight();
        int top = (myHeight - childHeight) / 2;
        params.mTop = top;
        params.mBottom = top + childHeight;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams st = (LayoutParams) child.getLayoutParams();
                child.layout(st.mLeft, st.mTop, st.mRight, st.mBottom);
            }
        }
    }

    // ------------------------------------------------------------ params

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) lp);
        } else if (lp instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) lp);
        }
        return new LayoutParams(lp);
    }

    public CharSequence getAccessibilityClassName() {
        return RelativeLayout.class.getName();
    }
}
