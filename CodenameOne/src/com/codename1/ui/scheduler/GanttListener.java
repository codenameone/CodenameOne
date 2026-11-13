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

import com.codename1.scheduling.GanttTask;

/// What a `Gantt` tells an application about. Extend `GanttAdapter` to
/// implement only what is needed.
public interface GanttListener {
    /// The selection changed. `task` is null when nothing is selected now.
    void taskSelected(Gantt gantt, GanttTask task);

    /// A task was double tapped, in the table or on the chart.
    void taskOpened(Gantt gantt, GanttTask task);

    /// A task was dragged or resized and is about to be given new dates, or
    /// its progress was dragged, which offers the dates it already has. The
    /// project is scheduled again afterwards, so the dates it ends up with
    /// are these only where its links and its calendar allow them.
    ///
    /// #### Returns
    ///
    /// false to refuse, which leaves the task as it was
    boolean taskChanging(Gantt gantt, GanttTask task, long start, long end);

    /// A task was changed by the user and the project has been scheduled
    /// again: moved, resized, or given a new progress.
    void taskChanged(Gantt gantt, GanttTask task);

    /// A link was dragged from one task to another and is about to be made,
    /// finish to start.
    ///
    /// #### Returns
    ///
    /// false to refuse
    boolean linkCreating(Gantt gantt, GanttTask predecessor, GanttTask successor);
}
