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
package com.codenameone.developerguide.scheduling;

import com.codename1.calendar.CalendarInfo;
import com.codename1.calendar.LocalCalendarSource;
import com.codename1.printing.PrintResult;
import com.codename1.printing.PrintResultListener;
import com.codename1.scheduling.CalendarScheduleModel;
import com.codename1.scheduling.DefaultScheduleModel;
import com.codename1.scheduling.GanttTask;
import com.codename1.scheduling.LevelingResult;
import com.codename1.scheduling.ProjectModel;
import com.codename1.scheduling.ResourceLeveler;
import com.codename1.scheduling.ScheduleItem;
import com.codename1.scheduling.ScheduleResource;
import com.codename1.scheduling.TaskDependency;
import com.codename1.scheduling.TimeMath;
import com.codename1.scheduling.WorkCalendar;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.scheduler.Gantt;
import com.codename1.ui.scheduler.GanttAdapter;
import com.codename1.ui.scheduler.SchedulePrinter;
import com.codename1.ui.scheduler.Scheduler;
import com.codename1.ui.scheduler.SchedulerAdapter;
import com.codename1.ui.spinner.Picker;
import java.time.ZoneId;
import java.util.Date;

public class SchedulingSnippets {
    public void firstScheduler() {
        // tag::scheduler-first[]
        ZoneId zone = ZoneId.systemDefault();
        long today = TimeMath.epochDay(System.currentTimeMillis(), zone);

        DefaultScheduleModel model = new DefaultScheduleModel();
        model.addItem(new ScheduleItem("standup", "Stand-up",
                TimeMath.at(today, 9 * 60, zone), TimeMath.at(today, 9 * 60 + 30, zone)));
        model.addItem(new ScheduleItem("review", "Design review",
                TimeMath.at(today, 13 * 60, zone), TimeMath.at(today, 14 * 60 + 30, zone))
                .setDetail("Room 2"));
        model.addItem(new ScheduleItem("offsite", "Offsite",
                TimeMath.startOfEpochDay(today + 1, zone), TimeMath.startOfEpochDay(today + 2, zone))
                .setAllDay(true));

        Scheduler scheduler = new Scheduler(model);
        scheduler.setView(Scheduler.View.WEEK);

        Form form = new Form("Schedule", new BorderLayout());
        form.add(BorderLayout.CENTER, scheduler);
        form.show();
        // end::scheduler-first[]
    }

    public void resources() {
        // tag::scheduler-resources[]
        ZoneId zone = ZoneId.systemDefault();
        long today = TimeMath.epochDay(System.currentTimeMillis(), zone);

        DefaultScheduleModel model = new DefaultScheduleModel();
        model.addResource(new ScheduleResource("ana", "Ana").setDetail("Stylist"));
        model.addResource(new ScheduleResource("ben", "Ben").setDetail("Colourist"));

        // The hours Ana works, drawn as a band under her bookings.
        model.addItem(new ScheduleItem("ana-shift", "",
                TimeMath.at(today, 9 * 60, zone), TimeMath.at(today, 17 * 60, zone))
                .setResourceId("ana").setKind(ScheduleItem.Kind.BACKGROUND));
        model.addItem(new ScheduleItem("cut", "Cut and finish",
                TimeMath.at(today, 10 * 60, zone), TimeMath.at(today, 11 * 60, zone))
                .setResourceId("ana"));
        // Time taken by another calendar: hatched, and nobody's to move.
        model.addItem(new ScheduleItem("dentist", "Busy",
                TimeMath.at(today, 12 * 60, zone), TimeMath.at(today, 13 * 60, zone))
                .setResourceId("ben").setKind(ScheduleItem.Kind.BLOCKED));

        Scheduler scheduler = new Scheduler(model);
        scheduler.setViews(Scheduler.View.RESOURCE_DAY, Scheduler.View.RESOURCE_TIMELINE);
        scheduler.setView(Scheduler.View.RESOURCE_DAY);

        Form form = new Form("Team", new BorderLayout());
        form.add(BorderLayout.CENTER, scheduler);
        form.show();
        // end::scheduler-resources[]
    }

