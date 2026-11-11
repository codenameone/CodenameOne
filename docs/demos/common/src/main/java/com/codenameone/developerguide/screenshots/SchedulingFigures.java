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
package com.codenameone.developerguide.screenshots;

import com.codename1.scheduling.DefaultScheduleModel;
import com.codename1.scheduling.GanttTask;
import com.codename1.scheduling.ProjectModel;
import com.codename1.scheduling.ScheduleItem;
import com.codename1.scheduling.ScheduleResource;
import com.codename1.scheduling.TaskDependency;
import com.codename1.scheduling.TimeMath;
import com.codename1.scheduling.WorkCalendar;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.Painter;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.scheduler.Gantt;
import com.codename1.ui.scheduler.Scheduler;
import com.codename1.ui.scheduler.SchedulerAdapter;
import com.codename1.ui.spinner.MaterialDatePicker;
import com.codename1.ui.spinner.MaterialTimePicker;
import com.codename1.ui.util.Resources;
import com.codenameone.developerguide.scheduling.CardItemRenderer;

import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/// The figures of the scheduling chapter and of the Material pickers: every
/// view of a `Scheduler`, a `Gantt` in its variations, the pickers, what CSS
/// does to each, and the animations as rows of stopped frames.
///
/// Everything is on fixed dates in a fixed zone with a fixed "now", and the
/// animated ones stop the components' clock with `setAnimationTime`, so a
/// frame from the middle of an animation is the same picture every run.
public final class SchedulingFigures {
    /// Monday 2026-03-09, as a count of days since 1970-01-01.
    private static final long MONDAY = 20521L;

    /// 2024-06-15T12:00:00Z, the day the date pickers open on. Not the month
    /// the figures are generated in, so no day is ever ringed as today.
    private static final long PICKER_DAY = 1718452800000L;

    /// When the stopped clock starts, and when everything that was started
    /// before it has long finished.
    private static final long T0 = 1000;
    private static final long T1 = 9000;

    private SchedulingFigures() {
    }

    /// What builds a figure's form.
    private interface Scene {
        Form build();
    }

    /// What a figure does to its form once it is on screen.
    private interface Act {
        void run(Form form);
    }

    private static final class Fig implements GuideFigure {
        private final String id;
        private final Scene scene;
        private final Act act;
        private final boolean fills;
        private final boolean themed;

        Fig(String id, Scene scene, Act act, boolean fills, boolean themed) {
            this.id = id;
            this.scene = scene;
            this.act = act;
            this.fills = fills;
            this.themed = themed;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Form build() {
            return scene.build();
        }

        @Override
        public void afterShow(Form form) {
            if (act != null) {
                act.run(form);
            }
        }

        @Override
        public boolean fillsViewport() {
            return fills;
        }

        @Override
        public boolean changesTheme() {
            return themed;
        }
    }

    private static FigureVariant portrait(String name, Scene scene) {
        return new FigureVariant(new Fig(name, scene, null, true, false), FigureDevice.ANDROID, false, name + ".png");
    }

    private static FigureVariant landscape(String name, Scene scene) {
        return new FigureVariant(new Fig(name, scene, null, true, false), FigureDevice.ANDROID, false, true,
                name + ".png");
    }

    private static FigureVariant staged(String name, boolean wide, Scene scene, Act act) {
        return new FigureVariant(new Fig(name, scene, act, true, false), FigureDevice.ANDROID, false, wide,
                name + ".png");
    }

    private static FigureVariant cropped(String name, Scene scene, Act act) {
        return new FigureVariant(new Fig(name, scene, act, false, false), FigureDevice.ANDROID, false, name + ".png");
    }

