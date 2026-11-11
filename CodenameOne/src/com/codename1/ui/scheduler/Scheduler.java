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
package com.codename1.ui.scheduler;

import com.codename1.scheduling.ScheduleChangeListener;
import com.codename1.scheduling.ScheduleItem;
import com.codename1.scheduling.ScheduleModel;
import com.codename1.scheduling.ScheduleResource;
import com.codename1.scheduling.TimeMath;
import com.codename1.scheduling.WorkCalendar;
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.UITimer;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/// A schedule on screen: the items of a `ScheduleModel` by day, week, month,
/// as an agenda, or by resource.
///
/// ```java
/// long start = System.currentTimeMillis();
/// DefaultScheduleModel model = new DefaultScheduleModel();
/// model.addItem(new ScheduleItem("1", "Design review", start, start + TimeMath.HOUR));
/// Scheduler scheduler = new Scheduler(model);
/// scheduler.setView(Scheduler.View.WEEK);
///
/// Form form = new Form("Schedule", new BorderLayout());
/// form.add(BorderLayout.CENTER, scheduler);
/// form.show();
/// ```
///
/// The views draw their items and keep no component for each, so a schedule
/// of thousands of items scrolls as one of ten does. Time zooms with a pinch
/// or with the wheel and the control key held.
///
/// #### Editing
///
/// With a mouse an item is dragged to move it and by its edge to resize it,
/// and a drag across empty time selects a range. On a touch screen a drag
/// scrolls, so a long press picks an item up or starts a range, and the
/// selected item has handles to resize it by. Every change is offered to the
/// listeners first, then made through `ScheduleModel#updateItem`, and either
/// may refuse. `setEditable(false)` makes the scheduler read only.
///
/// #### Styling
///
/// `SchedulerGrid` is the background, with its foreground the colour of the
/// lines; `SchedulerOffHours` shades the time outside a `WorkCalendar`;
/// `SchedulerHeader`, `SchedulerHeaderToday` and `SchedulerTimeLabel` are the
/// headings; `SchedulerItem`, `SchedulerBackgroundItem` and
/// `SchedulerBlocked` the items by kind; `SchedulerNow` the current time and
/// `SchedulerSelection` a range being dragged out. The bar above the views
/// is `SchedulerToolbar`, `SchedulerTitle` and `SchedulerViewButton`. An item
/// that names a UIID of its own is painted with that one.
public class Scheduler extends Container {
    /// The ways a schedule is shown.
    public enum View {
        /// One day, time running down.
        DAY,
        /// The working days of a week side by side.
        WORK_WEEK,
        /// A week side by side.
        WEEK,
        /// A month of day cells.
        MONTH,
        /// A list, day by day.
        AGENDA,
        /// A row per resource, time running across.
        RESOURCE_TIMELINE,
        /// One day with a column per resource.
        RESOURCE_DAY
    }

    private ScheduleModel model;
    private View view = View.WEEK;
    private View[] views = {View.DAY, View.WEEK, View.MONTH, View.AGENDA};
    private ZoneId zone = ZoneId.systemDefault();
    private int firstDayOfWeek = 1;
    private WorkCalendar workCalendar;
    private Boolean hours24;
    private int snapMinutes = 15;
    private int defaultMinutes = 60;
    private int initialMinute = 7 * 60;
    private int timelineDays = 7;
    private int agendaDays = 30;
    private boolean editable = true;
    private boolean openOnTap = !Display.getInstance().isDesktop();
    private Boolean dragEdits;
    private boolean compactToolbar = !(Display.getInstance().isDesktop() || Display.getInstance().isTablet());
    private long fixedNow = -1;
    private long anchorDay;
    private long visibleStart;
    private long visibleEnd;
    private ScheduleItem selected;
    private ScheduleItemRenderer renderer = new DefaultScheduleItemRenderer();
    private final ArrayList<SchedulerListener> listeners = new ArrayList<SchedulerListener>();

    private DayGrid dayGrid;
    private MonthGrid monthGrid;
    private AgendaList agenda;
    private ResourceTimeline timeline;
    private Component current;
    private final Container toolbar = new Container(new BorderLayout());
    private final Label title = new Label("", "SchedulerTitle");
    private final Container viewButtons = new Container(BoxLayout.x());
    private UITimer clock;

