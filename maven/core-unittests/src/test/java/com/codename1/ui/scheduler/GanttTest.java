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
package com.codename1.ui.scheduler;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.scheduling.GanttTask;
import com.codename1.scheduling.LevelingResult;
import com.codename1.scheduling.ProjectModel;
import com.codename1.scheduling.ScheduleResource;
import com.codename1.scheduling.TimeMath;
import com.codename1.scheduling.WorkCalendar;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The Gantt chart through its surface: which rows it has, where the bars
/// are, and what a drag does to the project. January 2026 in UTC, where the
/// 5th is a Monday and a working day is 9 to 17.
class GanttTest extends UITestBase {
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final long D = 8 * TimeMath.HOUR;

    private static long at(int day, int hour) {
        return ZonedDateTime.of(2026, 1, day, hour, 0, 0, 0, UTC).toInstant().toEpochMilli();
    }

    private static ProjectModel project() {
        return new ProjectModel(new WorkCalendar(UTC), at(5, 0));
    }

    private static GanttTask task(ProjectModel p, GanttTask parent, String id, int days) {
        GanttTask t = new GanttTask(id, id, days * D);
        if (parent == null) {
            p.addTask(t);
        } else {
            p.addTask(parent, t);
        }
        return t;
    }

    /// Shows a project at a scale of about 48 pixels a day, wide enough to
    /// tell the end of a bar from its middle.
    private Gantt show(ProjectModel model) {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        Gantt g = new Gantt(model);
        g.set24Hours(true);
        g.setCurrentTime(at(5, 10));
        g.setDragEdits(true);
        form.add(BorderLayout.CENTER, g);
        form.revalidate();
        GanttChart chart = g.surface();
        chart.layoutNow();
        double dayPx = TimeMath.DAY / chart.axis().getMillisPerPixel();
        chart.zoom((float) (48 / dayPx));
        chart.layoutNow();
        chart.scrollTo(0, 0);
        return g;
    }

    /// The rectangle of a task's bar in the chart's own coordinates.
    private static int[] bar(GanttChart chart, GanttTask t) {
        int[] r = new int[4];
        int row = chart.rows().indexOf(t);
        assertTrue(row >= 0, t.getId() + " has a row");
        chart.barRect(t, row, r);
        r[0] += chart.headerW - chart.getOffX();
        r[1] += chart.headerH - chart.getOffY();
        return r;
    }

    private static void drag(ScrollSurface s, int fromX, int fromY, int toX, int toY) {
        int ax = s.getAbsoluteX();
        int ay = s.getAbsoluteY();
        s.pointerPressed(ax + fromX, ay + fromY);
        s.pointerDragged(ax + (fromX + toX) / 2, ay + (fromY + toY) / 2);
        s.pointerDragged(ax + toX, ay + toY);
        s.pointerReleased(ax + toX, ay + toY);
    }

    private static void tap(ScrollSurface s, int x, int y) {
        s.pointerPressed(s.getAbsoluteX() + x, s.getAbsoluteY() + y);
        s.pointerReleased(s.getAbsoluteX() + x, s.getAbsoluteY() + y);
    }

    private static int dayPixels(GanttChart chart) {
        return chart.axis().x(at(6, 0)) - chart.axis().x(at(5, 0));
    }

    @FormTest
    void zoomToFitShowsTheProjectsDaysAndNotTheRoomAroundThem() {
        ProjectModel p = project();
        // Long enough that the finest zoom does not stop the fit short.
        GanttTask a = task(p, null, "a", 15);
        GanttTask b = task(p, null, "b", 10);
        p.link(a, b);
        p.update();
        Gantt g = show(p);
        GanttChart chart = g.surface();
        long first = at(5, 0);
        long end = 0;
        for (GanttTask t : p.getTasks()) {
            end = Math.max(end, t.getEnd());
        }
        long last = TimeMath.startOfDay(end - 1, UTC) + TimeMath.DAY;

        g.zoomToFit();
        chart.layoutNow();

        int left = chart.axis().x(first) - chart.getOffX();
        int right = chart.axis().x(last) - chart.getOffX();
        int body = chart.bodyWidth();
        assertEquals(0, left, 1, "the first day starts at the left edge");
        assertEquals(body, right, 2, "the last day ends at the right edge");
    }

