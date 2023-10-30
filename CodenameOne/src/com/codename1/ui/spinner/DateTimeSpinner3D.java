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

import static com.codename1.ui.CN.convertToPixels;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Style;
import java.util.Calendar;
import java.util.Date;

/**
 * The date and time spinner extends the time spinner by allowing to pick a specific day as well
 * 
 * Used by Picker in lightweight mode.
 *
 * @author Steve Hannah
 */
class DateTimeSpinner3D extends Container implements InternalPickerWidget {
    private Spinner3D date;
    private TimeSpinner3D time;
    private Date today = new Date();
    private Date currentDate = today;
    
    private Date startDate = new Date(0);
    private Date endDate = new Date(System.currentTimeMillis() + 10000L * 24L * 60L * 60000L);
    private boolean markToday = true;
    private boolean includeYear;
    private int off;
    private Container wrapper = new Container(BoxLayout.x());

    /**
     * Default constructor
     */
    public DateTimeSpinner3D(int minuteStep) {
        off = 0;
        initSpinner(minuteStep);
    }

    public DateTimeSpinner3D() {
        this(TimeSpinner3D.DEFAULT_MINUTE_STEP);
    }
    
    void initSpinner(int minuteStep) {
        if(date == null) {
            date = Spinner3D.createDate(startDate.getTime() + off, endDate.getTime() + off, currentDate.getTime());
            date.setPreferredW((int)(new Label("Thu Dec 27", "Spinner3DRow").getPreferredW() * 1.5f ));
            Style dateStyle = Style.createProxyStyle(date.getRowStyle(), date.getSelectedRowStyle());
            dateStyle.setAlignment(Component.RIGHT);
            dateStyle.setPaddingRight(3f);
            
            this.setCurrentDate(currentDate);
            this.setStartDate(startDate);
            this.setEndDate(endDate);
            
            time = new TimeSpinner3D(minuteStep);
            addComponents();
        }
    }

    public int getMinuteStep() {
        return time.getMinuteStep();
    }

    public void setMinuteStep(int minuteStep) {
        time.setMinuteStep(minuteStep);
    }

    @Override
    protected Dimension calcPreferredSize() {
        Dimension size = super.calcPreferredSize();
        Label l = new Label("Thu Dec 27    55  55  AM", "Spinner3DRow");
        size.setWidth((int)(l.getPreferredW() * 1.5f + convertToPixels(10f)));
        return size;
    }
    
    
    