    public void events() {
        // tag::scheduler-events[]
        DefaultScheduleModel model = new DefaultScheduleModel();
        // The application shows its own editor for a new item, so the model
        // does not make one by itself.
        model.setCreationEnabled(false);

        Scheduler scheduler = new Scheduler(model);
        scheduler.setSnapMinutes(15);
        scheduler.addSchedulerListener(new SchedulerAdapter() {
            @Override
            public void itemOpened(Scheduler s, ScheduleItem item) {
                Dialog.show(item.getTitle(), s.formatRange(item.getStart(), item.getEnd()), "OK", null);
            }

            @Override
            public void rangeSelected(Scheduler s, long start, long end, String resourceId) {
                model.addItem(new ScheduleItem("new-" + start, "New booking", start, end)
                        .setResourceId(resourceId));
                s.refresh();
            }

            @Override
            public boolean itemChanging(Scheduler s, ScheduleItem item, long start, long end,
                    String resourceId) {
                // Nothing is moved into the past.
                return start >= System.currentTimeMillis();
            }
        });

        Form form = new Form("Bookings", new BorderLayout());
        form.add(BorderLayout.CENTER, scheduler);
        form.show();
        // end::scheduler-events[]
    }

    public void workingHours() {
        // tag::scheduler-work-calendar[]
        ZoneId zone = ZoneId.systemDefault();
        WorkCalendar hours = new WorkCalendar(zone);
        // 08:30 to 12:00 and 13:00 to 17:30, Monday to Friday.
        hours.setWorkingHours(8 * 60 + 30, 12 * 60, 13 * 60, 17 * 60 + 30);
        // Saturday mornings.
        hours.setWorkingDay(6, true);
        hours.setWorkingHours(6, new int[] {9 * 60, 13 * 60});
        hours.addHoliday(TimeMath.epochDay(System.currentTimeMillis(), zone) + 3);

        Scheduler scheduler = new Scheduler();
        scheduler.setWorkCalendar(hours);
        scheduler.setView(Scheduler.View.WORK_WEEK);

        Form form = new Form("Working hours", new BorderLayout());
        form.add(BorderLayout.CENTER, scheduler);
        form.show();
        // end::scheduler-work-calendar[]
    }

    public void deviceCalendar() {
        // tag::scheduler-calendar-source[]
        LocalCalendarSource source = LocalCalendarSource.getInstance();
        CalendarScheduleModel model = new CalendarScheduleModel(source, ZoneId.systemDefault());
        source.listCalendars(CalendarInfo.ContentType.EVENTS, null).ready(page -> {
            for (CalendarInfo calendar : page.getItems()) {
                model.addCalendar(calendar);
            }
        });

        Scheduler scheduler = new Scheduler(model);
        Form form = new Form("My calendars", new BorderLayout());
        form.add(BorderLayout.CENTER, scheduler);
        form.show();
        // end::scheduler-calendar-source[]
    }

    public void firstGantt() {
        // tag::gantt-first[]
        long day = TimeMath.DAY;
        WorkCalendar calendar = new WorkCalendar(ZoneId.systemDefault());
        ProjectModel project = new ProjectModel(calendar,
                TimeMath.startOfDay(System.currentTimeMillis(), calendar.getZone()));
        project.addResource(new ScheduleResource("dev", "Developer"));
        project.addResource(new ScheduleResource("qa", "Tester"));

        // A duration is working time: three days of the calendar's hours.
        long workDay = calendar.getMillisPerDay();
        GanttTask build = new GanttTask("build", "Build");
        GanttTask api = new GanttTask("api", "API", 3 * workDay).assign("dev");
        GanttTask screens = new GanttTask("screens", "Screens", 5 * workDay).assign("dev");
        GanttTask testing = new GanttTask("test", "Testing", 2 * workDay).assign("qa");
        GanttTask release = new GanttTask("release", "Release", 0);

        project.addTask(build);
        project.addTask(build, api);
        project.addTask(build, screens);
        project.addTask(testing);
        project.addTask(release);

        project.link(api, screens);
        project.link(screens, testing);
        // The release follows testing by a day.
        project.link(testing, release, TaskDependency.Type.FINISH_TO_START, day);
        project.update();

        Gantt gantt = new Gantt(project);
        gantt.setShowCriticalPath(true);

        Form form = new Form("Plan", new BorderLayout());
        form.add(BorderLayout.CENTER, gantt);
        form.show();
        // end::gantt-first[]
    }

