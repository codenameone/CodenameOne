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
package com.codenameone.examples.pulse;

import com.codename1.system.Lifecycle;
import com.codenameone.examples.pulse.net.Account;
import com.codenameone.examples.pulse.net.Session;
import com.codenameone.examples.pulse.notify.Notifications;
import com.codenameone.examples.pulse.member.Reminders;
import com.codenameone.examples.pulse.pay.CardForm;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Look;
import com.codenameone.examples.pulse.ui.Nav;

/// The app. It opens on whatever the stored session allows: the mode the user
/// was last in when they are still signed in, the welcome screen when not.
///
/// Where things are:
///
/// - `net` -- the session, and the typed clients generated from the contracts in
///   the `shared` module. Nothing else in the app builds a URL.
/// - `live` -- the WebSocket the server pushes changes down.
/// - `notify` -- push notifications, for a phone that is not looking.
/// - `ui` -- sign-in, settings and the pieces every screen is built from.
/// - `member` -- the timetable, bookings, the pass, the household and the plan.
/// - `desk` -- the front desk kiosk: scanning passes, NFC fobs and walk-ins.
/// - `trainer`, `owner` -- the coaching and studio-owner modes.
/// - `pay` -- saved cards.
/// - `src/main/css/theme.css` -- every colour, font and margin.
/// - `src/main/l10n` -- every word, in each language the app speaks.
/// - `Prefs` -- what the user chose in Settings.
/// - `Telemetry` -- what is reported about how the app is used, once the user
///   has agreed to it.
public class Pulse extends Lifecycle {
    @Override
    public void runApp() {
        // Before the first screen is built: a screen keeps the look and the
        // words it was built with.
        Look.apply();
        Lang.apply();
        Telemetry.start();
        Session.start(AppConfig.serverUrl());
        Account.start();
        Nav.launch();
    }

    /// Coming back to the app is how a card added on the payment provider's
    /// page is noticed: the browser it opened in tells the app nothing.
    ///
    /// It is also when the app asks for its push token, which the platform
    /// answers at once when nothing has changed, and when the class reminders
    /// are brought in line with the bookings, which may have changed elsewhere.
    @Override
    public void start() {
        super.start();
        CardForm.resumed();
        Notifications.start();
        Reminders.resumed();
    }

    /// The app is put away, which is the last sure moment to send the usage
    /// reports gathered since the last batch.
    @Override
    public void stop() {
        super.stop();
        Telemetry.flush();
    }
}