    @FormTest
    void rowsFollowTheTreeAndACollapsedSummaryHidesItsChildren() {
        ProjectModel p = project();
        GanttTask phase = task(p, null, "phase", 0);
        GanttTask a = task(p, phase, "a", 2);
        GanttTask b = task(p, phase, "b", 1);
        GanttTask c = task(p, null, "c", 1);
        p.update();
        Gantt g = show(p);
        GanttChart chart = g.surface();
        List<GanttTask> rows = chart.rows();
        assertEquals(4, rows.size());
        assertSame(phase, rows.get(0));
        assertSame(a, rows.get(1));
        assertSame(b, rows.get(2));
        assertSame(c, rows.get(3));

        // The triangle before the name of the summary task.
        int[] r = bar(chart, phase);
        tap(chart, 2, r[1] + r[3] / 2);
        assertTrue(g.isCollapsed(phase));
        chart.layoutNow();
        assertEquals(2, chart.rows().size());
        assertSame(c, chart.rows().get(1));

        g.scrollToTask(b);
        assertFalse(g.isCollapsed(phase), "showing a task opens what it is under");
        chart.layoutNow();
        assertEquals(4, chart.rows().size());

        g.collapseAll();
        chart.layoutNow();
        assertEquals(2, chart.rows().size());
        g.expandAll();
        chart.layoutNow();
        assertEquals(4, chart.rows().size());
    }

    @FormTest
    void aBarSpansItsDatesAndAMilestoneIsCentredOnItsDate() {
        ProjectModel p = project();
        GanttTask a = task(p, null, "a", 2);
        GanttTask done = task(p, null, "done", 0);
        p.link(a, done);
        p.update();
        assertTrue(done.isMilestone());
        Gantt g = show(p);
        GanttChart chart = g.surface();
        TimeAxis axis = chart.axis();
        int[] ra = new int[4];
        chart.barRect(a, 0, ra);
        assertEquals(axis.x(at(5, 9)), ra[0]);
        assertEquals(axis.x(at(6, 17)) - axis.x(at(5, 9)), ra[2]);
        int[] rm = new int[4];
        chart.barRect(done, 1, rm);
        assertEquals(axis.x(at(6, 17)), rm[0] + rm[2] / 2, 1);
        assertTrue(rm[1] > ra[1], "a row each");
        assertTrue(axis.getStart() < at(5, 0), "room before the first task to drag it earlier");
        assertTrue(axis.getEnd() > at(6, 17));
    }

    @FormTest
    void draggingABarConstrainsItsStartAndItsSuccessorFollows() {
        ProjectModel p = project();
        GanttTask a = task(p, null, "a", 2);
        GanttTask b = task(p, null, "b", 2);
        p.link(a, b);
        p.update();
        assertEquals(at(7, 9), b.getStart());
        Gantt g = show(p);
        GanttChart chart = g.surface();
        final List<String> log = new ArrayList<String>();
        g.addGanttListener(new GanttAdapter() {
            @Override
            public boolean taskChanging(Gantt gantt, GanttTask task, long start, long end) {
                log.add("changing " + task.getId() + " " + (start == at(6, 9)) + " " + (end == at(7, 17)));
                return true;
            }

            @Override
            public void taskChanged(Gantt gantt, GanttTask task) {
                log.add("changed " + task.getId());
            }
        });
        int[] r = bar(chart, a);
        int y = r[1] + r[3] / 4;
        drag(chart, r[0] + r[2] / 2, y, r[0] + r[2] / 2 + dayPixels(chart), y);
        assertEquals(at(6, 9), a.getStart());
        assertEquals(at(7, 17), a.getEnd());
        assertEquals(GanttTask.Constraint.START_NO_EARLIER_THAN, a.getConstraint());
        assertEquals(at(8, 9), b.getStart(), "the successor is scheduled after it again");
        assertSame(a, g.getSelectedTask());
        assertEquals(2, log.size());
        assertEquals("changing a true true", log.get(0));
        assertEquals("changed a", log.get(1));
    }