    public void ganttEvents() {
        // tag::gantt-events[]
        ProjectModel project = new ProjectModel(new WorkCalendar(), System.currentTimeMillis());
        Gantt gantt = new Gantt(project);
        gantt.addGanttListener(new GanttAdapter() {
            @Override
            public boolean taskChanging(Gantt g, GanttTask task, long start, long end) {
                // A task that is finished stays where it is.
                return task.getPercentComplete() < 100;
            }

            @Override
            public boolean linkCreating(Gantt g, GanttTask predecessor, GanttTask successor) {
                return !successor.isSummary();
            }
        });

        // What the plan was, drawn as a ghost bar under what it becomes.
        project.captureBaseline();
        gantt.setShowBaseline(true);
        // end::gantt-events[]
    }

    public void leveling() {
        // tag::gantt-leveling[]
        WorkCalendar calendar = new WorkCalendar();
        ProjectModel project = new ProjectModel(calendar, System.currentTimeMillis());
        project.addResource(new ScheduleResource("dev", "Developer"));
        // Two tasks at once for somebody there is one of.
        project.addTask(new GanttTask("api", "API", 3 * calendar.getMillisPerDay()).assign("dev"));
        project.addTask(new GanttTask("screens", "Screens", 2 * calendar.getMillisPerDay()).assign("dev"));
        project.update();

        if (!ResourceLeveler.findOverallocations(project).isEmpty()) {
            LevelingResult proposal = ResourceLeveler.level(project);
            long slip = proposal.getFinishAfter() - proposal.getFinishBefore();
            if (proposal.isChanged() && Dialog.show("Level resources",
                    proposal.getChangedTasks().size() + " tasks move; the project ends "
                            + slip / TimeMath.DAY + " days later.", "Apply", "Cancel")) {
                proposal.apply();
            }
        }
        // end::gantt-leveling[]
    }

    public void print() {
        // tag::schedule-print[]
        Scheduler scheduler = new Scheduler();
        SchedulePrinter printer = new SchedulePrinter(scheduler);
        printer.setTitle("Week 12");

        // One picture of all of it, to share.
        Image picture = printer.renderImage(2000);

        // Or page by page.
        for (int page = 0; page < printer.getPageCount(); page++) {
            Image sheet = printer.renderPage(page);
        }

        // Or to the platform's print dialog.
        printer.print(new PrintResultListener() {
            @Override
            public void onResult(PrintResult result) {
                if (result.isFailed()) {
                    Dialog.show("Print", result.getError(), "OK", null);
                }
            }
        });
        // end::schedule-print[]
    }

    public void customRenderer(Scheduler scheduler) {
        // tag::scheduler-renderer-use[]
        scheduler.setItemRenderer(new CardItemRenderer());
        // end::scheduler-renderer-use[]
    }

    public Image animationFrame(Scheduler scheduler, Runnable drag) {
        // tag::scheduler-animation-time[]
        // Stop the clock, do what starts the animation, then move the clock
        // to the moment wanted: 80 milliseconds into picking an item up.
        scheduler.setAnimationTime(1000);
        drag.run();
        scheduler.setAnimationTime(1080);

        Image frame = Image.createImage(scheduler.getWidth(), scheduler.getHeight());
        scheduler.paintComponent(frame.getGraphics(), true);

        // Back to the real clock.
        scheduler.setAnimationTime(-1);
        // end::scheduler-animation-time[]
        return frame;
    }

    public void materialPicker() {
        // tag::picker-material[]
        Picker date = new Picker();
        date.setType(com.codename1.ui.Display.PICKER_TYPE_DATE);
        date.setDate(new Date());

        // An application that wants the platform's own dialog on this one
        // field says so; the theme decides for every other.
        Picker time = new Picker();
        time.setType(com.codename1.ui.Display.PICKER_TYPE_TIME);
        time.setUseLightweightPopup(false);
        // end::picker-material[]
    }
}
