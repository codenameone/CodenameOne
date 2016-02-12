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

import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.list.DefaultListModel;
import java.util.Calendar;
import java.util.Date;

/**
 * The picker is a component and API that allows either poping up a spinner or
 * using the native picker API when applicable. This is quite important for some
 * platforms where the native spinner behavior is very hard to replicate.
 *
 * @author Shai Almog
 */
public class Picker extends Button {
    private int type = Display.PICKER_TYPE_DATE;
    private Object value = new Date();
    private boolean showMeridiem;
    private Object metaData;
    private Object renderingPrototype;
    private SimpleDateFormat formatter;
    private int preferredPopupWidth;
    private int preferredPopupHeight;
    
    /**
     * Default constructor
     */
    public Picker() {
        setUIID("TextField");
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if(Display.getInstance().isNativePickerTypeSupported(type)) {
                    setEnabled(false);
                    Object val = Display.getInstance().showNativePicker(type, Picker.this, value, metaData);
                    if(val != null) {
                        value = val;
                        updateValue();
                    }
                    setEnabled(true);
                } else {
                    Dialog pickerDlg = new Dialog();
                    pickerDlg.setDisposeWhenPointerOutOfBounds(true);
                    pickerDlg.setLayout(new BorderLayout());
                    Calendar cld = Calendar.getInstance();
                    switch(type) {
                        case Display.PICKER_TYPE_STRINGS:
                            GenericSpinner gs = new GenericSpinner();
                            if(renderingPrototype != null) {
                                gs.setRenderingPrototype((String)renderingPrototype);
                            }
                            String[] strArr = (String[])metaData;
                            gs.setModel(new DefaultListModel(strArr));
                            if(value != null) {
                                int slen = strArr.length;
                                for(int iter = 0 ; iter < slen ; iter++) {
                                    if(strArr[iter].equals(value)) {
                                        gs.getModel().setSelectedIndex(iter);
                                        break;
                                    }
                                }
                            }
                            showDialog(pickerDlg, gs);
                            value = gs.getValue();
                            break;
                        case Display.PICKER_TYPE_DATE:
                            DateSpinner ds = new DateSpinner();
                            cld.setTime((Date)value);
                            ds.setStartYear(1900);
                            ds.setCurrentDay(cld.get(Calendar.DAY_OF_MONTH));
                            ds.setCurrentMonth(cld.get(Calendar.MONTH) + 1);
                            ds.setCurrentYear(cld.get(Calendar.YEAR));
                            showDialog(pickerDlg, ds);
                            cld.set(Calendar.DAY_OF_MONTH, ds.getCurrentDay());
                            cld.set(Calendar.MONTH, ds.getCurrentMonth() - 1);
                            cld.set(Calendar.YEAR, ds.getCurrentYear());
                            value = cld.getTime();
                            break;
                        case Display.PICKER_TYPE_TIME:
                            int v = ((Integer)value).intValue();
                            int hour = v / 60;
                            int minute = v % 60;
                            TimeSpinner ts = new TimeSpinner();
                            ts.setShowMeridiem(isShowMeridiem());
                            if(showMeridiem && hour > 12) {
                                ts.setCurrentMeridiem(true);
                                ts.setCurrentHour(hour - 12);
                            } else {
                                ts.setCurrentHour(hour);
                            }
                            ts.setCurrentMinute(minute);
                            showDialog(pickerDlg, ts);
                            if(isShowMeridiem() && ts.isCurrentMeridiem()) {
                                hour = ts.getCurrentHour() + 12;
                            } else {
                                hour = ts.getCurrentHour();
                            }
                            value = new Integer(hour * 60 + ts.getCurrentMinute());
                            break;
                        case Display.PICKER_TYPE_DATE_AND_TIME:
                            DateTimeSpinner dts = new DateTimeSpinner();
                            cld.setTime((Date)value);
                            dts.setCurrentDate((Date)value);
                            dts.setShowMeridiem(isShowMeridiem());
                            if(isShowMeridiem() && dts.isCurrentMeridiem()) {
                                dts.setCurrentHour(cld.get(Calendar.HOUR));
                            } else {
                                dts.setCurrentHour(cld.get(Calendar.HOUR_OF_DAY));
                            }
                            dts.setCurrentMinute(cld.get(Calendar.MINUTE));
                            showDialog(pickerDlg, dts);
                            cld.setTime(dts.getCurrentDate());
                            if(isShowMeridiem() && dts.isCurrentMeridiem()) {
                                cld.set(Calendar.AM_PM, Calendar.PM);
                                cld.set(Calendar.HOUR, dts.getCurrentHour());
                            } else {
                                cld.set(Calendar.HOUR_OF_DAY, dts.getCurrentHour());
                            }
                            cld.set(Calendar.MINUTE, dts.getCurrentMinute());
                            value = cld.getTime();
                            break;
                    }
                    updateValue();
                }
            }
            
