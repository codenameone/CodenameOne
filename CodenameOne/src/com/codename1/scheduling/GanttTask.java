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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A task in a project: a piece of work, a milestone, or a summary of the
/// tasks under it.
///
/// A task with children is a summary, and its dates are the span of its
/// children. A task with no children and no duration is a milestone. For any
/// other, `duration` is working time in the project's `WorkCalendar`, and
/// `ProjectScheduler` works out `start` and `end` from the dependencies, the
/// constraint and the leveling delay. A task that is scheduled manually keeps
/// the dates it was given.
///
/// Add a task to a `ProjectModel` before using it; the model owns the tree.
public final class GanttTask {
    /// A limit on when a task may be scheduled, on top of its dependencies.
    public enum Constraint {
        /// No limit: as early as the dependencies allow.
        AS_SOON_AS_POSSIBLE,
        START_NO_EARLIER_THAN,
        START_NO_LATER_THAN,
        FINISH_NO_EARLIER_THAN,
        FINISH_NO_LATER_THAN,
        /// Starts on the date whatever the dependencies say.
        MUST_START_ON,
        /// Finishes on the date whatever the dependencies say.
        MUST_FINISH_ON
    }

    private final String id;
    private String name;
    GanttTask parent;
    final List<GanttTask> children = new ArrayList<GanttTask>();
    final List<ResourceAssignment> assignments = new ArrayList<ResourceAssignment>();
    private long duration;
    private long start;
    private long end;
    private int percentComplete;
    private Constraint constraint = Constraint.AS_SOON_AS_POSSIBLE;
    private long constraintDate;
    private boolean manuallyScheduled;
    private long levelingDelay;
    private int priority = 500;
    private boolean hasBaseline;
    private long baselineStart;
    private long baselineEnd;
    private int color = -1;
    private String uiid;
    private Object userObject;

    // What the scheduler worked out.
    long earlyStart;
    long earlyFinish;
    long lateStart;
    long lateFinish;
    long totalSlack;
    long freeSlack;
    boolean critical;
    boolean constraintViolated;
    // The task's position in the scheduler's arrays during a pass.
    int index;

