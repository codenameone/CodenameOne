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
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

/// A frame showing one child at a time, animating between them with its
/// in and out animations.
public class ViewAnimator extends FrameLayout {

    int mWhichChild;
    boolean mFirstTime = true;
    boolean mAnimateFirstTime = true;
    Animation mInAnimation;
    Animation mOutAnimation;

    public ViewAnimator(Context context) {
        super(context);
        initViewAnimator(context, null);
    }

    public ViewAnimator(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.ViewAnimator);
        int resource = a.getResourceId(android.R.styleable.ViewAnimator_inAnimation, 0);
        if (resource > 0) {
            setInAnimation(context, resource);
        }
        resource = a.getResourceId(android.R.styleable.ViewAnimator_outAnimation, 0);
        if (resource > 0) {
            setOutAnimation(context, resource);
        }
        boolean flag = a.getBoolean(android.R.styleable.ViewAnimator_animateFirstView, true);
        setAnimateFirstView(flag);
        a.recycle();
        initViewAnimator(context, attrs);
    }

    private void initViewAnimator(Context context, AttributeSet attrs) {
        if (attrs == null) {
            setMeasureAllChildren(true);
            return;
        }
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.FrameLayout);
        boolean measureAllChildren = a.getBoolean(android.R.styleable.FrameLayout_measureAllChildren, true);
        setMeasureAllChildren(measureAllChildren);
        a.recycle();
    }

    public void setDisplayedChild(int whichChild) {
        mWhichChild = whichChild;
        if (whichChild >= getChildCount()) {
            mWhichChild = 0;
        } else if (whichChild < 0) {
            mWhichChild = getChildCount() - 1;
        }
        boolean hasFocus = getFocusedChild() != null;
        showOnly(mWhichChild);
        if (hasFocus) {
            requestFocus(FOCUS_FORWARD);
        }
    }

    public int getDisplayedChild() {
        return mWhichChild;
    }

    public void showNext() {
        setDisplayedChild(mWhichChild + 1);
    }

    public void showPrevious() {
        setDisplayedChild(mWhichChild - 1);
    }

    void showOnly(int childIndex, boolean animate) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (i == childIndex) {
                if (animate && mInAnimation != null) {
                    child.startAnimation(mInAnimation);
                }
                child.setVisibility(View.VISIBLE);
                mFirstTime = false;
            } else {
                if (animate && mOutAnimation != null && child.getVisibility() == View.VISIBLE) {
                    child.startAnimation(mOutAnimation);
                } else if (child.getAnimation() == mInAnimation) {
                    child.clearAnimation();
                }
                child.setVisibility(View.GONE);
            }
        }
    }

    void showOnly(int childIndex) {
        boolean animate = !mFirstTime || mAnimateFirstTime;
        showOnly(childIndex, animate);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (getChildCount() == 1) {
            child.setVisibility(View.VISIBLE);
        } else {
            child.setVisibility(View.GONE);
        }
        if (index >= 0 && mWhichChild >= index) {
            setDisplayedChild(mWhichChild + 1);
        }
    }

    @Override
    public void removeAllViews() {
        super.removeAllViews();
        mWhichChild = 0;
        mFirstTime = true;
    }

    @Override
    public void removeView(View view) {
        int index = indexOfChild(view);
        if (index >= 0) {
            removeViewAt(index);
        }
    }

    @Override
    public void removeViewAt(int index) {
        super.removeViewAt(index);
        adjustAfterRemoval(index, 1);
    }

    private void adjustAfterRemoval(int start, int count) {
        int childCount = getChildCount();
        if (childCount == 0) {
            mWhichChild = 0;
            mFirstTime = true;
        } else if (mWhichChild >= start + count) {
            // The same visible view survived, only its index changed.
            mWhichChild -= count;
        } else if (mWhichChild >= start) {
            setDisplayedChild(Math.min(start, childCount - 1));
        }
    }

    @Override
    public void removeViewInLayout(View view) {
        removeView(view);
    }

    @Override
    public void removeViews(int start, int count) {
        super.removeViews(start, count);
        adjustAfterRemoval(start, count);
    }

    @Override
    public void removeViewsInLayout(int start, int count) {
        removeViews(start, count);
    }

    public View getCurrentView() {
        return getChildAt(mWhichChild);
    }

    public Animation getInAnimation() {
        return mInAnimation;
    }

    public void setInAnimation(Animation inAnimation) {
        mInAnimation = inAnimation;
    }

    public Animation getOutAnimation() {
        return mOutAnimation;
    }

    public void setOutAnimation(Animation outAnimation) {
        mOutAnimation = outAnimation;
    }

    public void setInAnimation(Context context, int resourceID) {
        setInAnimation(AnimationUtils.loadAnimation(context, resourceID));
    }

    public void setOutAnimation(Context context, int resourceID) {
        setOutAnimation(AnimationUtils.loadAnimation(context, resourceID));
    }

    public boolean getAnimateFirstView() {
        return mAnimateFirstTime;
    }

    public void setAnimateFirstView(boolean animate) {
        mAnimateFirstTime = animate;
    }

    @Override
    public int getBaseline() {
        return getCurrentView() != null ? getCurrentView().getBaseline() : super.getBaseline();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return ViewAnimator.class.getName();
    }
}
