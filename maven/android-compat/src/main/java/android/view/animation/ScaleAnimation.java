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
import android.util.TypedValue;

/// Scales a view about a pivot point.
public class ScaleAnimation extends Animation {

    private float mFromX;
    private float mToX;
    private float mFromY;
    private float mToY;
    private int mFromXType = TypedValue.TYPE_NULL;
    private int mToXType = TypedValue.TYPE_NULL;
    private int mFromYType = TypedValue.TYPE_NULL;
    private int mToYType = TypedValue.TYPE_NULL;
    private int mFromXData;
    private int mToXData;
    private int mFromYData;
    private int mToYData;
    private int mPivotXType = ABSOLUTE;
    private int mPivotYType = ABSOLUTE;
    private float mPivotXValue;
    private float mPivotYValue;
    private float mPivotX;
    private float mPivotY;
    private Context mContext;

    public ScaleAnimation(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.ScaleAnimation);
        TypedValue tv = a.peekValue(android.R.styleable.ScaleAnimation_fromXScale);
        mFromX = 0.0f;
        if (tv != null) {
            if (tv.type == TypedValue.TYPE_FLOAT) {
                mFromX = tv.getFloat();
            } else {
                mFromXType = tv.type;
                mFromXData = tv.data;
            }
        }
        tv = a.peekValue(android.R.styleable.ScaleAnimation_toXScale);
        mToX = 0.0f;
        if (tv != null) {
            if (tv.type == TypedValue.TYPE_FLOAT) {
                mToX = tv.getFloat();
            } else {
                mToXType = tv.type;
                mToXData = tv.data;
            }
        }
        tv = a.peekValue(android.R.styleable.ScaleAnimation_fromYScale);
        mFromY = 0.0f;
        if (tv != null) {
            if (tv.type == TypedValue.TYPE_FLOAT) {
                mFromY = tv.getFloat();
            } else {
                mFromYType = tv.type;
                mFromYData = tv.data;
            }
        }
        tv = a.peekValue(android.R.styleable.ScaleAnimation_toYScale);
        mToY = 0.0f;
        if (tv != null) {
            if (tv.type == TypedValue.TYPE_FLOAT) {
                mToY = tv.getFloat();
            } else {
                mToYType = tv.type;
                mToYData = tv.data;
            }
        }
        Description d = Description.parseValue(a.peekValue(android.R.styleable.ScaleAnimation_pivotX), context);
        mPivotXType = d.type;
        mPivotXValue = d.value;
        d = Description.parseValue(a.peekValue(android.R.styleable.ScaleAnimation_pivotY), context);
        mPivotYType = d.type;
        mPivotYValue = d.value;
        a.recycle();
        initializePivotPoint();
    }

    public ScaleAnimation(float fromX, float toX, float fromY, float toY) {
        mFromX = fromX;
        mToX = toX;
        mFromY = fromY;
        mToY = toY;
        mPivotX = 0;
        mPivotY = 0;
    }

    public ScaleAnimation(float fromX, float toX, float fromY, float toY, float pivotX, float pivotY) {
        mFromX = fromX;
        mToX = toX;
        mFromY = fromY;
        mToY = toY;
        mPivotXType = ABSOLUTE;
        mPivotYType = ABSOLUTE;
        mPivotXValue = pivotX;
        mPivotYValue = pivotY;
        initializePivotPoint();
    }

    public ScaleAnimation(float fromX, float toX, float fromY, float toY,
                          int pivotXType, float pivotXValue, int pivotYType, float pivotYValue) {
        mFromX = fromX;
        mToX = toX;
        mFromY = fromY;
        mToY = toY;
        mPivotXValue = pivotXValue;
        mPivotXType = pivotXType;
        mPivotYValue = pivotYValue;
        mPivotYType = pivotYType;
        initializePivotPoint();
    }

    private void initializePivotPoint() {
        if (mPivotXType == ABSOLUTE) {
            mPivotX = mPivotXValue;
        }
        if (mPivotYType == ABSOLUTE) {
            mPivotY = mPivotYValue;
        }
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float sx = 1.0f;
        float sy = 1.0f;
        float scale = getScaleFactor();
        if (mFromX != 1.0f || mToX != 1.0f) {
            sx = mFromX + ((mToX - mFromX) * interpolatedTime);
        }
        if (mFromY != 1.0f || mToY != 1.0f) {
            sy = mFromY + ((mToY - mFromY) * interpolatedTime);
        }
        if (mPivotX == 0 && mPivotY == 0) {
            t.getMatrix().setScale(sx, sy);
        } else {
            t.getMatrix().setScale(sx, sy, scale * mPivotX, scale * mPivotY);
        }
    }

    /// A scale given as a fraction or dimension is relative to `size`.
    private float resolveScale(float scale, int type, int data, int size, int psize) {
        float targetSize;
        if (type == TypedValue.TYPE_FRACTION) {
            targetSize = TypedValue.complexToFraction(data, size, psize);
        } else if (type == TypedValue.TYPE_DIMENSION && mContext != null) {
            targetSize = TypedValue.complexToDimension(data, mContext.getResources().getDisplayMetrics());
        } else {
            return scale;
        }
        if (size == 0) {
            return 1;
        }
        return targetSize / (float) size;
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        mFromX = resolveScale(mFromX, mFromXType, mFromXData, width, parentWidth);
        mToX = resolveScale(mToX, mToXType, mToXData, width, parentWidth);
        mFromY = resolveScale(mFromY, mFromYType, mFromYData, height, parentHeight);
        mToY = resolveScale(mToY, mToYType, mToYData, height, parentHeight);
        mPivotX = resolveSize(mPivotXType, mPivotXValue, width, parentWidth);
        mPivotY = resolveSize(mPivotYType, mPivotYValue, height, parentHeight);
    }

    @Override
    protected ScaleAnimation clone() {
        ScaleAnimation c = new ScaleAnimation(mFromX, mToX, mFromY, mToY);
        c.copyAnimationFrom(this);
        c.mFromXType = mFromXType;
        c.mToXType = mToXType;
        c.mFromYType = mFromYType;
        c.mToYType = mToYType;
        c.mFromXData = mFromXData;
        c.mToXData = mToXData;
        c.mFromYData = mFromYData;
        c.mToYData = mToYData;
        c.mPivotXType = mPivotXType;
        c.mPivotYType = mPivotYType;
        c.mPivotXValue = mPivotXValue;
        c.mPivotYValue = mPivotYValue;
        c.mPivotX = mPivotX;
        c.mPivotY = mPivotY;
        c.mContext = mContext;
        return c;
    }
}
