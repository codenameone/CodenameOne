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
import java.util.Arrays;
import java.util.List;

/// Finds where a project asks more of a resource than there is, and proposes
/// delays that take the excess away.
///
/// Leveling never shortens or splits a task and never changes a link. It
/// takes the earliest moment a resource is overbooked, keeps the tasks that
/// can least afford to wait and holds one of the others back until a place is
/// free: the one with the most slack first, so the project's finish moves
/// only when no slack is left, then the one with the lowest priority. A task
/// that is scheduled manually, has a "must" constraint or has priority 1000
/// is never moved.
///
/// This is a heuristic, as it is in every planning tool: the best order is
/// not something that can be computed for a project of any size.
public final class ResourceLeveler {
    private static final float EPSILON = 0.0001f;

    private ResourceLeveler() {
    }

    /// Every stretch in which a resource is overbooked, ordered by resource
    /// and then by time.
    public static List<ResourceOverallocation> findOverallocations(ProjectModel model) {
        List<ResourceOverallocation> out = new ArrayList<ResourceOverallocation>();
        List<ScheduleResource> resources = model.getResources();
        List<GanttTask> leaves = leaves(model);
        for (int r = 0; r < resources.size(); r++) {
            scan(resources.get(r), leaves, out, null);
        }
        return out;
    }

    /// How much of a resource is asked for over time: the most units in use
    /// at once in each of the `buckets` equal slices of `[from, to)`. Divide
    /// by the resource's capacity for a load.
    public static float[] loadProfile(ProjectModel model, String resourceId, long from, long to, int buckets) {
        float[] out = new float[Math.max(0, buckets)];
        if (buckets <= 0 || to <= from) {
            return out;
        }
        List<GanttTask> leaves = leaves(model);
        double width = (to - from) / (double) buckets;
        for (int i = 0; i < leaves.size(); i++) {
            GanttTask t = leaves.get(i);
            float units = t.getUnits(resourceId);
            if (units <= 0 || t.getEnd() <= from || t.getStart() >= to || t.getEnd() <= t.getStart()) {
                continue;
            }
            // Tasks that overlap in a bucket add up only where they overlap in
            // time, so the peak is taken per boundary inside the bucket below.
            int first = (int) Math.max(0, Math.floor((t.getStart() - from) / width));
            int last = (int) Math.min(buckets - 1, Math.floor((t.getEnd() - 1 - from) / width));
            for (int b = first; b <= last; b++) {
                out[b] = -1f;
            }
        }
        for (int b = 0; b < buckets; b++) {
            if (out[b] < 0) {
                long bs = from + (long) (b * width);
                long be = from + (long) ((b + 1) * width);
                out[b] = peak(leaves, resourceId, bs, be);
            }
        }
        return out;
    }

    private static float peak(List<GanttTask> leaves, String resourceId, long from, long to) {
        float best = 0;
        // The sum only changes where a task starts, so those are the only
        // instants worth measuring, with the start of the range itself.
        for (int i = -1; i < leaves.size(); i++) {
            long at = from;
            if (i >= 0) {
                GanttTask t = leaves.get(i);
                if (t.getUnits(resourceId) <= 0 || t.getStart() <= from || t.getStart() >= to) {
                    continue;
                }
                at = t.getStart();
            }
            float sum = 0;
            for (int j = 0; j < leaves.size(); j++) {
                GanttTask o = leaves.get(j);
                if (o.getStart() <= at && o.getEnd() > at) {
                    sum += o.getUnits(resourceId);
                }
            }
            best = Math.max(best, sum);
        }
        return best;
    }

    /// Proposes leveling delays for the whole project, starting from none.
    /// The project is left exactly as it was; see `LevelingResult.apply`.
    public static LevelingResult level(ProjectModel model) {
        List<GanttTask> all = model.getTasks();
        int n = all.size();
        long[] before = new long[n];
        for (int i = 0; i < n; i++) {
            before[i] = all.get(i).getLevelingDelay();
        }
        ProjectScheduler.schedule(model);
        long finishBefore = model.getFinish();
        for (int i = 0; i < n; i++) {
            all.get(i).setLevelingDelay(0);
        }
        ProjectScheduler.schedule(model);

        WorkCalendar cal = model.getCalendar();
        List<GanttTask> leaves = leaves(model);
        List<ScheduleResource> resources = model.getResources();
        List<GanttTask> stuck = new ArrayList<GanttTask>();
        List<ResourceOverallocation> found = new ArrayList<ResourceOverallocation>();
        List<GanttTask> conflict = new ArrayList<GanttTask>();
        // Every step moves one task later, so this ends by itself; the limit
        // is for a calendar or a set of constraints that makes it crawl.
        int limit = 200 + leaves.size() * leaves.size() * 4;
        for (int step = 0; step < limit; step++) {
            ResourceOverallocation first = null;
            for (int r = 0; r < resources.size(); r++) {
                found.clear();
                scan(resources.get(r), leaves, found, stuck);
                if (!found.isEmpty() && (first == null || found.get(0).getStart() < first.getStart())) {
                    first = found.get(0);
                }
            }
            if (first == null) {
                break;
            }
            conflict.clear();
            for (int i = 0; i < leaves.size(); i++) {
                GanttTask t = leaves.get(i);
                if (t.getUnits(first.getResourceId()) > 0 && t.getStart() <= first.getStart() && t.getEnd() > first.getStart()) {
                    conflict.add(t);
                }
            }
            GanttTask victim = conflict.size() < 2 ? null : pick(conflict, stuck);
            if (victim == null) {
                addAllOnce(stuck, conflict);
                continue;
            }
            long until = Long.MAX_VALUE;
            for (int i = 0; i < conflict.size(); i++) {
                GanttTask o = conflict.get(i);
                if (o != victim) { //NOPMD CompareObjectsWithEquals
                    until = Math.min(until, o.getEnd());
                }
            }
            long add = cal.workingTimeBetween(victim.getStart(), cal.nextWorkingTime(until));
            if (add <= 0) {
                stuck.add(victim);
                continue;
            }
            victim.setLevelingDelay(victim.getLevelingDelay() + add);
            ProjectScheduler.schedule(model);
        }
        List<ResourceOverallocation> unresolved = findOverallocations(model);
        LevelingResult result = new LevelingResult(model, all, unresolved, finishBefore, model.getFinish(), before);
        for (int i = 0; i < n; i++) {
            all.get(i).setLevelingDelay(before[i]);
        }
        ProjectScheduler.schedule(model);
        return result;
    }

