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

import com.codename1.io.Preferences;
import com.codename1.notifications.LocalNotification;
import com.codename1.ui.Display;
import com.codenameone.examples.pulse.Prefs;
import com.codenameone.examples.pulse.api.BookingDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.net.Session;
import com.codenameone.examples.pulse.ui.Lang;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/// A local notification an hour before every class the household holds a
/// place in.
///
/// Local, not pushed: the phone knows the bookings, and a reminder that the
/// phone schedules itself still arrives when the network does not. They are
/// brought in line with the server's list whenever the app comes to the
/// front and whenever a booking is made or cancelled from this phone; a
/// booking made elsewhere is picked up the next time the app is opened.
///
/// The ids scheduled are remembered, so a cancelled booking's reminder is
/// taken back, and so is every reminder when the account signs out.
public final class Reminders {
    private static final String SCHEDULED = "pulse.reminders";
    private static final String PREFIX = "class-";
    private static final long BEFORE_MILLIS = 60L * 60L * 1000L;

    private Reminders() {
    }

    /// The app came to the front: the reminders are checked against the
    /// server's bookings.
    public static void resumed() {
        if (Session.user() != null) {
            refresh();
        }
    }

    /// Reads the bookings and schedules from them.
    public static void refresh() {
        Api.bookings().upcoming(Net.to(Reminders::sync, (status, message) -> { }));
    }

    /// Schedules a reminder for every booked place in `bookings` and takes
    /// back the ones no longer there.
    public static void sync(List<BookingDto> bookings) {
        clear();
        if (!Prefs.notifyClasses() || bookings == null) {
            return;
        }
        long now = System.currentTimeMillis();
        StringBuilder ids = new StringBuilder();
        for (BookingDto booking : bookings) {
            if (!Classes.BOOKED.equals(booking.state()) || booking.session() == null) {
                continue;
            }
            long at = booking.session().startsAt() - BEFORE_MILLIS;
            if (at <= now || quiet(at)) {
                continue;
            }
            LocalNotification note = new LocalNotification();
            note.setId(PREFIX + booking.id());
            note.setAlertTitle(booking.session().name());
            note.setAlertBody(Lang.tr("{0} starts in an hour. See you there, {1}",
                    booking.session().name(), Classes.nullToEmpty(booking.memberName())));
            Display.getInstance().scheduleLocalNotification(note, at,
                    LocalNotification.REPEAT_NONE);
            if (ids.length() > 0) {
                ids.append(',');
            }
            ids.append(note.getId());
        }
        Preferences.set(SCHEDULED, ids.toString());
    }

    /// Takes back every reminder this phone scheduled.
    public static void clear() {
        String ids = Preferences.get(SCHEDULED, "");
        int start = 0;
        while (start < ids.length()) {
            int end = ids.indexOf(',', start);
            if (end < 0) {
                end = ids.length();
            }
            if (end > start) {
                Display.getInstance().cancelLocalNotification(ids.substring(start, end));
            }
            start = end + 1;
        }
        Preferences.set(SCHEDULED, "");
    }

    /// Whether `millis` falls in the quiet hours chosen in Settings, which may
    /// run over midnight.
    static boolean quiet(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(millis));
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int from = Prefs.quietFrom();
        int to = Prefs.quietTo();
        if (from == to) {
            return false;
        }
        return from < to ? hour >= from && hour < to : hour >= from || hour < to;
    }
}
