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

import java.time.ZoneId;

/// The calendars this server can read, chosen once from its settings by
/// `Services`. A kind with no client set up is simulated on a development
/// profile and missing anywhere else, where connecting one answers that the
/// server is not set up for it.
public final class CalendarSources {
    private final ExternalCalendar google;
    private final ExternalCalendar microsoft;
    private final ExternalCalendar ics;
    private final String googleClientId;
    private final String microsoftClientId;

    public CalendarSources(String googleClientId, String googleSecret, String microsoftClientId,
            String microsoftSecret, boolean simulate, ZoneId zone) {
        this.googleClientId = googleSecret.length() > 0 ? googleClientId : "";
        this.microsoftClientId = microsoftSecret.length() > 0 ? microsoftClientId : "";
        google = this.googleClientId.length() > 0
                ? new GoogleCalendar(googleClientId, googleSecret)
                : simulate ? new SimulatedCalendar(CalendarLinks.GOOGLE, zone) : null;
        microsoft = this.microsoftClientId.length() > 0
                ? new MicrosoftCalendar(microsoftClientId, microsoftSecret)
                : simulate ? new SimulatedCalendar(CalendarLinks.MICROSOFT, zone) : null;
        ics = new IcsFeed(zone);
    }

    /// The calendar that reads links of `kind`, or null.
    public ExternalCalendar of(String kind) {
        if (CalendarLinks.GOOGLE.equals(kind)) {
            return google;
        }
        if (CalendarLinks.MICROSOFT.equals(kind)) {
            return microsoft;
        }
        if (CalendarLinks.ICS.equals(kind)) {
            return ics;
        }
        return null;
    }

    /// The client id the app opens Google's consent screen with: empty when
    /// the server simulates Google, and the app then skips the screen.
    public String googleClientId() {
        return googleClientId;
    }

    public String microsoftClientId() {
        return microsoftClientId;
    }
}
