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

import com.codename1.l10n.DateFormatSymbols;
import com.codename1.l10n.L10NManager;
import com.codename1.l10n.SimpleDateFormat;
import static com.codename1.ui.CN.convertToPixels;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import java.util.Calendar;
import java.util.Date;

/**
 * A date spinner allows selecting a date value within the given date range
 * 
 * This is used by the Picker when in lightweight mode.
 * 
 * @author Steve Hannah
 */
class DateSpinner3D extends Container implements InternalPickerWidget {
    private Spinner3D month;
    private Spinner3D day;
    private Spinner3D year;
    
    private boolean explicitStartYear, explicitEndYear, explicitStartMonth, explicitEndMonth, explicitStartDay, explicitEndDay, explicitCurrentYear;
    private int startYear = 1970;
    private int endYear = 2100;
    private int startMonth = 1;
    private int endMonth = 13;
    private int startDay = 1;
    private int endDay = 32;
    private int currentYear;
    private int currentDay;
    private int currentMonth;
    
    private int hourOfDay;
    private int minuteOfDay;
    private int secondsOfDay;
    private int millisOfDay;

    private boolean monthDayYear = true;
    private boolean numericMonths = false;

    private String monthRenderingPrototype = "WWW";
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM");
    private Container wrapper = new Container(BoxLayout.x());
    
    /**
     * Default constructor
     */
    public DateSpinner3D() {
        setLayout(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE));
        add(BorderLayout.CENTER, wrapper);
        Calendar c = Calendar.getInstance();
        currentDay = c.get(Calendar.DAY_OF_MONTH);
        currentMonth = c.get(Calendar.MONTH) + 1;
        currentYear = c.get(Calendar.YEAR);
        
