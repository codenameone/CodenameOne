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
package com.codename1.desktopcompat.org.jdesktop.swingx.calendar;

import com.codename1.desktopcompat.org.jdesktop.swingx.event.DateSelectionEvent.EventType;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.SortedSet;

/// A selection of instants: dates are stored as they are handed in, time
/// of day included, and an interval holds its start and every instant a
/// whole number of days after it up to its end. Use [DaySelectionModel]
/// to select days whatever their time.
///
/// A date beyond the bounds or among the unselectable dates is left out
/// of an interval that covers it.
public class DefaultDateSelectionModel extends AbstractDateSelectionModel {

    private final DateSet dates = new DateSet();

    public DefaultDateSelectionModel() {
        this(null);
    }

    public DefaultDateSelectionModel(Locale locale) {
        super(locale);
    }

    @Override
    public SelectionMode getSelectionMode() {
        return dates.mode;
    }

    @Override
    public void setSelectionMode(SelectionMode selectionMode) {
        if (selectionMode == null) {
            throw new NullPointerException("selectionMode must not be null");
        }
        dates.mode = selectionMode;
        clearSelection();
    }

    @Override
    public void addSelectionInterval(Date startDate, Date endDate) {
        if (dates.add(calendar, getNormalizedDate(startDate), getNormalizedDate(endDate), lowerBound, upperBound)) {
            fireValueChanged(EventType.DATES_ADDED);
        }
    }

    @Override
    public void setSelectionInterval(Date startDate, Date endDate) {
        if (dates.set(calendar, getNormalizedDate(startDate), getNormalizedDate(endDate), lowerBound, upperBound)) {
            fireValueChanged(EventType.DATES_SET);
        }
    }

    @Override
    public void removeSelectionInterval(Date startDate, Date endDate) {
        if (dates.remove(startDate, endDate)) {
            fireValueChanged(EventType.DATES_REMOVED);
        }
    }

    @Override
    public void clearSelection() {
        if (dates.clear()) {
            fireValueChanged(EventType.SELECTION_CLEARED);
        }
    }

    @Override
    public SortedSet<Date> getSelection() {
        return dates.selection();
    }

    @Override
    public Date getFirstSelectionDate() {
        return dates.first();
    }

    @Override
    public Date getLastSelectionDate() {
        return dates.last();
    }

    @Override
    public boolean isSelected(Date date) {
        if (date == null) {
            throw new NullPointerException("date must not be null");
        }
        return dates.contains(date);
    }

    /// A copy of `date`: this model stores instants unchanged.
    @Override
    public Date getNormalizedDate(Date date) {
        return new Date(date.getTime());
    }

    @Override
    public boolean isSelectionEmpty() {
        return dates.isEmpty();
    }

    @Override
    public SortedSet<Date> getUnselectableDates() {
        return dates.unselectable();
    }

    @Override
    public void setUnselectableDates(SortedSet<Date> unselectables) {
        ArrayList<Date> all = new ArrayList<Date>();
        if (unselectables != null) {
            Iterator<Date> it = unselectables.iterator();
            while (it.hasNext()) {
                all.add(getNormalizedDate(it.next()));
            }
        }
        dates.setUnselectable(all);
        fireValueChanged(EventType.UNSELECTED_DATES_CHANGED);
    }

    @Override
    public boolean isUnselectableDate(Date date) {
        return dates.isUnselectable(date, lowerBound, upperBound);
    }
}
