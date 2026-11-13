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

import com.codename1.appkit.Net;
import com.codename1.scheduling.GanttTask;
import com.codename1.scheduling.ProjectModel;
import com.codename1.scheduling.ScheduleResource;
import com.codename1.scheduling.WorkCalendar;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.scheduler.Gantt;
import com.codename1.ui.scheduler.GanttAdapter;
import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.api.BusyDto;
import com.codenameone.examples.slate.api.ScheduleDto;
import com.codenameone.examples.slate.api.ShiftDto;
import com.codenameone.examples.slate.client.Times;
import com.codenameone.examples.slate.live.LiveChannel;
import com.codenameone.examples.slate.net.Api;
import com.codenameone.examples.slate.ui.Lang;
import com.codenameone.examples.slate.ui.Ui;

import java.time.ZoneId;
import java.util.List;

// tag::slate-staffing[]
/// The week's staffing plan as a Gantt chart: a row per provider, a bar for
/// each shift they work, and how much of that shift is booked.
///
/// A shift is a task with fixed dates (`setManuallyScheduled`), so the plan
/// is drawn exactly as the hours were set and nothing reschedules it. Its
/// progress is the share of the shift that's booked, and it's assigned to
/// its provider in those units, so the load under the chart reads as each
/// provider's utilisation, hour by hour, against a capacity of one. A shift
/// lost to time off stays in the plan, struck through in the busy style, so
/// the gap it leaves is plain to see.
///
/// The plan is for reading: hours change in the provider's settings, so every
/// drag is refused, and opening a shift opens that day's board.
public final class StaffingForm extends LiveChannel.Adapter {
    private static final long MINUTE = 60000L;

    private final Form form = Ui.form("Staffing", "Staffing");
    private final Container body = new Container(new BorderLayout());
    private final Label title = Ui.label("", "SvHeading");
    private long week = Times.weekStart(System.currentTimeMillis());

    private StaffingForm(Form previous) {
        Ui.back(form, previous);
        Container heading = new Container(new BorderLayout());
        heading.add(BorderLayout.WEST, Ui.flat(FontImage.MATERIAL_CHEVRON_LEFT, "Earlier",
                "earlier", e -> move(-7)));
        heading.add(BorderLayout.CENTER, title);
        heading.add(BorderLayout.EAST, Ui.flat(FontImage.MATERIAL_CHEVRON_RIGHT, "Later",
                "later", e -> move(7)));
        body.setName("staffing");
        form.add(BorderLayout.NORTH, heading);
        form.add(BorderLayout.CENTER, body);
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            load();
        });
    }

    public static void show(Form previous) {
        new StaffingForm(previous).form.show();
    }

    @Override
    public void scheduleChanged(String username) {
        load();
    }

    private void move(int days) {
        week = Times.addDays(week, days);
        load();
    }

    private void load() {
        title.setText(Lang.tr("Week of") + " " + Times.monthDay(week));
        Api.owner().staffing(Times.iso(week), 7, Net.to(this::show, Ui::fail));
    }

    private void show(List<ScheduleDto> staff) {
        body.removeAll();
        if (staff.isEmpty()) {
            body.add(Ui.empty(FontImage.MATERIAL_PEOPLE, "No providers",
                    "Add providers and their hours to plan the week."));
            Ui.relayout(body);
            return;
        }
        Gantt gantt = new Gantt(plan(staff));
        gantt.setName("staffing-gantt");
        gantt.setToolbarVisible(false);
        gantt.setDragEdits(false);
        gantt.setShowLinks(false);
        gantt.setLoadVisible(true);
        gantt.setColumns(Gantt.Column.START, Gantt.Column.PROGRESS);
        gantt.setCurrentTime(System.currentTimeMillis());
        gantt.addGanttListener(new GanttAdapter() {
            @Override
            public boolean taskChanging(Gantt g, GanttTask task, long start, long end) {
                return false;
            }

            @Override
            public void taskOpened(Gantt g, GanttTask task) {
                if (!task.isSummary()) {
                    BoardForm.show(form, Times.dayStart(task.getStart()));
                }
            }
        });
        body.add(BorderLayout.CENTER, gantt);
        Ui.relayout(body);
        // Fitting needs the chart's width, which it has once laid out.
        CN.callSerially(gantt::zoomToFit);
    }

    /// The plan for a week of schedules: what the chart and the load draw.
    static ProjectModel plan(List<ScheduleDto> staff) {
        // No working hours of its own: each shift's dates are the provider's
        // hours as they were set, nights and weekends included.
        WorkCalendar calendar = WorkCalendar.continuous(ZoneId.systemDefault());
        ProjectModel project = new ProjectModel(calendar, staff.get(0).from());
        for (ScheduleDto s : staff) {
            project.addResource(new ScheduleResource(s.username(), s.displayName())
                    .setCapacity(1f));
            GanttTask person = new GanttTask("p-" + s.username(), s.displayName());
            project.addTask(person);
            int n = 0;
            for (ShiftDto shift : s.shifts()) {
                if (shift.end() <= shift.start()) {
                    continue;
                }
                long off = overlap(shift, s.busy(), "TIME_OFF");
                long open = shift.end() - shift.start() - off;
                GanttTask task = new GanttTask(s.username() + "-" + n++,
                        Times.shortDay(shift.start()) + " " + Times.clock(shift.start()));
                task.setManuallyScheduled(true);
                task.setDates(shift.start(), shift.end());
                if (open <= 0) {
                    task.setName(Times.shortDay(shift.start()) + " " + Lang.tr("Time off"));
                    task.setUIID("SvTimelineBusy");
                } else {
                    float used = Math.min(1f, booked(shift, s.appointments()) / (float) open);
                    task.setPercentComplete(Math.round(used * 100));
                    if (used > 0) {
                        // An assignment is in units greater than zero; an
                        // empty shift puts no load on anyone.
                        task.assign(s.username(), used);
                    }
                }
                project.addTask(person, task);
            }
        }
        project.update();
        return project;
    }

    // The booked time inside a shift, counting a booking's buffers since the
    // provider can't take anyone else then either.
    private static long booked(ShiftDto shift, List<AppointmentDto> appointments) {
        long total = 0;
        for (AppointmentDto a : appointments) {
            if ("CANCELLED".equals(a.state()) || "EXPIRED".equals(a.state())) {
                continue;
            }
            long from = Math.max(shift.start(), a.blockStart() > 0 ? a.blockStart() : a.start());
            long to = Math.min(shift.end(), a.blockEnd() > 0 ? a.blockEnd() : a.end());
            total += Math.max(0, to - from);
        }
        return total / MINUTE * MINUTE;
    }

    private static long overlap(ShiftDto shift, List<BusyDto> busy, String kind) {
        long total = 0;
        for (BusyDto b : busy) {
            if (kind.equals(b.kind())) {
                total += Math.max(0, Math.min(shift.end(), b.end())
                        - Math.max(shift.start(), b.start()));
            }
        }
        return Math.min(total, shift.end() - shift.start());
    }
}
// end::slate-staffing[]
