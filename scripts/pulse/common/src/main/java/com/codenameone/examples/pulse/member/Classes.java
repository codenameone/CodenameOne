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
package com.codenameone.examples.pulse.member;

import com.codename1.calendar.CalendarAccess;
import com.codename1.calendar.CalendarAlarm;
import com.codename1.calendar.CalendarAuthorizationStatus;
import com.codename1.calendar.CalendarDateTime;
import com.codename1.calendar.CalendarEvent;
import com.codename1.calendar.CalendarMutationScope;
import com.codename1.calendar.LocalCalendarSource;
import com.codenameone.examples.pulse.Telemetry;
import com.codenameone.examples.pulse.api.BookRequestDto;
import com.codenameone.examples.pulse.api.BookingDto;
import com.codenameone.examples.pulse.api.MemberDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/// What every screen that shows a class does with it: books it, cancels it,
/// puts it in the device's calendar, and says how it stands.
///
/// The server decides whether a booking is a place or a place in the queue,
/// and whether the member is old enough, young enough or paid up enough to
/// have either; nothing is checked here first, so the reason the server gives
/// is the one the member reads.
public final class Classes {
    public static final String BOOKED = "BOOKED";
    public static final String WAITLISTED = "WAITLISTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String CHECKED_IN = "CHECKED_IN";

    /// Age bands for the timetable filter.
    public static final String BAND_ALL = "all";
    public static final String BAND_KIDS = "kids";
    public static final String BAND_TEENS = "teens";
    public static final String BAND_ADULTS = "adults";

    private Classes() {
    }

    /// Books `member` into `session`; a full class puts them in its queue.
    public static void book(SessionDto session, MemberDto member, final Runnable done) {
        final boolean child = member != null && "CHILD".equals(member.relation());
        Api.bookings().book(new BookRequestDto(session.id(), member == null ? null : member.id(),
                null), Net.to(booking -> {
                    boolean queued = WAITLISTED.equals(booking.state());
                    Telemetry.booked(queued, child);
                    Ui.say(queued ? Lang.tr("Class is full. You are number {0} in the queue",
                            Integer.valueOf(booking.position())) : Lang.tr("Booked"));
                    Reminders.refresh();
                    done.run();
                }, Ui::fail));
    }

    public static void cancel(BookingDto booking, final Runnable done) {
        Api.bookings().cancel(booking.id(), Net.to(cancelled -> {
            Telemetry.bookingCancelled();
            Ui.say("Cancelled");
            Reminders.refresh();
            done.run();
        }, Ui::fail));
    }

    /// The booking `member` holds in `session`, or null.
    public static BookingDto bookingOf(List<BookingDto> bookings, String sessionId,
            String memberId) {
        for (int iter = 0; bookings != null && iter < bookings.size(); iter++) {
            BookingDto booking = bookings.get(iter);
            if (sessionId.equals(booking.sessionId()) && memberId != null
                    && memberId.equals(booking.memberId())
                    && !CANCELLED.equals(booking.state())) {
                return booking;
            }
        }
        return null;
    }

    /// Whether `session` is for someone of `age`. A maximum of 0 is no maximum.
    public static boolean fits(SessionDto session, int age) {
        return age >= session.minAge() && (session.maxAge() <= 0 || age <= session.maxAge());
    }

    /// Whether `session` belongs in the age band the timetable is filtered to.
    public static boolean inBand(SessionDto session, String band) {
        if (BAND_KIDS.equals(band)) {
            return session.maxAge() > 0 && session.maxAge() <= 12;
        }
        if (BAND_TEENS.equals(band)) {
            return session.minAge() >= 13 && session.maxAge() > 0 && session.maxAge() <= 17;
        }
        if (BAND_ADULTS.equals(band)) {
            return session.minAge() >= 18 || session.maxAge() <= 0 && session.minAge() >= 16;
        }
        return true;
    }

    /// Places left, or the queue, in words.
    public static String places(SessionDto session) {
        int left = session.capacity() - session.booked();
        if ("CANCELLED".equals(session.state())) {
            return Lang.tr("Cancelled by the studio");
        }
        if (left > 0) {
            return Lang.tr("{0} of {1} places left", Integer.valueOf(left),
                    Integer.valueOf(session.capacity()));
        }
        return Lang.tr("Full, {0} waiting", Integer.valueOf(session.waitlisted()));
    }

    /// The trainer, room and time of `session` on one line.
    public static String detail(SessionDto session) {
        return Ui.when(session.startsAt()) + " - " + nullToEmpty(session.trainerName())
                + (session.roomName() == null ? "" : ", " + session.roomName());
    }

    /// Writes `session` into the device's calendar, with an alarm half an hour
    /// before. The device asks the user's permission the first time.
    public static void addToCalendar(final SessionDto session) {
        final LocalCalendarSource calendar = LocalCalendarSource.getInstance();
        if (!calendar.isAvailable()) {
            Ui.say("This device has no calendar to add to");
            return;
        }
        calendar.requestAuthorization(CalendarAccess.EVENTS_WRITE_ONLY).ready(status -> {
            if (status != CalendarAuthorizationStatus.FULL
                    && status != CalendarAuthorizationStatus.WRITE_ONLY) {
                Ui.say("Calendar access was not allowed");
                return;
            }
            ZoneId zone = ZoneId.systemDefault();
            CalendarEvent event = new CalendarEvent()
                    .setTitle(session.name())
                    .setLocation(nullToEmpty(session.locationName()))
                    .setDescription(Lang.tr("With {0}", nullToEmpty(session.trainerName())))
                    .setStart(CalendarDateTime.instant(Instant.ofEpochMilli(session.startsAt()),
                            zone))
                    .setEnd(CalendarDateTime.instant(Instant.ofEpochMilli(session.endsAt()), zone))
                    .addAlarm(new CalendarAlarm().setTimeBefore(Duration.ofMinutes(30)));
            calendar.saveEvent(event, CalendarMutationScope.THIS_INSTANCE).ready(saved -> {
                Telemetry.addedToCalendar();
                Ui.say("Added to your calendar");
            }).except(failed -> Ui.say("Could not add it to your calendar"));
        }).except(failed -> Ui.say("Could not add it to your calendar"));
    }

    static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}
