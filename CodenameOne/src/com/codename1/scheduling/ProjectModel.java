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

/// A project: a tree of tasks, the links between them, the resources they
/// use and the calendar their durations are measured in.
///
/// Building or editing a project is two steps. The methods that change it
/// (`addTask`, `link`, a setter on a task) only change it; `update` then
/// schedules the whole project and tells the listeners, so a hundred edits
/// cost one pass:
///
/// ```java
/// ProjectModel p = new ProjectModel();
/// GanttTask design = new GanttTask("design", "Design", 3 * p.getCalendar().getMillisPerDay());
/// GanttTask build = new GanttTask("build", "Build", 5 * p.getCalendar().getMillisPerDay());
/// p.addTask(design).addTask(build);
/// p.link(design, build);
/// p.update();
/// ```
public class ProjectModel {
    private final List<GanttTask> roots = new ArrayList<GanttTask>();
    private final List<TaskDependency> dependencies = new ArrayList<TaskDependency>();
    private final List<ScheduleResource> resources = new ArrayList<ScheduleResource>();
    private final List<ProjectChangeListener> listeners = new ArrayList<ProjectChangeListener>();
    private WorkCalendar calendar;
    private long start;
    long finish;
    private long criticalSlack;
    private List<GanttTask> flat;

    /// A project that starts today, on the default working week.
    public ProjectModel() {
        this(new WorkCalendar(), System.currentTimeMillis());
    }

    public ProjectModel(WorkCalendar calendar, long start) {
        if (calendar == null) {
            throw new IllegalArgumentException("calendar");
        }
        this.calendar = calendar;
        this.start = TimeMath.startOfDay(start, calendar.getZone());
        this.finish = this.start;
    }

    public WorkCalendar getCalendar() {
        return calendar;
    }

    public ProjectModel setCalendar(WorkCalendar v) {
        if (v == null) {
            throw new IllegalArgumentException("calendar");
        }
        calendar = v;
        return this;
    }

    /// The earliest any task is scheduled.
    public long getStart() {
        return start;
    }

    public ProjectModel setStart(long millis) {
        start = millis;
        return this;
    }

    /// When the last task finishes, from the last scheduling pass.
    public long getFinish() {
        return finish;
    }

    /// A task with this much total slack or less is critical. Zero by
    /// default; a day makes the tasks that are nearly critical stand out too.
    public long getCriticalSlack() {
        return criticalSlack;
    }

    public ProjectModel setCriticalSlack(long millis) {
        criticalSlack = Math.max(0, millis);
        return this;
    }

    /// Adds a top level task.
    public ProjectModel addTask(GanttTask task) {
        return addTask(null, -1, task);
    }

    /// Adds a task under `parent`, which becomes a summary.
    public ProjectModel addTask(GanttTask parent, GanttTask task) {
        return addTask(parent, -1, task);
    }

    /// Adds a task at a position among `parent`'s children; -1 is the end,
    /// and a null parent is the top level. A task that is in the project
    /// already is moved.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the task would go under itself,
    ///   when the parent is not in this project or the task is still in
    ///   another, or when its links would then run in a loop through the
    ///   summary
    public ProjectModel addTask(GanttTask parent, int index, GanttTask task) {
        if (task == null || task == parent) { //NOPMD CompareObjectsWithEquals
            throw new IllegalArgumentException("task");
        }
        if (parent != null && parent.isDescendantOf(task)) {
            throw new IllegalArgumentException("A task cannot go under one of its own children");
        }
        if (parent != null && !contains(parent)) {
            throw new IllegalArgumentException("The parent is not a task of this project");
        }
        if (task.parent != null && !contains(task)) {
            // Its place in the other tree is not this project's to take.
            throw new IllegalArgumentException("The task is in another project; remove it there first");
        }
        if (dependencies.isEmpty()) {
            // Nothing is linked, so no place in the tree can close a loop.
            place(parent, index, task);
            return this;
        }
        boolean was = contains(task);
        GanttTask oldParent = task.parent;
        int oldIndex = was ? (oldParent == null ? roots : oldParent.children).indexOf(task) : -1;
        place(parent, index, task);
        if (ProjectScheduler.order(this) == null) {
            // A link into a summary holds its children, so a task that is
            // linked to its new parent would wait for itself.
            if (was) {
                place(oldParent, oldIndex, task);
            } else {
                detach(task);
                task.parent = null;
                flat = null;
            }
            throw new IllegalArgumentException("The links of " + task + " would run in a loop under " + parent);
        }
        return this;
    }

