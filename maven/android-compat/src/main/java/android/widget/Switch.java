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
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import com.codename1.androidcompat.runtime.ControlDrawables;

/// A two-state toggle switch: the text at the start, a Material track and
/// thumb at the end. The thumb slides between positions over 250ms. Custom
/// `thumb`/`track` drawables replace the drawn ones.
public class Switch extends CompoundButton {

    private static final int THUMB_ANIMATION_MS = 250;

    private Drawable mThumb;
    private Drawable mTrack;
    private ColorStateList mThumbTint;
    private ColorStateList mTrackTint;
    private CharSequence mTextOn;
    private CharSequence mTextOff;
    private boolean mShowText;
    private int mSwitchMinWidth;
    private int mSwitchPadding;
    private float mThumbPosition;
    private long mAnimStart;
    private float mAnimFrom;
    private boolean mAnimating;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int[] mColors;

    public Switch(Context context) {
        this(context, null);
    }

    public Switch(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.switchStyle);
    }

    public Switch(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, android.R.style.Widget_Material_CompoundButton_Switch);
    }

    public Switch(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.Switch, defStyleAttr, defStyleRes);
        mThumb = a.getDrawable(android.R.styleable.Switch_thumb);
        mTrack = a.getDrawable(android.R.styleable.Switch_track);
        if (a.hasValue(android.R.styleable.Switch_thumbTint)) {
            mThumbTint = a.getColorStateList(android.R.styleable.Switch_thumbTint);
        }
        if (a.hasValue(android.R.styleable.Switch_trackTint)) {
            mTrackTint = a.getColorStateList(android.R.styleable.Switch_trackTint);
        }
        mTextOn = a.getText(android.R.styleable.Switch_textOn);
        mTextOff = a.getText(android.R.styleable.Switch_textOff);
        mShowText = a.getBoolean(android.R.styleable.Switch_showText, false);
        mSwitchMinWidth = a.getDimensionPixelSize(android.R.styleable.Switch_switchMinWidth, 0);
        mSwitchPadding = a.getDimensionPixelSize(android.R.styleable.Switch_switchPadding, 0);
        a.recycle();
        if (mThumb != null) {
            mThumb.setCallback(this);
        }
        if (mTrack != null) {
            mTrack.setCallback(this);
        }
        mColors = ControlDrawables.controlColors(context);
        mThumbPosition = isChecked() ? 1f : 0f;
        textChanged();
    }

    @Override
    Drawable createDefaultButtonDrawable(Context context) {
        return null;
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private int switchWidth() {
        int w = mTrack != null && mTrack.getIntrinsicWidth() > 0 ? mTrack.getIntrinsicWidth() : Math.round(dp(36));
        if (mShowText && mTextOn != null && mTextOff != null) {
            float t = Math.max(getPaint().measureText(mTextOn.toString()), getPaint().measureText(mTextOff.toString()));
            w = Math.max(w, Math.round(t * 2 + dp(16)));
        }
        return Math.max(w, mSwitchMinWidth);
    }

    private int switchHeight() {
        int h = mThumb != null && mThumb.getIntrinsicHeight() > 0 ? mThumb.getIntrinsicHeight() : Math.round(dp(20));
        return Math.max(h, Math.round(dp(20)));
    }

    @Override
    public int getCompoundPaddingRight() {
        int p = super.getCompoundPaddingRight();
        if (!isLayoutRtl()) {
            p += switchWidth() + (getText().length() > 0 ? mSwitchPadding : 0);
        }
        return p;
    }

    @Override
    public int getCompoundPaddingLeft() {
        int p = super.getCompoundPaddingLeft();
        if (isLayoutRtl()) {
            p += switchWidth() + (getText().length() > 0 ? mSwitchPadding : 0);
        }
        return p;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int need = switchHeight() + getPaddingTop() + getPaddingBottom();
        if (getMeasuredHeight() < need && MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            setMeasuredDimension(getMeasuredWidthAndState(), need);
        }
    }

    @Override
    public void setChecked(boolean checked) {
        boolean was = isChecked();
        super.setChecked(checked);
        if (was != checked && mColors != null) {
            if (isAttachedToWindow() && isShown()) {
                mAnimFrom = mThumbPosition;
                mAnimStart = System.currentTimeMillis();
                mAnimating = true;
            } else {
                mThumbPosition = checked ? 1f : 0f;
            }
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mColors == null) {
            return;
        }
        if (mAnimating) {
            float f = Math.min(1f, (System.currentTimeMillis() - mAnimStart) / (float) THUMB_ANIMATION_MS);
            float target = isChecked() ? 1f : 0f;
            mThumbPosition = mAnimFrom + (target - mAnimFrom) * f;
            if (f >= 1f) {
                mAnimating = false;
            } else {
                postInvalidateDelayed(16);
            }
        }
        int w = switchWidth();
        int h = switchHeight();
        int right = isLayoutRtl() ? getPaddingLeft() + w : getWidth() - getPaddingRight();
        int left = right - w;
        int top = getPaddingTop() + (getHeight() - getPaddingTop() - getPaddingBottom() - h) / 2;
        int[] state = getDrawableState();
        boolean enabled = isEnabled();
        float thumbR = h / 2f;
        float cx = left + thumbR + (w - 2 * thumbR) * (isLayoutRtl() ? 1f - mThumbPosition : mThumbPosition);
        float cy = top + h / 2f;
        if (mTrack != null) {
            mTrack.setState(state);
            mTrack.setBounds(left, top, right, top + h);
            mTrack.draw(canvas);
        } else {
            float th = dp(14);
            int trackColor = isChecked() ? mColors[1] : 0xff000000;
            int alpha = isChecked() ? 0x80 : 0x61;
            if (mTrackTint != null) {
                trackColor = mTrackTint.getColorForState(state, trackColor);
                alpha = trackColor >>> 24;
            }
            if (!enabled) {
                alpha = alpha * mColors[2] / 255;
            }
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor((trackColor & 0xffffff) | (alpha << 24));
            RectF r = new RectF(left + dp(3), cy - th / 2, right - dp(3), cy + th / 2);
            canvas.drawRoundRect(r, th / 2, th / 2, mPaint);
        }
        if (mThumb != null) {
            mThumb.setState(state);
            int tw = Math.max(1, mThumb.getIntrinsicWidth());
            mThumb.setBounds(Math.round(cx - tw / 2f), top, Math.round(cx + tw / 2f), top + h);
            mThumb.draw(canvas);
        } else {
            int thumbColor = isChecked() ? mColors[1] : 0xfff1f1f1;
            if (mThumbTint != null) {
                thumbColor = mThumbTint.getColorForState(state, thumbColor);
            }
            if (!enabled) {
                thumbColor = isChecked() ? (thumbColor & 0xffffff) | ((((thumbColor >>> 24) * mColors[2]) / 255) << 24)
                        : 0xffbdbdbd;
            }
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(0x33000000);
            canvas.drawCircle(cx, cy + dp(1), thumbR, mPaint);
            mPaint.setColor(thumbColor);
            canvas.drawCircle(cx, cy, thumbR - dp(0.5f), mPaint);
        }
        if (mShowText) {
            CharSequence t = isChecked() ? mTextOn : mTextOff;
            if (t != null) {
                Paint p = new Paint(getPaint());
                p.setTextAlign(Paint.Align.CENTER);
                p.setColor(getCurrentTextColor());
                canvas.drawText(t.toString(), cx, cy - (p.ascent() + p.descent()) / 2, p);
            }
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mThumb || who == mTrack;
    }

    public void setTextOn(CharSequence textOn) {
        mTextOn = textOn;
        requestLayout();
    }

    public CharSequence getTextOn() {
        return mTextOn;
    }

    public void setTextOff(CharSequence textOff) {
        mTextOff = textOff;
        requestLayout();
    }

    public CharSequence getTextOff() {
        return mTextOff;
    }

    public void setShowText(boolean showText) {
        mShowText = showText;
        requestLayout();
    }

    public boolean getShowText() {
        return mShowText;
    }

    public void setSwitchMinWidth(int pixels) {
        mSwitchMinWidth = pixels;
        requestLayout();
    }

    public int getSwitchMinWidth() {
        return mSwitchMinWidth;
    }

    public void setSwitchPadding(int pixels) {
        mSwitchPadding = pixels;
        requestLayout();
    }

    public int getSwitchPadding() {
        return mSwitchPadding;
    }

    public void setThumbDrawable(Drawable thumb) {
        mThumb = thumb;
        if (thumb != null) {
            thumb.setCallback(this);
        }
        requestLayout();
    }

    public void setThumbResource(int resId) {
        setThumbDrawable(getContext().getDrawable(resId));
    }

    public Drawable getThumbDrawable() {
        return mThumb;
    }

    public void setTrackDrawable(Drawable track) {
        mTrack = track;
        if (track != null) {
            track.setCallback(this);
        }
        requestLayout();
    }

    public void setTrackResource(int resId) {
        setTrackDrawable(getContext().getDrawable(resId));
    }

    public Drawable getTrackDrawable() {
        return mTrack;
    }

    public void setThumbTintList(ColorStateList tint) {
        mThumbTint = tint;
        invalidate();
    }

    public void setTrackTintList(ColorStateList tint) {
        mTrackTint = tint;
        invalidate();
    }

    public void setSplitTrack(boolean splitTrack) {
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return Switch.class.getName();
    }
}
