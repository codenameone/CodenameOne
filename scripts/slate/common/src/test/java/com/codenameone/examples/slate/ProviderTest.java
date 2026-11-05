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
package com.codenameone.examples.slate;

import com.codenameone.examples.slate.ui.Timeline;

/// A provider opens their schedule and moves between the day, the week and
/// the agenda; their calendars screen offers every kind of connection.
public class ProviderTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        onEdt(() -> AppConfig.setMode(""));
        signIn(PROVIDER, "Provider");
        until(() -> findByName("timeline-grid") instanceof Timeline
                && OwnerTest.booked((Timeline) findByName("timeline-grid")), 20000,
                "today's bookings");
        shot("provider-day");
        click("view-week");
        until(() -> findByName("timeline-grid") instanceof Timeline
                && ((Timeline) findByName("timeline-grid")).lanes().size() == 7, 20000, "the week");
        shot("provider-week");
        click("view-agenda");
        click("later");
        click("menu-open");
        click("menu-calendars");
        waitForForm("Calendars");
        until(() -> has("connect-ics"), 20000, "the ways to connect a calendar");
        shot("provider-calendars");
        // The demo server simulates Google: connecting needs no account.
        click("connect-google");
        Actor provider = Actor.signIn(PROVIDER);
        until(() -> provider.list("/api/provider/calendars").size() > 0, 20000,
                "the simulated calendar to be connected");
        back();
        click("menu-open");
        click("menu-hours");
        waitForForm("Hours");
        until(() -> has("save"), 20000, "the week");
        shot("provider-hours");
        return true;
    }
}
