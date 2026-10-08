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
package com.google.android.material.progressindicator;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.google.android.material.R;
import com.google.android.material.internal.MaterialAttrs;

/// A circular Material progress indicator: an arc over a ring, or, when
/// indeterminate, a growing and shrinking arc that rotates.
public class CircularProgressIndicator extends BaseProgressIndicator {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int mIndicatorSize;
    private int mIndicatorInset;

    public CircularProgressIndicator(Context context) {
        this(context, null);
    }

    public CircularProgressIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, MaterialAttrs.defStyleAttr(context, R.attr.circularProgressIndicatorStyle));
    }

    public CircularProgressIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, MaterialAttrs.isMaterial3(context)
                ? R.style.Widget_Material3_CircularProgressIndicator
                : R.style.Widget_MaterialComponents_CircularProgressIndicator);
        int defStyleRes = MaterialAttrs.isMaterial3(context) ? R.style.Widget_Material3_CircularProgressIndicator
                : R.style.Widget_MaterialComponents_CircularProgressIndicator;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircularProgressIndicator, defStyleAttr,
                defStyleAttr == 0 ? defStyleRes : 0);
        try {
            mIndicatorSize = a.getDimensionPixelSize(R.styleable.CircularProgressIndicator_indicatorSize,
                    MaterialAttrs.dpi(context, 40));
            mIndicatorInset = a.getDimensionPixelSize(R.styleable.CircularProgressIndicator_indicatorInset,
                    MaterialAttrs.dpi(context, 4));
        } finally {
            a.recycle();
        }
    }

    public void setIndicatorSize(int indicatorSize) {
        mIndicatorSize = indicatorSize;
        requestLayout();
    }

    public int getIndicatorSize() {
        return mIndicatorSize;
    }

    public void setIndicatorInset(int indicatorInset) {
        mIndicatorInset = indicatorInset;
        requestLayout();
    }

    public int getIndicatorInset() {
        return mIndicatorInset;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int d = mIndicatorSize + mIndicatorInset * 2;
        setMeasuredDimension(resolveSize(d + getPaddingLeft() + getPaddingRight(), widthMeasureSpec),
                resolveSize(d + getPaddingTop() + getPaddingBottom(), heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getPaddingLeft() + (getWidth() - getPaddingLeft() - getPaddingRight()) / 2f;
        float cy = getPaddingTop() + (getHeight() - getPaddingTop() - getPaddingBottom()) / 2f;
        float radius = (mIndicatorSize - mTrackThickness) / 2f;
        RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mTrackThickness);
        mPaint.setStrokeCap(mTrackCornerRadius > 0 ? Paint.Cap.ROUND : Paint.Cap.BUTT);
        if ((mTrackColor >>> 24) != 0) {
            mPaint.setColor(mTrackColor);
            canvas.drawArc(oval, 0, 360, false, mPaint);
        }
        mPaint.setColor(mIndicatorColors[0]);
        if (mIndeterminateMode) {
            long t = animationTime();
            float rotation = (t % 1568L) / 1568f * 360f;
            float phase = (t % 1333L) / 1333f;
            float sweep = 10f + 260f * (phase < 0.5f ? phase * 2f : (1f - phase) * 2f);
            float start = rotation + (phase < 0.5f ? 0f : (phase - 0.5f) * 2f * 260f) - 90f;
            canvas.drawArc(oval, start, sweep, false, mPaint);
        } else {
            canvas.drawArc(oval, -90f, 360f * progressFraction(), false, mPaint);
        }
        mPaint.setStyle(Paint.Style.FILL);
    }
}
