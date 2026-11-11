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
package com.codename1.scheduling;

import org.junit.jupiter.api.Test;

import static com.codename1.scheduling.WorkCalendarTest.UTC;
import static com.codename1.scheduling.WorkCalendarTest.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A project that starts on Monday 2026-01-05, 09:00 to 17:00, UTC.
class ProjectSchedulerTest {
    static final long D = 8 * TimeMath.HOUR;

    static ProjectModel project() {
        return new ProjectModel(new WorkCalendar(UTC), at(5, 0));
    }

    static GanttTask task(ProjectModel p, String id, int days) {
        GanttTask t = new GanttTask(id, id, days * D);
        p.addTask(t);
        return t;
    }

    static void assertDates(GanttTask t, long start, long end) {
        assertEquals(start, t.getStart(), t.getId() + " start");
        assertEquals(end, t.getEnd(), t.getId() + " end");
    }

    @Test
    void aChainRunsEndToEndAndIsCritical() {
        ProjectModel p = project();
        GanttTask a = task(p, "a", 2);
        GanttTask b = task(p, "b", 3);
        GanttTask c = task(p, "c", 1);
        p.link(a, b);
        p.update();
        assertDates(a, at(5, 9), at(6, 17));
        assertDates(b, at(7, 9), at(9, 17));
        assertDates(c, at(5, 9), at(5, 17));
        assertEquals(at(9, 17), p.getFinish());
        assertTrue(a.isCritical());
        assertTrue(b.isCritical());
        assertFalse(c.isCritical());
        assertEquals(0, a.getTotalSlack());
        assertEquals(4 * D, c.getTotalSlack());
        assertEquals(4 * D, c.getFreeSlack());
        assertEquals(at(9, 9), c.getLateStart());
        assertEquals(at(9, 17), c.getLateFinish());
        assertEquals(at(6, 17), a.getLateFinish());
    }

    @Test
    void aSuccessorStartsAfterTheWeekend() {
        ProjectModel p = project();
        GanttTask a = task(p, "a", 5);
        GanttTask b = task(p, "b", 1);
        p.link(a, b);
        p.update();
        assertDates(a, at(5, 9), at(9, 17));
        assertDates(b, at(12, 9), at(12, 17));
    }

    @Test
    void lagAndLeadAreWorkingTime() {
        ProjectModel p = project();
        GanttTask a = task(p, "a", 2);
        GanttTask b = task(p, "b", 1);
        TaskDependency d = p.link(a, b, TaskDependency.Type.FINISH_TO_START, D);
        p.update();
        assertDates(b, at(8, 9), at(8, 17));
        d.setLag(-D);
        p.update();
        assertDates(b, at(6, 9), at(6, 17));
        // Slack is what is left before the project's end, which a still sets.
        assertEquals(0, b.getTotalSlack());
    }

    @Test
    void everyLinkTypeHoldsTheRightEnd() {
        ProjectModel p = project();
        GanttTask a = task(p, "a", 3);
        GanttTask ss = task(p, "ss", 1);
        GanttTask ff = task(p, "ff", 1);
        GanttTask sf = task(p, "sf", 1);
        p.link(a, ss, TaskDependency.Type.START_TO_START, D);
        p.link(a, ff, TaskDependency.Type.FINISH_TO_FINISH, 0);
        p.link(a, sf, TaskDependency.Type.START_TO_FINISH, 2 * D);
        p.update();
        assertDates(a, at(5, 9), at(7, 17));
        assertDates(ss, at(6, 9), at(6, 17));
        assertDates(ff, at(7, 9), at(7, 17));
        // Finishes two days after a starts: the end of Tuesday.
        assertDates(sf, at(6, 9), at(6, 17));
        assertTrue(ff.isCritical());
        assertEquals(D, ss.getTotalSlack());
    }

    @Test
    void constraintsMoveATaskOrAreReportedBroken() {
        ProjectModel p = project();
        GanttTask a = task(p, "a", 2);
        GanttTask b = task(p, "b", 1);
        p.link(a, b);
        b.setConstraint(GanttTask.Constraint.START_NO_EARLIER_THAN, at(8, 9));
        p.update();
        assertDates(b, at(8, 9), at(8, 17));
        assertFalse(b.isConstraintViolated());
        assertEquals(D, a.getTotalSlack());

        b.setConstraint(GanttTask.Constraint.FINISH_NO_LATER_THAN, at(6, 17));
        p.update();
        assertDates(b, at(7, 9), at(7, 17));
        assertTrue(b.isConstraintViolated());
        assertTrue(a.getTotalSlack() < 0);
        assertTrue(a.isCritical());

        b.setConstraint(GanttTask.Constraint.MUST_START_ON, at(6, 9));
        p.update();
        assertDates(b, at(6, 9), at(6, 17));
        assertTrue(b.isConstraintViolated());

        b.setConstraint(GanttTask.Constraint.MUST_FINISH_ON, at(13, 17));
        p.update();
        assertDates(b, at(13, 9), at(13, 17));
        assertFalse(b.isConstraintViolated());

        b.setConstraint(GanttTask.Constraint.FINISH_NO_EARLIER_THAN, at(9, 17));
        p.update();
        assertDates(b, at(9, 9), at(9, 17));
    }