    public static FigureVariant[] variants() {
        List<FigureVariant> v = new ArrayList<FigureVariant>();

        // ---- every view of a Scheduler
        v.add(portrait("scheduler-day", () -> schedulerForm(Scheduler.View.DAY)));
        v.add(landscape("scheduler-week", () -> schedulerForm(Scheduler.View.WEEK)));
        v.add(landscape("scheduler-work-week", () -> schedulerForm(Scheduler.View.WORK_WEEK)));
        v.add(portrait("scheduler-month", () -> schedulerForm(Scheduler.View.MONTH)));
        v.add(portrait("scheduler-agenda", () -> schedulerForm(Scheduler.View.AGENDA)));
        v.add(landscape("scheduler-resource-timeline", () -> schedulerForm(Scheduler.View.RESOURCE_TIMELINE)));
        v.add(portrait("scheduler-resource-day", () -> schedulerForm(Scheduler.View.RESOURCE_DAY)));
        // ---- the same schedule in the other appearance and on the other platform
        GuideFigure day = new Fig("scheduler-day-appearance", () -> schedulerForm(Scheduler.View.DAY), null, true,
                false);
        v.add(new FigureVariant(day, FigureDevice.ANDROID, true, "scheduler-day-dark.png"));
        v.add(new FigureVariant(day, FigureDevice.IOS, false, "scheduler-day-ios.png"));
        GuideFigure month = new Fig("scheduler-month-appearance", () -> schedulerForm(Scheduler.View.MONTH), null,
                true, false);
        v.add(new FigureVariant(month, FigureDevice.ANDROID, true, "scheduler-month-dark.png"));
        // ---- restyled
        v.add(new FigureVariant(new Fig("scheduler-css", SchedulingFigures::styledScheduler, null, true, true),
                FigureDevice.ANDROID, false, "scheduler-css.png"));
        v.add(new FigureVariant(new Fig("scheduler-item-colors", SchedulingFigures::coloredScheduler, null, true,
                false), FigureDevice.ANDROID, false, true, "scheduler-item-colors.png"));
        v.add(portrait("scheduler-renderer", SchedulingFigures::renderedScheduler));
        // ---- in motion
        DayStages drag = new DayStages(5, 9 * 60);
        v.add(staged("scheduler-drag-stages", true, drag::build, drag::drag));
        DayStages resize = new DayStages(4, 13 * 60);
        v.add(staged("scheduler-resize-stages", true, resize::build, resize::resize));
        DayStages range = new DayStages(3, 13 * 60);
        v.add(staged("scheduler-range-stages", true, range::build, range::range));
        MonthStages monthDrag = new MonthStages();
        v.add(staged("scheduler-month-drag-stages", true, monthDrag::build, monthDrag::drag));

        // ---- a Gantt
        v.add(landscape("gantt-chart", () -> ganttForm(gantt(project(), false, false))));
        v.add(landscape("gantt-critical-path", () -> ganttForm(gantt(project(), true, false))));
        v.add(landscape("gantt-baseline", () -> ganttForm(gantt(slipped(), true, true))));
        v.add(landscape("gantt-collapsed", SchedulingFigures::collapsedGantt));
        v.add(staged("gantt-load", false, () -> loadForm(false), SchedulingFigures::fitChart));
        v.add(staged("gantt-leveled", false, () -> loadForm(true), SchedulingFigures::fitChart));
        v.add(new FigureVariant(new Fig("gantt-dark", () -> ganttForm(gantt(project(), true, false)), null, true,
                false), FigureDevice.ANDROID, true, true, "gantt-dark.png"));
        v.add(new FigureVariant(new Fig("gantt-ios", () -> ganttForm(gantt(project(), true, false)), null, true,
                false), FigureDevice.IOS, false, true, "gantt-ios.png"));
        v.add(new FigureVariant(new Fig("gantt-css", SchedulingFigures::styledGantt, null, true, true),
                FigureDevice.ANDROID, false, true, "gantt-css.png"));
        GanttStages ganttDrag = new GanttStages(4);
        v.add(staged("gantt-drag-stages", false, ganttDrag::build, ganttDrag::drag));
        GanttStages ganttLink = new GanttStages(3);
        v.add(staged("gantt-link-stages", false, ganttLink::build, ganttLink::link));

        // ---- the Material pickers
        v.add(cropped("picker-material-date", () -> pickerForm("Date", datePicker()), null));
        v.add(cropped("picker-material-date-years", () -> pickerForm("Date", datePicker()),
                f -> press(f, "June 2024")));
        v.add(cropped("picker-material-date-input", () -> {
            MaterialDatePicker p = datePicker();
            p.setInputMode(true);
            return pickerForm("Date", p);
        }, null));
        v.add(cropped("picker-material-date-range", () -> {
            MaterialDatePicker p = datePicker();
            p.setDateRange(new Date(PICKER_DAY - 5 * TimeMath.DAY), new Date(PICKER_DAY + 9 * TimeMath.DAY));
            return pickerForm("Date", p);
        }, null));
        v.add(cropped("picker-material-time", () -> pickerForm("Time", timePicker(true, 9, 25)), null));
        v.add(cropped("picker-material-time-minutes", () -> pickerForm("Time", timePicker(true, 9, 25)),
                f -> press(f, "25")));
        v.add(cropped("picker-material-time-24h", () -> pickerForm("Time", timePicker(false, 17, 25)), null));
        v.add(cropped("picker-material-time-input", () -> {
            MaterialTimePicker p = timePicker(true, 9, 25);
            p.setInputMode(true);
            return pickerForm("Time", p);
        }, null));
        GuideFigure dateLook = new Fig("picker-material-date-look", () -> pickerForm("Date", datePicker()), null,
                false, false);
        v.add(new FigureVariant(dateLook, FigureDevice.ANDROID, true, "picker-material-date-dark.png"));
        v.add(new FigureVariant(dateLook, FigureDevice.IOS, false, "picker-material-date-ios.png"));
        GuideFigure timeLook = new Fig("picker-material-time-look", () -> pickerForm("Time", timePicker(true, 9, 25)),
                null, false, false);
        v.add(new FigureVariant(timeLook, FigureDevice.ANDROID, true, "picker-material-time-dark.png"));
        v.add(new FigureVariant(new Fig("picker-material-date-css", () -> {
            overlay("PickerMaterial");
            return pickerForm("Date", datePicker());
        }, null, false, true), FigureDevice.ANDROID, false, "picker-material-date-css.png"));
        v.add(new FigureVariant(new Fig("picker-material-time-css", () -> {
            overlay("PickerMaterial");
            return pickerForm("Time", timePicker(true, 9, 25));
        }, null, false, true), FigureDevice.ANDROID, false, "picker-material-time-css.png"));
        PickerStages hand = new PickerStages();
        v.add(new FigureVariant(new Fig("picker-material-hand-stages", hand::buildTime, hand::sweep, false, false),
                FigureDevice.ANDROID, false, true, "picker-material-hand-stages.png"));
        PickerStages slide = new PickerStages();
        v.add(new FigureVariant(new Fig("picker-material-month-stages", slide::buildDate, slide::slide, false,
                false), FigureDevice.ANDROID, false, true, "picker-material-month-stages.png"));
        return v.toArray(new FigureVariant[0]);
    }

