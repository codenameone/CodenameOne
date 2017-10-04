/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

/**
 * <p>
 * Date widget for selecting a date/time value.<br>
 * To localize strings for month names use the values "Calendar.Month" using the
 * 3 first characters of the month name in the resource localization e.g.
 * "{@code Calendar.Jan}", "{@code Calendar.Feb}" etc...<br>
 * To localize strings for day names use the values "Calendar.Day" in the
 * resource localization e.g. "{@code Calendar.Sunday}",
 * "{@code Calendar.Monday}" etc...</p>
 *
 * <p>
 * Note that we recommend using the {@link com.codename1.ui.spinner.Picker}
 * class which is superior when running on the device for most use cases.
 * </p>
 * <script src="https://gist.github.com/codenameone/8f520493f7681b5d16a3.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-calendar.png" alt="Default calendar look" />
 *
 * @author Iddo Ari, Shai Almog
 */
public class Calendar extends Container {

    /**
     * When set to true days will be rendered as 2 digits with 0 preceding
     * single digit days
     */
    private boolean twoDigitMode;
    private ComboBox month;
    private ComboBox year;
    private MonthView mv;
    private Label dateLabel;
    private static final String[] MONTHS = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private static final String[] DAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private static final String[] LABELS = {"Su", "M", "Tu", "W", "Th", "F", "Sa"};
    static final long MINUTE = 1000 * 60;
    static final long HOUR = MINUTE * 60;
    static final long DAY = HOUR * 24;
    static final long WEEK = DAY * 7;
    private EventDispatcher dispatcher = new EventDispatcher();
    private EventDispatcher dataChangedListeners = new EventDispatcher();
    private EventDispatcher monthChangedListeners = new EventDispatcher();
    private long[] dates = new long[42];
    private boolean changesSelectedDateEnabled = true;
    private TimeZone tmz;
    private long SELECTED_DAY = -1;
    private Collection<Date> selectedDays = new ArrayList<Date>();
    private boolean multipleSelectionEnabled = false;
    private String selectedDaysUIID = "CalendarMultipleDay";
    private Map<String, Collection<Date>> highlightGroup = new HashMap<String, Collection<Date>>();
    private ArrayList<ActionListener> dayListeners = new ArrayList<ActionListener>();

    /**
     * Creates a new instance of Calendar set to the given date based on time
     * since epoch (the java.util.Date convention)
     *
     * @param time time since epoch
     */
    public Calendar(long time) {
        this(time, java.util.TimeZone.getDefault());
    }

    /**
     * Constructs a calendar with the current date and time
     */
    public Calendar() {
        this(System.currentTimeMillis());
    }

    /**
     * Creates a new instance of Calendar set to the given date based on time
     * since epoch (the java.util.Date convention)
     *
     * @param time time since epoch
     * @param tmz a reference timezone
     */
    public Calendar(long time, TimeZone tmz) {
        this(time, java.util.TimeZone.getDefault(), null, null);
    }

    /**
     * Constructs a calendar with the current date and time with left and right
     * images set
     *
     * @param leftArrowImage an image for calendar left arrow
     * @param rightArrowImage an image for calendar right arrow
     */
    public Calendar(Image leftArrowImage, Image rightArrowImage) {
        this(System.currentTimeMillis(), java.util.TimeZone.getDefault(), leftArrowImage, rightArrowImage);
    }

    /**
     * Creates a new instance of Calendar set to the given date based on time
     * since epoch (the java.util.Date convention)
     *
     * @param time time since epoch
     * @param tmz a reference timezone
     * @param leftArrowImage an image for calendar left arrow
     * @param rightArrowImage an image for calendar right arrow
     */
    public Calendar(long time, TimeZone tmz, Image leftArrowImage, Image rightArrowImage) {
        super(new BorderLayout());
        this.tmz = tmz;
        setUIID("Calendar");
        mv = new MonthView(time);

        Image leftArrow = leftArrowImage != null ? leftArrowImage : UIManager.getInstance().getThemeImageConstant("calendarLeftImage");
        Image rightArrow = rightArrowImage != null ? rightArrowImage : UIManager.getInstance().getThemeImageConstant("calendarRightImage");
        if (leftArrow != null && rightArrow != null) {
            final Button left = new Button(leftArrow, "CalendarLeft");
            final Button right = new Button(rightArrow, "CalendarRight");
            ActionListener progress = new ActionListener() {
                private boolean lock = false;

                public void actionPerformed(ActionEvent evt) {
                    if (lock) {
                        return;
                    }
                    lock = true;
                    int month = mv.getMonth();
                    int year = mv.getYear();
                    if (evt.getSource() == left) {
                        month--;
                        if (month < java.util.Calendar.JANUARY) {
                            month = java.util.Calendar.DECEMBER;
                            year--;
                        }
                    } else {
                        month++;
                        if (month > java.util.Calendar.DECEMBER) {
                            month = java.util.Calendar.JANUARY;
                            year++;
                        }
                    }
                    boolean tran = UIManager.getInstance().isThemeConstant("calTransitionBool", true);
                    if (tran) {
                        Transition cm;
                        if (UIManager.getInstance().isThemeConstant("calTransitionVertBool", false)) {
                            cm = CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, evt.getSource() == left, 300);
                        } else {
                            cm = CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, evt.getSource() == left, 300);
                        }
                        MonthView newMv = new MonthView(mv.currentDay);
                        newMv.setMonth(year, month);
                        replaceAndWait(mv, newMv, cm);
                        mv = newMv;
                        newMv.fireActionEvent();
                    } else {
                        mv.setMonth(year, month);
                        componentChanged();
                    }
                    dateLabel.setText(getLocalizedMonth(month) + " " + year);
                    lock = false;
                }
            };
            left.addActionListener(progress);
            right.addActionListener(progress);

