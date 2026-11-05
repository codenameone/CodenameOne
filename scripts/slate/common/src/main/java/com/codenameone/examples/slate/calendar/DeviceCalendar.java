/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.slate.calendar;

import com.codename1.calendar.CalendarAccess;
import com.codename1.calendar.CalendarAlarm;
import com.codename1.calendar.CalendarAuthorizationStatus;
import com.codename1.calendar.CalendarCapability;
import com.codename1.calendar.CalendarDateTime;
import com.codename1.calendar.CalendarEvent;
import com.codename1.calendar.CalendarInfo;
import com.codename1.calendar.CalendarMutationScope;
import com.codename1.calendar.CalendarPage;
import com.codename1.calendar.CalendarSource;
import com.codename1.calendar.FreeBusyInterval;
import com.codename1.calendar.LocalCalendarSource;
import com.codename1.io.Preferences;
import com.codename1.ui.CN;
import com.codenameone.examples.slate.api.AppointmentDto;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

// tag::slate-device-calendar[]
/// The phone's own calendars, through `com.codename1.calendar`.
///
/// Three things are asked of them, and nothing else:
///
/// - **busy time**, as free/busy intervals and never as events: a client is
///   warned that a slot clashes with their own day, and a provider's busy time
///   is uploaded so nobody books over it. A title, a place or a guest never
///   leaves this class.
/// - **adding an appointment**, with an alarm ahead of it, once it is booked.
/// - **keeping that event true** when the appointment moves or is cancelled.
///
/// Each asks for access the first time it needs it, which is when the user
/// has just tapped the thing that needs it: the platform's prompt then makes
/// sense. Every answer arrives on the EDT.
public final class DeviceCalendar {
    /// What the phone remembers about an appointment it added: the calendar
    /// and event ids as `calendar|event`, keyed by appointment id, so a later
    /// change finds the same event.
    private static final String EVENT_PREFIX = "slate.event.";
    private static final String INDEX = "slate.events";

    /// The answer to a question about the calendars.
    public interface Answer<T> {
        void answer(T value);
    }

    private DeviceCalendar() {
    }

    private static LocalCalendarSource source() {
        return LocalCalendarSource.getInstance();
    }

    /// Whether this phone has calendars the app can read busy time from.
    public static boolean canReadBusy() {
        LocalCalendarSource s = source();
        return s != null && s.isAvailable()
                && s.getCapabilities().supports(CalendarCapability.FREE_BUSY);
    }

    /// Whether this phone can have events added to it.
    public static boolean canWrite() {
        LocalCalendarSource s = source();
        return s != null && s.isAvailable()
                && s.getCapabilities().supports(CalendarCapability.WRITE_EVENTS);
    }

    /// Whether the phone can tell the app when its calendars change, so busy
    /// time is uploaded again without waiting for the next sync.
    public static boolean canListen() {
        LocalCalendarSource s = source();
        return s != null && s.isAvailable()
                && s.getCapabilities().supports(CalendarCapability.LOCAL_CHANGE_LISTENER);
    }

    /// Asks for `access` when it was never asked, and answers whether it is held.
    public static void authorize(final CalendarAccess access, final Answer<Boolean> done) {
        final LocalCalendarSource s = source();
        if (s == null || !s.isAvailable()) {
            done.answer(Boolean.FALSE);
            return;
        }
        CalendarAuthorizationStatus now = s.getAuthorizationStatus(access);
        if (granted(now, access)) {
            done.answer(Boolean.TRUE);
            return;
        }
        if (now != CalendarAuthorizationStatus.NOT_DETERMINED) {
            done.answer(Boolean.FALSE);
            return;
        }
        s.requestAuthorization(access)
                .ready(status -> later(() -> done.answer(Boolean.valueOf(granted(status, access)))))
                .except(failed -> later(() -> done.answer(Boolean.FALSE)));
    }

    private static boolean granted(CalendarAuthorizationStatus status, CalendarAccess access) {
        if (status == CalendarAuthorizationStatus.FULL) {
            return true;
        }
        return status == CalendarAuthorizationStatus.WRITE_ONLY
                && access == CalendarAccess.EVENTS_WRITE_ONLY;
    }

    /// The calendars on the phone, for a provider choosing which count as busy.
    public static void calendars(final Answer<List<CalendarInfo>> done) {
        authorize(CalendarAccess.EVENTS_READ_ONLY, ok -> {
            if (!ok.booleanValue()) {
                done.answer(new ArrayList<CalendarInfo>());
                return;
            }
            source().listCalendars(CalendarInfo.ContentType.EVENTS, null)
                    .ready(page -> later(() -> done.answer(items(page))))
                    .except(failed -> later(() -> done.answer(new ArrayList<CalendarInfo>())));
        });
    }

    private static List<CalendarInfo> items(CalendarPage<CalendarInfo> page) {
        List<CalendarInfo> out = new ArrayList<CalendarInfo>();
        if (page != null && page.getItems() != null) {
            out.addAll(page.getItems());
        }
        return out;
    }

    /// Busy time between `from` and `to` in the calendars named by `ids`, or
    /// in every calendar when `ids` is empty, as `{start, end}` millis. Time
    /// marked free does not count. Empty when access is refused.
    public static void busy(final List<String> ids, final long from, final long to,
            final Answer<List<long[]>> done) {
        authorize(CalendarAccess.EVENTS_READ_ONLY, ok -> {
            if (!ok.booleanValue()) {
                done.answer(new ArrayList<long[]>());
                return;
            }
            if (ids != null && ids.size() > 0) {
                query(ids, from, to, done);
                return;
            }
            calendars(all -> {
                List<String> every = new ArrayList<String>();
                for (int iter = 0; iter < all.size(); iter++) {
                    every.add(all.get(iter).getId());
                }
                query(every, from, to, done);
            });
        });
    }

