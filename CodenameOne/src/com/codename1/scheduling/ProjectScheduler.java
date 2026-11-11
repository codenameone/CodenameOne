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
import java.util.List;

/// Works out when every task of a project happens.
///
/// A forward pass gives each task the earliest dates its predecessors, its
/// constraint and its leveling delay allow, in the project's working time. A
/// backward pass from the project's finish gives the latest dates that delay
/// nothing, and the difference is slack: a task with none is critical.
///
/// `ProjectModel.update` runs it. The whole project is scheduled every time;
/// a pass is linear in tasks and links, and a partial one would have to prove
/// what it may skip.
public final class ProjectScheduler {
    private ProjectScheduler() {
    }

    /// Schedules `model`, writing dates and slack into its tasks.
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when the links form a loop, which
    ///   `ProjectModel.link` and `moveTask` do not allow
    public static void schedule(ProjectModel model) {
        List<GanttTask> tasks = model.getTasks();
        int[] order = order(model);
        if (order == null) {
            throw new IllegalStateException("The project's links form a loop");
        }
        int n = tasks.size();
        WorkCalendar cal = model.getCalendar();
        List<TaskDependency> deps = model.getDependencies();
        List<List<TaskDependency>> incoming = new ArrayList<List<TaskDependency>>(n);
        List<List<TaskDependency>> outgoing = new ArrayList<List<TaskDependency>>(n);
        for (int i = 0; i < n; i++) {
            incoming.add(null);
            outgoing.add(null);
        }
        for (TaskDependency d : deps) {
            put(incoming, d.getSuccessor().index, d);
            put(outgoing, d.getPredecessor().index, d);
        }

        long projectStart = cal.nextWorkingTime(model.getStart());
        long projectFinish = projectStart;
        for (int o = 0; o < n; o++) {
            GanttTask t = tasks.get(order[o]);
            if (t.isSummary()) {
                long es = Long.MAX_VALUE;
                long ef = Long.MIN_VALUE;
                for (int c = 0; c < t.children.size(); c++) {
                    GanttTask child = t.children.get(c);
                    es = Math.min(es, child.earlyStart);
                    ef = Math.max(ef, child.earlyFinish);
                }
                t.earlyStart = es;
                t.earlyFinish = ef;
                continue;
            }
            forward(t, cal, incoming, projectStart);
            projectFinish = Math.max(projectFinish, t.earlyFinish);
        }
        model.finish = projectFinish;

        List<GanttTask> leaves = new ArrayList<GanttTask>();
        for (int o = n - 1; o >= 0; o--) {
            GanttTask t = tasks.get(order[o]);
            if (!t.isSummary()) {
                backward(t, cal, outgoing, projectFinish, leaves);
            }
        }
        long threshold = model.getCriticalSlack();
        for (int i = 0; i < n; i++) {
            GanttTask t = tasks.get(i);
            if (t.isSummary()) {
                continue;
            }
            t.totalSlack = cal.workingTimeBetween(t.earlyStart, t.lateStart);
            t.critical = t.totalSlack <= threshold;
            t.freeSlack = freeSlack(t, cal, outgoing, projectFinish, leaves);
            if (!t.isManuallyScheduled()) {
                t.setDates(t.earlyStart, t.earlyFinish);
            }
        }
        // A parent comes before its children in the outline, so backwards is
        // children first.
        for (int i = n - 1; i >= 0; i--) {
            GanttTask t = tasks.get(i);
            if (t.isSummary()) {
                rollUp(t, cal);
            }
        }
    }

    /// The tasks' indices in an order in which every task comes after what it
    /// waits for, or null when the links form a loop. Also numbers the tasks.
    static int[] order(ProjectModel model) {
        List<GanttTask> tasks = model.getTasks();
        int n = tasks.size();
        for (int i = 0; i < n; i++) {
            tasks.get(i).index = i;
        }
        List<TaskDependency> deps = model.getDependencies();
        // Edges as two parallel lists: a summary waits for its children, a
        // successor for its predecessor, and every task under a summary that
        // is a successor waits for that predecessor too.
        IntList from = new IntList();
        IntList to = new IntList();
        for (int i = 0; i < n; i++) {
            GanttTask t = tasks.get(i);
            if (t.parent != null) {
                from.add(i);
                to.add(t.parent.index);
            }
        }
        List<GanttTask> leaves = new ArrayList<GanttTask>();
        for (TaskDependency d : deps) {
            GanttTask s = d.getSuccessor();
            if (s.isSummary()) {
                leaves.clear();
                leavesOf(s, leaves);
                for (GanttTask leaf : leaves) {
                    from.add(d.getPredecessor().index);
                    to.add(leaf.index);
                }
            } else {
                from.add(d.getPredecessor().index);
                to.add(s.index);
            }
        }
        int edges = from.size;
        int[] degree = new int[n];
        int[] firstEdge = new int[n + 1];
        for (int e = 0; e < edges; e++) {
            degree[to.data[e]]++;
            firstEdge[from.data[e] + 1]++;
        }
        for (int i = 0; i < n; i++) {
            firstEdge[i + 1] += firstEdge[i];
        }
        int[] fill = new int[n];
        int[] target = new int[edges];
        for (int e = 0; e < edges; e++) {
            int f = from.data[e];
            target[firstEdge[f] + fill[f]++] = to.data[e];
        }
        int[] out = new int[n];
        int head = 0;
        int tail = 0;
        for (int i = 0; i < n; i++) {
            if (degree[i] == 0) {
                out[tail++] = i;
            }
        }
        while (head < tail) {
            int t = out[head++];
            for (int e = firstEdge[t]; e < firstEdge[t + 1]; e++) {
                int next = target[e];
                degree[next]--;
                if (degree[next] == 0) {
                    out[tail++] = next;
                }
            }
        }
        return tail == n ? out : null;
    }

