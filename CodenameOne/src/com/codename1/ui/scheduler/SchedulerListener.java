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

import com.codename1.scheduling.ScheduleItem;

/// What a `Scheduler` tells an application about. Extend `SchedulerAdapter`
/// to implement only what is needed.
public interface SchedulerListener {
    /// The selection changed. `item` is null when nothing is selected now.
    void itemSelected(Scheduler scheduler, ScheduleItem item);

    /// An item was opened: tapped on a touch screen, double clicked with a
    /// mouse. This is where an application shows the item's details.
    void itemOpened(Scheduler scheduler, ScheduleItem item);

    /// A stretch of empty time was chosen, by a long press, a drag or a double
    /// click. `resourceId` is null in a view with no resources. The model is
    /// asked to create an item after this returns, so an application that
    /// shows its own editor leaves creation off in the model.
    void rangeSelected(Scheduler scheduler, long start, long end, String resourceId);

    /// An item was dragged or resized and is about to change.
    ///
    /// #### Returns
    ///
    /// false to refuse, which leaves the item where it was
    boolean itemChanging(Scheduler scheduler, ScheduleItem item, long start, long end, String resourceId);

    /// The view moved to another stretch of time, `start` inclusive to `end`
    /// exclusive.
    void visibleRangeChanged(Scheduler scheduler, long start, long end);
}
