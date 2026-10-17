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

import com.codename1.desktopcompat.org.jdesktop.swingx.calendar.DateSelectionModel.SelectionMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/// The storage the concrete selection models share: the selected dates,
/// the unselectable dates and the selection mode, with the interval
/// operations that honor the mode. A model normalizes the dates before it
/// hands them over; an interval is walked a day at a time from its start.
final class DateSet {

    /// A hard stop for the walk over an interval, about 27 years, so that
    /// a mistaken end date cannot hang the device.
    private static final int MAX_DAYS = 10000;

    private final TreeSet<Date> selected = new TreeSet<Date>();
    private final TreeSet<Date> unselectable = new TreeSet<Date>();
    SelectionMode mode = SelectionMode.SINGLE_SELECTION;

    SortedSet<Date> selection() {
        return copy(selected);
    }

    SortedSet<Date> unselectable() {
        return copy(unselectable);
    }

    private static TreeSet<Date> copy(TreeSet<Date> from) {
        TreeSet<Date> out = new TreeSet<Date>();
        Iterator<Date> it = from.iterator();
        while (it.hasNext()) {
            out.add(new Date(it.next().getTime()));
        }
        return out;
    }

    boolean isEmpty() {
        return selected.isEmpty();
    }

    Date first() {
        return selected.isEmpty() ? null : new Date(selected.first().getTime());
    }

    Date last() {
        return selected.isEmpty() ? null : new Date(selected.last().getTime());
    }

    boolean contains(Date d) {
        return selected.contains(d);
    }

    boolean isUnselectable(Date d, Date lower, Date upper) {
        if (upper != null && upper.getTime() < d.getTime()) {
            return true;
        }
        if (lower != null && lower.getTime() > d.getTime()) {
            return true;
        }
        return unselectable.contains(d);
    }

    /// Replaces the unselectable dates and drops them from the selection.
    void setUnselectable(ArrayList<Date> dates) {
        unselectable.clear();
        for (int i = 0; i < dates.size(); i++) {
            unselectable.add(dates.get(i));
            selected.remove(dates.get(i));
        }
    }

    /// Empties the selection; answers whether it held anything.
    boolean clear() {
        boolean had = !selected.isEmpty();
        selected.clear();
        return had;
    }

    /// The days from `start` to `end` that can be selected, each a day
    /// after the one before.
    private static ArrayList<Date> days(Calendar cal, Date start, Date end, DateSet set, Date lower, Date upper) {
        ArrayList<Date> out = new ArrayList<Date>();
        cal.setTime(start);
        Date d = start;
        for (int i = 0; i < MAX_DAYS && d.getTime() <= end.getTime(); i++) {
            if (!set.isUnselectable(d, lower, upper)) {
                out.add(d);
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
            d = cal.getTime();
        }
        return out;
    }

    /// Makes the interval the selection; answers whether anything changed.
    boolean set(Calendar cal, Date start, Date end, Date lower, Date upper) {
        if (start.getTime() > end.getTime()) {
            return false;
        }
        Date last = mode == SelectionMode.SINGLE_SELECTION ? start : end;
        ArrayList<Date> days = days(cal, start, last, this, lower, upper);
        if (days.size() == selected.size()) {
            boolean same = true;
            for (int i = 0; i < days.size() && same; i++) {
                same = selected.contains(days.get(i));
            }
            if (same) {
                return false;
            }
        }
        selected.clear();
        selected.addAll(days);
        return true;
    }

    /// Adds the interval, or replaces the selection with it in the modes
    /// that hold one date or one interval; answers whether anything
    /// changed.
    boolean add(Calendar cal, Date start, Date end, Date lower, Date upper) {
        if (start.getTime() > end.getTime()) {
            return false;
        }
        if (mode != SelectionMode.MULTIPLE_INTERVAL_SELECTION) {
            return set(cal, start, end, lower, upper);
        }
        ArrayList<Date> days = days(cal, start, end, this, lower, upper);
        boolean changed = false;
        for (int i = 0; i < days.size(); i++) {
            changed |= selected.add(days.get(i));
        }
        return changed;
    }

    /// Removes the selected dates from `start` to `end`, both included;
    /// answers whether there were any.
    boolean remove(Date start, Date end) {
        if (start.getTime() > end.getTime()) {
            return false;
        }
        ArrayList<Date> gone = new ArrayList<Date>();
        Iterator<Date> it = selected.iterator();
        while (it.hasNext()) {
            Date d = it.next();
            if (d.getTime() >= start.getTime() && d.getTime() <= end.getTime()) {
                gone.add(d);
            }
        }
        for (int i = 0; i < gone.size(); i++) {
            selected.remove(gone.get(i));
        }
        return !gone.isEmpty();
    }
}
