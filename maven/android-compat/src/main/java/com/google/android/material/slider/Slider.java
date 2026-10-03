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
package com.google.android.material.slider;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.MaterialAttrs;

import java.util.ArrayList;
import java.util.List;

/// A Material slider: picks a value between `valueFrom` and `valueTo`
/// (snapped to `stepSize` when set) by dragging a thumb along a track, and
/// shows the value in a floating label while dragged.
public class Slider extends View {

    /// Called when the value changes.
    public interface OnChangeListener {
        void onValueChange(Slider slider, float value, boolean fromUser);
    }

    /// Called when the user starts and stops dragging.
    public interface OnSliderTouchListener {
        void onStartTrackingTouch(Slider slider);

        void onStopTrackingTouch(Slider slider);
    }

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<OnChangeListener> mChangeListeners = new ArrayList<OnChangeListener>();
    private final List<OnSliderTouchListener> mTouchListeners = new ArrayList<OnSliderTouchListener>();
    private float mFrom;
    private float mTo = 1f;
    private float mValue;
    private float mStep;
    private ColorStateList mActiveTrack;
    private ColorStateList mInactiveTrack;
    private ColorStateList mThumb;
    private ColorStateList mHalo;
    private int mThumbRadius;
    private int mTrackHeight;
    private int mLabelBehavior;
    private LabelFormatter mFormatter;
    private boolean mDragging;

    public Slider(Context context) {
        this(context, null);
    }

    public Slider(Context context, AttributeSet attrs) {
        this(context, attrs, MaterialAttrs.defStyleAttr(context, R.attr.sliderStyle));
    }

