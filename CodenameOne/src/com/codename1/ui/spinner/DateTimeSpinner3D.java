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
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

import java.util.Calendar;
import java.util.Date;

import static com.codename1.ui.CN.convertToPixels;

/// The date and time spinner extends the time spinner by allowing to pick a specific day as well
///
/// Used by Picker in lightweight mode.
///
/// @author Steve Hannah
class DateTimeSpinner3D extends Container implements InternalPickerWidget {
    private final Date today = new Date();
    private final int off;
    private final Container wrapper = new Container(BoxLayout.x());
    private Spinner3D date;
    private TimeSpinner3D time;
    private Date currentDate = today;
    private Date startDate = new Date(0);
    private Date endDate = new Date(System.currentTimeMillis() + 10000L * 24L * 60L * 60000L);
    private boolean markToday = true;
    private boolean includeYear;

    /// Default constructor
    public DateTimeSpinner3D(int minuteStep) {
        off = 0;
        initSpinner(minuteStep);
    }

    public DateTimeSpinner3D() {
        this(TimeSpinner3D.DEFAULT_MINUTE_STEP);
    }

    void initSpinner(int minuteStep) {
        if (date == null) {
            date = Spinner3D.createDate(startDate.getTime() + off, endDate.getTime() + off, currentDate.getTime());
            date.setPreferredW((int) (new Label("Thu Dec 27", "Spinner3DRow").getPreferredW() * 1.5f));
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
        size.setWidth((int) (l.getPreferredW() * 1.5f + convertToPixels(10f)));
        return size;
    }


    void addComponents() {
        if (date != null) {
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

    /// #### Returns
    ///
    /// the currentDate
    public Date getCurrentDate() {
        if (date != null) {
            Date dt = (Date) date.getValue();
            Calendar cld = Calendar.getInstance();
            cld.setTime(dt);
            cld.set(Calendar.HOUR_OF_DAY, 0);
            cld.set(Calendar.MINUTE, 0);
            cld.set(Calendar.SECOND, 0);
            cld.set(Calendar.MILLISECOND, 0);

            Integer minutesInDay = (Integer) time.getValue();
            if (minutesInDay == null) {
                minutesInDay = 0;
            }
            cld.setTime(new Date(cld.getTime().getTime() + minutesInDay * 60L * 1000L));
            return cld.getTime();
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
        if (time != null) {
            Calendar cld = Calendar.getInstance();
            cld.setTime(currentDate);

            Calendar zero = Calendar.getInstance();
            zero.setTime(currentDate);
            zero.set(Calendar.HOUR_OF_DAY, 0);
            zero.set(Calendar.MINUTE, 0);
            zero.set(Calendar.SECOND, 0);
            zero.set(Calendar.MILLISECOND, 0);

            int minutesInDay = (int) ((cld.getTime().getTime() - zero.getTime().getTime()) / 60L / 1000L);
            time.setValue(minutesInDay);
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
        rebuildDate();
    }

    private void rebuildDate() {
        if (date != null) {
            long currTime = Math.max(startDate.getTime() + off, Math.min(endDate.getTime() + off, currentDate.getTime() + off));
            date.setModel(new SpinnerDateModel(startDate.getTime() + off, endDate.getTime() + off, currTime));
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
        rebuildDate();
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
    }

    /// Checks if time is shown in 12 hour format with AM/PM selector.
    ///
    /// #### Returns
    ///
    /// True if time is in 12 hour format.  False otherwise.
    ///
    /// #### Since
    ///
    /// 6.0
    ///
    /// #### See also
    ///
    /// - #setShowMeridiem(boolean)
    public boolean isShowMeridiem() {
        if (time == null) {
            return false;
        }
        return time.isShowMeridiem();
    }

    /// Sets whether the time spinner should show times 12 hour format with an AM/PM selector.
    ///
    /// #### Parameters
    ///
    /// - `showMeridiem`: True to show times in 12 hour format (the default).  False to show in 24 hour format.
    ///
    /// #### Since
    ///
    /// 6.0
    ///
    /// #### See also
    ///
    /// - #isShowMeridiem()
    public void setShowMeridiem(boolean showMeridiem) {
        if (time == null) {
            return;
        }
        time.setShowMeridiem(showMeridiem);
    }

    /// Sets the hour range to show for the time selector.  Setting the range will automatically switch
    /// the time to 24 hour format.
    ///
    /// #### Parameters
    ///
    /// - `min`: The minimum hour to display (0-24).  -1 for no limit.
    ///
    /// - `max`: The max hour to display (0-24).  -1 for no limit.
    ///
    /// #### Since
    ///
    /// 6.0
    ///
    /// #### See also
    ///
    /// - #getMinHour()
    ///
    /// - #getMaxHour()
    public void setHourRange(int min, int max) {
        if (time == null) {
            return;
        }
        if (min >= 0 && max > min && isShowMeridiem()) {
            time.setShowMeridiem(false);
        }
        time.setHourRange(min, max);

    }

    /// Gets the minimum hour to display.  Default -1 (for no limit).
    ///
    /// #### Returns
    ///
    /// Min hour (0-24) or -1 for no limit.
    ///
    /// #### Since
    ///
    /// 6.0
    ///
    /// #### See also
    ///
    /// - #getMaxHour()
    ///
    /// - #setHourRange(int, int)
    public int getMinHour() {
        if (time == null) {
            return -1;
        }
        return time.getMinHour();
    }

    /// Gets the maximum hour to display.  Default -1 (for no limit).
    ///
    /// #### Returns
    ///
    /// Max hour (0-24) or -1 for no limit.
    ///
    /// #### Since
    ///
    /// 6.0
    ///
    /// #### See also
    ///
    /// - #getMinHour()
    ///
    /// - #setHourRange(int, int)
    public int getMaxHour() {
        if (time == null) {
            return -1;
        }
        return time.getMaxHour();
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

    @Override
    public Object getValue() {
        return getCurrentDate();
    }

    @Override
    public void setValue(Object value) {
        setCurrentDate((Date) value);
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
