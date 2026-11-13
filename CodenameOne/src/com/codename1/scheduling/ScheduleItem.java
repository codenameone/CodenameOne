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

/// One thing on a schedule: an appointment, a shift, a block of busy time.
///
/// `start` and `end` are epoch milliseconds and `end` is exclusive. An all-day
/// item runs from the start of its first day to the start of the day after its
/// last, in the zone of the view that shows it.
public final class ScheduleItem {
    /// How an item is drawn, apart from its colour.
    public enum Kind {
        /// An item of its own: a block with a title that can be selected.
        EVENT,
        /// A band under the events, such as the hours somebody is available.
        /// It cannot be selected or moved.
        BACKGROUND,
        /// Time that is taken but is nobody's to edit here, such as busy time
        /// read from another calendar. Drawn hatched.
        BLOCKED
    }

    private final String id;
    private String title = "";
    private String detail;
    private long start;
    private long end;
    private boolean allDay;
    private String resourceId;
    private Kind kind = Kind.EVENT;
    private String uiid;
    private int color = -1;
    private boolean movable = true;
    private boolean resizable = true;
    private boolean recurring;
    private Object userObject;

    public ScheduleItem(String id, String title, long start, long end) {
        if (id == null) {
            throw new IllegalArgumentException("id");
        }
        this.id = id;
        setTitle(title);
        setTime(start, end);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public ScheduleItem setTitle(String v) {
        title = v == null ? "" : v;
        return this;
    }

    /// A second line: a place, a client's name.
    public String getDetail() {
        return detail;
    }

    public ScheduleItem setDetail(String v) {
        detail = v;
        return this;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public ScheduleItem setTime(long start, long end) {
        if (end < start) {
            throw new IllegalArgumentException("end before start");
        }
        this.start = start;
        this.end = end;
        return this;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public ScheduleItem setAllDay(boolean v) {
        allDay = v;
        return this;
    }

    /// The resource the item belongs to, or null when the schedule has none.
    public String getResourceId() {
        return resourceId;
    }

    public ScheduleItem setResourceId(String v) {
        resourceId = v;
        return this;
    }

    public Kind getKind() {
        return kind;
    }

    public ScheduleItem setKind(Kind v) {
        kind = v == null ? Kind.EVENT : v;
        return this;
    }

    /// The UIID the item is drawn with in place of `SchedulerItem`, or null.
    /// Its background, foreground and font are used, and its selected style
    /// while the item is selected.
    public String getUIID() {
        return uiid;
    }

    public ScheduleItem setUIID(String v) {
        uiid = v;
        return this;
    }

    /// An RGB colour, or -1 for the resource's or the theme's.
    public int getColor() {
        return color;
    }

    public ScheduleItem setColor(int v) {
        color = v;
        return this;
    }

    public boolean isMovable() {
        return movable && kind == Kind.EVENT;
    }

    public ScheduleItem setMovable(boolean v) {
        movable = v;
        return this;
    }

    public boolean isResizable() {
        return resizable && kind == Kind.EVENT && !allDay;
    }

    public ScheduleItem setResizable(boolean v) {
        resizable = v;
        return this;
    }

    /// Whether the item is one occurrence of a series, which a view marks.
    public boolean isRecurring() {
        return recurring;
    }

    public ScheduleItem setRecurring(boolean v) {
        recurring = v;
        return this;
    }

    public Object getUserObject() {
        return userObject;
    }

    public ScheduleItem setUserObject(Object v) {
        userObject = v;
        return this;
    }

    /// Whether any of the item is inside `[from, to)`.
    public boolean intersects(long from, long to) {
        return start < to && (end > from || (end == start && start >= from));
    }

    @Override
    public String toString() {
        return title;
    }
}
