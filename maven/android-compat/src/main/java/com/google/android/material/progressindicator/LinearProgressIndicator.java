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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.google.android.material.R;
import com.google.android.material.internal.MaterialAttrs;

/// A horizontal Material progress bar: an indicator over a track, or, when
/// indeterminate, a segment sweeping across it.
public class LinearProgressIndicator extends BaseProgressIndicator {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public LinearProgressIndicator(Context context) {
        this(context, null);
    }

    public LinearProgressIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, MaterialAttrs.defStyleAttr(context, R.attr.linearProgressIndicatorStyle));
    }

    public LinearProgressIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, MaterialAttrs.isMaterial3(context)
                ? R.style.Widget_Material3_LinearProgressIndicator
                : R.style.Widget_MaterialComponents_LinearProgressIndicator);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int h = mTrackThickness + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float left = getPaddingLeft();
        float right = getWidth() - getPaddingRight();
        float top = getPaddingTop() + (getHeight() - getPaddingTop() - getPaddingBottom() - mTrackThickness) / 2f;
        float bottom = top + mTrackThickness;
        float r = Math.min(mTrackCornerRadius, mTrackThickness / 2f);
        mPaint.setColor(mTrackColor);
        canvas.drawRoundRect(new RectF(left, top, right, bottom), r, r, mPaint);
        mPaint.setColor(mIndicatorColors[0]);
        float width = right - left;
        float s;
        float e;
        if (mIndeterminateMode) {
            float t = (animationTime() % 1800L) / 1800f;
            float head = Math.min(1f, t * 1.6f);
            float tail = Math.max(0f, t * 1.6f - 0.6f);
            s = left + width * tail;
            e = left + width * head;
        } else {
            s = left;
            e = left + width * progressFraction();
        }
        if (isLayoutRtl()) {
            float ns = right - (e - left);
            float ne = right - (s - left);
            s = ns;
            e = ne;
        }
        if (e > s) {
            canvas.drawRoundRect(new RectF(s, top, e, bottom), r, r, mPaint);
        }
    }
}
