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
package com.codenameone.examples.hellocodenameone.tests;

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
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.scheduler.Scheduler;

import java.time.ZoneId;

/// The schedule and the project the scheduling screenshot tests show: fixed
/// dates in a fixed zone, and a fixed "now", so a capture is the same on any
/// day and on any device.
final class SchedulingFixtures {
    /// Monday 2026-03-09, as a count of days since 1970-01-01.
    static final long MONDAY = 20521L;

    private SchedulingFixtures() {
    }

    static ZoneId zone() {
        return ZoneId.of("UTC");
    }

    static long at(int day, int hour, int minute) {
        return TimeMath.at(MONDAY + day, hour * 60 + minute, zone());
    }

    /// A week of items for three people: plain events, one that overlaps
    /// another, an all-day one, a band of working time and a blocked stretch.
    static DefaultScheduleModel schedule() {
        DefaultScheduleModel m = new DefaultScheduleModel();
        m.addResource(new ScheduleResource("ana", "Ana").setDetail("Design"));
        m.addResource(new ScheduleResource("ben", "Ben").setDetail("Build"));
        m.addResource(new ScheduleResource("cy", "Cy").setDetail("Test"));
        m.addItem(new ScheduleItem("shift", "", at(0, 8, 0), at(0, 16, 0))
                .setResourceId("ana").setKind(ScheduleItem.Kind.BACKGROUND));
        m.addItem(new ScheduleItem("standup", "Stand-up", at(0, 9, 0), at(0, 9, 30))
                .setResourceId("ana"));
        m.addItem(new ScheduleItem("review", "Design review", at(0, 10, 0), at(0, 11, 30))
                .setResourceId("ana").setDetail("Room 2"));
        m.addItem(new ScheduleItem("pairing", "Pairing", at(0, 10, 30), at(0, 12, 0))
                .setResourceId("ben"));
        m.addItem(new ScheduleItem("busy", "Busy", at(0, 13, 0), at(0, 14, 0))
                .setResourceId("cy").setKind(ScheduleItem.Kind.BLOCKED));
        m.addItem(new ScheduleItem("release", "Release", at(1, 11, 0), at(1, 12, 0))
                .setResourceId("ben").setColor(0x12805c));
        m.addItem(new ScheduleItem("workshop", "Workshop", at(2, 9, 0), at(2, 12, 30))
                .setResourceId("cy"));
        m.addItem(new ScheduleItem("retro", "Retrospective", at(3, 14, 0), at(3, 15, 0))
                .setResourceId("ana"));
        m.addItem(new ScheduleItem("offsite", "Offsite", TimeMath.startOfEpochDay(MONDAY + 4, zone()),
                TimeMath.startOfEpochDay(MONDAY + 5, zone())).setAllDay(true).setResourceId("ben"));
        return m;
    }

    /// A scheduler on the fixed week, at ten past ten on its Monday.
    static Scheduler scheduler(Scheduler.View view) {
        Scheduler s = new Scheduler(schedule());
        s.setZone(zone());
        s.setWorkCalendar(new WorkCalendar(zone()));
        s.set24Hours(true);
        s.setCurrentTime(at(0, 10, 10));
        s.setInitialMinute(8 * 60);
        s.setView(view);
        s.setDate(at(0, 0, 0));
        return s;
    }

    /// A small project: a summary over two linked tasks, a task the same
    /// person is double booked on, and a milestone at the end.
    static ProjectModel project() {
        WorkCalendar calendar = new WorkCalendar(zone());
        ProjectModel p = new ProjectModel(calendar, at(0, 0, 0));
        p.addResource(new ScheduleResource("dev", "Developer"));
        p.addResource(new ScheduleResource("qa", "Tester"));
        long day = calendar.getMillisPerDay();
        GanttTask build = new GanttTask("build", "Build");
        GanttTask api = new GanttTask("api", "API", 3 * day).assign("dev").setPercentComplete(60);
        GanttTask screens = new GanttTask("screens", "Screens", 4 * day).assign("dev");
        GanttTask docs = new GanttTask("docs", "Docs", 2 * day).assign("dev");
        GanttTask testing = new GanttTask("test", "Testing", 2 * day).assign("qa");
        GanttTask release = new GanttTask("release", "Release", 0);
        p.addTask(build);
        p.addTask(build, api);
        p.addTask(build, screens);
        p.addTask(docs);
        p.addTask(testing);
        p.addTask(release);
        p.link(api, screens);
        p.link(screens, testing);
        p.link(testing, release, TaskDependency.Type.FINISH_TO_START, 0);
        p.update();
        return p;
    }

    /// A Material picker the way a `Picker` shows it: a card of its own
    /// width in the middle of the screen, over a dimmed form, with the two
    /// buttons under it. A picker laid across a whole form is stretched to
    /// a width it is never given in an application.
    ///
    /// `mode` is the keyboard button of a time picker, which a dialog moves
    /// to the row of its buttons, or null.
    static Container pickerDialog(Component picker, Button mode) {
        Container card = new Container(new BorderLayout());
        card.setUIID("PickerMaterialDialog");
        card.add(BorderLayout.CENTER, picker);
        Container buttons = new Container(new BorderLayout());
        buttons.setUIID("PickerMaterialButtons");
        if (mode != null) {
            Container was = mode.getParent();
            mode.remove();
            if (was != null) {
                was.setHidden(true);
            }
            buttons.add(BorderLayout.WEST, mode);
        }
        buttons.add(BorderLayout.EAST, BoxLayout.encloseX(new Button("Cancel", "PickerMaterialButton"),
                new Button("OK", "PickerMaterialButton")));
        card.add(BorderLayout.SOUTH, buttons);
        Container scrim = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER));
        scrim.getAllStyles().setBgColor(0);
        scrim.getAllStyles().setBgTransparency(82);
        scrim.add(BorderLayout.CENTER, card);
        return scrim;
    }
}
