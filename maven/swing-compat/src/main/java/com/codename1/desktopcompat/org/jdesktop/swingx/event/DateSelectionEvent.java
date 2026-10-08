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
package com.codename1.desktopcompat.org.jdesktop.swingx.event;

import com.codename1.desktopcompat.org.jdesktop.swingx.calendar.DateSelectionModel;
import java.util.Date;
import java.util.EventObject;
import java.util.SortedSet;
import java.util.TreeSet;

/// A change of a date selection model: of the selected dates, of what can
/// be selected, or of the calendar the model reads dates with.
public class DateSelectionEvent extends EventObject {

    /// What changed.
    public enum EventType {
        DATES_ADDED,
        DATES_REMOVED,
        DATES_SET,
        SELECTION_CLEARED,
        SELECTABLE_DATES_CHANGED,
        SELECTABLE_RANGE_CHANGED,
        UNSELECTED_DATES_CHANGED,
        LOWER_BOUND_CHANGED,
        UPPER_BOUND_CHANGED,
        ADJUSTING_STARTED,
        ADJUSTING_STOPPED,
        CALENDAR_CHANGED
    }

    private final EventType eventType;
    private final boolean adjusting;

    /// `source` is the model that changed.
    public DateSelectionEvent(Object source, EventType eventType, boolean adjusting) {
        super(source);
        this.eventType = eventType;
        this.adjusting = adjusting;
    }

    /// The selection of the source model as it is now; empty when the
    /// source is not a selection model.
    public SortedSet<Date> getSelection() {
        Object src = getSource();
        if (src instanceof DateSelectionModel) {
            return ((DateSelectionModel) src).getSelection();
        }
        return new TreeSet<Date>();
    }

    public final EventType getEventType() {
        return eventType;
    }

    public boolean isAdjusting() {
        return adjusting;
    }

    @Override
    public String toString() {
        return "DateSelectionEvent[type=" + eventType + ",adjusting=" + adjusting + "]";
    }
}