    private static void forward(GanttTask t, WorkCalendar cal, List<List<TaskDependency>> incoming, long projectStart) {
        t.constraintViolated = false;
        if (t.isManuallyScheduled()) {
            t.earlyStart = t.getStart();
            t.earlyFinish = t.getEnd();
            return;
        }
        long dur = t.getDuration();
        long s = projectStart;
        for (GanttTask a = t; a != null; a = a.parent) {
            List<TaskDependency> in = incoming.get(a.index);
            if (in == null) {
                continue;
            }
            for (TaskDependency d : in) {
                GanttTask p = d.getPredecessor();
                // Without a lag the bound is the predecessor's own instant:
                // adding zero working time would carry the close of one day
                // to the opening of the next, and a milestone belongs to the
                // day it closes.
                long bound = d.fromFinish() ? p.earlyFinish : p.earlyStart;
                if (d.getLag() != 0) {
                    bound = cal.addWorkingTime(bound, d.getLag());
                }
                // A link to a summary holds the start of what is under it.
                if (a == t && !d.constrainsStart()) { //NOPMD CompareObjectsWithEquals
                    bound = cal.subtractWorkingTime(bound, dur);
                }
                s = Math.max(s, bound);
            }
        }
        long date = t.getConstraintDate();
        switch (t.getConstraint()) {
            case START_NO_EARLIER_THAN:
                s = Math.max(s, date);
                break;
            case FINISH_NO_EARLIER_THAN:
                s = Math.max(s, cal.subtractWorkingTime(date, dur));
                break;
            case MUST_START_ON:
                t.constraintViolated = date < s;
                s = date;
                break;
            case MUST_FINISH_ON:
                long must = cal.subtractWorkingTime(date, dur);
                t.constraintViolated = must < s;
                s = must;
                break;
            default:
                break;
        }
        if (t.getLevelingDelay() > 0) {
            s = cal.addWorkingTime(cal.nextWorkingTime(s), t.getLevelingDelay());
        }
        if (dur == 0) {
            // A milestone may sit where a working period closes: the end of
            // Friday, not the start of Monday.
            t.earlyStart = cal.previousWorkingTime(s) == s ? s : cal.nextWorkingTime(s);
            t.earlyFinish = t.earlyStart;
        } else {
            t.earlyStart = cal.nextWorkingTime(s);
            t.earlyFinish = cal.addWorkingTime(t.earlyStart, dur);
        }
        if (t.getConstraint() == GanttTask.Constraint.START_NO_LATER_THAN) {
            t.constraintViolated = t.earlyStart > date;
        } else if (t.getConstraint() == GanttTask.Constraint.FINISH_NO_LATER_THAN) {
            t.constraintViolated = t.earlyFinish > date;
        }
    }

    private static boolean pinned(GanttTask t) {
        return t.isManuallyScheduled() || t.getConstraint() == GanttTask.Constraint.MUST_START_ON
                || t.getConstraint() == GanttTask.Constraint.MUST_FINISH_ON;
    }

    private static void backward(GanttTask t, WorkCalendar cal, List<List<TaskDependency>> outgoing,
                                 long projectFinish, List<GanttTask> leaves) {
        if (pinned(t)) {
            t.lateStart = t.earlyStart;
            t.lateFinish = t.earlyFinish;
            return;
        }
        long dur = t.getDuration();
        long lf = projectFinish;
        for (GanttTask a = t; a != null; a = a.parent) {
            List<TaskDependency> out = outgoing.get(a.index);
            if (out == null) {
                continue;
            }
            for (TaskDependency d : out) {
                // A summary's finish is the latest of its children, so a link
                // from its finish holds each of them. Its start holds none.
                if (a != t && !d.fromFinish()) { //NOPMD CompareObjectsWithEquals
                    continue;
                }
                GanttTask s = d.getSuccessor();
                if (s.isSummary()) {
                    leaves.clear();
                    leavesOf(s, leaves);
                    for (GanttTask leaf : leaves) {
                        lf = Math.min(lf, lateBound(d, leaf.lateStart, cal, dur));
                    }
                } else {
                    lf = Math.min(lf, lateBound(d, d.constrainsStart() ? s.lateStart : s.lateFinish, cal, dur));
                }
            }
        }
        if (t.getConstraint() == GanttTask.Constraint.FINISH_NO_LATER_THAN) {
            lf = Math.min(lf, t.getConstraintDate());
        } else if (t.getConstraint() == GanttTask.Constraint.START_NO_LATER_THAN) {
            lf = Math.min(lf, cal.addWorkingTime(t.getConstraintDate(), dur));
        }
        if (dur == 0) {
            t.lateFinish = lf;
            t.lateStart = lf;
        } else {
            t.lateFinish = cal.previousWorkingTime(lf);
            t.lateStart = cal.subtractWorkingTime(t.lateFinish, dur);
        }
    }

