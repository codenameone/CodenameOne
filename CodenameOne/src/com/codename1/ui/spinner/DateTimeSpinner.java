/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui.spinner;

import java.util.Date;

/// The date and time spinner extends the time spinner by allowing to pick a specific day as well
///
/// @author Shai Almog
///
/// #### Deprecated
///
/// use Picker instead
public class DateTimeSpinner extends TimeSpinner {
    private final Date today = new Date();
    private final int off;
    private Spinner date;
    private Date currentDate = today;
    private Date startDate = new Date(0);
    private Date endDate = new Date(System.currentTimeMillis() + 10000L * 24L * 60L * 60000L);
    private boolean markToday = true;
    private boolean includeYear;

    /// Default constructor
    public DateTimeSpinner() {
        off = 0;
    }

    @Override
    void initSpinner() {
        if (date == null) {
            date = Spinner.createDate(startDate.getTime() + off, endDate.getTime() + off, currentDate.getTime(), ' ', Spinner.DATE_FORMAT_DOW_MON_DD);
            if (includeYear) {
                date.setRenderingPrototype("XXX XXX 99 9999");
                ((DateTimeRenderer) date.getRenderer()).setType(Spinner.DATE_FORMAT_DOW_MON_DD_YY);
            } else {
                date.setRenderingPrototype("XXX XXX 99");
                ((DateTimeRenderer) date.getRenderer()).setType(Spinner.DATE_FORMAT_DOW_MON_DD);
            }
            if (markToday) {
                ((DateTimeRenderer) date.getRenderer()).setMarkToday(markToday, today.getTime());
            }
            this.setCurrentDate(currentDate);
            this.setStartDate(startDate);
            this.setEndDate(endDate);

        }
        super.initSpinner();
    }

    @Override
    void addComponents() {
        if (date != null) {
            addComponent(date);
            addComponent(createSeparator());
            super.addComponents();
        }
    }

    /// #### Returns
    ///
    /// the currentDate
    public Date getCurrentDate() {
        if (date != null) {
            return (Date) date.getValue();
        }
        return currentDate;
    }

    /// #### Parameters
    ///
    /// - `currentDate`: the currentDate to set
    public void setCurrentDate(Date currentDate) {
        this.currentDate = currentDate;
        if (date != null) {
            date.setValue(currentDate);
        }
    }

    /// #### Returns
    ///
    /// the startDate
    public Date getStartDate() {
        return startDate;
    }

    /// #### Parameters
    ///
    /// - `startDate`: the startDate to set
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
        if (date != null) {
            date.setModel(new SpinnerDateModel(startDate.getTime() + off, endDate.getTime() + off, currentDate.getTime() + off));
        }
    }

    /// #### Returns
    ///
    /// the endDate
    public Date getEndDate() {
        return endDate;
    }

    /// #### Parameters
    ///
    /// - `endDate`: the endDate to set
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
        if (date != null) {
            date.setModel(new SpinnerDateModel(startDate.getTime() + off, endDate.getTime() + off, currentDate.getTime() + off));
        }
    }

    /// #### Returns
    ///
    /// the markToday
    public boolean isMarkToday() {
        return markToday;
    }

    /// #### Parameters
    ///
    /// - `markToday`: the markToday to set
    public void setMarkToday(boolean markToday) {
        this.markToday = markToday;
        if (date != null) {
            ((DateTimeRenderer) date.getRenderer()).setMarkToday(markToday, today.getTime() + off);
        }
    }

    /// #### Returns
    ///
    /// the includeYear
    public boolean isIncludeYear() {
        return includeYear;
    }

    /// #### Parameters
    ///
    /// - `includeYear`: the includeYear to set
    public void setIncludeYear(boolean includeYear) {
        this.includeYear = includeYear;
        if (date != null) {
            if (includeYear) {
                ((DateTimeRenderer) date.getRenderer()).setType(Spinner.DATE_FORMAT_DOW_MON_DD_YY);
                date.setRenderingPrototype("XXX XXX 99 9999");
            } else {
                ((DateTimeRenderer) date.getRenderer()).setType(Spinner.DATE_FORMAT_DOW_MON_DD);
                date.setRenderingPrototype("XXX XXX 99");
            }
        }
    }

    /// {@inheritDoc}
    @Override
    public String[] getPropertyNames() {
        return new String[]{"currentHour", "currentMinute", "minuteStep", "currentMeridiem", "showMeridiem",
                "currentDate", "startDate", "endDate", "markToday", "includeYear"};
    }

    /// {@inheritDoc}
    @Override
    public Class[] getPropertyTypes() {
        return new Class[]{Integer.class, Integer.class, Integer.class, Boolean.class, Boolean.class,
                Date.class, Date.class, Date.class, Boolean.class, Boolean.class};
    }

    /// {@inheritDoc}
    @Override
    public Object getPropertyValue(String name) {
        if ("currentDate".equals(name)) {
            return currentDate;
        }
        if ("startDate".equals(name)) {
            return startDate;
        }
        if ("endDate".equals(name)) {
            return endDate;
        }
        if ("markToday".equals(name)) {
            return Boolean.valueOf(markToday);
        }
        if ("includeYear".equals(name)) {
            return Boolean.valueOf(includeYear);
        }
        return super.getPropertyValue(name);
    }

    /// {@inheritDoc}
    @Override
    public String setPropertyValue(String name, Object value) {
        if ("currentDate".equals(name)) {
            setCurrentDate((Date) value);
            return null;
        }
        if ("startDate".equals(name)) {
            setStartDate((Date) value);
            return null;
        }
        if ("endDate".equals(name)) {
            setEndDate((Date) value);
            return null;
        }
        if ("markToday".equals(name)) {
            setMarkToday(((Boolean) value).booleanValue());
            return null;
        }
        if ("includeYear".equals(name)) {
            setIncludeYear(((Boolean) value).booleanValue());
            return null;
        }

        return super.setPropertyValue(name, value);
    }
}
