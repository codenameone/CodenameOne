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
/// Schedules and projects as data: what a `Scheduler` or a `Gantt` shows,
/// and the engine that works out a project's dates.
///
/// Nothing here draws. The components are in `com.codename1.ui.scheduler`,
/// and everything in this package runs without one, on a server too.
///
/// - `ScheduleModel` is items in time, optionally per `ScheduleResource`.
///   `DefaultScheduleModel` keeps them in memory, and `CalendarScheduleModel`
///   reads and writes a `com.codename1.calendar.CalendarSource`.
/// - `ProjectModel` is a tree of `GanttTask` with `TaskDependency` links.
///   `ProjectScheduler` gives every task its dates and its slack, in the
///   working time a `WorkCalendar` describes, and marks the critical path.
/// - `ResourceLeveler` finds resources that are overbooked and proposes
///   delays that resolve it.
///
/// Every instant is epoch milliseconds, and every duration is milliseconds of
/// working time. The classes are not thread safe: use a model from one
/// thread, which for a model shown by a component is the EDT.
package com.codename1.scheduling;
