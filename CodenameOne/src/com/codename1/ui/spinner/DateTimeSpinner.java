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
import java.util.TimeZone;

/**
 * The date and time spinner extends the time spinner by allowing to pick a specific day as well
 *
 * @author Shai Almog
 */
public class DateTimeSpinner extends TimeSpinner {
    private Spinner date;
    private Date today = new Date();
    private Date currentDate = today;
    private Date startDate = new Date(0);
    private Date endDate = new Date(System.currentTimeMillis() + 10000L * 24L * 60L * 60000L);
    private boolean markToday = true;
    private boolean includeYear;
    private int off;

    /**
     * Default constructor
     */
    public DateTimeSpinner() {
        off = 0;
    }
    
    void initSpinner() {
        if(date == null) {
            date = Spinner.createDate(startDate.getTime() + off, endDate.getTime() + off, currentDate.getTime(), ' ', Spinner.DATE_FORMAT_DOW_MON_DD);
            if(includeYear) {
                date.setRenderingPrototype("XXX XXX 99 9999");
                ((DateTimeRenderer)date.getRenderer()).setType(Spinner.DATE_FORMAT_DOW_MON_DD_YY);
            } else {
                date.setRenderingPrototype("XXX XXX 99");
                ((DateTimeRenderer)date.getRenderer()).setType(Spinner.DATE_FORMAT_DOW_MON_DD);
            }
            if(markToday) {
                ((DateTimeRenderer)date.getRenderer()).setMarkToday(markToday, today.getTime());
            }
            this.setCurrentDate(currentDate);
            this.setStartDate(startDate);
            this.setEndDate(endDate);
            
        }
        super.initSpinner();
    }
    
    void addComponents() {
        if(date != null) {
            addComponent(date);
            addComponent(createSeparator());
            super.addComponents();
        } 
    }

    /**
     * @return the currentDate
     */
    public Date getCurrentDate() {
        if(date != null) {
            return (Date)date.getValue();
        }
        return currentDate;
    }

    /**
     * @param currentDate the currentDate to set
     */
    public void setCurrentDate(Date currentDate) {
        this.currentDate = currentDate;
        if(date != null) {
            date.setValue(currentDate);
        }
    }

    /**
     * @return the startDate
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * @param startDate the startDate to set
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
        if(date != null) {
            date.setModel(new SpinnerDateModel(startDate.getTime() + off, endDate.getTime() + off, currentDate.getTime() + off));
        }
    }

    /**
     * @return the endDate
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * @param endDate the endDate to set
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
        if(date != null) {
            date.setModel(new SpinnerDateModel(startDate.getTime() + off, endDate.getTime() + off, currentDate.getTime() + off));
        }
    }

    /**
     * @return the markToday
     */
    public boolean isMarkToday() {
        return markToday;
    }

    /**
     * @param markToday the markToday to set
     */
    public void setMarkToday(boolean markToday) {
        this.markToday = markToday;
        if(date != null) {
            ((DateTimeRenderer)date.getRenderer()).setMarkToday(markToday, today.getTime() + off);
        }
    }

    /**
     * @return the includeYear
     */
    public boolean isIncludeYear() {
        return includeYear;
    }

    /**
     * @param includeYear the includeYear to set
     */
    public void setIncludeYear(boolean includeYear) {
        this.includeYear = includeYear;
        if(date != null) {
            if(includeYear) {
                ((DateTimeRenderer)date.getRenderer()).setType(Spinner.DATE_FORMAT_DOW_MON_DD_YY);
                date.setRenderingPrototype("XXX XXX 99 9999");
            } else {
                ((DateTimeRenderer)date.getRenderer()).setType(Spinner.DATE_FORMAT_DOW_MON_DD);
                date.setRenderingPrototype("XXX XXX 99");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] {"currentHour", "currentMinute", "minuteStep", "currentMeridiem", "showMeridiem",
            "currentDate", "startDate", "endDate", "markToday", "includeYear"};
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
       return new Class[] {Integer.class, Integer.class, Integer.class, Boolean.class, Boolean.class,
            Date.class, Date.class, Date.class, Boolean.class, Boolean.class};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if(name.equals("currentDate")) {
            return currentDate;
        }
        if(name.equals("startDate")) {
            return startDate;
        }
        if(name.equals("endDate")) {
            return endDate;
        }
        if(name.equals("markToday")) {
            return new Boolean(markToday);
        }
        if(name.equals("includeYear")) {
            return new Boolean(includeYear);
        }
        return super.getPropertyValue(name);
    }

    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("currentDate")) {
            setCurrentDate((Date)value);
            return null;
        }
        if(name.equals("startDate")) {
            setStartDate((Date)value);
            return null;
        }
        if(name.equals("endDate")) {
            setEndDate((Date)value);
            return null;
        }
        if(name.equals("markToday")) {
            setMarkToday(((Boolean)value).booleanValue());
            return null;
        }
        if(name.equals("includeYear")) {
            setIncludeYear(((Boolean)value).booleanValue());
            return null;
        }
        
        return super.setPropertyValue(name, value);
    }
}
