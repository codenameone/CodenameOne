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

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;

/// A `setMinDate`/`setMaxDate` that moves the selection to the new bound
/// tells the date listener, as Android's calendar-mode picker does. A bound
/// that leaves the selection alone reports nothing.
public class DatePickerRangeClampNotifiesTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static long date(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, day, 12, 0, 0);
        return c.getTime().getTime();
    }

    @Test
    public void clampingBoundsNotify() {
        DatePicker picker = new DatePicker(AndroidTestSupport.context());
        final int[] last = new int[4];
        picker.init(2020, Calendar.JUNE, 15, new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                last[0]++;
                last[1] = year;
                last[2] = monthOfYear;
                last[3] = dayOfMonth;
            }
        });
        picker.setMinDate(date(2019, Calendar.JANUARY, 1));
        assertEquals("selection unchanged, nothing reported", 0, last[0]);

        picker.setMinDate(date(2021, Calendar.MARCH, 3));
        assertEquals(1, last[0]);
        assertEquals(2021, last[1]);
        assertEquals(Calendar.MARCH, last[2]);
        assertEquals(3, last[3]);

        picker.setMaxDate(date(2030, Calendar.JANUARY, 1));
        assertEquals("selection unchanged, nothing reported", 1, last[0]);

        picker.setMaxDate(date(2021, Calendar.MARCH, 2));
        assertEquals(2, last[0]);
        assertEquals(picker.getYear(), last[1]);
        assertEquals(picker.getMonth(), last[2]);
        assertEquals(picker.getDayOfMonth(), last[3]);
        assertEquals(2, picker.getDayOfMonth());
    }
}
