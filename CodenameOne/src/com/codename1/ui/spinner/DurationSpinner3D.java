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
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/**
 * A duration Spinner widget  Used by the Picker in lightweight mode.
 * @author Steve Hannah
 */
class DurationSpinner3D extends Container implements InternalPickerWidget {
    public static final int FIELD_YEAR=0;
    public static final int FIELD_MONTH=1;
    public static final int FIELD_DAY=2;
    public static final int FIELD_HOUR=4;
    public static final int FIELD_MINUTE=8;
    public static final int FIELD_SECOND=16;
    public static final int FIELD_MILLISECOND=32;
    
    private Spinner3D days, hours, minutes, seconds, milliseconds;
    private final boolean includeDays, includeHours, includeMinutes, includeSeconds, includeMilliseconds;
    
    public DurationSpinner3D(int fields) {

        includeDays = (fields & FIELD_DAY) != 0;
        includeHours = (fields & FIELD_HOUR) != 0;
        includeMinutes = (fields & FIELD_MINUTE) != 0;
        includeSeconds = (fields & FIELD_SECOND) != 0;
        includeMilliseconds = (fields & FIELD_MILLISECOND) != 0;
        init();
    }
    
    private void init() {
        setLayout(new BorderLayout());
        Container wrapper = new Container(new LayeredLayout());
        
        Container box = new Container(BoxLayout.x());
        UIManager uim = UIManager.getInstance();
        
        Style s = null;

        if (includeDays) {
            days = Spinner3D.create(0, 1000, 0, 1);
            days.setPreferredW(new Label("000", "Spinner3DRow").getPreferredW());
            s = Style.createProxyStyle(days.getRowStyle(), days.getSelectedRowStyle());
            s.setAlignment(Component.RIGHT);
            box.add(days);
            box.add(new Label(uim.localize("day", "day")));
        }
        if (includeHours) {
            hours = Spinner3D.create(0, includeDays ? 24 : 1000, 0, 1);
            hours.setPreferredW(new Label("000", "Spinner3DRow").getPreferredW());
            s = Style.createProxyStyle(hours.getRowStyle(), hours.getSelectedRowStyle());
            s.setAlignment(Component.RIGHT);
            box.add(hours);
            box.add(new Label(uim.localize("hour", "hour")));
        }
        if (includeMinutes) {
            minutes = Spinner3D.create(0, includeHours ? 59 : 1000, 0, 1);
            minutes.setPreferredW(new Label("000", "Spinner3DRow").getPreferredW());
            s = Style.createProxyStyle(minutes.getRowStyle(), minutes.getSelectedRowStyle());
            s.setAlignment(Component.RIGHT);
            box.add(minutes);
            box.add(new Label(uim.localize("min", "min")));
        }
        if (includeSeconds) {
            seconds = Spinner3D.create(0, includeMinutes ? 59 : 1000, 0, 1);
            seconds.setPreferredW(new Label("0000", "Spinner3DRow").getPreferredW());
            s = Style.createProxyStyle(seconds.getRowStyle(), seconds.getSelectedRowStyle());
            s.setAlignment(Component.RIGHT);
            box.add(seconds);
            box.add(new Label(uim.localize("sec", "sec")));
        }
        if (includeMilliseconds) {
            milliseconds = Spinner3D.create(0, 1000, 0, 1);
            milliseconds.setPreferredW(new Label("0000", "Spinner3DRow").getPreferredW());
            s = Style.createProxyStyle(milliseconds.getRowStyle(), milliseconds.getSelectedRowStyle());
            s.setAlignment(Component.RIGHT);
            box.add(milliseconds);
            box.add(new Label("ms", "ms"));
        }
        
        wrapper.add(box);
        LayeredLayout ll = (LayeredLayout)wrapper.getLayout();
        ll.setInsets(box, "0 auto 0 auto");
        add(BorderLayout.CENTER, wrapper);
    }

    @Override
    public void setValue(Object value) {
        long l = (Long)value;
        if (days != null) {
            long DAY = (1000l * 60l * 60l * 24l);
            long numDays = l / DAY;
            days.setValue((int)numDays);
            l -= DAY * numDays;
        }
        
        if (hours != null) {
            long HOUR = (1000l * 60l * 60l);
            long numHours = l / HOUR;
            hours.setValue((int)numHours);
            l -= HOUR * numHours;
        }
        
        if (minutes != null) {
            long MINUTE = (1000l * 60l);
            long numMinutes = l / MINUTE;
            minutes.setValue((int)numMinutes);
            l -= MINUTE * numMinutes;
        }
        
        if (seconds != null) {
            long SECOND = (1000l);
            long numSeconds = l / SECOND;
            seconds.setValue((int)numSeconds);
            l -= SECOND * numSeconds;
        }
        
        if (milliseconds != null) {
            milliseconds.setValue((int)l);
        }
    }

    @Override
    public Object getValue() {
        long l = 0l;
        if (days != null) {
            long DAY = (1000l * 60l * 60l * 24l);
            long numDays = ((Integer)days.getValue()).intValue();
            l += DAY * numDays;
        }
        
        if (hours != null) {
            long HOUR = (1000l * 60l * 60l);
            long numHours = ((Integer)hours.getValue()).intValue();
            l += HOUR * numHours;
        }
        
        if (minutes != null) {
            long MINUTE = (1000l * 60l);
            long numMinutes = ((Integer)minutes.getValue()).intValue();
            
            l += MINUTE * numMinutes;
        }
        
        if (seconds != null) {
            long SECOND = (1000l);
            long numSeconds = ((Integer)seconds.getValue()).intValue();
            l += SECOND * numSeconds;
        }
        
        if (milliseconds != null) {
            l += ((Integer)milliseconds.getValue());
        }
        return l;
    }
}