    // ------------------------------------------------------------ the data

    private static ZoneId zone() {
        return ZoneId.of("UTC");
    }

    private static long at(int day, int hour, int minute) {
        return TimeMath.at(MONDAY + day, hour * 60 + minute, zone());
    }

    private static long allDay(int day) {
        return TimeMath.startOfEpochDay(MONDAY + day, zone());
    }

    /// Three people's month: a full Monday with two meetings at once, a band
    /// of working time, a blocked hour, days off and something in every week.
    private static DefaultScheduleModel team() {
        DefaultScheduleModel m = new DefaultScheduleModel();
        m.addResource(new ScheduleResource("ana", "Ana").setDetail("Design"));
        m.addResource(new ScheduleResource("ben", "Ben").setDetail("Build"));
        m.addResource(new ScheduleResource("cy", "Cy").setDetail("Test"));
        m.addItem(new ScheduleItem("shift", "", at(0, 8, 0), at(0, 16, 0))
                .setResourceId("ana").setKind(ScheduleItem.Kind.BACKGROUND));
        m.addItem(new ScheduleItem("standup", "Stand-up", at(0, 9, 0), at(0, 9, 30)).setResourceId("ana"));
        m.addItem(new ScheduleItem("review", "Design review", at(0, 10, 0), at(0, 11, 30))
                .setResourceId("ana").setDetail("Room 2"));
        m.addItem(new ScheduleItem("pairing", "Pairing", at(0, 10, 30), at(0, 12, 0)).setResourceId("ben"));
        m.addItem(new ScheduleItem("busy", "Busy", at(0, 13, 0), at(0, 14, 0))
                .setResourceId("cy").setKind(ScheduleItem.Kind.BLOCKED));
        m.addItem(new ScheduleItem("demo", "Demo", at(0, 14, 30), at(0, 15, 30)).setResourceId("ben"));
        m.addItem(new ScheduleItem("planning", "Planning", at(1, 9, 0), at(1, 10, 30)).setResourceId("ana"));
        m.addItem(new ScheduleItem("release", "Release", at(1, 11, 0), at(1, 12, 0)).setResourceId("ben"));
        m.addItem(new ScheduleItem("workshop", "Workshop", at(2, 9, 0), at(2, 12, 30)).setResourceId("cy"));
        m.addItem(new ScheduleItem("sync", "Sync", at(2, 14, 0), at(2, 14, 30)).setResourceId("ana"));
        m.addItem(new ScheduleItem("interview", "Interview", at(3, 10, 0), at(3, 11, 0)).setResourceId("ben"));
        m.addItem(new ScheduleItem("retro", "Retrospective", at(3, 14, 0), at(3, 15, 0)).setResourceId("ana"));
        m.addItem(new ScheduleItem("offsite", "Offsite", allDay(4), allDay(5)).setAllDay(true).setResourceId("ben"));
        m.addItem(new ScheduleItem("kickoff", "Kick-off", at(7, 10, 0), at(7, 11, 0)).setResourceId("ana"));
        m.addItem(new ScheduleItem("audit", "Audit", at(9, 13, 0), at(9, 15, 0)).setResourceId("cy"));
        m.addItem(new ScheduleItem("conference", "Conference", allDay(10), allDay(12)).setAllDay(true)
                .setResourceId("cy"));
        m.addItem(new ScheduleItem("launch", "Launch", at(15, 9, 0), at(15, 10, 0)).setResourceId("ben"));
        m.addItem(new ScheduleItem("budget", "Budget", at(-5, 11, 0), at(-5, 12, 0)).setResourceId("ana"));
        m.addItem(new ScheduleItem("training", "Training", at(-4, 9, 0), at(-4, 12, 0)).setResourceId("cy"));
        return m;
    }

