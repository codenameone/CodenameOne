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

import java.util.List;
import org.junit.jupiter.api.Test;

import static com.codename1.scheduling.ProjectSchedulerTest.D;
import static com.codename1.scheduling.ProjectSchedulerTest.project;
import static com.codename1.scheduling.ProjectSchedulerTest.task;
import static com.codename1.scheduling.WorkCalendarTest.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceLevelerTest {
    @Test
    void twoTasksForOnePersonAreDoneOneAfterTheOther() {
        ProjectModel p = project();
        p.addResource(new ScheduleResource("ann", "Ann"));
        GanttTask a = task(p, "a", 2).assign("ann");
        GanttTask b = task(p, "b", 2).assign("ann");
        p.update();
        List<ResourceOverallocation> over = ResourceLeveler.findOverallocations(p);
        assertEquals(1, over.size());
        assertEquals("ann", over.get(0).getResourceId());
        assertEquals(at(5, 9), over.get(0).getStart());
        assertEquals(at(6, 17), over.get(0).getEnd());
        assertEquals(2f, over.get(0).getPeak(), 0.001f);

        LevelingResult r = ResourceLeveler.level(p);
        // A proposal: nothing has moved yet.
        assertEquals(at(5, 9), b.getStart());
        assertEquals(0, b.getLevelingDelay());
        assertTrue(r.isChanged());
        assertEquals(1, r.getChangedTasks().size());
        assertEquals(b, r.getChangedTasks().get(0));
        assertEquals(2 * D, r.getDelay(b));
        assertEquals(at(6, 17), r.getFinishBefore());
        assertEquals(at(8, 17), r.getFinishAfter());
        assertTrue(r.getUnresolved().isEmpty());

        r.apply();
        assertEquals(at(5, 9), a.getStart());
        assertEquals(at(7, 9), b.getStart());
        assertTrue(ResourceLeveler.findOverallocations(p).isEmpty());

        p.clearLeveling().update();
        assertEquals(at(5, 9), b.getStart());
    }

    @Test
    void theTaskWithSlackWaitsAndTheFinishDoesNotMove() {
        ProjectModel p = project();
        p.addResource(new ScheduleResource("ann", "Ann"));
        GanttTask longOne = task(p, "long", 5);
        GanttTask spare = task(p, "spare", 1).assign("ann");
        GanttTask onPath = task(p, "onPath", 1).assign("ann");
        GanttTask tail = task(p, "tail", 4);
        p.link(onPath, tail);
        p.update();
        assertTrue(onPath.isCritical());
        assertFalse(spare.isCritical());
        LevelingResult r = ResourceLeveler.level(p);
        assertEquals(0, r.getDelay(onPath));
        assertEquals(D, r.getDelay(spare));
        assertEquals(r.getFinishBefore(), r.getFinishAfter());
        assertEquals(at(9, 17), longOne.getEnd());
    }

    @Test
    void priorityDecidesBetweenEquals() {
        ProjectModel p = project();
        p.addResource(new ScheduleResource("ann", "Ann"));
        GanttTask first = task(p, "first", 1).assign("ann").setPriority(100);
        GanttTask second = task(p, "second", 1).assign("ann").setPriority(900);
        p.update();
        LevelingResult r = ResourceLeveler.level(p);
        assertEquals(D, r.getDelay(first));
        assertEquals(0, r.getDelay(second));
    }

    @Test
    void capacityAndUnitsAreCounted() {
        ProjectModel p = project();
        p.addResource(new ScheduleResource("team", "Team").setCapacity(2));
        task(p, "a", 2).assign("team");
        task(p, "b", 2).assign("team", 0.5f);
        GanttTask c = task(p, "c", 2).assign("team", 0.5f);
        p.update();
        assertTrue(ResourceLeveler.findOverallocations(p).isEmpty());
        c.assign("team", 1f);
        assertEquals(1f, c.getUnits("team"), 0.001f);
        assertEquals(1, c.getAssignments().size());
        p.update();
        assertEquals(1, ResourceLeveler.findOverallocations(p).size());

        float[] load = ResourceLeveler.loadProfile(p, "team", at(5, 0), at(9, 0), 4);
        assertEquals(2.5f, load[0], 0.001f);
        assertEquals(2.5f, load[1], 0.001f);
        assertEquals(0f, load[2], 0.001f);
        assertEquals(0, ResourceLeveler.loadProfile(p, "team", at(5, 0), at(5, 0), 4)[0], 0.001f);
    }

    @Test
    void whatCannotBeMovedIsReportedAndLevelingStillEnds() {
        ProjectModel p = project();
        p.addResource(new ScheduleResource("ann", "Ann"));
        // Needs two of one person by itself.
        task(p, "double", 1).assign("ann", 2f);
        // Two that may not be moved.
        task(p, "m1", 1).assign("ann").setConstraint(GanttTask.Constraint.MUST_START_ON, at(12, 9));
        task(p, "m2", 1).assign("ann").setPriority(1000).setConstraint(GanttTask.Constraint.START_NO_EARLIER_THAN, at(12, 9));
        // And one that can be, after them.
        GanttTask free = task(p, "free", 1).assign("ann").setConstraint(GanttTask.Constraint.START_NO_EARLIER_THAN, at(12, 9));
        p.update();
        LevelingResult r = ResourceLeveler.level(p);
        assertEquals(D, r.getDelay(free));
        assertEquals(2, r.getUnresolved().size());
        r.apply();
        assertEquals(at(13, 9), free.getStart());
    }
}
