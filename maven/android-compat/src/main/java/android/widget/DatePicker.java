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
import android.view.ViewGroup;

import java.util.Calendar;
import java.util.Date;

/// Picks a date with three wheels: month, day and year. Android's calendar
/// mode (`datePickerMode="calendar"`) is shown in this spinner form too.
/// Scrolling a day or month wheel past its end rolls the month or year over,
/// and the date is kept between the minimum and maximum dates.
public class DatePicker extends FrameLayout {

    public interface OnDateChangedListener {
        void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth);
    }

    private static final String[] FALLBACK_MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep",
        "Oct", "Nov", "Dec"};

    private final NumberPicker mMonthSpinner;
    private final NumberPicker mDaySpinner;
    private final NumberPicker mYearSpinner;
    private final String[] mShortMonths;
    private OnDateChangedListener mOnDateChangedListener;
    private OnDateChangedListener mAutoFillChangeListener;
    private int mYear;
    private int mMonth;
    private int mDay;
    private int mMinYear = 1900;
    private int mMinMonth;
    private int mMinDay = 1;
    private int mMaxYear = 2100;
    private int mMaxMonth = 11;
    private int mMaxDay = 31;
    private int mFirstDayOfWeek = Calendar.SUNDAY;
    private boolean mSpinnersShown = true;

    public DatePicker(Context context) {
        this(context, null);
    }

    public DatePicker(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.datePickerStyle);
    }

    public DatePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DatePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.DatePicker, defStyleAttr, defStyleRes);
        int startYear = a.getInt(android.R.styleable.DatePicker_startYear, 1900);
        int endYear = a.getInt(android.R.styleable.DatePicker_endYear, 2100);
        String minDate = a.getString(android.R.styleable.DatePicker_minDate);
        String maxDate = a.getString(android.R.styleable.DatePicker_maxDate);
        mFirstDayOfWeek = a.getInt(android.R.styleable.DatePicker_firstDayOfWeek, Calendar.SUNDAY);
        mSpinnersShown = a.getBoolean(android.R.styleable.DatePicker_spinnersShown, true);
        a.recycle();
        mShortMonths = shortMonths();
        mMinYear = startYear;
        mMinMonth = 0;
        mMinDay = 1;
        mMaxYear = endYear;
        mMaxMonth = 11;
        mMaxDay = 31;
        int[] parsed = parseDate(minDate);
        if (parsed != null) {
            mMinYear = parsed[0];
            mMinMonth = parsed[1];
            mMinDay = parsed[2];
        }
        parsed = parseDate(maxDate);
        if (parsed != null) {
            mMaxYear = parsed[0];
            mMaxMonth = parsed[1];
            mMaxDay = parsed[2];
        }

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        NumberPicker.OnValueChangeListener listener = new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                onSpinnerChanged(picker, oldVal, newVal);
            }
        };
        mMonthSpinner = new NumberPicker(context);
        mDaySpinner = new NumberPicker(context);
        mYearSpinner = new NumberPicker(context);
        mDaySpinner.setFormatter(NumberPicker.TWO_DIGITS);
        NumberPicker[] spinners = {mMonthSpinner, mDaySpinner, mYearSpinner};
        int gap = Math.round(8 * getResources().getDisplayMetrics().density);
        for (NumberPicker p : spinners) {
            p.setOnValueChangedListener(listener);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = gap;
            lp.rightMargin = gap;
            row.addView(p, lp);
        }
        addView(row, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        Calendar now = Calendar.getInstance();
        mYear = now.get(Calendar.YEAR);
        mMonth = now.get(Calendar.MONTH);
        mDay = now.get(Calendar.DAY_OF_MONTH);
        clampDate();
        updateSpinners();
    }

    @Override
    protected android.os.Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), mYear, mMonth, mDay);
    }

    @Override
    protected void onRestoreInstanceState(android.os.Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState saved = (SavedState) state;
        super.onRestoreInstanceState(saved.superState);
        // Restore the spinners without reporting a new user selection.
        init(saved.year, saved.month, saved.day, mOnDateChangedListener);
    }

    /// In-memory view state for activity recreation, like the other widgets.
    private static final class SavedState implements android.os.Parcelable {
        final android.os.Parcelable superState;
        final int year;
        final int month;
        final int day;

        SavedState(android.os.Parcelable superState, int year, int month, int day) {
            this.superState = superState;
            this.year = year;
            this.month = month;
            this.day = day;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(android.os.Parcel dest, int flags) {
        }
    }

    private static String[] shortMonths() {
        try {
            String[] m = new com.codename1.l10n.DateFormatSymbols().getShortMonths();
            if (m != null && m.length >= 12) {
                String[] out = new String[12];
                System.arraycopy(m, 0, out, 0, 12);
                return out;
            }
        } catch (RuntimeException e) {
            com.codename1.io.Log.e(e);
        }
        return FALLBACK_MONTHS;
    }

    /// `MM/dd/yyyy`, the format of the `minDate` and `maxDate` attributes.
    static int[] parseDate(String s) {
        if (s == null) {
            return null;
        }
        int a = s.indexOf('/');
        int b = a < 0 ? -1 : s.indexOf('/', a + 1);
        if (a < 0 || b < 0) {
            return null;
        }
        try {
            int month = Integer.parseInt(s.substring(0, a).trim()) - 1;
            int day = Integer.parseInt(s.substring(a + 1, b).trim());
            int year = Integer.parseInt(s.substring(b + 1).trim());
            return new int[] {year, month, day};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static boolean isLeap(int year) {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
    }

    static int daysInMonth(int year, int month) {
        switch (month) {
            case 1:
                return isLeap(year) ? 29 : 28;
            case 3:
            case 5:
            case 8:
            case 10:
                return 30;
            default:
                return 31;
        }
    }

    private static int compare(int y1, int m1, int d1, int y2, int m2, int d2) {
        if (y1 != y2) {
            return y1 < y2 ? -1 : 1;
        }
        if (m1 != m2) {
            return m1 < m2 ? -1 : 1;
        }
        return d1 < d2 ? -1 : d1 == d2 ? 0 : 1;
    }

    private void clampDate() {
        if (compare(mYear, mMonth, mDay, mMinYear, mMinMonth, mMinDay) < 0) {
            mYear = mMinYear;
            mMonth = mMinMonth;
            mDay = mMinDay;
        } else if (compare(mYear, mMonth, mDay, mMaxYear, mMaxMonth, mMaxDay) > 0) {
            mYear = mMaxYear;
            mMonth = mMaxMonth;
            mDay = mMaxDay;
        }
        mDay = Math.min(mDay, daysInMonth(mYear, mMonth));
    }

    /// Adds `months` to the date, keeping the day within the new month.
    private void addMonths(int months) {
        int total = mYear * 12 + mMonth + months;
        mYear = total / 12;
        mMonth = total % 12;
        mDay = Math.min(mDay, daysInMonth(mYear, mMonth));
    }

    private void addDays(int days) {
        mDay += days;
        while (mDay > daysInMonth(mYear, mMonth)) {
            mDay -= daysInMonth(mYear, mMonth);
            addMonthsKeepDay(1);
        }
        while (mDay < 1) {
            addMonthsKeepDay(-1);
            mDay += daysInMonth(mYear, mMonth);
        }
    }

    private void addMonthsKeepDay(int months) {
        int total = mYear * 12 + mMonth + months;
        mYear = total / 12;
        mMonth = total % 12;
    }

    /// Android's rules: a wheel that wraps past its end moves the next field.
    private void onSpinnerChanged(NumberPicker picker, int oldVal, int newVal) {
        if (picker == mDaySpinner) {
            int max = picker.getMaxValue();
            if (oldVal == max && newVal == picker.getMinValue()) {
                addDays(1);
            } else if (oldVal == picker.getMinValue() && newVal == max) {
                addDays(-1);
            } else {
                addDays(newVal - oldVal);
            }
        } else if (picker == mMonthSpinner) {
            if (oldVal == 11 && newVal == 0) {
                addMonths(1);
            } else if (oldVal == 0 && newVal == 11) {
                addMonths(-1);
            } else {
                addMonths(newVal - oldVal);
            }
        } else {
            mYear = newVal;
            mDay = Math.min(mDay, daysInMonth(mYear, mMonth));
        }
        clampDate();
        updateSpinners();
        notifyDateChanged();
    }

    private void updateSpinners() {
        int dayMin = 1;
        int dayMax = daysInMonth(mYear, mMonth);
        boolean dayWrap = true;
        int monthMin = 0;
        int monthMax = 11;
        boolean monthWrap = true;
        if (mYear == mMinYear) {
            monthMin = mMinMonth;
            monthWrap = false;
            if (mMonth == mMinMonth) {
                dayMin = mMinDay;
                dayWrap = false;
            }
        }
        if (mYear == mMaxYear) {
            monthMax = mMaxMonth;
            monthWrap = false;
            if (mMonth == mMaxMonth) {
                dayMax = Math.min(dayMax, mMaxDay);
                dayWrap = false;
            }
        }
        mDaySpinner.setMinValue(dayMin);
        mDaySpinner.setMaxValue(dayMax);
        mDaySpinner.setWrapSelectorWheel(dayWrap);
        mMonthSpinner.setDisplayedValues(null);
        mMonthSpinner.setMinValue(monthMin);
        mMonthSpinner.setMaxValue(monthMax);
        String[] names = new String[monthMax - monthMin + 1];
        System.arraycopy(mShortMonths, monthMin, names, 0, names.length);
        mMonthSpinner.setDisplayedValues(names);
        mMonthSpinner.setWrapSelectorWheel(monthWrap);
        mYearSpinner.setMinValue(mMinYear);
        mYearSpinner.setMaxValue(mMaxYear);
        mYearSpinner.setWrapSelectorWheel(false);
        mYearSpinner.setValue(mYear);
        mMonthSpinner.setValue(mMonth);
        mDaySpinner.setValue(mDay);
    }

    private void notifyDateChanged() {
        if (mOnDateChangedListener != null) {
            mOnDateChangedListener.onDateChanged(this, mYear, mMonth, mDay);
        }
        if (mAutoFillChangeListener != null) {
            mAutoFillChangeListener.onDateChanged(this, mYear, mMonth, mDay);
        }
    }

    // ------------------------------------------------------------ API

    public void init(int year, int monthOfYear, int dayOfMonth, OnDateChangedListener onDateChangedListener) {
        mYear = year;
        mMonth = monthOfYear;
        mDay = dayOfMonth;
        clampDate();
        updateSpinners();
        mOnDateChangedListener = onDateChangedListener;
    }

    public void setOnDateChangedListener(OnDateChangedListener onDateChangedListener) {
        mOnDateChangedListener = onDateChangedListener;
    }

    /// Runtime use: a second listener, for the dialog that hosts the picker.
    void setInternalListener(OnDateChangedListener listener) {
        mAutoFillChangeListener = listener;
    }

    public void updateDate(int year, int month, int dayOfMonth) {
        if (year == mYear && month == mMonth && dayOfMonth == mDay) {
            return;
        }
        mYear = year;
        mMonth = month;
        mDay = dayOfMonth;
        clampDate();
        updateSpinners();
        notifyDateChanged();
    }

    public int getYear() {
        return mYear;
    }

    public int getMonth() {
        return mMonth;
    }

    public int getDayOfMonth() {
        return mDay;
    }

    private static int[] fields(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(millis));
        return new int[] {c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)};
    }

    private static long millis(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime().getTime();
    }

    public void setMinDate(long minDate) {
        int[] f = fields(minDate);
        mMinYear = f[0];
        mMinMonth = f[1];
        mMinDay = f[2];
        clampAfterRangeChange();
    }

    public long getMinDate() {
        return millis(mMinYear, mMinMonth, mMinDay);
    }

    public void setMaxDate(long maxDate) {
        int[] f = fields(maxDate);
        mMaxYear = f[0];
        mMaxMonth = f[1];
        mMaxDay = f[2];
        clampAfterRangeChange();
    }

    /// A new bound that moves the selection tells the listeners, as
    /// Android's calendar-mode picker does, so a model observing the picker
    /// does not keep a date the picker no longer shows.
    private void clampAfterRangeChange() {
        int year = mYear;
        int month = mMonth;
        int day = mDay;
        clampDate();
        updateSpinners();
        if (year != mYear || month != mMonth || day != mDay) {
            notifyDateChanged();
        }
    }

    public long getMaxDate() {
        return millis(mMaxYear, mMaxMonth, mMaxDay);
    }

    public void setFirstDayOfWeek(int firstDayOfWeek) {
        mFirstDayOfWeek = firstDayOfWeek;
    }

    public int getFirstDayOfWeek() {
        return mFirstDayOfWeek;
    }

    @Deprecated
    public boolean getSpinnersShown() {
        return mSpinnersShown;
    }

    /// Recorded and reported, but the wheels stay visible. On Android the
    /// flag hides the spinners only in spinner mode, where it is paired with
    /// `setCalendarViewShown(true)` to swap in the calendar; calendar mode
    /// ignores it. This picker has no calendar view and draws every mode as
    /// wheels, so hiding them would leave an empty widget where Android shows
    /// a calendar.
    @Deprecated
    public void setSpinnersShown(boolean shown) {
        mSpinnersShown = shown;
    }

    @Deprecated
    public boolean getCalendarViewShown() {
        return false;
    }

    @Deprecated
    public void setCalendarViewShown(boolean shown) {
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mMonthSpinner.setEnabled(enabled);
        mDaySpinner.setEnabled(enabled);
        mYearSpinner.setEnabled(enabled);
    }

    public CharSequence getAccessibilityClassName() {
        return DatePicker.class.getName();
    }
}
