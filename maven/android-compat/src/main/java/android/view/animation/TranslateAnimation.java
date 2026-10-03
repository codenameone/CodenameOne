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

/// Moves a view by an offset, absolute or relative to its own or its
/// parent's size.
public class TranslateAnimation extends Animation {

    private int mFromXType = ABSOLUTE;
    private int mToXType = ABSOLUTE;
    private int mFromYType = ABSOLUTE;
    private int mToYType = ABSOLUTE;
    protected float mFromXValue;
    protected float mToXValue;
    protected float mFromYValue;
    protected float mToYValue;
    protected float mFromXDelta;
    protected float mToXDelta;
    protected float mFromYDelta;
    protected float mToYDelta;

    public TranslateAnimation(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.TranslateAnimation);
        Description d = Description.parseValue(a.peekValue(android.R.styleable.TranslateAnimation_fromXDelta), context);
        mFromXType = d.type;
        mFromXValue = d.value;
        d = Description.parseValue(a.peekValue(android.R.styleable.TranslateAnimation_toXDelta), context);
        mToXType = d.type;
        mToXValue = d.value;
        d = Description.parseValue(a.peekValue(android.R.styleable.TranslateAnimation_fromYDelta), context);
        mFromYType = d.type;
        mFromYValue = d.value;
        d = Description.parseValue(a.peekValue(android.R.styleable.TranslateAnimation_toYDelta), context);
        mToYType = d.type;
        mToYValue = d.value;
        a.recycle();
    }

    public TranslateAnimation(float fromXDelta, float toXDelta, float fromYDelta, float toYDelta) {
        mFromXValue = fromXDelta;
        mToXValue = toXDelta;
        mFromYValue = fromYDelta;
        mToYValue = toYDelta;
    }

    public TranslateAnimation(int fromXType, float fromXValue, int toXType, float toXValue,
                              int fromYType, float fromYValue, int toYType, float toYValue) {
        mFromXValue = fromXValue;
        mToXValue = toXValue;
        mFromYValue = fromYValue;
        mToYValue = toYValue;
        mFromXType = fromXType;
        mToXType = toXType;
        mFromYType = fromYType;
        mToYType = toYType;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float dx = mFromXDelta;
        float dy = mFromYDelta;
        if (mFromXDelta != mToXDelta) {
            dx = mFromXDelta + ((mToXDelta - mFromXDelta) * interpolatedTime);
        }
        if (mFromYDelta != mToYDelta) {
            dy = mFromYDelta + ((mToYDelta - mFromYDelta) * interpolatedTime);
        }
        t.getMatrix().setTranslate(dx, dy);
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        mFromXDelta = resolveSize(mFromXType, mFromXValue, width, parentWidth);
        mToXDelta = resolveSize(mToXType, mToXValue, width, parentWidth);
        mFromYDelta = resolveSize(mFromYType, mFromYValue, height, parentHeight);
        mToYDelta = resolveSize(mToYType, mToYValue, height, parentHeight);
    }

    @Override
    protected TranslateAnimation clone() {
        TranslateAnimation c = new TranslateAnimation(0, 0, 0, 0);
        c.copyAnimationFrom(this);
        c.mFromXType = mFromXType;
        c.mToXType = mToXType;
        c.mFromYType = mFromYType;
        c.mToYType = mToYType;
        c.mFromXValue = mFromXValue;
        c.mToXValue = mToXValue;
        c.mFromYValue = mFromYValue;
        c.mToYValue = mToYValue;
        c.mFromXDelta = mFromXDelta;
        c.mToXDelta = mToXDelta;
        c.mFromYDelta = mFromYDelta;
        c.mToYDelta = mToYDelta;
        return c;
    }
}