    /// Whether a task is in this project's tree.
    public boolean contains(GanttTask task) {
        if (task == null) {
            return false;
        }
        GanttTask top = task;
        while (top.parent != null) {
            top = top.parent;
        }
        return roots.contains(top);
    }

    private void place(GanttTask parent, int index, GanttTask task) {
        detach(task);
        List<GanttTask> into = parent == null ? roots : parent.children;
        if (index < 0 || index > into.size()) {
            into.add(task);
        } else {
            into.add(index, task);
        }
        task.parent = parent;
        flat = null;
    }

    /// Moves a task, with everything under it, to another place in the tree.
    ///
    /// #### Returns
    ///
    /// false when the move would put the task under itself or would close a
    /// loop of links through a summary
    public boolean moveTask(GanttTask task, GanttTask newParent, int index) {
        if (task == newParent || (newParent != null && newParent.isDescendantOf(task))) { //NOPMD CompareObjectsWithEquals
            return false;
        }
        GanttTask oldParent = task.parent;
        List<GanttTask> from = oldParent == null ? roots : oldParent.children;
        int oldIndex = from.indexOf(task);
        place(newParent, index, task);
        if (ProjectScheduler.order(this) == null) {
            place(oldParent, oldIndex, task);
            return false;
        }
        return true;
    }

    /// Removes a task, everything under it, and every link to any of them.
    public ProjectModel removeTask(GanttTask task) {
        for (int i = dependencies.size() - 1; i >= 0; i--) {
            TaskDependency d = dependencies.get(i);
            if (within(d.getPredecessor(), task) || within(d.getSuccessor(), task)) {
                dependencies.remove(i);
            }
        }
        detach(task);
        task.parent = null;
        flat = null;
        return this;
    }

    private static boolean within(GanttTask t, GanttTask root) {
        return t == root || t.isDescendantOf(root); //NOPMD CompareObjectsWithEquals
    }

    private void detach(GanttTask task) {
        if (task.parent != null) {
            task.parent.children.remove(task);
        } else {
            roots.remove(task);
        }
    }

    public List<GanttTask> getRootTasks() {
        return Collections.unmodifiableList(roots);
    }

    /// Every task, a parent before its children, in the order of the outline.
    public List<GanttTask> getTasks() {
        if (flat == null) {
            List<GanttTask> out = new ArrayList<GanttTask>();
            flatten(roots, out);
            flat = Collections.unmodifiableList(out);
        }
        return flat;
    }

    private static void flatten(List<GanttTask> level, List<GanttTask> out) {
        for (GanttTask t : level) {
            out.add(t);
            if (!t.children.isEmpty()) {
                flatten(t.children, out);
            }
        }
    }

    public GanttTask getTask(String id) {
        List<GanttTask> all = getTasks();
        for (GanttTask candidate : all) {
            if (candidate.getId().equals(id)) {
                return candidate;
            }
        }
        return null;
    }

    /// Links two tasks finish to start.
    public TaskDependency link(GanttTask predecessor, GanttTask successor) {
        return link(predecessor, successor, TaskDependency.Type.FINISH_TO_START, 0);
    }

    /// Links two tasks.
    ///
    /// A link whose successor is a summary holds the start of every task
    /// under it, whatever its type: a summary's finish is the result of its
    /// children and cannot be held by itself.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `canLink` says no
    public TaskDependency link(GanttTask predecessor, GanttTask successor, TaskDependency.Type type, long lag) {
        if (!canLink(predecessor, successor)) {
            throw new IllegalArgumentException("Cannot link " + predecessor + " to " + successor);
        }
        TaskDependency d = new TaskDependency(predecessor, successor, type, lag);
        dependencies.add(d);
        return d;
    }