    @FormTest
    void aListenerRefusesADragAndAReadOnlyChartOffersNone() {
        ProjectModel p = project();
        GanttTask a = task(p, null, "a", 2);
        p.update();
        Gantt g = show(p);
        GanttChart chart = g.surface();
        final int[] asked = new int[1];
        g.addGanttListener(new GanttAdapter() {
            @Override
            public boolean taskChanging(Gantt gantt, GanttTask task, long start, long end) {
                asked[0]++;
                return false;
            }
        });
        int[] r = bar(chart, a);
        int y = r[1] + r[3] / 4;
        drag(chart, r[0] + r[2] / 2, y, r[0] + r[2] / 2 + dayPixels(chart), y);
        assertEquals(1, asked[0]);
        assertEquals(at(5, 9), a.getStart());
        assertEquals(GanttTask.Constraint.AS_SOON_AS_POSSIBLE, a.getConstraint());

        g.setEditable(false);
        chart.scrollTo(0, 0);
        r = bar(chart, a);
        drag(chart, r[0] + r[2] / 2, y, r[0] + r[2] / 2 + dayPixels(chart), y);
        assertEquals(1, asked[0], "a read only chart scrolls under a drag");
        assertEquals(at(5, 9), a.getStart());
    }

    @FormTest
    void aMoveSnapsTheDistanceAndNotTheStart() {
        ProjectModel p = project();
        GanttTask a = new GanttTask("a", "a", D / 2);
        p.addTask(a);
        GanttTask b = task(p, null, "b", 1);
        p.link(a, b);
        p.update();
        assertEquals(at(5, 13), b.getStart());
        Gantt g = show(p);
        GanttChart chart = g.surface();
        int[] r = bar(chart, b);
        int y = r[1] + r[3] / 4;
        drag(chart, r[0] + r[2] / 2, y, r[0] + r[2] / 2 + dayPixels(chart) / 4, y);
        assertEquals(at(5, 13), b.getStart(), "less than half a day is no move");
        assertEquals(GanttTask.Constraint.AS_SOON_AS_POSSIBLE, b.getConstraint());

        chart.scrollTo(0, 0);
        r = bar(chart, b);
        drag(chart, r[0] + r[2] / 2, y, r[0] + r[2] / 2 + dayPixels(chart), y);
        assertEquals(at(6, 13), b.getStart(), "a day later, at the hour it started at");
    }

    @FormTest
    void aManualTaskKeepsItsSpanWhenItIsMoved() {
        ProjectModel p = project();
        GanttTask m = new GanttTask("m", "m", 0);
        m.setManuallyScheduled(true);
        m.setDates(at(5, 9), at(6, 17));
        p.addTask(m);
        p.update();
        Gantt g = show(p);
        GanttChart chart = g.surface();
        int[] r = bar(chart, m);
        int y = r[1] + r[3] / 4;
        drag(chart, r[0] + r[2] / 2, y, r[0] + r[2] / 2 + dayPixels(chart), y);
        assertEquals(at(6, 9), m.getStart());
        assertEquals(at(7, 17), m.getEnd());
    }

    @FormTest
    void aMoveKeepsTheKindOfConstraintATaskHas() {
        ProjectModel p = project();
        GanttTask a = task(p, null, "a", 2);
        a.setConstraint(GanttTask.Constraint.MUST_START_ON, at(5, 9));
        p.update();
        Gantt g = show(p);
        GanttChart chart = g.surface();
        int[] r = bar(chart, a);
        int y = r[1] + r[3] / 4;
        drag(chart, r[0] + r[2] / 2, y, r[0] + r[2] / 2 + dayPixels(chart), y);
        assertEquals(GanttTask.Constraint.MUST_START_ON, a.getConstraint());
        assertEquals(at(6, 9), a.getConstraintDate());
        assertEquals(at(6, 9), a.getStart());
    }

