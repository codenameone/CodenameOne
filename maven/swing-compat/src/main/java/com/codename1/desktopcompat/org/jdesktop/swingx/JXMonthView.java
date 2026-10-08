/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.java.awt.AWTEvent;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.org.jdesktop.swingx.calendar.CalendarUtils;
import com.codename1.desktopcompat.org.jdesktop.swingx.calendar.DateSelectionModel;
import com.codename1.desktopcompat.org.jdesktop.swingx.calendar.DateSelectionModel.SelectionMode;
import com.codename1.desktopcompat.org.jdesktop.swingx.calendar.DaySelectionModel;
import com.codename1.desktopcompat.org.jdesktop.swingx.event.DateSelectionEvent;
import com.codename1.desktopcompat.org.jdesktop.swingx.event.DateSelectionListener;
import com.codename1.desktopcompat.rt.CellTheme;
import com.codename1.desktopcompat.rt.Fonts;
import com.codename1.desktopcompat.rt.ScrollDelegate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

/// One month as a grid of days, with a selection of dates.
///
/// ## How it is drawn
///
/// The view paints itself in the colors of the Codename One theme: a
/// title row with the month and year and, when the view is traversable, an
/// arrow at each end; a row of day names; six rows of days. The eight rows
/// and seven columns divide the component's size evenly, and the preferred
/// size gives every box room for a finger on a touch screen.
///
/// ## Input
///
/// With a mouse a press selects the day under it and a drag extends the
/// selection in the interval modes; the release commits. Shift extends
/// from the last pressed day, and Control or Meta adds or removes a day in
/// the multiple interval mode. On a touch screen a tap selects and
/// commits, and a drag is handed to the enclosing scroll pane. The arrows
/// move a month. The arrow keys move the selected day, Page Up and Page
/// Down move a month, Enter commits and Escape cancels.
///
/// ## What differs from SwingX
///
///  - One month is shown whatever the preferred row and column counts,
///    which are recorded only, as are the zoomable flag and the week
///    number flag: there is no month zoom and no week number column.
///  - The first day of the week starts as Sunday whatever the locale, and
///    the locale is recorded only; month and day names come from the
///    device's date formatter.
///  - There is no UI delegate, and "today" is not advanced by a timer:
///    it is the day the view was created or last told.
///  - A day beyond the bounds or among the unselectable dates is drawn
///    dimmed and ignores the pointer.
public class JXMonthView extends JComponent {

    public static final String COMMIT_KEY = "monthViewCommit";
    public static final String CANCEL_KEY = "monthViewCancel";
    public static final String BOX_PADDING_X = "boxPaddingX";
    public static final String BOX_PADDING_Y = "boxPaddingY";
    public static final String DAYS_OF_THE_WEEK = "daysOfTheWeek";
    public static final String SELECTION_MODEL = "selectionModel";
    public static final String TRAVERSABLE = "traversable";
    public static final String FLAGGED_DATES = "flaggedDates";
    public static final String uiClassID = "MonthViewUI";
    public static final int DAYS_IN_WEEK = 7;
    public static final int MONTHS_IN_YEAR = 12;

    private static final int ROWS = 8;
    private static final int TOUCH_BOX = 40;

    private DateSelectionModel model;
    private final DateSelectionListener modelListener = new DateSelectionListener() {
        @Override
        public void valueChanged(DateSelectionEvent ev) {
            repaint();
        }
    };
    private Date firstDisplayedDay;
    private Date today;
    private final TreeSet<Date> flagged = new TreeSet<Date>();
    private boolean traversable;
    private boolean zoomable;
    private boolean showingLeadingDays;
    private boolean showingTrailingDays;
    private boolean showingWeekNumber;
    private boolean componentInputMapEnabled;
    private String[] daysOfTheWeek;
    private int boxPaddingX = 3;
    private int boxPaddingY = 3;
    private Color selectionBackground;
    private Color selectionForeground;
    private Color todayBackground;
    private Color monthStringBackground;
    private Color monthStringForeground;
    private Color daysOfTheWeekForeground;
    private Color flaggedDayForeground;
    private final Color[] dayForeground = new Color[DAYS_IN_WEEK + 1];
    private Insets monthStringInsets = new Insets(0, 0, 0, 0);
    private int preferredColumnCount = 1;
    private int preferredRowCount = 1;
    private Date anchor;
    private boolean pressTouch;
    private boolean pressOnDay;

    public JXMonthView() {
        this(null, null, null);
    }

    public JXMonthView(Locale locale) {
        this(null, null, locale);
    }

    public JXMonthView(Date firstDisplayedDay) {
        this(firstDisplayedDay, null, null);
    }

