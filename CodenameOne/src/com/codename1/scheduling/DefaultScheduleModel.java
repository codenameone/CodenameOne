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

/// A schedule held in memory, which accepts every edit.
///
/// Override `updateItem`, `createItem` or `removeItem` to validate a change
/// or to send it somewhere before it is accepted.
public class DefaultScheduleModel extends AbstractScheduleModel {
    private final List<ScheduleResource> resources = new ArrayList<ScheduleResource>();
    private final List<ScheduleItem> items = new ArrayList<ScheduleItem>();
    private long defaultDuration = TimeMath.HOUR;
    private int created;
    private boolean creationEnabled = true;

    public DefaultScheduleModel addResource(ScheduleResource r) {
        if (r != null) {
            resources.add(r);
            fireChanged();
        }
        return this;
    }

    public DefaultScheduleModel removeResource(ScheduleResource r) {
        if (resources.remove(r)) {
            fireChanged();
        }
        return this;
    }

    public DefaultScheduleModel addItem(ScheduleItem item) {
        if (item != null) {
            items.add(item);
            fireChanged();
        }
        return this;
    }

    /// Adds several items and fires one change.
    public DefaultScheduleModel addItems(List<ScheduleItem> all) {
        items.addAll(all);
        fireChanged();
        return this;
    }

    /// Removes every item and keeps the resources.
    public DefaultScheduleModel clearItems() {
        items.clear();
        fireChanged();
        return this;
    }

    public List<ScheduleItem> getAllItems() {
        return Collections.unmodifiableList(items);
    }

    public ScheduleItem getItem(String id) {
        for (ScheduleItem item : items) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    /// Whether dragging on empty space makes an item. On by default.
    public DefaultScheduleModel setCreationEnabled(boolean v) {
        creationEnabled = v;
        return this;
    }

    public boolean isCreationEnabled() {
        return creationEnabled;
    }

    /// The length of an item created by a tap, where there is no range.
    public DefaultScheduleModel setDefaultDuration(long millis) {
        defaultDuration = millis;
        return this;
    }

    @Override
    public List<ScheduleResource> getResources() {
        return Collections.unmodifiableList(resources);
    }

    @Override
    public void getItems(long from, long to, List<ScheduleItem> out) {
        for (ScheduleItem it : items) {
            if (it.intersects(from, to)) {
                out.add(it);
            }
        }
    }

    @Override
    public boolean updateItem(ScheduleItem item, long start, long end, String resourceId) {
        item.setTime(start, end);
        item.setResourceId(resourceId);
        fireChanged();
        return true;
    }

    @Override
    public ScheduleItem createItem(long start, long end, String resourceId) {
        if (!creationEnabled) {
            return null;
        }
        if (end <= start) {
            end = start + defaultDuration;
        }
        // The application chooses its own ids and may have chosen this one.
        String id;
        do {
            created++;
            id = "new-" + created;
        } while (getItem(id) != null);
        ScheduleItem item = new ScheduleItem(id, "", start, end).setResourceId(resourceId);
        items.add(item);
        fireChanged();
        return item;
    }

    @Override
    public boolean removeItem(ScheduleItem item) {
        boolean removed = items.remove(item);
        if (removed) {
            fireChanged();
        }
        return removed;
    }
}