    private static Scheduler scheduler(DefaultScheduleModel model, Scheduler.View view) {
        Scheduler s = new Scheduler(model);
        s.setZone(zone());
        s.setWorkCalendar(new WorkCalendar(zone()));
        s.set24Hours(true);
        s.setCurrentTime(at(0, 10, 10));
        s.setInitialMinute(8 * 60);
        s.setView(view);
        s.setDate(at(0, 0, 0));
        return s;
    }

    private static Form schedulerForm(Scheduler.View view) {
        return fill("Schedule", scheduler(team(), view));
    }

    private static Form fill(String title, Component c) {
        Form f = new Form(title, new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        return f;
    }

    private static Form styledScheduler() {
        overlay("Scheduler", "TentativeBooking");
        DefaultScheduleModel m = team();
        m.getItem("pairing").setUIID("TentativeBooking");
        m.getItem("demo").setUIID("TentativeBooking");
        return fill("Schedule", scheduler(m, Scheduler.View.DAY));
    }

    /// A colour for each person, and one item with a colour of its own.
    private static Form coloredScheduler() {
        DefaultScheduleModel m = team();
        m.getResources().get(0).setColor(0x7c3aed);
        m.getResources().get(1).setColor(0x0369a1);
        m.getResources().get(2).setColor(0xb45309);
        m.getItem("release").setColor(0x15803d);
        return fill("Schedule", scheduler(m, Scheduler.View.WEEK));
    }

    private static Form renderedScheduler() {
        DefaultScheduleModel m = team();
        m.getResources().get(1).setColor(0x0369a1);
        Scheduler s = scheduler(m, Scheduler.View.DAY);
        s.setItemRenderer(new CardItemRenderer());
        return fill("Schedule", s);
    }

    /// A small project: a summary over three linked tasks, a task the same
    /// person is double booked on, testing and a milestone at the end.
    private static ProjectModel project() {
        WorkCalendar calendar = new WorkCalendar(zone());
        ProjectModel p = new ProjectModel(calendar, at(0, 0, 0));
        p.addResource(new ScheduleResource("dev", "Developer"));
        p.addResource(new ScheduleResource("qa", "Tester"));
        long day = calendar.getMillisPerDay();
        GanttTask build = new GanttTask("build", "Build");
        GanttTask design = new GanttTask("design", "Design", 2 * day).assign("dev").setPercentComplete(100);
        GanttTask api = new GanttTask("api", "API", 3 * day).assign("dev").setPercentComplete(60);
        GanttTask screens = new GanttTask("screens", "Screens", 4 * day).assign("dev");
        GanttTask docs = new GanttTask("docs", "Docs", 2 * day).assign("dev");
        GanttTask testing = new GanttTask("test", "Testing", 2 * day).assign("qa");
        GanttTask release = new GanttTask("release", "Release", 0);
        p.addTask(build);
        p.addTask(build, design);
        p.addTask(build, api);
        p.addTask(build, screens);
        p.addTask(docs);
        p.addTask(testing);
        p.addTask(release);
        p.link(design, api);
        p.link(api, screens);
        p.link(screens, testing);
        p.link(testing, release, TaskDependency.Type.FINISH_TO_START, 0);
        p.update();
        return p;
    }

    /// The project with a plan kept and the API then taking two days more.
    private static ProjectModel slipped() {
        ProjectModel p = project();
        p.captureBaseline();
        GanttTask api = p.getTask("api");
        api.setDuration(api.getDuration() + 2 * p.getCalendar().getMillisPerDay());
        p.update();
        return p;
    }

    private static Gantt gantt(ProjectModel p, boolean critical, boolean baseline) {
        Gantt g = new Gantt(p);
        g.set24Hours(true);
        g.setCurrentTime(at(2, 10, 0));
        g.setShowCriticalPath(critical);
        g.setShowBaseline(baseline);
        return g;
    }

    private static Form ganttForm(Gantt g) {
        return fill("Plan", g);
    }

    private static Form collapsedGantt() {
        ProjectModel p = project();
        Gantt g = gantt(p, false, false);
        g.setCollapsed(p.getTask("build"), true);
        return ganttForm(g);
    }

    private static Form loadForm(boolean leveled) {
        ProjectModel p = project();
        Gantt g = gantt(p, false, false);
        g.setLoadVisible(true);
        g.setColumns();
        if (leveled) {
            g.level().apply();
        }
        return ganttForm(g);
    }

    /// Zooms the chart of a form until the whole project is in view, which
    /// can only be asked once the chart has a width.
    private static void fitChart(Form form) {
        Gantt g = chartIn(form);
        if (g != null) {
            g.zoomToFit();
        }
    }

    private static Gantt chartIn(Container root) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (c instanceof Gantt) {
                return (Gantt) c;
            }
            if (c instanceof Container) {
                Gantt g = chartIn((Container) c);
                if (g != null) {
                    return g;
                }
            }
        }
        return null;
    }

