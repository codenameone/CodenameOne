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
package com.codename1.scheduling;

import com.codename1.calendar.CalendarChange;
import com.codename1.calendar.CalendarChangeListener;
import com.codename1.calendar.CalendarDateTime;
import com.codename1.calendar.CalendarEvent;
import com.codename1.calendar.CalendarInfo;
import com.codename1.calendar.CalendarMutationScope;
import com.codename1.calendar.CalendarPage;
import com.codename1.calendar.CalendarQuery;
import com.codename1.calendar.CalendarSource;
import com.codename1.calendar.FreeBusyInterval;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.util.SuccessCallback;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A schedule read from a `CalendarSource`: the device's calendars, CalDAV,
/// Google or Microsoft.
///
/// Every calendar added is a resource, so a resource view shows calendars
/// side by side and dragging an event to another column moves it to that
/// calendar. The model loads the events around the range a view shows,
/// with recurring events expanded into their occurrences, and loads again
/// when the view moves outside what it has or the source reports a change.
///
/// ```java
/// LocalCalendarSource source = LocalCalendarSource.getInstance();
/// CalendarScheduleModel model = new CalendarScheduleModel(source, ZoneId.systemDefault());
/// source.listCalendars(CalendarInfo.ContentType.EVENTS, null).ready(page -> {
///     for (CalendarInfo calendar : page.getItems()) {
///         model.addCalendar(calendar);
///     }
/// });
/// Scheduler scheduler = new Scheduler(model);
/// ```
///
/// An edit is shown at once and saved in the background; if the source
/// refuses it, the item goes back and `getLastError` says why.
public class CalendarScheduleModel extends AbstractScheduleModel {
    private final CalendarSource source;
    private final ZoneId zone;
    private final List<ScheduleResource> resources = new ArrayList<ScheduleResource>();
    private final Map<String, CalendarInfo> calendars = new HashMap<String, CalendarInfo>();
    private final Map<String, List<ScheduleItem>> items = new HashMap<String, List<ScheduleItem>>();
    private final Map<String, List<ScheduleItem>> busy = new HashMap<String, List<ScheduleItem>>();
    private final List<String> freeBusyOnly = new ArrayList<String>();
    private CalendarMutationScope mutationScope = CalendarMutationScope.THIS_INSTANCE;
    private int created;
    private int loads;
    private int pending;
    private boolean creationEnabled;
    private String newEventTitle = "";
    private long loadedFrom;
    private long loadedTo;
    private long wantedFrom;
    private long wantedTo;
    // Raised by every reload, so an answer to an older question is dropped.
    private int generation;
    private Throwable lastError;
    private final CalendarChangeListener sourceListener = new CalendarChangeListener() {
        @Override
        public void calendarChanged(CalendarChange change) {
            if (Display.getInstance().isEdt()) {
                reload();
            } else {
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        reload();
                    }
                });
            }
        }
    };

    /// A model over `source`. `zone` decides where an all-day event's days
    /// start; use the zone of the view.
    public CalendarScheduleModel(CalendarSource source, ZoneId zone) {
        if (source == null || zone == null) {
            throw new IllegalArgumentException("source and zone are required");
        }
        this.source = source;
        this.zone = zone;
        source.addChangeListener(sourceListener);
    }

    /// Stops listening to the source. Call it when the model is discarded.
    public void dispose() {
        source.removeChangeListener(sourceListener);
        generation++;
    }

    public CalendarSource getSource() {
        return source;
    }

    /// Shows a calendar's events, as a resource of its own.
    public CalendarScheduleModel addCalendar(CalendarInfo info) {
        return add(info, false);
    }

    /// Shows only when a calendar is busy, hatched, without any detail: the
    /// way somebody else's calendar is shown.
    public CalendarScheduleModel addFreeBusyCalendar(CalendarInfo info) {
        return add(info, true);
    }

    private CalendarScheduleModel add(CalendarInfo info, boolean freeBusy) {
        if (info == null || info.getId() == null || calendars.containsKey(info.getId())) {
            return this;
        }
        calendars.put(info.getId(), info);
        ScheduleResource r = new ScheduleResource(info.getId(), info.getName());
        r.setDetail(info.getOwner());
        if (info.getColor() != 0) {
            r.setColor(info.getColor() & 0xffffff);
        }
        r.setUserObject(info);
        resources.add(r);
        if (freeBusy) {
            freeBusyOnly.add(info.getId());
        }
        reload();
        return this;
    }

    public CalendarScheduleModel removeCalendar(String calendarId) {
        if (calendars.remove(calendarId) != null) {
            for (int i = resources.size() - 1; i >= 0; i--) {
                if (resources.get(i).getId().equals(calendarId)) {
                    resources.remove(i);
                }
            }
            items.remove(calendarId);
            busy.remove(calendarId);
            freeBusyOnly.remove(calendarId);
            fireChanged();
        }
        return this;
    }

    /// Which occurrences an edit of a recurring event changes. A view asks the
    /// user and sets this before it commits the edit. One occurrence by default.
    public CalendarScheduleModel setMutationScope(CalendarMutationScope scope) {
        mutationScope = scope == null ? CalendarMutationScope.THIS_INSTANCE : scope;
        return this;
    }

    public CalendarMutationScope getMutationScope() {
        return mutationScope;
    }

    /// Whether dragging on empty space saves a new event with `title`. Off by
    /// default: an application usually opens its own editor from the
    /// scheduler's range listener instead.
    public CalendarScheduleModel setCreationEnabled(boolean enabled, String title) {
        creationEnabled = enabled;
        newEventTitle = title == null ? "" : title;
        return this;
    }

    /// The last failure of a load, a save or a delete, or null. It stays
    /// until `reload` is called or a later save or delete works: one
    /// calendar that loads says nothing of another that did not.
    public Throwable getLastError() {
        return lastError;
    }

    /// The calendar event an item was made from, or null for busy time.
    public static CalendarEvent eventOf(ScheduleItem item) {
        Object o = item == null ? null : item.getUserObject();
        return o instanceof CalendarEvent ? (CalendarEvent) o : null;
    }

    @Override
    public List<ScheduleResource> getResources() {
        return Collections.unmodifiableList(resources);
    }

    @Override
    public void getItems(long from, long to, List<ScheduleItem> out) {
        collect(items, from, to, out);
        collect(busy, from, to, out);
    }

    private void collect(Map<String, List<ScheduleItem>> map, long from, long to, List<ScheduleItem> out) {
        for (ScheduleResource resource : resources) {
            List<ScheduleItem> list = map.get(resource.getId());
            if (list == null) {
                continue;
            }
            for (ScheduleItem item : list) {
                if (item.intersects(from, to)) {
                    out.add(item);
                }
            }
        }
    }

    @Override
    public void setVisibleRange(long from, long to) {
        wantedFrom = from;
        wantedTo = to;
        if (from < loadedFrom || to > loadedTo) {
            reload();
        }
    }

    /// Loads the events around the visible range again.
    public void reload() {
        // A failure is reported until something is asked for again.
        lastError = null;
        load();
    }

    private void load() {
        if (wantedTo <= wantedFrom || calendars.isEmpty()) {
            return;
        }
        // A window three times the range, so scrolling one screen either way
        // asks for nothing.
        long span = wantedTo - wantedFrom;
        final long from = wantedFrom - span;
        final long to = wantedTo + span;
        loadedFrom = from;
        loadedTo = to;
        final int asked = ++generation;
        pending = resources.size();
        for (ScheduleResource resource : resources) {
            String id = resource.getId();
            if (freeBusyOnly.contains(id)) {
                loadBusy(id, from, to, asked);
            } else {
                loadPage(id, from, to, null, new ArrayList<ScheduleItem>(), asked);
            }
        }
    }

    private void loadPage(final String calendarId, final long from, final long to, String pageToken,
                          final List<ScheduleItem> into, final int asked) {
        CalendarQuery q = new CalendarQuery().setCalendarId(calendarId)
                .setStartTime(Instant.ofEpochMilli(from)).setEndTime(Instant.ofEpochMilli(to))
                .setExpandRecurrences(true);
        if (pageToken != null) {
            q.setPageToken(pageToken);
        }
        source.queryEvents(q).ready(new SuccessCallback<CalendarPage<CalendarEvent>>() {
            @Override
            public void onSucess(CalendarPage<CalendarEvent> page) {
                if (asked != generation || !calendars.containsKey(calendarId)) {
                    // Answered after a newer question, or for a calendar
                    // that was taken away while it was asked.
                    answered(asked);
                    return;
                }
                List<CalendarEvent> events = page.getItems();
                for (CalendarEvent event : events) {
                    ScheduleItem item = toItem(event, calendarId);
                    if (item != null) {
                        into.add(item);
                    }
                }
                String next = page.getNextPageToken();
                if (next != null && next.length() > 0) {
                    loadPage(calendarId, from, to, next, into, asked);
                    return;
                }
                // One calendar that answered says nothing about another that
                // failed in the same load, so the error is left as it is.
                items.put(calendarId, into);
                answered(asked);
                loads++;
                fireChanged();
            }
        }).except(failed(asked, calendarId));
    }

    private void loadBusy(final String calendarId, long from, long to, final int asked) {
        List<String> ids = new ArrayList<String>();
        ids.add(calendarId);
        source.queryFreeBusy(ids, Instant.ofEpochMilli(from), Instant.ofEpochMilli(to))
                .ready(new SuccessCallback<List<FreeBusyInterval>>() {
                    @Override
                    public void onSucess(List<FreeBusyInterval> intervals) {
                        answered(asked);
                        if (asked != generation || !calendars.containsKey(calendarId)) {
                            return;
                        }
                        List<ScheduleItem> list = new ArrayList<ScheduleItem>();
                        for (FreeBusyInterval f : intervals) {
                            if (f.getAvailability() == CalendarEvent.Availability.FREE) {
                                continue;
                            }
                            long s = f.getStartTime().toEpochMilli();
                            list.add(new ScheduleItem(calendarId + "-busy-" + s, "", s, f.getEndTime().toEpochMilli())
                                    .setKind(ScheduleItem.Kind.BLOCKED).setResourceId(calendarId));
                        }
                        busy.put(calendarId, list);
                        fireChanged();
                    }
                }).except(failed(asked, calendarId));
    }

    private SuccessCallback<Throwable> failed(final int asked, final String calendarId) {
        return new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable err) {
                answered(asked);
                if (asked == generation && calendars.containsKey(calendarId)) {
                    lastError = err;
                    Log.e(err);
                    // Nothing is loaded for this window, so the next range
                    // asked for, even the same one, asks the source again.
                    loadedFrom = 0;
                    loadedTo = 0;
                    fireChanged();
                }
            }
        };
    }

    private ScheduleItem toItem(CalendarEvent e, String calendarId) {
        CalendarDateTime s = e.getStart();
        if (s == null || e.getStatus() == CalendarEvent.Status.CANCELED) {
            return null;
        }
        CalendarDateTime en = e.getEnd();
        long start;
        long end;
        if (s.isAllDay()) {
            long first = s.getDate().toEpochDay();
            // An all-day end is the day after the last one; a source that
            // leaves it out or repeats the start means a single day.
            long after = en != null && en.isAllDay() ? en.getDate().toEpochDay() : first + 1;
            if (after <= first) {
                after = first + 1;
            }
            start = TimeMath.startOfEpochDay(first, zone);
            end = TimeMath.startOfEpochDay(after, zone);
        } else {
            start = s.getDateTime().toInstant().toEpochMilli();
            end = en == null || en.isAllDay() ? start : en.getDateTime().toInstant().toEpochMilli();
            if (end < start) {
                end = start;
            }
        }
        boolean series = e.getRecurrence() != null || e.getRecurringEventId() != null;
        // Every occurrence of an expanded series carries the series' id.
        String id = series ? e.getId() + "@" + start : e.getId();
        CalendarInfo info = calendars.get(calendarId);
        boolean editable = info == null || !info.isReadOnly();
        return new ScheduleItem(id == null ? calendarId + "@" + start : id, e.getTitle(), start, end)
                .setAllDay(s.isAllDay()).setDetail(e.getLocation()).setResourceId(calendarId)
                .setRecurring(series).setMovable(editable).setResizable(editable).setUserObject(e);
    }

    @Override
    public boolean updateItem(ScheduleItem given, long start, long end, String resourceId) {
        // A load since the caller was given its item made another object
        // for the same event, and that one is what the views draw.
        final ScheduleItem item = held(given);
        final CalendarEvent e = item == null ? null : eventOf(item);
        if (e == null || !item.isMovable()) {
            return false;
        }
        final int seen = loads;
        final long oldStart = item.getStart();
        final long oldEnd = item.getEnd();
        final String oldResource = item.getResourceId();
        final CalendarDateTime oldS = e.getStart();
        final CalendarDateTime oldE = e.getEnd();
        final String target = resourceId == null ? oldResource : resourceId;
        if (item.isAllDay()) {
            e.setStart(CalendarDateTime.allDay(java.time.LocalDate.ofEpochDay(TimeMath.epochDay(start, zone))));
            e.setEnd(CalendarDateTime.allDay(java.time.LocalDate.ofEpochDay(TimeMath.epochDay(end, zone))));
        } else {
            ZoneId z = oldS.getDateTime().getZone();
            e.setStart(CalendarDateTime.instant(Instant.ofEpochMilli(start), z));
            e.setEnd(CalendarDateTime.instant(Instant.ofEpochMilli(end), z));
        }
        e.setCalendarId(target);
        final CalendarDateTime newS = e.getStart();
        item.setTime(start, end);
        move(item, oldResource, target);
        fireChanged();
        source.saveEvent(e, mutationScope).ready(new SuccessCallback<CalendarEvent>() {
            @Override
            public void onSucess(CalendarEvent saved) {
                lastError = null;
                // Changing a series moves occurrences that are not this item.
                if (item.isRecurring() && mutationScope != CalendarMutationScope.THIS_INSTANCE) {
                    reload();
                } else {
                    loadIfOvertaken(seen);
                }
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable err) {
                lastError = err;
                Log.e(err);
                // The item goes back at once, but only if this was the last
                // edit of it and it is still what the model shows: a later
                // edit has its own answer coming, and after a reload the
                // item is a copy nobody draws. Either way the source is then
                // asked what it has, which is the only thing known for sure.
                List<ScheduleItem> shown = items.get(target);
                if (e.getStart() == newS && shown != null && shown.contains(item)) { //NOPMD CompareObjectsWithEquals - this edit's own value
                    e.setStart(oldS).setEnd(oldE).setCalendarId(oldResource);
                    item.setTime(oldStart, oldEnd);
                    move(item, target, oldResource);
                }
                fireChanged();
                load();
            }
        });
        return true;
    }

    // The item the model holds for the event of `item`: the same object,
    // or the one a later load made for the same id. Null when it is gone.
    private ScheduleItem held(ScheduleItem item) {
        if (item == null) {
            return null;
        }
        // In its own calendar first: a source may give one meeting the same
        // id in two calendars that are both shown, and the copy that was
        // dragged is the one to change, not whichever comes first.
        ScheduleItem own = sameId(items.get(item.getResourceId()), item);
        if (own != null) {
            return own;
        }
        // Then in every other, since the event may have been moved to
        // another since; and failing its id by the event behind it: an item
        // that was created here has an id of its own making, and the load
        // after it was saved has the source's.
        CalendarEvent e = eventOf(item);
        String eventId = e == null ? null : e.getId();
        // Its own calendar first here too, for the same reason.
        ScheduleItem sameEvent = sameEvent(items.get(item.getResourceId()), eventId);
        for (List<ScheduleItem> list : items.values()) {
            ScheduleItem found = sameId(list, item);
            if (found != null) {
                return found;
            }
            if (sameEvent == null) {
                sameEvent = sameEvent(list, eventId);
            }
        }
        return sameEvent;
    }

    // The item of the list that is the event with this id, and not one
    // occurrence of many.
    private static ScheduleItem sameEvent(List<ScheduleItem> list, String eventId) {
        if (list == null || eventId == null) {
            return null;
        }
        for (ScheduleItem each : list) {
            if (!each.isRecurring()) {
                CalendarEvent other = eventOf(each);
                if (other != null && eventId.equals(other.getId())) {
                    return each;
                }
            }
        }
        return null;
    }

    // The item itself when the list has it, or else the one with its id.
    private static ScheduleItem sameId(List<ScheduleItem> list, ScheduleItem item) {
        if (list == null) {
            return null;
        }
        ScheduleItem byId = null;
        for (ScheduleItem each : list) {
            if (each == item) { //NOPMD CompareObjectsWithEquals - the very item is the common case
                return each;
            }
            if (byId == null && each.getId().equals(item.getId())) {
                byId = each;
            }
        }
        return byId;
    }

    // An answer to a load that was asked before an edit was made, and came
    // after it, drew the event where it was. The edit has now been saved,
    // so what the source holds is asked for once more.
    private void loadIfOvertaken(int seen) {
        // One that has answered, or one that is still to answer: either
        // way what it says was read before the edit.
        if (loads != seen || pending > 0) {
            load();
        }
    }

    // One calendar of the load that is under way has given its last answer.
    private void answered(int asked) {
        if (asked == generation && pending > 0) {
            pending--;
        }
    }

    private void move(ScheduleItem item, String from, String to) {
        item.setResourceId(to);
        if (from == null ? to == null : from.equals(to)) {
            return;
        }
        List<ScheduleItem> a = items.get(from);
        if (a != null) {
            a.remove(item);
        }
        List<ScheduleItem> b = items.get(to);
        if (b == null) {
            b = new ArrayList<ScheduleItem>();
            items.put(to, b);
        }
        b.add(item);
    }

    @Override
    public ScheduleItem createItem(long start, long end, String resourceId) {
        String calendarId = resourceId;
        if (calendarId == null && !resources.isEmpty()) {
            calendarId = resources.get(0).getId();
        }
        CalendarInfo info = calendars.get(calendarId);
        if (!creationEnabled || info == null || info.isReadOnly() || freeBusyOnly.contains(calendarId) || end <= start) {
            return null;
        }
        final CalendarEvent e = new CalendarEvent().setCalendarId(calendarId).setTitle(newEventTitle)
                .setStart(CalendarDateTime.instant(Instant.ofEpochMilli(start), zone))
                .setEnd(CalendarDateTime.instant(Instant.ofEpochMilli(end), zone));
        final ScheduleItem item = new ScheduleItem("new-" + (++created) + "-" + start, newEventTitle, start, end)
                .setResourceId(calendarId).setUserObject(e);
        move(item, null, calendarId);
        fireChanged();
        final String cal = calendarId;
        final int seen = loads;
        source.saveEvent(e, CalendarMutationScope.ALL).ready(new SuccessCallback<CalendarEvent>() {
            @Override
            public void onSucess(CalendarEvent saved) {
                lastError = null;
                if (saved != null) {
                    item.setUserObject(saved);
                }
                loadIfOvertaken(seen);
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable err) {
                lastError = err;
                Log.e(err);
                List<ScheduleItem> list = items.get(cal);
                if (list != null) {
                    list.remove(item);
                }
                fireChanged();
            }
        });
        return item;
    }

    @Override
    public boolean removeItem(ScheduleItem given) {
        // The held item has the version the source gave last.
        final ScheduleItem found = held(given);
        final ScheduleItem item = found == null ? given : found;
        final CalendarEvent e = eventOf(item);
        if (e == null || !item.isMovable()) {
            return false;
        }
        final int seen = loads;
        final String cal = item.getResourceId();
        final List<ScheduleItem> list = items.get(cal);
        // A load since the caller was given this item made another object
        // for the same event, so the one to take out is found by its id.
        ScheduleItem held = null;
        if (list != null) {
            for (ScheduleItem each : list) {
                if (each.getId().equals(item.getId())) {
                    held = each;
                    break;
                }
            }
            if (held != null) {
                list.remove(held);
            }
        }
        final ScheduleItem taken = held;
        fireChanged();
        source.deleteEvent(cal, e.getId(), mutationScope, e.getVersion()).ready(new SuccessCallback<Boolean>() {
            @Override
            public void onSucess(Boolean done) {
                lastError = null;
                loadIfOvertaken(seen);
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable err) {
                lastError = err;
                Log.e(err);
                // Back at once if nothing was loaded in between, and then
                // whatever the source really holds.
                if (taken != null && items.get(cal) == list && !list.contains(taken)) { //NOPMD CompareObjectsWithEquals - the same list, not an equal one
                    list.add(taken);
                }
                fireChanged();
                load();
            }
        });
        return true;
    }
}
