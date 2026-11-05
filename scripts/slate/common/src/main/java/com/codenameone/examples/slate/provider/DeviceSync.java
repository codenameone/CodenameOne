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
package com.codenameone.examples.slate.provider;

import com.codename1.calendar.CalendarChangeListener;
import com.codename1.calendar.LocalCalendarSource;
import com.codename1.io.Preferences;
import com.codenameone.examples.slate.Telemetry;
import com.codenameone.examples.slate.api.BusyUploadDto;
import com.codenameone.examples.slate.api.IntervalDto;
import com.codenameone.examples.slate.calendar.DeviceCalendar;
import com.codenameone.examples.slate.client.Times;
import com.codenameone.examples.slate.net.Api;
import com.codenameone.examples.slate.net.Net;

import java.util.ArrayList;
import java.util.List;

// tag::slate-device-busy[]
/// Sends the server when the provider is busy in the phone's own calendars.
///
/// Only intervals leave the phone: a start and an end, no title, no
/// attendees, nothing that says what the time is for. The server stores them
/// against the provider's `DEVICE` link and the slot engine treats them as it
/// treats a Google or Microsoft calendar's busy blocks.
///
/// It uploads the next `DAYS` days when the provider's home comes forward,
/// and again whenever the calendar changes underneath, on a platform that
/// can say so (`LOCAL_CHANGE_LISTENER`).
public final class DeviceSync {
    static final String LINK = "slate.device.link";
    static final String CALENDARS = "slate.device.calendars";
    private static final int DAYS = 30;
    private static CalendarChangeListener listening;

    private DeviceSync() {
    }

    /// Starts keeping `linkId` up to date from `calendars`, a comma separated
    /// list of the phone's calendar ids.
    public static void start(String linkId, String calendars) {
        Preferences.set(LINK, linkId);
        Preferences.set(CALENDARS, calendars);
        resume();
    }

    /// Uploads once, and listens for changes, if a device link is set up.
    public static void resume() {
        if (Preferences.get(LINK, "").length() == 0 || !DeviceCalendar.canReadBusy()) {
            return;
        }
        upload();
        if (listening == null && DeviceCalendar.canListen()) {
            listening = change -> upload();
            LocalCalendarSource.getInstance().addChangeListener(listening);
        }
    }

    /// Stops listening. Signing out, or disconnecting the link.
    public static void stop() {
        if (listening != null) {
            LocalCalendarSource.getInstance().removeChangeListener(listening);
            listening = null;
        }
    }

    /// Forgets the link: the next provider on this phone starts afresh.
    public static void forget() {
        stop();
        Preferences.delete(LINK);
        Preferences.delete(CALENDARS);
    }

    static void upload() {
        final String link = Preferences.get(LINK, "");
        final long from = Times.dayStart(System.currentTimeMillis());
        final long to = Times.addDays(from, DAYS);
        DeviceCalendar.busy(split(Preferences.get(CALENDARS, "")), from, to, busy -> {
            List<IntervalDto> intervals = new ArrayList<IntervalDto>();
            for (int iter = 0; iter < busy.size(); iter++) {
                long[] span = busy.get(iter);
                intervals.add(new IntervalDto(span[0], span[1]));
            }
            final int count = intervals.size();
            Api.provider().uploadBusy(new BusyUploadDto(link, from, to, intervals),
                    Net.to(done -> Telemetry.busyUploaded(count), (status, message) -> {
                    }));
        });
    }
    // end::slate-device-busy[]

    static List<String> split(String ids) {
        List<String> out = new ArrayList<String>();
        int from = 0;
        while (from < ids.length()) {
            int comma = ids.indexOf(',', from);
            int to = comma < 0 ? ids.length() : comma;
            if (to > from) {
                out.add(ids.substring(from, to));
            }
            from = to + 1;
        }
        return out;
    }
}
