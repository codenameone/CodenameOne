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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewParent;

/// A wheel for picking a number from a range: the selected value between two
/// divider lines, its neighbours faded above and below. Dragging scrolls the
/// wheel one value per row; tapping above or below the selection steps once.
public class NumberPicker extends LinearLayout {

    public interface OnValueChangeListener {
        void onValueChange(NumberPicker picker, int oldVal, int newVal);
    }

    public interface OnScrollListener {
        int SCROLL_STATE_IDLE = 0;
        int SCROLL_STATE_TOUCH_SCROLL = 1;
        int SCROLL_STATE_FLING = 2;

        void onScrollStateChange(NumberPicker view, int scrollState);
    }

    public interface Formatter {
        String format(int value);
    }

    /// Two digits with a leading zero, as the date and time pickers show
    /// days, hours and minutes.
    static final Formatter TWO_DIGITS = new Formatter() {
        @Override
        public String format(int value) {
            return value < 10 ? "0" + value : String.valueOf(value);
        }
    };

    /// Rows shown: the selection and one neighbour on each side.
    private static final int SELECTOR_WHEEL_ITEM_COUNT = 3;

    private int mMinValue;
    private int mMaxValue;
    private int mValue;
    private String[] mDisplayedValues;
    private Formatter mFormatter;
    private boolean mWrapRequested = true;
    private OnValueChangeListener mOnValueChangeListener;
    private OnScrollListener mOnScrollListener;
    private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDividerPaint = new Paint();
    private int mTextColor;
    private int mDividerColor;
    private int mDividerHeight;
    private int mDividerDistance;
    private int mMinWidth;
    private int mMaxHeight;
    private float mOffset;
    private float mLastY;
    private float mDownY;
    private boolean mDragging;
    private final int mTouchSlop;

    public NumberPicker(Context context) {
        this(context, null);
    }

