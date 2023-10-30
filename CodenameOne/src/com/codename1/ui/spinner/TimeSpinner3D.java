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

import com.codename1.ui.CN;
import static com.codename1.ui.CN.convertToPixels;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.spinner.SpinnerNode.RowFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * Allows selecting a time of day either in 24 hour batches or AM/PM format.
 * 
 * <p>If {@link #setDurationMode(boolean) } is {@literal true } then this will allow
 * users to set a duration in hours and minutes.</p>
 * 
 * @author Steve Hannah
 */
class TimeSpinner3D extends Container implements InternalPickerWidget {

    static final int DEFAULT_MINUTE_STEP = 5;
    private Spinner3D hour;
    private Spinner3D minute;
    private Spinner3D amPM;
    
    private int minHour = -1;
    private int maxHour = -1;
    
    private int startHour = 1;
    private int endHour = 12;
    private int minuteStep = DEFAULT_MINUTE_STEP;

    private boolean durationMode;
    private boolean showHours=true;
    private boolean showMinutes=true;
    private boolean showMeridiem = true;
    private int currentHour = 8;
    private int currentMinute = 0;
    private boolean currentMeridiem;
    
    /**
     * Default constructor
     */
    public TimeSpinner3D(int minuteStep) {
        this.minuteStep = minuteStep;
        initSpinner();
    }

    public TimeSpinner3D() {
        this(DEFAULT_MINUTE_STEP);
    }
    
    /**
     * Default constructor
     */
    void initSpinner() {
        if(hour == null) {
            hour = Spinner3D.create(startHour, endHour, currentHour, 1);
            hour.setRowFormatter(new RowFormatter() {

                @Override
                public String format(String input) {
                    if (input != null) {
                        return ""+new Double(Double.parseDouble(input)).intValue();
                    }
                    return null;
                }
                
            });
            Style hourStyle = Style.createProxyStyle(hour.getRowStyle(), hour.getSelectedRowStyle());
            hourStyle.setAlignment(Component.RIGHT);
            hourStyle.setPaddingRight(3f);
            //hour.refreshStyles();
            minute = Spinner3D.create(0, 59, currentMinute, minuteStep);
            minute.setRowFormatter(new SpinnerNode.RowFormatter() {

                @Override
                public String format(String input) {
                    if (input != null) {
                        Integer value = null;
                        try {
                            value = new Integer(new Double(Double.parseDouble(input)).intValue());
                        } catch (Throwable t) {
                            
                        }
                        if(value != null && value instanceof Integer) {
                            int i = ((Integer)value).intValue();
                            if(i < 10) {
                                return "0" + i;
                            } else {
                                return ""+i;
                            }
                        }
                    }
                    return null;
                }
            });
            
            Style minuteStyle = Style.createProxyStyle(minute.getRowStyle(), minute.getSelectedRowStyle());
            minuteStyle.setAlignment(Component.RIGHT);
            minuteStyle.setPaddingRight(3f);
            
            //minute.refreshStyles();
            if(currentMeridiem) {
                amPM = Spinner3D.create(0, 2, 1, 1);
            } else {
                amPM = Spinner3D.create(0, 2, 0, 1);
            }
            
            amPM.setRowFormatter(new RowFormatter() {

                @Override
                public String format(String input) {
                    if (Double.parseDouble(input) < 1) {
                        return "AM";
                    }
                    return "PM";
                }
                
            });
            //getAllStyles().setBgColor(hour.getUnselectedStyle().getBgColor());
            //getAllStyles().setBgTransparency(255);
            addComponents();
        }
    }
        
    void addComponents() {
       
        
        
        if(amPM != null) {
            //setLayout(BoxLayout.x());
            setLayout(new LayeredLayout());
            addComponent(hour);
            
            addComponent(minute);
            LayeredLayout ll = (LayeredLayout)getLayout();
            if(showMeridiem) {
                //content.addComponent(createSeparator());
                addComponent(amPM);
                ll.setInsets(hour, "0 67% 0 0")
                        .setInsets(minute, "0 33% 0 33%")
                        .setInsets(amPM, "0 0 0 67%");
            }  else {
                ll.setInsets(hour, "0 50% 0 0")
                        .setInsets(minute, "0 0 0 50%");
            }
            
            
            
        }
        setHoursVisible(showHours);
        setMinutesVisible(showMinutes);

    }

    @Override
    protected Dimension calcPreferredSize() {
        Dimension size = super.calcPreferredSize();
        Label l = new Label("00  00  AM", "Spinner3DRow");
        float w = l.getPreferredW() * 1.5f + convertToPixels(10f);
        size.setWidth((int)w);
        return size;
    }
    
    
    
    
    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] {"currentHour", "currentMinute", "minuteStep", "currentMeridiem", "showMeridiem", "durationMode"};
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
       return new Class[] {Integer.class, Integer.class, Integer.class, Boolean.class, Boolean.class, Boolean.class};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if(name.equals("durationMode")) {
            if(durationMode) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if(name.equals("currentHour")) {
            return new Integer(currentHour);
        }
        if(name.equals("currentMinute")) {
            return new Integer(currentMinute);
        }
        if(name.equals("minuteStep")) {
            return new Integer(minuteStep);
        }
        if(name.equals("currentMeridiem")) {
            if(currentMeridiem) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if(name.equals("showMeridiem")) {
            if(showMeridiem) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("currentHour")) {
            setCurrentHour(Integer.parseInt(value.toString()));
            return null;
        }
        if(name.equals("currentMinute")) {
            setCurrentMinute(Integer.parseInt(value.toString()));
            return null;
        }
        if(name.equals("minuteStep")) {
            setMinuteStep(Integer.parseInt(value.toString()));
            return null;
        }
        if(name.equals("currentMeridiem")) {
            setCurrentMeridiem(((Boolean)value).booleanValue());
            return null;
        }
        if(name.equals("showMeridiem")) {
            setShowMeridiem(((Boolean)value).booleanValue());
            return null;
        }
        
        
        return super.setPropertyValue(name, value);
    }

    /**
     * Gets the minutes spinner step size.
     * @return the minuteStep
     */
    public int getMinuteStep() {
        return minuteStep;
    }

    /**
     * Sets the step-size for the minutes spinner.
     * @param minuteStep The step size.  Must be beween 1 and 60.
     */
    public void setMinuteStep(int minuteStep) {
        if (minuteStep < 1 || minuteStep > 60) {
            throw new IllegalArgumentException("Minute step must be between 1 and 60");
        }
        this.minuteStep = minuteStep;
        if(minute != null) {
            minute.setModel(new SpinnerNumberModel(0, 60, currentMinute, minuteStep));
        }
    }
    
    /**
     * Sets the hours range to display. Note that the spinner must not be in duration mode, and must by in 24 hour
     * mode, otherwise this will thrown an {@link IllegalStateException}.
     * @param min The minimum hour to display (0-24). Set to -1 to allow any minimum.
     * @param nax The maximum hour to display (0-24).  Set -1 to allow any maximum.
     * @throws IllegalStateException If the spinner is not in 24 hour mode.
     * @see #getMinHour() 
     * @see #getMaxHour() 
     * @since 6.0
     */
    public void setHourRange(int min, int max) {
        if (min >= 0 && max >= min && (showMeridiem || durationMode)) {
            throw new IllegalStateException("TimeSpinner hour range only applies when isShowMeridiem() is false and durationMode is false.");
        }
        minHour = min;
        maxHour = max;
        rebuildHours();
    }
    
    /**
     * Gets the current minimum hour to display.  -1 for no limit
     * @return The current minimum hour (0-24).  -1 for no limit.
     * @see #setHourRange(int, int) 
     * @see #getMaxHour() 
     * @since 6.0
     */
    public int getMinHour() {
        return minHour;
    }
    
    /**
     * Gets the current maximum hour to display.  -1 for no limit.
     * 
     * @return The current maximum hour (0-24). -1 for no limit.
     * @see #getMinHour() 
     * @see #setHourRange(int, int) 
     * @since 6.0
     */
    public int getMaxHour() {
        return maxHour;
    }
    
    /**
     * @return the showMeridiem
     */
    public boolean isShowMeridiem() {
        return showMeridiem && !durationMode;
    }

    private void rebuildHours() {
        if(showMeridiem) {
            startHour = 1;
            endHour = 12;
        } else {
            startHour = 0;
            endHour = 23;
            if (minHour >= 0 && minHour <= 23) {
                startHour = minHour;
            }
            if (maxHour >= 0 && maxHour <= 23 && maxHour > startHour) {
                endHour = maxHour;
            }
        }
        if(hour != null) {
            hour.setModel(new SpinnerNumberModel(startHour, endHour, currentHour, 1));
        }
        removeAll();
        addComponents();
        if(isInitialized()) {
            getParent().revalidate();
        }
    }
    
    /**
     * Shows AM/PM indication
     * @param showMeridiem the showMeridiem to set
     */
    public void setShowMeridiem(boolean showMeridiem) {
        if(durationMode) {
            return;
        }
        this.showMeridiem = showMeridiem;
        rebuildHours();
    }

    /**
     * The hour from 1-12 or 0-23
     * @return the currentHour
     */
    public int getCurrentHour() {
        if(hour != null) {
            return ((Integer)hour.getValue()).intValue();
        } 
        return currentHour;
    }

    /**
     * Set the hour from 1-12 or 0-23
     * @param currentHour the currentHour to set
     */
    public void setCurrentHour(int currentHour) {
        this.currentHour = currentHour;
        if(hour != null) {
            hour.setValue(new Integer(currentHour));
        }
    }

    /**
     * @return the currentMinute
     */
    public int getCurrentMinute() {
        if(minute != null) {
            return ((Integer)minute.getValue()).intValue();
        }
        return currentMinute;
    }

    /**
     * @param currentMinute the currentMinute to set
     */
    public void setCurrentMinute(int currentMinute) {
        this.currentMinute = currentMinute;
        if(minute != null) {
            minute.setValue(new Integer(currentMinute));
        }
    }

    /**
     * @return the currentMeridiem
     */
    public boolean isCurrentMeridiem() {
        if(durationMode) {
            return false;
        }
        if(amPM != null) {
            return ((Integer)amPM.getValue()).intValue() != 0;
        } 
        return currentMeridiem;
    }

    /**
     * @param currentMeridiem the currentMeridiem to set
     */
    public void setCurrentMeridiem(boolean currentMeridiem) {
        if(durationMode) {
            return;
        }
        this.currentMeridiem = currentMeridiem;
        if(amPM != null) {
            if(currentMeridiem) {
                amPM.setValue(new Integer(1));
            } else {
                amPM.setValue(new Integer(0));
            }
        }
    }

    
    /**
     * Show or hide the hours spinner.
     * @param visible True to show the hours spinner.
     */
    public void setHoursVisible(boolean visible) {
        showHours = visible;

        hour.setVisible(visible);
        hour.setHidden(!visible);
        
        
        
    }
    
    /**
     * Show or hide the minutes spinner.
     * @param visible True to make the minutes spinner visible.
     */
    public void setMinutesVisible(boolean visible) {
        showMinutes = visible;
        minute.setVisible(visible);
        minute.setHidden(!visible);

    }
    
    @Override
    public Object getValue() {
        Calendar cld = Calendar.getInstance();
        cld.setTime(new Date());
        cld.set(Calendar.MINUTE, getCurrentMinute());
        cld.set(Calendar.SECOND, 0);
        cld.set(Calendar.MILLISECOND, 0);
        int hour = getCurrentHour();
        if (showMeridiem && hour == 12) {
            hour = 0;
        }
        cld.set(showMeridiem ? Calendar.HOUR : Calendar.HOUR_OF_DAY, hour);
        if (showMeridiem) {
            cld.set(Calendar.AM_PM, isCurrentMeridiem() ? 1 : 0);
        }
        Calendar zero = Calendar.getInstance();
        zero.setTime(new Date());
        zero.set(Calendar.HOUR_OF_DAY, 0);
        zero.set(Calendar.MINUTE, 0);
        zero.set(Calendar.SECOND, 0);
        zero.set(Calendar.MILLISECOND, 0);
        return (int)((cld.getTime().getTime()- zero.getTime().getTime())/1000l/60l);
    }
    
    @Override
    public void setValue(Object value) {
        if (value == null) {
            value = 0;
        }
        Integer dt = (Integer)value;
        Calendar cld = Calendar.getInstance();
        cld.setTime(new Date());
        cld.set(Calendar.HOUR_OF_DAY, 0);
        cld.set(Calendar.MINUTE, 0);
        cld.set(Calendar.SECOND, 0);
        cld.set(Calendar.MILLISECOND, 0);
        cld.setTime(new Date(cld.getTime().getTime() + dt.intValue() * 60l * 1000l));
        setCurrentMinute(cld.get(Calendar.MINUTE));
        int hour = cld.get(showMeridiem ? Calendar.HOUR : Calendar.HOUR_OF_DAY);
        if (showMeridiem && hour == 0) {
            hour = 12;
        }
        setCurrentHour(hour);
        setCurrentMeridiem(cld.get(Calendar.AM_PM) == 0 ? false : true);
    }

    @Override
    public void paint(Graphics g) {
        int alpha = g.getAlpha();
        g.setColor(hour.getSelectedOverlayStyle().getBgColor());
        g.setAlpha(255);
        g.fillRect(getX(), getY(), getWidth(), getHeight());
        super.paint(g); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