    void addComponents() {
        if(date != null) {
            //setLayout(new LayeredLayout());
            setLayout(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE));
            add(BorderLayout.CENTER, wrapper);
            wrapper.addComponent(date);
            wrapper.addComponent(time);
            //LayeredLayout ll = (LayeredLayout)getLayout();
            //ll.setInsets(date, "0 auto 0 0")
            //        .setInsets(time, "0 auto 0 0")
            //        .setReferenceComponentLeft(time, date, 1f);
            
        } 
    }

    /**
     * @return the currentDate
     */
    public Date getCurrentDate() {
        if(date != null) {
            Date dt = (Date)date.getValue();
            Calendar cld = Calendar.getInstance();
            cld.setTime(dt);
            cld.set(Calendar.HOUR_OF_DAY, 0);
            cld.set(Calendar.MINUTE, 0);
            cld.set(Calendar.SECOND, 0);
            cld.set(Calendar.MILLISECOND, 0);
            
            Integer minutesInDay = (Integer)time.getValue();
            if (minutesInDay == null) {
                minutesInDay = 0;
            }
            cld.setTime(new Date(cld.getTime().getTime() + minutesInDay * 60l * 1000l));
            return cld.getTime();
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
        if (time != null) {
            Calendar cld = Calendar.getInstance();
            cld.setTime(currentDate);
            
            Calendar zero = Calendar.getInstance();
            zero.setTime(currentDate);
            zero.set(Calendar.HOUR_OF_DAY, 0);
            zero.set(Calendar.MINUTE, 0);
            zero.set(Calendar.SECOND, 0);
            zero.set(Calendar.MILLISECOND, 0);
            
            int minutesInDay = (int)((cld.getTime().getTime() - zero.getTime().getTime())/60l/1000l);
            time.setValue(minutesInDay);
        }
    }
    

    /**
     * @return the startDate
     */
    public Date getStartDate() {
        return startDate;
    }

    private void rebuildDate() {
        if(date != null) {
            long currTime = Math.max(startDate.getTime()+off, Math.min(endDate.getTime()+off, currentDate.getTime() + off));
            date.setModel(new SpinnerDateModel(startDate.getTime() + off, endDate.getTime() + off, currTime));
        }
    }
    
    /**
     * @param startDate the startDate to set
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
        rebuildDate();
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
        rebuildDate();
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
            //((DateTimeRenderer)date.getRenderer()).setMarkToday(markToday, today.getTime() + off);
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
                //((DateTimeRenderer)date.getRenderer()).setType(Spinner.DATE_FORMAT_DOW_MON_DD_YY);
                //date.setRenderingPrototype("XXX XXX 99 9999");
            } else {
                //((DateTimeRenderer)date.getRenderer()).setType(Spinner.DATE_FORMAT_DOW_MON_DD);
                //date.setRenderingPrototype("XXX XXX 99");
            }
        }
    }

    /**
     * Sets whether the time spinner should show times 12 hour format with an AM/PM selector.
     * @param showMeridiem True to show times in 12 hour format (the default).  False to show in 24 hour format.
     * @since 6.0
     * @see #isShowMeridiem() 
     * 
     */
    public void setShowMeridiem(boolean showMeridiem) {
        if (time == null) return;
        time.setShowMeridiem(showMeridiem);
    }
    
    /**
     * Checks if time is shown in 12 hour format with AM/PM selector.
     * @return True if time is in 12 hour format.  False otherwise.
     * @since 6.0
     * @see #setShowMeridiem(boolean) 
     */
    public boolean isShowMeridiem() {
        if (time == null) {
            return false;
        }
        return time.isShowMeridiem();
    }
    
    /**
     * Sets the hour range to show for the time selector.  Setting the range will automatically switch
     * the time to 24 hour format.
     * @param min The minimum hour to display (0-24).  -1 for no limit.
     * @param max The max hour to display (0-24).  -1 for no limit.
     * @since 6.0
     * @see #getMinHour() 
     * @see #getMaxHour() 
     */
    public void setHourRange(int min, int max) {
        if (time == null) return;
        if (min >= 0 && max > min && isShowMeridiem()) {
            time.setShowMeridiem(false);
        }
        time.setHourRange(min, max);
        
    }
    
    /**
     * Gets the minimum hour to display.  Default -1 (for no limit).  
     * @return Min hour (0-24) or -1 for no limit.
     * @since 6.0
     * @see #getMaxHour() 
     * @see #setHourRange(int, int) 
     */
    public int getMinHour() {
        if (time == null) return -1;
        return time.getMinHour();
    }
    
    /**
     * Gets the maximum hour to display.  Default -1 (for no limit).
     * @return Max hour (0-24) or -1 for no limit.
     * @since 6.0
     * @see #getMinHour() 
     * @see #setHourRange(int, int) 
     */
    public int getMaxHour() {
        if (time == null) return -1;
        return time.getMaxHour();
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

    @Override
    public Object getValue() {
        return getCurrentDate();
    }

    @Override
    public void setValue(Object value) {
        setCurrentDate((Date)value);
    }

    @Override
    public void paint(Graphics g) {

        int alpha = g.getAlpha();
        g.setColor(date.getSelectedOverlayStyle().getBgColor());
        g.setAlpha(255);
        g.fillRect(getX(), getY(), getWidth(), getHeight());
        g.setAlpha(alpha);
        super.paint(g); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
    
}
