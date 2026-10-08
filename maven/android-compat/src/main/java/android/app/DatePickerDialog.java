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
import android.widget.DatePicker;

import java.util.Calendar;

/// A dialog holding a [DatePicker], with OK and Cancel buttons. OK reports
/// the picked date to the [OnDateSetListener].
public class DatePickerDialog extends AlertDialog implements DialogInterface.OnClickListener,
        DatePicker.OnDateChangedListener {

    public interface OnDateSetListener {
        void onDateSet(DatePicker view, int year, int month, int dayOfMonth);
    }

    private final DatePicker mDatePicker;
    private OnDateSetListener mDateSetListener;

    /// A dialog showing today's date.
    public DatePickerDialog(Context context) {
        this(context, 0, null, -1, -1, -1);
    }

    public DatePickerDialog(Context context, int themeResId) {
        this(context, themeResId, null, -1, -1, -1);
    }

    public DatePickerDialog(Context context, OnDateSetListener listener, int year, int month, int dayOfMonth) {
        this(context, 0, listener, year, month, dayOfMonth);
    }

    public DatePickerDialog(Context context, int themeResId, OnDateSetListener listener, int year, int monthOfYear,
                            int dayOfMonth) {
        super(context, themeResId);
        mDateSetListener = listener;
        mDatePicker = new DatePicker(getContext());
        if (year < 0) {
            Calendar now = Calendar.getInstance();
            year = now.get(Calendar.YEAR);
            monthOfYear = now.get(Calendar.MONTH);
            dayOfMonth = now.get(Calendar.DAY_OF_MONTH);
        }
        mDatePicker.init(year, monthOfYear, dayOfMonth, this);
        setView(mDatePicker);
        setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok), this);
        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), this);
    }

    @Override
    public void onDateChanged(DatePicker view, int year, int month, int dayOfMonth) {
    }

    public void setOnDateSetListener(OnDateSetListener listener) {
        mDateSetListener = listener;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == BUTTON_POSITIVE) {
            if (mDateSetListener != null) {
                mDatePicker.clearFocus();
                mDateSetListener.onDateSet(mDatePicker, mDatePicker.getYear(), mDatePicker.getMonth(),
                        mDatePicker.getDayOfMonth());
            }
        } else if (which == BUTTON_NEGATIVE) {
            cancel();
        }
    }

    public DatePicker getDatePicker() {
        return mDatePicker;
    }

    public void updateDate(int year, int month, int dayOfMonth) {
        mDatePicker.updateDate(year, month, dayOfMonth);
    }
}
