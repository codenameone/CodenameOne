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
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import java.util.Calendar;

/// Picks a time of day with wheels: hour, minute and (in 12-hour mode)
/// AM/PM. Android's clock mode (`timePickerMode="clock"`) is shown in this
/// spinner form too. Scrolling minutes past the hour moves the hour, and
/// scrolling the hour past 11 flips AM/PM, as on Android.
public class TimePicker extends FrameLayout {

    public interface OnTimeChangedListener {
        void onTimeChanged(TimePicker view, int hourOfDay, int minute);
    }

    private final NumberPicker mHourSpinner;
    private final NumberPicker mMinuteSpinner;
    private final NumberPicker mAmPmSpinner;
    private final TextView mDivider;
    private final String[] mAmPmStrings;
    private OnTimeChangedListener mOnTimeChangedListener;
    private OnTimeChangedListener mInternalListener;
    private boolean mIs24HourView;
    private int mHour;
    private int mMinute;

    public TimePicker(Context context) {
        this(context, null);
    }

    public TimePicker(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.timePickerStyle);
    }

    public TimePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TimePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.TimePicker, defStyleAttr, defStyleRes);
        a.recycle();
        mAmPmStrings = amPm();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        NumberPicker.OnValueChangeListener listener = new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                onSpinnerChanged(picker, oldVal, newVal);
            }
        };
        mHourSpinner = new NumberPicker(context);
        mMinuteSpinner = new NumberPicker(context);
        mAmPmSpinner = new NumberPicker(context);
        NumberPicker.Formatter twoDigits = NumberPicker.TWO_DIGITS;
        mMinuteSpinner.setMinValue(0);
        mMinuteSpinner.setMaxValue(59);
        mMinuteSpinner.setFormatter(twoDigits);
        mAmPmSpinner.setMinValue(0);
        mAmPmSpinner.setMaxValue(1);
        mAmPmSpinner.setDisplayedValues(mAmPmStrings);
        mDivider = new TextView(context);
        mDivider.setText(":");
        mDivider.setTextSize(16);
        mDivider.setGravity(Gravity.CENTER);
        int gap = Math.round(8 * getResources().getDisplayMetrics().density);
        View[] parts = {mHourSpinner, mDivider, mMinuteSpinner, mAmPmSpinner};
        for (View p : parts) {
            if (p instanceof NumberPicker) {
                ((NumberPicker) p).setOnValueChangedListener(listener);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = p == mDivider ? 0 : gap;
            lp.rightMargin = p == mDivider ? 0 : gap;
            row.addView(p, lp);
        }
        addView(row, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        Calendar now = Calendar.getInstance();
        mHour = now.get(Calendar.HOUR_OF_DAY);
        mMinute = now.get(Calendar.MINUTE);
        updateSpinners();
    }

    @Override
    protected android.os.Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), mHour, mMinute, mIs24HourView);
    }

    @Override
    protected void onRestoreInstanceState(android.os.Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState saved = (SavedState) state;
        super.onRestoreInstanceState(saved.superState);
        mHour = saved.hour;
        mMinute = saved.minute;
        mIs24HourView = saved.is24Hour;
        updateSpinners();
    }

    /// In-memory state for view recreation, matching the other widgets.
    private static final class SavedState implements android.os.Parcelable {
        final android.os.Parcelable superState;
        final int hour;
        final int minute;
        final boolean is24Hour;

        SavedState(android.os.Parcelable superState, int hour, int minute, boolean is24Hour) {
            this.superState = superState;
            this.hour = hour;
            this.minute = minute;
            this.is24Hour = is24Hour;
        }

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(android.os.Parcel dest, int flags) { }
    }

    private static String[] amPm() {
        try {
            String[] s = new com.codename1.l10n.DateFormatSymbols().getAmPmStrings();
            if (s != null && s.length >= 2) {
                return new String[] {s[0], s[1]};
            }
        } catch (RuntimeException e) {
            com.codename1.io.Log.e(e);
        }
        return new String[] {"AM", "PM"};
    }

    private void onSpinnerChanged(NumberPicker picker, int oldVal, int newVal) {
        if (picker == mMinuteSpinner) {
            int hourDelta = 0;
            if (oldVal == 59 && newVal == 0) {
                hourDelta = 1;
            } else if (oldVal == 0 && newVal == 59) {
                hourDelta = -1;
            }
            mMinute = newVal;
            mHour = (mHour + hourDelta + 24) % 24;
        } else if (picker == mHourSpinner) {
            if (mIs24HourView) {
                mHour = newVal;
            } else {
                boolean pm = mHour >= 12;
                // 11 -> 12 or 12 -> 11 crosses noon or midnight.
                if ((oldVal == 11 && newVal == 12) || (oldVal == 12 && newVal == 11)) {
                    pm = !pm;
                }
                mHour = (newVal % 12) + (pm ? 12 : 0);
            }
        } else {
            mHour = (mHour % 12) + (newVal == 1 ? 12 : 0);
        }
        updateSpinners();
        notifyChanged();
    }

    private void updateSpinners() {
        if (mIs24HourView) {
            mHourSpinner.setMinValue(0);
            mHourSpinner.setMaxValue(23);
            mHourSpinner.setFormatter(NumberPicker.TWO_DIGITS);
            mHourSpinner.setValue(mHour);
            mAmPmSpinner.setVisibility(View.GONE);
        } else {
            mHourSpinner.setMinValue(1);
            mHourSpinner.setMaxValue(12);
            mHourSpinner.setFormatter(null);
            int h = mHour % 12;
            mHourSpinner.setValue(h == 0 ? 12 : h);
            mAmPmSpinner.setVisibility(View.VISIBLE);
            mAmPmSpinner.setValue(mHour >= 12 ? 1 : 0);
        }
        mMinuteSpinner.setValue(mMinute);
    }

    private void notifyChanged() {
        if (mOnTimeChangedListener != null) {
            mOnTimeChangedListener.onTimeChanged(this, mHour, mMinute);
        }
        if (mInternalListener != null) {
            mInternalListener.onTimeChanged(this, mHour, mMinute);
        }
    }

    /// Runtime use: a second listener, for the dialog that hosts the picker.
    void setInternalListener(OnTimeChangedListener listener) {
        mInternalListener = listener;
    }

    public void setOnTimeChangedListener(OnTimeChangedListener listener) {
        mOnTimeChangedListener = listener;
    }

    public void setHour(int hour) {
        int h = Math.max(0, Math.min(23, hour));
        if (h != mHour) {
            mHour = h;
            updateSpinners();
            notifyChanged();
        }
    }

    public int getHour() {
        return mHour;
    }

    public void setMinute(int minute) {
        int m = Math.max(0, Math.min(59, minute));
        if (m != mMinute) {
            mMinute = m;
            updateSpinners();
            notifyChanged();
        }
    }

    public int getMinute() {
        return mMinute;
    }

    @Deprecated
    public void setCurrentHour(Integer currentHour) {
        // A null leaves the hour alone, as the legacy picker did, rather than
        // resetting the selection to midnight.
        if (currentHour == null) {
            return;
        }
        setHour(currentHour.intValue());
    }

    @Deprecated
    public Integer getCurrentHour() {
        return Integer.valueOf(mHour);
    }

    @Deprecated
    public void setCurrentMinute(Integer currentMinute) {
        if (currentMinute == null) {
            return;
        }
        setMinute(currentMinute.intValue());
    }

    @Deprecated
    public Integer getCurrentMinute() {
        return Integer.valueOf(mMinute);
    }

    public void setIs24HourView(Boolean is24HourView) {
        boolean v = is24HourView != null && is24HourView.booleanValue();
        if (v != mIs24HourView) {
            mIs24HourView = v;
            updateSpinners();
        }
    }

    public boolean is24HourView() {
        return mIs24HourView;
    }

    public boolean validateInput() {
        return true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mHourSpinner.setEnabled(enabled);
        mMinuteSpinner.setEnabled(enabled);
        mAmPmSpinner.setEnabled(enabled);
    }

    public CharSequence getAccessibilityClassName() {
        return TimePicker.class.getName();
    }
}
