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
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

/// A progress bar with a draggable thumb: the base of SeekBar and RatingBar.
public abstract class AbsSeekBar extends ProgressBar {

    private Drawable mThumb;
    private ColorStateList mThumbTint;
    private int mThumbOffset;
    private int mKeyProgressIncrement = 1;
    private boolean mIsDragging;
    boolean mIsUserSeekable = true;

    public AbsSeekBar(Context context) {
        this(context, null);
    }

    public AbsSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbsSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AbsSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.AbsSeekBar, defStyleAttr,
                defStyleRes);
        mThumb = a.getDrawable(android.R.styleable.AbsSeekBar_thumb);
        if (a.hasValue(android.R.styleable.AbsSeekBar_thumbTint)) {
            mThumbTint = a.getColorStateList(android.R.styleable.AbsSeekBar_thumbTint);
        }
        mThumbOffset = a.getDimensionPixelOffset(android.R.styleable.AbsSeekBar_thumbOffset, 0);
        a.recycle();
        if (mThumb != null) {
            mThumb.setCallback(this);
        }
        int range = getMax() - getMin();
        if (range / 20 > 1) {
            mKeyProgressIncrement = Math.max(1, Math.round(range / 20f));
        }
    }

    public void setThumb(Drawable thumb) {
        mThumb = thumb;
        if (thumb != null) {
            thumb.setCallback(this);
        }
        invalidate();
    }

    public Drawable getThumb() {
        return mThumb;
    }

    public void setThumbTintList(ColorStateList tint) {
        mThumbTint = tint;
        invalidate();
    }

    public int getThumbOffset() {
        return mThumbOffset;
    }

    public void setThumbOffset(int thumbOffset) {
        mThumbOffset = thumbOffset;
        invalidate();
    }

    public void setKeyProgressIncrement(int increment) {
        mKeyProgressIncrement = Math.abs(increment);
    }

    public int getKeyProgressIncrement() {
        return mKeyProgressIncrement;
    }

    public void setSplitTrack(boolean splitTrack) {
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mThumb || super.verifyDrawable(who);
    }

    boolean isDragging() {
        return mIsDragging;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mColors == null || isIndeterminate()) {
            return;
        }
        drawThumb(canvas);
    }

    void drawThumb(Canvas canvas) {
        float[] g = trackGeometry();
        float x = g[0] + (g[1] - g[0]) * scale(getProgress());
        if (mThumb != null) {
            int w = Math.max(1, mThumb.getIntrinsicWidth());
            int h = Math.max(1, mThumb.getIntrinsicHeight());
            mThumb.setState(getDrawableState());
            mThumb.setBounds(Math.round(x - w / 2f), Math.round(g[2] - h / 2f), Math.round(x + w / 2f),
                    Math.round(g[2] + h / 2f));
            mThumb.draw(canvas);
            return;
        }
        float r = dp(isPressed() ? 10 : 6);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(color(mThumbTint, mColors[1]));
        canvas.drawCircle(x, g[2], r, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsUserSeekable || !isEnabled()) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                onStartTrackingTouch();
                trackTouchEvent(event);
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    trackTouchEvent(event);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate();
                break;
            default:
                break;
        }
        return true;
    }

    private void trackTouchEvent(MotionEvent event) {
        float[] g = trackGeometry();
        float width = g[1] - g[0];
        float scale = width <= 0 ? 0 : (event.getX() - g[0]) / width;
        if (isLayoutRtl()) {
            scale = 1 - scale;
        }
        scale = Math.max(0, Math.min(1, scale));
        int progress = getMin() + Math.round(scale * (getMax() - getMin()));
        setProgressInternal(progress, true);
    }

    void onStartTrackingTouch() {
        mIsDragging = true;
    }

    void onStopTrackingTouch() {
        mIsDragging = false;
    }

    void onKeyChange() {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isEnabled()) {
            int increment = mKeyProgressIncrement;
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                increment = -increment;
            } else if (keyCode != KeyEvent.KEYCODE_DPAD_RIGHT) {
                return super.onKeyDown(keyCode, event);
            }
            if (isLayoutRtl()) {
                increment = -increment;
            }
            if (setProgressInternal(getProgress() + increment, true)) {
                onKeyChange();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return AbsSeekBar.class.getName();
    }
}