    public Slider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int defStyleRes = MaterialAttrs.isMaterial3(context) ? R.style.Widget_Material3_Slider
                : R.style.Widget_MaterialComponents_Slider;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Slider, defStyleAttr,
                defStyleAttr == 0 ? defStyleRes : 0);
        try {
            mFrom = a.getFloat(R.styleable.Slider_valueFrom, a.getFloat(R.styleable.Slider_android_valueFrom, 0f));
            mTo = a.getFloat(R.styleable.Slider_valueTo, a.getFloat(R.styleable.Slider_android_valueTo, 1f));
            mValue = a.getFloat(R.styleable.Slider_android_value, mFrom);
            mStep = a.getFloat(R.styleable.Slider_stepSize, a.getFloat(R.styleable.Slider_android_stepSize, 0f));
            int primary = MaterialColors.getColor(context, R.attr.colorPrimary, 0xff6750a4);
            mActiveTrack = colorOr(a, R.styleable.Slider_trackColorActive, primary);
            mInactiveTrack = colorOr(a, R.styleable.Slider_trackColorInactive, MaterialColors.withAlpha(primary, 0.24f));
            mThumb = colorOr(a, R.styleable.Slider_thumbColor, primary);
            mHalo = colorOr(a, R.styleable.Slider_haloColor, primary);
            mThumbRadius = a.getDimensionPixelSize(R.styleable.Slider_thumbRadius, MaterialAttrs.dpi(context, 10));
            mTrackHeight = a.getDimensionPixelSize(R.styleable.Slider_trackHeight, MaterialAttrs.dpi(context, 4));
            mLabelBehavior = a.getInt(R.styleable.Slider_labelBehavior, LabelFormatter.LABEL_FLOATING);
            setEnabled(a.getBoolean(R.styleable.Slider_android_enabled, true));
        } finally {
            a.recycle();
        }
        mValue = clamp(mValue);
        setFocusable(true);
        setClickable(true);
    }

    private static ColorStateList colorOr(TypedArray a, int index, int fallback) {
        ColorStateList c = a.getColorStateList(index);
        return c != null ? c : ColorStateList.valueOf(fallback);
    }

    private float clamp(float v) {
        float lo = Math.min(mFrom, mTo);
        float hi = Math.max(mFrom, mTo);
        float c = Math.max(lo, Math.min(hi, v));
        if (mStep > 0) {
            c = mFrom + Math.round((c - mFrom) / mStep) * mStep;
            c = Math.max(lo, Math.min(hi, c));
        }
        return c;
    }

    // ------------------------------------------------------------ value

    public float getValue() {
        return mValue;
    }

    public void setValue(float value) {
        setValueInternal(value, false);
    }

    private void setValueInternal(float value, boolean fromUser) {
        float v = clamp(value);
        if (Float.compare(v, mValue) == 0) {
            return;
        }
        mValue = v;
        invalidate();
        for (OnChangeListener l : new ArrayList<OnChangeListener>(mChangeListeners)) {
            l.onValueChange(this, mValue, fromUser);
        }
    }

    public float getValueFrom() {
        return mFrom;
    }

    public void setValueFrom(float valueFrom) {
        mFrom = valueFrom;
        mValue = clamp(mValue);
        invalidate();
    }

    public float getValueTo() {
        return mTo;
    }

    public void setValueTo(float valueTo) {
        mTo = valueTo;
        mValue = clamp(mValue);
        invalidate();
    }

    public float getStepSize() {
        return mStep;
    }

    public void setStepSize(float stepSize) {
        mStep = stepSize;
        mValue = clamp(mValue);
        invalidate();
    }

    public void addOnChangeListener(OnChangeListener listener) {
        mChangeListeners.add(listener);
    }

    public void removeOnChangeListener(OnChangeListener listener) {
        mChangeListeners.remove(listener);
    }

    public void clearOnChangeListeners() {
        mChangeListeners.clear();
    }

    public void addOnSliderTouchListener(OnSliderTouchListener listener) {
        mTouchListeners.add(listener);
    }

    public void removeOnSliderTouchListener(OnSliderTouchListener listener) {
        mTouchListeners.remove(listener);
    }

    public void setLabelFormatter(LabelFormatter formatter) {
        mFormatter = formatter;
    }

    public boolean hasLabelFormatter() {
        return mFormatter != null;
    }

    public void setLabelBehavior(int labelBehavior) {
        mLabelBehavior = labelBehavior;
        invalidate();
    }

    public int getLabelBehavior() {
        return mLabelBehavior;
    }

    public void setTrackActiveTintList(ColorStateList color) {
        mActiveTrack = color;
        invalidate();
    }

    public void setTrackInactiveTintList(ColorStateList color) {
        mInactiveTrack = color;
        invalidate();
    }

    public void setThumbTintList(ColorStateList color) {
        mThumb = color;
        invalidate();
    }

    public void setHaloTintList(ColorStateList color) {
        mHalo = color;
        invalidate();
    }

    /// The label text: the formatter's, else the value without a decimal
    /// part when it has none, else one decimal.
    private String format(float value) {
        if (mFormatter != null) {
            return mFormatter.getFormattedValue(value);
        }
        int whole = Math.round(value);
        if (Math.abs(value - whole) < 0.0001f) {
            return String.valueOf(whole);
        }
        int tenths = Math.round(value * 10f);
        return (tenths < 0 ? "-" : "") + Math.abs(tenths / 10) + "." + Math.abs(tenths % 10);
    }

    // ------------------------------------------------------------ measure, draw, touch

    private int trackSidePadding() {
        return Math.max(mThumbRadius, MaterialAttrs.dpi(getContext(), 12)) + MaterialAttrs.dpi(getContext(), 4);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int h = MaterialAttrs.dpi(getContext(), 48) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                resolveSize(h, heightMeasureSpec));
    }

    private float fraction() {
        return Float.compare(mTo, mFrom) == 0 ? 0f : (mValue - mFrom) / (mTo - mFrom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int[] state = getDrawableState();
        float left = getPaddingLeft() + trackSidePadding();
        float right = getWidth() - getPaddingRight() - trackSidePadding();
        float cy = getPaddingTop() + (getHeight() - getPaddingTop() - getPaddingBottom()) / 2f;
        float f = fraction();
        float x = isLayoutRtl() ? right - (right - left) * f : left + (right - left) * f;
        float half = mTrackHeight / 2f;
        int onSurface = MaterialAttrs.onSurface(getContext());
        boolean enabled = isEnabled();
        int inactive = enabled ? mInactiveTrack.getColorForState(state, mInactiveTrack.getDefaultColor())
                : MaterialColors.withAlpha(onSurface, 0.12f);
        int active = enabled ? mActiveTrack.getColorForState(state, mActiveTrack.getDefaultColor())
                : MaterialColors.withAlpha(onSurface, 0.38f);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(inactive);
        canvas.drawRoundRect(new RectF(left, cy - half, right, cy + half), half, half, mPaint);
        mPaint.setColor(active);
        float a0 = isLayoutRtl() ? x : left;
        float a1 = isLayoutRtl() ? right : x;
        if (a1 > a0) {
            canvas.drawRoundRect(new RectF(a0, cy - half, a1, cy + half), half, half, mPaint);
        }
        if (mStep > 0 && Float.compare(mTo, mFrom) != 0) {
            int ticks = Math.round(Math.abs(mTo - mFrom) / mStep);
            if (ticks > 0 && ticks <= (right - left) / MaterialAttrs.dp(getContext(), 8)) {
                for (int i = 0; i <= ticks; i++) {
                    float tx = left + (right - left) * i / ticks;
                    boolean on = isLayoutRtl() ? tx >= x : tx <= x;
                    mPaint.setColor(on ? MaterialColors.withAlpha(0xffffffff, 0.54f)
                            : MaterialColors.withAlpha(active, 0.54f));
                    canvas.drawCircle(tx, cy, MaterialAttrs.dp(getContext(), 1), mPaint);
                }
            }
        }
        if (mDragging && enabled) {
            int halo = mHalo.getColorForState(state, mHalo.getDefaultColor());
            mPaint.setColor(MaterialColors.withAlpha(halo, 0.24f));
            canvas.drawCircle(x, cy, MaterialAttrs.dp(getContext(), 24) / 2f + mThumbRadius / 2f, mPaint);
        }
        mPaint.setColor(enabled ? mThumb.getColorForState(state, mThumb.getDefaultColor())
                : MaterialColors.withAlpha(onSurface, 0.38f));
        canvas.drawCircle(x, cy, mThumbRadius, mPaint);
        if (mDragging && mLabelBehavior != LabelFormatter.LABEL_GONE) {
            drawLabel(canvas, x, cy);
        }
    }

    private void drawLabel(Canvas canvas, float x, float cy) {
        String text = format(mValue);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setTextSize(MaterialAttrs.dp(getContext(), 12) * getResources().getDisplayMetrics().scaledDensity
                / getResources().getDisplayMetrics().density);
        float tw = tp.measureText(text);
        float pad = MaterialAttrs.dp(getContext(), 8);
        float h = MaterialAttrs.dp(getContext(), 28);
        float w = Math.max(h, tw + pad * 2);
        float bottom = cy - mThumbRadius - MaterialAttrs.dp(getContext(), 6);
        RectF r = new RectF(x - w / 2f, bottom - h, x + w / 2f, bottom);
        mPaint.setColor(MaterialAttrs.isMaterial3(getContext())
                ? MaterialColors.getColor(getContext(), R.attr.colorPrimary, 0xff6750a4)
                : MaterialColors.getColor(getContext(), R.attr.colorSurfaceInverse, 0xff323232));
        canvas.drawRoundRect(r, h / 2f, h / 2f, mPaint);
        tp.setColor(MaterialAttrs.isMaterial3(getContext())
                ? MaterialColors.getColor(getContext(), R.attr.colorOnPrimary, 0xffffffff) : 0xffffffff);
        Paint.FontMetrics fm = tp.getFontMetrics();
        canvas.drawText(text, x - tw / 2f, r.centerY() - (fm.ascent + fm.descent) / 2f, tp);
    }

    @Override
    protected boolean drawsOutsideBounds() {
        return mDragging;
    }

    private float valueAt(float px) {
        float left = getPaddingLeft() + trackSidePadding();
        float right = getWidth() - getPaddingRight() - trackSidePadding();
        float f = right <= left ? 0f : (px - left) / (right - left);
        if (isLayoutRtl()) {
            f = 1f - f;
        }
        return mFrom + (mTo - mFrom) * Math.max(0f, Math.min(1f, f));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDragging = true;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                setPressed(true);
                for (OnSliderTouchListener l : new ArrayList<OnSliderTouchListener>(mTouchListeners)) {
                    l.onStartTrackingTouch(this);
                }
                setValueInternal(valueAt(event.getX()), true);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                setValueInternal(valueAt(event.getX()), true);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    setValueInternal(valueAt(event.getX()), true);
                }
                mDragging = false;
                setPressed(false);
                for (OnSliderTouchListener l : new ArrayList<OnSliderTouchListener>(mTouchListeners)) {
                    l.onStopTrackingTouch(this);
                }
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }
}