    @Test
    void aSummarySpansItsChildrenAndPassesItsLinksDown() {
        ProjectModel p = project();
        GanttTask prep = task(p, "prep", 1);
        GanttTask phase = new GanttTask("phase", "Phase");
        p.addTask(phase);
        GanttTask x = new GanttTask("x", "x", 2 * D).setPercentComplete(100);
        GanttTask y = new GanttTask("y", "y", 2 * D);
        p.addTask(phase, x).addTask(phase, y);
        p.link(x, y);
        p.link(prep, phase);
        GanttTask after = task(p, "after", 1);
        p.link(phase, after);
        p.update();
        assertTrue(phase.isSummary());
        assertEquals(1, x.getLevel());
        assertDates(x, at(6, 9), at(7, 17));
        assertDates(y, at(8, 9), at(9, 17));
        assertDates(phase, at(6, 9), at(9, 17));
        assertEquals(4 * D, phase.getDuration());
        assertEquals(50, phase.getPercentComplete());
        assertDates(after, at(12, 9), at(12, 17));
        assertTrue(phase.isCritical());
        assertTrue(prep.isCritical());
        assertEquals(at(9, 17), y.getLateFinish());
    }

    @Test
    void aLoopIsRefused() {
        ProjectModel p = project();
        GanttTask a = task(p, "a", 1);
        GanttTask b = task(p, "b", 1);
        GanttTask c = task(p, "c", 1);
        p.link(a, b);
        p.link(b, c);
        assertFalse(p.canLink(c, a));
        assertFalse(p.canLink(a, a));
        assertFalse(p.canLink(a, b));
        assertThrows(IllegalArgumentException.class, () -> p.link(c, a));
        assertEquals(2, p.getDependencies().size());

        GanttTask group = new GanttTask("group", "group");
        p.addTask(group);
        p.addTask(group, new GanttTask("inner", "inner", D));
        assertFalse(p.canLink(group, p.getTask("inner")));
        // c waits for b; putting b under a summary that waits for c is a loop.
        p.link(c, group);
        assertFalse(p.moveTask(b, group, -1));
        assertNull(b.getParent());
        // And so is a, which b waits for.
        assertFalse(p.moveTask(a, group, 0));
        GanttTask free = task(p, "free", 1);
        assertTrue(p.moveTask(free, group, 0));
        assertEquals(group, free.getParent());
        assertEquals(free, group.getChildren().get(0));
        assertFalse(p.moveTask(group, free, -1));
        p.update();
        assertNotNull(p.getTask("inner"));
    }

    @Test
    void aTaskCannotBePutUnderASummaryThatWaitsForIt() {
        ProjectModel p = project();
        GanttTask a = task(p, "a", 1);
        GanttTask group = new GanttTask("group", "group");
        p.addTask(group);
        p.addTask(group, new GanttTask("inner", "inner", D));
        p.link(a, group);
        assertThrows(IllegalArgumentException.class, () -> p.addTask(group, a));
        assertNull(a.getParent());
        assertTrue(p.getRootTasks().contains(a));
        assertTrue(p.contains(a));

        GanttTask stranger = new GanttTask("stranger", "stranger", D);
        assertFalse(p.contains(stranger));
        assertFalse(p.canLink(a, stranger));
        assertFalse(p.canLink(stranger, a));
        p.removeTask(a);
        assertFalse(p.contains(a));
        p.update();
    }

    @Test
    void aMilestoneSitsWhereThePreviousTaskEnds() {
        ProjectModel p = project();
        GanttTask a = task(p, "a", 2);
        GanttTask done = task(p, "done", 0);
        GanttTask next = task(p, "next", 1);
        p.link(a, done);
        p.link(done, next);
        p.update();
        assertTrue(done.isMilestone());
        assertDates(done, at(6, 17), at(6, 17));
        assertDates(next, at(7, 9), at(7, 17));
        assertTrue(done.isCritical());
    }

    @Test
    void aManualTaskKeepsItsDatesAndItsSuccessorsFollow() {
        ProjectModel p = project();
        GanttTask a = task(p, "a", 1);
        GanttTask fixed = task(p, "fixed", 1).setManuallyScheduled(true).setDates(at(14, 9), at(15, 17));
        GanttTask b = task(p, "b", 1);
        p.link(a, fixed);
        p.link(fixed, b);
        p.update();
        assertDates(fixed, at(14, 9), at(15, 17));
        assertDates(b, at(16, 9), at(16, 17));
        assertFalse(a.isCritical());
    }

    @Test
    void removingATaskRemovesItsLinksAndTheBaselineRemembers() {
        ProjectModel p = project();
        GanttTask a = task(p, "a", 1);
        GanttTask b = task(p, "b", 1);
        GanttTask c = task(p, "c", 1);
        p.link(a, b);
        p.link(b, c);
        p.update();
        p.captureBaseline();
        assertEquals(1, p.getPredecessors(b).size());
        assertEquals(1, p.getSuccessors(b).size());
        p.removeTask(b);
        p.setCriticalSlack(D);
        p.update();
        assertEquals(0, p.getDependencies().size());
        assertDates(c, at(5, 9), at(5, 17));
        assertTrue(c.hasBaseline());
        assertEquals(at(7, 9), c.getBaselineStart());
        assertEquals(2, p.getTasks().size());
        assertTrue(c.isCritical());
    }
}