            private void showDialog(Dialog pickerDlg, Component c) {
                pickerDlg.addComponent(BorderLayout.CENTER, c);
                if(Display.getInstance().isTablet()) {
                    pickerDlg.showPopupDialog(Picker.this);
                } else {
                    Button ok = new Button(new Command("OK"));
                    pickerDlg.addComponent(BorderLayout.SOUTH, ok);
                    pickerDlg.show();
                }
            }
        });
        updateValue();
    }
    
    /**
     * Sets the type of the picker to one of Display.PICKER_TYPE_DATE, Display.PICKER_TYPE_DATE_AND_TIME, Display.PICKER_TYPE_STRINGS or
     * Display.PICKER_TYPE_TIME
     * @param type the type
     */
    public void setType(int type) {
        this.type = type;
        switch(type) {
            case Display.PICKER_TYPE_DATE:
            case Display.PICKER_TYPE_DATE_AND_TIME:
                if(!(value instanceof Date)) {
                    value = new Date();
                }
                break;
            case Display.PICKER_TYPE_STRINGS:
                if(!Util.instanceofObjArray(value)) {
                    setStrings(new String[] {" "});
                }
                break;
            case Display.PICKER_TYPE_TIME:
                if(!(value instanceof Integer)) {
                    setTime(0);
                }
                break;
        }
    }

    /**
     * Returns the type of the picker
     * @return one of Display.PICKER_TYPE_DATE, Display.PICKER_TYPE_DATE_AND_TIME, Display.PICKER_TYPE_STRINGS or
     * Display.PICKER_TYPE_TIME
     */
    public int getType() {
        return type;
    }
    
    /**
     * Returns the date, this value is used both for type date/date and time. Notice that this 
     * value isn't used for time
     * @return the date object
     */
    public Date getDate() {
        return (Date)value;
    }
    
    /**
     * Sets the date, this value is used both for type date/date and time. Notice that this 
     * value isn't used for time. Notice that this value will have no effect if the picker
     * is currently showing.
     * 
     * @param d the new date
     */
    public void setDate(Date d) {
        value = d;
        updateValue();
    }
    
    private String twoDigits(int i) {
        if(i < 10) {
            return "0" + i;
        }
        return "" + i;
    }
    
    /**
     * Sets the string entries for the string picker
     * @param strs string array
     */
    public void setStrings(String... strs) {
        int slen = strs.length;
        for (int i = 0; i < slen; i++) {
            String str = strs[i];
            strs[i] = getUIManager().localize(str, str);
        }
        metaData = strs;
        
        if(!(value instanceof String)) {
            value = null;
        }
        updateValue();
    }
    
    /**
     * Returns the String array matching the metadata
     * @return a string array
     */
    public String[] getStrings() {
        return (String[])metaData;
    }
    
    /**
     * Sets the current value in a string array picker
     * @param str the current value
     */
    public void setSelectedString(String str) {
        value = str;
        updateValue();
    }
    
    /**
     * Returns the current string
     * @return the selected string
     */
    public String getSelectedString() {
        return (String) value;
    }
    
    /**
     * Updates the display value of the picker, subclasses can override this to invoke 
     * set text with the right value
     */
    protected void updateValue() {
        if(value == null) {
            setText("...");
            return;
        }
        
        if(getFormatter() != null) {
            setText(formatter.format(value));
            return;
        }
        
        switch(type) {
            case Display.PICKER_TYPE_STRINGS:
                value = getUIManager().localize(value.toString(), value.toString());
                setText(value.toString());
                break;
            case Display.PICKER_TYPE_DATE:
                setText(L10NManager.getInstance().formatDateShortStyle((Date)value));
                break;
            case Display.PICKER_TYPE_TIME:
                int v = ((Integer)value).intValue();
                int hour = v / 60;
                int minute = v % 60;
                if(showMeridiem) {
                    String text;
                    if(hour >= 12) {
                        text = "pm";
                    } else {
                        text = "am";
                    }
                    setText(twoDigits(hour % 13 + 1) + ":" + twoDigits(minute) + text);
                } else {
                    setText(twoDigits(hour) + ":" + twoDigits(minute));
                }
                break;
            case Display.PICKER_TYPE_DATE_AND_TIME:
                setText(L10NManager.getInstance().formatDateTimeShort((Date)value));
                break;
        }
    }
    
    /**
     * This value is only used for time type and is ignored in the case of date and time where
     * both are embedded within the date.
     * @param time the time value as minutes since midnight e.g. 630 is 10:30am
     */
    public void setTime(int time) {
        value = new Integer(time);
        updateValue();
    }

    /**
     * Convenience method equivalent to invoking setTime(hour * 60 + minute);
     * @param hour the hour in 24hr format
     * @param minute the minute within the hour
     */
    public void setTime(int hour, int minute) {
        setTime(hour * 60 + minute);
    }
    
    /**
     * This value is only used for time type and is ignored in the case of date and time where
     * both are embedded within the date.
     * 
     * @return the time value as minutes since midnight e.g. 630 is 10:30am
     */
    public int getTime() {
        return ((Integer)value).intValue();
    }

    /**
     * Indicates whether hours should be rendered as AM/PM or 24hr format
     * @return the showMeridiem
     */
    public boolean isShowMeridiem() {
        return showMeridiem;
    }

    /**
     * Indicates whether hours should be rendered as AM/PM or 24hr format
     * @param showMeridiem the showMeridiem to set
     */
    public void setShowMeridiem(boolean showMeridiem) {
        this.showMeridiem = showMeridiem;
        updateValue();
    }

    /**
     * When using a lightweight spinner this will be used as the rendering prototype
     * @return the renderingPrototype
     */
    public Object getRenderingPrototype() {
        return renderingPrototype;
    }

    /**
     * When using a lightweight spinner this will be used as the rendering prototype
     * @param renderingPrototype the renderingPrototype to set
     */
    public void setRenderingPrototype(Object renderingPrototype) {
        this.renderingPrototype = renderingPrototype;
    }

    /**
     * Allows us to define a date format for the display of dates/times
     * @return the defined formatter
     */
    public SimpleDateFormat getFormatter() {
        return formatter;
    }

    /**
     * Allows us to define a date format for the display of dates/times
     * 
     * @param formatter the new formatter
     */
    public void setFormatter(SimpleDateFormat formatter) {
        this.formatter = formatter;
        updateValue();
    }
    
    /**
     * The preferred width of the popup dialog for the picker.  This will only 
     * be used on devices where the popup width and height are configurable, such 
     * as the iPad or tablets.  On iPhone, the picker always spans the width of the 
     * screen along the bottom.
     * @param width The preferred width of the popup.
     */
    public void setPreferredPopupWidth(int width) {
        this.preferredPopupWidth = width;
    }
    
    /**
     * The preferred height of the popup dialog for the picker.  This will only 
     * be used on devices where the popup width and height are configurable, such 
     * as the iPad or tablets.  On iPhone, the picker always spans the width of the 
     * screen along the bottom.
     * @param width The preferred width of the popup.
     */
    public void setPreferredPopupHeight(int height) {
        this.preferredPopupHeight = height;
    }
    
    /**
     * The preferred width of the popup dialog. This will only 
     * be used on devices where the popup width and height are configurable, such 
     * as the iPad or tablets.  On iPhone, the picker always spans the width of the 
     * screen along the bottom. 
     * @return 
     */
    public int getPreferredPopupWidth() {
        return preferredPopupWidth;
    }
    
    /**
     * The preferred height of the popup dialog.  This will only 
     * be used on devices where the popup width and height are configurable, such 
     * as the iPad or tablets.  On iPhone, the picker always spans the width of the 
     * screen along the bottom.
     * @return 
     */
    public int getPreferredPopupHeight() {
        return preferredPopupHeight;
    }
}
