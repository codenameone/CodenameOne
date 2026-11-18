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
package com.codenameone.examples.slate.provider;

import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.slate.api.TimeOffDto;
import com.codenameone.examples.slate.client.Times;
import com.codenameone.examples.slate.net.Api;
import com.codename1.appkit.Net;
import com.codenameone.examples.slate.ui.Layouts;
import com.codenameone.examples.slate.ui.Ui;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/// Time off: holidays, appointments of their own, a training day. A block
/// here is as busy as a booking; clients already booked inside it are not
/// moved, the provider is shown them instead.
public final class TimeOffForm {
    private final Form form = Ui.form("Time off", "TimeOff");
    private final Container list = Ui.list("time-off");

    private TimeOffForm(Form previous) {
        Ui.back(form, previous);
        Container add = Ui.page();
        long tomorrow = Times.addDays(System.currentTimeMillis(), 1);
        final Picker from = when("off-from", tomorrow + 9 * Times.HOUR);
        final Picker to = when("off-to", tomorrow + 17 * Times.HOUR);
        // An end left behind by a new start follows it, the same length away.
        final long[] length = {to.getDate().getTime() - from.getDate().getTime()};
        from.addActionListener(e -> {
            quarter(from);
            if (to.getDate().getTime() <= from.getDate().getTime()) {
                to.setDate(new Date(from.getDate().getTime() + length[0]));
            }
        });
        to.addActionListener(e -> {
            quarter(to);
            if (to.getDate().getTime() > from.getDate().getTime()) {
                length[0] = to.getDate().getTime() - from.getDate().getTime();
            }
        });
        add.add(Ui.label("From", "SvLabel")).add(from);
        add.add(Ui.label("Until", "SvLabel")).add(to);
        final TextField reason = Ui.field(add, "Reason", "off-reason", "Only you and the owner see it");
        add.add(Ui.primary("Block this time", "off-add", e -> {
            long start = from.getDate().getTime();
            long end = to.getDate().getTime();
            if (end <= start) {
                Ui.say("The end is before the start");
                return;
            }
            Api.provider().addTimeOff(new TimeOffDto(null, start, end, reason.getText()),
                    Net.to(added -> {
                        reason.setText("");
                        load();
                    }, Ui::fail));
        }));
        Layouts.broaden(add);
        form.add(BorderLayout.NORTH, add);
        form.add(BorderLayout.CENTER, list);
        load();
    }

    public static void show(Form previous) {
        new TimeOffForm(previous).form.show();
    }

    private void load() {
        Api.provider().timeOff(Net.to(this::show, Ui.failed(list, this::load)));
    }

    private void show(List<TimeOffDto> blocks) {
        list.removeAll();
        if (blocks.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_BEACH_ACCESS, "No time off",
                    "Blocks added above show here."));
        }
        for (int iter = 0; iter < blocks.size(); iter++) {
            final TimeOffDto b = blocks.get(iter);
            list.add(Ui.row(FontImage.MATERIAL_BEACH_ACCESS,
                    b.reason() == null || b.reason().length() == 0 ? Ui.when(b.start()) : b.reason(),
                    Ui.when(b.start()) + " - " + Ui.when(b.end()),
                    Ui.link("Remove", "off-remove-" + b.id(), e -> Api.provider().removeTimeOff(b.id(),
                            Net.to(gone -> load(), Ui::fail)))));
        }
        Ui.relayout(list);
    }

    /// Brings a time that was typed between two quarters back to the one
    /// before it. The hour and the day are the calendar's, so it is the
    /// minute of the clock on the wall that is rounded.
    private static void quarter(Picker p) {
        Calendar c = Calendar.getInstance();
        c.setTime(p.getDate());
        int minute = c.get(Calendar.MINUTE);
        if (minute % HoursForm.QUARTER != 0 || c.get(Calendar.SECOND) != 0
                || c.get(Calendar.MILLISECOND) != 0) {
            c.set(Calendar.MINUTE, minute / HoursForm.QUARTER * HoursForm.QUARTER);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            p.setDate(c.getTime());
        }
    }

    /// A day and a time of day, in the phone's zone. The theme's
    /// `pickerStyle: material` makes the field open the Material calendar
    /// and clock face behind two tabs.
    private static Picker when(String name, long millis) {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_DATE_AND_TIME);
        p.setMinuteStep(HoursForm.QUARTER);
        // Time off that is already over blocks nothing.
        p.setStartDate(new Date(Times.dayStart(System.currentTimeMillis())));
        p.setDate(new Date(millis));
        p.setName(name);
        p.setUIID("SvPicker");
        return p;
    }
}
