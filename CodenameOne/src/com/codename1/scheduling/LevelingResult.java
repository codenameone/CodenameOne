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

/// What `ResourceLeveler.level` proposes: a delay for some tasks, and what
/// the project would look like with them.
///
/// Nothing has changed in the project until `apply` is called, so the
/// proposal can be shown and turned down.
public final class LevelingResult {
    private final ProjectModel model;
    private final List<GanttTask> tasks = new ArrayList<GanttTask>();
    private final List<Long> delays = new ArrayList<Long>();
    private final List<GanttTask> delayed = new ArrayList<GanttTask>();
    private final List<ResourceOverallocation> unresolved;
    private final long finishBefore;
    private final long finishAfter;

    LevelingResult(ProjectModel model, List<GanttTask> all, List<ResourceOverallocation> unresolved,
                   long finishBefore, long finishAfter, long[] before) {
        this.model = model;
        this.unresolved = unresolved;
        this.finishBefore = finishBefore;
        this.finishAfter = finishAfter;
        for (int i = 0; i < all.size(); i++) {
            GanttTask t = all.get(i);
            tasks.add(t);
            delays.add(Long.valueOf(t.getLevelingDelay()));
            if (t.getLevelingDelay() != before[i]) {
                delayed.add(t);
            }
        }
    }

    /// The tasks whose delay the proposal changes.
    public List<GanttTask> getChangedTasks() {
        return Collections.unmodifiableList(delayed);
    }

    /// Whether applying the proposal would change anything.
    public boolean isChanged() {
        return !delayed.isEmpty();
    }

    /// The leveling delay the proposal gives a task, in working milliseconds.
    public long getDelay(GanttTask task) {
        int i = tasks.indexOf(task);
        return i < 0 ? 0 : delays.get(i).longValue();
    }

    /// What could not be solved by delaying: a task that alone needs more of
    /// a resource than exists, or tasks that may not be moved.
    public List<ResourceOverallocation> getUnresolved() {
        return Collections.unmodifiableList(unresolved);
    }

    /// The project's finish as it is now.
    public long getFinishBefore() {
        return finishBefore;
    }

    /// The project's finish with the proposal applied.
    public long getFinishAfter() {
        return finishAfter;
    }

    /// Writes the delays into the project and schedules it.
    ///
    /// The proposal describes the project as it was when it was made. If
    /// tasks were added, removed or changed since, the delays are still
    /// written for the tasks that remain and every other task gets none, but
    /// `getFinishAfter` and `getUnresolved` no longer say what comes out:
    /// level again to know.
    public void apply() {
        model.clearLeveling();
        for (int i = 0; i < tasks.size(); i++) {
            GanttTask t = tasks.get(i);
            if (model.contains(t)) {
                t.setLevelingDelay(delays.get(i).longValue());
            }
        }
        model.update();
    }
}