    public JXMonthView(Date firstDisplayedDay, DateSelectionModel model) {
        this(firstDisplayedDay, model, null);
    }

    /// Shows the month of `firstDisplayedDay`, or the current month for
    /// `null`, over `model`, or a new [DaySelectionModel] for `null`.
    public JXMonthView(Date firstDisplayedDay, DateSelectionModel model, Locale locale) {
        this.model = model != null ? model : new DaySelectionModel(locale);
        this.model.addDateSelectionListener(modelListener);
        Date now = new Date();
        today = startOfDay(now);
        this.firstDisplayedDay = startOfMonth(firstDisplayedDay != null ? firstDisplayedDay : now);
        setFocusable(true);
        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
    }

    // ------------------------------------------------------------ calendar

    private Date startOfDay(Date d) {
        return CalendarUtils.startOfDay(model.getCalendar(), d);
    }

    private Date startOfMonth(Date d) {
        Calendar c = model.getCalendar();
        c.setTime(d);
        CalendarUtils.startOfMonth(c);
        return c.getTime();
    }

    /// The first millisecond of the given day of the displayed month.
    private Date dayOfMonth(int day) {
        Calendar c = model.getCalendar();
        c.setTime(firstDisplayedDay);
        c.set(Calendar.DAY_OF_MONTH, day);
        CalendarUtils.startOfDay(c);
        return c.getTime();
    }

    private int daysInMonth() {
        Calendar c = model.getCalendar();
        c.setTime(firstDisplayedDay);
        CalendarUtils.endOfMonth(c);
        return c.get(Calendar.DAY_OF_MONTH);
    }

    /// How many boxes of the first week row come before day one.
    private int leadingBoxes() {
        Calendar c = model.getCalendar();
        c.setTime(firstDisplayedDay);
        int dow = c.get(Calendar.DAY_OF_WEEK);
        return (dow - model.getFirstDayOfWeek() + DAYS_IN_WEEK) % DAYS_IN_WEEK;
    }

    private Date addMonths(Date from, int months) {
        Calendar c = model.getCalendar();
        c.setTime(startOfMonth(from));
        c.add(Calendar.MONTH, months);
        CalendarUtils.startOfMonth(c);
        return c.getTime();
    }

    private Date addDays(Date from, int days) {
        Calendar c = model.getCalendar();
        c.setTime(from);
        c.add(Calendar.DAY_OF_MONTH, days);
        CalendarUtils.startOfDay(c);
        return c.getTime();
    }

    /// Records the locale in the selection model; names are not
    /// translated by it.
    public void setLocale(Locale locale) {
        model.setLocale(locale);
        repaint();
    }

    /// A calendar of the model's time zone, set to the first displayed
    /// day. It is a new object on every call.
    public Calendar getCalendar() {
        Calendar c = model.getCalendar();
        c.setTime(firstDisplayedDay);
        return c;
    }

    public TimeZone getTimeZone() {
        return model.getTimeZone();
    }

    public void setTimeZone(TimeZone tz) {
        TimeZone old = getTimeZone();
        if (tz == null || tz.equals(old)) {
            return;
        }
        Date shown = firstDisplayedDay;
        model.setTimeZone(tz);
        firstDisplayedDay = shown;
        updateDatesAfterTimeZoneChange(old);
        firePropertyChange("timeZone", old, tz);
        repaint();
    }

    public int getFirstDayOfWeek() {
        return model.getFirstDayOfWeek();
    }

