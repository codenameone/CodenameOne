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
package com.google.android.material.divider;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.material.R;
import com.google.android.material.internal.MaterialAttrs;

/// A horizontal Material divider line, inset from its start and end.
public class MaterialDivider extends View {

    private final Paint mPaint = new Paint();
    private int mColor;
    private int mThickness;
    private int mInsetStart;
    private int mInsetEnd;

    public MaterialDivider(Context context) {
        this(context, null);
    }

    public MaterialDivider(Context context, AttributeSet attrs) {
        this(context, attrs, MaterialAttrs.defStyleAttr(context, R.attr.materialDividerStyle));
    }

    public MaterialDivider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int defStyleRes = MaterialAttrs.isMaterial3(context) ? R.style.Widget_Material3_MaterialDivider
                : R.style.Widget_MaterialComponents_MaterialDivider;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaterialDivider, defStyleAttr,
                defStyleAttr == 0 ? defStyleRes : 0);
        try {
            mColor = a.getColor(R.styleable.MaterialDivider_dividerColor,
                    MaterialAttrs.themeColor(context, R.attr.colorOutlineVariant, 0x1f000000));
            mThickness = a.getDimensionPixelSize(R.styleable.MaterialDivider_dividerThickness,
                    MaterialAttrs.dpi(context, 1));
            mInsetStart = a.getDimensionPixelSize(R.styleable.MaterialDivider_dividerInsetStart, 0);
            mInsetEnd = a.getDimensionPixelSize(R.styleable.MaterialDivider_dividerInsetEnd, 0);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int mode = MeasureSpec.getMode(heightMeasureSpec);
        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.UNSPECIFIED) {
            int h = mThickness;
            if (mode == MeasureSpec.AT_MOST) {
                h = Math.min(h, MeasureSpec.getSize(heightMeasureSpec));
            }
            setMeasuredDimension(getMeasuredWidth(), h);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        boolean rtl = isLayoutRtl();
        int left = rtl ? mInsetEnd : mInsetStart;
        int right = getWidth() - (rtl ? mInsetStart : mInsetEnd);
        mPaint.setColor(mColor);
        canvas.drawRect(left, 0, right, getHeight(), mPaint);
    }

    public void setDividerColor(int color) {
        mColor = color;
        invalidate();
    }

    public void setDividerColorResource(int colorRes) {
        setDividerColor(getContext().getColor(colorRes));
    }

    public int getDividerColor() {
        return mColor;
    }

    public void setDividerThickness(int thickness) {
        mThickness = thickness;
        requestLayout();
    }

    public int getDividerThickness() {
        return mThickness;
    }

    public void setDividerInsetStart(int insetStart) {
        mInsetStart = insetStart;
        invalidate();
    }

    public int getDividerInsetStart() {
        return mInsetStart;
    }

    public void setDividerInsetEnd(int insetEnd) {
        mInsetEnd = insetEnd;
        invalidate();
    }

    public int getDividerInsetEnd() {
        return mInsetEnd;
    }
}