    private final ScheduleChangeListener modelListener = new ScheduleChangeListener() {
        @Override
        public void scheduleChanged(ScheduleModel m) {
            // A model that loads again makes new items for the same things:
            // the selection follows the id, or is gone with the item.
            if (selected != null) {
                selected = again(m, selected);
            }
            refresh();
        }

        private ScheduleItem again(ScheduleModel m, ScheduleItem was) {
            List<ScheduleItem> near = new ArrayList<ScheduleItem>();
            long span = Math.max(TimeMath.DAY, visibleEnd - visibleStart);
            m.getItems(Math.min(visibleStart, was.getStart()) - span, Math.max(visibleEnd, was.getEnd()) + span, near);
            for (ScheduleItem item : near) {
                if (item == was || item.getId().equals(was.getId())) { //NOPMD CompareObjectsWithEquals - the very item is the common case
                    return item;
                }
            }
            return null;
        }
    };

    /// A scheduler with nothing in it yet.
    public Scheduler() {
        this(null);
    }

    public Scheduler(ScheduleModel model) {
        super(new BorderLayout());
        setUIID("Scheduler");
        toolbar.setUIID("SchedulerToolbar");
        anchorDay = TimeMath.epochDay(System.currentTimeMillis(), zone);
        buildToolbar();
        add(BorderLayout.NORTH, toolbar);
        setModel(model);
    }

    // ------------------------------------------------------------ toolbar

    private Button toolButton(String text, char icon, ActionListener action) {
        Button b = new Button(text == null ? "" : text, "SchedulerViewButton");
        if (icon != 0) {
            FontImage.setMaterialIcon(b, icon);
        }
        b.addActionListener(action);
        return b;
    }

