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

import static org.junit.Assert.assertEquals;

/// A null passed to the legacy `setCurrentHour`/`setCurrentMinute` leaves
/// the time alone. It used to reset the field to zero, and tell the
/// listener, so a selected time jumped to midnight.
public class TimePickerNullLegacyTimeTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    @SuppressWarnings("deprecation")
    public void nullLegacyValuesAreIgnored() {
        TimePicker picker = new TimePicker(AndroidTestSupport.context());
        picker.setHour(14);
        picker.setMinute(35);
        final int[] changes = new int[1];
        picker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                changes[0]++;
            }
        });
        picker.setCurrentHour(null);
        picker.setCurrentMinute(null);
        assertEquals(14, picker.getHour());
        assertEquals(35, picker.getMinute());
        assertEquals("no change was made, so none is reported", 0, changes[0]);
    }
}
