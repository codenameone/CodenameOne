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

import com.codename1.ui.Component;
import com.codename1.ui.List;
import com.codename1.ui.list.DefaultListCellRenderer;
import java.util.Calendar;

/**
 * A date spinner allows selecting a date value within the given date range
 * 
 * @author Shai Almog
 */
public class DateSpinner extends BaseSpinner {
    private Spinner month;
    private Spinner day;
    private Spinner year;
    
    
    private int startYear = 1970;
    private int endYear = 2100;
    private int currentYear;
    private int currentDay;
    private int currentMonth;
    
    private boolean monthDayYear = true;
    private boolean numericMonths = false;

    private String monthRenderingPrototype = "WWW";
    
    /**
     * Default constructor
     */
    public DateSpinner() {
        Calendar c = Calendar.getInstance();
        currentDay = c.get(Calendar.DAY_OF_MONTH);
        currentMonth = c.get(Calendar.MONTH) + 1;
        currentYear = c.get(Calendar.YEAR);
    }
    
    void initSpinner() {
        if(month == null) {
            day = Spinner.create(1, 32, currentDay, 1);
            month = Spinner.create(1, 13, currentMonth, 1);
            SpinnerRenderer<Object> render = new SpinnerRenderer<Object>() {
                public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected) {
                    if(value != null && value instanceof Integer) {
                        // round the number in the spinner to two digits
                        int d = ((Integer)value).intValue();
                        if(numericMonths) {
                            value = "" + d;
                        } else {
                            value = DateTimeRenderer.MONTHS[d - 1];
                        }
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected);
                }                
            };
            month.setRenderer(render);
            month.initSpinnerRenderer();
            month.setRenderingPrototype(monthRenderingPrototype);

            year = Spinner.create(startYear, endYear, currentYear, 1);
            addComponents();
        }
    }
        
    private void addComponents() {
        if(year != null) {
            if(monthDayYear) {
                addComponent(month);
                addComponent(createSeparator());
                addComponent(day);
                addComponent(createSeparator());
                addComponent(year);
            } else {
                addComponent(day);
                addComponent(createSeparator());
                addComponent(month);
                addComponent(createSeparator());
                addComponent(year);
            }
        }
    }

    /**
     * @return the startYear
     */
    public int getStartYear() {
        return startYear;
    }

    /**
     * @param startYear the startYear to set
     */
    public void setStartYear(int startYear) {
        this.startYear = startYear;
        if(year != null) {
            year.setModel(new SpinnerNumberModel(startYear, endYear, currentYear, 1));
        }
    }

    /**
     * @return the endYear
     */
    public int getEndYear() {
        return endYear;
    }

    /**
     * @param endYear the endYear to set
     */
    public void setEndYear(int endYear) {
        this.endYear = endYear;
        if(year != null) {
            year.setModel(new SpinnerNumberModel(startYear, endYear, currentYear, 1));
        }
    }

    /**
     * @return the currentYear
     */
    public int getCurrentYear() {
        if(year != null) {
            return ((Integer)year.getValue()).intValue();
        }
        return currentYear;
    }

    /**
     * @param currentYear the currentYear to set
     */
    public void setCurrentYear(int currentYear) {
        this.currentYear = currentYear;
        if(year != null) {
            year.setModel(new SpinnerNumberModel(startYear, endYear, currentYear, 1));
        }
    }

    /**
     * @return the currentDay
     */
    public int getCurrentDay() {
        return ((Integer)day.getValue()).intValue();
    }

    /**
     * @param currentDay the currentDay to set
     */
    public void setCurrentDay(int currentDay) {
        this.currentDay = currentDay;
        if(day != null) {
            day.setModel(new SpinnerNumberModel(1, 32, currentDay, 1));
        }
    }

    /**
     * @return the currentMonth
     */
    public int getCurrentMonth() {
        if(month != null) {
            return ((Integer)month.getValue()).intValue();
        }
        return currentMonth;
    }

    /**
     * @param currentMonth the currentMonth to set
     */
    public void setCurrentMonth(int currentMonth) {
        this.currentMonth = currentMonth;
        if(month != null) {
            month.setModel(new SpinnerNumberModel(1, 13, currentMonth, 1));
        }
    }

    /**
     * @return the monthDayYear
     */
    public boolean isMonthDayYear() {
        return monthDayYear;
    }

    /**
     * @param monthDayYear the monthDayYear to set
     */
    public void setMonthDayYear(boolean monthDayYear) {
        this.monthDayYear = monthDayYear;
        removeAll();
        addComponents();
    }

    /**
     * @return the numericMonths
     */
    public boolean isNumericMonths() {
        return numericMonths;
    }

    /**
     * @param numericMonths the numericMonths to set
     */
    public void setNumericMonths(boolean numericMonths) {
        this.numericMonths = numericMonths;
        if(month != null) {
            month.repaint();
        }
    }
 
    
    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[] {"startYear", "endYear", "currentYear", "currentDay", "currentMonth", "monthDayYear", "numericMonths"};
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
       return new Class[] {Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Boolean.class, Boolean.class};
    }

    /**
     * @inheritDoc
     */
    public Object getPropertyValue(String name) {
        if(name.equals("startYear")) {
            return new Integer(startYear);
        }
        if(name.equals("endYear")) {
            return new Integer(endYear);
        }
        if(name.equals("currentYear")) {
            return new Integer(currentYear);
        }
        if(name.equals("currentDay")) {
            return new Integer(currentDay);
        }
        if(name.equals("currentMonth")) {
            return new Integer(currentMonth);
        }
        if(name.equals("monthDayYear")) {
            return new Boolean(monthDayYear);
        }
        if(name.equals("numericMonths")) {
            return new Boolean(numericMonths);
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("startYear")) {
            setStartYear(Integer.parseInt(value.toString()));
            return null;
        }
        if(name.equals("endYear")) {
            setEndYear(Integer.parseInt(value.toString()));
            return null;
        }
        if(name.equals("currentYear")) {
            setCurrentYear(Integer.parseInt(value.toString()));
            return null;
        }
        if(name.equals("currentDay")) {
            setCurrentDay(Integer.parseInt(value.toString()));
            return null;
        }
        if(name.equals("currentMonth")) {
            setCurrentMonth(Integer.parseInt(value.toString()));
            return null;
        }
        if(name.equals("monthDayYear")) {
            setMonthDayYear(((Boolean)value).booleanValue());
            return null;
        }
        if(name.equals("numericMonths")) {
            setNumericMonths(((Boolean)value).booleanValue());
            return null;
        }
        
        return super.setPropertyValue(name, value);
    }        
    
    
    /**
     * Sets the Month Rendering Prototype to be used, useful when the language 
     * is changed and you need the month spinner to be wider.
     * 
     * @param monthPrototype a prototype to be used to calc the month cell size
     */ 
    public void setMonthRenderingPrototype(String monthPrototype){
        this.monthRenderingPrototype = monthPrototype;
    }
}