            Container dateCnt = new Container(new BorderLayout());
            dateCnt.setUIID("CalendarDate");
            dateLabel = new Label();
            dateLabel.setUIID("CalendarDateLabel");
            dateLabel.setText(getLocalizedMonth(mv.getMonth()) + " " + mv.getYear());

            dateCnt.addComponent(BorderLayout.CENTER, dateLabel);
            dateCnt.addComponent(BorderLayout.EAST, right);
            dateCnt.addComponent(BorderLayout.WEST, left);

            addComponent(BorderLayout.NORTH, dateCnt);
        } else {
            month = new ComboBox();
            year = new ComboBox();
            Vector months = new Vector();
            for (int i = 0; i < MONTHS.length; i++) {
                months.addElement("" + getLocalizedMonth(i));
            }
            ListModel monthsModel = new DefaultListModel(months);
            int selected = months.indexOf(getLocalizedMonth(mv.getMonth()));
            month.setModel(monthsModel);
            month.setSelectedIndex(selected);
            month.addActionListener(mv);

            java.util.Calendar cal = java.util.Calendar.getInstance(tmz);
            cal.setTime(new java.util.Date(time));
            month.getStyle().setBgTransparency(0);
            int y = cal.get(java.util.Calendar.YEAR);
            Vector years = new Vector();
            for (int i = 2100; i >= 1900; i--) {
                years.addElement("" + i);
            }
            ListModel yearModel = new DefaultListModel(years);
            selected = years.indexOf("" + y);
            year.setModel(yearModel);
            year.setSelectedIndex(selected);
            year.getStyle().setBgTransparency(0);
            year.addActionListener(mv);
            Container cnt = new Container(new BoxLayout(BoxLayout.X_AXIS));
            cnt.setRTL(false);

            Container dateCnt = new Container(new BoxLayout(BoxLayout.X_AXIS));
            dateCnt.setUIID("CalendarDate");
            dateCnt.addComponent(month);
            dateCnt.addComponent(year);
            cnt.addComponent(dateCnt);

            Container upper = new Container(new FlowLayout(Component.CENTER));
            upper.addComponent(cnt);

            addComponent(BorderLayout.NORTH, upper);
        }
        addComponent(BorderLayout.CENTER, mv);
    }

    /**
     * Returns the time for the current calendar.
     *
     * @return the time for the current calendar.
     */
    public long getSelectedDay() {
        return mv.getSelectedDay();
    }

    private String getLocalizedMonth(int i) {
        Map<String, String> t = getUIManager().getBundle();
        String text = MONTHS[i];
        if (t != null) {
            Object o = t.get("Calendar." + text);
            if (o != null) {
                text = (String) o;
            }
        }
        return text;
    }

    void componentChanged() {
        java.util.Calendar cal = java.util.Calendar.getInstance(tmz);
        cal.set(java.util.Calendar.YEAR, mv.getYear());
        cal.set(java.util.Calendar.MONTH, mv.getMonth());
        cal.set(java.util.Calendar.DAY_OF_MONTH, mv.getDayOfMonth());
        if (month != null) {
            month.getParent().revalidate();
        }
    }

    /**
     * Return the date object matching the current selection
     *
     * @return the date object matching the current selection
     */
    public Date getDate() {
        return new Date(mv.getSelectedDay());
    }

    /**
     * Sets the current date in the view and the selected date to be the same.
     *
     * @param d new date
     */
    public void setDate(Date d) {
        mv.setSelectedDay(d.getTime());
        mv.setCurrentDay(SELECTED_DAY, true);
        componentChanged();
    }

    /**
     * Sets the Calendar min and max years
     *
     * @param minYear the min year
     * @param maxYear the max year
     */
    public void setYearRange(int minYear, int maxYear) {
        if (minYear > maxYear) {
            throw new IllegalArgumentException("Max year should be bigger than or equal to min year!");
        }
        //The year combobox may not exist in the current context
        if (year != null) {
            Object previouslySelectedYear = year.getSelectedItem();
            Vector years = new Vector();
            for (int i = maxYear; i >= minYear; i--) {
                years.addElement("" + i);
            }
            ListModel yearModel = new DefaultListModel(years);
            year.setModel(yearModel);
            if (years.contains(previouslySelectedYear)) {
                year.setSelectedItem(previouslySelectedYear);
            }
        }
    }

    /**
     * This method sets the Calendar selected day
     *
     * @param d the selected day
     */
    public void setSelectedDate(Date d) {
        mv.setSelectedDay(d.getTime());
        mv.setCurrentDay(SELECTED_DAY, true);
        componentChanged();
    }

    /**
     * Sets the Calendar view on the given date, only the the month and year are
     * being considered.
     *
     * @param d the date to set the calendar view on.
     */
    public void setCurrentDate(Date d) {
        mv.setCurrentDay(d.getTime(), true);
        componentChanged();
    }

    /**
     * Returns the currently viewed date (as opposed to the selected date)
     *
     * @return the currently viewed date
     */
    public Date getCurrentDate() {
        return new Date(mv.getCurrentDay());
    }

    /**
     * Sets the Calendar timezone, if not specified Calendar will use the
     * default timezone
     *
     * @param tmz the timezone
     */
    public void setTimeZone(TimeZone tmz) {
        this.tmz = tmz;
    }

    /**
     * Gets the Calendar timezone
     *
     * @return Calendar TimeZone
     */
    public TimeZone getTimeZone() {
        return tmz;
    }

    /**
     * Sets the selected style of the month view component within the calendar
     *
     * @param s style for the month view
     */
    public void setMonthViewSelectedStyle(Style s) {
        mv.setSelectedStyle(s);
    }

    /**
     * Sets the un selected style of the month view component within the
     * calendar
     *
     * @param s style for the month view
     */
    public void setMonthViewUnSelectedStyle(Style s) {
        mv.setUnselectedStyle(s);
    }

    /**
     * Gets the selected style of the month view component within the calendar
     *
     * @return the style of the month view
     */
    public Style getMonthViewSelectedStyle() {
        return mv.getSelectedStyle();
    }

    /**
     * Gets the un selected style of the month view component within the
     * calendar
     *
     * @return the style of the month view
     */
    public Style getMonthViewUnSelectedStyle() {
        return mv.getUnselectedStyle();
    }

    /**
     * Fires when a change is made to the month view of this component
     *
     * @param l listener to add
     */
    public void addActionListener(ActionListener l) {
        mv.addActionListener(l);
    }

    /**
     * Fires when a change is made to the month view of this component
     *
     * @param l listener to remove
     */
    public void removeActionListener(ActionListener l) {
        mv.removeActionListener(l);
    }

    /**
     * Fires when a new month is selected
     *
     * @param l listener to add
     */
    public void addMonthChangedListener(ActionListener l) {
        mv.addMonthChangedListener(l);
    }

    /**
     * Fires when a new month is selected
     *
     * @param l listener to remove
     */
    public void removeMonthChangedListener(ActionListener l) {
        mv.removeMonthChangedListener(l);
    }

    /**
     * Adds an ActionListener to the day buttons. This is different from
     * {@code Calendar.addActionListener} and will only fire when an active day
     * is selected.
     *
     * @param l listener to add
     */
    public void addDayActionListener(ActionListener l) {
        mv.addDayActionListener(l);
    }

    /**
     * Removes ActionListener from day buttons
     *
     * @param l listener to remove
     */
    public void removeDayActionListener(ActionListener l) {
        mv.removeDayActionListener(l);
    }

    /**
     * Allows tracking selection changes in the calendar in real time
     *
     * @param l listener to add
     */
    public void addDataChangedListener(DataChangedListener l) {
        mv.addDataChangedListener(l);
    }

    /**
     * Allows tracking selection changes in the calendar in real time
     *
     * @param l listener to remove
     */
    public void removeDataChangedListener(DataChangedListener l) {
        mv.removeDataChangedListener(l);
    }

    /**
     * Allows tracking selection changes in the calendar in real time
     *
     * @param l listener to add
     * @deprecated use #addDataChangedListener(DataChangedListener) instead
     */
    public void addDataChangeListener(DataChangedListener l) {
        mv.addDataChangedListener(l);
    }

    /**
     * Allows tracking selection changes in the calendar in real time
     *
     * @param l listener to remove
     * @deprecated use #removeDataChangedListener(DataChangedListener) instead
     */
    public void removeDataChangeListener(DataChangedListener l) {
        mv.removeDataChangedListener(l);
    }

    /**
     * This flag determines if selected date can be changed by selecting an
     * alternative date
     *
     * @param changesSelectedDateEnabled if true pressing on a date will cause
     * the selected date to be changed to the pressed one
     */
    public void setChangesSelectedDateEnabled(boolean changesSelectedDateEnabled) {
        this.changesSelectedDateEnabled = changesSelectedDateEnabled;
    }

    /**
     * This flag determines if selected date can be changed by selecting an
     * alternative date
     *
     * @return true if enabled
     */
    public boolean isChangesSelectedDateEnabled() {
        return changesSelectedDateEnabled;
    }

    /**
     * This method creates the Day Button Component for the Month View
     *
     * @return a Button that corresponds to the Days Components
     * @deprecated override {@code createDayComponent()} instead
     */
    protected Button createDay() {
        Button day = new Button("", "CalendarDay");
        day.setAlignment(CENTER);
        day.setEndsWith3Points(false);
        day.setTickerEnabled(false);
        return day;
    }

    /**
     * This method creates the Day title Component for the Month View
     *
     * @param day the relevant day values are 0-6 where 0 is Sunday.
     * @return a Label that corresponds to the relevant Day
     */
    protected Label createDayTitle(int day) {
        String value = getUIManager().localize("Calendar." + DAYS[day], LABELS[day]);
        Label dayh = new Label(value, "CalendarTitle");
        dayh.setEndsWith3Points(false);
        dayh.setTickerEnabled(false);
        return dayh;
    }

    /**
     * This method updates the Button day.
     *
     * @param dayButton the button to be updated
     * @param year the current year
     * @param currentMonth the current month
     * @param day the new button day
     */
    protected void updateButtonDayDate(Component dayButton, int year, int currentMonth, int day) {
        if (dayButton instanceof Button) {
            updateButtonDayDate((Button) dayButton, currentMonth, day);
        }
    }

    /**
     * This method updates the Button day.
     *
     * @param dayButton the button to be updated
     * @param currentMonth the current month
     * @param day the new button day
     */
    protected void updateButtonDayDate(Component dayButton, int currentMonth, int day) {
        if (dayButton instanceof Button) {
            updateButtonDayDate((Button) dayButton, currentMonth, day);
        }
    }

    /**
     * This method updates the Button day.
     *
     * @param dayButton the button to be updated
     * @param year the current year
     * @param currentMonth the current month
     * @param day the new button day
     * @deprecated override the method that accepts a generic component
     */
    protected void updateButtonDayDate(Button dayButton, int year, int currentMonth, int day) {
        updateButtonDayDate(dayButton, currentMonth, day);
    }

    /**
     * This method updates the Button day.
     *
     * @param dayButton the button to be updated
     * @param currentMonth the current month
     * @param day the new button day
     * @deprecated override the method that accepts a generic component
     */
    protected void updateButtonDayDate(Button dayButton, int currentMonth, int day) {
        if (twoDigitMode) {
            if (day < 10) {
                dayButton.setText("0" + day);
            } else {
                dayButton.setText("" + day);
            }
        } else {
            if (day < 10) {
                dayButton.setText(" " + day + " "); //To match the space occupied by 2 digits buttons
            } else {
                dayButton.setText("" + day);
            }
        }
    }

    /**
     * When set to true days will be rendered as 2 digits with 0 preceding
     * single digit days
     *
     * @return the twoDigitMode
     */
    public boolean isTwoDigitMode() {
        return twoDigitMode;
    }

    /**
     * When set to true days will be rendered as 2 digits with 0 preceding
     * single digit days
     *
     * @param twoDigitMode the twoDigitMode to set
     */
    public void setTwoDigitMode(boolean twoDigitMode) {
        this.twoDigitMode = twoDigitMode;
    }

    /**
     * Gets the dates selected on the calendar or null if no date is selected
     *
     * @return the selected days
     */
    public Collection<Date> getSelectedDays() {
        return selectedDays;
    }

    /**
     * Sets the dates to be selected on the calendar
     *
     * @param selectedDays the multipleDateSelection to set
     */
    public void setSelectedDays(Collection<Date> selectedDays) {
        for (Date selectedDay : selectedDays) {
            java.util.Calendar cal = java.util.Calendar.getInstance(tmz);
            cal.setTime(selectedDay);
            cal.set(java.util.Calendar.HOUR, 1);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 1);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            this.selectedDays.add(cal.getTime());
        }
        selectedDaysUIID = "CalendarMultipleDay";
        mv.setCurrentDay(SELECTED_DAY, true);
        componentChanged();
    }

    /**
     * Sets the dates to be selected on the calendar with a custom uiid. To use
     * default uiid "{@code CalendarMultipleDay}", call this method without the
     * "{@code uiid} parameter"
     *
     * @param selectedDays the multipleDateSelection to set
     * @param uiid a custom uiid to be used in the dates selected
     */
    public void setSelectedDays(Collection<Date> selectedDays, String uiid) {
        for (Date selectedDay : selectedDays) {
            java.util.Calendar cal = java.util.Calendar.getInstance(tmz);
            cal.setTime(selectedDay);
            cal.set(java.util.Calendar.HOUR, 1);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 1);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            this.selectedDays.add(cal.getTime());
        }
        selectedDaysUIID = uiid;
        mv.setCurrentDay(SELECTED_DAY, true);
        componentChanged();
    }

    /**
     *
     * @return selectedDays uiid
     */
    public String getSelectedDaysUIID() {
        return selectedDaysUIID;
    }

    /**
     * Sets the selectedDays UIID to the given uiid. being considered.
     *
     * @param uiid the uiid to change to
     */
    public void setSelectedDaysUIID(String uiid) {
        this.selectedDaysUIID = uiid;
    }

    /**
     * Highlights a date on the calendar using the supplied uiid. (Selected
     * dates uiid takes precedence over highlighted dates uiid)
     *
     * @param date the date to be highlighted
     * @param uiid a custom uiid to be used in highlighting the date
     */
    public void highlightDate(Date date, String uiid) {
        java.util.Calendar cal = java.util.Calendar.getInstance(tmz);
        cal.setTime(date);
        cal.set(java.util.Calendar.HOUR, 1);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 1);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);

        if (!highlightGroup.isEmpty()) {
            Collection<Date> datesArray = new ArrayList<Date>();
            if (highlightGroup.containsKey(uiid)) {
                datesArray = highlightGroup.get(uiid);
            }
            datesArray.add(cal.getTime());
            highlightGroup.put(uiid, datesArray);
        } else {
            Collection<Date> datesArray = new ArrayList<Date>();
            datesArray.add(cal.getTime());
            highlightGroup.put(uiid, datesArray);
        }
        mv.setCurrentDay(SELECTED_DAY, true);
        componentChanged();
    }

    /**
     * Highlights dates on the calendar using the supplied uiid. (Selected dates
     * uiid takes precedence over highlighted dates uiid)
     *
     * @param dates the dates to be highlighted
     * @param uiid a custom uiid to be used in highlighting the dates
     */
    public void highlightDates(Collection<Date> dates, String uiid) {
        for (Date selectedDay : dates) {
            java.util.Calendar cal = java.util.Calendar.getInstance(tmz);
            cal.setTime(selectedDay);
            cal.set(java.util.Calendar.HOUR, 1);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 1);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);

            if (!highlightGroup.isEmpty()) {
                Collection<Date> datesArray = new ArrayList<Date>();
                if (highlightGroup.containsKey(uiid)) {
                    datesArray = highlightGroup.get(uiid);
                }
                datesArray.add(cal.getTime());
                highlightGroup.put(uiid, datesArray);
            } else {
                Collection<Date> datesArray = new ArrayList<Date>();
                datesArray.add(cal.getTime());
                highlightGroup.put(uiid, datesArray);
            }
        }
        mv.setCurrentDay(SELECTED_DAY, true);
        componentChanged();
    }

    /**
     * Un-highlights dates on the calendar by removing the highlighting uiid.
     * ({@code selectedDaysUIID} uiid will be applied to any of the dates that
     * are part of {@code selectedDays})
     *
     * @param dates the dates to be un-highlighted
     */
    public void unHighlightDates(Collection<Date> dates) {
        if (!highlightGroup.isEmpty()) {
            for (Date selectedDay : dates) {
                java.util.Calendar cal = java.util.Calendar.getInstance(tmz);
                cal.setTime(selectedDay);
                cal.set(java.util.Calendar.HOUR, 1);
                cal.set(java.util.Calendar.HOUR_OF_DAY, 1);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);

                Iterator it = highlightGroup.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Collection<Date>> entry = (Map.Entry) it.next();
                    if (entry.getValue().contains(cal.getTime())) {
                        entry.getValue().remove(cal.getTime());
                        if (entry.getValue().isEmpty()) {
                            it.remove();
                        }
                    }
                }
            }
            mv.setCurrentDay(SELECTED_DAY, true);
            componentChanged();
        }
    }

    /**
     * Un-highlights dates on the calendar by removing the highlighting uiid.
     * ({@code selectedDaysUIID} uiid will be applied to the date if it is part
     * of {@code selectedDays})
     *
     * @param date the date to be un-highlighted
     */
    public void unHighlightDate(Date date) {
        if (!highlightGroup.isEmpty()) {
            java.util.Calendar cal = java.util.Calendar.getInstance(tmz);
            cal.setTime(date);
            cal.set(java.util.Calendar.HOUR, 1);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 1);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);

            Iterator it = highlightGroup.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Collection<Date>> entry = (Map.Entry) it.next();
                if (entry.getValue().contains(cal.getTime())) {
                    entry.getValue().remove(cal.getTime());
                    if (entry.getValue().isEmpty()) {
                        it.remove();
                    }
                }
            }
            mv.setCurrentDay(SELECTED_DAY, true);
            componentChanged();
        }
    }

    /**
     * If true multiple days can be selected on a calendar and
     * "{@code getSelectedDays()}" will return the dates selected
     *
     * @return the multipleSelectionEnabled
     */
    public boolean isMultipleSelectionEnabled() {
        return multipleSelectionEnabled;
    }

    /**
     * When set to true multiple days can be selected on a calendar and
     * "{@code getSelectedDays()}" will return the dates selected
     *
     * @param multipleSelectionEnabled the multipleSelectionEnabled to set
     */
    public void setMultipleSelectionEnabled(boolean multipleSelectionEnabled) {
        this.multipleSelectionEnabled = multipleSelectionEnabled;
    }

    /**
     * Creates a day within the Calendar, this method is protected allowing
     * Calendar to be subclassed to replace the rendering logic of individual
     * day buttons.
     *
     * @return a button representing the day within the Calendar
     */
    protected Component createDayComponent() {
        return createDay();
    }

    /**
     * Since a day may be any component type, developers should override this
     * method to add support for binding the click listener to the given
     * component.
     *
     * @param l listener interface
     * @param cmp day component returned by createDayComponent()
     */
    protected void bindDayListener(Component cmp, ActionListener l) {
        if (cmp instanceof Button) {
            ((Button) cmp).addActionListener(l);
        }
    }

    /**
     * Since a day may be any component type, developers should override this
     * method to add support for removing the click listener from the given
     * component.
     *
     * @param l listener interface
     * @param cmp day component returned by createDayComponent()
     */
    private void unBindDayListener(Component cmp, ActionListener l) {
        if (cmp instanceof Button) {
            ((Button) cmp).removeActionListener(l);
        }
    }

    /**
     * Since a day may be any component type, developers should override this
     * method to add support for setting the displayed string.
     *
     * @param text the text set the component to
     * @param cmp day component returned by createDayComponent()
     */
    protected void setDayText(Component cmp, String text) {
        if (cmp instanceof Button) {
            ((Button) cmp).setText(text);
        }
    }

    /**
     * Since a day may be any component type, developers should override this
     * method to add support for removing the click listener from the given
     * component.
     *
     * @param cmp day component returned by createDayComponent
     * @return the day text
     */
    protected String getDayText(Component cmp) {
        if (cmp instanceof Button) {
            return ((Button) cmp).getText();
        }
        return null;
    }

    /**
     * Since a day may be any component type, developers should override this
     * method to add support for setting the right component's UIID.
     *
     * @param cmp day component returned by createDayComponent()
     * @param uiid the text set the component to
     */
    protected void setDayUIID(Component cmp, String uiid) {
        cmp.setUIID(uiid);
    }

    /**
     * Since a day may be any component type, developers should override this
     * method to add support for enabling or disabling the right component.
     *
     * @param cmp day component returned by createDayComponent()
     * @param enable the text set the component to
     */
    protected void setDayEnabled(Component cmp, boolean enable) {
        cmp.setEnabled(enable);
    }

    class MonthView extends Container implements ActionListener {

        long currentDay;
        private final Component[] components = new Component[42];
        private Component selected;
        private final Container titles;
        private final Container days;

        public long getCurrentDay() {
            return currentDay;
        }

        public MonthView(long time) {
            super(new BoxLayout(BoxLayout.Y_AXIS));
            setUIID("MonthView");
            titles = new Container(new GridLayout(1, 7));
            days = new Container(new GridLayout(6, 7));
            addComponent(titles);
            addComponent(days);
            if (UIManager.getInstance().isThemeConstant("calTitleDayStyleBool", false)) {
                titles.setUIID("CalendarTitleArea");
                days.setUIID("CalendarDayArea");
            }
            for (int iter = 0; iter < DAYS.length; iter++) {
                titles.addComponent(createDayTitle(iter));
            }
            for (int iter = 0; iter < components.length; iter++) {
                components[iter] = createDayComponent();
                days.add(components[iter]);
                if (iter <= 7) {
                    components[iter].setNextFocusUp(year);
                }
                bindDayListener(components[iter], this);
                for (ActionListener dayListener : dayListeners) {
                    bindDayListener(components[iter], dayListener);
                }
            }
            setCurrentDay(time);

        }

        public void setCurrentDay(long day) {
            setCurrentDay(day, false);
        }

        private void setCurrentDay(long day, boolean force) {
            repaint();
            java.util.Calendar cal = java.util.Calendar.getInstance(tmz);
            cal.setTime(new Date(currentDay));
            cal.set(java.util.Calendar.HOUR, 1);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 1);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);

            int yearOld = cal.get(java.util.Calendar.YEAR);
            int monthOld = cal.get(java.util.Calendar.MONTH);
            int dayOld = cal.get(java.util.Calendar.DAY_OF_MONTH);
            Date dateObject = new Date(day);
            cal.setTime(dateObject);
            cal.set(java.util.Calendar.HOUR, 1);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 1);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            int yearNew = cal.get(java.util.Calendar.YEAR);
            int monthNew = cal.get(java.util.Calendar.MONTH);
            int dayNew = cal.get(java.util.Calendar.DAY_OF_MONTH);
            if (month != null) {
                year.setSelectedItem("" + yearNew);
                month.setSelectedIndex(monthNew);
            } else {
                if (dateLabel != null) {
                    dateLabel.setText(getLocalizedMonth(monthNew) + " " + yearNew);
                }
            }

            if (yearNew != yearOld || monthNew != monthOld || dayNew != dayOld || force) {
                currentDay = cal.getTime().getTime();
                if (SELECTED_DAY == -1) {
                    SELECTED_DAY = currentDay;
                }
                int month = cal.get(java.util.Calendar.MONTH);
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
                long startDate = cal.getTime().getTime();
                int dow = cal.get(java.util.Calendar.DAY_OF_WEEK);
                cal.setTime(new Date(cal.getTime().getTime() - DAY));
                cal.set(java.util.Calendar.HOUR, 1);
                cal.set(java.util.Calendar.HOUR_OF_DAY, 1);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                int lastDay = cal.get(java.util.Calendar.DAY_OF_MONTH);
                int i = 0;
                if (dow > java.util.Calendar.SUNDAY) {
                    //last day of previous month

                    while (dow > java.util.Calendar.SUNDAY) {
                        cal.setTime(new Date(cal.getTime().getTime() - DAY));
                        dow = cal.get(java.util.Calendar.DAY_OF_WEEK);
                    }
                    int previousMonthSunday = cal.get(java.util.Calendar.DAY_OF_MONTH);
                    for (; i <= lastDay - previousMonthSunday; i++) {
                        setDayUIID(components[i], "CalendarDay");
                        setDayEnabled(components[i], false);
                        setDayText(components[i], "" + (previousMonthSunday + i));
                    }
                }
                //last day of current month
                cal.set(java.util.Calendar.MONTH, (month + 1) % 12);
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
                cal.setTime(new Date(cal.getTime().getTime() - DAY));

                lastDay = cal.get(java.util.Calendar.DAY_OF_MONTH);

                int j = i;
                for (; j < components.length && (j - i + 1) <= lastDay; j++) {
                    setDayEnabled(components[j], true);
                    dates[j] = startDate;
                    if (dates[j] == SELECTED_DAY) {
                        setDayUIID(components[j], "CalendarSelectedDay");
                        selected = components[j];
                    } else {
                        setDayUIID(components[j], "CalendarDay");
                    }

                    for (Map.Entry<String, Collection<Date>> entry : highlightGroup.entrySet()) {
                        if (entry.getValue().contains(new Date(dates[j]))) {
                            setDayUIID(components[j], entry.getKey());
                        }
                    }

                    if (multipleSelectionEnabled) {
                        if (selectedDays.contains(new Date(dates[j]))) {
                            setDayUIID(components[j], selectedDaysUIID);
                        }
                    }
                    updateButtonDayDate(components[j], yearNew, month, j - i + 1);
                    startDate += DAY;
                }
                int d = 1;
                for (; j < components.length; j++) {
                    setDayUIID(components[j], "CalendarDay");
                    setDayEnabled(components[j], false);
                    setDayText(components[j], "" + d++);
                }
            }
        }

        public int getDayOfMonth() {
            java.util.Calendar cal = java.util.Calendar.getInstance(tmz);
            cal.setTime(new Date(currentDay));
            return cal.get(java.util.Calendar.DAY_OF_MONTH);
        }

        public int getMonth() {
            java.util.Calendar cal = java.util.Calendar.getInstance(tmz);
            cal.setTime(new Date(currentDay));
            return cal.get(java.util.Calendar.MONTH);
        }

        public void incrementMonth() {
            int month = getMonth();
            month++;
            int year = getYear();
            if (month > java.util.Calendar.DECEMBER) {
                month = java.util.Calendar.JANUARY;
                year++;
            }
            setMonth(year, month);
        }

        private long getSelectedDay() {
            return SELECTED_DAY;
        }

        public void setSelectedDay(long selectedDay) {
            java.util.Calendar cal = java.util.Calendar.getInstance(tmz);
            cal.setTime(new Date(selectedDay));
            cal.set(java.util.Calendar.HOUR, 1);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 1);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            SELECTED_DAY = cal.getTime().getTime();
        }

        private void setMonth(int year, int month) {
            fireMonthChangedEvent();
            java.util.Calendar cal = java.util.Calendar.getInstance(tmz);
            cal.setTimeZone(TimeZone.getDefault());
            cal.set(java.util.Calendar.MONTH, month);
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
            cal.set(java.util.Calendar.YEAR, year);

            Date date = cal.getTime();
            long d = date.getTime();

            // if this is past the last day of the month (e.g. going from January 31st
            // to Febuary) we need to decrement the day until the month is correct
            while (cal.get(java.util.Calendar.MONTH) != month) {
                d -= DAY;
                cal.setTime(new Date(d));
            }
            setCurrentDay(d);
        }

        public void decrementMonth() {
            int month = getMonth();
            month--;
            int year = getYear();
            if (month < java.util.Calendar.JANUARY) {
                month = java.util.Calendar.DECEMBER;
                year--;
            }
            setMonth(year, month);
        }

        public int getYear() {
            java.util.Calendar cal = java.util.Calendar.getInstance(tmz);
            cal.setTime(new Date(currentDay));
            return cal.get(java.util.Calendar.YEAR);
        }

        public void addActionListener(ActionListener l) {
            dispatcher.addListener(l);
        }

        public void removeActionListener(ActionListener l) {
            dispatcher.removeListener(l);
        }

        public void addMonthChangedListener(ActionListener l) {
            monthChangedListeners.addListener(l);
        }

        public void removeMonthChangedListener(ActionListener l) {
            monthChangedListeners.removeListener(l);
        }

        public void addDayActionListener(ActionListener l) {
            dayListeners.add(l);
            for (Component cmp : components) {
                bindDayListener(cmp, l);
            }
        }

        public void removeDayActionListener(ActionListener l) {
            dayListeners.remove(l);
            for (Component cmp : components) {
                unBindDayListener(cmp, l);
            }
        }

        /**
         * Allows tracking selection changes in the calendar in real time
         *
         * @param l listener to add
         */
        public void addDataChangedListener(DataChangedListener l) {
            dataChangedListeners.addListener(l);
        }

        /**
         * Allows tracking selection changes in the calendar in real time
         *
         * @param l listener to remove
         */
        public void removeDataChangedListener(DataChangedListener l) {
            dataChangedListeners.removeListener(l);
        }

        protected void fireActionEvent() {
            componentChanged();
            super.fireActionEvent();
            dispatcher.fireActionEvent(new ActionEvent(Calendar.this, ActionEvent.Type.Calendar));
        }

        protected void fireMonthChangedEvent() {
            monthChangedListeners.fireActionEvent(new ActionEvent(Calendar.this, ActionEvent.Type.Calendar));
        }

        public void actionPerformed(ActionEvent evt) {
            Object src = evt.getSource();
            if (src instanceof ComboBox) {
                setMonth(Integer.parseInt((String) year.getSelectedItem()), month.getSelectedIndex());
                componentChanged();
                return;
            }
            if (changesSelectedDateEnabled) {
                for (int iter = 0; iter < components.length; iter++) {
                    boolean isContained = false;
                    if (components[iter] instanceof Container) {
                        isContained = ((Container) components[iter]).contains((Component) src);
                    }

                    if (src == components[iter] || isContained) {
                        System.out.println(src);
                        if (multipleSelectionEnabled) {
                            if (selectedDays.contains(new Date(dates[iter]))) {
                                if (SELECTED_DAY == dates[iter]) {
                                    setDayUIID(components[iter], "CalendarSelectedDay");
                                } else {
                                    setDayUIID(components[iter], "CalendarDay");
                                }
                                if (!highlightGroup.isEmpty()) {
                                    for (Map.Entry<String, Collection<Date>> entry : highlightGroup.entrySet()) {
                                        if (entry.getValue().contains(new Date(dates[iter]))) {
                                            setDayUIID(components[iter], entry.getKey());
                                            break;
                                        }
                                    }
                                }

                                selectedDays.remove(new Date(dates[iter]));
                            } else {
                                setDayUIID(components[iter], selectedDaysUIID);
                                selectedDays.add(new Date(dates[iter]));
                            }
                        } else {
                            if (selected != null) {
                                setDayUIID(selected, "CalendarDay");
                                java.util.Calendar cal = java.util.Calendar.getInstance(tmz);
                                cal.setTime(new Date(currentDay));
                                cal.set(java.util.Calendar.DAY_OF_MONTH, Integer.parseInt(getDayText(selected).trim()));
                                cal.set(java.util.Calendar.HOUR, 1);
                                cal.set(java.util.Calendar.HOUR_OF_DAY, 1);
                                cal.set(java.util.Calendar.MINUTE, 0);
                                cal.set(java.util.Calendar.SECOND, 0);
                                cal.set(java.util.Calendar.MILLISECOND, 0);
                                if (!highlightGroup.isEmpty()) {
                                    for (Map.Entry<String, Collection<Date>> entry : highlightGroup.entrySet()) {
                                        if (entry.getValue().contains(cal.getTime())) {
                                            setDayUIID(selected, entry.getKey());
                                            break;
                                        }
                                    }
                                }
                            }
                            setDayUIID(components[iter], "CalendarSelectedDay");

                            SELECTED_DAY = dates[iter];
                            selected = components[iter];
                        }
                        fireActionEvent();
                        if (!getComponentForm().isSingleFocusMode()) {
                            setHandlesInput(false);
                        }
                        revalidate();
                        return;
                    }
                }
            }
        }

    }
}