    @FormTest
    void aListenerRefusesProgressAndOneThatLeavesDoesNotSilenceTheNext() {
        ProjectModel p = project();
        GanttTask a = task(p, null, "a", 2);
        p.update();
        final Gantt g = show(p);
        GanttChart chart = g.surface();
        final int[] asked = new int[1];
        g.addGanttListener(new GanttAdapter() {
            @Override
            public boolean taskChanging(Gantt gantt, GanttTask task, long start, long end) {
                g.removeGanttListener(this);
                return true;
            }
        });
        g.addGanttListener(new GanttAdapter() {
            @Override
            public boolean taskChanging(Gantt gantt, GanttTask task, long start, long end) {
                asked[0]++;
                return false;
            }
        });
        g.setSelectedTask(a);
        int[] r = bar(chart, a);
        int y = r[1] + r[3];
        drag(chart, r[0], y, r[0] + r[2] / 2, y);
        assertEquals(1, asked[0]);
        assertEquals(0, a.getPercentComplete());
    }

    @FormTest
    void aTaskRemovedWhileItIsHeldEndsTheDrag() {
        ProjectModel p = project();
        GanttTask a = task(p, null, "a", 2);
        task(p, null, "b", 1);
        p.update();
        Gantt g = show(p);
        GanttChart chart = g.surface();
        int[] r = bar(chart, a);
        int y = r[1] + r[3] / 4;
        int ax = chart.getAbsoluteX();
        int ay = chart.getAbsoluteY();
        chart.pointerPressed(ax + r[0] + r[2] / 2, ay + y);
        chart.pointerDragged(ax + r[0] + r[2] / 2 + dayPixels(chart) / 2, ay + y);
        p.removeTask(a);
        p.update();
        chart.pointerDragged(ax + r[0] + r[2] / 2 + dayPixels(chart), ay + y);
        chart.pointerReleased(ax + r[0] + r[2] / 2 + dayPixels(chart), ay + y);
        assertFalse(chart.isDragEditing());
        assertEquals(GanttTask.Constraint.AS_SOON_AS_POSSIBLE, a.getConstraint());
    }

    @FormTest
    void draggingTheEndOfABarChangesItsDuration() {
        ProjectModel p = project();
        GanttTask a = task(p, null, "a", 2);
        GanttTask b = task(p, null, "b", 1);
        p.link(a, b);
        p.update();
        Gantt g = show(p);
        GanttChart chart = g.surface();
        int[] r = bar(chart, a);
        int y = r[1] + r[3] / 4;
        drag(chart, r[0] + r[2] - 1, y, r[0] + r[2] - 1 + dayPixels(chart), y);
        assertEquals(at(5, 9), a.getStart());
        assertEquals(at(7, 17), a.getEnd());
        assertEquals(3 * D, a.getDuration());
        assertEquals(at(8, 9), b.getStart());
    }

    @FormTest
    void theMarkerOfTheSelectedTaskSetsItsProgress() {
        ProjectModel p = project();
        GanttTask a = task(p, null, "a", 2);
        p.update();
        Gantt g = show(p);
        GanttChart chart = g.surface();
        g.setSelectedTask(a);
        int[] r = bar(chart, a);
        int y = r[1] + r[3];
        drag(chart, r[0], y, r[0] + r[2] / 2, y);
        assertEquals(50, a.getPercentComplete());
        assertEquals(at(5, 9), a.getStart(), "progress does not move the task");
    }

    @FormTest
    void draggingFromTheEndOfABarOntoAnotherLinksThem() {
        ProjectModel p = project();
        GanttTask a = task(p, null, "a", 2);
        GanttTask b = task(p, null, "b", 2);
        p.update();
        assertEquals(at(5, 9), b.getStart());
        Gantt g = show(p);
        GanttChart chart = g.surface();
        final List<String> asked = new ArrayList<String>();
        g.addGanttListener(new GanttAdapter() {
            @Override
            public boolean linkCreating(Gantt gantt, GanttTask predecessor, GanttTask successor) {
                asked.add(predecessor.getId() + ">" + successor.getId());
                return true;
            }
        });
        g.setSelectedTask(a);
        int[] ra = bar(chart, a);
        int[] rb = bar(chart, b);
        drag(chart, ra[0] + ra[2] + Look.mm(2.2f), ra[1] + ra[3] / 2, rb[0] + rb[2] / 2, rb[1] + rb[3] / 2);
        assertEquals(1, asked.size());
        assertEquals("a>b", asked.get(0));
        assertEquals(1, p.getDependencies().size());
        assertSame(a, p.getDependencies().get(0).getPredecessor());
        assertSame(b, p.getDependencies().get(0).getSuccessor());
        assertEquals(at(7, 9), b.getStart());

        // The other way round would be a cycle, and is not offered.
        g.setSelectedTask(b);
        chart.layoutNow();
        chart.reflowDone();
        ra = bar(chart, a);
        rb = bar(chart, b);
        drag(chart, rb[0] + rb[2] + Look.mm(2.2f), rb[1] + rb[3] / 2, ra[0] + ra[2] / 2, ra[1] + ra[3] / 2);
        assertEquals(1, asked.size());
        assertEquals(1, p.getDependencies().size());
    }

