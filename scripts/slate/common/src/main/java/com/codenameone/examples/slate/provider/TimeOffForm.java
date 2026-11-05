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
import com.codenameone.examples.slate.net.Net;
import com.codenameone.examples.slate.ui.Layouts;
import com.codenameone.examples.slate.ui.Ui;

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

    private static Picker when(String name, long millis) {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_DATE_AND_TIME);
        p.setDate(new Date(millis));
        p.setName(name);
        p.setUIID("SvPicker");
        return p;
    }
}
