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

import com.codename1.ui.CheckBox;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.slate.api.WeekDto;
import com.codenameone.examples.slate.api.WorkingHoursDto;
import com.codenameone.examples.slate.net.Api;
import com.codenameone.examples.slate.net.Net;
import com.codenameone.examples.slate.ui.Lang;
import com.codenameone.examples.slate.ui.Layouts;
import com.codenameone.examples.slate.ui.Ui;

import java.util.ArrayList;
import java.util.List;

/// The provider's week: which days they work and from when to when.
///
/// One span a day keeps the screen short; a split day (a long lunch, an
/// evening clinic) is two rows on the server, and the owner's editor shows
/// both. Saving replaces the whole week at once, so a half-edited week is
/// never bookable.
public final class HoursForm {
    static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
        "Saturday", "Sunday"};

    private final Form form = Ui.form("Working hours", "Hours");
    private final Container page = Ui.page();
    private final CheckBox[] open = new CheckBox[7];
    private final Picker[] starts = new Picker[7];
    private final Picker[] ends = new Picker[7];
    private String location = "";

    private HoursForm(Form previous) {
        Ui.back(form, previous);
        form.add(BorderLayout.CENTER, page);
        Api.provider().hours(Net.to(this::fill, Ui::fail));
    }

    public static void show(Form previous) {
        new HoursForm(previous).form.show();
    }

    private void fill(WeekDto week) {
        page.removeAll();
        for (int d = 0; d < 7; d++) {
            WorkingHoursDto h = first(week, d + 1);
            if (h != null && location.length() == 0) {
                location = h.locationId();
            }
            open[d] = CheckBox.createToggle(Lang.tr(DAYS[d]));
            open[d].setUIID("SvCheck");
            open[d].setName("open-" + (d + 1));
            open[d].setSelected(h != null);
            starts[d] = time("start-" + (d + 1), h == null ? 540 : h.startMinute());
            ends[d] = time("end-" + (d + 1), h == null ? 1020 : h.endMinute());
            Container row = new Container(new BorderLayout());
            row.setUIID("SvRow");
            row.add(BorderLayout.CENTER, open[d]);
            row.add(BorderLayout.EAST, BoxLayout.encloseX(starts[d], Ui.label("-", "SvText"), ends[d]));
            page.add(row);
        }
        page.add(Ui.primary("Save hours", "save", e -> save()));
        Layouts.broaden(page);
        Ui.relayout(page);
    }

    // tag::slate-hours-form[]
    private void save() {
        List<WorkingHoursDto> hours = new ArrayList<WorkingHoursDto>();
        for (int d = 0; d < 7; d++) {
            if (!open[d].isSelected()) {
                continue;
            }
            int start = starts[d].getTime();
            int end = ends[d].getTime();
            if (end <= start) {
                Ui.say(Lang.tr("{0} ends before it starts", Lang.tr(DAYS[d])));
                return;
            }
            hours.add(new WorkingHoursDto(null, location, d + 1, start, end));
        }
        Api.provider().saveHours(new WeekDto(hours), Net.to(saved -> {
            Ui.say("Hours saved");
            form.getBackCommand().actionPerformed(null);
        }, Ui::fail));
    }
    // end::slate-hours-form[]

    static Picker time(String name, int minute) {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_TIME);
        p.setMinuteStep(15);
        p.setTime(minute);
        p.setName(name);
        p.setUIID("SvPicker");
        return p;
    }

    private static WorkingHoursDto first(WeekDto week, int weekday) {
        for (int iter = 0; iter < week.hours().size(); iter++) {
            if (week.hours().get(iter).weekday() == weekday) {
                return week.hours().get(iter);
            }
        }
        return null;
    }
}
