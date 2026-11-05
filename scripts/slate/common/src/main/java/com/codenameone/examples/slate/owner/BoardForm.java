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
package com.codenameone.examples.slate.owner;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.api.BusyDto;
import com.codenameone.examples.slate.api.ScheduleDto;
import com.codenameone.examples.slate.client.Appointments;
import com.codenameone.examples.slate.client.Times;
import com.codenameone.examples.slate.live.LiveChannel;
import com.codenameone.examples.slate.net.Api;
import com.codenameone.examples.slate.net.Net;
import com.codenameone.examples.slate.ui.Lang;
import com.codenameone.examples.slate.ui.Layouts;
import com.codenameone.examples.slate.ui.Ui;

import java.util.List;

/// Every provider's day side by side: one column each, wide enough on a
/// tablet or desktop to see who is free at a glance, scrolling sideways on a
/// phone.
public final class BoardForm extends LiveChannel.Adapter {
    private final Form form = Ui.form("Day board", "Board");
    private final Container columns = new Container(BoxLayout.x());
    private final Label title = Ui.label("", "SvHeading");
    private long day = Times.dayStart(System.currentTimeMillis());

    private BoardForm(Form previous) {
        Ui.back(form, previous);
        Container heading = new Container(new BorderLayout());
        heading.add(BorderLayout.WEST, Ui.flat(FontImage.MATERIAL_CHEVRON_LEFT, "Earlier",
                "earlier", e -> move(-1)));
        heading.add(BorderLayout.CENTER, title);
        heading.add(BorderLayout.EAST, Ui.flat(FontImage.MATERIAL_CHEVRON_RIGHT, "Later",
                "later", e -> move(1)));
        columns.setScrollableX(true);
        columns.setName("board");
        form.add(BorderLayout.NORTH, heading);
        form.add(BorderLayout.CENTER, columns);
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            load();
        });
    }

    public static void show(Form previous) {
        new BoardForm(previous).form.show();
    }

    @Override
    public void scheduleChanged(String username) {
        load();
    }

    private void move(int days) {
        day = Times.addDays(day, days);
        load();
    }

    private void load() {
        title.setText(Ui.weekday(day));
        Api.owner().schedule(Times.iso(day), Net.to(this::show, Ui::fail));
    }

    private void show(List<ScheduleDto> board) {
        columns.removeAll();
        int width = Math.max(Layouts.column() / 2, columns.getWidth() / Math.max(1,
                Math.min(board.size(), 4)));
        for (int iter = 0; iter < board.size(); iter++) {
            ScheduleDto s = board.get(iter);
            Container column = new Container(BoxLayout.y());
            column.setUIID("SvBoardColumn");
            column.setName("column-" + s.username());
            column.setPreferredW(width);
            column.add(Ui.label(s.displayName(), "SvHeading"));
            if (s.shifts().isEmpty()) {
                column.add(Ui.text(Lang.tr("Not working"), "SvSmall"));
            }
            for (int i = 0; i < s.appointments().size(); i++) {
                AppointmentDto a = s.appointments().get(i);
                Container cell = new Container(BoxLayout.y());
                cell.setUIID("SvBoardBooking");
                cell.add(Ui.label(Times.clock(a.start()) + " - " + Times.clock(a.end()), "SvCaption"));
                cell.add(Ui.text(a.serviceName(), "SvText"));
                cell.add(Ui.text(a.clientName() + Ui.DOT + Appointments.state(a), "SvSmall"));
                column.add(cell);
            }
            for (int i = 0; i < s.busy().size(); i++) {
                BusyDto b = s.busy().get(i);
                column.add(Ui.label(Lang.tr("Busy {0} - {1}", Times.clock(b.start()),
                        Times.clock(b.end())), "SvBoardBusy"));
            }
            columns.add(column);
        }
        if (board.isEmpty()) {
            columns.add(Ui.empty(FontImage.MATERIAL_PEOPLE, "No providers",
                    "Add providers and their hours to fill the board."));
        }
        Ui.relayout(columns);
    }
}