    private static Form styledGantt() {
        overlay("Gantt");
        return ganttForm(gantt(slipped(), true, false));
    }

    private static MaterialDatePicker datePicker() {
        MaterialDatePicker p = new MaterialDatePicker();
        p.setDate(new Date(PICKER_DAY));
        return p;
    }

    private static MaterialTimePicker timePicker(boolean meridiem, int hour, int minute) {
        MaterialTimePicker p = new MaterialTimePicker();
        // Said outright, so the figure does not follow the host's setting.
        p.setShowMeridiem(meridiem);
        p.setTime(hour, minute);
        return p;
    }

    private static Form pickerForm(String title, Component picker) {
        Form f = new Form(title, new BorderLayout());
        f.add(BorderLayout.NORTH, picker);
        return f;
    }

    // ------------------------------------------------------------- styling

    /// Lays the rules the guide's own stylesheet has for some UIIDs over the
    /// installed theme, so a styled figure is drawn by the CSS the chapter
    /// prints and not by a copy of its colours.
    private static void overlay(String... prefixes) {
        try {
            Resources res = Resources.open("/guide-snippets-theme.res");
            Hashtable<?, ?> theme = res.getTheme(res.getThemeResourceNames()[0]);
            Hashtable<String, Object> rules = new Hashtable<String, Object>();
            for (Object k : theme.keySet()) {
                String key = (String) k;
                for (String prefix : prefixes) {
                    if (key.startsWith(prefix)) {
                        rules.put(key, theme.get(k));
                    }
                }
            }
            if (rules.isEmpty()) {
                throw new IllegalStateException("the guide's stylesheet has no rule for " + prefixes[0]);
            }
            UIManager.getInstance().addThemeProps(rules);
        } catch (IOException err) {
            throw new IllegalStateException("the guide's stylesheet did not load", err);
        }
    }

    // --------------------------------------------------------- the pointer

    /// Where on screen something is, found by asking the components the way
    /// a mouse would: a hover over every few pixels, and the tooltip or the
    /// cursor the component answers with.
    private static final class Hit {
        private Component c;
        private int x0 = Integer.MAX_VALUE;
        private int y0 = Integer.MAX_VALUE;
        private int x1 = -1;
        private int y1 = -1;

        boolean found() {
            return c != null;
        }

        int cx() {
            return (x0 + x1) / 2;
        }

        int cy() {
            return (y0 + y1) / 2;
        }

        int width() {
            return x1 - x0;
        }

        int height() {
            return y1 - y0;
        }
    }

    private interface Match {
        boolean is(Component c);
    }

    private static Hit find(Container root, Rectangle area, Match match) {
        Hit hit = new Hit();
        List<Component> touched = new ArrayList<Component>();
        int[] px = new int[1];
        int[] py = new int[1];
        for (int y = area.getY(); y < area.getY() + area.getHeight(); y += 2) {
            for (int x = area.getX(); x < area.getX() + area.getWidth(); x += 2) {
                Component c = root.getComponentAt(x, y);
                if (c == null || c instanceof Container) {
                    continue;
                }
                px[0] = x;
                py[0] = y;
                c.pointerHover(px, py);
                if (!touched.contains(c)) {
                    touched.add(c);
                }
                if (match.is(c)) {
                    hit.c = c;
                    hit.x0 = Math.min(hit.x0, x);
                    hit.y0 = Math.min(hit.y0, y);
                    hit.x1 = Math.max(hit.x1, x);
                    hit.y1 = Math.max(hit.y1, y);
                }
            }
        }
        for (Component c : touched) {
            c.setHovered(false);
        }
        return hit;
    }

    private static Rectangle bounds(Component c) {
        return new Rectangle(c.getAbsoluteX(), c.getAbsoluteY(), c.getWidth(), c.getHeight());
    }

    /// The part of a component whose tooltip starts with a text: an item of
    /// a schedule or the bar of a task, by its name.
    private static Hit named(Container root, String name) {
        Hit hit = find(root, bounds(root), c -> c.getTooltip() != null && c.getTooltip().startsWith(name));
        if (!hit.found()) {
            throw new IllegalStateException("nothing on screen answers to " + name);
        }
        return hit;
    }

