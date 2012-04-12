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

/**
 * Allows selecting a time of day either in 24 hour batches or AM/PM format
 *
 * @author Shai Almog
 */
public class TimeSpinner extends BaseSpinner {
    private Spinner hour;
    private Spinner minute;
    private Spinner amPM;
    
    
    private int startHour = 1;
    private int endHour = 13;
    private int minuteStep = 5;

    private boolean durationMode;
    private boolean showMeridiem = true;
    private int currentHour = 8;
    private int currentMinute = 0;
    private boolean currentMeridiem;
    
    /**
     * Default constructor
     */
    public TimeSpinner() {
    }
    
    /**
     * Default constructor
     */
    protected void initComponent() {
        super.initComponent();
        if(hour == null) {
            hour = Spinner.create(startHour, endHour, currentHour, 1);
            minute = Spinner.create(0, 60, currentMinute, minuteStep);
            if(currentMeridiem) {
                amPM = Spinner.create(0, 2, 1, 1);
            } else {
                amPM = Spinner.create(0, 2, 0, 1);
            }
            ((DefaultListCellRenderer)hour.getRenderer()).setRightAlignNumbers(true);
            DefaultListCellRenderer twoDigitRender = new DefaultListCellRenderer(false) {
                public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected) {
                    if(value != null && value instanceof Integer) {
                        int i = ((Integer)value).intValue();
                        if(i < 10) {
                            value = "0" + i;
                        }
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected);
                }                
            };
            minute.setRenderer(twoDigitRender);
            
            DefaultListCellRenderer render = new DefaultListCellRenderer(false) {
                public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected) {
                    if(value != null && value instanceof Integer) {
                        int d = ((Integer)value).intValue();
                        if(d == 0) {
                            value = "AM";
                        } else {
                            value = "PM";
                        }
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected);
                }                
            };
            amPM.setRenderer(render);
            render.setRTL(false);
            render.setShowNumbers(false);
            twoDigitRender.setUIID("SpinnerRenderer");
            render.setUIID("SpinnerRenderer");
            amPM.setRenderingPrototype("WW");

            addComponents();
        }
    }
        
    void addComponents() {
        if(amPM != null) {
            addComponent(hour);
            addComponent(createSeparator());
            addComponent(minute);
            if(showMeridiem) {
                addComponent(createSeparator());
                addComponent(amPM);
            } 
        }
    }

    
    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[] {"currentHour", "currentMinute", "minuteStep", "currentMeridiem", "showMeridiem", "durationMode"};
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
       return new Class[] {Integer.class, Integer.class, Integer.class, Boolean.class, Boolean.class, Boolean.class};
    }

    /**
     * @inheritDoc
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
     * @inheritDoc
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
        if(name.equals("durationMode")) {
            setDurationMode(((Boolean)value).booleanValue());
            return null;
        }
        
        return super.setPropertyValue(name, value);
    }

    /**
     * @return the minuteStep
     */
    public int getMinuteStep() {
        return minuteStep;
    }

    /**
     * @param minuteStep the minuteStep to set
     */
    public void setMinuteStep(int minuteStep) {
        this.minuteStep = minuteStep;
        if(minute != null) {
            minute.setModel(new SpinnerNumberModel(0, 60, currentMinute, minuteStep));
        }
    }

    /**
     * @return the showMeridiem
     */
    public boolean isShowMeridiem() {
        return showMeridiem && !durationMode;
    }

    /**
     * @param showMeridiem the showMeridiem to set
     */
    public void setShowMeridiem(boolean showMeridiem) {
        if(durationMode) {
            return;
        }
        this.showMeridiem = showMeridiem;
        if(showMeridiem) {
            startHour = 1;
            endHour = 13;
        } else {
            startHour = 0;
            endHour = 24;
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
     * @return the currentHour
     */
    public int getCurrentHour() {
        if(hour != null) {
            return ((Integer)hour.getValue()).intValue();
        } 
        return currentHour;
    }

    /**
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
        if(currentMeridiem) {
            amPM.setValue(new Integer(1));
        } else {
            amPM.setValue(new Integer(0));
        }
    }

    /**
     * Duration mode uses the time spinner to indicate a duration in hours and minutes
     * @return the durationMode
     */
    public boolean isDurationMode() {
        return durationMode;
    }

    /**
     * Duration mode uses the time spinner to indicate a duration in hours and minutes
     * @param durationMode the durationMode to set
     */
    public void setDurationMode(boolean durationMode) {
        if(durationMode) {
            setShowMeridiem(false);
            startHour = 0;
            endHour = 24;
        } else {
            if(showMeridiem) {
                startHour = 1;
                endHour = 13;
            } else {
                startHour = 0;
                endHour = 24;
            }
        }
        this.durationMode = durationMode;
    }
}