        // If dates should be formatted with month day year in this locale,
        // then the first character of a formatted date should start with a letter
        // otherwise it will start with a number.
        String firstChar = L10NManager.getInstance().formatDateLongStyle(new Date()).substring(0, 1);
        monthDayYear = !firstChar.toLowerCase().equals(firstChar.toUpperCase());
        initSpinner();
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        monthFormat.getDateFormatSymbols().setResourceBundle(getUIManager().getResourceBundle());
    }
    
    
    
    void initSpinner() {
        if(month == null) {
            day = Spinner3D.create(1, 32, currentDay, 1);
            day.setRowFormatter(new SpinnerNode.RowFormatter() {
                
                @Override
                public String format(String input) {
                    if (input != null) {
                        return String.valueOf(new Double(Double.parseDouble(input)).intValue());
                    }
                    return null;
                }
            });
            
            month = Spinner3D.create(1, 13, currentMonth, 1);
            month.setRowFormatter(new SpinnerNode.RowFormatter() {
                @Override
                public String format(String input) {
                    if (input != null) {
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.MONTH, new Double(Double.parseDouble(input)).intValue()-1);
                        c.set(Calendar.DAY_OF_MONTH, 15);
                        return monthFormat.format(c.getTime());
                    }
                    return null;
                }
            });
            
            year = Spinner3D.create(startYear, endYear, currentYear, 1);
            year.setRowFormatter(new SpinnerNode.RowFormatter() {
                
                @Override
                public String format(String input) {
                    if (input != null) {
                        return String.valueOf(new Double(Double.parseDouble(input)).intValue());
                    }
                    return null;
                }
            });
            addComponents();
            
            //getAllStyles().setBgColor(year.getUnselectedStyle().getBgColor());
            //getAllStyles().setBgTransparency(255);
        }
    }
        
    private void addComponents() {
        if(year != null) {
            Label l = new Label("December", "Spinner3DRow");
            if(monthDayYear) {
                wrapper.addComponent(month);
                
                month.setPreferredW((int)(l.getPreferredW() * 1.5f));
                Style monthStyle = Style.createProxyStyle(month.getRowStyle(), month.getSelectedRowStyle());
                monthStyle.setAlignment(Component.LEFT);
                monthStyle.setPaddingLeft(3f);
                

                
                //month.refreshStyles();
                
                
                
                //addComponent(createSeparator());
                l.setText("00");
                day.setPreferredW((int)(l.getPreferredW() * 1.5f) + convertToPixels(3f));
                wrapper.addComponent(day);
                Style dayStyle = Style.createProxyStyle(day.getRowStyle(), day.getSelectedRowStyle());
                dayStyle.setAlignment(Component.RIGHT);
                dayStyle.setPaddingRight(3f);
                //day.refreshStyles();
                //addComponent(createSeparator());
                l.setText("0000");
                year.setPreferredW((int)(l.getPreferredW()*1.5f) + convertToPixels(3f));
                wrapper.addComponent(year);
                
                Style yearStyle = Style.createProxyStyle(year.getRowStyle(), year.getSelectedRowStyle());
                yearStyle.setAlignment(Component.RIGHT);
                yearStyle.setPaddingRight(3f);
                //year.refreshStyles();
                //LayeredLayout ll = (LayeredLayout)getLayout();
                //ll.setInsets(month, "0 55% 0 0")
                //        .setInsets(day, "0 35% 0 45%")
                //        .setInsets(year, "0 0 0 65%");

            } else {
                month.setPreferredW((int)(l.getPreferredW() * 1.5f));
                l.setText("00");
                day.setPreferredW((int)(l.getPreferredW() * 1.5f) + convertToPixels(3f));
                l.setText("0000");
                year.setPreferredW((int)(l.getPreferredW()*1.5f) + convertToPixels(3f));
                
                wrapper.addComponent(day);
                Style dayStyle = Style.createProxyStyle(day.getRowStyle(), day.getSelectedRowStyle());
                dayStyle.setAlignment(Component.RIGHT);
                dayStyle.setPaddingRight(3f);
                //day.refreshStyles();
                //addComponent(createSeparator());
                wrapper.addComponent(month);
                Style monthStyle = Style.createProxyStyle(month.getRowStyle(), month.getSelectedRowStyle());
                monthStyle.setAlignment(Component.LEFT);
                monthStyle.setPaddingLeft(3f);
                //month.refreshStyles();
                //addComponent(createSeparator());
                
                Style yearStyle = Style.createProxyStyle(year.getRowStyle(), year.getSelectedRowStyle());
                yearStyle.setAlignment(Component.RIGHT);
                yearStyle.setPaddingRight(3f);
                
                //year.refreshStyles();
                
                wrapper.addComponent(year);
                //LayeredLayout ll = (LayeredLayout)getLayout();
                //ll.setInsets(day, "0 67% 0 0")
                //        .setInsets(month, "0 33% 0 33%")
                //        .setInsets(year, "0 0 0 67%");
            }
            
        }
    }
    
    private void rebuildMonth() {
        month.setModel(new SpinnerNumberModel(startMonth, endMonth, Math.max(startMonth, Math.min(endMonth, currentMonth)), 1));
    }
    
    private void rebuildDay() {
        day.setModel(new SpinnerNumberModel(startDay, endDay, Math.max(startDay, Math.min(endDay, currentDay)), 1));
    }

    
    /**
     * Sets the start and end dates in this spinner.  Month range is only limited if the year of the start and end dates are the same.  Day range is only limited if
     * both the year and month  of the start and end dates are the same.
     * @param start The start date.
     * @param end The end date
     * @since 6.0
     */
    public void setDateRange(Date start, Date end) {
        explicitStartMonth = true;
        explicitEndMonth = true;
        explicitStartDay = true;
        explicitEndDay = true;
        explicitStartYear = true;
        explicitEndYear = true;
        int setEndYear = (end == null) ? 2100 : getYear(end) + 1900+1;
        if (!explicitCurrentYear && currentYear > setEndYear-1) {
            currentYear = setEndYear-1;
        }
        setEndYear(setEndYear);
        
        int setStartYear = start == null ? 1970 : getYear(start) + 1900;
        if (!explicitCurrentYear && currentYear < setStartYear) {
            currentYear = setStartYear;
        }
        setStartYear(setStartYear);
        
        
        if (start != null && end != null && getYear(start) == getYear(end)) {
            startMonth = getMonth(start)+1;
            endMonth = getMonth(end) + 2;
        } else {
            startMonth = 1;
            endMonth = 13;
        }
        rebuildMonth();
        
        if (start != null && end != null && getYear(start) == getYear(end) && getMonth(start) == getMonth(end)) {
            startDay = getDate(start);
            endDay = getDate(end)+1;
        } else {
            startDay = 1;
            endDay = 32;
        }
        rebuildDay();
        
    }
    
    private Calendar tmpCal=Calendar.getInstance();
    
    /**
     * Since CLDC11 doesn't have {@link Date#getDate() }, this returns the date of the given date as {@link Date#getDate() } would have.
     * @param dt The date
     * @return The day of month.
     */
    private int getDate(Date dt) {
        tmpCal.setTime(dt);
        return tmpCal.get(Calendar.DAY_OF_MONTH);
    }
    
    /**
     * Since CLDC11 doesn't have {@link Date#getMonth() }, this returns the month of the given date as {@link Date#getMonth()} would have.
     * @param dt
     * @return 
     */
    private int getMonth(Date dt) {
        tmpCal.setTime(dt);
        return tmpCal.get(Calendar.MONTH);
    }
    
    /**
     * Since CLDC doesn't have {@link Date#getYear() }, this returns the year of the given date as {@link Date#getYear() } would have.  E.g.
     * this returns 0 for year 1900
     * @param dt
     * @return 
     */
    private int getYear(Date dt) {
        tmpCal.setTime(dt);
        return tmpCal.get(Calendar.YEAR)-1900;
    }
    
    /**
     * Gets the start month of this Spinner.
     * @return 
     * @since 6.0
     */
    public int getStartMonth() {
        return startMonth;
    }
    
    
    /**
     * Gets the end month of this spinner.
     * @return 
     * @since 6.0
     */
    public int getEndMonth() {
        return endMonth;
    }
    
    
    /**
     * Gets the start day of month.
     * @return 
     * @since 6.0
     */
    public int getStartDay() {
        return startDay;
    }
    
    /**
     * Gets the end day of month.
     * @return 
     * @since 6.0
     */
    public int getEndDay() {
        return endDay;
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
        explicitStartYear = true;
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
        explicitEndYear = true;
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
        explicitCurrentYear = true;
        if (!explicitStartYear && startYear > currentYear) {
            startYear = currentYear;
        }
        if (!explicitEndYear && endYear -1 < currentYear) {
            endYear = currentYear+1;
        }
        if (currentYear < startYear) {
            throw new IllegalArgumentException("Current year "+currentYear+" before start year "+startYear);
        }
        if (currentYear > endYear - 1) {
            throw new IllegalArgumentException("Current year "+currentYear+" after end year "+endYear);
        }
        if(year != null) {
            year.setModel(new SpinnerNumberModel(startYear, endYear, currentYear, 1));
        }
    }

    /**
     * @return the currentDay
     */
    public int getCurrentDay() {
        Integer i = (Integer)day.getValue();
        return i.intValue();
    }

    /**
     * @param currentDay the currentDay to set
     */
    public void setCurrentDay(int currentDay) {
        this.currentDay = currentDay;
        if (!explicitStartDay && startDay > currentDay) {
            startDay = currentDay;
        }
        if (!explicitEndDay && endDay -1 < currentDay) {
            endDay = currentDay + 1;
        }
        if (startDay > currentDay) {
            throw new IllegalArgumentException("Start day "+startDay+" after current day "+currentDay);
        }
        if (endDay -1 < currentDay) {
            throw new IllegalArgumentException("End day "+endDay+" before current day "+currentDay);
        }
        if(day != null) {
            rebuildDay();
        }
    }

    /**
     * @return the currentMonth
     */
    public int getCurrentMonth() {
        if(month != null) {
            Integer i = ((Integer)month.getValue()-1)%12 + 1;
            return i.intValue();
        }
        return currentMonth;
    }

    /**
     * @param currentMonth the currentMonth to set
     */
    public void setCurrentMonth(int currentMonth) {
        this.currentMonth = currentMonth;
        if (!explicitStartMonth && startMonth > currentMonth) {
            startMonth = currentMonth;
        }
        if (!explicitEndMonth && endMonth -1 < currentMonth) {
            endMonth = currentMonth+1;
        }
        if (startMonth > currentMonth) {
            throw new IllegalArgumentException("Start month "+startMonth+" after current month "+currentMonth);
        }
        if (endMonth -1 < currentMonth) {
            throw new IllegalArgumentException("End month "+endMonth+" before current month "+currentMonth);
        }
        if(month != null) {
            rebuildMonth();
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
        wrapper.removeAll();
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
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] {"startYear", "endYear", "currentYear", "currentDay", "currentMonth", "monthDayYear", "numericMonths"};
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
       return new Class[] {Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Boolean.class, Boolean.class};
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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

    @Override
    public Object getValue() {
        Calendar cld = Calendar.getInstance();
        cld.setTime(new Date());
        cld.set(Calendar.DAY_OF_MONTH, getCurrentDay());
        cld.set(Calendar.MONTH, getCurrentMonth() - 1);
        cld.set(Calendar.YEAR, getCurrentYear());
        
        cld.set(Calendar.HOUR_OF_DAY,hourOfDay);
        cld.set(Calendar.MINUTE,minuteOfDay);
        cld.set(Calendar.SECOND,secondsOfDay);
        cld.set(Calendar.MILLISECOND, millisOfDay);

        return cld.getTime();
    }

    @Override
    public void setValue(Object value) {
        Date dt = (Date)value;
        Calendar cld = Calendar.getInstance();
        cld.setTime(dt);
        setCurrentDay(cld.get(Calendar.DAY_OF_MONTH));
        setCurrentMonth(cld.get(Calendar.MONTH)+1);
        setCurrentYear(cld.get(Calendar.YEAR));
        //keep time of day when editing the date
        hourOfDay=cld.get(Calendar.HOUR_OF_DAY);
        minuteOfDay=cld.get(Calendar.MINUTE);
        secondsOfDay=cld.get(Calendar.SECOND);
        millisOfDay=cld.get(Calendar.MILLISECOND);
    }

    @Override
    public void paint(Graphics g) {
        int alpha = g.getAlpha();
        g.setColor(year.getSelectedOverlayStyle().getBgColor());
        g.setAlpha(255);
        g.fillRect(getX(), getY(), getWidth(), getHeight());
        g.setAlpha(alpha);
        super.paint(g); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
}