    // The latest finish of a predecessor that lets the successor's end be at
    // `target`.
    private static long lateBound(TaskDependency d, long target, WorkCalendar cal, long dur) {
        long b = d.getLag() == 0 ? target : cal.subtractWorkingTime(target, d.getLag());
        if (!d.fromFinish()) {
            b = cal.addWorkingTime(b, dur);
        }
        return b;
    }

    private static long freeSlack(GanttTask t, WorkCalendar cal, List<List<TaskDependency>> outgoing,
                                  long projectFinish, List<GanttTask> leaves) {
        long best = Long.MAX_VALUE;
        for (GanttTask a = t; a != null; a = a.parent) {
            List<TaskDependency> out = outgoing.get(a.index);
            if (out == null) {
                continue;
            }
            for (TaskDependency d : out) {
                if (a != t && !d.fromFinish()) { //NOPMD CompareObjectsWithEquals
                    continue;
                }
                long mine = d.fromFinish() ? t.earlyFinish : t.earlyStart;
                GanttTask s = d.getSuccessor();
                if (s.isSummary()) {
                    leaves.clear();
                    leavesOf(s, leaves);
                    for (GanttTask leaf : leaves) {
                        long allowed = cal.subtractWorkingTime(leaf.earlyStart, d.getLag());
                        best = Math.min(best, cal.workingTimeBetween(mine, allowed));
                    }
                } else {
                    long allowed = cal.subtractWorkingTime(d.constrainsStart() ? s.earlyStart : s.earlyFinish, d.getLag());
                    best = Math.min(best, cal.workingTimeBetween(mine, allowed));
                }
            }
        }
        if (best == Long.MAX_VALUE) {
            best = cal.workingTimeBetween(t.earlyFinish, projectFinish);
        }
        return Math.max(0, best);
    }

    private static void rollUp(GanttTask t, WorkCalendar cal) {
        long ls = Long.MAX_VALUE;
        long lf = Long.MIN_VALUE;
        long slack = Long.MAX_VALUE;
        long free = Long.MAX_VALUE;
        boolean critical = false;
        boolean violated = false;
        long work = 0;
        long done = 0;
        for (int c = 0; c < t.children.size(); c++) {
            GanttTask child = t.children.get(c);
            ls = Math.min(ls, child.lateStart);
            lf = Math.max(lf, child.lateFinish);
            slack = Math.min(slack, child.totalSlack);
            free = Math.min(free, child.freeSlack);
            critical |= child.critical;
            violated |= child.constraintViolated;
            long d = child.getDuration();
            work += d;
            done += d * child.getPercentComplete();
        }
        t.lateStart = ls;
        t.lateFinish = lf;
        t.totalSlack = slack;
        t.freeSlack = free;
        t.critical = critical;
        t.constraintViolated = violated;
        t.setDates(t.earlyStart, t.earlyFinish);
        t.setDuration(Math.max(0, cal.workingTimeBetween(t.earlyStart, t.earlyFinish)));
        if (work > 0) {
            t.setPercentComplete((int) (done / work));
        }
    }

    /// Adds every task under `t` that has no children of its own to `out`.
    static void leavesOf(GanttTask t, List<GanttTask> out) {
        for (int c = 0; c < t.children.size(); c++) {
            GanttTask child = t.children.get(c);
            if (child.children.isEmpty()) {
                out.add(child);
            } else {
                leavesOf(child, out);
            }
        }
    }

    private static void put(List<List<TaskDependency>> lists, int index, TaskDependency d) {
        List<TaskDependency> l = lists.get(index);
        if (l == null) {
            l = new ArrayList<TaskDependency>(2);
            lists.set(index, l);
        }
        l.add(d);
    }

    private static final class IntList {
        int[] data = new int[32];
        int size;

        void add(int v) {
            if (size == data.length) {
                int[] bigger = new int[size * 2];
                System.arraycopy(data, 0, bigger, 0, size);
                data = bigger;
            }
            data[size++] = v;
        }
    }
}