    /// Sets the day the week rows start with, one of the `Calendar` day
    /// constants.
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        int old = getFirstDayOfWeek();
        model.setFirstDayOfWeek(firstDayOfWeek);
        firePropertyChange("firstDayOfWeek", old, getFirstDayOfWeek());
        repaint();
    }

    /// The date the displayed month was last asked for with.
    protected Date getAnchorDate() {
        return new Date(firstDisplayedDay.getTime());
    }

    /// Reads the displayed month and today again in the new time zone and
    /// forgets the flagged dates, which meant days of the old one.
    protected void updateDatesAfterTimeZoneChange(TimeZone oldTimeZone) {
        firstDisplayedDay = startOfMonth(firstDisplayedDay);
        today = startOfDay(today);
        flagged.clear();
    }

    /// The first millisecond of the last day of the displayed month.
    public Date getLastDisplayedDay() {
        return dayOfMonth(daysInMonth());
    }

    /// The first millisecond of the displayed month.
    public Date getFirstDisplayedDay() {
        return new Date(firstDisplayedDay.getTime());
    }

    /// Shows the month `date` falls in.
    public void setFirstDisplayedDay(Date date) {
        Date old = firstDisplayedDay;
        Date first = startOfMonth(date);
        if (first.equals(old)) {
            return;
        }
        firstDisplayedDay = first;
        firePropertyChange("firstDisplayedDay", old, getFirstDisplayedDay());
        repaint();
    }

    /// Shows the month of `date` if it is not the one displayed.
    public void ensureDateVisible(Date date) {
        if (date.getTime() < firstDisplayedDay.getTime()
                || date.getTime() >= addMonths(firstDisplayedDay, 1).getTime()) {
            setFirstDisplayedDay(date);
        }
    }

    // ------------------------------------------------------------ geometry

    private Font font() {
        return getFont();
    }

    /// `{x, y, boxWidth, boxHeight}` of the grid of seven columns and
    /// eight rows inside the insets.
    private int[] grid() {
        Insets in = getInsets();
        int w = Math.max(0, getWidth() - in.left - in.right);
        int h = Math.max(0, getHeight() - in.top - in.bottom);
        int bw = Math.max(1, w / DAYS_IN_WEEK);
        int bh = Math.max(1, h / ROWS);
        return new int[]{in.left + (w - bw * DAYS_IN_WEEK) / 2, in.top, bw, bh};
    }

    /// The day under a point of the component, or `null` over the title,
    /// the day names, a box of another month or outside the grid.
    public Date getDayAtLocation(int x, int y) {
        int[] g = grid();
        if (x < g[0] || y < g[1] + 2 * g[3]) {
            return null;
        }
        int col = (x - g[0]) / g[2];
        int row = (y - g[1]) / g[3] - 2;
        if (col >= DAYS_IN_WEEK || row >= ROWS - 2) {
            return null;
        }
        int day = row * DAYS_IN_WEEK + col - leadingBoxes() + 1;
        if (day < 1 || day > daysInMonth()) {
            return null;
        }
        return dayOfMonth(day);
    }

    /// The box of a day of the displayed month, or `null` for a date of
    /// another month.
    public Rectangle cn1DayBounds(Date date) {
        if (date.getTime() < firstDisplayedDay.getTime()
                || date.getTime() >= addMonths(firstDisplayedDay, 1).getTime()) {
            return null;
        }
        Calendar c = model.getCalendar();
        c.setTime(date);
        int index = c.get(Calendar.DAY_OF_MONTH) - 1 + leadingBoxes();
        int[] g = grid();
        return new Rectangle(g[0] + (index % DAYS_IN_WEEK) * g[2], g[1] + (2 + index / DAYS_IN_WEEK) * g[3], g[2],
                g[3]);
    }

    /// The box of the arrow to the previous month, or to the next one.
    public Rectangle cn1ArrowBounds(boolean next) {
        int[] g = grid();
        return new Rectangle(next ? g[0] + (DAYS_IN_WEEK - 1) * g[2] : g[0], g[1], g[2], g[3]);
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        FontMetrics fm = Fonts.metrics(font());
        int bw = fm.stringWidth("00");
        String[] names = getDaysOfTheWeek();
        for (int i = 0; i < names.length; i++) {
            bw = Math.max(bw, fm.stringWidth(names[i]));
        }
        bw += 2 * boxPaddingX;
        int bh = fm.getHeight() + 2 * boxPaddingY;
        if (CellTheme.touch()) {
            bw = Math.max(bw, TOUCH_BOX);
            bh = Math.max(bh, TOUCH_BOX);
        }
        Insets in = getInsets();
        return new Dimension(bw * DAYS_IN_WEEK + in.left + in.right, bh * ROWS + in.top + in.bottom);
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return getPreferredSize();
    }

    // ------------------------------------------------------------ paint

    private static void centered(Graphics g, FontMetrics fm, String s, int x, int y, int w, int h) {
        g.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
    }

    private static void arrow(Graphics g, int x, int y, int w, int h, boolean next) {
        int s = Math.max(3, Math.min(w, h) / 4);
        int cx = x + w / 2;
        int cy = y + h / 2;
        int tip = next ? cx + s / 2 : cx - s / 2;
        int back = next ? cx - s / 2 : cx + s / 2;
        g.fillPolygon(new int[]{tip, back, back}, new int[]{cy, cy - s, cy + s}, 3);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Color bg = getBackground();
        if (!isBackgroundSet() || bg == null) {
            bg = CellTheme.background("JXMonthView.background");
        }
        if (isOpaque()) {
            g.setColor(bg);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        Color fg = isForegroundSet() && getForeground() != null ? getForeground()
                : CellTheme.foreground("JXMonthView.foreground");
        Font f = font();
        g.setFont(f);
        FontMetrics fm = Fonts.metrics(f);
        int[] gr = grid();
        int x0 = gr[0];
        int y0 = gr[1];
        int bw = gr[2];
        int bh = gr[3];

        // Title.
        g.setColor(getMonthStringBackground());
        g.fillRect(x0, y0, bw * DAYS_IN_WEEK, bh);
        g.setColor(getMonthStringForeground());
        Insets mi = monthStringInsets;
        centered(g, fm, cn1Title(), x0 + mi.left, y0 + mi.top, bw * DAYS_IN_WEEK - mi.left - mi.right,
                bh - mi.top - mi.bottom);
        if (traversable) {
            arrow(g, x0, y0, bw, bh, false);
            arrow(g, x0 + (DAYS_IN_WEEK - 1) * bw, y0, bw, bh, true);
        }

        // Day names.
        g.setColor(getDaysOfTheWeekForeground());
        int first = model.getFirstDayOfWeek();
        for (int col = 0; col < DAYS_IN_WEEK; col++) {
            int dow = (first - 1 + col) % DAYS_IN_WEEK + 1;
            centered(g, fm, getDayOfTheWeek(dow), x0 + col * bw, y0 + bh, bw, bh);
        }
        g.setColor(CellTheme.grid(null));
        g.drawLine(x0, y0 + 2 * bh - 1, x0 + DAYS_IN_WEEK * bw - 1, y0 + 2 * bh - 1);

        // Days.
        int lead = leadingBoxes();
        int count = daysInMonth();
        Color dim = CellTheme.mix(bg, fg, 0.4f);
        for (int i = 0; i < DAYS_IN_WEEK * (ROWS - 2); i++) {
            int day = i - lead + 1;
            int x = x0 + (i % DAYS_IN_WEEK) * bw;
            int y = y0 + (2 + i / DAYS_IN_WEEK) * bh;
            boolean inMonth = day >= 1 && day <= count;
            if (!inMonth) {
                if ((day < 1 && showingLeadingDays) || (day > count && showingTrailingDays)) {
                    Calendar c = model.getCalendar();
                    c.setTime(addDays(firstDisplayedDay, day - 1));
                    g.setColor(dim);
                    centered(g, fm, String.valueOf(c.get(Calendar.DAY_OF_MONTH)), x, y, bw, bh);
                }
                continue;
            }
            Date date = dayOfMonth(day);
            boolean selected = model.isSelected(date);
            int dow = (first - 1 + i % DAYS_IN_WEEK) % DAYS_IN_WEEK + 1;
            Color c;
            if (model.isUnselectableDate(date)) {
                c = dim;
            } else if (selected) {
                g.setColor(getSelectionBackground());
                g.fillRect(x, y, bw, bh);
                c = getSelectionForeground();
            } else if (flagged.contains(date)) {
                c = getFlaggedDayForeground();
            } else {
                c = dayForeground[dow] != null ? dayForeground[dow] : fg;
            }
            if (date.equals(today)) {
                g.setColor(getTodayBackground());
                g.drawRect(x, y, bw - 1, bh - 1);
            }
            g.setColor(c);
            centered(g, fm, String.valueOf(day), x, y, bw, bh);
        }
    }

    /// The title: the displayed month's name and year.
    public String cn1Title() {
        return new SimpleDateFormat("MMMM yyyy").format(firstDisplayedDay);
    }

    // ------------------------------------------------------------ today

    protected void updateTodayFromCurrentTime() {
        setToday(new Date());
    }

    protected void incrementToday() {
        setToday(addDays(today, 1));
    }

    protected void setToday(Date date) {
        Date old = today;
        today = startOfDay(date);
        firePropertyChange("today", old, getToday());
        repaint();
    }

    /// The first millisecond of the day drawn as today.
    public Date getToday() {
        return new Date(today.getTime());
    }

    /// Does nothing: there are no UI delegates.
    @Override
    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    // ------------------------------------------------------------ selection

    public DateSelectionModel getSelectionModel() {
        return model;
    }

    public void setSelectionModel(DateSelectionModel model) {
        if (model == null) {
            throw new NullPointerException("date selection model must not be null");
        }
        DateSelectionModel old = this.model;
        if (old == model) {
            return;
        }
        old.removeDateSelectionListener(modelListener);
        this.model = model;
        model.addDateSelectionListener(modelListener);
        firstDisplayedDay = startOfMonth(firstDisplayedDay);
        today = startOfDay(today);
        firePropertyChange(SELECTION_MODEL, old, model);
        repaint();
    }

    public void clearSelection() {
        model.clearSelection();
    }

    public boolean isSelectionEmpty() {
        return model.isSelectionEmpty();
    }

    public SortedSet<Date> getSelection() {
        return model.getSelection();
    }

    public void addSelectionInterval(Date startDate, Date endDate) {
        if (startDate != null && endDate != null) {
            model.addSelectionInterval(startDate, endDate);
        }
    }

    public void setSelectionInterval(Date startDate, Date endDate) {
        if (startDate != null && endDate != null) {
            model.setSelectionInterval(startDate, endDate);
        }
    }

    public void removeSelectionInterval(Date startDate, Date endDate) {
        if (startDate != null && endDate != null) {
            model.removeSelectionInterval(startDate, endDate);
        }
    }

    public SelectionMode getSelectionMode() {
        return model.getSelectionMode();
    }

    public void setSelectionMode(SelectionMode selectionMode) {
        model.setSelectionMode(selectionMode);
    }

    public Date getFirstSelectionDate() {
        return model.getFirstSelectionDate();
    }

    public Date getLastSelectionDate() {
        return model.getLastSelectionDate();
    }

    public Date getSelectionDate() {
        return model.getFirstSelectionDate();
    }

    /// Selects one date, or clears the selection for `null`.
    public void setSelectionDate(Date newDate) {
        if (newDate == null) {
            clearSelection();
        } else {
            setSelectionInterval(newDate, newDate);
        }
    }

    public boolean isSelected(Date date) {
        return model.isSelected(date);
    }

    public void setLowerBound(Date lowerBound) {
        model.setLowerBound(lowerBound);
    }

    public void setUpperBound(Date upperBound) {
        model.setUpperBound(upperBound);
    }

    public Date getLowerBound() {
        return model.getLowerBound();
    }

    public Date getUpperBound() {
        return model.getUpperBound();
    }

    public boolean isUnselectableDate(Date date) {
        return model.isUnselectableDate(date);
    }

    /// Sets the dates that cannot be selected; none for `null` or no
    /// arguments.
    public void setUnselectableDates(Date... unselectableDates) {
        TreeSet<Date> set = new TreeSet<Date>();
        if (unselectableDates != null) {
            for (int i = 0; i < unselectableDates.length; i++) {
                if (unselectableDates[i] != null) {
                    set.add(unselectableDates[i]);
                }
            }
        }
        model.setUnselectableDates(set);
        repaint();
    }

    // ------------------------------------------------------------ flagged

    public boolean isFlaggedDate(Date date) {
        return date != null && flagged.contains(startOfDay(date));
    }

    public void setFlaggedDates(Date... flaggedDates) {
        SortedSet<Date> old = getFlaggedDates();
        flagged.clear();
        addAll(flaggedDates);
        firePropertyChange(FLAGGED_DATES, old, getFlaggedDates());
        repaint();
    }

    private void addAll(Date[] dates) {
        if (dates != null) {
            for (int i = 0; i < dates.length; i++) {
                if (dates[i] != null) {
                    flagged.add(startOfDay(dates[i]));
                }
            }
        }
    }

    public void addFlaggedDates(Date... flaggedDates) {
        SortedSet<Date> old = getFlaggedDates();
        addAll(flaggedDates);
        firePropertyChange(FLAGGED_DATES, old, getFlaggedDates());
        repaint();
    }

    public void removeFlaggedDates(Date... flaggedDates) {
        SortedSet<Date> old = getFlaggedDates();
        if (flaggedDates != null) {
            for (int i = 0; i < flaggedDates.length; i++) {
                if (flaggedDates[i] != null) {
                    flagged.remove(startOfDay(flaggedDates[i]));
                }
            }
        }
        firePropertyChange(FLAGGED_DATES, old, getFlaggedDates());
        repaint();
    }

    public void clearFlaggedDates() {
        SortedSet<Date> old = getFlaggedDates();
        flagged.clear();
        firePropertyChange(FLAGGED_DATES, old, getFlaggedDates());
        repaint();
    }

    /// A copy of the flagged dates, each the first millisecond of its day.
    public SortedSet<Date> getFlaggedDates() {
        TreeSet<Date> out = new TreeSet<Date>();
        Iterator<Date> it = flagged.iterator();
        while (it.hasNext()) {
            out.add(new Date(it.next().getTime()));
        }
        return out;
    }

    public boolean hasFlaggedDates() {
        return !flagged.isEmpty();
    }

    // ------------------------------------------------------------ properties

    /// Whether the last days of the month before are drawn, dimmed, in
    /// the boxes ahead of day one.
    public void setShowingLeadingDays(boolean value) {
        boolean old = showingLeadingDays;
        showingLeadingDays = value;
        firePropertyChange("showingLeadingDays", old, value);
        repaint();
    }

    public boolean isShowingLeadingDays() {
        return showingLeadingDays;
    }

    /// Whether the first days of the month after are drawn, dimmed, in
    /// the boxes behind the last day.
    public void setShowingTrailingDays(boolean value) {
        boolean old = showingTrailingDays;
        showingTrailingDays = value;
        firePropertyChange("showingTrailingDays", old, value);
        repaint();
    }

    public boolean isShowingTrailingDays() {
        return showingTrailingDays;
    }

    public boolean isTraversable() {
        return traversable;
    }

    /// Whether the title has arrows that move to the month before and
    /// after.
    public void setTraversable(boolean traversable) {
        boolean old = this.traversable;
        this.traversable = traversable;
        firePropertyChange(TRAVERSABLE, old, traversable);
        repaint();
    }

    public boolean isZoomable() {
        return zoomable;
    }

    /// Recorded; a zoomable view is traversable and nothing more.
    public void setZoomable(boolean zoomable) {
        boolean old = this.zoomable;
        this.zoomable = zoomable;
        if (zoomable) {
            setTraversable(true);
        }
        firePropertyChange("zoomable", old, zoomable);
    }

    public boolean isShowingWeekNumber() {
        return showingWeekNumber;
    }

    /// Recorded only: no week number column is drawn.
    public void setShowingWeekNumber(boolean showWeekNumber) {
        boolean old = showingWeekNumber;
        showingWeekNumber = showWeekNumber;
        firePropertyChange("showingWeekNumber", old, showWeekNumber);
    }

    /// Sets the names in the day name row, seven of them starting with
    /// Sunday; `null` goes back to the formatter's short names.
    public void setDaysOfTheWeek(String[] days) {
        if (days != null && days.length != DAYS_IN_WEEK) {
            throw new IllegalArgumentException("Array of days is not of length " + DAYS_IN_WEEK + " as expected.");
        }
        String[] old = getDaysOfTheWeek();
        if (days == null) {
            daysOfTheWeek = null;
        } else {
            daysOfTheWeek = new String[DAYS_IN_WEEK];
            System.arraycopy(days, 0, daysOfTheWeek, 0, DAYS_IN_WEEK);
        }
        firePropertyChange(DAYS_OF_THE_WEEK, old, getDaysOfTheWeek());
        revalidate();
        repaint();
    }

    /// The seven day names, starting with Sunday.
    public String[] getDaysOfTheWeek() {
        String[] out = new String[DAYS_IN_WEEK];
        if (daysOfTheWeek != null) {
            System.arraycopy(daysOfTheWeek, 0, out, 0, DAYS_IN_WEEK);
            return out;
        }
        String[] names = new SimpleDateFormat().getDateFormatSymbols().getShortWeekdays();
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            // The formatter's names are indexed by the Calendar day constants.
            String n = names != null && names.length > i + 1 ? names[i + 1] : null;
            out[i] = n != null ? n : "";
        }
        return out;
    }

    /// The name of a day given as a `Calendar` day constant.
    public String getDayOfTheWeek(int dayOfWeek) {
        return getDaysOfTheWeek()[dayOfWeek - 1];
    }

    public int getBoxPaddingX() {
        return boxPaddingX;
    }

    public void setBoxPaddingX(int boxPaddingX) {
        int old = this.boxPaddingX;
        this.boxPaddingX = boxPaddingX;
        firePropertyChange(BOX_PADDING_X, old, boxPaddingX);
        revalidate();
    }

    public int getBoxPaddingY() {
        return boxPaddingY;
    }

    public void setBoxPaddingY(int boxPaddingY) {
        int old = this.boxPaddingY;
        this.boxPaddingY = boxPaddingY;
        firePropertyChange(BOX_PADDING_Y, old, boxPaddingY);
        revalidate();
    }

    public Color getSelectionBackground() {
        return selectionBackground != null ? selectionBackground
                : CellTheme.selectionBackground("JXMonthView.selectedBackground");
    }

    public void setSelectionBackground(Color c) {
        Color old = selectionBackground;
        selectionBackground = c;
        firePropertyChange("selectionBackground", old, c);
        repaint();
    }

    public Color getSelectionForeground() {
        return selectionForeground != null ? selectionForeground
                : CellTheme.selectionForeground("JXMonthView.selectedForeground");
    }

    public void setSelectionForeground(Color c) {
        Color old = selectionForeground;
        selectionForeground = c;
        firePropertyChange("selectionForeground", old, c);
        repaint();
    }

    /// The color of the frame around today.
    public Color getTodayBackground() {
        return todayBackground != null ? todayBackground : CellTheme.foreground("JXMonthView.todayBackground");
    }

    public void setTodayBackground(Color c) {
        Color old = todayBackground;
        todayBackground = c;
        firePropertyChange("todayBackground", old, c);
        repaint();
    }

    public Color getMonthStringBackground() {
        return monthStringBackground != null ? monthStringBackground
                : CellTheme.headerBackground("JXMonthView.monthStringBackground");
    }

    public void setMonthStringBackground(Color c) {
        Color old = monthStringBackground;
        monthStringBackground = c;
        firePropertyChange("monthStringBackground", old, c);
        repaint();
    }

    public Color getMonthStringForeground() {
        return monthStringForeground != null ? monthStringForeground
                : CellTheme.headerForeground("JXMonthView.monthStringForeground");
    }

    public void setMonthStringForeground(Color c) {
        Color old = monthStringForeground;
        monthStringForeground = c;
        firePropertyChange("monthStringForeground", old, c);
        repaint();
    }

    public void setDaysOfTheWeekForeground(Color c) {
        Color old = daysOfTheWeekForeground;
        daysOfTheWeekForeground = c;
        firePropertyChange("daysOfTheWeekForeground", old, c);
        repaint();
    }

    public Color getDaysOfTheWeekForeground() {
        return daysOfTheWeekForeground != null ? daysOfTheWeekForeground
                : CellTheme.foreground("JXMonthView.daysOfTheWeekForeground");
    }

    /// Sets the text color of one day of the week, given as a `Calendar`
    /// day constant; `null` goes back to the foreground.
    public void setDayForeground(int dayOfWeek, Color c) {
        if (dayOfWeek >= Calendar.SUNDAY && dayOfWeek <= Calendar.SATURDAY) {
            dayForeground[dayOfWeek] = c;
            repaint();
        }
    }

    public Color getDayForeground(int dayOfWeek) {
        Color c = getPerDayOfWeekForeground(dayOfWeek);
        return c != null ? c : getForeground();
    }

    /// The color set for that day of the week alone, or `null`.
    public Color getPerDayOfWeekForeground(int dayOfWeek) {
        return dayOfWeek >= Calendar.SUNDAY && dayOfWeek <= Calendar.SATURDAY ? dayForeground[dayOfWeek] : null;
    }

    public void setFlaggedDayForeground(Color c) {
        Color old = flaggedDayForeground;
        flaggedDayForeground = c;
        firePropertyChange("flaggedDayForeground", old, c);
        repaint();
    }

    public Color getFlaggedDayForeground() {
        return flaggedDayForeground != null ? flaggedDayForeground : Color.RED;
    }

    public Insets getMonthStringInsets() {
        Insets i = monthStringInsets;
        return new Insets(i.top, i.left, i.bottom, i.right);
    }

    public void setMonthStringInsets(Insets insets) {
        Insets old = getMonthStringInsets();
        monthStringInsets = insets == null ? new Insets(0, 0, 0, 0)
                : new Insets(insets.top, insets.left, insets.bottom, insets.right);
        firePropertyChange("monthStringInsets", old, getMonthStringInsets());
        repaint();
    }

    public int getPreferredColumnCount() {
        return preferredColumnCount;
    }

    /// Recorded only: one month is shown.
    public void setPreferredColumnCount(int cols) {
        if (cols > 0) {
            int old = preferredColumnCount;
            preferredColumnCount = cols;
            firePropertyChange("preferredColumnCount", old, cols);
        }
    }

    public int getPreferredRowCount() {
        return preferredRowCount;
    }

    /// Recorded only: one month is shown.
    public void setPreferredRowCount(int rows) {
        if (rows > 0) {
            int old = preferredRowCount;
            preferredRowCount = rows;
            firePropertyChange("preferredRowCount", old, rows);
        }
    }

    // ------------------------------------------------------------ actions

    /// Ends an adjusting selection and tells the action listeners
    /// [#COMMIT_KEY].
    public void commitSelection() {
        model.setAdjusting(false);
        fireActionPerformed(COMMIT_KEY);
    }

    /// Ends an adjusting selection and tells the action listeners
    /// [#CANCEL_KEY]. The selection stays as it is.
    public void cancelSelection() {
        model.setAdjusting(false);
        fireActionPerformed(CANCEL_KEY);
    }

    /// Whether the keys work when the view does not own the focus;
    /// recorded only, the keys always need the focus.
    public void setComponentInputMapEnabled(boolean enabled) {
        boolean old = componentInputMapEnabled;
        componentInputMapEnabled = enabled;
        firePropertyChange("componentInputMapEnabled", old, enabled);
    }

    public boolean isComponentInputMapEnabled() {
        return componentInputMapEnabled;
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    protected void fireActionPerformed(String actionCommand) {
        ActionListener[] ls = listenerList.getListeners(ActionListener.class);
        if (ls.length == 0) {
            return;
        }
        ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionCommand);
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].actionPerformed(e);
        }
    }

    // ------------------------------------------------------------ input

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (isEnabled()) {
            switch (e.getID()) {
                case MouseEvent.MOUSE_PRESSED:
                    pressTouch = CellTheme.touch();
                    pressOnDay = false;
                    if (pressTouch) {
                        ScrollDelegate.forward(this, e);
                    } else if (e.getButton() == MouseEvent.BUTTON1) {
                        requestFocusInWindow();
                        pressOnDay = pick(e, true);
                    }
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    if (pressTouch) {
                        ScrollDelegate.forward(this, e);
                    } else if (pressOnDay) {
                        pressOnDay = false;
                        commitSelection();
                    }
                    break;
                case MouseEvent.MOUSE_CLICKED:
                    if (pressTouch && pick(e, false)) {
                        commitSelection();
                    }
                    break;
                default:
                    break;
            }
        }
        super.processMouseEvent(e);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (isEnabled() && e.getID() == MouseEvent.MOUSE_DRAGGED) {
            if (pressTouch) {
                ScrollDelegate.forward(this, e);
            } else if (pressOnDay && anchor != null && model.getSelectionMode() != SelectionMode.SINGLE_SELECTION) {
                Date d = getDayAtLocation(e.getX(), e.getY());
                if (d != null && !model.isUnselectableDate(d)) {
                    span(anchor, d);
                }
            }
        }
        super.processMouseMotionEvent(e);
    }

    private void span(Date a, Date b) {
        if (a.getTime() <= b.getTime()) {
            model.setSelectionInterval(a, b);
        } else {
            model.setSelectionInterval(b, a);
        }
    }

    /// What a press or tap does: moves a month over an arrow, selects
    /// over a day. Answers whether a day was selected.
    private boolean pick(MouseEvent e, boolean adjusting) {
        if (traversable) {
            if (cn1ArrowBounds(false).contains(e.getX(), e.getY())) {
                setFirstDisplayedDay(addMonths(firstDisplayedDay, -1));
                return false;
            }
            if (cn1ArrowBounds(true).contains(e.getX(), e.getY())) {
                setFirstDisplayedDay(addMonths(firstDisplayedDay, 1));
                return false;
            }
        }
        Date d = getDayAtLocation(e.getX(), e.getY());
        if (d == null || model.isUnselectableDate(d)) {
            return false;
        }
        SelectionMode mode = model.getSelectionMode();
        if (adjusting) {
            model.setAdjusting(true);
        }
        if (e.isShiftDown() && anchor != null && mode != SelectionMode.SINGLE_SELECTION) {
            span(anchor, d);
        } else if ((e.isControlDown() || e.isMetaDown()) && mode == SelectionMode.MULTIPLE_INTERVAL_SELECTION) {
            if (model.isSelected(d)) {
                model.removeSelectionInterval(d, d);
            } else {
                model.addSelectionInterval(d, d);
            }
            anchor = d;
        } else {
            model.setSelectionInterval(d, d);
            anchor = d;
        }
        return true;
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (isEnabled() && e.getID() == KeyEvent.KEY_PRESSED && !e.isConsumed()) {
            int days = 0;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    days = -1;
                    break;
                case KeyEvent.VK_RIGHT:
                    days = 1;
                    break;
                case KeyEvent.VK_UP:
                    days = -DAYS_IN_WEEK;
                    break;
                case KeyEvent.VK_DOWN:
                    days = DAYS_IN_WEEK;
                    break;
                case KeyEvent.VK_PAGE_UP:
                    setFirstDisplayedDay(addMonths(firstDisplayedDay, -1));
                    e.consume();
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    setFirstDisplayedDay(addMonths(firstDisplayedDay, 1));
                    e.consume();
                    break;
                case KeyEvent.VK_ENTER:
                    commitSelection();
                    e.consume();
                    break;
                case KeyEvent.VK_ESCAPE:
                    cancelSelection();
                    e.consume();
                    break;
                default:
                    break;
            }
            if (days != 0) {
                Date from = model.getFirstSelectionDate();
                Date to = from != null ? addDays(from, days) : firstDisplayedDay;
                if (!model.isUnselectableDate(to)) {
                    model.setSelectionInterval(to, to);
                    anchor = to;
                    ensureDateVisible(to);
                }
                e.consume();
            }
        }
        super.processKeyEvent(e);
    }
}
