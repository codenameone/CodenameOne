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

/// The listener list and the refusals every read-only model shares.
///
/// A subclass has to supply `getResources` and `getItems`. Every edit is
/// refused until the matching method is overridden.
public abstract class AbstractScheduleModel implements ScheduleModel {
    private final List<ScheduleChangeListener> listeners = new ArrayList<ScheduleChangeListener>();

    @Override
    public void setVisibleRange(long from, long to) {
    }

    @Override
    public boolean updateItem(ScheduleItem item, long start, long end, String resourceId) {
        return false;
    }

    @Override
    public ScheduleItem createItem(long start, long end, String resourceId) {
        return null;
    }

    @Override
    public boolean removeItem(ScheduleItem item) {
        return false;
    }

    @Override
    public void addChangeListener(ScheduleChangeListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    @Override
    public void removeChangeListener(ScheduleChangeListener l) {
        listeners.remove(l);
    }

    /// Tells every listener the model changed.
    protected void fireChanged() {
        // A listener may remove itself while it is told.
        for (int i = listeners.size() - 1; i >= 0; i--) {
            if (i < listeners.size()) {
                listeners.get(i).scheduleChanged(this);
            }
        }
    }
}