    private static void query(List<String> ids, long from, long to,
            final Answer<List<long[]>> done) {
        if (ids.isEmpty()) {
            done.answer(new ArrayList<long[]>());
            return;
        }
        source().queryFreeBusy(ids, Instant.ofEpochMilli(from), Instant.ofEpochMilli(to))
                .ready(list -> later(() -> done.answer(spans(list))))
                .except(failed -> later(() -> done.answer(new ArrayList<long[]>())));
    }

    private static List<long[]> spans(List<FreeBusyInterval> list) {
        List<long[]> out = new ArrayList<long[]>();
        for (int iter = 0; list != null && iter < list.size(); iter++) {
            FreeBusyInterval i = list.get(iter);
            if (i.getAvailability() == CalendarEvent.Availability.FREE) {
                continue;
            }
            out.add(new long[] {i.getStartTime().toEpochMilli(), i.getEndTime().toEpochMilli()});
        }
        return out;
    }
    // end::slate-device-calendar[]

    /// Whether `[start, end)` meets any of `busy`.
    public static boolean clashes(List<long[]> busy, long start, long end) {
        for (int iter = 0; iter < busy.size(); iter++) {
            long[] b = busy.get(iter);
            if (b[0] < end && start < b[1]) {
                return true;
            }
        }
        return false;
    }

    // tag::slate-add-to-calendar[]
    /// Adds `a` to the phone's default calendar with an alarm `alarmMinutes`
    /// ahead, or moves the event added for it before. Answers whether it is
    /// there now.
    public static void save(final AppointmentDto a, final String title, final int alarmMinutes,
            final Answer<Boolean> done) {
        authorize(CalendarAccess.EVENTS_WRITE_ONLY, ok -> {
            if (!ok.booleanValue()) {
                done.answer(Boolean.FALSE);
                return;
            }
            ZoneId zone = ZoneId.systemDefault();
            CalendarEvent event = new CalendarEvent()
                    .setTitle(title)
                    .setLocation(a.online() ? a.meetingUrl() : a.locationName())
                    .setDescription(a.icsUrl() == null ? "" : a.icsUrl())
                    .setStart(CalendarDateTime.instant(Instant.ofEpochMilli(a.start()), zone))
                    .setEnd(CalendarDateTime.instant(Instant.ofEpochMilli(a.end()), zone))
                    .setAvailability(CalendarEvent.Availability.BUSY)
                    .putProviderData("slate.appointment", a.id());
            String known = eventOf(a.id());
            int bar = known.indexOf('|');
            if (bar >= 0) {
                event.setCalendarId(bar > 0 ? known.substring(0, bar) : null);
                event.setId(known.substring(bar + 1));
            }
            if (alarmMinutes > 0 && source().getCapabilities().supports(CalendarCapability.ALARMS)) {
                event.addAlarm(new CalendarAlarm().setTimeBefore(Duration.ofMinutes(alarmMinutes)));
            }
            source().saveEvent(event, CalendarMutationScope.THIS_INSTANCE)
                    .ready(saved -> later(() -> {
                        if (saved != null && saved.getId() != null) {
                            String calendar = saved.getCalendarId() == null ? ""
                                    : saved.getCalendarId();
                            Preferences.set(EVENT_PREFIX + a.id(), calendar + "|" + saved.getId());
                            index(a.id(), true);
                        }
                        done.answer(Boolean.TRUE);
                    }))
                    .except(failed -> later(() -> done.answer(Boolean.FALSE)));
        });
    }
    // end::slate-add-to-calendar[]

    /// Whether the phone holds an event this app added for appointment `id`.
    public static boolean added(String id) {
        return eventOf(id).length() > 0;
    }

    /// Removes the event added for appointment `id`, if there is one.
    public static void remove(final String id) {
        final String known = eventOf(id);
        if (known.length() == 0) {
            return;
        }
        Preferences.delete(EVENT_PREFIX + id);
        index(id, false);
        CalendarSource s = source();
        if (s == null || !s.isAvailable()) {
            return;
        }
        int bar = known.indexOf('|');
        String calendar = bar > 0 ? known.substring(0, bar) : null;
        s.deleteEvent(calendar, known.substring(bar + 1), CalendarMutationScope.THIS_INSTANCE, null);
    }

    /// The appointments this app has put an event in the calendar for.
    /// Preferences cannot list their keys, so the ids are kept as one
    /// comma-separated value beside the events themselves.
    public static List<String> owned() {
        List<String> out = new ArrayList<String>();
        String all = Preferences.get(INDEX, "");
        int from = 0;
        while (from < all.length()) {
            int comma = all.indexOf(',', from);
            int to = comma < 0 ? all.length() : comma;
            if (to > from) {
                out.add(all.substring(from, to));
            }
            from = to + 1;
        }
        return out;
    }

    /// Removes every event this app added.
    public static void removeAll() {
        List<String> ids = owned();
        for (int iter = 0; iter < ids.size(); iter++) {
            remove(ids.get(iter));
        }
        Preferences.delete(INDEX);
    }

    private static void index(String id, boolean add) {
        List<String> ids = owned();
        ids.remove(id);
        if (add) {
            ids.add(id);
        }
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < ids.size(); iter++) {
            if (iter > 0) {
                b.append(',');
            }
            b.append(ids.get(iter));
        }
        Preferences.set(INDEX, b.toString());
    }

    private static String eventOf(String id) {
        return Preferences.get(EVENT_PREFIX + id, "");
    }

    private static void later(Runnable r) {
        CN.callSerially(r);
    }
}
