/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.slate;

import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.spinner.MaterialDatePicker;
import com.codename1.ui.spinner.MaterialTimePicker;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.slate.client.Times;
import com.codenameone.examples.slate.ui.Look;

import java.util.Date;

/// A provider sets a working hour on the Material clock face and a block of
/// time off on the calendar and clock: quarter hours, Cancel, and an end that
/// stays after its start.
public class HoursPickerTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        onEdt(() -> AppConfig.setMode(""));
        signIn(PROVIDER, "Provider");
        click("menu-open");
        click("menu-hours");
        waitForForm("Hours");
        until(() -> has("save"), 20000, "the week");

        // The hand stops on the quarter hours, and OK keeps where it stopped.
        click("start-1");
        until(() -> open(MaterialTimePicker.class) != null, 20000, "the clock face");
        final int[] step = new int[1];
        onEdt(() -> {
            step[0] = open(MaterialTimePicker.class).getMinuteStep();
            open(MaterialTimePicker.class).setTime(8, 30);
        });
        assertEqual(15, step[0], "the clock turns in quarter hours");
        shot("provider-hours-time");
        click("PickerMaterialOk");
        waitForForm("Hours");

        // Dark: the same face on the dark surface, the hand still the brand's.
        final int[] light = new int[2];
        final int[] dark = new int[2];
        onEdt(() -> {
            face(light);
            Prefs.setTheme(Prefs.THEME_DARK);
            Look.apply();
        });
        click("start-1");
        until(() -> open(MaterialTimePicker.class) != null, 20000, "the clock face");
        onEdt(() -> face(dark));
        shot("provider-hours-time-dark");
        onEdt(() -> open(MaterialTimePicker.class).setTime(11, 15));
        back();
        waitForForm("Hours");
        onEdt(() -> {
            Prefs.setTheme(Prefs.THEME_LIGHT);
            Look.apply();
        });
        assertTrue(luma(light[0]) > 200, "the light clock face is light");
        assertTrue(luma(dark[0]) < 80, "the dark clock face is dark");
        assertTrue(light[1] != light[0] && dark[1] != dark[0], "the hand stands out in both");
        assertEqual(8 * 60 + 30, time("start-1"), "Back out of the dark face left the start alone");
        assertEqual(8 * 60 + 30, time("start-1"), "the time confirmed is the start");

        // A time between two quarters, as one typed can be, goes back to one.
        click("start-1");
        until(() -> open(MaterialTimePicker.class) != null, 20000, "the clock face");
        onEdt(() -> open(MaterialTimePicker.class).setTime(9, 40));
        click("PickerMaterialOk");
        waitForForm("Hours");
        assertEqual(9 * 60 + 30, time("start-1"), "a start is kept to the quarter hour");

        // Cancel leaves the end what it was.
        final int end = time("end-1");
        click("end-1");
        until(() -> open(MaterialTimePicker.class) != null, 20000, "the clock face");
        onEdt(() -> open(MaterialTimePicker.class).setTime(end / 60 == 21 ? 20 : 21, 15));
        click("PickerMaterialCancel");
        waitForForm("Hours");
        assertEqual(end, time("end-1"), "a cancelled clock leaves the end alone");
        // Nothing is saved: the week the other tests book into stays as it was.
        back();
        waitForForm("Provider");

        click("menu-open");
        click("menu-time-off");
        waitForForm("TimeOff");
        until(() -> has("off-add"), 20000, "the time off screen");
        final long today = Times.dayStart(System.currentTimeMillis());
        final long was = date("off-from");
        assertTrue(date("off-to") > was, "time off starts with an end after its start");

        // The start moved past the end: the end follows, the same length on.
        final long length = date("off-to") - was;
        click("off-from");
        until(() -> open(MaterialDatePicker.class) != null, 20000, "the calendar");
        final long[] read = new long[1];
        onEdt(() -> {
            // Bounds: time off cannot start before today.
            open(MaterialDatePicker.class).setDate(new Date(Times.addDays(today, -3)));
            read[0] = Times.dayStart(open(MaterialDatePicker.class).getDate().getTime());
            open(MaterialDatePicker.class).setDate(new Date(Times.addDays(today, 6) + 9 * Times.HOUR));
        });
        assertEqual(today, read[0], "a day before today is brought to today");
        shot("provider-time-off-date");
        click("PickerMaterialOk");
        waitForForm("TimeOff");
        assertEqual(Times.addDays(today, 6), Times.dayStart(date("off-from")),
                "the day confirmed is the start");
        assertEqual(date("off-from") + length, date("off-to"), "the end followed the start");

        // Cancel, by Back, leaves the end alone.
        final long until = date("off-to");
        click("off-to");
        until(() -> open(MaterialDatePicker.class) != null, 20000, "the calendar");
        onEdt(() -> open(MaterialDatePicker.class).setDate(new Date(Times.addDays(today, 9))));
        back();
        waitForForm("TimeOff");
        assertEqual(until, date("off-to"), "a cancelled calendar leaves the end alone");
        return true;
    }

    private int time(final String name) {
        final int[] minute = new int[1];
        onEdt(() -> minute[0] = ((Picker) findByName(name)).getTime());
        return minute[0];
    }

    private long date(final String name) {
        final long[] millis = new long[1];
        onEdt(() -> millis[0] = ((Picker) findByName(name)).getDate().getTime());
        return millis[0];
    }

    // The colours the clock face is drawn in: the face, and the hand over it.
    private static void face(int[] into) {
        UIManager m = UIManager.getInstance();
        into[0] = m.getComponentStyle("PickerMaterialDial").getBgColor();
        into[1] = m.getComponentSelectedStyle("PickerMaterialDial").getBgColor();
    }

    private static int luma(int rgb) {
        return (((rgb >> 16) & 0xff) * 299 + ((rgb >> 8) & 0xff) * 587 + (rgb & 0xff) * 114) / 1000;
    }
}
