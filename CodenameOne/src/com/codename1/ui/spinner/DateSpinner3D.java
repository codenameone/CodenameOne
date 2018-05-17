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

import com.codename1.l10n.L10NManager;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.CN;
import static com.codename1.ui.CN.convertToPixels;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
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
class DateSpinner3D extends Container implements ISpinner3D {
    private Spinner3D month;
    private Spinner3D day;
    private Spinner3D year;
    
    
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
    public DateSpinner3D() {
        setLayout(BoxLayout.x());
        Calendar c = Calendar.getInstance();
        currentDay = c.get(Calendar.DAY_OF_MONTH);
        currentMonth = c.get(Calendar.MONTH) + 1;
        currentYear = c.get(Calendar.YEAR);
        initSpinner();
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
                SimpleDateFormat fmt = new SimpleDateFormat("MMMM");

                @Override
                public String format(String input) {
                    if (input != null) {
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.MONTH, new Double(Double.parseDouble(input)).intValue()-1);
                        return fmt.format(c.getTime());
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
                addComponent(month);
                
                month.setPreferredW((int)(l.getPreferredW() * 1.5f));
                Style monthStyle = Style.createProxyStyle(month.getRowStyle(), month.getSelectedRowStyle());
                monthStyle.setAlignment(Component.LEFT);
                monthStyle.setPaddingLeft(3f);
                

                
                //month.refreshStyles();
                
                
                
                //addComponent(createSeparator());
                l.setText("00");
                day.setPreferredW((int)(l.getPreferredW() * 1.5f) + convertToPixels(3f));
                addComponent(day);
                Style dayStyle = Style.createProxyStyle(day.getRowStyle(), day.getSelectedRowStyle());
                dayStyle.setAlignment(Component.RIGHT);
                dayStyle.setPaddingRight(3f);
                //day.refreshStyles();
                //addComponent(createSeparator());
                l.setText("0000");
                year.setPreferredW((int)(l.getPreferredW()*1.5f) + convertToPixels(3f));
                addComponent(year);
                
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
                
                addComponent(day);
                Style dayStyle = Style.createProxyStyle(day.getRowStyle(), day.getSelectedRowStyle());
                dayStyle.setAlignment(Component.RIGHT);
                dayStyle.setPaddingRight(3f);
                //day.refreshStyles();
                //addComponent(createSeparator());
                addComponent(month);
                Style monthStyle = Style.createProxyStyle(month.getRowStyle(), month.getSelectedRowStyle());
                monthStyle.setAlignment(Component.LEFT);
                monthStyle.setPaddingLeft(3f);
                //month.refreshStyles();
                //addComponent(createSeparator());
                
                Style yearStyle = Style.createProxyStyle(year.getRowStyle(), year.getSelectedRowStyle());
                yearStyle.setAlignment(Component.RIGHT);
                yearStyle.setPaddingRight(3f);
                
                //year.refreshStyles();
                
                addComponent(year);
                //LayeredLayout ll = (LayeredLayout)getLayout();
                //ll.setInsets(day, "0 67% 0 0")
                //        .setInsets(month, "0 33% 0 33%")
                //        .setInsets(year, "0 0 0 67%");
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