    public NumberPicker(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.numberPickerStyle);
    }

    public NumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NumberPicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mTextColor = themeColor(android.R.attr.textColorPrimary, 0xde000000);
        mDividerColor = themeColor(android.R.attr.colorControlNormal, 0x8a000000);
        mDividerHeight = Math.round(dp(2));
        mDividerDistance = Math.round(dp(48));
        mMinWidth = Math.round(dp(64));
        mMaxHeight = Math.round(dp(180));
        mTextPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16,
                getResources().getDisplayMetrics()));
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private int themeColor(int attr, int def) {
        TypedValue tv = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attr, tv, true)) {
            if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return tv.data;
            }
            if (tv.resourceId != 0) {
                android.content.res.ColorStateList csl = getContext().getColorStateList(tv.resourceId);
                if (csl != null) {
                    return csl.getDefaultColor();
                }
            }
        }
        return def;
    }

    // ------------------------------------------------------------ values

    public void setOnValueChangedListener(OnValueChangeListener listener) {
        mOnValueChangeListener = listener;
    }

    public void setOnScrollListener(OnScrollListener listener) {
        mOnScrollListener = listener;
    }

    public void setFormatter(Formatter formatter) {
        mFormatter = formatter;
        invalidate();
    }

    public void setOnLongPressUpdateInterval(long intervalMillis) {
    }

    public int getMinValue() {
        return mMinValue;
    }

    public void setMinValue(int minValue) {
        if (minValue < 0) {
            throw new IllegalArgumentException("minValue must be >= 0");
        }
        mMinValue = minValue;
        if (mMinValue > mValue) {
            mValue = mMinValue;
        }
        requestLayout();
        invalidate();
    }

    public int getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(int maxValue) {
        if (maxValue < 0) {
            throw new IllegalArgumentException("maxValue must be >= 0");
        }
        mMaxValue = maxValue;
        if (mMaxValue < mValue) {
            mValue = mMaxValue;
        }
        requestLayout();
        invalidate();
    }

    public int getValue() {
        return mValue;
    }

    /// Sets the value without telling the listener, as on Android.
    public void setValue(int value) {
        setValueInternal(value, false);
    }

    public String[] getDisplayedValues() {
        return mDisplayedValues;
    }

    public void setDisplayedValues(String[] displayedValues) {
        mDisplayedValues = displayedValues;
        requestLayout();
        invalidate();
    }

    public boolean getWrapSelectorWheel() {
        return mWrapRequested && mMaxValue - mMinValue >= SELECTOR_WHEEL_ITEM_COUNT;
    }

    public void setWrapSelectorWheel(boolean wrapSelectorWheel) {
        mWrapRequested = wrapSelectorWheel;
        invalidate();
    }

    public int getTextColor() {
        return mTextColor;
    }

    public void setTextColor(int color) {
        mTextColor = color;
        invalidate();
    }

    public float getTextSize() {
        return mTextPaint.getTextSize();
    }

    public void setTextSize(float size) {
        mTextPaint.setTextSize(size);
        requestLayout();
        invalidate();
    }

    public int getSelectionDividerHeight() {
        return mDividerHeight;
    }

    public void setSelectionDividerHeight(int height) {
        mDividerHeight = height;
        invalidate();
    }

    public int getSolidColor() {
        return 0;
    }

    private int wrap(int value) {
        int range = mMaxValue - mMinValue + 1;
        if (range <= 0) {
            return mMinValue;
        }
        int v = (value - mMinValue) % range;
        if (v < 0) {
            v += range;
        }
        return mMinValue + v;
    }

    private void setValueInternal(int value, boolean notify) {
        int v = getWrapSelectorWheel() ? wrap(value) : Math.max(mMinValue, Math.min(value, mMaxValue));
        int old = mValue;
        mValue = v;
        invalidate();
        if (notify && old != v && mOnValueChangeListener != null) {
            mOnValueChangeListener.onValueChange(this, old, v);
        }
    }

    private void changeValueByOne(boolean increment) {
        if (!getWrapSelectorWheel() && (increment ? mValue >= mMaxValue : mValue <= mMinValue)) {
            return;
        }
        setValueInternal(mValue + (increment ? 1 : -1), true);
    }

    /// The text shown for `value`.
    private String format(int value) {
        if (mDisplayedValues != null) {
            int i = value - mMinValue;
            return i >= 0 && i < mDisplayedValues.length ? mDisplayedValues[i] : "";
        }
        return mFormatter != null ? mFormatter.format(value) : String.valueOf(value);
    }

    // ------------------------------------------------------------ measure & draw

    private int rowHeight() {
        return mDividerDistance;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int textWidth = 0;
        if (mDisplayedValues != null) {
            for (String s : mDisplayedValues) {
                textWidth = Math.max(textWidth, Math.round(mTextPaint.measureText(s == null ? "" : s)));
            }
        } else {
            for (int v = mMinValue; v <= mMaxValue && v - mMinValue < 200; v++) {
                textWidth = Math.max(textWidth, Math.round(mTextPaint.measureText(format(v))));
            }
        }
        int w = Math.max(mMinWidth, textWidth + Math.round(dp(16)) + getPaddingLeft() + getPaddingRight());
        int h = Math.min(mMaxHeight, rowHeight() * SELECTOR_WHEEL_ITEM_COUNT + getPaddingTop() + getPaddingBottom());
        h = Math.max(h, getSuggestedMinimumHeight());
        setMeasuredDimension(resolveSizeAndState(w, widthMeasureSpec, 0), resolveSizeAndState(h, heightMeasureSpec, 0));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        int row = rowHeight();
        float center = h / 2f;
        float textMid = -(mTextPaint.ascent() + mTextPaint.descent()) / 2f;
        boolean wrap = getWrapSelectorWheel();
        for (int i = -2; i <= 2; i++) {
            int value = mValue + i;
            if (!wrap && (value < mMinValue || value > mMaxValue)) {
                continue;
            }
            if (wrap) {
                value = wrap(value);
            }
            float y = center + i * row + mOffset;
            float distance = Math.abs(y - center) / row;
            if (distance >= 2f) {
                continue;
            }
            int alpha = Math.round(((mTextColor >>> 24) & 0xff) * Math.max(0f, 1f - distance * 0.6f));
            mTextPaint.setColor((mTextColor & 0xffffff) | (alpha << 24));
            String text = format(value);
            float x = (w - mTextPaint.measureText(text)) / 2f;
            canvas.drawText(text, x, y + textMid, mTextPaint);
        }
        mDividerPaint.setColor(mDividerColor);
        float top = center - mDividerDistance / 2f;
        float bottom = center + mDividerDistance / 2f;
        canvas.drawRect(0, top - mDividerHeight / 2f, w, top + mDividerHeight / 2f, mDividerPaint);
        canvas.drawRect(0, bottom - mDividerHeight / 2f, w, bottom + mDividerHeight / 2f, mDividerPaint);
    }

    // ------------------------------------------------------------ touch

    private void setScrollState(int state) {
        if (mScrollState != state) {
            mScrollState = state;
            if (mOnScrollListener != null) {
                mOnScrollListener.onScrollStateChange(this, state);
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // The wheel scrolls vertically; it takes every gesture that starts on it.
        return isEnabled() && ev.getActionMasked() == MotionEvent.ACTION_DOWN;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        int row = rowHeight();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                mDownY = event.getY();
                mLastY = mDownY;
                mDragging = false;
                ViewParent p = getParent();
                if (p != null) {
                    p.requestDisallowInterceptTouchEvent(true);
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                float y = event.getY();
                if (!mDragging && Math.abs(y - mDownY) > mTouchSlop) {
                    mDragging = true;
                    setScrollState(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                }
                if (mDragging) {
                    mOffset += y - mLastY;
                    // Content moving down shows smaller values.
                    while (mOffset > row / 2f) {
                        if (!getWrapSelectorWheel() && mValue <= mMinValue) {
                            mOffset = Math.min(mOffset, row / 2f);
                            break;
                        }
                        mOffset -= row;
                        changeValueByOne(false);
                    }
                    while (mOffset < -row / 2f) {
                        if (!getWrapSelectorWheel() && mValue >= mMaxValue) {
                            mOffset = Math.max(mOffset, -row / 2f);
                            break;
                        }
                        mOffset += row;
                        changeValueByOne(true);
                    }
                    invalidate();
                }
                mLastY = y;
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (!mDragging) {
                    float center = getHeight() / 2f;
                    float y = event.getY();
                    if (y < center - mDividerDistance / 2f) {
                        changeValueByOne(false);
                    } else if (y > center + mDividerDistance / 2f) {
                        changeValueByOne(true);
                    } else {
                        performClick();
                    }
                }
                mOffset = 0;
                mDragging = false;
                setScrollState(OnScrollListener.SCROLL_STATE_IDLE);
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                mOffset = 0;
                mDragging = false;
                setScrollState(OnScrollListener.SCROLL_STATE_IDLE);
                invalidate();
                return true;
            default:
                return true;
        }
    }

    public CharSequence getAccessibilityClassName() {
        return NumberPicker.class.getName();
    }
}