    public GanttTask(String id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("id");
        }
        this.id = id;
        this.name = name == null ? "" : name;
    }

    /// A task of `duration` milliseconds of working time.
    public GanttTask(String id, String name, long duration) {
        this(id, name);
        setDuration(duration);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public GanttTask setName(String v) {
        name = v == null ? "" : v;
        return this;
    }

    public GanttTask getParent() {
        return parent;
    }

    public List<GanttTask> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean isSummary() {
        return !children.isEmpty();
    }

    public boolean isMilestone() {
        return children.isEmpty() && duration == 0 && !manuallyScheduled;
    }

    /// How deep the task is in the tree; a top level task is at 0.
    public int getLevel() {
        int level = 0;
        for (GanttTask p = parent; p != null; p = p.parent) {
            level++;
        }
        return level;
    }

    /// Whether `other` is this task's parent, or its parent's, and so on.
    public boolean isDescendantOf(GanttTask other) {
        for (GanttTask p = parent; p != null; p = p.parent) {
            if (p == other) { //NOPMD CompareObjectsWithEquals - identity is the point
                return true;
            }
        }
        return false;
    }

    /// Working time the task takes, in milliseconds. Ignored for a summary.
    public long getDuration() {
        return duration;
    }

    public GanttTask setDuration(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("duration");
        }
        duration = millis;
        return this;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    /// Sets the dates directly. The scheduler overwrites them unless the task
    /// is scheduled manually.
    public GanttTask setDates(long start, long end) {
        if (end < start) {
            throw new IllegalArgumentException("end before start");
        }
        this.start = start;
        this.end = end;
        return this;
    }

    /// 0 to 100.
    public int getPercentComplete() {
        return percentComplete;
    }

    public GanttTask setPercentComplete(int v) {
        percentComplete = Math.max(0, Math.min(100, v));
        return this;
    }

    public Constraint getConstraint() {
        return constraint;
    }

    public long getConstraintDate() {
        return constraintDate;
    }

    public GanttTask setConstraint(Constraint type, long date) {
        constraint = type == null ? Constraint.AS_SOON_AS_POSSIBLE : type;
        constraintDate = date;
        return this;
    }

    /// Whether the task keeps the dates it was given instead of being moved by
    /// its dependencies. Its successors still follow it.
    public boolean isManuallyScheduled() {
        return manuallyScheduled;
    }

    public GanttTask setManuallyScheduled(boolean v) {
        manuallyScheduled = v;
        return this;
    }

    /// Working time the task is held back by after its dependencies would let
    /// it start. `ResourceLeveler` sets it; zero undoes leveling.
    public long getLevelingDelay() {
        return levelingDelay;
    }

    public GanttTask setLevelingDelay(long millis) {
        levelingDelay = Math.max(0, millis);
        return this;
    }

    /// 0 to 1000; when two tasks want the same resource the higher keeps its
    /// dates. 1000 is never delayed by leveling.
    public int getPriority() {
        return priority;
    }

    public GanttTask setPriority(int v) {
        priority = Math.max(0, Math.min(1000, v));
        return this;
    }

    public boolean hasBaseline() {
        return hasBaseline;
    }

    public long getBaselineStart() {
        return baselineStart;
    }

    public long getBaselineEnd() {
        return baselineEnd;
    }

    /// Records the dates the task was planned at, to compare with later.
    public GanttTask setBaseline(long start, long end) {
        hasBaseline = true;
        baselineStart = start;
        baselineEnd = end;
        return this;
    }

    public GanttTask clearBaseline() {
        hasBaseline = false;
        return this;
    }

    public List<ResourceAssignment> getAssignments() {
        return Collections.unmodifiableList(assignments);
    }

    /// Assigns a resource full time.
    public GanttTask assign(String resourceId) {
        return assign(resourceId, 1f);
    }

    /// Assigns `units` of a resource: `0.5f` is half of somebody's time.
    public GanttTask assign(String resourceId, float units) {
        unassign(resourceId);
        assignments.add(new ResourceAssignment(resourceId, units));
        return this;
    }

    public GanttTask unassign(String resourceId) {
        for (int i = assignments.size() - 1; i >= 0; i--) {
            if (assignments.get(i).getResourceId().equals(resourceId)) {
                assignments.remove(i);
            }
        }
        return this;
    }

    /// The units of a resource the task uses, 0 when it is not assigned.
    public float getUnits(String resourceId) {
        for (int i = 0; i < assignments.size(); i++) {
            if (assignments.get(i).getResourceId().equals(resourceId)) {
                return assignments.get(i).getUnits();
            }
        }
        return 0f;
    }

    /// An RGB colour for the bar, or -1 for the theme's.
    public int getColor() {
        return color;
    }

    public GanttTask setColor(int v) {
        color = v;
        return this;
    }

    /// The UIID the bar is drawn with in place of `GanttBar`, or null.
    public String getUIID() {
        return uiid;
    }

    public GanttTask setUIID(String v) {
        uiid = v;
        return this;
    }

    public Object getUserObject() {
        return userObject;
    }

    public GanttTask setUserObject(Object v) {
        userObject = v;
        return this;
    }

    /// The earliest the task can start, from the last scheduling pass.
    public long getEarlyStart() {
        return earlyStart;
    }

    public long getEarlyFinish() {
        return earlyFinish;
    }

    /// The latest the task can start without delaying the project.
    public long getLateStart() {
        return lateStart;
    }

    public long getLateFinish() {
        return lateFinish;
    }

    /// Working time the task can slip by without delaying the project.
    /// Negative when a constraint or a deadline cannot be met.
    public long getTotalSlack() {
        return totalSlack;
    }

    /// Working time the task can slip by without delaying any successor.
    public long getFreeSlack() {
        return freeSlack;
    }

    /// Whether the task is on the critical path: any slip delays the project.
    public boolean isCritical() {
        return critical;
    }

    /// Whether the dependencies pushed the task past a "no later than"
    /// constraint, or a "must" constraint pulled it ahead of a predecessor.
    public boolean isConstraintViolated() {
        return constraintViolated;
    }

    @Override
    public String toString() {
        return name;
    }
}
