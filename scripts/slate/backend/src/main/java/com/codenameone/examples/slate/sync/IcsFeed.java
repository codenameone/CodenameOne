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
package com.codenameone.examples.slate.sync;

import com.codename1.backend.Web;

import java.io.IOException;
import java.time.ZoneId;
import java.util.List;

/// A calendar a provider publishes as a feed: the "secret address in iCal
/// format" Google, Outlook and Apple calendars all offer. No sign-in, and no
/// client id to register, which makes it the way to connect a calendar on a
/// server nobody has set OAuth up for.
final class IcsFeed implements ExternalCalendar {
    private final ZoneId zone;

    IcsFeed(ZoneId zone) {
        this.zone = zone;
    }

    @Override
    public String kind() {
        return CalendarLinks.ICS;
    }

    /// Checks the address is one and answers it, with `webcal:` read as
    /// `https:`. Plain `http:` is refused: the address is a secret, and the
    /// feed lists the provider's day.
    @Override
    public String connect(String given, String redirectUri) throws IOException {
        String url = given == null ? "" : given.trim();
        if (url.regionMatches(true, 0, "webcal://", 0, 9)) {
            url = "https://" + url.substring(9);
        }
        if (!url.regionMatches(true, 0, "https://", 0, 8) || url.length() > 1900) {
            throw new IOException("Paste the calendar's https or webcal address");
        }
        fetch(url);
        return url;
    }

    @Override
    public List<long[]> busy(String credential, String calendars, long from, long to)
            throws IOException {
        return Ics.busy(fetch(credential), zone, from, to);
    }

    private static String fetch(String url) throws IOException {
        Web.Result answer = Web.get(url);
        if (answer.getStatus() / 100 != 2) {
            throw new IOException("The calendar address answered " + answer.getStatus());
        }
        String body = answer.getBodyAsString();
        if (body == null || body.indexOf("BEGIN:VCALENDAR") < 0) {
            throw new IOException("That address is not a calendar");
        }
        return body;
    }
}