    // The task to hold back: most slack, then lowest priority, then the one
    // that starts last, then the one furthest down the outline.
    private static GanttTask pick(List<GanttTask> conflict, List<GanttTask> stuck) {
        GanttTask best = null;
        for (int i = 0; i < conflict.size(); i++) {
            GanttTask t = conflict.get(i);
            if (t.isManuallyScheduled() || t.getPriority() >= 1000 || stuck.contains(t)
                    || t.getConstraint() == GanttTask.Constraint.MUST_START_ON
                    || t.getConstraint() == GanttTask.Constraint.MUST_FINISH_ON) {
                continue;
            }
            if (best == null || later(t, best)) {
                best = t;
            }
        }
        return best;
    }

    private static boolean later(GanttTask a, GanttTask b) {
        long sa = Math.max(0, a.getTotalSlack());
        long sb = Math.max(0, b.getTotalSlack());
        if (sa != sb) {
            return sa > sb;
        }
        if (a.getPriority() != b.getPriority()) {
            return a.getPriority() < b.getPriority();
        }
        if (a.getStart() != b.getStart()) {
            return a.getStart() > b.getStart();
        }
        return a.index > b.index;
    }

    private static void addAllOnce(List<GanttTask> into, List<GanttTask> from) {
        for (int i = 0; i < from.size(); i++) {
            if (!into.contains(from.get(i))) {
                into.add(from.get(i));
            }
        }
    }

    private static List<GanttTask> leaves(ProjectModel model) {
        List<GanttTask> all = model.getTasks();
        List<GanttTask> out = new ArrayList<GanttTask>();
        for (int i = 0; i < all.size(); i++) {
            GanttTask t = all.get(i);
            if (!t.isSummary() && !t.getAssignments().isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    // Adds the overbooked stretches of one resource to `out`, in time order.
    // A stretch made only of tasks in `ignore` is left out.
    private static void scan(ScheduleResource res, List<GanttTask> leaves, List<ResourceOverallocation> out,
                             List<GanttTask> ignore) {
        String id = res.getId();
        int count = 0;
        for (int i = 0; i < leaves.size(); i++) {
            GanttTask t = leaves.get(i);
            if (t.getUnits(id) > 0 && t.getEnd() > t.getStart()) {
                count++;
            }
        }
        if (count == 0) {
            return;
        }
        long[] bounds = new long[count * 2];
        int b = 0;
        for (int i = 0; i < leaves.size(); i++) {
            GanttTask t = leaves.get(i);
            if (t.getUnits(id) > 0 && t.getEnd() > t.getStart()) {
                bounds[b++] = t.getStart();
                bounds[b++] = t.getEnd();
            }
        }
        Arrays.sort(bounds);
        long runStart = 0;
        long runEnd = 0;
        float runPeak = 0;
        boolean open = false;
        for (int s = 0; s + 1 < bounds.length; s++) {
            long from = bounds[s];
            long to = bounds[s + 1];
            if (to == from) {
                continue;
            }
            float sum = 0;
            boolean movable = false;
            for (int i = 0; i < leaves.size(); i++) {
                GanttTask t = leaves.get(i);
                float u = t.getUnits(id);
                if (u > 0 && t.getStart() <= from && t.getEnd() >= to && t.getEnd() > t.getStart()) {
                    sum += u;
                    movable |= ignore == null || !ignore.contains(t);
                }
            }
            boolean over = sum > res.getCapacity() + EPSILON && movable;
            if (over && open && from == runEnd) {
                runEnd = to;
                runPeak = Math.max(runPeak, sum);
            } else {
                if (open) {
                    out.add(new ResourceOverallocation(id, runStart, runEnd, runPeak, res.getCapacity()));
                }
                open = over;
                runStart = from;
                runEnd = to;
                runPeak = sum;
            }
        }
        if (open) {
            out.add(new ResourceOverallocation(id, runStart, runEnd, runPeak, res.getCapacity()));
        }
    }
}
