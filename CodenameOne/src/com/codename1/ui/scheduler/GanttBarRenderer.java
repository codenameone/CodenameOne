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
import com.codename1.ui.Graphics;

/// Paints one task of a `Gantt` chart: a bar, a summary bracket or a
/// milestone. `DefaultGanttBarRenderer` is the one a chart starts with.
public interface GanttBarRenderer {
    /// The task is the selected one.
    int SELECTED = 1;
    /// The task is on the critical path, and the chart is showing it.
    int CRITICAL = 2;
    /// The task is being dragged, and this is the copy under the pointer.
    int DRAGGED = 4;

    /// Paints `task` into a rectangle: the span of its dates across and the
    /// height of a bar. For a milestone the rectangle is the square its mark
    /// is drawn in.
    ///
    /// #### Parameters
    ///
    /// - `state`: a combination of the constants of this interface
    void paintBar(Graphics g, Gantt gantt, GanttTask task, int x, int y, int width, int height, int state);
}
