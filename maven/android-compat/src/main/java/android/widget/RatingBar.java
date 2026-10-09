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
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

/// A row of stars showing (and, unless it is an indicator, setting) a rating.
/// Progress runs over `numStars / stepSize` steps; the stars are drawn as
/// paths, filled in the activated color up to the rating.
public class RatingBar extends AbsSeekBar {

    public interface OnRatingBarChangeListener {
        void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser);
    }

    private int mNumStars = 5;
    private float mStepSize = 0.5f;
    private float mRating;
    private OnRatingBarChangeListener mOnRatingBarChangeListener;

    public RatingBar(Context context) {
        this(context, null);
    }

    public RatingBar(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.ratingBarStyle);
    }

    public RatingBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, defaultStyle(defStyleAttr));
    }

    private static int defaultStyle(int defStyleAttr) {
        if (defStyleAttr == android.R.attr.ratingBarStyleIndicator) {
            return android.R.style.Widget_Material_RatingBar_Indicator;
        }
        if (defStyleAttr == android.R.attr.ratingBarStyleSmall) {
            return android.R.style.Widget_Material_RatingBar_Small;
        }
        return android.R.style.Widget_Material_RatingBar;
    }

    public RatingBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.RatingBar, defStyleAttr, defStyleRes);
        int numStars = a.getInt(android.R.styleable.RatingBar_numStars, mNumStars);
        mIsUserSeekable = !a.getBoolean(android.R.styleable.RatingBar_isIndicator, false);
        float rating = a.getFloat(android.R.styleable.RatingBar_rating, -1);
        float stepSize = a.getFloat(android.R.styleable.RatingBar_stepSize, -1);
        a.recycle();
        if (numStars > 0) {
            mNumStars = numStars;
        }
        if (stepSize > 0) {
            mStepSize = stepSize;
        }
        updateMax();
        if (rating >= 0) {
            setRating(rating);
        }
    }

    private void updateMax() {
        setMax(Math.max(1, Math.round(mNumStars / mStepSize)));
    }

    public void setNumStars(int numStars) {
        if (numStars > 0) {
            mNumStars = numStars;
            updateMax();
            requestLayout();
        }
    }

    public int getNumStars() {
        return mNumStars;
    }

    public void setRating(float rating) {
        mRating = rating;
        setProgress(Math.round(rating / mStepSize));
        invalidate();
    }

    public float getRating() {
        return getProgress() * mStepSize;
    }

    public void setStepSize(float stepSize) {
        if (stepSize > 0) {
            float r = getRating();
            mStepSize = stepSize;
            updateMax();
            setRating(r);
        }
    }

    public float getStepSize() {
        return mStepSize;
    }

    public void setIsIndicator(boolean isIndicator) {
        mIsUserSeekable = !isIndicator;
    }

    public boolean isIndicator() {
        return !mIsUserSeekable;
    }

    public void setOnRatingBarChangeListener(OnRatingBarChangeListener listener) {
        mOnRatingBarChangeListener = listener;
    }

    public OnRatingBarChangeListener getOnRatingBarChangeListener() {
        return mOnRatingBarChangeListener;
    }

    @Override
    void onProgressRefresh(float scale, boolean fromUser, int progress) {
        super.onProgressRefresh(scale, fromUser, progress);
        mRating = progress * mStepSize;
        if (mOnRatingBarChangeListener != null) {
            mOnRatingBarChangeListener.onRatingChanged(this, getRating(), fromUser);
        }
    }

    private int starSize() {
        return Math.max(1, Math.min(mMaxHeight, Math.max(mMinHeight, Math.round(dp(48)))));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int s = starSize();
        int w = s * mNumStars + getPaddingLeft() + getPaddingRight();
        int h = s + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSizeAndState(w, widthMeasureSpec, 0), resolveSizeAndState(h, heightMeasureSpec, 0));
    }

    @Override
    float[] trackGeometry() {
        float left = getPaddingLeft();
        float right = left + starSize() * mNumStars;
        return new float[] {left, right, getPaddingTop() + starSize() / 2f};
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mColors == null) {
            return;
        }
        int s = starSize();
        float rating = getRating();
        int on = color(getProgressTintList(), mColors[1]);
        int off = withAlpha(mColors[0], 0.4f);
        for (int i = 0; i < mNumStars; i++) {
            float left = getPaddingLeft() + i * s;
            Path star = star(left + s / 2f, getPaddingTop() + s / 2f, s * 0.42f);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(off);
            canvas.drawPath(star, mPaint);
            float fill = Math.max(0, Math.min(1, rating - i));
            if (fill > 0) {
                canvas.save();
                canvas.clipRect(left, getPaddingTop(), left + s * fill, getPaddingTop() + s);
                mPaint.setColor(on);
                canvas.drawPath(star, mPaint);
                canvas.restore();
            }
        }
    }

    private static Path star(float cx, float cy, float r) {
        Path p = new Path();
        float inner = r * 0.45f;
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(-90 + i * 36);
            float rad = i % 2 == 0 ? r : inner;
            float x = cx + (float) Math.cos(a) * rad;
            float y = cy + (float) Math.sin(a) * rad;
            if (i == 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        p.close();
        return p;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return RatingBar.class.getName();
    }
}
