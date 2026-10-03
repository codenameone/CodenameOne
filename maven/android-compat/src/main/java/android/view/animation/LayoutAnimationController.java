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
package android.view.animation;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.Random;

/// Plays one animation on each child of a view group, staggered by the
/// child's position: the `android:layoutAnimation` of a group.
public class LayoutAnimationController {

    public static final int ORDER_NORMAL = 0;
    public static final int ORDER_REVERSE = 1;
    public static final int ORDER_RANDOM = 2;

    protected Animation mAnimation;
    protected Random mRandomizer;
    protected Interpolator mInterpolator;
    private float mDelay;
    private int mOrder;
    private long mDuration;
    private long mMaxDelay;

    public LayoutAnimationController(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.LayoutAnimation);
        Animation.Description d = Animation.Description.parseValue(
                a.peekValue(android.R.styleable.LayoutAnimation_delay), context);
        mDelay = d.value;
        mOrder = a.getInt(android.R.styleable.LayoutAnimation_animationOrder, ORDER_NORMAL);
        int resource = a.getResourceId(android.R.styleable.LayoutAnimation_animation, 0);
        if (resource > 0) {
            setAnimation(context, resource);
        }
        resource = a.getResourceId(android.R.styleable.LayoutAnimation_interpolator, 0);
        if (resource > 0) {
            setInterpolator(context, resource);
        }
        a.recycle();
    }

    public LayoutAnimationController(Animation animation) {
        this(animation, 0.5f);
    }

    public LayoutAnimationController(Animation animation, float delay) {
        mDelay = delay;
        setAnimation(animation);
    }

    public int getOrder() {
        return mOrder;
    }

    public void setOrder(int order) {
        mOrder = order;
    }

    public void setAnimation(Context context, int resourceID) {
        setAnimation(AnimationUtils.loadAnimation(context, resourceID));
    }

    public void setAnimation(Animation animation) {
        mAnimation = animation;
        mAnimation.setFillBefore(true);
    }

    public Animation getAnimation() {
        return mAnimation;
    }

    public void setInterpolator(Context context, int resourceID) {
        setInterpolator(AnimationUtils.loadInterpolator(context, resourceID));
    }

    public void setInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    public Interpolator getInterpolator() {
        return mInterpolator;
    }

    public float getDelay() {
        return mDelay;
    }

    public void setDelay(float delay) {
        mDelay = delay;
    }

    public boolean willOverlap() {
        return mDelay < 1.0f;
    }

    public void start() {
        mDuration = mAnimation.getDuration();
        mMaxDelay = Long.MIN_VALUE;
        mAnimation.setStartTime(-1);
    }

    public final Animation getAnimationForView(View view) {
        long delay = getDelayForView(view) + mAnimation.getStartOffset();
        mMaxDelay = Math.max(mMaxDelay, delay);
        Animation animation = mAnimation.clone();
        animation.setStartOffset(delay);
        return animation;
    }

    public boolean isDone() {
        return AnimationUtils.currentAnimationTimeMillis() > mAnimation.getStartTime() + mMaxDelay + mDuration;
    }

    protected long getDelayForView(View view) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        AnimationParameters params = lp == null ? null : lp.layoutAnimationParameters;
        if (params == null) {
            return 0;
        }
        float delay = mDelay * mAnimation.getDuration();
        long viewDelay = (long) (getTransformedIndex(params) * delay);
        float totalDelay = delay * params.count;
        if (mInterpolator == null) {
            mInterpolator = new LinearInterpolator();
        }
        float normalizedDelay = totalDelay == 0 ? 0 : viewDelay / totalDelay;
        normalizedDelay = mInterpolator.getInterpolation(normalizedDelay);
        return (long) (normalizedDelay * totalDelay);
    }

    protected int getTransformedIndex(AnimationParameters params) {
        switch (getOrder()) {
            case ORDER_REVERSE:
                return params.count - 1 - params.index;
            case ORDER_RANDOM:
                if (mRandomizer == null) {
                    mRandomizer = new Random();
                }
                return (int) (params.count * mRandomizer.nextFloat());
            case ORDER_NORMAL:
            default:
                return params.index;
        }
    }

    /// Where a child sits among the children animated together.
    public static class AnimationParameters {
        public int count;
        public int index;
    }
}
