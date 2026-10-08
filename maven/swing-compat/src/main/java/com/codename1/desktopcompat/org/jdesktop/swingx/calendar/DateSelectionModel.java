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

import com.codename1.desktopcompat.org.jdesktop.swingx.event.DateSelectionListener;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TimeZone;

/// The selected dates of a month view, the dates that cannot be selected
/// and the calendar both are read with.
public interface DateSelectionModel {

    /// How many dates can be selected at once.
    enum SelectionMode {
        /// One date.
        SINGLE_SELECTION,
        /// One run of consecutive dates.
        SINGLE_INTERVAL_SELECTION,
        /// Any dates.
        MULTIPLE_INTERVAL_SELECTION
    }

    SelectionMode getSelectionMode();

    void setSelectionMode(SelectionMode mode);

    /// A calendar set to the model's time zone. It is a new object on
    /// every call and changing it does not change the model.
    Calendar getCalendar();

    int getFirstDayOfWeek();

    void setFirstDayOfWeek(int firstDayOfWeek);

    int getMinimalDaysInFirstWeek();

    void setMinimalDaysInFirstWeek(int minimalDays);

    TimeZone getTimeZone();

    void setTimeZone(TimeZone timeZone);

    Locale getLocale();

    void setLocale(Locale locale);

    void addSelectionInterval(Date startDate, Date endDate);

    void setSelectionInterval(Date startDate, Date endDate);

    void removeSelectionInterval(Date startDate, Date endDate);

    void clearSelection();

    SortedSet<Date> getSelection();

    Date getFirstSelectionDate();

    Date getLastSelectionDate();

    boolean isSelected(Date date);

    /// The date as the model would store it.
    Date getNormalizedDate(Date date);

    boolean isSelectionEmpty();

    SortedSet<Date> getUnselectableDates();

    void setUnselectableDates(SortedSet<Date> unselectableDates);

    boolean isUnselectableDate(Date unselectableDate);

    Date getUpperBound();

    void setUpperBound(Date upperBound);

    Date getLowerBound();

    void setLowerBound(Date lowerBound);

    void setAdjusting(boolean adjusting);

    boolean isAdjusting();

    void addDateSelectionListener(DateSelectionListener listener);

    void removeDateSelectionListener(DateSelectionListener listener);
}
