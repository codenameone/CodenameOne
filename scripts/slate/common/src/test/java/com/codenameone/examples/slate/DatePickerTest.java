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

import com.codename1.ui.Button;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.spinner.MaterialDatePicker;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.slate.client.Times;
import com.codenameone.examples.slate.ui.Look;

import java.util.Date;
import java.util.Map;

/// A client chooses the day of a booking on the Material calendar: a day the
/// strip does not have, one that is cancelled, one before today, and the
/// calendar in the light and the dark look.
public class DatePickerTest extends E2e {
    @Override
    @SuppressWarnings("unchecked")
    protected boolean run() throws Exception {
        Actor client = Actor.signIn(CLIENT);
        String serviceId = "";
        for (Object s : client.list("/api/services")) {
            if (serviceId.length() == 0 && s instanceof Map) {
                serviceId = Actor.text((Map<String, Object>) s, "id");
            }
        }
        assertTrue(serviceId.length() > 0, "the demo business offers a service");
        signIn(CLIENT, "Client");
        click("service-" + serviceId);
        waitForForm("Book");
        until(() -> has("day-picker"), 20000, "the calendar button");

        final long today = Times.dayStart(System.currentTimeMillis());
        assertEqual(today, chosenDay(), "booking opens on today");

        // Bounds: yesterday is not a day the calendar will take.
        click("day-picker");
        until(() -> calendar() != null, 20000, "the calendar");
        final long[] read = new long[1];
        onEdt(() -> {
            calendar().setDate(new Date(Times.addDays(today, -1)));
            read[0] = Times.dayStart(calendar().getDate().getTime());
        });
        assertEqual(today, read[0], "a day before today is brought to today");
        final int[] light = new int[2];
        onEdt(() -> {
            calendar().setDate(new Date(Times.addDays(today, 3)));
            light[0] = calendar().getUnselectedStyle().getBgColor();
            light[1] = UIManager.getInstance().getComponentSelectedStyle("PickerMaterialDay")
                    .getBgColor();
        });
        shot("book-date");

        // Cancel: the day that was looked at is not the day that is shown.
        click("PickerMaterialCancel");
        waitForForm("Book");
        assertEqual(today, chosenDay(), "a cancelled calendar leaves the day alone");
        assertTrue(selected("day-" + Times.iso(today)), "today is still the pill that is down");

        // A day past the strip: the strip moves to it and its times are asked for.
        final long later = Times.addDays(today, 25);
        click("day-picker");
        until(() -> calendar() != null, 20000, "the calendar");
        onEdt(() -> calendar().setDate(new Date(later)));
        click("PickerMaterialOk");
        waitForForm("Book");
        until(() -> has("day-" + Times.iso(later)), 20000, "the strip to reach the day chosen");
        assertEqual(later, chosenDay(), "the calendar button says the day chosen");
        assertTrue(selected("day-" + Times.iso(later)), "the day chosen is the pill that is down");
        assertFalse(has("day-" + Times.iso(today)), "the strip left today behind");

        // A pill of the strip is the same choice, and the button follows it.
        click("day-" + Times.iso(Times.addDays(later, 1)));
        until(() -> chosenDay() == Times.addDays(later, 1), 20000, "the button to follow the pill");

        // Dark: the same calendar on the dark surface, the brand still on it.
        onEdt(() -> {
            Prefs.setTheme(Prefs.THEME_DARK);
            Look.apply();
        });
        click("day-picker");
        until(() -> calendar() != null, 20000, "the calendar");
        final int[] dark = new int[2];
        onEdt(() -> {
            dark[0] = calendar().getUnselectedStyle().getBgColor();
            dark[1] = UIManager.getInstance().getComponentSelectedStyle("PickerMaterialDay")
                    .getBgColor();
        });
        shot("book-date-dark");
        assertTrue(luma(light[0]) > 200, "the light calendar is on a light surface");
        assertTrue(luma(dark[0]) < 80, "the dark calendar is on a dark surface");
        assertTrue(light[1] != light[0] && dark[1] != dark[0], "the chosen day stands out in both");
        // Back is a cancel too.
        back();
        waitForForm("Book");
        assertEqual(Times.addDays(later, 1), chosenDay(), "Back leaves the day alone");
        onEdt(() -> {
            Prefs.setTheme(Prefs.THEME_LIGHT);
            Look.apply();
        });
        return true;
    }

    private long chosenDay() {
        final long[] day = new long[1];
        onEdt(() -> day[0] = Times.dayStart(((Picker) findByName("day-picker")).getDate().getTime()));
        return day[0];
    }

    private boolean selected(String pill) {
        Button found = button(pill);
        return found != null && found.isSelected();
    }

    private MaterialDatePicker calendar() {
        return open(MaterialDatePicker.class);
    }

    private static int luma(int rgb) {
        return (((rgb >> 16) & 0xff) * 299 + ((rgb >> 8) & 0xff) * 587 + (rgb & 0xff) * 114) / 1000;
    }
}