    private void buildToolbar() {
        Container nav = new Container(BoxLayout.x());
        nav.add(toolButton(null, FontImage.MATERIAL_CHEVRON_LEFT, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                previous();
            }
        }));
        nav.add(toolButton(localize("scheduler.today", "Today"), (char) 0, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                today();
            }
        }));
        nav.add(toolButton(null, FontImage.MATERIAL_CHEVRON_RIGHT, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                next();
            }
        }));
        toolbar.add(BorderLayout.WEST, nav);
        toolbar.add(BorderLayout.CENTER, title);
        toolbar.add(BorderLayout.EAST, viewButtons);
        buildViewButtons();
    }

    private String viewName(View v) {
        switch (v) {
            case DAY:
                return localize("scheduler.view.day", "Day");
            case WORK_WEEK:
                return localize("scheduler.view.workWeek", "Work week");
            case WEEK:
                return localize("scheduler.view.week", "Week");
            case MONTH:
                return localize("scheduler.view.month", "Month");
            case AGENDA:
                return localize("scheduler.view.agenda", "Agenda");
            case RESOURCE_TIMELINE:
                return localize("scheduler.view.timeline", "Timeline");
            default:
                return localize("scheduler.view.resources", "Resources");
        }
    }

    private void buildViewButtons() {
        viewButtons.removeAll();
        if (views.length < 2) {
            return;
        }
        if (compactToolbar) {
            // One button that names the view and opens the list of them.
            final Button pick = new Button(viewName(view), "SchedulerViewButton");
            FontImage.setMaterialIcon(pick, FontImage.MATERIAL_ARROW_DROP_DOWN);
            pick.setTextPosition(Component.LEFT);
            pick.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    showViewMenu(pick);
                }
            });
            viewButtons.add(pick);
            return;
        }
        ButtonGroup group = new ButtonGroup();
        for (final View v : views) {
            RadioButton b = new RadioButton(viewName(v));
            b.setToggle(true);
            b.setUIID("SchedulerViewButton");
            group.add(b);
            b.setSelected(v == view);
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    setView(v);
                }
            });
            viewButtons.add(b);
        }
    }

    private void showViewMenu(Button anchor) {
        final Dialog menu = new Dialog();
        // Placed against the button that opens it. In a window of its own it
        // would come out centred, wherever the theme's default points.
        menu.setNativeWindowMode(false);
        menu.setLayout(BoxLayout.y());
        menu.setDisposeWhenPointerOutOfBounds(true);
        for (final View v : views) {
            Button b = new Button(viewName(v), "SchedulerViewButton");
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    menu.dispose();
                    setView(v);
                }
            });
            menu.add(b);
        }
        menu.showPopupDialog(anchor);
    }

    /// Whether the bar with the title and the buttons is shown. An
    /// application with navigation of its own hides it and calls `next`,
    /// `previous`, `today` and `setView` itself.
    public void setToolbarVisible(boolean v) {
        toolbar.setVisible(v);
        toolbar.setHidden(!v);
        revalidateLater();
    }

    public boolean isToolbarVisible() {
        return toolbar.isVisible();
    }

    /// Whether the views are picked from one button with a menu, as on a
    /// phone, or each has a button of its own.
    public void setCompactToolbar(boolean v) {
        compactToolbar = v;
        buildViewButtons();
        revalidateLater();
    }

    public boolean isCompactToolbar() {
        return compactToolbar;
    }

    // -------------------------------------------------------------- model

    public ScheduleModel getModel() {
        return model;
    }

    public void setModel(ScheduleModel m) {
        if (model != null) {
            model.removeChangeListener(modelListener);
        }
        model = m;
        selected = null;
        if (model != null) {
            model.addChangeListener(modelListener);
        }
        apply();
        if (model != null && visibleEnd > visibleStart) {
            // The range has not moved, so apply told nobody; but this model
            // has never heard it, and one that loads would load nothing.
            model.setVisibleRange(visibleStart - (visibleEnd - visibleStart), visibleEnd + (visibleEnd - visibleStart));
        }
    }

    /// Reads the model again. A model that fires its change listeners needs
    /// no call to this.
    public void refresh() {
        if (current == dayGrid && dayGrid != null) { //NOPMD CompareObjectsWithEquals - the same component, not an equal one
            if (view == View.RESOURCE_DAY) {
                // The resources themselves may have changed.
                apply();
            } else {
                dayGrid.modelChanged();
            }
        } else if (current == monthGrid && monthGrid != null) { //NOPMD CompareObjectsWithEquals - the same component, not an equal one
            monthGrid.modelChanged();
        } else if (current == agenda && agenda != null) { //NOPMD CompareObjectsWithEquals - the same component, not an equal one
            agenda.modelChanged();
        } else if (current == timeline && timeline != null) { //NOPMD CompareObjectsWithEquals - the same component, not an equal one
            timeline.modelChanged();
        }
    }

    /// The resource with an id, or null.
    public ScheduleResource getResource(String id) {
        if (id == null || model == null) {
            return null;
        }
        List<ScheduleResource> all = model.getResources();
        for (ScheduleResource resource : all) {
            if (id.equals(resource.getId())) {
                return resource;
            }
        }
        return null;
    }

    int resourceColor(String id) {
        ScheduleResource r = getResource(id);
        return r == null ? -1 : r.getColor();
    }

    // --------------------------------------------------------------- view

    public View getView() {
        return view;
    }

    public void setView(View v) {
        if (v == null) {
            throw new IllegalArgumentException("view");
        }
        view = v;
        buildViewButtons();
        apply();
    }

    /// The views the toolbar offers, in order. The default is day, week,
    /// month and agenda; a schedule with resources adds the two resource
    /// views.
    public void setViews(View... offered) {
        views = new View[offered.length];
        System.arraycopy(offered, 0, views, 0, offered.length);
        buildViewButtons();
        revalidateLater();
    }

    /// Shows the day, week or month that an instant is in.
    public void setDate(long millis) {
        anchorDay = TimeMath.epochDay(millis, zone);
        apply();
    }

    /// The start of the day the view is anchored on.
    public long getDate() {
        return TimeMath.startOfEpochDay(anchorDay, zone);
    }

    /// Moves forward by what the view shows: a day, a week, a month.
    public void next() {
        step(1);
    }

    public void previous() {
        step(-1);
    }

    /// Shows the current day, week or month.
    public void today() {
        anchorDay = TimeMath.epochDay(now(), zone);
        apply();
    }

    private void step(int direction) {
        switch (view) {
            case DAY:
            case RESOURCE_DAY:
                anchorDay += direction;
                break;
            case WEEK:
            case WORK_WEEK:
                anchorDay += 7L * direction;
                break;
            case MONTH:
                anchorDay = TimeMath.plusMonths(anchorDay, direction);
                break;
            case AGENDA:
                anchorDay += (long) agendaDays * direction;
                break;
            default:
                anchorDay += (long) timelineDays * direction;
                break;
        }
        apply();
    }

    /// The start of what the view shows, inclusive.
    public long getVisibleStart() {
        return visibleStart;
    }

    /// The end of what the view shows, exclusive.
    public long getVisibleEnd() {
        return visibleEnd;
    }

    private void show(Component c) {
        if (current == c) { //NOPMD CompareObjectsWithEquals - the same component, not an equal one
            return;
        }
        if (current != null) {
            removeComponent(current);
        }
        current = c;
        add(BorderLayout.CENTER, c);
        revalidateLater();
    }

    private void applyDragEdits(ScrollSurface s) {
        if (dragEdits != null) {
            s.setDragEdits(dragEdits.booleanValue());
        }
    }

    // Puts the view, the date and the model on screen.
    private void apply() {
        long first;
        int count;
        String text;
        switch (view) {
            case MONTH: {
                if (monthGrid == null) {
                    monthGrid = new MonthGrid(this);
                }
                applyDragEdits(monthGrid);
                monthGrid.show(anchorDay);
                show(monthGrid);
                first = monthGrid.firstDay();
                count = monthGrid.dayCount();
                text = Dates.monthTitle(anchorDay);
                break;
            }
            case AGENDA: {
                if (agenda == null) {
                    agenda = new AgendaList(this);
                }
                first = anchorDay;
                count = agendaDays;
                agenda.show(first, count);
                show(agenda);
                text = rangeTitle(first, count);
                break;
            }
            case RESOURCE_TIMELINE: {
                if (timeline == null) {
                    timeline = new ResourceTimeline(this);
                }
                applyDragEdits(timeline);
                first = timelineDays >= 7 ? TimeMath.startOfWeek(anchorDay, firstDayOfWeek) : anchorDay;
                count = timelineDays;
                timeline.show(first, count);
                show(timeline);
                text = rangeTitle(first, count);
                break;
            }
            default: {
                if (dayGrid == null) {
                    dayGrid = new DayGrid(this);
                }
                applyDragEdits(dayGrid);
                if (view == View.DAY || view == View.RESOURCE_DAY) {
                    first = anchorDay;
                    count = 1;
                    dayGrid.show(first, 1, view == View.RESOURCE_DAY && model != null ? model.getResources() : null);
                } else {
                    first = TimeMath.startOfWeek(anchorDay, firstDayOfWeek);
                    count = 7;
                    if (view == View.WORK_WEEK) {
                        dayGrid.show(workDays(first));
                    } else {
                        dayGrid.show(first, 7, null);
                    }
                }
                show(dayGrid);
                text = rangeTitle(first, count);
                break;
            }
        }
        title.setText(text);
        long from = TimeMath.startOfEpochDay(first, zone);
        long to = TimeMath.startOfEpochDay(first + count, zone);
        boolean moved = from != visibleStart || to != visibleEnd;
        visibleStart = from;
        visibleEnd = to;
        if (moved) {
            if (model != null) {
                // A model that loads from somewhere is told a wider range, so
                // the next step is there before it is asked for.
                model.setVisibleRange(from - (to - from), to + (to - from));
            }
            for (SchedulerListener l : told()) {
                l.visibleRangeChanged(this, from, to);
            }
        }
        repaint();
    }

    private long[] workDays(long weekStart) {
        long[] tmp = new long[7];
        int n = 0;
        for (int i = 0; i < 7; i++) {
            int dow = TimeMath.dayOfWeek(weekStart + i);
            boolean working = workCalendar == null ? dow <= 5 : workCalendar.isWorkingWeekday(dow);
            if (working) {
                tmp[n++] = weekStart + i;
            }
        }
        if (n == 0) {
            return new long[]{weekStart};
        }
        long[] out = new long[n];
        System.arraycopy(tmp, 0, out, 0, n);
        return out;
    }

    private String rangeTitle(long first, int count) {
        if (count <= 1) {
            return Dates.dayTitle(first, TimeMath.dayOfWeek(first)) + ", " + Dates.year(first);
        }
        long last = first + count - 1;
        if (Dates.year(first) != Dates.year(last)) {
            return Dates.monthDay(first) + ", " + Dates.year(first) + " - " + Dates.monthDay(last) + ", "
                    + Dates.year(last);
        }
        if (Dates.month(first) != Dates.month(last)) {
            return Dates.monthDay(first) + " - " + Dates.monthDay(last) + ", " + Dates.year(last);
        }
        return Dates.monthDay(first) + " - " + Dates.dayOfMonth(last) + ", " + Dates.year(last);
    }

    // ----------------------------------------------------------- settings

    public ZoneId getZone() {
        return zone;
    }

    /// The zone the days and hours are those of. The default is the
    /// device's.
    public void setZone(ZoneId z) {
        if (z == null) {
            throw new IllegalArgumentException("zone");
        }
        // The same date in the new zone. The instant that date started at
        // in the old zone may be the day before in the new one, and going by
        // it would step back a day with every change to a zone further west.
        zone = z;
        apply();
    }

    public int getFirstDayOfWeek() {
        return firstDayOfWeek;
    }

    /// The day a week starts on: 1 for Monday, which is the default, to 7
    /// for Sunday.
    public void setFirstDayOfWeek(int isoDay) {
        if (isoDay < 1 || isoDay > 7) {
            throw new IllegalArgumentException("isoDay " + isoDay);
        }
        firstDayOfWeek = isoDay;
        apply();
    }

    public WorkCalendar getWorkCalendar() {
        return workCalendar;
    }

    /// The working hours: time outside them is shaded, and the work week
    /// shows the days this calendar works. A resource with a calendar of its
    /// own is shaded by that one. Null, the default, shades nothing.
    public void setWorkCalendar(WorkCalendar c) {
        workCalendar = c;
        apply();
    }

    /// Whether times are written on a 24 hour clock. The default follows
    /// the device's locale.
    public boolean is24Hours() {
        if (hours24 == null) {
            hours24 = Boolean.valueOf(Dates.deviceUses24Hours());
        }
        return hours24.booleanValue();
    }

    public void set24Hours(boolean v) {
        hours24 = Boolean.valueOf(v);
        apply();
    }

    public int getSnapMinutes() {
        return snapMinutes;
    }

    /// What a drag snaps to, in minutes. The default is 15.
    public void setSnapMinutes(int minutes) {
        snapMinutes = Math.max(1, minutes);
    }

    public int getDefaultMinutes() {
        return defaultMinutes;
    }

    /// The length of a range chosen without dragging, by a long press or a
    /// double click. The default is 60.
    public void setDefaultMinutes(int minutes) {
        defaultMinutes = Math.max(1, minutes);
    }

    public int getInitialMinute() {
        return initialMinute;
    }

    /// The time of day the day and week views open scrolled to, in minutes
    /// since midnight. The default is 07:00.
    public void setInitialMinute(int minute) {
        initialMinute = Math.max(0, Math.min(TimeMath.MINUTES_PER_DAY, minute));
        if (dayGrid != null) {
            dayGrid.scrollToMinute(initialMinute);
        }
    }

    public int getTimelineDays() {
        return timelineDays;
    }

    /// How many days the resource timeline covers. The default is 7.
    public void setTimelineDays(int days) {
        timelineDays = Math.max(1, days);
        apply();
    }

    public int getAgendaDays() {
        return agendaDays;
    }

    /// How many days the agenda lists. The default is 30.
    public void setAgendaDays(int days) {
        agendaDays = Math.max(1, days);
        apply();
    }

    @Override
    public boolean isEditable() {
        return editable;
    }

    /// Whether items can be dragged and ranges selected. Selecting and
    /// opening an item work either way.
    public void setEditable(boolean v) {
        editable = v;
    }

    public boolean isOpenOnTap() {
        return openOnTap;
    }

    /// Whether a single tap opens an item. The default is true on a touch
    /// screen and false where there is a mouse, which double clicks to open.
    public void setOpenOnTap(boolean v) {
        openOnTap = v;
    }

    /// Whether a plain drag edits: moves the item under it, or selects a
    /// range of empty time. The default is true where there is a mouse and
    /// false on a touch screen, where a drag scrolls and a long press edits.
    public void setDragEdits(boolean v) {
        dragEdits = Boolean.valueOf(v);
        apply();
    }

    public ScheduleItemRenderer getItemRenderer() {
        return renderer;
    }

    public void setItemRenderer(ScheduleItemRenderer r) {
        if (r == null) {
            throw new IllegalArgumentException("renderer");
        }
        renderer = r;
        repaint();
    }

    /// Fixes what the scheduler takes the current time to be, for a
    /// screenshot or a test. A negative value returns to the clock.
    public void setCurrentTime(long millis) {
        fixedNow = millis;
        repaint();
    }

    long now() {
        return fixedNow >= 0 ? fixedNow : System.currentTimeMillis();
    }

    // ---------------------------------------------------------- selection

    public ScheduleItem getSelectedItem() {
        return selected;
    }

    /// Selects an item, or nothing when `item` is null. The listeners are
    /// not told: this is the application's own doing.
    public void setSelectedItem(ScheduleItem item) {
        selected = item;
        repaint();
    }

    public void addSchedulerListener(SchedulerListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeSchedulerListener(SchedulerListener l) {
        listeners.remove(l);
    }

    // The listeners as they are now: one that removes itself, or another,
    // while it is told does not make the loop skip the one after it, which
    // for `itemChanging` would be a refusal nobody heard.
    private SchedulerListener[] told() {
        return listeners.toArray(new SchedulerListener[listeners.size()]);
    }

    void viewSelected(ScheduleItem item) {
        if (selected == item) { //NOPMD CompareObjectsWithEquals - the selection is this very object
            return;
        }
        selected = item;
        for (SchedulerListener l : told()) {
            l.itemSelected(this, item);
        }
        repaint();
    }

    void viewOpened(ScheduleItem item) {
        for (SchedulerListener l : told()) {
            l.itemOpened(this, item);
        }
    }

    void viewRange(long start, long end, String resourceId) {
        for (SchedulerListener l : told()) {
            l.rangeSelected(this, start, end, resourceId);
        }
        if (model != null) {
            ScheduleItem made = model.createItem(start, end, resourceId);
            if (made != null) {
                viewSelected(made);
            }
        }
        repaint();
    }

    boolean viewChanged(ScheduleItem item, long start, long end, String resourceId) {
        for (SchedulerListener l : told()) {
            if (!l.itemChanging(this, item, start, end, resourceId)) {
                repaint();
                return false;
            }
        }
        boolean done = model != null && model.updateItem(item, start, end, resourceId);
        refresh();
        return done;
    }

    // A day was tapped in a view that shows many: go to it.
    void viewDrill(long day) {
        for (View v : views) {
            if (v == View.DAY) {
                anchorDay = day;
                setView(View.DAY);
                return;
            }
        }
    }

    // ---------------------------------------------------------------- text

    /// The time of day of an instant, as the views write it.
    public String formatTime(long millis) {
        return Dates.time(TimeMath.minuteOfDay(millis, zone), is24Hours(), false);
    }

    /// A start and an end as the views write them: "9:00 AM - 10:30 AM".
    public String formatRange(long start, long end) {
        if (end - start >= TimeMath.DAY) {
            long a = TimeMath.epochDay(start, zone);
            long b = TimeMath.epochDay(Math.max(start, end - 1), zone);
            return a == b ? Dates.monthDay(a) : Dates.monthDay(a) + " - " + Dates.monthDay(b);
        }
        return formatTime(start) + " - " + formatTime(end);
    }

    String localize(String key, String value) {
        return UIManager.getInstance().localize(key, value);
    }

    // ----------------------------------------------------------- lifecycle

    @Override
    protected void initComponent() {
        super.initComponent();
        Form f = getComponentForm();
        if (f != null && clock == null) {
            // The line of the current time moves, and midnight changes which
            // day is today.
            clock = UITimer.timer(60000, true, f, new Runnable() {
                @Override
                public void run() {
                    repaint();
                }
            });
        }
    }

    @Override
    protected void deinitialize() {
        if (clock != null) {
            clock.cancel();
            clock = null;
        }
        super.deinitialize();
    }

    /// The range the toolbar names, for a printed page's heading.
    String titleText() {
        return title.getText();
    }

    /// The surface of the current view, for the tests of this package.
    Component surface() {
        return current;
    }
}
