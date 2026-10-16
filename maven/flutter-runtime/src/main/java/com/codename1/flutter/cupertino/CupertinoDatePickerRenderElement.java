/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.cupertino;

import com.codename1.flutter.Widget;
import com.codename1.ui.Container;
import com.codename1.ui.spinner.DateSpinner3D;
import com.codename1.ui.spinner.DateTimeSpinner3D;
import com.codename1.ui.spinner.InternalPickerWidget;
import com.codename1.ui.spinner.TimeSpinner3D;

import dart.core.DateTime;

import java.util.Calendar;
import java.util.Date;

/**
 * {@link CupertinoDatePicker}: one of Codename One's three date/time wheels, chosen by
 * the mode. {@code date} is a day-month-year spinner, {@code time} an hour-minute one,
 * and the default {@code dateAndTime} the combined wheel.
 *
 * <p>{@code monthYear} has no spinner of its own and uses the date wheel, which shows one
 * column more than it was asked for. That is a visible extra column rather than a missing
 * control, and the gallery does not ask for the mode.</p>
 */
class CupertinoDatePickerRenderElement extends CupertinoWheelRenderElement {

    CupertinoDatePickerRenderElement(Widget widget) {
        super(widget);
    }

    private CupertinoDatePicker picker() {
        return (CupertinoDatePicker) widget();
    }

    @Override
    protected Container createWheel() {
        CupertinoDatePickerMode mode = picker().getMode();
        Date initial = toDate(picker().getInitialDateTime());
        if (mode == CupertinoDatePickerMode.time) {
            TimeSpinner3D t = new TimeSpinner3D();
            if (initial != null) {
                Calendar c = Calendar.getInstance();
                c.setTime(initial);
                t.setValue(Integer.valueOf(
                        c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)));
            }
            return t;
        }
        if (mode == CupertinoDatePickerMode.dateAndTime || mode == null) {
            DateTimeSpinner3D d = new DateTimeSpinner3D();
            if (initial != null) {
                d.setValue(initial);
            }
            return d;
        }
        DateSpinner3D d = new DateSpinner3D();
        if (initial != null) {
            d.setValue(initial);
        }
        return d;
    }

    private static Date toDate(DateTime dt) {
        return dt == null ? null : new Date(dt.millisecondsSinceEpoch());
    }

    @Override
    protected void report(Object value) {
        dart.runtime.Funcs.VoidFunc1<DateTime> f = picker().getOnDateTimeChanged();
        if (f == null) {
            return;
        }
        Date d = asDate(value);
        if (d != null) {
            f.call(DateTime.fromMillisecondsSinceEpoch(d.getTime(), false));
        }
    }

    /**
     * The spinners answer in their own currency: a Date from the date wheels, minutes
     * past midnight from the time one. Both become the instant the Dart expects.
     */
    private Date asDate(Object value) {
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Integer) {
            Calendar c = Calendar.getInstance();
            Date base = toDate(picker().getInitialDateTime());
            if (base != null) {
                c.setTime(base);
            }
            int minutes = ((Integer) value).intValue();
            c.set(Calendar.HOUR_OF_DAY, minutes / 60);
            c.set(Calendar.MINUTE, minutes % 60);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTime();
        }
        return null;
    }
}