    /// The part of an area over which a component asks for a cursor.
    private static Hit cursor(Container root, Rectangle area, int cursor) {
        Hit hit = find(root, area, c -> c.getCursor() == cursor);
        if (!hit.found()) {
            throw new IllegalStateException("nothing on screen asks for cursor " + cursor);
        }
        return hit;
    }

    private static void hover(Hit hit, int x, int y) {
        hit.c.pointerHover(new int[]{x}, new int[]{y});
    }

    private static void pull(Hit hit, int fromX, int fromY, int toX, int toY, boolean release) {
        hit.c.pointerPressed(fromX, fromY);
        for (int i = 1; i <= 6; i++) {
            hit.c.pointerDragged(fromX + (toX - fromX) * i / 6, fromY + (toY - fromY) * i / 6);
        }
        if (release) {
            hit.c.pointerReleased(toX, toY);
        }
    }

    /// Presses the button of a form that shows a text.
    private static void press(Form form, String text) {
        Button b = button(form, text);
        if (b == null) {
            throw new IllegalStateException("no button shows " + text);
        }
        b.pressed();
        b.released();
    }

    private static Button button(Container root, String text) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (c instanceof Button && text.equals(((Button) c).getText())) {
                return (Button) c;
            }
            if (c instanceof Container) {
                Button b = button((Container) c, text);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    /// The buttons under a container that have a UIID, in the order they
    /// were added.
    private static void buttons(Container root, String uiid, List<Button> into) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (c instanceof Button && uiid.equals(c.getUIID())) {
                into.add((Button) c);
            } else if (c instanceof Container) {
                buttons((Container) c, uiid, into);
            }
        }
    }

    /// Draws a mouse pointer over a form at each of some points, since a
    /// screenshot has none and a hover means nothing without one.
    private static final class Pointers implements Painter {
        private final List<int[]> points = new ArrayList<int[]>();

        void add(int x, int y) {
            points.add(new int[]{x, y});
        }

        @Override
        public void paint(Graphics g, Rectangle rect) {
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            for (int[] p : points) {
                int x = p[0];
                int y = p[1];
                int[] xs = {x, x, x + 4, x + 7, x + 9, x + 6, x + 11};
                int[] ys = {y, y + 16, y + 12, y + 18, y + 17, y + 11, y + 11};
                g.setColor(0x111111);
                g.fillPolygon(xs, ys, 7);
                g.setColor(0xffffff);
                g.drawPolygon(xs, ys, 7);
            }
            g.setAntiAliased(aa);
        }
    }

    // ---------------------------------------------------- frames of motion

    private static Label caption(String text) {
        Label l = new Label(text);
        Font f = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(Display.getInstance().convertToPixels(2.2f), Font.STYLE_PLAIN);
        l.getAllStyles().setFont(f);
        l.getAllStyles().setPadding(1, 1, 1, 1);
        l.getAllStyles().setMargin(0, 0, 0, 0);
        return l;
    }

    /// A form of frames side by side or one under another, each under a
    /// line saying what moment it is.
    private static Form frames(String title, boolean across, Component[] frames, String[] captions) {
        Form f = new Form(title, across ? new GridLayout(1, frames.length) : new GridLayout(frames.length, 1));
        for (int i = 0; i < frames.length; i++) {
            Container cell = new Container(new BorderLayout());
            cell.getAllStyles().setPadding(1, 1, 2, 2);
            cell.add(BorderLayout.NORTH, caption((i + 1) + ". " + captions[i]));
            cell.add(BorderLayout.CENTER, frames[i]);
            f.add(cell);
        }
        return f;
    }

    /// One day of the schedule, several times over, each stopped at a
    /// moment of the same gesture.
    private static final class DayStages {
        private final Scheduler[] views;
        private final Pointers pointers = new Pointers();

        /// The hour the frames open on, in minutes of the day.
        private final int from;

        DayStages(int count, int from) {
            views = new Scheduler[count];
            this.from = from;
        }

        private void make(boolean refuse) {
            for (int i = 0; i < views.length; i++) {
                Scheduler s = scheduler(team(), Scheduler.View.DAY);
                s.setToolbarVisible(false);
                s.setDragEdits(true);
                s.setInitialMinute(from);
                s.setAnimationTime(T0);
                views[i] = s;
            }
        }

        Form build() {
            make(false);
            String[] captions;
            if (views.length == 5) {
                captions = new String[]{"Pointer over an item", "Picked up", "Carried", "Dropped", "At rest"};
            } else if (views.length == 4) {
                captions = new String[]{"Pointer on the edge", "Pulled", "Let go", "At rest"};
            } else {
                captions = new String[]{"Pointer over free time", "Dragged across it", "Selected"};
            }
            Form f = frames("Schedule", true, views, captions);
            f.setGlassPane(pointers);
            return f;
        }

        /// An item is picked up, carried three quarters of an hour down the
        /// day and dropped beside the meeting that is already there.
        void drag(Form form) {
            for (int i = 0; i < views.length; i++) {
                Scheduler s = views[i];
                Hit item = named(s, "Stand-up");
                s.setAnimationTime(T1);
                int x = item.cx();
                int y = item.cy();
                // The item is half an hour tall: down by one and a half of it.
                int toY = y + item.height() * 3 / 2;
                if (i == 0) {
                    hover(item, x, y);
                    s.setAnimationTime(T1 + 140);
                    pointers.add(x, y);
                } else if (i == 1) {
                    pull(item, x, y, x, y + 6, false);
                    s.setAnimationTime(T1 + 70);
                    pointers.add(x, y + 6);
                } else if (i == 2) {
                    pull(item, x, y, x, toY, false);
                    s.setAnimationTime(T1 + 400);
                    pointers.add(x, toY);
                } else if (i == 3) {
                    pull(item, x, y, x, toY, true);
                    s.setAnimationTime(T1 + 80);
                    pointers.add(x, toY);
                } else {
                    pull(item, x, y, x, toY, true);
                    s.setAnimationTime(T1 + 2000);
                }
            }
        }

        /// The end of an item is pulled later.
        void resize(Form form) {
            for (int i = 0; i < views.length; i++) {
                Scheduler s = views[i];
                Hit item = named(s, "Demo");
                Rectangle low = new Rectangle(item.x0, item.cy(), item.width(), item.height());
                Hit edge = cursor(s, low, Component.S_RESIZE_CURSOR);
                s.setAnimationTime(T1);
                int x = edge.cx();
                int y = edge.cy();
                int toY = y + item.height();
                if (i == 0) {
                    hover(edge, x, y);
                    s.setAnimationTime(T1 + 140);
                    pointers.add(x, y);
                } else if (i == 1) {
                    pull(edge, x, y, x, toY, false);
                    s.setAnimationTime(T1 + 400);
                    pointers.add(x, toY);
                } else if (i == 2) {
                    pull(edge, x, y, x, toY, true);
                    s.setAnimationTime(T1 + 80);
                    pointers.add(x, toY);
                } else {
                    pull(edge, x, y, x, toY, true);
                    s.setAnimationTime(T1 + 2000);
                }
            }
        }

        /// A drag over time nothing is in selects it.
        void range(Form form) {
            for (int i = 0; i < views.length; i++) {
                Scheduler s = views[i];
                Hit item = named(s, "Demo");
                s.setAnimationTime(T1);
                int x = item.cx();
                int y = item.y1 + item.height() * 3 / 4;
                int toY = y + item.height() * 3 / 2;
                if (i == 0) {
                    hover(item, x, y);
                    s.setAnimationTime(T1 + 140);
                    pointers.add(x, y);
                } else if (i == 1) {
                    pull(item, x, y, x, toY, false);
                    s.setAnimationTime(T1 + 400);
                    pointers.add(x, toY);
                } else {
                    pull(item, x, y, x, toY, true);
                    s.setAnimationTime(T1 + 2000);
                }
            }
        }
    }

    /// A month, three times over: an item carried to another day.
    private static final class MonthStages {
        private final Scheduler[] views = new Scheduler[3];
        private final Pointers pointers = new Pointers();

        Form build() {
            for (int i = 0; i < views.length; i++) {
                Scheduler s = scheduler(team(), Scheduler.View.MONTH);
                s.setToolbarVisible(false);
                s.setDragEdits(true);
                s.setAnimationTime(T0);
                views[i] = s;
            }
            Form f = frames("Schedule", true, views, new String[]{"Pointer over an item", "Carried to another day",
                "Dropped"});
            f.setGlassPane(pointers);
            return f;
        }

        void drag(Form form) {
            for (int i = 0; i < views.length; i++) {
                Scheduler s = views[i];
                Hit item = named(s, "Kick-off");
                s.setAnimationTime(T1);
                int x = item.cx();
                int y = item.cy();
                // The next day: the columns are a seventh of the view each.
                int toX = x + item.c.getWidth() / 7;
                if (i == 0) {
                    hover(item, x, y);
                    s.setAnimationTime(T1 + 140);
                    pointers.add(x, y);
                } else if (i == 1) {
                    pull(item, x, y, toX, y + 4, false);
                    s.setAnimationTime(T1 + 400);
                    pointers.add(toX, y + 4);
                } else {
                    pull(item, x, y, toX, y + 4, true);
                    s.setAnimationTime(T1 + 2000);
                }
            }
        }
    }

    /// The chart of the project, several times over.
    private static final class GanttStages {
        private final Gantt[] views;
        private final Pointers pointers = new Pointers();

        GanttStages(int count) {
            views = new Gantt[count];
        }

        Form build() {
            for (int i = 0; i < views.length; i++) {
                Gantt g = gantt(project(), false, false);
                g.setToolbarVisible(false);
                // The names and no dates: the dates are what the bars show.
                g.setColumns();
                g.setDragEdits(true);
                g.setAnimationTime(T0);
                views[i] = g;
            }
            String[] captions = views.length == 4
                    ? new String[]{"Pointer over a bar", "Carried two days later", "Let go: what follows it moves",
                        "At rest"}
                    : new String[]{"Pointer on the link handle of the selected bar", "Pulled to another bar",
                        "Linked: the other task now follows"};
            Form f = frames("Plan", false, views, captions);
            f.setGlassPane(pointers);
            return f;
        }

        /// A task is moved later, and the tasks that depend on it follow.
        void drag(Form form) {
            for (int i = 0; i < views.length; i++) {
                Gantt g = views[i];
                g.zoomToFit();
                Hit bar = named(g, "API");
                Hit two = named(g, "Design");
                g.setAnimationTime(T1);
                int x = bar.cx();
                int y = bar.cy();
                // Design is two days long: as far as that.
                int toX = x + two.width();
                if (i == 0) {
                    hover(bar, x, y);
                    g.setAnimationTime(T1 + 140);
                    pointers.add(x, y);
                } else if (i == 1) {
                    pull(bar, x, y, toX, y, false);
                    g.setAnimationTime(T1 + 400);
                    pointers.add(toX, y);
                } else if (i == 2) {
                    pull(bar, x, y, toX, y, true);
                    g.setAnimationTime(T1 + 110);
                    pointers.add(toX, y);
                } else {
                    pull(bar, x, y, toX, y, true);
                    g.setAnimationTime(T1 + 2000);
                }
            }
        }

        /// A dependency is drawn from one bar to another.
        void link(Form form) {
            for (int i = 0; i < views.length; i++) {
                Gantt g = views[i];
                ProjectModel p = g.getModel();
                g.zoomToFit();
                Hit to = named(g, "Docs");
                g.setSelectedTask(p.getTask("api"));
                Hit handle = cursor(g, bounds(g), Component.CROSSHAIR_CURSOR);
                g.setAnimationTime(T1);
                int x = handle.cx();
                int y = handle.cy();
                if (i == 0) {
                    hover(handle, x, y);
                    g.setAnimationTime(T1 + 140);
                    pointers.add(x, y);
                } else if (i == 1) {
                    pull(handle, x, y, to.cx(), to.cy(), false);
                    g.setAnimationTime(T1 + 400);
                    pointers.add(to.cx(), to.cy());
                } else {
                    pull(handle, x, y, to.cx(), to.cy(), true);
                    g.setAnimationTime(T1 + 2000);
                }
            }
        }
    }

    /// The pickers, several times over.
    private static final class PickerStages {
        private MaterialTimePicker[] times;
        private MaterialDatePicker[] dates;

        Form buildTime() {
            times = new MaterialTimePicker[3];
            for (int i = 0; i < times.length; i++) {
                times[i] = timePicker(true, 10, 10);
            }
            return frames("Time", true, times, new String[]{"Ten o'clock", "Two is set: the hand turns",
                "Two o'clock"});
        }

        /// The hour is set to two and the hand turns there from ten.
        void sweep(Form form) {
            long[] at = {0, 100, 2000};
            for (int i = 0; i < times.length; i++) {
                MaterialTimePicker p = times[i];
                p.setAnimationTime(T1);
                if (i > 0) {
                    p.setTime(14, 10);
                    p.setAnimationTime(T1);
                    p.setAnimationTime(T1 + at[i]);
                }
            }
        }

        Form buildDate() {
            dates = new MaterialDatePicker[3];
            for (int i = 0; i < dates.length; i++) {
                dates[i] = datePicker();
            }
            return frames("Date", true, dates, new String[]{"June", "Next month is asked for", "July"});
        }

        /// The next month slides in over the one shown.
        void slide(Form form) {
            long[] at = {0, 60, 2000};
            for (int i = 0; i < dates.length; i++) {
                MaterialDatePicker p = dates[i];
                p.setAnimationTime(T1);
                if (i > 0) {
                    List<Button> all = new ArrayList<Button>();
                    buttons(p, "PickerMaterialButton", all);
                    // The mode, the month, the month before and the month after.
                    Button next = all.get(all.size() - 1);
                    next.pressed();
                    next.released();
                    p.setAnimationTime(T1 + at[i]);
                }
            }
        }
    }
}
