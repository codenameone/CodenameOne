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
package android.app;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.TimePicker;

/// A dialog holding a [TimePicker], with OK and Cancel buttons. OK reports
/// the picked time to the [OnTimeSetListener].
public class TimePickerDialog extends AlertDialog implements DialogInterface.OnClickListener,
        TimePicker.OnTimeChangedListener {

    public interface OnTimeSetListener {
        void onTimeSet(TimePicker view, int hourOfDay, int minute);
    }

    private final TimePicker mTimePicker;
    private final OnTimeSetListener mTimeSetListener;

    public TimePickerDialog(Context context, OnTimeSetListener listener, int hourOfDay, int minute,
                            boolean is24HourView) {
        this(context, 0, listener, hourOfDay, minute, is24HourView);
    }

    public TimePickerDialog(Context context, int themeResId, OnTimeSetListener listener, int hourOfDay, int minute,
                            boolean is24HourView) {
        super(context, themeResId);
        mTimeSetListener = listener;
        mTimePicker = new TimePicker(getContext());
        mTimePicker.setIs24HourView(Boolean.valueOf(is24HourView));
        mTimePicker.setHour(hourOfDay);
        mTimePicker.setMinute(minute);
        mTimePicker.setOnTimeChangedListener(this);
        setView(mTimePicker);
        setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok), this);
        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), this);
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == BUTTON_POSITIVE) {
            if (mTimeSetListener != null) {
                mTimeSetListener.onTimeSet(mTimePicker, mTimePicker.getHour(), mTimePicker.getMinute());
            }
        } else if (which == BUTTON_NEGATIVE) {
            cancel();
        }
    }

    public void updateTime(int hourOfDay, int minuteOfHour) {
        mTimePicker.setHour(hourOfDay);
        mTimePicker.setMinute(minuteOfHour);
    }
}
