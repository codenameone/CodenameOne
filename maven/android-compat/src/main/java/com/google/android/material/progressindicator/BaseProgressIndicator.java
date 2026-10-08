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
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.MaterialAttrs;

/// The Material progress indicators' common state: indicator and track
/// colors, thickness and corner radius, and the indeterminate animation
/// clock. Subclasses draw.
public abstract class BaseProgressIndicator extends ProgressBar {

    public static final int SHOW_NONE = 0;
    public static final int SHOW_OUTWARD = 1;
    public static final int SHOW_INWARD = 2;
    public static final int HIDE_NONE = 0;
    public static final int HIDE_OUTWARD = 1;
    public static final int HIDE_INWARD = 2;

    int[] mIndicatorColors;
    int mTrackColor;
    int mTrackThickness;
    int mTrackCornerRadius;
    boolean mIndeterminateMode;
    private long mAnimationStart;

    protected BaseProgressIndicator(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BaseProgressIndicator, defStyleAttr,
                defStyleAttr == 0 ? defStyleRes : 0);
        try {
            int primary = MaterialColors.getColor(context, R.attr.colorPrimary, 0xff6750a4);
            mIndicatorColors = new int[] {a.getColor(R.styleable.BaseProgressIndicator_indicatorColor, primary)};
            mTrackColor = a.getColor(R.styleable.BaseProgressIndicator_trackColor, MaterialColors.withAlpha(primary, 0.24f));
            mTrackThickness = a.getDimensionPixelSize(R.styleable.BaseProgressIndicator_trackThickness,
                    MaterialAttrs.dpi(context, 4));
            mTrackCornerRadius = a.getDimensionPixelSize(R.styleable.BaseProgressIndicator_trackCornerRadius, 0);
            mIndeterminateMode = a.getBoolean(R.styleable.BaseProgressIndicator_android_indeterminate, false);
        } finally {
            a.recycle();
        }
        mAnimationStart = SystemClock.uptimeMillis();
    }

    @Override
    public void setIndeterminate(boolean indeterminate) {
        super.setIndeterminate(indeterminate);
        mIndeterminateMode = indeterminate;
        mAnimationStart = SystemClock.uptimeMillis();
        invalidate();
    }

    @Override
    public boolean isIndeterminate() {
        return mIndeterminateMode;
    }

    /// Sets the progress; `animated` is accepted and the value applied at once.
    public void setProgressCompat(int progress, boolean animated) {
        if (mIndeterminateMode) {
            setIndeterminate(false);
        }
        setProgress(progress);
        invalidate();
    }

    public void setIndicatorColor(int... colors) {
        mIndicatorColors = colors.length == 0 ? new int[] {0} : colors;
        invalidate();
    }

    public int[] getIndicatorColor() {
        return mIndicatorColors;
    }

    public void setTrackColor(int trackColor) {
        mTrackColor = trackColor;
        invalidate();
    }

    public int getTrackColor() {
        return mTrackColor;
    }

    public void setTrackThickness(int trackThickness) {
        mTrackThickness = trackThickness;
        requestLayout();
    }

    public int getTrackThickness() {
        return mTrackThickness;
    }

    public void setTrackCornerRadius(int trackCornerRadius) {
        mTrackCornerRadius = trackCornerRadius;
        invalidate();
    }

    public int getTrackCornerRadius() {
        return mTrackCornerRadius;
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.INVISIBLE);
    }

    /// Milliseconds into the indeterminate animation; schedules the next
    /// frame while indeterminate and shown.
    long animationTime() {
        if (mIndeterminateMode && isShown()) {
            postInvalidateOnAnimation();
        }
        return SystemClock.uptimeMillis() - mAnimationStart;
    }

    float progressFraction() {
        int max = getMax();
        return max <= 0 ? 0f : Math.max(0f, Math.min(1f, getProgress() / (float) max));
    }
}
