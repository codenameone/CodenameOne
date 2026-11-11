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

/// Something work is booked against: a person, a room, a machine, a calendar.
///
/// A `Scheduler` draws one row or one column per resource, and a project
/// assigns tasks to them. `capacity` is how much of it there is at once:
/// `1` is one person full time, `3` a pool of three.
public final class ScheduleResource {
    private final String id;
    private String name;
    private String detail;
    private String groupId;
    private float capacity = 1f;
    private WorkCalendar calendar;
    private int color = -1;
    private Object userObject;

    public ScheduleResource(String id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("id");
        }
        this.id = id;
        this.name = name == null ? "" : name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ScheduleResource setName(String v) {
        name = v == null ? "" : v;
        return this;
    }

    /// A second line under the name: a role, a room's size.
    public String getDetail() {
        return detail;
    }

    public ScheduleResource setDetail(String v) {
        detail = v;
        return this;
    }

    /// The id of the resource this one is grouped under, or null.
    public String getGroupId() {
        return groupId;
    }

    public ScheduleResource setGroupId(String v) {
        groupId = v;
        return this;
    }

    public float getCapacity() {
        return capacity;
    }

    public ScheduleResource setCapacity(float v) {
        if (v <= 0) {
            throw new IllegalArgumentException("capacity");
        }
        capacity = v;
        return this;
    }

    /// The resource's own working hours, or null for the project's.
    public WorkCalendar getCalendar() {
        return calendar;
    }

    public ScheduleResource setCalendar(WorkCalendar v) {
        calendar = v;
        return this;
    }

    /// An RGB colour for the resource's items, or -1 for the theme's.
    public int getColor() {
        return color;
    }

    public ScheduleResource setColor(int v) {
        color = v;
        return this;
    }

    public Object getUserObject() {
        return userObject;
    }

    public ScheduleResource setUserObject(Object v) {
        userObject = v;
        return this;
    }

    @Override
    public String toString() {
        return name;
    }
}