    @FormTest
    void tapsSelectAndADoubleTapOpens() {
        ProjectModel p = project();
        GanttTask a = task(p, null, "a", 2);
        GanttTask b = task(p, null, "b", 2);
        p.update();
        Gantt g = show(p);
        GanttChart chart = g.surface();
        final List<String> log = new ArrayList<String>();
        g.addGanttListener(new GanttAdapter() {
            @Override
            public void taskSelected(Gantt gantt, GanttTask task) {
                log.add("selected " + (task == null ? "nothing" : task.getId()));
            }

            @Override
            public void taskOpened(Gantt gantt, GanttTask task) {
                log.add("opened " + task.getId());
            }
        });
        int[] rb = bar(chart, b);
        tap(chart, rb[0] + rb[2] / 2, rb[1] + rb[3] / 2);
        assertSame(b, g.getSelectedTask());
        tap(chart, rb[0] + rb[2] / 2, rb[1] + rb[3] / 2);
        assertEquals(2, log.size());
        assertEquals("selected b", log.get(0));
        assertEquals("opened b", log.get(1));

        // The name in the table selects as the bar does.
        int[] ra = bar(chart, a);
        tap(chart, chart.headerW / 2, ra[1] + ra[3] / 2);
        assertSame(a, g.getSelectedTask());
        assertNotNull(chart.barAt(rb[0] + 1, rb[1] + 1));
        assertNull(chart.barAt(rb[0] + rb[2] + 3 * dayPixels(chart), rb[1] + 1));
    }

    @FormTest
    void levelingDelaysTheSecondTaskOfOnePerson() {
        ProjectModel p = project();
        p.addResource(new ScheduleResource("ann", "Ann"));
        GanttTask a = task(p, null, "a", 2).assign("ann");
        GanttTask b = task(p, null, "b", 2).assign("ann");
        p.update();
        Gantt g = show(p);
        assertTrue(g.isOverallocated("ann"));
        LevelingResult r = g.level();
        assertTrue(r.isChanged());
        assertEquals(at(5, 9), a.getStart());
        assertEquals(at(7, 9), b.getStart());
        assertFalse(g.isOverallocated("ann"));
        assertFalse(g.level().isChanged(), "nothing left to level");
    }

    @FormTest
    void theLoadIsShownOnlyWithResourcesAndScrollsWithTheChart() {
        ProjectModel p = project();
        GanttTask a = task(p, null, "a", 30);
        p.update();
        Gantt g = show(p);
        GanttChart chart = g.surface();
        ResourceLoadView load = g.loadSurface();
        assertTrue(load.isHidden());
        g.setLoadVisible(true);
        assertTrue(load.isHidden(), "nothing to show without a resource");

        p.addResource(new ScheduleResource("ann", "Ann"));
        a.assign("ann");
        p.update();
        assertFalse(load.isHidden());
        Display.getInstance().getCurrent().revalidate();
        chart.layoutNow();
        load.layoutNow();
        assertEquals(chart.headerW, load.headerW, "one time axis for both");

        chart.scrollTo(3 * dayPixels(chart), 0);
        assertTrue(chart.getOffX() > 0);
        assertEquals(chart.getOffX(), load.getOffX());
        load.scrollTo(dayPixels(chart), 0);
        assertEquals(dayPixels(chart), chart.getOffX());

        g.setLoadVisible(false);
        assertTrue(load.isHidden());
    }
}