    /// Whether a link from `predecessor` to `successor` is possible: they are
    /// two tasks of this project, neither is inside the other, they are not
    /// linked already, and the link would not close a loop.
    public boolean canLink(GanttTask predecessor, GanttTask successor) {
        if (predecessor == null || successor == null || predecessor == successor //NOPMD CompareObjectsWithEquals
                || predecessor.isDescendantOf(successor) || successor.isDescendantOf(predecessor)) {
            return false;
        }
        // The engine finds a task by its place in the outline, which a task
        // outside the project does not have.
        if (!contains(predecessor) || !contains(successor)) {
            return false;
        }
        if (getDependency(predecessor, successor) != null) {
            return false;
        }
        TaskDependency probe = new TaskDependency(predecessor, successor, TaskDependency.Type.FINISH_TO_START, 0);
        dependencies.add(probe);
        boolean ok = ProjectScheduler.order(this) != null;
        dependencies.remove(dependencies.size() - 1);
        return ok;
    }

    public ProjectModel unlink(TaskDependency d) {
        dependencies.remove(d);
        return this;
    }

    public List<TaskDependency> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    /// The link from one task to another, or null.
    public TaskDependency getDependency(GanttTask predecessor, GanttTask successor) {
        for (TaskDependency d : dependencies) {
            if (d.getPredecessor() == predecessor && d.getSuccessor() == successor) { //NOPMD CompareObjectsWithEquals
                return d;
            }
        }
        return null;
    }

    /// The links that end at `task`.
    public List<TaskDependency> getPredecessors(GanttTask task) {
        List<TaskDependency> out = new ArrayList<TaskDependency>();
        for (TaskDependency d : dependencies) {
            if (d.getSuccessor() == task) { //NOPMD CompareObjectsWithEquals
                out.add(d);
            }
        }
        return out;
    }

    /// The links that start at `task`.
    public List<TaskDependency> getSuccessors(GanttTask task) {
        List<TaskDependency> out = new ArrayList<TaskDependency>();
        for (TaskDependency d : dependencies) {
            if (d.getPredecessor() == task) { //NOPMD CompareObjectsWithEquals
                out.add(d);
            }
        }
        return out;
    }

    public ProjectModel addResource(ScheduleResource r) {
        if (r != null) {
            resources.add(r);
        }
        return this;
    }

    public ProjectModel removeResource(ScheduleResource r) {
        resources.remove(r);
        return this;
    }

    public List<ScheduleResource> getResources() {
        return Collections.unmodifiableList(resources);
    }

    public ScheduleResource getResource(String id) {
        for (ScheduleResource resource : resources) {
            if (resource.getId().equals(id)) {
                return resource;
            }
        }
        return null;
    }

    /// Records every task's current dates as its baseline.
    public ProjectModel captureBaseline() {
        List<GanttTask> all = getTasks();
        for (GanttTask t : all) {
            t.setBaseline(t.getStart(), t.getEnd());
        }
        return this;
    }

    /// Removes every leveling delay. Call `update` after it.
    public ProjectModel clearLeveling() {
        List<GanttTask> all = getTasks();
        for (GanttTask each : all) {
            each.setLevelingDelay(0);
        }
        return this;
    }

    /// Schedules the project and tells the listeners. Call it after a change,
    /// or after a group of them.
    public void update() {
        ProjectScheduler.schedule(this);
        fireChanged();
    }

    public void addChangeListener(ProjectChangeListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeChangeListener(ProjectChangeListener l) {
        listeners.remove(l);
    }

    /// Tells the listeners without scheduling, for a change that moves no
    /// date, such as a name.
    public void fireChanged() {
        for (int i = listeners.size() - 1; i >= 0; i--) {
            if (i < listeners.size()) {
                listeners.get(i).projectChanged(this);
            }
        }
    }
}
